package com.mediaconverter.repository;

import com.mediaconverter.entity.ConversionTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversionTaskRepository extends JpaRepository<ConversionTask, Long> {
    @Query("SELECT COUNT(t) FROM ConversionTask t WHERE t.status = :status")
    long countByStatus(@Param("status") ConversionTask.TaskStatus status);

    /**
     * 获取待处理任务（限制数量）
     */
    @Query("SELECT t FROM ConversionTask t WHERE t.status = 'PENDING' ORDER BY t.createdAt ASC")
    List<ConversionTask> findPendingTasksWithLimit(@Param("limit") int limit);



}