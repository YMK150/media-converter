package com.mediaconverter.dto;

import lombok.Data;

/**
 * 批量转换结果DTO
 */
@Data
public class BatchConversionResultDto {
    
    private Integer total;
    private Integer successCount;
    private Integer failCount;
    private String message;

}