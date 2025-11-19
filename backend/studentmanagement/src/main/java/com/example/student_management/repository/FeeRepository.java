// FeeRepository
package com.example.student_management.repository;

import com.example.student_management.model.Fee;
import com.example.student_management.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeRepository extends JpaRepository<Fee, Long> {
    List<Fee> findByStudent(Student student);
}
