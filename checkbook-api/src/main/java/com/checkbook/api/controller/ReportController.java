package com.checkbook.api.controller;

import com.checkbook.api.dto.ApiResponse;
import com.checkbook.api.dto.ErrorResponse;
import com.checkbook.api.dto.SpendingReportResponseDto;
import com.checkbook.api.entity.User;
import com.checkbook.api.service.ReportExportService;
import com.checkbook.api.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportExportService reportExportService;

    @GetMapping("/spending")
    public ResponseEntity<ApiResponse> generateSpendingReport(
            @AuthenticationPrincipal User user,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String accountIds) {

        try {
            // Parse dates
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            // Parse account IDs (comma-separated)
            List<Long> accountIdList = null;
            if (accountIds != null && !accountIds.trim().isEmpty()) {
                accountIdList = Arrays.stream(accountIds.split(","))
                        .map(String::trim)
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
            }

            // Generate report
            SpendingReportResponseDto report = reportService.generateSpendingReport(
                    user, start, end, accountIdList);

            return ResponseEntity.ok(new ApiResponse(true, "Report generated successfully", report));

        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, "Invalid date format. Use YYYY-MM-DD", null));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, "Invalid account ID format", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, e.getMessage(), null));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse(false, e.getMessage(), null));
        }
    }

    @GetMapping("/spending/export/pdf")
    public ResponseEntity<?> exportReportPdf(
            @AuthenticationPrincipal User user,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String accountIds) {

        try {
            // Parse dates
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            // Parse account IDs
            List<Long> accountIdList = null;
            if (accountIds != null && !accountIds.trim().isEmpty()) {
                accountIdList = Arrays.stream(accountIds.split(","))
                        .map(String::trim)
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
            }

            // Generate report data
            SpendingReportResponseDto report = reportService.generateSpendingReport(
                    user, start, end, accountIdList);

            // Generate PDF
            byte[] pdfBytes = reportExportService.generatePdf(report);

            // Generate filename
            String filename = reportExportService.generateFilename(start, end, "pdf");

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, "Invalid date format. Use YYYY-MM-DD", null));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, "Invalid account ID format", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, e.getMessage(), null));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse(false, e.getMessage(), null));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ApiResponse(false, "Failed to generate PDF: " + e.getMessage(), null));
        }
    }

    @GetMapping("/spending/export/csv")
    public ResponseEntity<?> exportReportCsv(
            @AuthenticationPrincipal User user,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String accountIds) {

        try {
            // Parse dates
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            // Parse account IDs
            List<Long> accountIdList = null;
            if (accountIds != null && !accountIds.trim().isEmpty()) {
                accountIdList = Arrays.stream(accountIds.split(","))
                        .map(String::trim)
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
            }

            // Generate report data
            SpendingReportResponseDto report = reportService.generateSpendingReport(
                    user, start, end, accountIdList);

            // Generate CSV
            String csvContent = reportExportService.generateCsv(report, accountIdList, start, end);

            // Generate filename
            String filename = reportExportService.generateFilename(start, end, "csv");

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("text", "csv"));
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvContent);

        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, "Invalid date format. Use YYYY-MM-DD", null));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, "Invalid account ID format", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, e.getMessage(), null));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    new ApiResponse(false, e.getMessage(), null));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ApiResponse(false, "Failed to generate CSV: " + e.getMessage(), null));
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal Server Error", "An unexpected error occurred: " + e.getMessage(), "/reports"));
    }
}