package com.mediaconverter.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ConversionRequest {
    @NotEmpty(message = "源文件不能为空")
    private List<String> sourceFiles;

    @NotNull(message = "目标格式不能为空")
    private String targetFormat;

    @NotNull(message = "目标目录不能为空")
    private String targetDirectory;

    private Integer videoWidth;
    private Integer videoHeight;
    private Integer audioBitrate;
    private Integer videoBitrate;
    private Double frameRate;

}