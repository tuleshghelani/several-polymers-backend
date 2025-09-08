package com.inventory.service;

import com.inventory.dao.TransportDao;
import com.inventory.dto.TransportPdfDto;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TransportPdfGenerationService {
    private final TransportDao transportDao;
    private final UtilityService utilityService;

    public byte[] generateTransportPdf(TransportPdfDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Map<String, Object> transportData = transportDao.getTransportPdfData(dto.getId(), currentUser.getClient().getId());
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, out);
            
            document.open();
            addHeader(document);
            addCustomerDetails(document, transportData);
            addTransportDetails(document, transportData);
            addBagsTable(document, (List<Map<String, Object>>) transportData.get("bags"));
            document.close();
            
            return out.toByteArray();
        } catch (Exception e) {
            throw new ValidationException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private void addHeader(Document document) throws DocumentException {
        Font titleFont = new Font(Font.FontFamily.TIMES_ROMAN, 20, Font.BOLD);
        Font dateFont = new Font(Font.FontFamily.TIMES_ROMAN, 12);
        
        // Add title with background
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        
        PdfPCell titleCell = new PdfPCell(new Phrase("JK INDUSTRIES TRANSPORT", titleFont));
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setPadding(10);
        titleCell.setBackgroundColor(new BaseColor(240, 240, 240));
        titleCell.setBorder(Rectangle.BOX);
        headerTable.addCell(titleCell);
        
        document.add(headerTable);
        document.add(Chunk.NEWLINE);
        
        // Add date
        Paragraph date = new Paragraph(
            "Date: " + java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")), 
            dateFont
        );
        date.setAlignment(Element.ALIGN_RIGHT);
        document.add(date);
        document.add(Chunk.NEWLINE);
    }

    private void addCustomerDetails(Document document, Map<String, Object> data) throws DocumentException {
        Font normalFont = new Font(Font.FontFamily.TIMES_ROMAN, 12);
        Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
        
        PdfPTable customerTable = new PdfPTable(2);
        customerTable.setWidthPercentage(100);
        
        // Add styled customer details
        PdfPCell headerCell = new PdfPCell(new Phrase("Customer Details", boldFont));
        headerCell.setColspan(2);
        headerCell.setBackgroundColor(new BaseColor(240, 240, 240));
        headerCell.setPadding(5);
        customerTable.addCell(headerCell);
        
        addDetailRow(customerTable, "Name", data.get("customerName").toString(), normalFont);
        
        if (data.get("customerAddress") != null) {
            addDetailRow(customerTable, "Address", data.get("customerAddress").toString(), normalFont);
        }
        
        if (data.get("customerMobile") != null) {
            addDetailRow(customerTable, "Mobile", data.get("customerMobile").toString(), normalFont);
        }
        
        if (data.get("customerGst") != null) {
            addDetailRow(customerTable, "GST No", data.get("customerGst").toString(), normalFont);
        }
        
        document.add(customerTable);
        document.add(Chunk.NEWLINE);
    }

    private void addTransportDetails(Document document, Map<String, Object> data) throws DocumentException {
        Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
        
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        
        addDetailRow(table, "Total Bags", data.get("totalBags").toString(), boldFont);
        addDetailRow(table, "Total Weight", data.get("totalWeight").toString(), boldFont);
        
        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addBagsTable(Document document, List<Map<String, Object>> bags) throws DocumentException {
        Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.TIMES_ROMAN, 12);
        int currentBagNumber = 1;

        for (Map<String, Object> bag : bags) {
            PdfPTable bagHeader = new PdfPTable(2);
            bagHeader.setWidthPercentage(100);
            
            // Calculate bag number range
            int numberOfBags = ((Number) bag.get("numberOfBags")).intValue();
            String bagNumberText = numberOfBags > 1 
                ? "Bag #" + currentBagNumber + "-" + (currentBagNumber + numberOfBags - 1)
                : "Bag #" + currentBagNumber;
            
            PdfPCell bagTitleCell = new PdfPCell(new Phrase(bagNumberText, boldFont));
            bagTitleCell.setColspan(2);
            bagTitleCell.setBackgroundColor(new BaseColor(230, 230, 250));
            bagTitleCell.setPadding(5);
            bagHeader.addCell(bagTitleCell);
            
            addDetailRow(bagHeader, "Weight", bag.get("weight").toString() + " kg", normalFont);
            document.add(bagHeader);

            // Items table with improved styling
            PdfPTable itemsTable = new PdfPTable(3);
            itemsTable.setWidthPercentage(100);
            float[] columnWidths = new float[]{40f, 20f, 25f};
            itemsTable.setWidths(columnWidths);
            
            // Add headers with background
            Stream.of("Product Name", "Quantity", "Remarks")
                .forEach(header -> {
                    PdfPCell cell = new PdfPCell(new Phrase(header, boldFont));
                    cell.setBackgroundColor(new BaseColor(245, 245, 245));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setPadding(5);
                    itemsTable.addCell(cell);
                });

            // Add items
            List<Map<String, Object>> items = (List<Map<String, Object>>) bag.get("items");
            for (Map<String, Object> item : items) {
                addCell(itemsTable, item.get("productName").toString(), normalFont, Element.ALIGN_LEFT);
                
                // Calculate quantity per bag
                Object quantityObj = item.get("quantity");
                Object numberOfBagsObj = bag.get("numberOfBags");
                String quantityDisplay;
                
                if (numberOfBagsObj != null && ((Number) numberOfBagsObj).intValue() > 0) {
                    double quantity = ((Number) quantityObj).doubleValue();
                    int tempNumberOfBags = ((Number) numberOfBagsObj).intValue();
                    quantityDisplay = String.format("%.2f", quantity / tempNumberOfBags);
                } else {
                    quantityDisplay = quantityObj.toString();
                }
                
                addCell(itemsTable, quantityDisplay, normalFont, Element.ALIGN_RIGHT);
                addCell(itemsTable, item.get("remarks").toString(), normalFont, Element.ALIGN_LEFT);
            }

            document.add(itemsTable);
            document.add(Chunk.NEWLINE);

            currentBagNumber += numberOfBags;
        }
    }

    private void addDetailRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label + ": ", font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(valueCell);
    }

    private void addCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        table.addCell(cell);
    }
} 