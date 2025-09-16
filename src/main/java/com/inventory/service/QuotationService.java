package com.inventory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.inventory.dto.request.QuotationItemRequestDto;
import com.inventory.dto.request.QuotationItemCreatedRollUpdateDto;
import com.inventory.dto.request.QuotationItemProductionUpdateDto;
import com.inventory.dto.request.QuotationItemNumberOfRollUpdateDto;
import com.inventory.dto.request.QuotationItemStatusUpdateDto;
import com.inventory.dto.request.QuotationRequestDto;
import com.inventory.dto.request.QuotationItemRequestDto;
import com.inventory.entity.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.dao.QuotationDao;
import com.inventory.dao.QuotationItemDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.QuotationDto;
import com.inventory.enums.QuotationStatus;
import com.inventory.enums.QuotationStatusItem;
import com.inventory.exception.ValidationException;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.BrandRepository;
import com.inventory.repository.TransportMasterRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.QuotationItemRepository;
import com.inventory.repository.QuotationRepository;
import com.inventory.repository.PurchaseRepository;
import com.inventory.repository.SaleRepository;
 

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotationService {
    private static final BigDecimal INCHES_IN_FOOT = BigDecimal.valueOf(12);
    private static final BigDecimal SQ_FEET_MULTIPLIER = BigDecimal.valueOf(3.5);
    private static final BigDecimal DEFAULT_TAX_PERCENTAGE = BigDecimal.valueOf(18);
    private static final BigDecimal MM_TO_FEET_CONVERSION = BigDecimal.valueOf(304.8);

    private static final int WEIGHT_SCALE = 3;
    private static final RoundingMode WEIGHT_ROUNDING = RoundingMode.HALF_UP;

    private static final BigDecimal SINGLE_MULTIPLIER = BigDecimal.valueOf(1.16);
    private static final BigDecimal DOUBLE_MULTIPLIER = BigDecimal.valueOf(2.0);
    private static final BigDecimal FULL_SHEET_MULTIPLIER = BigDecimal.valueOf(4.0);

    private static final BigDecimal SQ_FEET_TO_METER = BigDecimal.valueOf(10.764);
    private static final BigDecimal MM_TO_METER = BigDecimal.valueOf(1000);

    private final QuotationRepository quotationRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UtilityService utilityService;
    private final QuotationDao quotationDao;
    private final QuotationItemDao quotationItemDao;
    private final QuoteNumberGeneratorService quoteNumberGeneratorService;
    private final PdfGenerationService pdfGenerationService;
    private final QuotationPdfGenerationService quotationPdfGenerationService;
    private final PurchaseRepository purchaseRepository;
    private final SaleRepository saleRepository;
    private final BrandRepository brandRepository;
    private final TransportMasterRepository transportMasterRepository;
    private final DispatchSlipPdfService dispatchSlipPdfService;

//    private final ProductQuantityService productQuantityService;
//    private final QuotationItemCalculationRepository quotationItemCalculationRepository;
//    private final DispatchSlipPdfService dispatchSlipPdfService;

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> createQuotation(QuotationRequestDto request) {
        try {
            validateQuotationRequest(request);
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Quotation quotation = new Quotation();

            // if(request.getQuotationId() != null){
            //     quotation = quotationRepository.findById(request.getQuotationId())
            //     .orElseThrow(() -> new ValidationException("Quotation not found"));

            //     if(quotation.getClient().getId() != currentUser.getClient().getId()){
            //         throw new ValidationException("Unauthorized access to quotation");
            //     }
            // }
            if(request.getCustomerId() != null){
                Customer customer = customerRepository.findById(request.getCustomerId())
                        .orElseThrow(() -> new ValidationException("Customer not found"));
                quotation.setCustomer(customer);
                quotation.setCustomerName(customer.getName());
            } else {
                quotation.setCustomerName(request.getCustomerName());
            }

            quotation.setQuoteDate(request.getQuoteDate());
            quotation.setValidUntil(request.getValidUntil());
            quotation.setRemarks(request.getRemarks());
            quotation.setTermsConditions(request.getTermsConditions());
            quotation.setContactNumber(request.getContactNumber());
            quotation.setAddress(request.getAddress());
            quotation.setReferenceName(request.getReferenceName());
            // New fields
            if (request.getTransportMasterId() != null) {
                TransportMaster transportMaster = transportMasterRepository.findById(request.getTransportMasterId())
                        .orElseThrow(() -> new ValidationException("Transport master not found"));
                quotation.setTransportMaster(transportMaster);
            } else {
                quotation.setTransportMaster(null);
            }
            quotation.setCaseNumber(request.getCaseNumber());
            quotation.setStatus(QuotationStatus.Q);
            quotation.setClient(currentUser.getClient());
            quotation.setCreatedBy(currentUser);

            // Generate quote number
            String quoteNumber = quoteNumberGeneratorService.generateQuoteNumber(currentUser.getClient());
            System.out.println("Generated quote number: " + quoteNumber);

            // Set the generated quote number
            quotation.setQuoteNumber(quoteNumber);

            quotation = quotationRepository.save(quotation);

            List<QuotationItem> items = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal taxAmount = BigDecimal.ZERO;
            BigDecimal discountedPrice = BigDecimal.ZERO;
            BigDecimal quotationDiscountAmount = BigDecimal.ZERO;
            BigDecimal quotationDiscountPrice = BigDecimal.ZERO;

            for (QuotationItemRequestDto itemDto : request.getItems()) {
                QuotationItem item = createQuotationItem(itemDto, quotation, currentUser, request);
                items.add(item);
                totalAmount = totalAmount.add(item.getFinalPrice());
                taxAmount = taxAmount.add(item.getTaxAmount());
                discountedPrice = discountedPrice.add(item.getDiscountPrice());
                quotationDiscountAmount = quotationDiscountAmount.add(item.getQuotationDiscountAmount());
                quotationDiscountPrice = quotationDiscountPrice.add(item.getQuotationDiscountPrice());
            }

            quotationItemRepository.saveAll(items);

            BigDecimal packagingAndForwadingCharges = request.getPackagingAndForwadingCharges() != null ? request.getPackagingAndForwadingCharges() : BigDecimal.ZERO;
            quotation.setPackagingAndForwadingCharges(packagingAndForwadingCharges);
            totalAmount = totalAmount.add(packagingAndForwadingCharges);
            totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP);
            quotation.setTotalAmount(totalAmount);
            quotation.setTaxAmount(taxAmount);
            // discountedPrice should reflect sum of item discountPrice (pre-tax)
            quotation.setDiscountedPrice(discountedPrice);
            quotation.setQuotationDiscountPercentage(request.getQuotationDiscountPercentage());
            quotation.setQuotationDiscountAmount(quotationDiscountAmount);
            quotationRepository.save(quotation);

            return ApiResponse.success("Quotation created successfully");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error creating quotation", e);
            throw new ValidationException("Failed to create quotation: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> updateQuotationItemCreatedRoll(QuotationItemCreatedRollUpdateDto request) {
        try {
            if (request.getId() == null) {
                throw new ValidationException("Quotation item id is required");
            }
            if (request.getCreatedRoll() == null || request.getCreatedRoll() < 0) {
                throw new ValidationException("createdRoll must be zero or positive");
            }

            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            QuotationItem item = quotationItemRepository.findById(request.getId())
                    .orElseThrow(() -> new ValidationException("Quotation item not found"));

            if (!item.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized access to quotation item");
            }

            int updated = quotationItemRepository.updateCreatedRollById(request.getId(), request.getCreatedRoll());
            if (updated == 0) {
                throw new ValidationException("Failed to update createdRoll");
            }

            return ApiResponse.success("Created roll updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update created roll: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> updateQuotationItemNumberOfRoll(QuotationItemNumberOfRollUpdateDto request) {
        try {
            if (request.getId() == null) {
                throw new ValidationException("Quotation item id is required");
            }
            if (request.getNumberOfRoll() == null || request.getNumberOfRoll() < 0) {
                throw new ValidationException("numberOfRoll must be zero or positive");
            }

            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            QuotationItem item = quotationItemRepository.findById(request.getId())
                    .orElseThrow(() -> new ValidationException("Quotation item not found"));

            if (!item.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized access to quotation item");
            }
            if(!Objects.isNull(item.getQuotationItemStatus()) && item.getQuotationItemStatus().equals(QuotationStatusItem.B.value)) {
                throw new ValidationException("Number of roll cannot be updated for billed items");
            }
            item.setNumberOfRoll(request.getNumberOfRoll());
            quotationItemRepository.save(item);
            return ApiResponse.success("Number of roll updated successfully");
        } catch (ValidationException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to update number of roll: " + e.getMessage());
        }
    }

    public ApiResponse<Map<String, Object>> searchQuotationItems(QuotationItemRequestDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Map<String, Object> result = quotationItemDao.search(dto, currentUser.getClient().getId());
            return ApiResponse.success("Quotation items retrieved successfully", result);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to search quotation items: " + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> updateQuotation(QuotationRequestDto request) {
        try {
            validateQuotationRequest(request);
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();

            Quotation quotation = quotationRepository.findById(request.getQuotationId())
                    .orElseThrow(() -> new ValidationException("Quotation not found"));

            if (!quotation.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized access to quotation");
            }

            if(quotation.getStatus() == QuotationStatus.I) {
                throw new ValidationException("Quotation is already Invoiced");
            }

            if(request.getCustomerId() != null){
                Customer customer = customerRepository.findById(request.getCustomerId())
                        .orElseThrow(() -> new ValidationException("Customer not found"));
                quotation.setCustomer(customer);
                quotation.setCustomerName(customer.getName());
            } else {
                quotation.setCustomerName(request.getCustomerName());
            }

            updateQuotationDetails(quotation, request, currentUser);

            // Delete existing items
            quotationItemRepository.deleteByQuotationId(quotation.getId());

            List<QuotationItem> items = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal taxAmount = BigDecimal.ZERO;
            BigDecimal discountedPrice = BigDecimal.ZERO;
            BigDecimal quotationDiscountAmount = BigDecimal.ZERO;
            BigDecimal quotationDiscountPrice = BigDecimal.ZERO;

            for (QuotationItemRequestDto itemDto : request.getItems()) {
                QuotationItem item = createQuotationItem(itemDto, quotation, currentUser, request);
                items.add(item);
                totalAmount = totalAmount.add(item.getFinalPrice());
                taxAmount = taxAmount.add(item.getTaxAmount());
                discountedPrice = discountedPrice.add(item.getDiscountPrice());
                quotationDiscountAmount = quotationDiscountAmount.add(item.getQuotationDiscountAmount());
                quotationDiscountPrice = quotationDiscountPrice.add(item.getQuotationDiscountPrice());
            }

            quotationItemRepository.saveAll(items);

            BigDecimal packagingAndForwadingCharges = request.getPackagingAndForwadingCharges() != null ? request.getPackagingAndForwadingCharges() : BigDecimal.ZERO;
            quotation.setPackagingAndForwadingCharges(packagingAndForwadingCharges);
            totalAmount = totalAmount.add(packagingAndForwadingCharges);
            totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP);
            quotation.setTotalAmount(totalAmount);
            quotation.setTaxAmount(taxAmount);
            // discountedPrice should reflect sum of item discountPrice (pre-tax)
            quotation.setDiscountedPrice(discountedPrice);
            quotation.setQuotationDiscountPercentage(request.getQuotationDiscountPercentage());
            quotation.setQuotationDiscountAmount(quotationDiscountAmount);
            quotationRepository.save(quotation);

            return ApiResponse.success("Quotation updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error updating quotation", e);
            throw new ValidationException("Failed to update quotation: " + e.getMessage());
        }
    }

    private void validateAndProcessItem(QuotationItemRequestDto itemDto, Product product, UserMaster currentUser) {
        // Set default tax percentage if not provided
        if (product.getTaxPercentage() == null) {
            itemDto.setTaxPercentage(DEFAULT_TAX_PERCENTAGE);
        }
    }

    private void validateNosProduct(QuotationItemRequestDto itemDto) {
        if (itemDto.getQuantity() == null || itemDto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Quantity must be greater than 0 for NOS products");
        }
    }


    private QuotationItem createQuotationItem(QuotationItemRequestDto itemDto, Quotation quotation, UserMaster currentUser, QuotationRequestDto quotationRequestDto) {
        Product product = productRepository.findById(itemDto.getProductId())
                .orElseThrow(() -> new ValidationException("Product not found"));

        if(!Objects.equals(product.getClient().getId(), currentUser.getClient().getId())) {
            throw new ValidationException("Product is not available for you");
        }
        // Validate and process item based on product type
        validateAndProcessItem(itemDto, product, currentUser);

        QuotationItem item = new QuotationItem();
        item.setQuotation(quotation);
        item.setProduct(product);
        item.setQuantity(itemDto.getQuantity());
        item.setUnitPrice(itemDto.getUnitPrice());
        item.setTaxPercentage(product.getTaxPercentage());
        // New optional relations and fields
        if (itemDto.getBrandId() != null) {
            Brand brand = brandRepository.findById(itemDto.getBrandId())
                    .orElseThrow(() -> new ValidationException("Brand not found"));
            if(!Objects.equals(brand.getClient().getId(), currentUser.getClient().getId())) {
                throw new ValidationException("Brand is not available for you");
            }
            item.setBrand(brand);
        } else {
            item.setBrand(null);
        }
        item.setNumberOfRoll(itemDto.getNumberOfRoll() != null ? itemDto.getNumberOfRoll() : 0);
        item.setWeightPerRoll(itemDto.getWeightPerRoll() != null ? itemDto.getWeightPerRoll() : BigDecimal.ZERO);
        item.setRemarks(itemDto.getRemarks());
        // New fields
        if(itemDto.getIsProduction() != null){
            item.setIsProduction(itemDto.getIsProduction());
        }
        if(itemDto.getQuotationItemStatus()!=null){
            item.setQuotationItemStatus(itemDto.getQuotationItemStatus());
        }

        // Calculate price components
        BigDecimal subTotal = itemDto.getUnitPrice().multiply(itemDto.getQuantity())
                .setScale(2, RoundingMode.HALF_UP);

        // Per requirement: no line-item discount
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal afterDiscount = subTotal; // unchanged since discount removed

        // Calculate tax on discountPrice (afterDiscount)
        BigDecimal baseTaxAmount = calculatePercentageAmount(afterDiscount, product.getTaxPercentage());

        // Apply quotation discount on tax only, if provided
        if (quotationRequestDto != null && quotationRequestDto.getQuotationDiscountPercentage() != null) {
            item.setQuotationDiscountPercentage(quotationRequestDto.getQuotationDiscountPercentage());
            BigDecimal quotationDiscountAmountOnTax = calculatePercentageAmount(baseTaxAmount, quotationRequestDto.getQuotationDiscountPercentage());
            item.setQuotationDiscountAmount(quotationDiscountAmountOnTax);

            BigDecimal discountedTaxAmount = baseTaxAmount.subtract(quotationDiscountAmountOnTax).setScale(2, RoundingMode.HALF_UP);
            item.setTaxAmount(discountedTaxAmount);

            BigDecimal finalPrice = afterDiscount.add(discountedTaxAmount).setScale(2, RoundingMode.HALF_UP);
            item.setFinalPrice(finalPrice);
            // quotationDiscountPrice should reflect total after applying quotation discount (tax-only discount)
            item.setQuotationDiscountPrice(finalPrice);
        } else {
            BigDecimal taxAmount = baseTaxAmount.setScale(2, RoundingMode.HALF_UP);
            item.setTaxAmount(taxAmount);
            BigDecimal finalPrice = afterDiscount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
            item.setFinalPrice(finalPrice);
            item.setQuotationDiscountPercentage(BigDecimal.ZERO);
            item.setQuotationDiscountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            item.setQuotationDiscountPrice(finalPrice);
        }

        item.setDiscountPrice(afterDiscount);
        item.setClient(currentUser.getClient());

        // Save the item first
        item = quotationItemRepository.save(item);

        return item;
    }

    private BigDecimal calculatePercentageAmount(BigDecimal base, BigDecimal percentage) {
        return percentage != null ?
                base.multiply(percentage)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
    }

    private void validateQuotationRequest(QuotationRequestDto request) {
        if (request.getCustomerName() == null) {
            throw new ValidationException("Customer Name is required");
        }
        if (request.getQuoteDate() == null) {
            throw new ValidationException("Quote date is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ValidationException("At least one item is required");
        }

        request.getItems().forEach(item -> {
            if (item.getProductId() == null) {
                throw new ValidationException("Product ID is required");
            }
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Valid quantity is required");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Valid unit price is required");
            }
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> updateQuotationItemStatus(QuotationItemStatusUpdateDto request) {
        UserMaster currentUser = utilityService.getCurrentLoggedInUser();
        QuotationItem item = quotationItemRepository.findById(request.getId())
                .orElseThrow(() -> new ValidationException("Quotation item not found"));
        if (!item.getClient().getId().equals(currentUser.getClient().getId())) {
            throw new ValidationException("Unauthorized access to quotation item");
        }
        String status = request.getQuotationItemStatus();
        if(status.equals(item.getQuotationItemStatus())) {
            throw new ValidationException("Quotation item status is already " + status);
        }
        int updated = quotationItemRepository.updateQuotationItemStatusById(item.getId(), status);
        if (updated == 0) {
            throw new ValidationException("Failed to update quotation item status");
        }
        return ApiResponse.success("Quotation item status updated successfully");
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> updateQuotationItemProduction(QuotationItemProductionUpdateDto request) {
        UserMaster currentUser = utilityService.getCurrentLoggedInUser();
        QuotationItem item = quotationItemRepository.findById(request.getId())
                .orElseThrow(() -> new ValidationException("Quotation item not found"));
        if (!item.getClient().getId().equals(currentUser.getClient().getId())) {
            throw new ValidationException("Unauthorized access to quotation item");
        }
        item.setIsProduction(request.getIsProduction());
        if(request.getIsProduction()) {
            item.setQuotationItemStatus(QuotationStatusItem.O.value);
        } else {
            item.setQuotationItemStatus(null);
        }
        quotationItemRepository.save(item);
        return ApiResponse.success("Quotation item production flag updated successfully");
    }

    private void updateQuotationDetails(Quotation quotation, QuotationRequestDto request, UserMaster currentUser) {
        // quotation.setQuoteDate(request.getQuoteDate());
//        quotation.setQuoteNumber(request.getQuoteNumber());
        quotation.setValidUntil(request.getValidUntil());
        quotation.setRemarks(request.getRemarks());
        quotation.setTermsConditions(request.getTermsConditions());
        quotation.setContactNumber(request.getContactNumber());
        quotation.setAddress(request.getAddress());
        if (request.getTransportMasterId() != null) {
            TransportMaster transportMaster = transportMasterRepository.findById(request.getTransportMasterId())
                    .orElseThrow(() -> new ValidationException("Transport master not found"));
            quotation.setTransportMaster(transportMaster);
        } else {
            quotation.setTransportMaster(null);
        }
        quotation.setCaseNumber(request.getCaseNumber());
        quotation.setReferenceName(request.getReferenceName());
        quotation.setUpdatedAt(OffsetDateTime.now());
        quotation.setUpdatedBy(currentUser);
    }

    public Map<String, Object> searchQuotations(QuotationDto searchParams) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            searchParams.setClientId(currentUser.getClient().getId());
            return quotationDao.searchQuotations(searchParams);
        } catch (Exception e) {
            log.error("Error searching quotations", e);
            throw new ValidationException("Failed to search quotations: " + e.getMessage());
        }
    }

    public ApiResponse getQuotationDetail(QuotationDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            request.setClientId(currentUser.getClient().getId());

            // Get quotation and verify access
            Quotation quotation = quotationRepository.findById(request.getId())
                    .orElseThrow(() -> new ValidationException("Quotation not found"));

            if (!quotation.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized access to quotation");
            }

            // Get all quotation items with calculations
            List<QuotationItem> items = quotationItemRepository.findByQuotationId(quotation.getId());

            // Transform data into response format
            Map<String, Object> response = new HashMap<>();

            // Add quotation details
            response.put("id", quotation.getId());
            response.put("quoteNumber", quotation.getQuoteNumber());
            response.put("quoteDate", quotation.getQuoteDate());
            response.put("validUntil", quotation.getValidUntil());
            response.put("totalAmount", quotation.getTotalAmount());
            response.put("status", quotation.getStatus());
            response.put("remarks", quotation.getRemarks());
            response.put("termsConditions", quotation.getTermsConditions());
            response.put("referenceName", quotation.getReferenceName());
            response.put("customerName", quotation.getCustomerName());
            response.put("customerId", quotation.getCustomer() != null ? quotation.getCustomer().getId() : null);
            response.put("contactNumber", quotation.getContactNumber());
            response.put("address", quotation.getAddress() != null ? quotation.getAddress() : quotation.getCustomer().getAddress());
            response.put("transportMasterId", quotation.getTransportMaster() != null ? quotation.getTransportMaster().getId() : null);
            response.put("caseNumber", quotation.getCaseNumber());
            response.put("packagingAndForwadingCharges", quotation.getPackagingAndForwadingCharges());
            response.put("quotationDiscountPercentage", quotation.getQuotationDiscountPercentage());
            response.put("quotationDiscountAmount", quotation.getQuotationDiscountAmount());

            // Transform and add items
            List<Map<String, Object>> itemsList = new ArrayList<>();
            for (QuotationItem item : items) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("productId", item.getProduct().getId());
                itemMap.put("productName", item.getProduct().getName());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("unitPrice", item.getUnitPrice());
                itemMap.put("price", item.getDiscountPrice());
                itemMap.put("taxPercentage", item.getTaxPercentage());
                itemMap.put("taxAmount", item.getTaxAmount());
                itemMap.put("finalPrice", item.getFinalPrice());
                itemMap.put("quotationDiscountPercentage", item.getQuotationDiscountPercentage());
                itemMap.put("quotationDiscountAmount", item.getQuotationDiscountAmount());
                itemMap.put("quotationDiscountPrice", item.getQuotationDiscountPrice());
                itemMap.put("brandId", item.getBrand() != null ? item.getBrand().getId() : null);
                itemMap.put("brandName", item.getBrand() != null ? item.getBrand().getName() : null);
                itemMap.put("numberOfRoll", item.getNumberOfRoll());
                itemMap.put("createdRoll", item.getCreatedRoll());
                itemMap.put("isProduction", item.getIsProduction());
                itemMap.put("quotationItemStatus", item.getQuotationItemStatus());
                itemMap.put("weightPerRoll", item.getWeightPerRoll());
                itemMap.put("remarks", item.getRemarks());

                itemsList.add(itemMap);
            }

            response.put("items", itemsList);

            return ApiResponse.success("Data fetched successfully", response);
        } catch (Exception e) {
            log.error("Error fetching quotation detail", e);
            throw new ValidationException("Failed to fetch quotation detail: " + e.getMessage());
        }
    }

    public byte[] generateQuotationPdf(QuotationDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            request.setClientId(currentUser.getClient().getId());
            Map<String, Object> quotationData = quotationDao.getQuotationDetail(request);
            return quotationPdfGenerationService.generateQuotationPdf(quotationData);
        } catch (ValidationException ve) {
            ve.printStackTrace();
            throw ve;
        } catch (Exception e) {
            log.error("Error generating quotation PDF", e);
            throw new ValidationException("Failed to generate PDF: " + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> updateQuotationStatus(QuotationItemRequestDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Quotation quotation = quotationRepository.findById(request.getId())
                    .orElseThrow(() -> new ValidationException("Quotation not found"));

            if (!quotation.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized access to quotation");
            }

            QuotationStatus newStatus = QuotationStatus.valueOf(request.getStatus());
            QuotationStatus currentStatus = quotation.getStatus();

            validateStatusTransition(currentStatus, newStatus);
            handleProductQuantities(quotation, currentStatus, newStatus);

            quotation.setStatus(newStatus);
            quotation.setUpdatedAt(OffsetDateTime.now());
            quotation.setUpdatedBy(currentUser);
            quotationRepository.save(quotation);

            return ApiResponse.success("Quotation status updated successfully");
        } catch (Exception e) {
            log.error("Error updating quotation status", e);
            throw new ValidationException("Failed to update quotation status: " + e.getMessage());
        }
    }

    private void validateStatusTransition(QuotationStatus currentStatus, QuotationStatus newStatus) {
        switch (currentStatus) {
            case Q:
                if (!Arrays.asList(QuotationStatus.A, QuotationStatus.D).contains(newStatus)) {
                    throw new ValidationException("Quote can only be Accepted or Declined");
                }
                break;
            case A:
                if (!Arrays.asList(QuotationStatus.D, QuotationStatus.P).contains(newStatus)) {
                    throw new ValidationException("Accepted quote can only be changed to Processing or Declined");
                }
                break;
            case P:
                if (newStatus != QuotationStatus.PC) {
                    throw new ValidationException("Processing quote can only be Completed");
                }
                break;
            case PC:
                if (newStatus != QuotationStatus.C) {
                    throw new ValidationException("Processing quote can only be Completed");
                }
                break;
            case D:
                if (newStatus != QuotationStatus.A) {
                    throw new ValidationException("Declined quote can only be changed to Accepted");
                }
                break;
            case C:
                if (newStatus != QuotationStatus.I) {
                    throw new ValidationException("Completed quote can only be changed to Invoiced");
                }
                break;
            case I:
                throw new ValidationException("Current status cannot be updated");
            default:
                throw new ValidationException("Invalid current status");
        }
    }

    private void handleProductQuantities(Quotation quotation, QuotationStatus currentStatus, QuotationStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (currentStatus == QuotationStatus.Q && newStatus == QuotationStatus.A) {
            // Block quantities when accepting
            updateProductQuantities(quotation, true);
        } else if (currentStatus == QuotationStatus.A && newStatus == QuotationStatus.D) {
            // Unblock quantities when declining
            updateProductQuantities(quotation, false);
        } else if (currentStatus == QuotationStatus.P && newStatus == QuotationStatus.C) {
            // Move quantities from blocked to used (subtract from blocked)
            updateProductQuantities(quotation, false);
        } else if (currentStatus == QuotationStatus.C && newStatus == QuotationStatus.I) {
            // Create purchase and sale entries
//            createPurchaseAndSaleEntries(quotation);
        }
    }

    private void updateProductQuantities(Quotation quotation, boolean block) {
        /*List<QuotationItem> items = quotationItemRepository.findByQuotationId(quotation.getId());

        for (QuotationItem item : items) {
            Product product = item.getProduct();
            productQuantityService.updateProductQuantity(
                    product.getId(),
                    item.getQuantity(),
                    false,  // not a purchase
                    false,  // not a sale
                    block   // block or unblock based on status
            );
        }*/
    }

//    private void createPurchaseAndSaleEntries(Quotation quotation) {
//        UserMaster currentUser = utilityService.getCurrentLoggedInUser();
//        List<QuotationItem> items = quotationItemRepository.findByQuotationId(quotation.getId());
//
//        for (QuotationItem item : items) {
//            Product product = item.getProduct();
//
//            // Create purchase entry
//            Purchase purchase = new Purchase();
//            purchase.setProduct(product);
//            purchase.setCategory(product.getCategory());
//            purchase.setQuantity(item.getQuantity().intValue());
//            purchase.setUnitPrice(product.getPurchaseAmount()); // Use product's purchase amount for purchase entry
//            purchase.setCustomer(quotation.getCustomer());
//            purchase.setClient(currentUser.getClient());
//            purchase.setQuotation(quotation);
//            purchase.setQuotationItem(item);
//
//            // Set quotation discount values
//
//            // Calculate purchase amounts
//            /*BigDecimal baseAmount = product.getPurchaseAmount().multiply(item.getQuantity());
//
//            // Apply regular discount if any
//            BigDecimal discountAmount = calculatePercentageAmount(baseAmount, item.getDiscountPercentage());
//            BigDecimal afterDiscount = baseAmount.subtract(discountAmount);
//
//            // Apply quotation discount
//            BigDecimal quotationDiscountAmount = calculatePercentageAmount(afterDiscount, item.getQuotationDiscountPercentage());
//            BigDecimal quotationDiscountPrice = afterDiscount.subtract(quotationDiscountAmount);*/
//
//            BigDecimal baseAmount = product.getPurchaseAmount().multiply(item.getQuantity());
//            // Set all calculated values for purchase
//            purchase.setDiscount(BigDecimal.ZERO);
//            purchase.setDiscountAmount(BigDecimal.ZERO);
//            purchase.setPurchaseDate(OffsetDateTime.now());
//            purchase.setDiscountPrice(baseAmount);
//            purchase.setTotalAmount(baseAmount);
//            purchase.setRemainingQuantity(0); // Since we're creating sale immediately
//            purchase.setCreatedBy(currentUser);
//            purchase.setOtherExpenses(BigDecimal.ZERO);
//
//            purchase = purchaseRepository.save(purchase);
//
//            // Create sale entry
//            Sale sale = new Sale();
//            sale.setPurchase(purchase);
//            sale.setQuantity(item.getQuantity().intValue());
//            sale.setUnitPrice(item.getUnitPrice()); // Use quotation item's unit price for sale
//            sale.setCustomer(quotation.getCustomer());
//            sale.setQuotation(quotation);
//            sale.setQuotationItem(item);
//
//            // Set quotation discount values for sale
//            sale.setQuotationDiscountPercentage(item.getQuotationDiscountPercentage());
//            sale.setQuotationDiscountAmount(item.getQuotationDiscountAmount());
//            sale.setQuotationDiscountPrice(item.getQuotationDiscountPrice());
//
//            // Calculate sale amounts
//            /*baseAmount = item.getUnitPrice().multiply(item.getQuantity());
//
//            // Apply regular discount if any
//            discountAmount = calculatePercentageAmount(baseAmount, item.getDiscountPercentage());
//            afterDiscount = baseAmount.subtract(discountAmount);
//
//            // Apply quotation discount
//            quotationDiscountAmount = calculatePercentageAmount(afterDiscount, item.getQuotationDiscountPercentage());
//            quotationDiscountPrice = afterDiscount.subtract(quotationDiscountAmount);*/
//
//            // Set all calculated values for sale
//            sale.setDiscount(item.getDiscountPercentage());
//            sale.setDiscountAmount(item.getDiscountAmount());
//            sale.setDiscountPrice(item.getDiscountPrice());
//            sale.setTotalAmount(item.getQuotationDiscountPrice());
//
//            sale.setSaleDate(OffsetDateTime.now());
//            sale.setCreatedBy(currentUser);
//            sale.setOtherExpenses(BigDecimal.ZERO);
//            sale.setClient(currentUser.getClient());
//
//            sale = saleRepository.save(sale);
//
//            
//        }
//    }
//
//    
//
//        // Use discounted prices for profit calculations (including quotation discount)
//        BigDecimal purchaseAmount = purchase.getDiscountPrice();
//
//        BigDecimal saleAmount = sale.getQuotationDiscountPrice() != null ?
//            sale.getQuotationDiscountPrice() : sale.getDiscountPrice();
//
//        dailyProfit.setPurchaseAmount(purchaseAmount);
//        dailyProfit.setSaleAmount(saleAmount);
//        dailyProfit.setGrossProfit(saleAmount.subtract(purchaseAmount));
//        dailyProfit.setOtherExpenses(sale.getOtherExpenses());
//        dailyProfit.setNetProfit(dailyProfit.getGrossProfit().subtract(
//            sale.getOtherExpenses() != null ? sale.getOtherExpenses() : BigDecimal.ZERO));
//        dailyProfit.setProfitDate(sale.getSaleDate());
//        dailyProfit.setClient(currentUser.getClient());
//
//        dailyProfitRepository.save(dailyProfit);
//    }

    public byte[] generateDispatchSlipPdf(QuotationDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            request.setClientId(currentUser.getClient().getId());
            Map<String, Object> quotationData = quotationDao.getQuotationDetail(request);
            return dispatchSlipPdfService.generateQuotationPdf(quotationData);
        } catch (ValidationException ve) {
            ve.printStackTrace();
            throw ve;
        } catch (Exception e) {
            log.error("Error generating quotation PDF", e);
            throw new ValidationException("Failed to generate PDF: " + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> deleteQuotation(QuotationRequestDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Quotation quotation = quotationRepository.findById(request.getQuotationId())
                    .orElseThrow(() -> new ValidationException("Quotation not found", HttpStatus.UNPROCESSABLE_ENTITY));

            if (!quotation.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to delete this quotation", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            // Validate status
            if (!Arrays.asList(QuotationStatus.Q, QuotationStatus.D).contains(quotation.getStatus())) {
                throw new ValidationException("Only quotations with status 'Quote' or 'Declined' can be deleted", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            quotationItemRepository.deleteByQuotationId(quotation.getId());
            quotationRepository.delete(quotation);

            return ApiResponse.success("Quotation deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting quotation", e);
            throw new ValidationException("Failed to delete quotation: " + e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
