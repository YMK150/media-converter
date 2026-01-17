package com.mediaconverter.dto;

import lombok.Data;

import java.util.List;

/**
 * 格式检查结果DTO
 */
@Data
public class FormatCheckDto {
    private String sourceFormat;
    private List<String> targetFormats;
    private List<String> audioFormats;
    private List<String> videoFormats;
}