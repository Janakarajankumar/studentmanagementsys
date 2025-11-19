package com.example.student_management.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class StudentFullDto {

    public Long id;
    public String name;
    public String rollNo;
    public String department;
    public LocalDate dob;
    public String email;

    public List<ExamDto> exams;
    public List<FeeDto> fees;

    public static class ExamDto {
        public Long id;
        public String semester;
        public String examName;
        public String subject;
        public Integer marksObtained;
        public Integer maxMarks;
        public LocalDate examDate;
    }

    public static class FeeDto {
        public Long id;
        public String term;
        public Double amount;
        public LocalDate dueDate;
        public Boolean paid;
        public LocalDateTime paidDate;
    }
}
