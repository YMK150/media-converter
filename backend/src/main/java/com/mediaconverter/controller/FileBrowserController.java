package com.mediaconverter.controller;

import com.mediaconverter.dto.FileListDto;
import com.mediaconverter.dto.FormatCheckDto;
import com.mediaconverter.dto.MediaValidationDto;
import com.mediaconverter.service.FileBrowserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/filebrowser")
@RequiredArgsConstructor
public class FileBrowserController {

    private final FileBrowserService fileBrowserService;

    @GetMapping("/roots")
    public List<String> getSystemRootDirectories() {
        return fileBrowserService.getSystemRootDirectories();
    }

    @GetMapping("/list")
    public FileListDto listFiles(@RequestParam(required = false, defaultValue = "/") String path) {
        return fileBrowserService.listFiles(path);
    }


    @GetMapping("/formats")
    public FormatCheckDto getSupportedFormats(@RequestParam(required = false) String sourceFormat) {
        return fileBrowserService.getSupportedFormats(sourceFormat);
    }

    @GetMapping("/validate")
    public MediaValidationDto validateMediaFile(@RequestParam String filePath) {
        return fileBrowserService.validateMediaFile(filePath);
    }
}