package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByStatusOrderByCreatedAtDesc(Report.Status status);

    List<Report> findAllByOrderByCreatedAtDesc();

    List<Report> findByReporterId(Long reporterId);
}
