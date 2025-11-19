package com.example.student_management.controller;

import com.example.student_management.dto.StudentFullDto;
import com.example.student_management.model.ExamResult;
import com.example.student_management.model.Fee;
import com.example.student_management.model.Student;
import com.example.student_management.repository.ExamResultRepository;
import com.example.student_management.repository.FeeRepository;
import com.example.student_management.repository.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    // ----------------------------------------------------------------
    // BASIC STUDENT CRUD (used by dashboard list)
    // ----------------------------------------------------------------

    @GetMapping
    public List<Student> getAllStudents() {
        return studentRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudent(@PathVariable Long id) {
        return studentRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Student> createStudent(@RequestBody Student body) {
        Student s = new Student();
        s.setName(body.getName());
        s.setEmail(body.getEmail());
        // if your Student has extra fields, you can set them here
        Student saved = studentRepo.save(s);
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable Long id,
                                                 @RequestBody Student body) {
        Optional<Student> opt = studentRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Student s = opt.get();
        s.setName(body.getName());
        s.setEmail(body.getEmail());
        Student saved = studentRepo.save(s);
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        Optional<Student> opt = studentRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Student s = opt.get();
        // delete related exams and fees first
        examRepo.deleteAll(examRepo.findByStudent(s));
        feeRepo.deleteAll(feeRepo.findByStudent(s));
        studentRepo.delete(s);
        return ResponseEntity.noContent().build();
    }

    // ----------------------------------------------------------------
    // SIMPLE EXAMS & FEES (for "View" buttons on dashboard)
    // ----------------------------------------------------------------

    @GetMapping("/{id}/exams")
    public ResponseEntity<List<ExamResult>> getExamsForStudent(@PathVariable Long id) {
        Optional<Student> opt = studentRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Student s = opt.get();
        return ResponseEntity.ok(examRepo.findByStudent(s));
    }

    @GetMapping("/{id}/fees")
    public ResponseEntity<List<Fee>> getFeesForStudent(@PathVariable Long id) {
        Optional<Student> opt = studentRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Student s = opt.get();
        return ResponseEntity.ok(feeRepo.findByStudent(s));
    }

    // ----------------------------------------------------------------
    // FULL STUDENT (student + exams + fees) for student_form.html
    // ----------------------------------------------------------------

    // map entity -> DTO, using ONLY methods we know exist
    private StudentFullDto toDto(Student s) {
        StudentFullDto dto = new StudentFullDto();
        dto.id = s.getId();
        dto.name = s.getName();
        dto.email = s.getEmail();
        // dto.rollNo, dto.department, dto.dob are ignored for now

        dto.exams = examRepo.findByStudent(s).stream().map(e -> {
            StudentFullDto.ExamDto ex = new StudentFullDto.ExamDto();
            ex.id = e.getId();
            // we don't call getExamName / getSemester because they may not exist
            ex.subject = e.getSubject();
            ex.marksObtained = e.getMarksObtained();
            ex.maxMarks = e.getMaxMarks();
            ex.examDate = e.getExamDate();
            return ex;
        }).collect(Collectors.toList());

        dto.fees = feeRepo.findByStudent(s).stream().map(f -> {
            StudentFullDto.FeeDto fd = new StudentFullDto.FeeDto();
            fd.id = f.getId();
            fd.term = f.getTerm();
            if (f.getAmount() != null) {
                fd.amount = f.getAmount().doubleValue();
            }
            fd.dueDate = f.getDueDate();
            fd.paid = f.isPaid();
            return fd;
        }).collect(Collectors.toList());

        return dto;
    }

    private void applyDtoToStudent(StudentFullDto dto, Student s) {
        s.setName(dto.name);
        s.setEmail(dto.email);
        // for now we ignore dto.rollNo / department / dob because Student has no such methods
    }

    // read full
    @GetMapping("/{id}/full")
    public ResponseEntity<StudentFullDto> getFull(@PathVariable Long id) {
        Optional<Student> opt = studentRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDto(opt.get()));
    }

    // create full
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/full")
    public ResponseEntity<StudentFullDto> createFull(@RequestBody StudentFullDto dto) {
        Student s = new Student();
        applyDtoToStudent(dto, s);
        s = studentRepo.save(s);

        // exams
        if (dto.exams != null) {
            for (StudentFullDto.ExamDto ex : dto.exams) {
                ExamResult er = new ExamResult();
                er.setStudent(s);
                er.setSubject(ex.subject);
                er.setMarksObtained(ex.marksObtained);
                er.setMaxMarks(ex.maxMarks);
                er.setExamDate(ex.examDate);
                examRepo.save(er);
            }
        }

        // fees
        if (dto.fees != null) {
            for (StudentFullDto.FeeDto fd : dto.fees) {
                Fee f = new Fee();
                f.setStudent(s);
                f.setTerm(fd.term);
                f.setAmount(fd.amount != null ? BigDecimal.valueOf(fd.amount) : BigDecimal.ZERO);
                f.setDueDate(fd.dueDate);
                f.setPaid(fd.paid != null && fd.paid);
                feeRepo.save(f);
            }
        }

        return ResponseEntity.ok(toDto(s));
    }

    // update full
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/full")
    public ResponseEntity<StudentFullDto> updateFull(@PathVariable Long id,
                                                     @RequestBody StudentFullDto dto) {
        Optional<Student> opt = studentRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Student s = opt.get();
        applyDtoToStudent(dto, s);
        s = studentRepo.save(s);

        // replace exams
        List<ExamResult> oldExams = examRepo.findByStudent(s);
        examRepo.deleteAll(oldExams);
        if (dto.exams != null) {
            for (StudentFullDto.ExamDto ex : dto.exams) {
                ExamResult er = new ExamResult();
                er.setStudent(s);
                er.setSubject(ex.subject);
                er.setMarksObtained(ex.marksObtained);
                er.setMaxMarks(ex.maxMarks);
                er.setExamDate(ex.examDate);
                examRepo.save(er);
            }
        }

        // replace fees
        List<Fee> oldFees = feeRepo.findByStudent(s);
        feeRepo.deleteAll(oldFees);
        if (dto.fees != null) {
            for (StudentFullDto.FeeDto fd : dto.fees) {
                Fee f = new Fee();
                f.setStudent(s);
                f.setTerm(fd.term);
                f.setAmount(fd.amount != null ? BigDecimal.valueOf(fd.amount) : BigDecimal.ZERO);
                f.setDueDate(fd.dueDate);
                f.setPaid(fd.paid != null && fd.paid);
                feeRepo.save(f);
            }
        }

        return ResponseEntity.ok(toDto(s));
    }
}
