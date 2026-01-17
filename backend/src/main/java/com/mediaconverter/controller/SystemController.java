package com.mediaconverter.controller;

import com.mediaconverter.util.BannerUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * 系统信息控制器
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Value("${logging.file.path}")
    private String logsDir;

    @GetMapping("/banner")
    public ResponseEntity<String> getBanner() {
        return ResponseEntity.ok(BannerUtil.generateBanner());
    }

    /**
     * 下载日志文件（打包成zip格式）
     */
    @GetMapping("/logs/download")
    public ResponseEntity<Resource> downloadLogs() {
        try {
            // 检查日志目录是否存在
            File logsDir = new File(this.logsDir);
            if (!logsDir.exists() || !logsDir.isDirectory()) {
                return ResponseEntity.notFound().build();
            }

            // 创建临时目录
            File tempDir = new File(this.logsDir + File.separator + "temp");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            // 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String zipFileName = "logs_" + timestamp + ".zip";
            File zipFile = new File(tempDir, zipFileName);

            // 获取所有日志文件
            List<File> logFiles = new ArrayList<>();
            File[] files = logsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        logFiles.add(file);
                    }
                }
            }

            // 如果没有日志文件
            if (logFiles.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // 打包成zip
            createZipArchive(logFiles, zipFile);

            // 创建响应资源
            Resource resource = new FileSystemResource(zipFile);


            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", zipFileName);
            headers.setContentLength(zipFile.length());

            ResponseEntity<Resource> body = ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
            return body;

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 创建zip压缩包
     */
    private void createZipArchive(List<File> files, File outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File file : files) {
                // 创建zip条目
                ZipEntry entry = new ZipEntry(file.getName());
                zos.putNextEntry(entry);

                // 写入文件内容
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                }

                zos.closeEntry();
            }
        }
    }



}
