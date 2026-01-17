package com.mediaconverter.config;

import com.mediaconverter.exception.LogicException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 队列和异步任务配置
 */
@Configuration
@EnableAsync
@EnableScheduling
public class QueueConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(QueueConfig.class);

    @Value("${media.converter.queue.core-pool-size:2}")
    private int corePoolSize;
    
    @Value("${media.converter.queue.max-pool-size:5}")
    private int maxPoolSize;
    
    @Value("${media.converter.queue.queue-capacity:100}")
    private int queueCapacity;
    
    @Bean(name = "conversionExecutor")
    public Executor conversionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 线程池配置
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("conversion-");
        
        // 拒绝策略：调用者运行
        executor.setRejectedExecutionHandler((r, executor1) -> {
            logger.warn("任务队列已满，拒绝执行任务: {}", r.toString());
            throw new LogicException("任务队列已满，请稍后再试");
        });
        
        // 关闭时等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        logger.info("转换线程池初始化完成: coreSize={}, maxSize={}, queueCapacity={}", 
            corePoolSize, maxPoolSize, queueCapacity);
        
        return executor;
    }
    
    /**
     * 进度更新专用线程池
     */
    @Bean("progressUpdateExecutor")
    public Executor progressUpdateExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ProgressUpdate-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}