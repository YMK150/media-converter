package com.mediaconverter.service;

import com.mediaconverter.dto.BatchConversionResultDto;
import com.mediaconverter.dto.ConversionRequest;
import com.mediaconverter.dto.QueueStats;
import com.mediaconverter.dto.TaskListDto;
import com.mediaconverter.entity.ConversionTask;
import com.mediaconverter.exception.LogicException;
import com.mediaconverter.repository.ConversionTaskRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversionQueueService {

    private final Executor conversionExecutor;
    private final ConversionTaskRepository taskRepository;
    private final MediaConversionService conversionService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${media.converter.supported-audio-formats}")
    private List<String> supportedAudioFormats;
    @Value("${media.converter.supported-video-formats}")
    private List<String> supportedVideoFormats;

    // 使用简单的标志位替代复杂的计数器
    private volatile boolean isTaskRunning = false;
    private final ConcurrentHashMap<Long, CompletableFuture<ConversionTask>> runningTasks = new ConcurrentHashMap<>();
    private final Object queueLock = new Object();

    public Long createConversionTask(@Valid ConversionRequest request) {
        log.info("收到转换请求: {}", request);

        // 验证格式支持
        if (!isSupportedFormat(request.getTargetFormat())) {
            throw new LogicException("不支持的目标格式: " + request.getTargetFormat());
        }

        // 验证源文件是否存在
        String sourceFile = request.getSourceFiles().get(0);
        if (!Files.exists(Paths.get(sourceFile))) {
            throw new LogicException("源文件不存在: " + sourceFile);
        }

        // 创建转换任务
        ConversionTask task = buildConversionTask(sourceFile, request);

        // 添加到队列
        ConversionTask savedTask = addTask(task);

        // 触发队列处理
        triggerQueueProcessing();

        return savedTask.getId();
    }

    public BatchConversionResultDto createBatchConversionTasks(@Valid ConversionRequest request) {
        log.info("收到批量转换请求，文件数量: {}", request.getSourceFiles().size());

        // 验证格式支持
        if (!isSupportedFormat(request.getTargetFormat())) {
            throw new LogicException("不支持的目标格式: " + request.getTargetFormat());
        }

        // 过滤存在的文件
        List<String> existingFiles = request.getSourceFiles().stream()
                .filter(file -> Files.exists(Paths.get(file)))
                .toList();

        if (existingFiles.isEmpty()) {
            throw new LogicException("所有源文件都不存在");
        }

        // 为每个文件创建转换任务
        int successCount = 0;
        int failCount = 0;
        for (String sourceFile : existingFiles) {
            try {
                ConversionTask task = buildConversionTask(sourceFile, request);
                ConversionTask savedTask = addTask(task);
                successCount++;
                log.debug("批量任务已创建: ID={}", savedTask.getId());
            } catch (Exception e) {
                failCount++;
                log.error("创建批量任务失败: 文件={}", sourceFile, e);
            }
        }

        // 触发队列处理
        triggerQueueProcessing();

        BatchConversionResultDto dto = new BatchConversionResultDto();
        dto.setTotal(existingFiles.size());
        dto.setSuccessCount(successCount);
        dto.setFailCount(failCount);
        dto.setMessage(String.format("批量转换任务已创建，成功%s个，失败%s个", successCount, failCount));
        return dto;
    }

    /**
     * 触发队列处理
     */
    private void triggerQueueProcessing() {
        if (!isTaskRunning) {
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(100); // 短暂延迟，确保事务提交
                    processQueue();
                } catch (Exception e) {
                    log.error("触发队列处理异常", e);
                }
            }, conversionExecutor);
        }
    }

    /**
     * 构建转换任务对象
     */
    private ConversionTask buildConversionTask(String sourceFile, ConversionRequest request) {
        ConversionTask task = new ConversionTask();
        task.setSourceFilePath(sourceFile);
        task.setTargetFormat(request.getTargetFormat());
        task.setTargetDirectory(request.getTargetDirectory());

        // 生成目标文件名
        String sourceFileName = new File(sourceFile).getName();
        String targetFileName = generateTargetFileName(sourceFileName, request.getTargetFormat());
        task.setTargetFileName(targetFileName);

        // 设置转换参数
        task.setVideoWidth(request.getVideoWidth());
        task.setVideoHeight(request.getVideoHeight());
        task.setAudioBitrate(request.getAudioBitrate());
        task.setVideoBitrate(request.getVideoBitrate());
        task.setFrameRate(request.getFrameRate());

        return task;
    }

    /**
     * 生成目标文件名
     */
    private String generateTargetFileName(String sourceFileName, String targetFormat) {
        if (sourceFileName == null || sourceFileName.trim().isEmpty()) {
            return "converted_" + System.currentTimeMillis() + "." + targetFormat;
        }

        // 获取不带扩展名的文件名
        int lastDotIndex = sourceFileName.lastIndexOf('.');
        String baseName = lastDotIndex > 0 ? sourceFileName.substring(0, lastDotIndex) : sourceFileName;

        // 确保目标格式不以点开头
        String format = targetFormat.startsWith(".") ? targetFormat.substring(1) : targetFormat;

        // 添加时间戳避免重名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return baseName + "_" + timestamp + "." + format;
    }

    /**
     * 添加转换任务到队列
     */
    public ConversionTask addTask(ConversionTask task) {
        task.setStatus(ConversionTask.TaskStatus.PENDING);
        task.setCreatedAt(LocalDateTime.now());
        task.setProgress(0);
        ConversionTask savedTask = taskRepository.save(task);
        log.info("转换任务已添加到队列: ID={}, 文件={}", savedTask.getId(), savedTask.getSourceFilePath());
        return savedTask;
    }

    /**
     * 取消转换任务
     */
    @Transactional(timeout = 10)
    public boolean cancelTask(Long taskId) {
        try {
            Optional<ConversionTask> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                log.warn("取消任务失败: 任务不存在, ID={}", taskId);
                return false;
            }

            ConversionTask task = taskOpt.get();

            // 如果任务已经完成或取消，直接返回
            if (task.getStatus() == ConversionTask.TaskStatus.COMPLETED ||
                    task.getStatus() == ConversionTask.TaskStatus.FAILED ||
                    task.getStatus() == ConversionTask.TaskStatus.CANCELLED) {
                log.info("任务已处于最终状态，无法取消: ID={}, 状态={}", taskId, task.getStatus());
                return false;
            }

            // 如果任务正在运行，尝试中断
            if (task.getStatus() == ConversionTask.TaskStatus.RUNNING) {
                // 首先真正终止FFmpeg进程
                boolean processTerminated = conversionService.cancelRunningTask(taskId);
                if (processTerminated) {
                    log.info("已终止FFmpeg进程: ID={}", taskId);
                }
                
                // 然后取消CompletableFuture
                CompletableFuture<ConversionTask> future = runningTasks.get(taskId);
                if (future != null && !future.isDone()) {
                    future.cancel(true);
                    runningTasks.remove(taskId);
                    isTaskRunning = false;
                    log.info("已中断运行中的任务: ID={}", taskId);
                }
            }

            // 更新任务状态
            task.setStatus(ConversionTask.TaskStatus.CANCELLED);
            task.setCompletedAt(LocalDateTime.now());
            taskRepository.save(task);
            
            // 发送WebSocket通知
            try {
                messagingTemplate.convertAndSend("/topic/conversion-progress", task);
            } catch (Exception e) {
                log.warn("发送取消通知失败: taskId={}", taskId, e);
            }

            log.info("转换任务已取消: ID={}", taskId);
            return true;

        } catch (Exception e) {
            log.error("取消任务失败: ID={}", taskId, e);
            return false;
        }
    }

    /**
     * 删除转换任务
     */
    @Transactional(timeout = 10)
    public boolean deleteTask(Long taskId) {
        try {
            Optional<ConversionTask> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                return false;
            }

            ConversionTask task = taskOpt.get();

            // 如果任务正在运行，先取消并中断
            if (task.getStatus() == ConversionTask.TaskStatus.RUNNING) {
                // 首先真正终止FFmpeg进程
                boolean processTerminated = conversionService.cancelRunningTask(taskId);
                if (processTerminated) {
                    log.info("已终止FFmpeg进程以便删除: ID={}", taskId);
                }
                
                // 然后取消CompletableFuture
                CompletableFuture<ConversionTask> future = runningTasks.get(taskId);
                if (future != null && !future.isDone()) {
                    future.cancel(true);
                    runningTasks.remove(taskId);
                    isTaskRunning = false;
                    log.info("已中断运行中的任务以便删除: ID={}", taskId);
                }
            }

            // 从数据库中删除任务
            taskRepository.deleteById(taskId);

            log.info("转换任务已删除: ID={}, 文件={}", taskId, task.getSourceFilePath());
            return true;

        } catch (Exception e) {
            log.error("删除任务失败: ID={}", taskId, e);
            return false;
        }
    }

    /**
     * 获取任务状态
     */
    public ConversionTask getTaskStatus(Long taskId) {
        return taskRepository.findById(taskId).orElse(null);
    }

    /**
     * 获取任务列表（分页）
     */
    public TaskListDto getTasks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        TaskListDto dto = new TaskListDto();
        dto.setTasks(taskRepository.findAll(pageable).getContent());
        dto.setTotal(getTotalTaskCount());
        return dto;
    }

    /**
     * 获取任务总数
     */
    public long getTotalTaskCount() {
        return taskRepository.count();
    }

    /**
     * 获取运行中的任务数量
     */
    public int getRunningTasksCount() {
        return isTaskRunning ? 1 : 0;
    }

    /**
     * 定时处理队列
     */
    @Scheduled(fixedDelayString = "${media.converter.queue-check-interval:5000}")
    public void processQueueScheduled() {
        try {
            log.debug("定时检查任务队列...");
            processQueue();
        } catch (Exception e) {
            log.error("定时处理队列时发生异常", e);
        }
    }

    /**
     * 处理队列中的任务（简化版）
     */
    private void processQueue() {
        synchronized (queueLock) {
            // 如果已经有任务在运行，直接返回
            if (isTaskRunning) {
                log.debug("已有任务正在运行，跳过队列处理");
                return;
            }

            try {
                // 获取第一个待处理任务
                List<ConversionTask> pendingTasks = taskRepository.findPendingTasksWithLimit(1);

                if (pendingTasks.isEmpty()) {
                    log.debug("当前没有待处理的任务");
                    return;
                }

                ConversionTask task = pendingTasks.get(0);
                log.info("发现待处理任务: ID={}, 文件={}", task.getId(), task.getSourceFilePath());

                // 检查任务是否已经在运行（理论上不会发生）
                if (runningTasks.containsKey(task.getId())) {
                    log.debug("任务已在运行中，跳过: ID={}", task.getId());
                    return;
                }

                // 使用原子性更新尝试获取任务
                boolean acquired = acquireTaskForProcessing(task.getId());
                if (!acquired) {
                    log.debug("任务已被其他线程获取，跳过: ID={}", task.getId());
                    return;
                }

                // 重新获取任务最新状态
                Optional<ConversionTask> updatedTaskOpt = taskRepository.findById(task.getId());
                if (updatedTaskOpt.isEmpty()) {
                    log.warn("任务不存在，跳过: ID={}", task.getId());
                    return;
                }

                ConversionTask updatedTask = updatedTaskOpt.get();

                // 再次检查源文件是否存在
                if (!isSourceFileExists(updatedTask.getSourceFilePath())) {
                    log.warn("源文件不存在，标记任务为失败: ID={}, 文件={}",
                            updatedTask.getId(), updatedTask.getSourceFilePath());
                    markTaskAsFailed(updatedTask, "源文件不存在: " + updatedTask.getSourceFilePath());
                    return;
                }

                // 标记任务为运行状态
                isTaskRunning = true;

                try {
                    log.info("成功启动任务: ID={}, 文件={}", updatedTask.getId(), updatedTask.getSourceFilePath());
                    // 启动转换任务
                    startConversion(updatedTask);
                } catch (Exception e) {
                    // 如果启动失败，重置状态
                    isTaskRunning = false;
                    log.error("启动任务失败: ID={}", updatedTask.getId(), e);
                    // 将任务状态重置为PENDING，以便重试
                    resetTaskToPending(updatedTask);
                }

            } catch (Exception e) {
                log.error("队列处理异常", e);
            }
        }
    }

    /**
     * 将任务状态重置为PENDING
     */
    private void resetTaskToPending(ConversionTask task) {
        try {
            task.setStatus(ConversionTask.TaskStatus.PENDING);
            task.setStartedAt(null);
            task.setProgress(0);
            taskRepository.save(task);
        } catch (Exception e) {
            log.error("重置任务状态失败: ID={}", task.getId(), e);
        }
    }

    /**
     * 原子性地获取任务进行处理
     */
    @Transactional(timeout = 5)
    protected boolean acquireTaskForProcessing(Long taskId) {
        Optional<ConversionTask> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return false;
        }

        ConversionTask task = taskOpt.get();

        // 只有当任务确实是PENDING状态时才更新
        if (task.getStatus() == ConversionTask.TaskStatus.PENDING) {
            task.setStatus(ConversionTask.TaskStatus.RUNNING);
            task.setStartedAt(LocalDateTime.now());
            task.setProgress(0);
            taskRepository.save(task);
            return true;
        }

        return false;
    }

    /**
     * 检查源文件是否存在
     */
    private boolean isSourceFileExists(String filePath) {
        try {
            return Files.exists(Paths.get(filePath));
        } catch (Exception e) {
            log.warn("检查源文件存在性时发生异常: {}", filePath, e);
            return false;
        }
    }

    /**
     * 标记任务为失败
     */
    private void markTaskAsFailed(ConversionTask task, String errorMessage) {
        try {
            task.setStatus(ConversionTask.TaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
            task.setCompletedAt(LocalDateTime.now());
            taskRepository.save(task);
        } catch (Exception e) {
            log.error("标记任务失败状态时发生异常: ID={}", task.getId(), e);
        }
    }

    /**
     * 启动转换任务
     */
    private void startConversion(ConversionTask task) {
        CompletableFuture<ConversionTask> future = CompletableFuture.supplyAsync(() -> {
            Long taskId = task.getId();
            try {
                log.info("开始转换任务: ID={}, 文件={}", taskId, task.getSourceFilePath());

                // 执行转换（MediaConversionService内部会更新进度）
                ConversionTask result = conversionService.convertMedia(task);
                log.info("转换任务完成: ID={}, 状态={}", taskId, result.getStatus());
                return result;

            } catch (Exception e) {
                log.error("转换任务异常: ID={}", taskId, e);
                
                // 检查任务是否已被取消，如果是，不要将其标记为失败
                Optional<ConversionTask> currentTaskOpt = taskRepository.findById(taskId);
                if (currentTaskOpt.isPresent()) {
                    ConversionTask currentTask = currentTaskOpt.get();
                    if (currentTask.getStatus() == ConversionTask.TaskStatus.CANCELLED) {
                        log.info("任务已被取消，不更新为失败状态: ID={}", taskId);
                        return currentTask;
                    }
                }
                
                // 检查异常消息是否包含"取消"
                if (e.getMessage() != null && e.getMessage().contains("取消")) {
                    log.info("检测到取消异常，将任务标记为已取消: ID={}", taskId);
                    task.setStatus(ConversionTask.TaskStatus.CANCELLED);
                    task.setCompletedAt(LocalDateTime.now());
                    taskRepository.save(task);
                    return task;
                }
                
                // 更新任务状态为失败
                task.setStatus(ConversionTask.TaskStatus.FAILED);
                task.setErrorMessage(e.getMessage());
                task.setCompletedAt(LocalDateTime.now());
                taskRepository.save(task);
                return task;
            } finally {
                runningTasks.remove(taskId);
                isTaskRunning = false;

                // 任务完成后立即尝试处理队列中的下一个任务
                triggerQueueProcessing();
            }
        }, conversionExecutor);

        runningTasks.put(task.getId(), future);
    }

    /**
     * 更新任务进度（供MediaConversionService调用）
     * 使用只读事务和较短的超时时间来避免数据库锁定
     */
    @Transactional(timeout = 5, readOnly = false)
    public void updateTaskProgress(Long taskId, int progress) {
        try {
            Optional<ConversionTask> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isPresent()) {
                ConversionTask task = taskOpt.get();
                // 确保进度在合理范围内
                int validProgress = Math.max(0, Math.min(100, progress));
                if (validProgress != task.getProgress()) {
                    task.setProgress(validProgress);
                    taskRepository.save(task);
                    log.debug("更新任务进度: ID={}, 进度={}%", taskId, validProgress);
                }
            }
        } catch (Exception e) {
            log.error("更新任务进度失败: ID={}", taskId, e);
        }
    }

    /**
     * 获取队列统计信息
     */
    public QueueStats getQueueStats() {
        long pendingCount = taskRepository.countByStatus(ConversionTask.TaskStatus.PENDING);
        long runningCount = taskRepository.countByStatus(ConversionTask.TaskStatus.RUNNING);
        long completedCount = taskRepository.countByStatus(ConversionTask.TaskStatus.COMPLETED);
        long failedCount = taskRepository.countByStatus(ConversionTask.TaskStatus.FAILED);
        long cancelledCount = taskRepository.countByStatus(ConversionTask.TaskStatus.CANCELLED);
        long totalCount = taskRepository.count();

        return new QueueStats(
                (int) pendingCount,
                (int) runningCount,
                (int) completedCount,
                (int) failedCount,
                (int) cancelledCount,
                isTaskRunning ? 1 : 0,
                (int) totalCount
        );
    }

    /**
     * 清理所有转换任务
     */
    @Transactional(timeout = 30)
    public int clearAllTasks() {
        try {
            // 获取所有任务以便清理相关文件
            List<ConversionTask> allTasks = taskRepository.findAll();
            int deletedCount = 0;

            // 1. 取消所有正在运行的任务
            for (ConversionTask task : allTasks) {
                if (task.getStatus() == ConversionTask.TaskStatus.RUNNING) {
                    CompletableFuture<ConversionTask> future = runningTasks.get(task.getId());
                    if (future != null && !future.isDone()) {
                        future.cancel(true);
                        runningTasks.remove(task.getId());
                    }
                }
            }

            // 2. 清空运行任务标志
            isTaskRunning = false;
            runningTasks.clear();

            // 3. 从数据库中删除所有任务
            taskRepository.deleteAll();
            deletedCount = allTasks.size();

            log.info("已成功清理 {} 个转换任务记录", deletedCount);
            return deletedCount;

        } catch (Exception e) {
            log.error("清理所有任务失败", e);
            throw new LogicException("清理任务失败: " + e.getMessage(), e);
        }
    }

    private boolean isSupportedFormat(String format){
        if (StringUtils.isNotBlank(format)) {
            return supportedVideoFormats.contains(format.toLowerCase()) || supportedAudioFormats.contains(format.toLowerCase());
        }
        return false;
    }
}