// ExamResultRepository
package com.example.student_management.repository;

import com.example.student_management.model.ExamResult;
import com.example.student_management.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    List<ExamResult> findByStudent(Student student);
}
