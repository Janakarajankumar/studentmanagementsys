// src/main/java/com/example/student_management/StudentManagementApplication.java
package com.example.student_management;

import com.example.student_management.model.AppUser;
import com.example.student_management.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class StudentManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentManagementApplication.class, args);
    }

    @Bean
    public CommandLineRunner initAdminUser(AppUserRepository userRepo,
                                           PasswordEncoder passwordEncoder) {
        return args -> {
            AppUser admin = userRepo.findByUsername("admin")
                    .orElseGet(AppUser::new);

            admin.setUsername("admin");
            admin.setName("Administrator");                // needs get/setName
            admin.setEmail("admin@example.com");           // needs get/setEmail
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");                        // NOT ROLE_ADMIN
            admin.setEnabled(true);

            userRepo.save(admin);
            System.out.println("✅ Default admin user ensured: admin / admin123");
        };
    }
}
