package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    List<UploadedFile> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<UploadedFile> findByTaskId(Long taskId);
    List<UploadedFile> findByProjectIdIn(List<Long> ids);
}
