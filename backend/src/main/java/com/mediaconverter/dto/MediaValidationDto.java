package com.mediaconverter.dto;

import lombok.Data;

/**
 * 媒体文件验证结果DTO
 */
@Data
public class MediaValidationDto {
    private Boolean isValid;
    private String mediaType;
}