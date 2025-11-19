// src/main/java/com/example/student_management/controller/AdminController.java
package com.example.student_management.controller;

import com.example.student_management.model.UserLogin;
import com.example.student_management.repository.UserLoginRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserLoginRepository loginRepo;

    public AdminController(UserLoginRepository loginRepo) {
        this.loginRepo = loginRepo;
    }

    // only ADMIN should be able to see this
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/logins")
    public List<UserLogin> getAllLogins() {
        return loginRepo.findAll();
    }
}
