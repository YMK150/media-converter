package com.mediaconverter.dto;

import lombok.Data;

import java.util.List;

/**
 * 文件列表响应DTO
 */
@Data
public class FileListDto {
    private String currentPath;
    private List<FileItemDto> files;
}