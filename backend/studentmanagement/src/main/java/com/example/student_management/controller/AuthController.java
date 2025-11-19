package com.example.student_management.controller;

import com.example.student_management.model.AppUser;
import com.example.student_management.model.UserLogin;
import com.example.student_management.repository.AppUserRepository;
import com.example.student_management.repository.UserLoginRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AppUserRepository userRepo;
    private final UserLoginRepository loginRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AppUserRepository userRepo,
                          UserLoginRepository loginRepo,
                          PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.loginRepo = loginRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // LOGIN: username OR email + password
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String loginId = body.get("loginId");
        String password = body.get("password");

        if (loginId == null || password == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "MISSING_FIELDS"));
        }

        Optional<AppUser> optUser = userRepo.findByUsername(loginId);
        if (optUser.isEmpty()) {
            optUser = userRepo.findByEmail(loginId);
        }

        if (optUser.isEmpty()) {
            // no such user
            return ResponseEntity.status(404)
                    .body(Map.of("error", "NO_USER"));
        }

        AppUser user = optUser.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            // wrong password
            return ResponseEntity.status(401)
                    .body(Map.of("error", "WRONG_PASSWORD"));
        }

        // correct login → store login info
        loginRepo.save(new UserLogin(user.getUsername(), LocalDateTime.now()));

        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),   // canonical username
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole()            // ADMIN or USER
        ));
    }

    // SIGNUP: name + email + password
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");

        if (name == null || email == null || password == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "MISSING_FIELDS"));
        }

        if (userRepo.findByEmail(email).isPresent()) {
            return ResponseEntity.status(409)
                    .body(Map.of("error", "EMAIL_EXISTS"));
        }

        AppUser user = new AppUser();
        user.setName(name);
        user.setEmail(email);
        // use email as username for login (except admin which we created)
        user.setUsername(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setEnabled(true);

        userRepo.save(user);
        return ResponseEntity.ok().build();
    }
}
