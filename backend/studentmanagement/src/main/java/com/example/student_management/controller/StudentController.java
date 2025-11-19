package com.example.student_management.controller;

import com.example.student_management.model.ExamResult;
import com.example.student_management.model.Fee;
import com.example.student_management.model.Student;
import com.example.student_management.repository.ExamResultRepository;
import com.example.student_management.repository.FeeRepository;
import com.example.student_management.repository.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentController {

    private final StudentRepository studentRepo;
    private final ExamResultRepository examRepo;
    private final FeeRepository feeRepo;

    public StudentController(StudentRepository studentRepo,
                             ExamResultRepository examRepo,
                             FeeRepository feeRepo) {
        this.studentRepo = studentRepo;
        this.examRepo = examRepo;
        this.feeRepo = feeRepo;
    }

    // ---------- BASIC LIST ----------

    @GetMapping
    public List<Student> getAllStudents() {
        return studentRepo.findAll();
    }

    // ---------- CREATE / UPDATE / DELETE STUDENT (ADMIN ONLY) ----------

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Student createStudent(@RequestBody Student body) {
        // id will be auto-generated
        return studentRepo.save(body);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable Long id,
                                                 @RequestBody Student body) {
        return studentRepo.findById(id)
                .map(s -> {
                    s.setName(body.getName());
                    s.setEmail(body.getEmail());
                    // if you have more fields, set them here too
                    return ResponseEntity.ok(studentRepo.save(s));
                })
                .orElse(ResponseEntity.notFound().build());
    }

       @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        return studentRepo.findById(id)
                .map(student -> {
                    // delete child rows first to avoid FK problems
                    List<ExamResult> exams = examRepo.findByStudent(student);
                    List<Fee> fees = feeRepo.findByStudent(student);

                    examRepo.deleteAll(exams);
                    feeRepo.deleteAll(fees);

                    studentRepo.delete(student);
                    return ResponseEntity.noContent().build();   // 204
                })
                .orElse(ResponseEntity.notFound().build());      // 404
    }


    // ---------- EXAMS / FEES BY STUDENT ----------

    @GetMapping("/{id}/exams")
    public ResponseEntity<List<ExamResult>> getExams(@PathVariable Long id) {
        return studentRepo.findById(id)
                .map(student -> ResponseEntity.ok(examRepo.findByStudent(student)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/fees")
    public ResponseEntity<List<Fee>> getFees(@PathVariable Long id) {
        return studentRepo.findById(id)
                .map(student -> ResponseEntity.ok(feeRepo.findByStudent(student)))
                .orElse(ResponseEntity.notFound().build());
    }
}
