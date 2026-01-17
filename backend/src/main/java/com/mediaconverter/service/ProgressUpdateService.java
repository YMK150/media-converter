package com.mediaconverter.service;

import com.mediaconverter.entity.ConversionTask;
import com.mediaconverter.repository.ConversionTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

/**
 * 进度更新服务，专门处理异步进度更新
 */
@Slf4j
@Service
public class ProgressUpdateService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ConversionTaskRepository taskRepository;

    @Autowired
    public ProgressUpdateService(SimpMessagingTemplate messagingTemplate,
                                ConversionTaskRepository taskRepository) {
        this.messagingTemplate = messagingTemplate;
        this.taskRepository = taskRepository;
    }

    /**
     * 异步更新任务进度
     */
    @Async("progressUpdateExecutor")
    public CompletableFuture<Void> updateProgressAsync(Long taskId, int progress, ConversionTask.TaskStatus status) {
        try {
            updateProgressInternal(taskId, progress, status);
        } catch (Exception e) {
            log.warn("异步更新进度失败: taskId={}, progress={}, error={}", taskId, progress, e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 内部进度更新方法
     */
    private void updateProgressInternal(Long taskId, int progress, ConversionTask.TaskStatus status) {
        try {
            // 确保进度在有效范围内
            int validProgress = Math.max(0, Math.min(progress, 100));
            
            // 发送WebSocket通知（优先）
            ConversionTask progressUpdate = new ConversionTask();
            progressUpdate.setId(taskId);
            progressUpdate.setProgress(validProgress);
            progressUpdate.setStatus(status);
            
            messagingTemplate.convertAndSend("/topic/conversion-progress", progressUpdate);
            
            // 使用带事务的方法更新数据库
            updateProgressInTransaction(taskId, validProgress);
            
            log.debug("任务 {} 进度更新到: {}%", taskId, validProgress);
            
        } catch (Exception e) {
            log.error("进度更新过程中发生错误: taskId={}, progress={}", taskId, progress, e);
            throw e;
        }
    }

    /**
     * 在事务中更新数据库进度（使用findById+save方式，兼容SQLite）
     * 使用较短的超时时间避免长时间锁定数据库
     */
    @Transactional(timeout = 3)
    public void updateProgressInTransaction(Long taskId, int progress) {
        try {
            // 使用findById+save的方式，更兼容SQLite
            taskRepository.findById(taskId).ifPresent(task -> {
                task.setProgress(progress);
                taskRepository.save(task);
            });
        } catch (Exception e) {
            log.warn("数据库进度更新失败: taskId={}, error={}", taskId, e.getMessage());
            // 不重新抛出异常，避免影响WebSocket通知
        }
    }

    /**
     * 简单的同步进度更新（用于关键节点）
     */
    public void updateProgressSync(Long taskId, int progress, ConversionTask.TaskStatus status) {
        try {
            updateProgressInternal(taskId, progress, status);
        } catch (Exception e) {
            log.warn("同步更新进度失败: taskId={}, progress={}, error={}", taskId, progress, e.getMessage());
        }
    }
}