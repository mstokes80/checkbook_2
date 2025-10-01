package com.checkbook.api.service;

import com.checkbook.api.dto.*;
import com.checkbook.api.entity.Transaction;
import com.checkbook.api.repository.TransactionRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportExportService {

    @Autowired
    private TransactionRepository transactionRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String generateFilename(LocalDate startDate, LocalDate endDate, String extension) {
        return String.format("spending-report-%s-%s.%s",
                startDate.format(DATE_FORMATTER),
                endDate.format(DATE_FORMATTER),
                extension);
    }

    public byte[] generatePdf(SpendingReportResponseDto report) throws IOException {
        PDDocument document = new PDDocument();

        try {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float yPosition = 750;
            float margin = 50;
            float fontSize = 12;

            // Title
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Spending Report");
            contentStream.endText();
            yPosition -= 30;

            // Report Metadata
            contentStream.setFont(PDType1Font.HELVETICA, fontSize);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Period: " + report.getReportMetadata().getStartDate() +
                    " to " + report.getReportMetadata().getEndDate());
            contentStream.endText();
            yPosition -= 20;

            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Accounts: " + String.join(", ", report.getReportMetadata().getAccountNames()));
            contentStream.endText();
            yPosition -= 20;

            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Generated: " + report.getReportMetadata().getGeneratedAt());
            contentStream.endText();
            yPosition -= 30;

            // Summary Section
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Summary");
            contentStream.endText();
            yPosition -= 25;

            contentStream.setFont(PDType1Font.HELVETICA, fontSize);
            ReportSummaryDto summary = report.getSummary();

            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Total Income: $" + summary.getTotalIncome());
            contentStream.endText();
            yPosition -= 20;

            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Total Expenses: $" + summary.getTotalExpenses());
            contentStream.endText();
            yPosition -= 20;

            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Net Balance: $" + summary.getNetBalance());
            contentStream.endText();
            yPosition -= 20;

            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Transaction Count: " + summary.getTransactionCount());
            contentStream.endText();
            yPosition -= 30;

            // Category Breakdown Section
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Top Categories");
            contentStream.endText();
            yPosition -= 25;

            contentStream.setFont(PDType1Font.HELVETICA, fontSize);
            for (CategoryBreakdownDto category : report.getTopCategories()) {
                if (yPosition < 100) {
                    // Add new page if running out of space
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = 750;
                    contentStream.setFont(PDType1Font.HELVETICA, fontSize);
                }

                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(category.getCategoryName() + ": $" + category.getTotalAmount() +
                        " (" + String.format("%.1f", category.getPercentage()) + "%) - " +
                        category.getTransactionCount() + " transactions");
                contentStream.endText();
                yPosition -= 20;
            }

            contentStream.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } finally {
            document.close();
        }
    }

    @Transactional(readOnly = true)
    public String generateCsv(SpendingReportResponseDto report, List<Long> accountIds,
                             LocalDate startDate, LocalDate endDate) throws IOException {
        // Fetch transactions for CSV export
        List<Transaction> transactions = transactionRepository
                .findByAccountIdInAndTransactionDateBetween(accountIds, startDate, endDate);

        StringWriter sw = new StringWriter();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("Transaction Date", "Account", "Category", "Description", "Amount", "Type")
                .build();

        try (CSVPrinter printer = new CSVPrinter(sw, csvFormat)) {
            for (Transaction transaction : transactions) {
                String type = transaction.getAmount().compareTo(BigDecimal.ZERO) >= 0 ? "Income" : "Expense";
                String categoryName = transaction.getCategory() != null ?
                        transaction.getCategory().getName() : "Uncategorized";

                printer.printRecord(
                        transaction.getTransactionDate(),
                        transaction.getAccount().getName(),
                        categoryName,
                        transaction.getDescription(),
                        transaction.getAmount(),
                        type
                );
            }
        }

        return sw.toString();
    }
}