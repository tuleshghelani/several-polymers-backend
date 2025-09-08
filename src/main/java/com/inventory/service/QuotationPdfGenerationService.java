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
    // Color scheme as per requirements
    private static final Color PRIMARY_COLOR = new DeviceRgb(245, 106, 73);     // #f56a49
    private static final Color SECONDARY_COLOR = new DeviceRgb(0, 63, 105);     // #003f69
    private static final Color PRIMARY_LIGHT = new DeviceRgb(255, 139, 115);    // #ff8b73
    private static final Color SECONDARY_LIGHT = new DeviceRgb(0, 92, 158);     // #005c9e
    private static final Color TEXT_DARK = new DeviceRgb(51, 51, 51);           // #333333
    private static final Color TEXT_LIGHT = new DeviceRgb(255, 255, 255);       // #ffffff
    private static final Color BACKGROUND_LIGHT = new DeviceRgb(245, 245, 245); // #f5f5f5

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

            addLastPage(document);
            addPageFooter(pdf, document, 4);

            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error generating PDF", e);
            throw new ValidationException("Failed to generate PDF: " + e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void addHeader(Document document, Map<String, Object> data) {
        
        // First add the logo in the center
        Table logoTable = new Table(1).useAllAvailableWidth();
        Cell logoCell = new Cell();
        try {
            InputStream imageStream = getClass().getClassLoader().getResourceAsStream("quotation/jk_logo.png");
            if (imageStream == null) {
                log.error("Image not found: quotation/jk_logo.png");
                throw new FileNotFoundException("Image not found: quotation/jk_logo.png");
            }
            log.info("Successfully loaded logo image: quotation/jk_logo.png");
            ImageData imageData = ImageDataFactory.create(imageStream.readAllBytes());
            Image img = new Image(imageData);
            img.setWidth(200);
            img.setHeight(50);
            // Center the image horizontally
            img.setHorizontalAlignment(HorizontalAlignment.CENTER);
            logoCell.add(img);
        } catch (Exception e) {
            log.error("Error loading logo image: quotation/jk_logo.png", e);
        }
        logoCell.setBorder(new SolidBorder(SECONDARY_COLOR, 1))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setPadding(5)
                .setTextAlignment(TextAlignment.CENTER);
        logoTable.addCell(logoCell);
        document.add(logoTable);
        
        // Logo and details in separate table - immediately after logo without spacing
        Table contentTable = new Table(1).useAllAvailableWidth();
        // Removed margin between logo and content table

        // Left side - Details with styled background
        Cell detailsCell = new Cell();
        detailsCell.add(new Paragraph("Address :- Radhekrishan Chowk, Sojitra park, Mavdi,")
                        .setFontSize(10)
                        .setFontColor(TEXT_DARK))
                .add(new Paragraph("baypass road, Dist. Rajkot, Gujarat - 360005")
                        .setFontSize(10)
                        .setFontColor(TEXT_DARK))
                .add(new Paragraph("E-mail: jkindustries1955@gmail.com")
                        .setFontSize(10)
                        .setFontColor(TEXT_DARK))
                .add(new Paragraph("Mo.No. 9979032430")
                        .setFontSize(10)
                        .setFontColor(TEXT_DARK))
                .add(new Paragraph("GST NO.24AAMFJ9388A1Z4")
                        .setFontSize(11)
                        .setBold()
                        .setFontColor(PRIMARY_COLOR))
                .setBorder(new SolidBorder(PRIMARY_COLOR, 1))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setPadding(10)
                .setTextAlignment(TextAlignment.LEFT);

        contentTable.addCell(detailsCell);
        document.add(contentTable);

        // Add styled quotation heading with appearance
        Table quotationHeadingTable = new Table(1).useAllAvailableWidth();
        Cell quotationHeadingCell = new Cell()
                .add(new Paragraph("Quotation")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(14)
                        .setBold()
                        .setFontColor(TEXT_LIGHT))
                .setBackgroundColor(PRIMARY_COLOR)
                .setPadding(1);
        quotationHeadingTable.addCell(quotationHeadingCell);
        
        document.add(new Paragraph("\n"));
        document.add(quotationHeadingTable);
    }

    private void addQuotationDetails(Document document, Map<String, Object> data) {
        document.add(new Paragraph("\n"));

        // Create a styled box for customer details
        Table infoTable = new Table(1).useAllAvailableWidth();
        Cell infoCell = new Cell();
        
        // Customer details with highlighting
        // Paragraph customerTitle = new Paragraph("Client Information")
        //         .setBold()
        //         .setFontSize(12)
        //         .setFontColor(SECONDARY_COLOR);
        
        Paragraph customerName = new Paragraph(data.get("customerName").toString())
                .setBold()
                .setFontSize(12)
                .setFontColor(PRIMARY_COLOR);
        
        Table detailsTable = new Table(2).useAllAvailableWidth();
        
        // Quote details with styling
        addDetailRow(detailsTable, "Quote Number", data.get("quoteNumber").toString());
        addDetailRow(detailsTable, "Quote Date", data.get("quoteDate").toString());
        addDetailRow(detailsTable, "Valid Until", data.get("validUntil").toString());
        
        if (data.get("contactNumber") != null) {
            addDetailRow(detailsTable, "Mobile No.", data.get("contactNumber").toString());
        }
        
        infoCell.add(customerName)
                .add(detailsTable)
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setBorder(new SolidBorder(SECONDARY_LIGHT, 1))
                .setPadding(15);
                
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
                .add(new Paragraph(label + ":").setBold().setFontColor(SECONDARY_COLOR))
                .setBorder(Border.NO_BORDER);
        
        Cell valueCell = new Cell()
                .add(new Paragraph(value).setFontColor(TEXT_DARK))
                .setBorder(Border.NO_BORDER);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addItemsTable(Document document, List<Map<String, Object>> items, Map<String, Object> quotationData) {
        document.add(new Paragraph("\n"));
        
        // Add title for items section
        Paragraph itemsTitle = new Paragraph("Items & Services")
                .setBold()
                .setFontSize(16)
                .setFontColor(SECONDARY_COLOR)
                .setMarginBottom(10);
        document.add(itemsTitle);
        
        // Create styled table
        Table table = new Table(new float[]{1, 4, 2, 2, 1, 2})
                .useAllAvailableWidth()
                .setMarginTop(5);

        // Add headers with styling
        Stream.of("No.", "ITEM NAME", "QUANTITY", "PRICE", "DISCOUNT.(%)", "TOTAL AMOUNT")
                .forEach(title -> table.addHeaderCell(
                        new Cell().add(new Paragraph(title))
                                .setBackgroundColor(SECONDARY_COLOR)
                                .setFontColor(TEXT_LIGHT)
                                .setBold()
                                .setPadding(0)
                ));

        // Add items with alternating row colors for look
        AtomicInteger counter = new AtomicInteger(1);
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map<String, Object> item : items) {
            boolean isEvenRow = counter.get() % 2 == 0;
            Color rowColor = isEvenRow ? BACKGROUND_LIGHT : ColorConstants.WHITE;
            
            table.addCell(new Cell()
                    .add(new Paragraph(String.valueOf(counter.getAndIncrement())))
                    .setBackgroundColor(rowColor));
                    
            table.addCell(new Cell()
                    .add(convertHtmlToParagraph(item, true))
                    .setBackgroundColor(rowColor));
                    
            String measurement = item.get("measurement") != null ? item.get("measurement").toString().trim() : "";
            String displayMeasurement = measurement.toLowerCase().equals("kg") ? 
                    "\n " + measurement + "(approx.)" : "\n" + measurement;

            table.addCell(new Cell()
                    .add(new Paragraph(((BigDecimal) item.get("quantity")).setScale(0, RoundingMode.DOWN).toString() + " " + displayMeasurement))
                    .setBackgroundColor(rowColor));
                    
            table.addCell(new Cell()
                    .add(new Paragraph(item.get("unitPrice").toString()))
                    .setBackgroundColor(rowColor));
                    
            table.addCell(new Cell()
                .add(new Paragraph(item.get("discountPercentage").toString()))
                .setBackgroundColor(rowColor));
                    
            table.addCell(new Cell()
                    .add(new Paragraph(item.get("discountPrice").toString()))
                    .setBackgroundColor(rowColor));

            totalAmount = totalAmount.add(new BigDecimal(item.get("discountPrice").toString()));
        }

        document.add(table);

        // Create summary table with styling
        Table summaryTable = new Table(2)
                .useAllAvailableWidth()
                .setMarginTop(20);
                
        summaryTable.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setWidth(350)); // Empty cell for spacing
                
        // Right side - totals with styling
        Cell totalsCell = new Cell();
        Table totalsTable = new Table(2).useAllAvailableWidth();
        
        // Add styled rows for totals
        addTotalRow(totalsTable, "SUBTOTAL", totalAmount.toString() + "/-", false);

        // Quotation discount
        BigDecimal quotationDiscountPercentage = ((BigDecimal) quotationData.get("quotationDiscountPercentage"));
        addTotalRow(totalsTable, "Quotation discount (%)", quotationDiscountPercentage.toString() + "%", false);

        // Quotation discount in Rs
        BigDecimal quotationDiscountAmount = ((BigDecimal) quotationData.get("quotationDiscountAmount"));
        addTotalRow(totalsTable, "Quotation discount (Rs.)", quotationDiscountAmount.toString() + "/-", false);
        
        // GST calculation
        BigDecimal gstAmount = ((BigDecimal) quotationData.get("quotationTaxAmount")).setScale(0, RoundingMode.HALF_UP);
        addTotalRow(totalsTable, "GST 18 % (SGST 9% CGST 9%)", gstAmount.toString() + "/-", false);
        
        // Grand total with prominent styling
        BigDecimal grandTotal = ((BigDecimal) quotationData.get("totalAmount")).setScale(0, RoundingMode.HALF_UP);
        addTotalRow(totalsTable, "GRAND TOTAL", grandTotal.toString() + "/-", true);
        
        totalsCell.add(totalsTable)
                .setBorder(new SolidBorder(SECONDARY_COLOR, 1))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setPadding(10);
                
        summaryTable.addCell(new Cell().setBorder(Border.NO_BORDER)); // Empty cell
        summaryTable.addCell(totalsCell);
        
        document.add(summaryTable);
    }
    
    private void addTotalRow(Table table, String label, String value, boolean isGrandTotal) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label)
                        .setBold()
                        .setFontColor(isGrandTotal ? PRIMARY_COLOR : SECONDARY_COLOR))
                .setBorder(Border.NO_BORDER);
                
        Cell valueCell = new Cell()
                .add(new Paragraph(value)
                        .setBold()
                        .setFontColor(isGrandTotal ? PRIMARY_COLOR : TEXT_DARK))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);
                
        if (isGrandTotal) {
            // Add top border for grand total
            labelCell.setBorderTop(new SolidBorder(SECONDARY_COLOR, 1));
            valueCell.setBorderTop(new SolidBorder(SECONDARY_COLOR, 1));
            
            // Increase font size for grand total
            labelCell.setFontSize(14);
            valueCell.setFontSize(14);
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

    private Table createMMCalculationTable(List<Map<String, Object>> calculations) {
        Table table = new Table(new float[]{2, 2, 2, 2, 2})
                .useAllAvailableWidth()
                .setMarginTop(5);

        // Add headers with styling
        Stream.of("MM", "R.Feet", "Nos", "Sq. Meter", "Sq.Feet")
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
            BigDecimal mm = toBigDecimal(calc.get("mm"));
            BigDecimal meter = mm.divide(MM_TO_METER, 4, RoundingMode.HALF_UP);
            BigDecimal sqFeet = toBigDecimal(calc.get("sqFeet"));

            // MM column
            table.addCell(new Cell()
                    .add(new Paragraph(formatValue(calc.get("mm"))))
                    .setBackgroundColor(PRIMARY_LIGHT));

            // R.Feet column
            table.addCell(new Cell()
                    .add(new Paragraph(formatValue(calc.get("runningFeet"))))
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
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        // Add styled section title
        Table titleTable = new Table(1).useAllAvailableWidth();
        Cell titleCell = new Cell()
                .add(new Paragraph("PAYMENT & TERMS")
                        .setFontSize(20)
                        .setBold()
                        .setFontColor(TEXT_LIGHT))
                .setBackgroundColor(SECONDARY_COLOR)
                .setPadding(10)
                .setTextAlignment(TextAlignment.CENTER);
        titleTable.addCell(titleCell);
        document.add(titleTable);
        document.add(new Paragraph("\n"));

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
        Table bankHeaderTable = new Table(1).useAllAvailableWidth();
        Cell bankHeaderCell = new Cell()
                .add(new Paragraph("BANK DETAILS")
                        .setFontColor(TEXT_LIGHT)
                        .setBold()
                        .setFontSize(14))
                .setBackgroundColor(PRIMARY_COLOR)
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER);
        bankHeaderTable.addCell(bankHeaderCell);
        document.add(bankHeaderTable);
        
        // Create a table with 2 columns for bank details and payment image
        Table bankDetailsTable = new Table(2).useAllAvailableWidth();
        bankDetailsTable.setMarginTop(10);
        
        // Left column - Bank details
        Cell bankDetailsCell = new Cell().setBorder(new SolidBorder(SECONDARY_COLOR, 1));
        Table bankTable = new Table(2).useAllAvailableWidth();
        
        addStyledBankDetail(bankTable, "GST: ", "24AAMFJ9388A1Z4");
        addStyledBankDetail(bankTable, "BANK: ", "ICICI INDIA");
        addStyledBankDetail(bankTable, "A/C NAME:", "JK INDUSTIES");
        addStyledBankDetail(bankTable, "A/C NO:", "820205000035");
        addStyledBankDetail(bankTable, "IFSC CODE:", "ICIC0006248/ ICIC0008202");
        addStyledBankDetail(bankTable, "BRANCH:", "RAJKOT");
        addStyledBankDetail(bankTable, "UPI:", "MSJKINDUSTRIES.eazypay3@icici");

        bankDetailsCell.add(bankTable);
        bankDetailsCell.setBackgroundColor(BACKGROUND_LIGHT);
        bankDetailsCell.setPadding(10);
        
        // Right column - Payment image
        Cell paymentImageCell = new Cell().setBorder(new SolidBorder(SECONDARY_COLOR, 1));
        paymentImageCell.setPadding(10);
        
        try {
            InputStream imageStream = getClass().getClassLoader().getResourceAsStream("quotation/payment.jpg");
            if (imageStream == null) {
                log.error("Image not found: quotation/payment.jpg");
                throw new FileNotFoundException("Image not found: quotation/payment.jpg");
            }
            
            ImageData imageData = ImageDataFactory.create(imageStream.readAllBytes());
            Image img = new Image(imageData);
            img.setAutoScale(true);
            img.setHorizontalAlignment(HorizontalAlignment.CENTER);
            
            paymentImageCell.add(img);
        } catch (Exception e) {
            log.error("Error loading payment image: quotation/payment.jpg", e);
            paymentImageCell.add(new Paragraph("Payment Image Not Available")
                    .setFontColor(PRIMARY_COLOR)
                    .setTextAlignment(TextAlignment.CENTER));
        }
        
        // Add cells to table
        bankDetailsTable.addCell(bankDetailsCell);
        bankDetailsTable.addCell(paymentImageCell);
        
        document.add(bankDetailsTable);

        // Terms and Conditions Section with styling
        document.add(new Paragraph("\n"));
        Table termsHeaderTable = new Table(1).useAllAvailableWidth();
        Cell termsHeaderCell = new Cell()
                .add(new Paragraph("TERMS AND CONDITIONS")
                        .setFontColor(TEXT_LIGHT)
                        .setBold()
                        .setFontSize(14))
                .setBackgroundColor(PRIMARY_COLOR)
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER);
        termsHeaderTable.addCell(termsHeaderCell);
        document.add(termsHeaderTable);
        document.add(new Paragraph("\n"));

        // Terms in a styled box
        Table termsTable = new Table(1).useAllAvailableWidth();
        Cell termsCell = new Cell()
                .setBorder(new SolidBorder(SECONDARY_COLOR, 1))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setPadding(15);
        
        // Add individual terms with styling
        addPremiumTerm(termsCell, "1", "Customer will be billed after indicating acceptance of this quote.");
        addPremiumTerm(termsCell, "2", "Payment 50% Advance And 50% before goods Dispatched.");
        addPremiumTerm(termsCell, "3", "Transport Transaction Extra");
        addPremiumTerm(termsCell, "4", "SUBJECT TO RAJKOT JURISDICTION.");
        
        termsTable.addCell(termsCell);
        document.add(termsTable);
    }

    private void addStyledBankDetail(Table table, String label, String value) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label)
                        .setBold()
                        .setFontColor(SECONDARY_COLOR))
                .setBorder(Border.NO_BORDER)
                .setPadding(5);
                
        Cell valueCell = new Cell()
                .add(new Paragraph(value)
                        .setFontColor(TEXT_DARK))
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

    private void addLastPage(Document document) {
        // Start new page
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        try {
            InputStream imageStream = getClass().getClassLoader().getResourceAsStream("quotation/Quotation_last_page.jpg");
            if (imageStream == null) {
                throw new FileNotFoundException("Image not found: quotation/Quotation_last_page.jpg");
            }
            ImageData imageData = ImageDataFactory.create(imageStream.readAllBytes());
            Image img = new Image(imageData);

            // Get page dimensions
            float pageWidth = document.getPdfDocument().getDefaultPageSize().getWidth();
            float pageHeight = document.getPdfDocument().getDefaultPageSize().getHeight();
    
            // Set image to fill the entire page
            img.setFixedPosition(0, 0);  // Start from top-left corner
            img.scaleToFit(pageWidth, pageHeight);
            img.setMargins(0, 0, 0, 0);  // Remove all margins
    
            document.add(img);
        } catch (Exception e) {
            log.error("Error loading last page image", e);
            e.printStackTrace();
        }
    }
    
    private void addPageFooter(PdfDocument pdfDoc, Document document, int pageNumber) {
        float footerY = 20;  // Distance from bottom
        float pageWidth = pdfDoc.getDefaultPageSize().getWidth();

        // Create styled footer with background
        Table footerBgTable = new Table(1)
                .useAllAvailableWidth()
                .setFixedPosition(36, footerY - 10, pageWidth - 72);
                
        Cell footerBgCell = new Cell()
                .setHeight(30)
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
                .add(new Paragraph("JK Industies [ CONTACT NO. 9979032430 ]")
                        .setFontSize(8)
                        .setFontColor(TEXT_LIGHT))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT);
                
        // Right - Website or additional info
        Cell websiteCell = new Cell()
                .add(new Paragraph("https://jkindustriesrajkot.com/")
                        .setFontSize(8)
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
        String html = item.get("productName").toString();

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
