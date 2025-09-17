package com.inventory.service;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.properties.AreaBreakType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.inventory.exception.ValidationException;
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
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotationPdfGenerationService {
    // Color scheme (updated as per request)
    // Base
    private static final Color PRIMARY_COLOR = new DeviceRgb(56, 166, 219);      // #38a6db
    private static final Color SECONDARY_COLOR = new DeviceRgb(30, 85, 150);     // #1e5596
    private static final Color BACKGROUND_LIGHT = new DeviceRgb(253, 245, 236);  // #FDF5EC
    private static final Color TEXT_DARK = new DeviceRgb(51, 51, 51);            // #333333
    private static final Color TEXT_MUTED = new DeviceRgb(118, 118, 118);        // #767676
    private static final Color SUPPORTING_COLOR = new DeviceRgb(174, 198, 207);  // #AEC6CF
    private static final Color TEXT_LIGHT = new DeviceRgb(255, 255, 255);        // #ffffff

    // Derived lights
    private static final Color PRIMARY_LIGHT = new DeviceRgb(191, 230, 246);     // #BFE6F6 softer tint of primary
    private static final Color SECONDARY_LIGHT = new DeviceRgb(157, 185, 225);   // #9DB9E1 softer tint of secondary

    private static final BigDecimal SQ_FEET_TO_METER = BigDecimal.valueOf(10.764);
    private static final BigDecimal MM_TO_METER = BigDecimal.valueOf(1000);

    public byte[] generateQuotationPdf(Map<String, Object> quotationData) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (PdfDocument pdf = new PdfDocument(new PdfWriter(outputStream))) {
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(36, 36, 36, 36);

            // Add content
            addHeader(document, quotationData);
            addPageFooter(pdf, document, 1);

            addQuotationDetails(document, quotationData);
            addItemsTable(document, (List<Map<String, Object>>) quotationData.get("items"), quotationData);
            addPageFooter(pdf, document, 2);

            addBankDetailsAndTerms(document);
            addPageFooter(pdf, document, 3);

            // Removed last decorative page as requested

            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error generating PDF", e);
            throw new ValidationException("Failed to generate PDF: " + e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void addHeader(Document document, Map<String, Object> data) {
        // Compact header: logo left, company info right, minimal padding
        // Table headerTable = new Table(new float[]{2, 5}).useAllAvailableWidth();
        // headerTable.setMarginTop(0).setMarginBottom(4);

        // // Logo cell
        // Cell logoCell = new Cell();
        // try (InputStream imageStream = getClass().getClassLoader().getResourceAsStream("quotation/several_logo.jpg")) {
        //     if (imageStream != null) {
        //         ImageData imageData = ImageDataFactory.create(imageStream.readAllBytes());
        //         Image img = new Image(imageData);
        //         img.setWidth(150);
        //         img.setHeight(40);
        //         img.setHorizontalAlignment(HorizontalAlignment.LEFT);
        //         logoCell.add(img);
        //     } else {
        //         logoCell.add(new Paragraph("SEVERAL POLYMERS").setBold().setFontColor(PRIMARY_COLOR));
        //     }
        // } catch (Exception e) {
        //     logoCell.add(new Paragraph("SEVERAL POLYMERS").setBold().setFontColor(PRIMARY_COLOR));
        // }
        // logoCell.setBorder(Border.NO_BORDER).setPadding(2);

        // // Company info cell (compact)
        // Cell infoCell = new Cell();
        // infoCell.add(new Paragraph("MOVAIYA, TA - PADDHARI, RAJKOT, Rajkot, Gujarat, 360110")
        //                 .setFontSize(9).setFontColor(TEXT_DARK))
        //         .add(new Paragraph("E-mail: severalpolymers@gmail.com")
        //                 .setFontSize(9).setFontColor(TEXT_DARK))
        //         .add(new Paragraph("GST NO.24DMAPP6011D1ZL").setFontSize(9).setBold().setFontColor(SECONDARY_COLOR));
        // infoCell.setBorder(Border.NO_BORDER).setPadding(2).setTextAlignment(TextAlignment.RIGHT);

        // headerTable.addCell(logoCell);
        // headerTable.addCell(infoCell);
        // document.add(headerTable);

        // Thin heading bar
        
        Table headingTable = new Table(1).useAllAvailableWidth();
        Cell headingTableCell = new Cell()
                .add(new Paragraph("SEVERAL POLYMERS").setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(12).setBold().setFontColor(PRIMARY_COLOR))
                // .add(new Paragraph(formatValue(data.get("customerName")))
                        .setBold()
                        .setFontSize(10)
                        .setFontColor(TEXT_LIGHT)
                .setPadding(2);
        headingTable.addCell(headingTableCell);
        document.add(headingTable);

        Table quotationHeadingTable = new Table(1).useAllAvailableWidth();
        Cell quotationHeadingCell = new Cell()
                .add(new Paragraph("Quotation").setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(12).setBold().setFontColor(TEXT_LIGHT))
                // .add(new Paragraph(formatValue(data.get("customerName")))
                        .setBold()
                        .setFontSize(10)
                        .setFontColor(PRIMARY_COLOR)
                .setBackgroundColor(PRIMARY_COLOR)
                .setPadding(2);
        quotationHeadingTable.addCell(quotationHeadingCell);
        document.add(quotationHeadingTable);
    }

    private void addQuotationDetails(Document document, Map<String, Object> data) {
        // Compact details, reduced padding and font sizes
        Table infoTable = new Table(new float[]{1}).useAllAvailableWidth();
        Cell infoCell = new Cell();
        
        // Customer details with highlighting
        // Paragraph customerTitle = new Paragraph("Client Information")
        //         .setBold()
        //         .setFontSize(12)
        //         .setFontColor(SECONDARY_COLOR);
        
        Paragraph customerName = new Paragraph(formatValue(data.get("customerName")))
                .setBold()
                .setFontSize(10)
                .setFontColor(PRIMARY_COLOR);
        Paragraph customerAddress = new Paragraph(formatValue(data.get("address")))
                .setFontSize(8)
                .setFontColor(TEXT_DARK);
        
        // 4-column grid: [label, value, label, value]
        Table detailsTable = new Table(new float[]{1.6f, 2.4f, 1.6f, 2.4f}).useAllAvailableWidth();

        // Row 1: Quote Number | Quote Date
        addDetailPair(detailsTable,
                "Quote Number", formatValue(data.get("quoteNumber")),
                "Quote Date", formatValue(data.get("quoteDate")),
                "Reference Name", formatValue(data.get("referenceName")),
                "Case Number", formatValue(data.get("caseNumber")));

        // Row 2: Valid Until | Mobile No.
        addThreeDetailPair(detailsTable,
                "Valid Until", formatValue(data.get("validUntil")),
                "Mobile No.", formatValue(data.get("contactNumber")),
                "Transport Name", formatValue(data.get("transportMasterName")));
        
        infoCell.add(customerName)
                .add(customerAddress)
                .add(detailsTable)
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setBorder(new SolidBorder(SECONDARY_LIGHT, 1))
                .setPadding(6);
                
        infoTable.addCell(infoCell);
        document.add(infoTable);

        // Add remarks if present with styled box
        /*if (data.get("remarks") != null) {
            Table remarksTable = new Table(1).useAllAvailableWidth().setMarginTop(15);
            Cell remarksCell = new Cell()
                    .add(new Paragraph("Remarks")
                            .setBold()
                            .setFontColor(PRIMARY_COLOR))
                    .add(new Paragraph(data.get("remarks").toString())
                            .setFontColor(TEXT_DARK))
                    .setBorder(new SolidBorder(PRIMARY_LIGHT, 1))
                    .setBackgroundColor(BACKGROUND_LIGHT)
                    .setPadding(10);
            remarksTable.addCell(remarksCell);
            document.add(remarksTable);
        }*/
    }
    
    private void addDetailRow(Table table, String label, String value) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label + ":").setBold().setFontSize(9).setFontColor(SECONDARY_COLOR))
                .setBorder(Border.NO_BORDER);
        
        Cell valueCell = new Cell()
                .add(new Paragraph(value).setFontSize(9).setFontColor(TEXT_DARK))
                .setBorder(Border.NO_BORDER);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addDetailPair(Table table, String label1, String value1, String label2, String value2, String label3, String value3,
    String label4, String value4) {
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(label1 + ":").setBold().setFontSize(9).setFontColor(SECONDARY_COLOR)));
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(value1).setFontSize(7).setFontColor(TEXT_DARK)));
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(label2 + ":").setBold().setFontSize(9).setFontColor(SECONDARY_COLOR)));
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(value2).setFontSize(9).setFontColor(TEXT_DARK)));
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(label3 + ":").setBold().setFontSize(9).setFontColor(SECONDARY_COLOR)));
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(value3).setFontSize(9).setFontColor(TEXT_DARK)));
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(label4 + ":").setBold().setFontSize(9).setFontColor(SECONDARY_COLOR)));
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(value4).setFontSize(9).setFontColor(TEXT_DARK)));
    }

    

    private void addThreeDetailPair(Table table, String label1, String value1, String label2, String value2, String label3, String value3) {
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(label1 + ":").setBold().setFontSize(9).setFontColor(SECONDARY_COLOR)));
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(value1).setFontSize(7).setFontColor(TEXT_DARK)));
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(label2 + ":").setBold().setFontSize(9).setFontColor(SECONDARY_COLOR)));
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(value2).setFontSize(9).setFontColor(TEXT_DARK)));
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(label3 + ":").setBold().setFontSize(9).setFontColor(SECONDARY_COLOR)));
        table.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(value3).setFontSize(9).setFontColor(TEXT_DARK)));
    }

    private void addItemsTable(Document document, List<Map<String, Object>> items, Map<String, Object> quotationData) {
        document.add(new Paragraph("\n"));
        
        // Add title for items section
        Paragraph itemsTitle = new Paragraph("Items & Services")
                .setBold()
                .setFontSize(10)
                .setFontColor(SECONDARY_COLOR)
                .setMarginBottom(5);
        document.add(itemsTitle);
        
        // Create styled table
        Table table = new Table(new float[]{1, 5, 2, 2, 2, 2, 2, 2, 2})
                .useAllAvailableWidth()
                .setMarginTop(2);

        // Add headers with styling
        Stream.of(
                "No.",
                "Item Name",
                "No. Of Roll",
                "WEIGHT PER ROLL",
                "QUANTITY",
                "Unit Price",
                "Amount",
                "GST",
                "Total Amount"
        )
                .forEach(title -> table.addHeaderCell(
                        new Cell().add(new Paragraph(title).setFontSize(9))
                                .setBackgroundColor(SECONDARY_COLOR)
                                .setFontColor(TEXT_LIGHT)
                                .setBold()
                                .setPadding(0)
                ));

        // Add items with alternating row colors for look
        AtomicInteger counter = new AtomicInteger(1);
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalRolls = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalFinalAmount = BigDecimal.ZERO;

        for (Map<String, Object> item : items) {
            boolean isEvenRow = counter.get() % 2 == 0;
            Color rowColor = isEvenRow ? BACKGROUND_LIGHT : ColorConstants.WHITE;
            
            table.addCell(new Cell()
                    .add(new Paragraph(String.valueOf(counter.getAndIncrement())).setFontSize(8))
                    .setBackgroundColor(rowColor));
                    
            table.addCell(new Cell()
                    .add(convertHtmlToParagraph(item, true).setFontSize(8))
                    .setBackgroundColor(rowColor));
                    
            // NUMBER OF ROLL
            BigDecimal numberOfRolls = toBigDecimal(item.get("numberOfRoll"));
            table.addCell(new Cell()
                    .add(new Paragraph(formatValue(item.get("numberOfRoll"))).setFontSize(8))
                    .setBackgroundColor(rowColor));

            // WEIGHT/ROLL
            table.addCell(new Cell()
                    .add(new Paragraph(formatValue(item.get("weightPerRoll"))).setFontSize(8))
                    .setBackgroundColor(rowColor));

            // QUANTITY
            BigDecimal quantity = toBigDecimal(item.get("quantity"));
            table.addCell(new Cell()
                    .add(new Paragraph(quantity.toString()).setFontSize(8))
                    .setBackgroundColor(rowColor));

            // UNIT PRICE
            BigDecimal unitPrice = toBigDecimal(item.get("unitPrice"));
            table.addCell(new Cell()
                    .add(new Paragraph(unitPrice.toPlainString()).setFontSize(8))
                    .setBackgroundColor(rowColor));

            // ITEM PRICE (pre-tax, i.e., discountPrice if present, else price)
            Object priceSource = item.get("discountPrice") != null ? item.get("discountPrice") : item.get("price");
            BigDecimal itemPrice = toBigDecimal(priceSource);
            table.addCell(new Cell()
                    .add(new Paragraph(itemPrice.toPlainString()).setFontSize(8))
                    .setBackgroundColor(rowColor));

            // GST (taxAmount)
            BigDecimal gstAmount = toBigDecimal(item.get("taxAmount"));
            table.addCell(new Cell()
                    .add(new Paragraph(gstAmount.toPlainString()).setFontSize(8))
                    .setBackgroundColor(rowColor));

            // FINAL PRICE
            BigDecimal finalPrice = toBigDecimal(item.get("finalPrice"));
            table.addCell(new Cell()
                    .add(new Paragraph(finalPrice.toPlainString()).setFontSize(8))
                    .setBackgroundColor(rowColor));

            // Accumulate totals
            totalAmount = totalAmount.add(itemPrice);
            totalTax = totalTax.add(gstAmount);
            totalRolls = totalRolls.add(numberOfRolls);
            totalQuantity = totalQuantity.add(quantity);
            totalFinalAmount = totalFinalAmount.add(finalPrice);
        }

        // Add totals row
        addTotalsRow(table, totalRolls, totalQuantity, totalAmount, totalTax, totalFinalAmount);

        document.add(table);

        // Create summary table with styling
        Table summaryTable = new Table(2)
                .useAllAvailableWidth()
                .setMarginTop(10);
                
        summaryTable.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setWidth(350)); // Empty cell for spacing
                
        // Right side - totals with styling
        Cell totalsCell = new Cell();
        Table totalsTable = new Table(2).useAllAvailableWidth();
        
        // Add styled rows for totals
        addTotalRow(totalsTable, "SUBTOTAL", totalAmount.toString() + "/-", false);

        // Quotation discount
        BigDecimal quotationDiscountPercentage = toBigDecimal(quotationData.get("quotationDiscountPercentage"));
        addTotalRow(totalsTable, "Quotation discount (%)", quotationDiscountPercentage.toPlainString() + "%", false);
        
        // GST calculation (overall)
        BigDecimal gstAmount = toBigDecimal(quotationData.get("quotationTaxAmount")).setScale(0, RoundingMode.HALF_UP);
        addTotalRow(totalsTable, "GST", gstAmount.toPlainString() + "/-", false);
        // Packaging & Forwarding charges (from quotation)
        BigDecimal packagingCharges = toBigDecimal(quotationData.get("packagingAndForwadingCharges"));
        addTotalRow(totalsTable, "PACKAGING & FORWARDING", packagingCharges.toPlainString() + "/-", false);
        
        // Grand total with prominent styling
        BigDecimal grandTotal = toBigDecimal(quotationData.get("totalAmount")).setScale(0, RoundingMode.HALF_UP);
        addTotalRow(totalsTable, "GRAND TOTAL", grandTotal.toPlainString() + "/-", true);
        
        totalsCell.add(totalsTable)
                .setBorder(new SolidBorder(SECONDARY_COLOR, 1))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setPadding(6);
                
        summaryTable.addCell(new Cell().setBorder(Border.NO_BORDER)); // Empty cell
        summaryTable.addCell(totalsCell);
        
        document.add(summaryTable);
    }
    
    private void addTotalsRow(Table table, BigDecimal totalRolls, BigDecimal totalQuantity, 
                             BigDecimal totalAmount, BigDecimal totalTax, BigDecimal totalFinalAmount) {
        // Create a distinctive totals row with bold styling
        Color totalsRowColor = new DeviceRgb(240, 248, 255); // Light blue background
        
        // Column 1: "TOTAL" label
        table.addCell(new Cell()
                .add(new Paragraph("TOTAL").setBold().setFontSize(9).setFontColor(SECONDARY_COLOR))
                .setBackgroundColor(totalsRowColor)
                .setBorder(new SolidBorder(SECONDARY_COLOR, 1)));
        
        // Column 2: Empty (Item Name column)
        table.addCell(new Cell()
                .add(new Paragraph("").setFontSize(8))
                .setBackgroundColor(totalsRowColor)
                .setBorder(new SolidBorder(SECONDARY_COLOR, 1)));
        
        // Column 3: Total Number of Rolls
        table.addCell(new Cell()
                .add(new Paragraph(totalRolls.setScale(0, RoundingMode.HALF_UP).toString()).setBold().setFontSize(8).setFontColor(TEXT_DARK))
                .setBackgroundColor(totalsRowColor)
                .setBorder(new SolidBorder(SECONDARY_COLOR, 1))
                .setTextAlignment(TextAlignment.CENTER));
        
        // Column 4: Empty (Weight per Roll column)
        table.addCell(new Cell()
                .add(new Paragraph("").setFontSize(8))
                .setBackgroundColor(totalsRowColor)
                .setBorder(new SolidBorder(SECONDARY_COLOR, 1)));
        
        // Column 5: Total Quantity
        table.addCell(new Cell()
                .add(new Paragraph(totalQuantity.toString()).setBold().setFontSize(8).setFontColor(TEXT_DARK))
                .setBackgroundColor(totalsRowColor)
                .setBorder(new SolidBorder(SECONDARY_COLOR, 1))
                .setTextAlignment(TextAlignment.CENTER));
        
        // Column 6: Empty (Unit Price column)
        table.addCell(new Cell()
                .add(new Paragraph("").setFontSize(8))
                .setBackgroundColor(totalsRowColor)
                .setBorder(new SolidBorder(SECONDARY_COLOR, 1)));
        
        // Column 7: Total Amount (pre-tax)
        table.addCell(new Cell()
                .add(new Paragraph(totalAmount.toPlainString()).setBold().setFontSize(8).setFontColor(TEXT_DARK))
                .setBackgroundColor(totalsRowColor)
                .setBorder(new SolidBorder(SECONDARY_COLOR, 1))
                .setTextAlignment(TextAlignment.RIGHT));
        
        // Column 8: Total GST
        table.addCell(new Cell()
                .add(new Paragraph(totalTax.toPlainString()).setBold().setFontSize(8).setFontColor(TEXT_DARK))
                .setBackgroundColor(totalsRowColor)
                .setBorder(new SolidBorder(SECONDARY_COLOR, 1))
                .setTextAlignment(TextAlignment.RIGHT));
        
        // Column 9: Total Final Amount
        table.addCell(new Cell()
                .add(new Paragraph(totalFinalAmount.toPlainString()).setBold().setFontSize(8).setFontColor(PRIMARY_COLOR))
                .setBackgroundColor(totalsRowColor)
                .setBorder(new SolidBorder(SECONDARY_COLOR, 1))
                .setTextAlignment(TextAlignment.RIGHT));
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
            // Add top border for grand total
            labelCell.setBorderTop(new SolidBorder(SECONDARY_COLOR, 1));
            valueCell.setBorderTop(new SolidBorder(SECONDARY_COLOR, 1));
            
            // Slightly increase emphasis for grand total
        }
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private boolean shouldShowCalculationDetails(Map<String, Object> item) {
        String productType = (String) item.get("productType");
        return "REGULAR".equals(productType) || "POLY_CARBONATE".equals(productType);
    }


    private Table createSqFeetCalculationTable(List<Map<String, Object>> calculations) {
        Table table = new Table(new float[]{2, 2, 2, 2, 2})
                .useAllAvailableWidth()
                .setMarginTop(5);

        // Add headers with styling
        Stream.of("Feet", "Inch", "Nos", "Sq. Meter", "Sq.Feet")
                .forEach(title -> {
                    Cell header = new Cell()
                            .add(new Paragraph(title))
                            .setBackgroundColor(PRIMARY_COLOR)
                            .setFontColor(TEXT_LIGHT)
                            .setBold()
                            .setPadding(5);
                    table.addHeaderCell(header);
                });

        // Add data rows with better color coordination
        for (Map<String, Object> calc : calculations) {
            BigDecimal sqFeet = toBigDecimal(calc.get("sqFeet"));
            BigDecimal meter = sqFeet.divide(SQ_FEET_TO_METER, 4, RoundingMode.HALF_UP);

            // Feet column
            table.addCell(new Cell()
                    .add(new Paragraph(formatValue(calc.get("feet"))))
                    .setBackgroundColor(PRIMARY_LIGHT));

            // Inch column
            table.addCell(new Cell()
                    .add(new Paragraph(formatValue(calc.get("inch"))))
                    .setBackgroundColor(SECONDARY_LIGHT)
                    .setFontColor(TEXT_LIGHT));

            // Nos column
            table.addCell(new Cell()
                    .add(new Paragraph(formatValue(calc.get("nos"))))
                    .setBackgroundColor(PRIMARY_LIGHT));

            // Sq. Meter column
            table.addCell(new Cell()
                    .add(new Paragraph(formatValue(meter)))
                    .setBackgroundColor(SECONDARY_LIGHT)
                    .setFontColor(TEXT_LIGHT));

            // Sq. Feet column
            table.addCell(new Cell()
                    .add(new Paragraph(formatValue(sqFeet)))
                    .setBackgroundColor(PRIMARY_LIGHT));
        }

        return table;
    }

    private String formatValue(Object value) {
        return value != null ? value.toString() : "0";
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.toString());
    }

    private void addBankDetailsAndTerms(Document document) {
        // Start new page
//        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        // Add styled section title
//        Table titleTable = new Table(1).useAllAvailableWidth();
//        Cell titleCell = new Cell()
//                .add(new Paragraph("PAYMENT & TERMS")
//                        .setFontSize(10)
//                        .setBold()
//                        .setFontColor(TEXT_LIGHT))
//                .setBackgroundColor(SECONDARY_COLOR)
//                .setPadding(10)
//                .setTextAlignment(TextAlignment.CENTER);
//        titleTable.addCell(titleCell);
//        document.add(titleTable);
//        document.add(new Paragraph("\n"));

        // GST Number with styling
        Table gstTable = new Table(1).useAllAvailableWidth();
        /*Cell gstCell = new Cell()
                .add(new Paragraph("GST No: 24AAMFJ9388A1Z4")
                        .setFontColor(TEXT_DARK)
                        .setFontSize(12)
                        .setBold())
                .setBorder(new SolidBorder(PRIMARY_COLOR, 1))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setPadding(10);
        gstTable.addCell(gstCell);
        document.add(gstTable);
        document.add(new Paragraph("\n"));*/

        // Bank Details Section with styling
//        Table bankHeaderTable = new Table(1).useAllAvailableWidth();
//        Cell bankHeaderCell = new Cell()
//                .add(new Paragraph("BANK DETAILS")
//                        .setFontColor(TEXT_LIGHT)
//                        .setBold()
//                        .setFontSize(10))
//                .setBackgroundColor(PRIMARY_COLOR)
//                .setPadding(8)
//                .setTextAlignment(TextAlignment.CENTER);
//        bankHeaderTable.addCell(bankHeaderCell);
//        document.add(bankHeaderTable);
//
//        // Create a table with 2 columns for bank details and payment image
//        Table bankDetailsTable = new Table(2).useAllAvailableWidth();
//        bankDetailsTable.setMarginTop(10);
//
//        // Left column - Bank details
//        Cell bankDetailsCell = new Cell().setBorder(new SolidBorder(SECONDARY_COLOR, 1));
//        Table bankTable = new Table(2).useAllAvailableWidth();
//
//        addStyledBankDetail(bankTable, "GST: ", "24DMAPP6011D1ZL");
//        addStyledBankDetail(bankTable, "BANK: ", "");
//        addStyledBankDetail(bankTable, "A/C NAME:", "SEVERAL INDUSTRIES");
//        addStyledBankDetail(bankTable, "A/C NO:", "");
//        addStyledBankDetail(bankTable, "IFSC CODE:", "");
//        addStyledBankDetail(bankTable, "BRANCH:", "RAJKOT");
//        addStyledBankDetail(bankTable, "UPI:", "");
//
//        bankDetailsCell.add(bankTable);
//        bankDetailsCell.setBackgroundColor(BACKGROUND_LIGHT);
//        bankDetailsCell.setPadding(10);
//
//        // Add only bank detail cell (removed payment image as requested)
//        bankDetailsTable.addCell(bankDetailsCell);
//        // Add an empty placeholder cell to keep layout tidy
//        bankDetailsTable.addCell(new Cell().setBorder(new SolidBorder(SECONDARY_COLOR, 1))
//                .add(new Paragraph(" ")).setBackgroundColor(BACKGROUND_LIGHT));
//
//        document.add(bankDetailsTable);

        // Terms and Conditions Section with styling
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

        // Compact terms: small bullets, tight line height
        Table termsTable = new Table(new float[]{1}).useAllAvailableWidth();
        Cell termsCell = new Cell()
                .setBorder(new SolidBorder(SECONDARY_COLOR, 1))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setPadding(2);

        addCompactTerm(termsCell, "Customer will be billed after indicating acceptance of this quote.");
        addCompactTerm(termsCell, "Payment 50% Advance and 50% before goods dispatched.");
        addCompactTerm(termsCell, "Transport transaction extra.");
        addCompactTerm(termsCell, "Subject to Rajkot jurisdiction.");

        termsTable.addCell(termsCell);
        document.add(termsTable);
    }

    private void addStyledBankDetail(Table table, String label, String value) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label)
                        .setBold()
                        .setFontColor(SECONDARY_COLOR)
                        .setFontSize(8))
                .setBorder(Border.NO_BORDER)
                .setPadding(5);
                
        Cell valueCell = new Cell()
                .add(new Paragraph(value)
                        .setFontColor(TEXT_DARK)
                        .setFontSize(8))
                .setBorder(Border.NO_BORDER)
                .setPadding(5);
                
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addPremiumTerm(Cell container, String number, String text) {
        // Create styled term with icon effect
        Table termTable = new Table(2).useAllAvailableWidth();
        
        // Number cell styled as a badge - reduced size
        Cell numberCell = new Cell()
                .add(new Paragraph(number)
                        .setFontColor(PRIMARY_COLOR)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(PRIMARY_COLOR)
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
                
        // Text cell with term content
        Cell textCell = new Cell()
                .add(new Paragraph(text)
                        .setFontColor(TEXT_DARK))
                .setBorder(Border.NO_BORDER)
                .setPaddingLeft(10);
                
        termTable.addCell(numberCell);
        termTable.addCell(textCell);
        
        container.add(termTable);
        container.add(new Paragraph("\n").setFontSize(5)); // Small spacing between terms
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

    // Removed decorative last page method as per request
    
    private void addPageFooter(PdfDocument pdfDoc, Document document, int pageNumber) {
        float footerY = 20;  // Distance from bottom
        float pageWidth = pdfDoc.getDefaultPageSize().getWidth();

        // Create styled footer with background
        Table footerBgTable = new Table(1)
                .useAllAvailableWidth()
                .setFixedPosition(36, footerY - 6, pageWidth - 72);
                
        Cell footerBgCell = new Cell()
                .setHeight(20)
                .setBackgroundColor(SECONDARY_COLOR)
                .setBorder(Border.NO_BORDER);
        footerBgTable.addCell(footerBgCell);

        // Create footer content table
        Table footerTable = new Table(3)
                .useAllAvailableWidth()
                .setFixedPosition(36, footerY, pageWidth - 72);

        // Left side - Page number
        Cell pageCell = new Cell()
                .add(new Paragraph("Page " + pageNumber)
                        .setFontSize(8)
                        .setFontColor(TEXT_LIGHT))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT);
                
        // Center - Contact information
        Cell contactCell = new Cell()
                .add(new Paragraph("Several Polymers")
                        .setFontSize(7)
                        .setFontColor(TEXT_LIGHT))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT);
                
        // Right - Website or additional info
        Cell websiteCell = new Cell()
                .add(new Paragraph("https://severalpolymers.in/")
                        .setFontSize(7)
                        .setFontColor(TEXT_LIGHT))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);

        // footerTable.addCell(pageCell);
        footerTable.addCell(contactCell);
        footerTable.addCell(websiteCell);

        // Add both tables to document
        document.add(footerBgTable);
        document.add(footerTable);
    }
    
    // Helper method to convert HTML to formatted paragraph
    private Paragraph convertHtmlToParagraph(Map<String, Object> item, boolean isPrintImage) {
        Paragraph paragraph = new Paragraph();
        Object rawName = item.get("productName");
        String html = rawName != null ? rawName.toString() : "";

        // Remove any null or empty strings
        if (html == null || html.trim().isEmpty()) {
            return paragraph;
        }

        // First handle HTML tags
        String[] parts = html.split("(<b>|</b>)");
        boolean isBold = false;

        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                Text text = new Text(part);
                if (isBold) {
                    text.setBold();
                    text.setFontColor(PRIMARY_COLOR); // Add color to bold text for look
                } else {
                    text.setFontColor(TEXT_DARK);
                }
                paragraph.add(text);
                isBold = !isBold;
            }
        }

        return paragraph;
    }
}
