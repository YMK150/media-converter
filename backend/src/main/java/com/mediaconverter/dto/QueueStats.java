package com.mediaconverter.dto;

import lombok.Data;

/**
 * 队列统计信息类
 */
@Data
public class QueueStats {

    private final int pendingCount;
    private final int runningCount;
    private final int completedCount;
    private final int failedCount;
    private final int cancelledCount;
    private final int activeThreads;
    private final int totalCount;

    public QueueStats(int pendingCount, int runningCount, int completedCount,
                      int failedCount, int cancelledCount, int activeThreads, int totalCount) {
        this.pendingCount = pendingCount;
        this.runningCount = runningCount;
        this.completedCount = completedCount;
        this.failedCount = failedCount;
        this.cancelledCount = cancelledCount;
        this.activeThreads = activeThreads;
        this.totalCount = totalCount;
    }

}