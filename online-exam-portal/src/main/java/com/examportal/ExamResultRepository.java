
package com.examportal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamResultRepository
        extends JpaRepository<ExamResult, Long> {

    List<ExamResult> findByUsernameOrderBySubmittedAtDesc(
            String username);
}
