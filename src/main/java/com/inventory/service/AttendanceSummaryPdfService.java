package com.inventory.service;

import com.inventory.dao.EmployeeWithdrawDao;
import com.inventory.exception.ValidationException;
import com.inventory.model.PdfGenerationStatus;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendancePdfService {
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(20, 88, 129);    // #145881
    private static final DeviceRgb SECONDARY_COLOR = new DeviceRgb(23, 163, 223); // #17a3df
    private static final DeviceRgb BACKGROUND = new DeviceRgb(245, 249, 252);     // #f5f9fc
    private static final DeviceRgb ACCENT1 = new DeviceRgb(79, 195, 247);        // #4fc3f7
    private static final DeviceRgb ACCENT2 = new DeviceRgb(29, 182, 246);        // #29b6f6
    private static final DeviceRgb TEXT_PRIMARY = new DeviceRgb(44, 62, 80);     // #2c3e50
    private static final DeviceRgb TEXT_SECONDARY = new DeviceRgb(96, 125, 139);  // #607d8b
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(IST);
    
    // Cache for managing concurrent PDF generations with request tracking
    private final ConcurrentHashMap<String, PdfGenerationStatus> processingRequests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // Dependencies
    private final EmployeeWithdrawDao employeeWithdrawDao;

    @PostConstruct
    public void init() {
        // Schedule cleanup of stale requests every 15 minutes
        cleanupExecutor.scheduleAtFixedRate(this::cleanupStaleRequests, 15, 15, TimeUnit.MINUTES);
    }

    public byte[] generatePdf(Map<String, Object> employeeData, List<Map<String, Object>> attendanceRecords,
    List<Map<String, Object>> withdrawRecords, LocalDate startDate, LocalDate endDate) {
        String requestId = generateRequestId(employeeData);

        PdfGenerationStatus status = processingRequests.get(requestId);
        if (status != null && !status.isStale()) {
            throw new ValidationException("PDF generation already in progress for this request");
        }

        processingRequests.put(requestId, new PdfGenerationStatus());
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PdfDocument pdf = new PdfDocument(new PdfWriter(baos))) {
            
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(36, 36, 36, 36);
            
            addHeader(document, startDate, endDate);
            addEmployeeDetails(document, employeeData);
            addAttendanceTable(document, attendanceRecords);
            addWithdrawTable(document, withdrawRecords);
            addSummary(document, calculateSummary(attendanceRecords, withdrawRecords));
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new ValidationException("Failed to generate PDF: " + e.getMessage());
        } finally {
            processingRequests.remove(requestId);
        }
    }
    
    private void addHeader(Document document, LocalDate startDate, LocalDate endDate) {
        Table header = new Table(2).useAllAvailableWidth();
        
        Cell titleCell = new Cell()
            .add(new Paragraph("Salary Report")
                .setFontSize(28)
                .setFontColor(PRIMARY_COLOR)
                .setBold())
            .add(new Paragraph("Monthly Attendance & Salary Details")
                .setFontSize(14)
                .setFontColor(TEXT_SECONDARY))
            .setBorder(Border.NO_BORDER);
        
        Cell dateCell = new Cell()
            .add(new Paragraph("Period: " + DATE_FORMATTER.format(startDate) + 
                 " to " + DATE_FORMATTER.format(endDate))
                .setFontColor(SECONDARY_COLOR)
                .setFontSize(12))
            .add(new Paragraph("Generated on: " + DATE_FORMATTER.format(LocalDate.now()))
                .setFontColor(TEXT_SECONDARY)
                .setFontSize(10))
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.RIGHT);
        
        header.addCell(titleCell);
        header.addCell(dateCell);
        
        // Add decorative line
        Table line = new Table(1).useAllAvailableWidth().setMarginTop(10);
        line.addCell(new Cell()
            .setHeight(2f)
            .setBackgroundColor(ACCENT1)
            .setBorder(Border.NO_BORDER));
        
        document.add(header);
        document.add(line);
    }
    
    private void addEmployeeDetails(Document document, Map<String, Object> employeeData) {
        Table details = new Table(2).useAllAvailableWidth().setMarginTop(20);

        if (employeeData.get("name") != null) {
            addDetailRow(details, "Name", employeeData.get("name").toString());
        }

        Object regularHours = employeeData.get("regularHours");
        if (regularHours != null) {
            addDetailRow(details, "Regular Hours", formatNumber(new BigDecimal(regularHours.toString())));
        }

        Object regularPay = employeeData.get("regularPay");
        if (regularPay != null) {
            addDetailRow(details, "Regular Pay", formatCurrency(new BigDecimal(regularPay.toString())));
        }

        Object overtimePay = employeeData.get("overtimePay");
        if (overtimePay != null) {
            addDetailRow(details, "Overtime Pay", formatCurrency(new BigDecimal(overtimePay.toString())));
        }

        if (employeeData.get("mobileNumber") != null) {
            addDetailRow(details, "Mobile", employeeData.get("mobileNumber").toString());
        }
        
        document.add(details);
    }
    
    private void addAttendanceTable(Document document, List<Map<String, Object>> records) {
        Table table = new Table(new float[]{1, 4, 2, 2, 1.2f, 1.2f, 2})
            .useAllAvailableWidth()
            .setMarginTop(20);

        addTableHeader(table, new String[]{
            "No", "Date", "Start Time", "End Time", 
            "Regular Hours", "Overtime Hours", "Total Pay"
        });

        int rowNum = 1;
        for (Map<String, Object> record : records) {
            addTableRow(table, rowNum++, record);
        }
        
        document.add(table);
    }
    
    private void addWithdrawTable(Document document, List<Map<String, Object>> withdrawRecords) {
        if (withdrawRecords.isEmpty()) {
            return; // Don't add withdraw section if no records
        }
        
        document.add(new Paragraph("\n"));
        
        Table table = new Table(new float[]{1, 3, 2, 3})
            .useAllAvailableWidth()
            .setMarginTop(20);

        addWithdrawTableHeader(table, new String[]{
            "No", "Withdraw Date", "Amount", "Remarks"
        });

        int rowNum = 1;
        for (Map<String, Object> record : withdrawRecords) {
            addWithdrawTableRow(table, rowNum++, record);
        }
        
        document.add(table);
    }
    
    private void cleanupStaleRequests() {
        processingRequests.entrySet().removeIf(entry -> entry.getValue().isStale());
    }
    
    private String generateRequestId(Map<String, Object> employeeData) {
        return employeeData.get("id") + "_" + System.currentTimeMillis();
    }
    
    private void addDetailRow(Table table, String label, String value) {
        Cell labelCell = new Cell()
            .add(new Paragraph(label))
            .setFontColor(TEXT_PRIMARY)
            .setBold()
            .setBorder(Border.NO_BORDER);
        
        Cell valueCell = new Cell()
            .add(new Paragraph(value))
            .setFontColor(TEXT_PRIMARY)
            .setBorder(Border.NO_BORDER);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
    
    private void addTableHeader(Table table, String[] headers) {
        for (String header : headers) {
            Cell cell = new Cell()
                .add(new Paragraph(header))
                .setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(PRIMARY_COLOR)
                .setBold()
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER);
            table.addHeaderCell(cell);
        }
        
        // Add subtle alternating row colors
        table.setBackgroundColor(BACKGROUND, 0.3f);
    }
    
    private void addWithdrawTableHeader(Table table, String[] headers) {
        for (String header : headers) {
            Cell cell = new Cell()
                .add(new Paragraph(header))
                .setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(SECONDARY_COLOR)
                .setBold()
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER);
            table.addHeaderCell(cell);
        }
        
        // Add subtle alternating row colors
        table.setBackgroundColor(BACKGROUND, 0.3f);
    }
    
    private void addTableRow(Table table, int rowNum, Map<String, Object> record) {
        // Add row number
        table.addCell(createCell(String.valueOf(rowNum)));
        
        Object dateObj = record.get("attendance_date");
        LocalDate attendanceDate;
        if (dateObj instanceof java.sql.Date) {
            attendanceDate = ((java.sql.Date) dateObj).toLocalDate();
        } else if (dateObj instanceof Instant) {
            attendanceDate = ((Instant) dateObj).atZone(IST).toLocalDate();
        } else {
            throw new ValidationException("Unsupported date type: " + dateObj.getClass());
        }
        
        Cell dateCell = new Cell()
            .add(new Paragraph(DATE_FORMATTER.format(attendanceDate)))
            .setFontColor(TEXT_PRIMARY)
            .setPadding(5)
            .setTextAlignment(TextAlignment.LEFT)
            .setMinHeight(25f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE);
        
        table.addCell(dateCell);
        
        // Add start time with IST
        Object startTimeObj = record.get("start_date_time");
        String startTime;
        if (startTimeObj instanceof java.sql.Timestamp) {
            startTime = TIME_FORMATTER.format(((java.sql.Timestamp) startTimeObj).toInstant());
        } else if (startTimeObj instanceof Instant) {
            startTime = TIME_FORMATTER.format((TemporalAccessor) startTimeObj);
        } else {
            throw new ValidationException("Unsupported time type: " + startTimeObj.getClass());
        }
        table.addCell(createCell(startTime));

        // Add end time with IST
        Object endTimeObj = record.get("end_date_time");
        String endTime;
        if (endTimeObj instanceof java.sql.Timestamp) {
            endTime = TIME_FORMATTER.format(((java.sql.Timestamp) endTimeObj).toInstant());
        } else if (endTimeObj instanceof Instant) {
            endTime = TIME_FORMATTER.format((TemporalAccessor) endTimeObj);
        } else {
            throw new ValidationException("Unsupported time type: " + endTimeObj.getClass());
        }
        table.addCell(createCell(endTime));

        BigDecimal regularHours = (BigDecimal) record.get("regular_hours");
        table.addCell(createCell(String.format("%s ", formatNumber(regularHours))));

        BigDecimal overtimeHours = (BigDecimal) record.get("overtime_hours");
        table.addCell(createCell(String.format("%s ", formatNumber(overtimeHours))));

        table.addCell(createCell(formatCurrency((BigDecimal) record.get("total_pay"))));
    }
    
    private void addWithdrawTableRow(Table table, int rowNum, Map<String, Object> record) {
        // Add row number
        table.addCell(createCell(String.valueOf(rowNum)));
        
        // Add withdraw date
        Object dateObj = record.get("withdrawDate");
        LocalDate withdrawDate;
        if (dateObj instanceof java.sql.Date) {
            withdrawDate = ((java.sql.Date) dateObj).toLocalDate();
        } else if (dateObj instanceof Instant) {
            withdrawDate = ((Instant) dateObj).atZone(IST).toLocalDate();
        } else {
            throw new ValidationException("Unsupported date type: " + dateObj.getClass());
        }
        
        table.addCell(createCell(DATE_FORMATTER.format(withdrawDate)));
        
        // Add amount
        BigDecimal payment = (BigDecimal) record.get("payment");
        table.addCell(createCell(formatCurrency(payment)));
        
        // Add remarks
        String remarks = (String) record.get("remarks");
        table.addCell(createCell(remarks != null ? remarks : "-"));
        
    }
    
    private Cell createCell(String content) {
        return new Cell()
            .add(new Paragraph(content))
            .setFontColor(TEXT_PRIMARY)
            .setPadding(8)
            .setTextAlignment(TextAlignment.CENTER);
    }
    
    private void addSummary(Document document, Map<String, BigDecimal> summary) {
        document.add(new Paragraph("\n"));
        
        Table summaryTable = new Table(new float[]{2, 2, 2, 2, 2})
            .useAllAvailableWidth()
            .setMarginTop(20);
        
        // Add decorative header
        Cell summaryHeader = new Cell(1, 5)
            .add(new Paragraph("Monthly Summary")
                .setFontSize(14)
                .setBold())
            .setFontColor(ColorConstants.WHITE)
            .setBackgroundColor(SECONDARY_COLOR)
            .setPadding(10)
            .setTextAlignment(TextAlignment.CENTER);
        
        summaryTable.addCell(summaryHeader);
        
        // Add summary cells
        Cell regularHoursCell = new Cell()
            .add(new Paragraph("Regular Hours")
                .setFontColor(TEXT_SECONDARY)
                .setFontSize(10))
            .add(new Paragraph(formatNumber(summary.get("totalRegularHours")))
                .setBold()
                .setFontColor(TEXT_PRIMARY))
            .add(new Paragraph(formatCurrency(summary.get("totalRegularPay")))
                .setFontColor(ACCENT2))
            .setBorder(Border.NO_BORDER)
            .setPadding(8)
            .setTextAlignment(TextAlignment.CENTER);
        
        Cell overtimeHoursCell = new Cell()
            .add(new Paragraph("Overtime Hours")
                .setFontColor(TEXT_SECONDARY)
                .setFontSize(10))
            .add(new Paragraph(formatNumber(summary.get("totalOvertimeHours")))
                .setBold()
                .setFontColor(TEXT_PRIMARY))
            .add(new Paragraph(formatCurrency(summary.get("totalOvertimePay")))
                .setFontColor(ACCENT2))
            .setBorder(Border.NO_BORDER)
            .setPadding(8)
            .setTextAlignment(TextAlignment.CENTER);
        
        Cell totalEarningsCell = new Cell()
            .add(new Paragraph("Total Earnings")
                .setFontColor(TEXT_SECONDARY)
                .setFontSize(10))
            .add(new Paragraph(formatCurrency(summary.get("totalEarnings")))
                .setBold()
                .setFontColor(PRIMARY_COLOR))
            .setBorder(Border.NO_BORDER)
            .setPadding(8)
            .setTextAlignment(TextAlignment.CENTER);
        
        Cell totalWithdrawsCell = new Cell()
            .add(new Paragraph("Total Withdraws")
                .setFontColor(TEXT_SECONDARY)
                .setFontSize(10))
            .add(new Paragraph(formatCurrency(summary.get("totalWithdraws")))
                .setBold()
                .setFontColor(ACCENT1))
            .setBorder(Border.NO_BORDER)
            .setPadding(8)
            .setTextAlignment(TextAlignment.CENTER);
        
        Cell finalPaymentCell = new Cell()
            .add(new Paragraph("Final Payment")
                .setFontColor(TEXT_SECONDARY)
                .setFontSize(12))
            .add(new Paragraph(formatCurrency(summary.get("finalPayment")))
                .setBold()
                .setFontSize(16)
                .setFontColor(PRIMARY_COLOR))
            .setBorder(Border.NO_BORDER)
            .setPadding(8)
            .setTextAlignment(TextAlignment.CENTER);
        
        // Add cells to table
        summaryTable.addCell(regularHoursCell);
        summaryTable.addCell(overtimeHoursCell);
        summaryTable.addCell(totalEarningsCell);
        summaryTable.addCell(totalWithdrawsCell);
        summaryTable.addCell(finalPaymentCell);
        
        // Table line = new Table(1).useAllAvailableWidth().setMarginTop(5);
        // line.addCell(new Cell()
        //     .setHeight(2f)
        //     .setBackgroundColor(ACCENT1)
        //     .setBorder(Border.NO_BORDER));
        
        document.add(summaryTable);
        // document.add(line);
    }
    
    private Map<String, BigDecimal> calculateSummary(List<Map<String, Object>> attendanceRecords, List<Map<String, Object>> withdrawRecords) {
        BigDecimal totalRegularHours = BigDecimal.ZERO;
        BigDecimal totalOvertimeHours = BigDecimal.ZERO;
        BigDecimal totalRegularPay = BigDecimal.ZERO;
        BigDecimal totalOvertimePay = BigDecimal.ZERO;
        BigDecimal totalEarnings = BigDecimal.ZERO;
        BigDecimal totalWithdraws = BigDecimal.ZERO;
        
        // Calculate attendance totals
        for (Map<String, Object> record : attendanceRecords) {
            totalRegularHours = totalRegularHours.add((BigDecimal) record.get("regular_hours"));
            totalOvertimeHours = totalOvertimeHours.add((BigDecimal) record.get("overtime_hours"));
            totalRegularPay = totalRegularPay.add((BigDecimal) record.get("regular_pay"));
            totalOvertimePay = totalOvertimePay.add((BigDecimal) record.get("overtime_pay"));
            totalEarnings = totalEarnings.add((BigDecimal) record.get("total_pay"));
        }
        
        // Calculate withdraw totals
        for (Map<String, Object> record : withdrawRecords) {
            totalWithdraws = totalWithdraws.add((BigDecimal) record.get("payment"));
        }
        
        // Calculate final payment (earnings - withdraws)
        BigDecimal finalPayment = totalEarnings.subtract(totalWithdraws);
        
        return Map.of(
            "totalRegularHours", totalRegularHours,
            "totalOvertimeHours", totalOvertimeHours,
            "totalRegularPay", totalRegularPay,
            "totalOvertimePay", totalOvertimePay,
            "totalEarnings", totalEarnings,
            "totalWithdraws", totalWithdraws,
            "finalPayment", finalPayment
        );
    }
    
    private String formatCurrency(BigDecimal amount) {
        return String.format("₹%.2f", amount);
    }
    
    private String formatNumber(BigDecimal number) {
        String formatted = number.stripTrailingZeros().toPlainString();
        return formatted;
    }
    
    public byte[] generatePayrollSummaryPdf(List<Map<String, Object>> attendanceSummaries, 
                                          List<Map<String, Object>> withdrawSummaries,
                                          LocalDate startDate, LocalDate endDate) {
        String requestId = "payroll_summary_" + System.currentTimeMillis();

        PdfGenerationStatus status = processingRequests.get(requestId);
        if (status != null && !status.isStale()) {
            throw new ValidationException("PDF generation already in progress for this request");
        }

        processingRequests.put(requestId, new PdfGenerationStatus());
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PdfDocument pdf = new PdfDocument(new PdfWriter(baos))) {
            
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(20, 20, 20, 20);
            
            addPayrollSummaryHeader(document, startDate, endDate);
            addPayrollSummaryTable(document, attendanceSummaries, withdrawSummaries);
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Error generating payroll summary PDF", e);
            throw new ValidationException("Failed to generate payroll summary PDF: " + e.getMessage());
        } finally {
            processingRequests.remove(requestId);
        }
    }
    
    private void addPayrollSummaryHeader(Document document, LocalDate startDate, LocalDate endDate) {
        Table header = new Table(2).useAllAvailableWidth();
        
        Cell titleCell = new Cell()
            .add(new Paragraph("Payroll Summary")
                .setFontSize(18)
                .setFontColor(PRIMARY_COLOR)
                .setBold())
            .add(new Paragraph("Employee Salary & Withdraw Summary")
                .setFontSize(10)
                .setFontColor(TEXT_SECONDARY))
            .setBorder(Border.NO_BORDER);
        
        Cell dateCell = new Cell()
            .add(new Paragraph("Period: " + DATE_FORMATTER.format(startDate) + 
                 " to " + DATE_FORMATTER.format(endDate))
                .setFontColor(SECONDARY_COLOR)
                .setFontSize(9))
            .add(new Paragraph("Year: " + startDate.getYear())
                .setFontColor(TEXT_SECONDARY)
                .setFontSize(8))
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.RIGHT);
        
        header.addCell(titleCell);
        header.addCell(dateCell);
        
        // Add decorative line
        Table line = new Table(1).useAllAvailableWidth().setMarginTop(8);
        line.addCell(new Cell()
            .setHeight(1.5f)
            .setBackgroundColor(ACCENT1)
            .setBorder(Border.NO_BORDER));
        
        document.add(header);
        document.add(line);
    }
    
    private void addPayrollSummaryTable(Document document, List<Map<String, Object>> attendanceSummaries, 
                                      List<Map<String, Object>> withdrawSummaries) {
        // Create a map of withdraw summaries for quick lookup
        Map<Long, BigDecimal> withdrawMap = withdrawSummaries.stream()
            .collect(Collectors.toMap(
                summary -> (Long) summary.get("employeeId"),
                summary -> (BigDecimal) summary.get("totalWithdraw")
            ));
        
        Table table = new Table(new float[]{0.4f, 1.8f, 1.2f, 1.2f, 1.2f, 1.2f, 1f})
            .useAllAvailableWidth()
            .setMarginTop(15);

        addPayrollSummaryTableHeader(table);
        
        int rowNum = 1;
        for (Map<String, Object> attendanceSummary : attendanceSummaries) {
            addPayrollSummaryTableRow(table, rowNum++, attendanceSummary, withdrawMap);
        }
        
        document.add(table);
    }
    
    private void addPayrollSummaryTableHeader(Table table) {
        String[] headers = {
            "No", "Name", "Hours(Day)", "Total Regular Pay", "Total Pay", "Upad", "Total", "Sign"
        };
        
        for (String header : headers) {
            Cell cell = new Cell()
                .add(new Paragraph(header)
                    .setFontSize(8))
                .setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(PRIMARY_COLOR)
                .setBold()
                .setPadding(4)
                .setTextAlignment(TextAlignment.CENTER);
            table.addHeaderCell(cell);
        }
        
        table.setBackgroundColor(BACKGROUND, 0.3f);
    }
    
    private void addPayrollSummaryTableRow(Table table, int rowNum, Map<String, Object> attendanceSummary, 
                                         Map<Long, BigDecimal> withdrawMap) {
        Long employeeId = (Long) attendanceSummary.get("employeeId");
        String employeeName = (String) attendanceSummary.get("employeeName");
        BigDecimal totalRegularHours = (BigDecimal) attendanceSummary.get("totalRegularHours");
        BigDecimal totalOvertimeHours = (BigDecimal) attendanceSummary.get("totalOvertimeHours");
        BigDecimal totalRegularPay = (BigDecimal) attendanceSummary.get("totalRegularPay");
        BigDecimal totalPay = (BigDecimal) attendanceSummary.get("totalPay");
        BigDecimal totalWithdraw = withdrawMap.getOrDefault(employeeId, BigDecimal.ZERO);
        
        // Calculate total hours and days
        BigDecimal totalHours = totalRegularHours.add(totalOvertimeHours);
        BigDecimal totalDays = totalHours.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        
        // Calculate final total (total pay - withdraw)
        BigDecimal finalTotal = totalPay.subtract(totalWithdraw);
        
        // Row number
        table.addCell(createPayrollCell(String.valueOf(rowNum), 7));
        
        // Employee name
        table.addCell(createPayrollCell(employeeName, 8));
        
        // Hours (Day) - show hours with days in brackets
        String hoursDayText = formatNumber(totalHours) + " (" + formatNumber(totalDays) + ")";
        table.addCell(createPayrollCell(hoursDayText, 7));
        
        // Total Regular Pay
        table.addCell(createPayrollCell(formatCurrency(totalRegularPay), 7));
        
        // Total Pay
        table.addCell(createPayrollCell(formatCurrency(totalPay), 7));
        
        // Upad (Withdraw)
        table.addCell(createPayrollCell(formatCurrency(totalWithdraw), 7));
        
        // Total (Final)
        table.addCell(createPayrollCell(formatCurrency(finalTotal), 7));
        
        // Sign (empty)
        table.addCell(createPayrollCell("", 7));
    }
    
    private Cell createPayrollCell(String content, int fontSize) {
        return new Cell()
            .add(new Paragraph(content)
                .setFontSize(fontSize))
            .setFontColor(TEXT_PRIMARY)
            .setPadding(3)
            .setTextAlignment(TextAlignment.CENTER)
            .setMinHeight(18f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }
    
} 