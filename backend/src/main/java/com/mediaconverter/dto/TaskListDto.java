package com.mediaconverter.dto;

import com.mediaconverter.entity.ConversionTask;
import lombok.Data;

import java.util.List;

/**
 * 任务列表响应DTO
 */
@Data
public class TaskListDto {
    private List<ConversionTask> tasks;
    private Long total;
}