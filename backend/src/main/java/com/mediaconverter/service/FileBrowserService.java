package com.mediaconverter.service;

import com.mediaconverter.dto.FileItemDto;
import com.mediaconverter.dto.FileListDto;
import com.mediaconverter.dto.FormatCheckDto;
import com.mediaconverter.dto.MediaValidationDto;
import com.mediaconverter.exception.LogicException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileBrowserService {

    @Value("${media.converter.supported-audio-formats}")
    private List<String> supportedAudioFormats;

    @Value("${media.converter.supported-video-formats}")
    private List<String> supportedVideoFormats;

    @Value("${file.browser.base-path}")
    private String roots;

    public FileListDto listFiles(String directoryPath) {
        FileListDto dto = new  FileListDto();
        dto.setCurrentPath(directoryPath);
        // 安全检查：防止路径遍历攻击
        // 检查路径遍历攻击
        if (directoryPath.contains("..")) {
            throw new LogicException("无效的路径：路径包含遍历字符");
        }
        // 在Linux系统中，~是用户主目录的有效引用，所以只对Windows系统检查
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win") && directoryPath.contains("~")) {
            throw new LogicException("无效的路径");
        }

        // 额外的安全检查：防止绝对路径攻击（仅在非Windows系统）
        if (!os.contains("win") && (directoryPath.contains("/etc/") || directoryPath.contains("/bin/") || directoryPath.contains("/sbin/") || directoryPath.contains("/boot/"))) {
            // 记录警告但不阻止访问，这些目录在某些情况下可能是合法的
            log.warn("尝试访问系统目录: {}", directoryPath);
        }
        if (!checkDir(directoryPath)) {
            dto.setFiles(getAuthDirectories());
            return dto;
        }

        log.debug("浏览目录: {}", directoryPath);

        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            throw new LogicException("目录不存在或不是有效目录: " + directoryPath);
        }

        List<FileItemDto> fileItems = new ArrayList<>();

        // 添加上级目录选项（如果不是根目录）
        File parent = directory.getParentFile();
        if (parent != null) {
            FileItemDto parentItem = new FileItemDto("..", parent.getAbsolutePath(), true);
            parentItem.setMediaType("directory");
            fileItems.add(parentItem);
        }

        File[] files = directory.listFiles();
        log.debug("目录 {} 下找到 {} 个文件/目录", directoryPath, files != null ? files.length : 0);

        if (files != null) {
            for (File file : files) {
                try {
                    // 过滤隐藏文件和文件夹
                    if (isHiddenFile(file)) {
                        log.debug("跳过隐藏文件/目录: {}", file.getName());
                        continue;
                    }

                    FileItemDto fileItem = createFileItem(file);
                    fileItems.add(fileItem);
                } catch (Exception e) {
                    log.warn("无法处理文件: {}", file.getAbsolutePath(), e);
                }
            }
        } else {
            log.warn("无法列出目录内容，可能是权限问题: {}", directoryPath);
        }

        // 排序：目录在前，然后按名称排序
        fileItems.sort((a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) {
                return -1;
            }
            if (!a.isDirectory() && b.isDirectory()) {
                return 1;
            }
            return a.getName().compareToIgnoreCase(b.getName());
        });
        dto.setFiles(fileItems);
        return dto;
    }

    public List<String> getSystemRootDirectories() {
        List<String> roots = new ArrayList<>();

        // Windows系统
        File[] rootsList = File.listRoots();
        if (rootsList != null) {
            for (File root : rootsList) {
                roots.add(root.getAbsolutePath());
            }
        }

        return roots;
    }

    private boolean checkDir(String path) {
        if (CollectionUtils.isEmpty(getRootDirectories())) {
            return false;
        }
        for (String s : getRootDirectories()) {
            if (path.startsWith(s)) {
                return true;
            }
        }
        return false;
    }

    public List<FileItemDto> getAuthDirectories() {
        List<FileItemDto> fileItems = new ArrayList<>();
        for (String s : getRootDirectories()) {
            FileItemDto dto = new FileItemDto();
            dto.setName(s);
            dto.setDirectory(true);
            dto.setPath(s);
            dto.setMediaFile(false);
            fileItems.add(dto);
        }
        return fileItems;
    }

    private List<String> getRootDirectories() {
        String[] split = roots.split(":");
        log.debug("有权限访问的目录：{}", roots);
        if (StringUtils.isAllBlank(split)) {
            throw new LogicException("未找到可以读取的目录，请先授权应用访问目录");
        }
        return List.of(split);
    }


    public boolean createDirectory(String parentPath, String directoryName) throws IOException {
        Path newDirPath = Paths.get(parentPath, directoryName);

        if (Files.exists(newDirPath)) {
            throw new LogicException("目录已存在: " + newDirPath);
        }

        Files.createDirectories(newDirPath);
        log.info("创建目录成功: {}", newDirPath);
        return true;
    }
    
    public String getMediaType(String filePath) {
        String extension = FilenameUtils.getExtension(filePath).toLowerCase();
        if (supportedAudioFormats.contains(extension)) {
            return "audio";
        } else if (supportedVideoFormats.contains(extension)) {
            return "video";
        }
        return "unknown";
    }

    private boolean isHiddenFile(File file) {
        // 检查文件是否为隐藏
        if (file.isHidden()) {
            return true;
        }

        // 检查文件名是否以点开头（Unix/Linux隐藏文件）
        String fileName = file.getName();
        if (fileName.startsWith(".")) {
            return true;
        }

        return false;
    }

    private FileItemDto createFileItem(File file) throws IOException {
        FileItemDto fileItem = new FileItemDto();
        fileItem.setName(file.getName());
        fileItem.setPath(file.getAbsolutePath());

        boolean isDir = file.isDirectory();
        fileItem.setDirectory(isDir);

        log.debug("处理文件: {} - 路径: {} - 是否为目录: {}", file.getName(), file.getAbsolutePath(), isDir);

        if (!isDir) {
            fileItem.setSize(file.length());
            fileItem.setExtension(FilenameUtils.getExtension(file.getName()));

            // 判断是否为媒体文件
            if (isValidMediaFile(file.getAbsolutePath())) {
                fileItem.setMediaFile(true);
                fileItem.setMediaType(getMediaType(file.getAbsolutePath()));
            } else {
                fileItem.setMediaFile(false);
            }

            // 获取文件修改时间
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            LocalDateTime lastModified = LocalDateTime.ofInstant(
                    attrs.lastModifiedTime().toInstant(),
                    ZoneId.systemDefault()
            );
            fileItem.setLastModified(lastModified);

            // 简单的MIME类型判断
            fileItem.setMimeType(Files.probeContentType(file.toPath()));
        }

        return fileItem;
    }

    private boolean isValidMediaFile(String filePath) {
        String extension = FilenameUtils.getExtension(filePath).toLowerCase();
        return supportedAudioFormats.contains(extension) || supportedVideoFormats.contains(extension);
    }


    public List<String> getSupportedTargetFormats(String sourceFormat) {
        List<String> formats = new ArrayList<>();

        String sourceType = getMediaType("dummy." + sourceFormat);

        if ("audio".equals(sourceType)) {
            // 音频可以转换为其他音频格式
            formats.addAll(supportedAudioFormats);
        } else if ("video".equals(sourceType)) {
            // 视频可以转换为其他视频格式和音频格式
            formats.addAll(supportedVideoFormats);
            formats.addAll(supportedAudioFormats);
        }

        // 移除源格式
        formats.remove(sourceFormat.toLowerCase());

        return formats.stream().distinct().collect(Collectors.toList());
    }

    public FormatCheckDto getSupportedFormats(String sourceFormat) {
        FormatCheckDto dto = new FormatCheckDto();
        dto.setSourceFormat(sourceFormat);
        if (sourceFormat != null && !sourceFormat.isEmpty()) {
            // 返回特定源格式支持的目标格式
            List<String> targetFormats = getSupportedTargetFormats(sourceFormat);
            dto.setTargetFormats(targetFormats);
        } else {
            // 返回所有支持的格式
            dto.setAudioFormats(supportedAudioFormats);
            dto.setVideoFormats(supportedVideoFormats);
        }
        return dto;
    }

    public MediaValidationDto validateMediaFile(String filePath) {
        // 安全检查：防止路径遍历攻击
        if (filePath.contains("..")) {
            throw new LogicException("无效的路径：路径包含遍历字符");
        }
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win") && filePath.contains("~")) {
            throw new LogicException("无效的路径");
        }
        MediaValidationDto dto = new MediaValidationDto();
        dto.setMediaType(getMediaType(filePath));
        dto.setIsValid(isValidMediaFile(filePath));
        return dto;
    }
}