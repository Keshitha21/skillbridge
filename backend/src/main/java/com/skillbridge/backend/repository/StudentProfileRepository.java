package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.StudentProfile;
import com.skillbridge.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUserId(Long userId);
    Optional<StudentProfile> findByUser(User user);
    List<StudentProfile> findByVisibility(StudentProfile.Visibility visibility);
}
