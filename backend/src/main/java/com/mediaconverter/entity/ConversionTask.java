package com.mediaconverter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "conversion_tasks")
public class ConversionTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String sourceFilePath;
    
    @Column(nullable = false)
    private String targetFileName;
    
    @Column(nullable = false)
    private String targetFormat;
    
    @Column(nullable = false)
    private String targetDirectory;
    
    @Column
    private String outputFilePath;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime startedAt;
    
    @Column
    private LocalDateTime completedAt;
    
    @Column
    private String errorMessage;
    
    @Column
    private Integer progress; // 0-100
    
    // 转换参数
    @Column
    private Integer videoWidth;
    
    @Column
    private Integer videoHeight;
    
    @Column
    private Integer audioBitrate;
    
    @Column
    private Integer videoBitrate;
    
    @Column
    private Double frameRate;
    
    @Column
    private Long fileSize;
    
    @Column
    private Long outputFileSize;
    
    public ConversionTask() {
        this.createdAt = LocalDateTime.now();
        this.status = TaskStatus.PENDING;
        this.progress = 0;
    }
    
    public enum TaskStatus {
        PENDING,    // 等待中
        RUNNING,    // 转换中
        COMPLETED,  // 已完成
        FAILED,     // 失败
        CANCELLED   // 已取消
    }
}