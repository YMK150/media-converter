package com.mediaconverter.controller;

import com.mediaconverter.dto.BatchConversionResultDto;
import com.mediaconverter.dto.ConversionRequest;
import com.mediaconverter.dto.QueueStats;
import com.mediaconverter.dto.TaskListDto;
import com.mediaconverter.entity.ConversionTask;
import com.mediaconverter.service.ConversionQueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversion")
@RequiredArgsConstructor
@Validated
public class ConversionController {

    private final ConversionQueueService queueService;

    /**
     * 创建转换任务
     */
    @PostMapping("/task")
    public Long createConversionTask(@Valid @RequestBody ConversionRequest request) {
        return queueService.createConversionTask(request);
    }

    /**
     * 批量创建转换任务
     */
    @PostMapping("/batch")
    public BatchConversionResultDto createBatchConversionTasks(@Valid @RequestBody ConversionRequest request) {
        return queueService.createBatchConversionTasks(request);
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/task/{taskId}")
    public ConversionTask getTaskStatus(@PathVariable Long taskId) {
        return queueService.getTaskStatus(taskId);
    }

    /**
     * 获取所有任务列表（分页查询）
     */
    @GetMapping("/tasks")
    public TaskListDto getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return queueService.getTasks(page, size);
    }

    /**
     * 取消任务
     */
    @PostMapping("/task/{taskId}/cancel")
    public Boolean cancelTask(@PathVariable Long taskId) {
        return queueService.cancelTask(taskId);
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/task/{taskId}")
    public Boolean deleteTask(@PathVariable Long taskId) {
        return queueService.deleteTask(taskId);
    }

    /**
     * 清理所有转换任务
     */
    @DeleteMapping("/tasks/clear")
    public Integer clearAllTasks() {
        return queueService.clearAllTasks();
    }

    /**
     * 获取队列统计信息
     */
    @GetMapping(value = "/queue/stats")
    public QueueStats getQueueStats() {
        return queueService.getQueueStats();
    }

}