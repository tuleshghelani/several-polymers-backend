package com.inventory.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;

import com.inventory.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalePdfGenerationService {
    // Same color palette as QuotationPdfGenerationService
    private static final com.itextpdf.kernel.colors.Color PRIMARY_COLOR = new DeviceRgb(56, 166, 219);      // #38a6db
    private static final com.itextpdf.kernel.colors.Color SECONDARY_COLOR = new DeviceRgb(30, 85, 150);     // #1e5596
    private static final com.itextpdf.kernel.colors.Color BACKGROUND_LIGHT = new DeviceRgb(253, 245, 236);  // #FDF5EC
    private static final com.itextpdf.kernel.colors.Color TEXT_DARK = new DeviceRgb(51, 51, 51);            // #333333
    private static final com.itextpdf.kernel.colors.Color TEXT_LIGHT = new DeviceRgb(255, 255, 255);        // #ffffff
    private static final com.itextpdf.kernel.colors.Color SECONDARY_LIGHT = new DeviceRgb(157, 185, 225);   // #9DB9E1

    public byte[] generateSalePdf(Map<String, Object> saleData) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (PdfDocument pdf = new PdfDocument(new PdfWriter(outputStream))) {
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(36, 36, 36, 36);

            addHeader(document);
            addPageFooter(pdf, document, 1);

            addSaleDetails(document, saleData);
            addItemsTable(document, (List<Map<String, Object>>) saleData.get("items"));
            addPageFooter(pdf, document, 2);

            addTerms(document);
            addPageFooter(pdf, document, 3);

            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error generating Sale PDF", e);
            throw new ValidationException("Failed to generate Sale PDF: " + e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void addHeader(Document document) {
        Table headingTable = new Table(1).useAllAvailableWidth();
        Cell headingTableCell = new Cell()
                .add(new Paragraph("SEVERAL POLYMERS").setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(12).setBold().setFontColor(PRIMARY_COLOR))
                .setPadding(2);
        headingTable.addCell(headingTableCell);
        document.add(headingTable);

        Table saleHeadingTable = new Table(1).useAllAvailableWidth();
        Cell saleHeadingCell = new Cell()
                .add(new Paragraph("Sale Invoice").setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(12).setBold().setFontColor(TEXT_LIGHT))
                .setBold()
                .setFontSize(10)
                .setFontColor(PRIMARY_COLOR)
                .setBackgroundColor(PRIMARY_COLOR)
                .setPadding(2);
        saleHeadingTable.addCell(saleHeadingCell);
        document.add(saleHeadingTable);
    }

    private void addSaleDetails(Document document, Map<String, Object> data) {
        Table infoTable = new Table(new float[]{1}).useAllAvailableWidth();
        Cell infoCell = new Cell();

        Paragraph customerName = new Paragraph(formatValue(data.get("customerName")))
                .setBold()
                .setFontSize(10)
                .setFontColor(PRIMARY_COLOR);
        Paragraph customerAddress = new Paragraph(formatValue(data.get("address")))
                .setFontSize(8)
                .setFontColor(TEXT_DARK);

        Table detailsTable = new Table(new float[]{1.6f, 2.4f, 1.6f, 2.4f}).useAllAvailableWidth();

        addDetailPair(detailsTable,
                "Invoice Number", formatValue(data.get("invoiceNumber")),
                "Invoice Date", formatValue(data.get("saleDate")));

        addDetailPair(detailsTable,
                "Customer GST", formatValue(data.get("customerGst")),
                "Mobile No.", formatValue(data.get("contactNumber")));

        addDetailPair(detailsTable,
                "Transport", formatValue(data.get("transportMasterId")),
                "Case Number", formatValue(data.get("caseNumber")));

        addDetailPair(detailsTable,
                "Reference Name", formatValue(data.get("referenceName")),
                null, null);

        infoCell.add(customerName)
                .add(customerAddress)
                .add(detailsTable)
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setBorder(new SolidBorder(SECONDARY_LIGHT, 1))
                .setPadding(6);

        infoTable.addCell(infoCell);
        document.add(infoTable);
    }

    private void addDetailPair(Table table, String label1, String value1, String label2, String value2) {
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(label1 + ":").setBold().setFontSize(9).setFontColor(SECONDARY_COLOR)));
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(value1).setFontSize(8).setFontColor(TEXT_DARK)));
        if(label2 != null && value2 != null) {
                table.addCell(new Cell().setBorder(Border.NO_BORDER)
                        .add(new Paragraph(label2 + ":").setBold().setFontSize(9).setFontColor(SECONDARY_COLOR)));
                table.addCell(new Cell().setBorder(Border.NO_BORDER)
                        .add(new Paragraph(value2).setFontSize(8).setFontColor(TEXT_DARK)));
        }
    }

    private void addItemsTable(Document document, List<Map<String, Object>> items) {
        document.add(new Paragraph("\n"));

        Paragraph itemsTitle = new Paragraph("Items & Services")
                .setBold()
                .setFontSize(10)
                .setFontColor(SECONDARY_COLOR)
                .setMarginBottom(5);
        document.add(itemsTitle);

        Table table = new Table(new float[]{1, 5, 2, 2, 2, 2, 2, 2, 2})
                .useAllAvailableWidth()
                .setMarginTop(2);

        Stream.of(
                "No.",
                "Item Name",
                "Weight per roll",
                "Pcs",
                "Quantity",
                "Unit Price",
                "Amount",
                "Tax Amount",
                "Total Amount"
        ).forEach(title -> table.addHeaderCell(
                new Cell().add(new Paragraph(title).setFontSize(9))
                        .setBackgroundColor(SECONDARY_COLOR)
                        .setFontColor(TEXT_LIGHT)
                        .setBold()
                        .setPadding(0)
        ));

        AtomicInteger counter = new AtomicInteger(1);
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        int totalPcs = 0;
        BigDecimal totalFinalAmount = BigDecimal.ZERO;

        for (Map<String, Object> item : items) {
            boolean isEvenRow = counter.get() % 2 == 0;
            Color rowColor = isEvenRow ? BACKGROUND_LIGHT : ColorConstants.WHITE;

            BigDecimal quantity = toBigDecimal(item.get("quantity"));
            BigDecimal unitPrice = toBigDecimal(item.get("unitPrice"));
            BigDecimal amount = toBigDecimal(item.get("price"));
            BigDecimal gstAmount = toBigDecimal(item.get("taxAmount"));
            BigDecimal finalPrice = toBigDecimal(item.get("finalPrice"));
            int numberOfRoll = item.get("numberOfRoll") != null ? ((Number) item.get("numberOfRoll")).intValue() : 0;
            BigDecimal weightPerRoll = toBigDecimal(item.get("weightPerRoll"));

            table.addCell(new Cell().add(new Paragraph(String.valueOf(counter.getAndIncrement())).setFontSize(8)).setBackgroundColor(rowColor));
            table.addCell(new Cell().add(new Paragraph(formatValue(item.get("productName"))).setFontSize(8)).setBackgroundColor(rowColor));
            table.addCell(new Cell().add(new Paragraph(weightPerRoll.setScale(3, RoundingMode.HALF_UP).toPlainString()).setFontSize(8)).setBackgroundColor(rowColor));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(numberOfRoll)).setFontSize(8)).setBackgroundColor(rowColor));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(quantity)).setFontSize(8)).setBackgroundColor(rowColor));
            table.addCell(new Cell().add(new Paragraph(unitPrice.toPlainString()).setFontSize(8)).setBackgroundColor(rowColor));
            table.addCell(new Cell().add(new Paragraph(amount.setScale(2, RoundingMode.HALF_UP).toPlainString()).setFontSize(8)).setBackgroundColor(rowColor));
            table.addCell(new Cell().add(new Paragraph(gstAmount.setScale(2, RoundingMode.HALF_UP).toPlainString()).setFontSize(8)).setBackgroundColor(rowColor));
            table.addCell(new Cell().add(new Paragraph(finalPrice.setScale(2, RoundingMode.HALF_UP).toPlainString()).setFontSize(8)).setBackgroundColor(rowColor));

            subtotal = subtotal.add(amount);
            totalTax = totalTax.add(gstAmount);
            totalQuantity = totalQuantity.add(quantity);
            totalPcs += numberOfRoll;
            totalFinalAmount = totalFinalAmount.add(finalPrice);
        }

        // Totals row
        Color totalsBg = SECONDARY_LIGHT;
        table.addCell(new Cell().add(new Paragraph("").setFontSize(8)).setBackgroundColor(totalsBg));
        table.addCell(new Cell().add(new Paragraph("TOTAL").setBold().setFontSize(8)).setBackgroundColor(totalsBg));
        table.addCell(new Cell().add(new Paragraph("").setFontSize(8)).setBackgroundColor(totalsBg));
        table.addCell(new Cell().add(new Paragraph(String.valueOf(totalPcs)).setBold().setFontSize(8)).setBackgroundColor(totalsBg));
        table.addCell(new Cell().add(new Paragraph(String.valueOf(totalQuantity)).setBold().setFontSize(8)).setBackgroundColor(totalsBg));
        table.addCell(new Cell().add(new Paragraph("").setFontSize(8)).setBackgroundColor(totalsBg));
        table.addCell(new Cell().add(new Paragraph(subtotal.setScale(2, RoundingMode.HALF_UP).toPlainString()).setBold().setFontSize(8)).setBackgroundColor(totalsBg));
        table.addCell(new Cell().add(new Paragraph(totalTax.setScale(2, RoundingMode.HALF_UP).toPlainString()).setBold().setFontSize(8)).setBackgroundColor(totalsBg));
        table.addCell(new Cell().add(new Paragraph(totalFinalAmount.setScale(2, RoundingMode.HALF_UP).toPlainString()).setBold().setFontSize(8)).setBackgroundColor(totalsBg));

        document.add(table);

        Table summaryTable = new Table(2)
                .useAllAvailableWidth()
                .setMarginTop(10);

        summaryTable.addCell(new Cell().setBorder(Border.NO_BORDER).setWidth(350));

        Cell totalsCell = new Cell();
        Table totalsTable = new Table(2).useAllAvailableWidth();

        BigDecimal grandTotal = subtotal.add(totalTax).setScale(0, RoundingMode.HALF_UP);
        addTotalRow(totalsTable, "GRAND TOTAL", grandTotal.toPlainString() + "/-", true);

        totalsCell.add(totalsTable)
                .setBorder(new SolidBorder(SECONDARY_COLOR, 1))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setPadding(6);

        summaryTable.addCell(new Cell().setBorder(Border.NO_BORDER));
        summaryTable.addCell(totalsCell);

        document.add(summaryTable);
    }

    private void addTotalRow(Table table, String label, String value, boolean isGrandTotal) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label)
                        .setBold()
                        .setFontColor(isGrandTotal ? PRIMARY_COLOR : SECONDARY_COLOR)
                        .setFontSize(isGrandTotal ? 10 : 8))
                .setBorder(Border.NO_BORDER);

        Cell valueCell = new Cell()
                .add(new Paragraph(value)
                        .setBold()
                        .setFontColor(isGrandTotal ? PRIMARY_COLOR : TEXT_DARK)
                        .setFontSize(isGrandTotal ? 10 : 8))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);

        if (isGrandTotal) {
            labelCell.setBorderTop(new SolidBorder(SECONDARY_COLOR, 1));
            valueCell.setBorderTop(new SolidBorder(SECONDARY_COLOR, 1));
        }

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addTerms(Document document) {
        document.add(new Paragraph("\n"));
        Table termsHeaderTable = new Table(1).useAllAvailableWidth();
        Cell termsHeaderCell = new Cell()
                .add(new Paragraph("TERMS AND CONDITIONS")
                        .setFontColor(TEXT_LIGHT)
                        .setBold()
                        .setFontSize(10))
                .setBackgroundColor(PRIMARY_COLOR)
                .setPadding(2)
                .setTextAlignment(TextAlignment.CENTER);
        termsHeaderTable.addCell(termsHeaderCell);
        document.add(termsHeaderTable);

        Table termsTable = new Table(new float[]{1}).useAllAvailableWidth();
        Cell termsCell = new Cell()
                .setBorder(new SolidBorder(SECONDARY_COLOR, 1))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setPadding(2);

        addCompactTerm(termsCell, "Subject to Rajkot jurisdiction.");

        termsTable.addCell(termsCell);
        document.add(termsTable);
    }

    private void addCompactTerm(Cell container, String content) {
        Table row = new Table(new float[]{0.4f, 12f}).useAllAvailableWidth();
        Cell bullet = new Cell().setBorder(Border.NO_BORDER)
                .setPadding(0)
                .add(new Paragraph("•").setFontColor(PRIMARY_COLOR).setBold().setFontSize(9));
        Cell text = new Cell().setBorder(Border.NO_BORDER)
                .setPaddingTop(0).setPaddingBottom(2).setPaddingLeft(2)
                .add(new Paragraph(content).setFontSize(9).setFontColor(TEXT_DARK));
        row.addCell(bullet);
        row.addCell(text);
        container.add(row);
    }

    private void addPageFooter(PdfDocument pdfDoc, Document document, int pageNumber) {
        float footerY = 20;
        float pageWidth = pdfDoc.getDefaultPageSize().getWidth();

        Table footerBgTable = new Table(1)
                .useAllAvailableWidth()
                .setFixedPosition(36, footerY - 6, pageWidth - 72);

        Cell footerBgCell = new Cell()
                .setHeight(20)
                .setBackgroundColor(SECONDARY_COLOR)
                .setBorder(Border.NO_BORDER);
        footerBgTable.addCell(footerBgCell);

        Table footerTable = new Table(2)
                .useAllAvailableWidth()
                .setFixedPosition(36, footerY, pageWidth - 72);

        Cell contactCell = new Cell()
                .add(new Paragraph("Several Polymers")
                        .setFontSize(7)
                        .setFontColor(TEXT_LIGHT))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT);

        Cell websiteCell = new Cell()
                .add(new Paragraph("https://severalpolymers.in/")
                        .setFontSize(7)
                        .setFontColor(TEXT_LIGHT))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);

        footerTable.addCell(contactCell);
        footerTable.addCell(websiteCell);

        document.add(footerBgTable);
        document.add(footerTable);
    }

    private String formatValue(Object value) {
        return value != null ? value.toString() : "";
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.toString());
    }
}


