package com.inventory.controller;

import com.inventory.dto.request.AttendanceDeleteRequestDto;
import com.inventory.dto.request.AttendancePdfRequestDto;
import com.inventory.dto.request.AttendanceRequestDto;
import com.inventory.dto.request.AttendanceSearchRequestDto;
import com.inventory.dto.request.PayrollSummaryRequestDto;
import com.inventory.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;
    
    @PostMapping("/create")
    public ResponseEntity<?> saveAttendance(@RequestBody AttendanceRequestDto request) {
        return ResponseEntity.ok(attendanceService.saveAttendance(request));
    }
    
    @PostMapping("/search")
    public ResponseEntity<?> getAttendanceByEmployee(@RequestBody AttendanceSearchRequestDto request) {
        return ResponseEntity.ok(attendanceService.getAttendanceByEmployee(request));
    }
    
    @PostMapping("/delete")
    public ResponseEntity<?> deleteAttendances(@RequestBody AttendanceDeleteRequestDto request) {
        return ResponseEntity.ok(attendanceService.deleteAttendances(request));
    }
    
    @PostMapping("/pdf")
    public ResponseEntity<byte[]> generateAttendancePdf(@RequestBody AttendancePdfRequestDto request) {
        byte[] pdfBytes = attendanceService.generateAttendancePdf(request);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
            .filename("attendance_.pdf")
            .build());
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
    
    @PostMapping("/payroll-summary-pdf")
    public ResponseEntity<byte[]> generatePayrollSummaryPdf(@RequestBody PayrollSummaryRequestDto request) {
        byte[] pdfBytes = attendanceService.generatePayrollSummaryPdf(request);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
            .filename("payroll_summary.pdf")
            .build());
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
} 