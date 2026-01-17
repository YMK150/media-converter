package com.mediaconverter.service;

import com.mediaconverter.dto.VideoResolution;
import com.mediaconverter.entity.ConversionTask;
import com.mediaconverter.exception.LogicException;
import com.mediaconverter.repository.ConversionTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaConversionService {

    @Value("${media.converter.ffmpeg-path}")
    private String ffmpegPath;
    @Value("${media.converter.ffprobe-path}")
    private String ffprobePath;
    @Value("${media.converter.supported-audio-formats}")
    private List<String> supportedAudioFormats;
    @Value("${media.converter.supported-video-formats}")
    private List<String> supportedVideoFormats;
    @Value("${media.converter.conversion-timeout:1800}") // 30分钟
    private long conversionTimeout;

    private final SimpMessagingTemplate messagingTemplate;
    private final ConversionTaskRepository taskRepository;
    private final ProgressUpdateService progressUpdateService;
    
    // 进程管理器：跟踪正在运行的FFmpeg进程，key为taskId
    private final ConcurrentHashMap<Long, Process> runningProcesses = new ConcurrentHashMap<>();

    public ConversionTask convertMedia(ConversionTask task) {
        log.info("=== 开始转换任务: {} -> {} ===", task.getSourceFilePath(), task.getTargetFormat());

        try {
            // 刷新创建时间，要记录的是转换的耗时
            task.setCreatedAt(LocalDateTime.now());
            // 设置任务状态为运行中，初始进度为5%
            task.setStatus(ConversionTask.TaskStatus.RUNNING);
            task.setStartedAt(LocalDateTime.now());
            updateTaskProgressAsync(task, 5);
            
            // 验证目标格式
            validateTargetFormat(task.getTargetFormat());

            // 检查源文件
            File sourceFile = validateSourceFile(task.getSourceFilePath());
            task.setFileSize(sourceFile.length());

            // 创建目标目录
            createTargetDirectory(task.getTargetDirectory());

            // 生成目标文件路径
            String targetFilePath = generateTargetFilePath(task);
            log.info("目标文件路径: {}", targetFilePath);

            // 执行转换
            performConversion(sourceFile, targetFilePath, task);

            // 验证输出文件
            validateOutputFile(targetFilePath, task);

            // 更新任务完成状态
            updateTaskCompletion(task, targetFilePath);

            log.info("=== 转换完成 ===");

        } catch (Exception e) {
            // 如果任务已被取消，不要将其标记为失败
            if (isTaskCancelled(task.getId())) {
                log.info("任务已被取消，不处理为失败: taskId={}", task.getId());
                task.setStatus(ConversionTask.TaskStatus.CANCELLED);
                task.setCompletedAt(LocalDateTime.now());
            } else {
                handleConversionError(task, e);
            }
        }

        saveAndNotifyTask(task);
        return task;
    }

    /**
     * 执行转换（修复版，支持真实进度更新和取消功能）
     */
    private void performConversion(File sourceFile, String targetFilePath, ConversionTask task)
            throws IOException, InterruptedException {
        log.info("开始媒体转换: {} -> {}", sourceFile.getName(), new File(targetFilePath).getName());

        // 检查任务是否已被取消
        if (isTaskCancelled(task.getId())) {
            log.info("任务已被取消，停止转换: ID={}", task.getId());
            throw new LogicException("转换任务已被取消");
        }

        // 获取媒体总时长（用于计算真实进度）
        double totalDuration = getMediaDuration(sourceFile.getAbsolutePath());
        log.info("媒体总时长: {} 秒", totalDuration);

        List<String> command = buildConversionCommand(sourceFile, targetFilePath, task);
        log.info("转换命令: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        // 不重定向错误流，分别读取标准输出和错误输出
        processBuilder.redirectErrorStream(false);

        Process process = null;
        StringBuilder combinedOutput = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();

        try {
            process = processBuilder.start();
            
            // 注册进程到进程管理器
            if (task.getId() != null) {
                runningProcesses.put(task.getId(), process);
                log.debug("已注册FFmpeg进程: taskId={}", task.getId());
            }

            // 初始化进度为5%
            updateTaskProgressAsync(task, 5);

            // 启动线程分别读取标准输出和错误输出
            // FFmpeg的进度信息在stderr中，需要从那里解析
            Thread outputThread = readProcessStream(process.getInputStream(), combinedOutput, "Output");
            Thread errorThread = readProcessStreamWithRealProgress(process.getErrorStream(), errorOutput, "Error", task, totalDuration);

            // 等待进程完成，同时定期检查取消状态
            boolean finished = false;
            long startTime = System.currentTimeMillis();
            while (!finished && !isTaskCancelled(task.getId())) {
                finished = process.waitFor(500, TimeUnit.MILLISECONDS); // 每500ms检查一次
                if (!finished && System.currentTimeMillis() - startTime > conversionTimeout * 1000) {
                    break; // 超时
                }
            }

            // 如果任务被取消，立即终止进程
            if (isTaskCancelled(task.getId())) {
                log.info("检测到任务已取消，终止FFmpeg进程: taskId={}", task.getId());
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
                throw new LogicException("转换任务已被取消");
            }

            // 等待输出线程结束
            outputThread.join(5000);
            errorThread.join(5000);

            if (!finished) {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
                String errorMsg = "转换过程超时（超过 " + conversionTimeout + " 秒）";
                if (errorOutput.length() > 0) {
                    errorMsg += "\nFFmpeg错误输出:\n" + errorOutput.toString();
                }
                throw new LogicException(errorMsg);
            }

            // 在检查退出码之前，先检查任务是否被取消
            if (isTaskCancelled(task.getId())) {
                log.info("任务在进程结束时检测到已取消: taskId={}", task.getId());
                throw new LogicException("转换任务已被取消");
            }

            int exitCode = process.exitValue();

            if (exitCode != 0) {
                // 再次检查是否是因为取消导致的非0退出码
                if (isTaskCancelled(task.getId())) {
                    log.info("进程退出码非0，但任务已被取消，不视为失败: taskId={}, exitCode={}", task.getId(), exitCode);
                    throw new LogicException("转换任务已被取消");
                }
                String detailedError = getDetailedErrorMessage(exitCode, combinedOutput.toString(), errorOutput.toString());
                throw new LogicException("FFmpeg转换失败\n退出码: " + exitCode + "\n" + detailedError);
            }

            log.info("转换执行成功");

        } finally {
            // 从进程管理器中移除
            if (task.getId() != null) {
                runningProcesses.remove(task.getId());
                log.debug("已移除FFmpeg进程: taskId={}", task.getId());
            }
            
            // 确保进程被终止
            if (process != null && process.isAlive()) {
                log.warn("进程仍在运行，强制终止: taskId={}", task.getId());
                process.destroyForcibly();
            }
        }
    }

    /**
     * 读取进程输出流
     */
    private Thread readProcessStream(InputStream inputStream, StringBuilder output, String streamName) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                    output.append(line).append("\n");
                    log.debug("FFmpeg {}: {}", streamName, line);
                }
            } catch (IOException e) {
                log.error("读取进程{}流失败", streamName, e);
            }
        }, "FFmpeg" + streamName + "Reader");

        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * 读取进程错误流并解析真实进度（FFmpeg的进度信息在stderr中）
     */
    private Thread readProcessStreamWithRealProgress(InputStream inputStream, StringBuilder output, String streamName, ConversionTask task, double totalDuration) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                int lastProgress = 5;
                // 匹配时间格式: time=00:00:05.00 或 time=00:05.00
                Pattern timePattern = Pattern.compile("time=(\\d{1,2}):(\\d{2}):(\\d{2}\\.\\d{2})|time=(\\d{2}):(\\d{2}\\.\\d{2})");
                
                while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                    output.append(line).append("\n");
                    log.debug("FFmpeg {}: {}", streamName, line);
                    
                    // 解析FFmpeg进度信息中的时间
                    // 格式示例: frame=  123 fps= 25 q=28.0 size=    1024kB time=00:00:05.00 bitrate=1677.7kbits/s speed=1.0x
                    Matcher matcher = timePattern.matcher(line);
                    if (matcher.find()) {
                        try {
                            double currentTime;
                            if (matcher.group(1) != null) {
                                // 格式: HH:MM:SS.mm
                                int hours = Integer.parseInt(matcher.group(1));
                                int minutes = Integer.parseInt(matcher.group(2));
                                double seconds = Double.parseDouble(matcher.group(3));
                                currentTime = hours * 3600 + minutes * 60 + seconds;
                            } else {
                                // 格式: MM:SS.mm
                                int minutes = Integer.parseInt(matcher.group(4));
                                double seconds = Double.parseDouble(matcher.group(5));
                                currentTime = minutes * 60 + seconds;
                            }
                            
                            int progress;
                            if (totalDuration > 0) {
                                // 基于真实时长的进度计算
                                progress = (int) Math.min(95, Math.max(5, (currentTime / totalDuration) * 100));
                            } else {
                                // 如果无法获取总时长，使用基于时间的估算（每10秒增加1%）
                                progress = Math.min(95, 5 + (int)(currentTime / 10));
                            }
                            
                            // 只有当进度真正增加时才更新，避免频繁更新
                            if (progress > lastProgress) {
                                updateTaskProgressAsync(task, progress);
                                lastProgress = progress;
                                if (totalDuration > 0) {
                                    log.debug("转换进度: {}% (当前时间: {}s / 总时长: {}s)", progress, String.format("%.2f", currentTime), String.format("%.2f", totalDuration));
                                } else {
                                    log.debug("转换进度: {}% (当前时间: {}s, 总时长未知)", progress, String.format("%.2f", currentTime));
                                }
                            }
                        } catch (NumberFormatException e) {
                            log.warn("解析进度时间失败: {}", line);
                        }
                    }
                }
            } catch (IOException e) {
                log.error("读取进程{}流失败", streamName, e);
            }
        }, "FFmpeg" + streamName + "ProgressReader");

        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * 获取详细的错误信息（增强版）
     */
    private String getDetailedErrorMessage(int exitCode, String standardOutput, String errorOutput) {
        StringBuilder detailedError = new StringBuilder();

        // 添加退出码描述
        detailedError.append("错误类型: ").append(getExitCodeDescription(exitCode)).append("\n\n");

        // 优先使用错误输出
        String primaryOutput = errorOutput.isEmpty() ? standardOutput : errorOutput;

        if (StringUtils.isNotBlank(primaryOutput)) {
            detailedError.append("FFmpeg输出分析:\n");
            detailedError.append("=").append(StringUtils.repeat("=", 50)).append("\n");

            // 分析特定的错误模式
            boolean foundSpecificError = analyzeFfmpegErrors(primaryOutput, detailedError);

            if (!foundSpecificError) {
                // 如果没有找到特定错误模式，显示关键错误行
                detailedError.append("原始输出中的关键信息:\n");
                String[] lines = primaryOutput.split("\n");
                int lineCount = 0;
                for (String line : lines) {
                    if (line.contains("Error") || line.contains("error") ||
                            line.contains("Failed") || line.contains("failed") ||
                            line.contains("Invalid") || line.contains("invalid") ||
                            line.contains("Unsupported") || line.contains("unsupported") ||
                            line.contains("No such") || line.contains("not found")) {
                        detailedError.append("• ").append(line.trim()).append("\n");
                        lineCount++;
                        if (lineCount >= 10) break; // 限制行数
                    }
                }

                if (lineCount == 0) {
                    // 如果没有关键错误行，显示最后10行输出
                    detailedError.append("最后10行输出:\n");
                    String[] allLines = primaryOutput.split("\n");
                    int start = Math.max(0, allLines.length - 10);
                    for (int i = start; i < allLines.length; i++) {
                        detailedError.append("• ").append(allLines[i].trim()).append("\n");
                    }
                }
            }

            detailedError.append("=").append(StringUtils.repeat("=", 50)).append("\n");
        }

        // 针对退出码187的特殊处理
        if (exitCode == 187) {
            detailedError.append("\n针对退出码187的建议:\n");
            detailedError.append("• 检查输入文件格式是否完整且未损坏\n");
            detailedError.append("• 验证FFmpeg是否支持该编码格式\n");
            detailedError.append("• 尝试使用不同的编码参数或预设\n");
            detailedError.append("• 检查输出路径权限和磁盘空间\n");
            detailedError.append("• 可能是分辨率缩放参数问题，尝试简化缩放滤镜\n");
        }

        return detailedError.toString();
    }

    /**
     * 分析FFmpeg特定错误
     */
    private boolean analyzeFfmpegErrors(String output, StringBuilder detailedError) {
        boolean foundError = false;

        // 编码器相关错误
        if (output.contains("Unknown encoder") || output.contains("Encoder not found")) {
            detailedError.append("• 编码器未找到：请检查FFmpeg是否包含所需的编码库\n");
            foundError = true;
        }
        if (output.contains("Unsupported codec") || output.contains("Codec not supported")) {
            detailedError.append("• 不支持的编解码器：当前FFmpeg版本不支持该格式\n");
            foundError = true;
        }

        // 文件相关错误
        if (output.contains("No such file or directory")) {
            detailedError.append("• 文件不存在：请检查源文件路径是否正确\n");
            foundError = true;
        }
        if (output.contains("Permission denied")) {
            detailedError.append("• 权限不足：请检查文件读写权限\n");
            foundError = true;
        }
        if (output.contains("Invalid data found") || output.contains("moov atom not found")) {
            detailedError.append("• 文件损坏：源文件可能已损坏或不完整\n");
            foundError = true;
        }

        // 参数相关错误
        if (output.contains("Invalid argument") || output.contains("Unrecognized option")) {
            detailedError.append("• 参数错误：FFmpeg命令包含无效参数\n");
            // 尝试找出具体是哪个参数
            Pattern argPattern = Pattern.compile("(Unrecognized option '[^']+')|(Invalid argument[^\\n]+)");
            Matcher matcher = argPattern.matcher(output);
            if (matcher.find()) {
                detailedError.append("  具体错误: ").append(matcher.group().trim()).append("\n");
            }
            foundError = true;
        }

        // 格式相关错误
        if (output.contains("Format not supported") || output.contains("Unsupported format")) {
            detailedError.append("• 格式不支持：FFmpeg不支持该输入或输出格式\n");
            foundError = true;
        }

        // 流相关错误
        if (output.contains("Stream mapping problem") || output.contains("Cannot determine format")) {
            detailedError.append("• 流映射问题：无法确定媒体流格式\n");
            foundError = true;
        }

        // 滤镜相关错误（针对退出码187）
        if (output.contains("scale") && (output.contains("Invalid") || output.contains("not found"))) {
            detailedError.append("• 缩放滤镜错误：可能是分辨率参数问题\n");
            detailedError.append("  建议：尝试简化缩放参数或使用默认分辨率\n");
            foundError = true;
        }

        return foundError;
    }

    /**
     * 获取退出码描述（增强版）
     */
    private String getExitCodeDescription(int exitCode) {
        switch (exitCode) {
            case 1: return "常规错误（参数错误或文件问题）";
            case 2: return "FFmpeg内部错误";
            case 127: return "FFmpeg命令未找到，请检查路径配置";
            case 134: return "程序崩溃（可能由于内存不足）";
            case 137: return "进程被终止（可能由于超时或资源限制）";
            case 139: return "段错误（内存访问违规）";
            case 187: return "FFmpeg参数错误或格式不支持（常见于编码器问题）";
            case 255: return "用户中断操作";
            default:
                if (exitCode > 128 && exitCode < 165) {
                    return "进程被信号 " + (exitCode - 128) + " 终止";
                }
                return "未知错误类型（退出码: " + exitCode + ")";
        }
    }

    /**
     * 验证目标格式
     */
    private void validateTargetFormat(String targetFormat) {
        if (!isSupportedFormat(targetFormat)) {
            throw new LogicException("不支持的目标格式: " + targetFormat);
        }
        log.debug("目标格式验证通过: {}", targetFormat);
    }

    /**
     * 验证源文件
     */
    private File validateSourceFile(String sourceFilePath) throws IOException {
        File sourceFile = new File(sourceFilePath);
        if (!sourceFile.exists()) {
            throw new LogicException("源文件不存在: " + sourceFilePath);
        }
        if (sourceFile.length() == 0) {
            throw new LogicException("源文件为空: " + sourceFilePath);
        }
        log.debug("源文件验证通过: {} (大小: {} bytes)", sourceFile.getAbsolutePath(), sourceFile.length());
        return sourceFile;
    }

    /**
     * 创建目标目录
     */
    private void createTargetDirectory(String targetDirectory) throws IOException {
        Path targetDirPath = Paths.get(targetDirectory);
        if (!Files.exists(targetDirPath)) {
            Files.createDirectories(targetDirPath);
            log.debug("目标目录已创建: {}", targetDirectory);
        }
    }

    /**
     * 生成目标文件路径
     */
    private String generateTargetFilePath(ConversionTask task) {
        String sourceName = new File(task.getSourceFilePath()).getName();
        String baseName = sourceName.substring(0, sourceName.lastIndexOf('.'));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = baseName + "_" + timestamp + "." + task.getTargetFormat();
        return Paths.get(task.getTargetDirectory(), fileName).toString();
    }

    /**
     * 验证输出文件
     */
    private void validateOutputFile(String targetFilePath, ConversionTask task) throws IOException {
        File outputFile = new File(targetFilePath);
        if (!outputFile.exists()) {
            throw new LogicException("转换失败：输出文件不存在");
        }
        if (outputFile.length() == 0) {
            throw new LogicException("转换失败：输出文件为空");
        }
        log.info("输出文件验证通过: {} (大小: {} bytes)", targetFilePath, outputFile.length());
    }

    /**
     * 更新任务完成状态
     */
    private void updateTaskCompletion(ConversionTask task, String targetFilePath) {
        File outputFile = new File(targetFilePath);
        task.setOutputFilePath(targetFilePath);
        task.setOutputFileSize(outputFile.length());
        task.setStatus(ConversionTask.TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        task.setProgress(100);
        
        // 使用同步更新确保最终进度能正确设置
        progressUpdateService.updateProgressSync(task.getId(), 100, ConversionTask.TaskStatus.COMPLETED);
    }

    /**
     * 处理转换错误
     */
    private void handleConversionError(ConversionTask task, Exception e) {
        // 如果任务已被取消，不要将其标记为失败
        if (task.getId() != null && isTaskCancelled(task.getId())) {
            log.info("任务已被取消，不处理为失败: taskId={}", task.getId());
            task.setStatus(ConversionTask.TaskStatus.CANCELLED);
            task.setCompletedAt(LocalDateTime.now());
            return;
        }
        
        // 检查异常消息是否包含"取消"
        if (e.getMessage() != null && e.getMessage().contains("取消")) {
            log.info("检测到取消异常，将任务标记为已取消: taskId={}", task.getId());
            task.setStatus(ConversionTask.TaskStatus.CANCELLED);
            task.setCompletedAt(LocalDateTime.now());
            return;
        }
        
        log.error("转换失败: {}", e.getMessage(), e);
        task.setStatus(ConversionTask.TaskStatus.FAILED);
        task.setErrorMessage(getFriendlyErrorMessage(e));
        task.setCompletedAt(LocalDateTime.now());
        // 保持进度为0或当前进度，不重置为100
    }

    /**
     * 构建转换命令（简化版，避免复杂滤镜）
     */
    private List<String> buildConversionCommand(File sourceFile, String targetFilePath, ConversionTask task) {
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-i");
        command.add(sourceFile.getAbsolutePath());

        String targetFormat = task.getTargetFormat().toLowerCase();

        if (supportedVideoFormats.contains(targetFormat)) {
            addVideoConversionParameters(command, task);
        } else if (supportedAudioFormats.contains(targetFormat)) {
            addAudioConversionParameters(command, task);
        } else {
            addDefaultConversionParameters(command, task);
        }

        command.add("-y");
        command.add(targetFilePath);

        return command;
    }

    /**
     * 添加视频转换参数（简化版）
     */
    private void addVideoConversionParameters(List<String> command, ConversionTask task) {
        command.add("-c:v");
        command.add("libx264");

        command.add("-preset");
        command.add("medium");
        command.add("-crf");
        command.add("23");

        // 简化分辨率缩放逻辑
        String scaleFilter = buildSimpleScaleFilter(task);
        if (!scaleFilter.isEmpty()) {
            command.add("-vf");
            command.add(scaleFilter);
            log.info("应用视频滤镜: {}", scaleFilter);
        }

        if (task.getVideoBitrate() != null && task.getVideoBitrate() > 0) {
            command.add("-b:v");
            command.add(task.getVideoBitrate() + "k");
            log.info("设置视频比特率: {}k", task.getVideoBitrate());
        }

        if (task.getFrameRate() != null && task.getFrameRate() > 0) {
            command.add("-r");
            command.add(String.valueOf(task.getFrameRate()));
            log.info("设置帧率: {}", task.getFrameRate());
        }

        command.add("-c:a");
        command.add("aac");
        if (task.getAudioBitrate() != null && task.getAudioBitrate() > 0) {
            command.add("-b:a");
            command.add(task.getAudioBitrate() + "k");
            log.info("设置音频比特率: {}k", task.getAudioBitrate());
        }
        command.add("-ac");
        command.add("2");

        command.add("-pix_fmt");
        command.add("yuv420p");
    }

    /**
     * 构建简化的分辨率缩放滤镜
     */
    private String buildSimpleScaleFilter(ConversionTask task) {
        boolean hasWidth = task.getVideoWidth() != null && task.getVideoWidth() > 0;
        boolean hasHeight = task.getVideoHeight() != null && task.getVideoHeight() > 0;

        if (!hasWidth && !hasHeight) {
            log.debug("未设置目标分辨率，使用原始分辨率");
            return "";
        }

        try {
            VideoResolution originalResolution = getVideoResolution(task.getSourceFilePath());
            log.info("原始分辨率: {}x{}", originalResolution.getWidth(), originalResolution.getHeight());

            int targetWidth = hasWidth ? task.getVideoWidth() : -1;
            int targetHeight = hasHeight ? task.getVideoHeight() : -1;

            log.info("目标分辨率设置: {}x{}", targetWidth, targetHeight);

            // 简化逻辑，只处理基本缩放
            if (targetWidth > 0 && targetHeight > 0) {
                // 确保宽高为偶数
                targetWidth = ensureEven(targetWidth);
                targetHeight = ensureEven(targetHeight);

                // 使用简单的缩放滤镜
                String filter = String.format("scale=%d:%d", targetWidth, targetHeight);
                log.info("使用简化缩放滤镜: {}", filter);
                return filter;
            }

        } catch (Exception e) {
            log.error("构建缩放滤镜失败，使用原始分辨率", e);
        }

        return "";
    }

    /**
     * 添加音频转换参数
     */
    private void addAudioConversionParameters(List<String> command, ConversionTask task) {
        String targetFormat = task.getTargetFormat().toLowerCase();

        command.add("-vn");

        if ("mp3".equals(targetFormat)) {
            command.add("-c:a");
            command.add("libmp3lame");
        } else if ("aac".equals(targetFormat)) {
            command.add("-c:a");
            command.add("aac");
        } else if ("wav".equals(targetFormat)) {
            command.add("-c:a");
            command.add("pcm_s16le");
        } else if ("flac".equals(targetFormat)) {
            command.add("-c:a");
            command.add("flac");
        } else {
            command.add("-c:a");
            command.add("aac");
        }

        if (task.getAudioBitrate() != null && task.getAudioBitrate() > 0) {
            command.add("-b:a");
            command.add(task.getAudioBitrate() + "k");
        }
        command.add("-ac");
        command.add("2");
        command.add("-ar");
        command.add("44100");
    }

    /**
     * 添加默认转换参数
     */
    private void addDefaultConversionParameters(List<String> command, ConversionTask task) {
        command.add("-c:v");
        command.add("libx264");
        command.add("-c:a");
        command.add("aac");
    }

    /**
     * 确保数值为偶数
     */
    private int ensureEven(int value) {
        return value % 2 == 0 ? value : value + 1;
    }

    /**
     * 获取媒体总时长（秒）
     */
    private double getMediaDuration(String filePath) {
        try {
            List<String> command = List.of(
                    ffprobePath,
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    filePath
            );

            Process process = new ProcessBuilder(command).start();
            String output = readProcessOutput(process);

            int exitCode = process.waitFor();
            if (exitCode == 0 && StringUtils.isNotBlank(output)) {
                try {
                    double duration = Double.parseDouble(output.trim());
                    if (duration > 0) {
                        log.debug("获取媒体时长成功: {} 秒", duration);
                        return duration;
                    }
                } catch (NumberFormatException e) {
                    log.warn("解析媒体时长失败: {}", output);
                }
            }
        } catch (Exception e) {
            log.warn("获取媒体时长失败，将使用估算进度: {}", e.getMessage());
        }

        // 如果无法获取时长，返回0，进度解析将使用备用逻辑
        return 0;
    }

    /**
     * 获取视频分辨率
     */
    private VideoResolution getVideoResolution(String filePath) {
        try {
            List<String> command = List.of(
                    ffprobePath,
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=width,height",
                    "-of", "csv=s=x:p=0",
                    filePath
            );

            Process process = new ProcessBuilder(command).start();
            String output = readProcessOutput(process);

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                String[] resolution = output.trim().split("x");
                if (resolution.length == 2) {
                    return new VideoResolution(Integer.parseInt(resolution[0]), Integer.parseInt(resolution[1]));
                }
            }
        } catch (Exception e) {
            log.error("获取视频分辨率失败", e);
        }

        return new VideoResolution(1920, 1080);
    }

    /**
     * 读取进程输出
     */
    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        return output.toString();
    }

    /**
     * 获取友好错误信息
     */
    private String getFriendlyErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null) return "转换过程中发生未知错误";

        if (message.contains("退出码:") || message.contains("FFmpeg转换失败")) {
            // 这是我们的详细错误信息，直接返回
            return message;
        }

        return "转换失败: " + message;
    }

    /**
     * 保存并通知任务状态
     */
    private void saveAndNotifyTask(ConversionTask task) {
        try {
            taskRepository.save(task);
            messagingTemplate.convertAndSend("/topic/conversion-progress", task);
        } catch (Exception e) {
            log.error("保存任务状态或发送通知失败", e);
        }
    }

    /**
     * 异步更新任务进度（使用专用服务）
     */
    private void updateTaskProgressAsync(ConversionTask task, int progress) {
        if (task.getId() == null) {
            log.warn("任务ID为空，无法更新进度");
            return;
        }
        
        // 确保进度在有效范围内
        int validProgress = Math.max(0, Math.min(progress, 100));
        
        // 使用专用的进度更新服务
        progressUpdateService.updateProgressAsync(task.getId(), validProgress, task.getStatus())
                .exceptionally(throwable -> {
                    log.warn("异步更新进度时发生异常: {}", throwable.getMessage());
                    return null;
                });
    }

    private boolean isSupportedFormat(String format) {
        if (StringUtils.isNotBlank(format)) {
            return supportedVideoFormats.contains(format.toLowerCase()) || supportedAudioFormats.contains(format.toLowerCase());
        }
        return false;
    }

    /**
     * 检查任务是否已被取消
     */
    private boolean isTaskCancelled(Long taskId) {
        if (taskId == null) {
            return false;
        }
        return taskRepository.findById(taskId)
                .map(task -> task.getStatus() == ConversionTask.TaskStatus.CANCELLED)
                .orElse(false);
    }

    /**
     * 取消正在运行的转换任务（终止FFmpeg进程）
     */
    public boolean cancelRunningTask(Long taskId) {
        if (taskId == null) {
            return false;
        }
        
        Process process = runningProcesses.get(taskId);
        if (process != null && process.isAlive()) {
            log.info("正在终止FFmpeg进程: taskId={}", taskId);
            try {
                // 先尝试正常终止
                process.destroy();
                
                // 等待最多3秒
                boolean terminated = process.waitFor(3, TimeUnit.SECONDS);
                
                if (!terminated) {
                    // 如果3秒后仍未终止，强制终止
                    log.warn("进程未在3秒内终止，强制终止: taskId={}", taskId);
                    process.destroyForcibly();
                    
                    // 再等待2秒确保进程被终止
                    process.waitFor(2, TimeUnit.SECONDS);
                }
                
                log.info("FFmpeg进程已终止: taskId={}", taskId);
                return true;
            } catch (InterruptedException e) {
                log.error("等待进程终止时被中断: taskId={}", taskId, e);
                Thread.currentThread().interrupt();
                // 强制终止
                process.destroyForcibly();
                return true;
            } catch (Exception e) {
                log.error("终止FFmpeg进程失败: taskId={}", taskId, e);
                // 尝试强制终止
                try {
                    process.destroyForcibly();
                } catch (Exception ex) {
                    log.error("强制终止进程也失败: taskId={}", taskId, ex);
                }
                return false;
            } finally {
                // 从进程管理器中移除
                runningProcesses.remove(taskId);
            }
        } else {
            log.debug("未找到运行中的进程或进程已结束: taskId={}", taskId);
            return false;
        }
    }
}