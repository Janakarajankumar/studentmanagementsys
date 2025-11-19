// src/main/java/com/example/student_management/service/AppUserDetailsService.java
package com.example.student_management.service;

import com.example.student_management.model.AppUser;
import com.example.student_management.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepo;

    public AppUserDetailsService(AppUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Find user by username (admin or email-based username)
        AppUser user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Spring adds "ROLE_" automatically when you call roles(...)
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())          // "ADMIN" or "USER"
                .disabled(!user.isEnabled())
                .build();
    }
}
