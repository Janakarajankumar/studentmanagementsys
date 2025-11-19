// UserLoginRepository
package com.example.student_management.repository;

import com.example.student_management.model.UserLogin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLoginRepository extends JpaRepository<UserLogin, Long> {}
