package com.inventory.service;

import com.inventory.dao.SaleDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.SaleDto;
import com.inventory.dto.SaleItemDto;
import com.inventory.dto.SaleRequestDto;
import com.inventory.entity.Customer;
import com.inventory.entity.Product;
import com.inventory.entity.Sale;
import com.inventory.entity.SaleItem;
import com.inventory.entity.UserMaster;
import com.inventory.entity.QuotationItem;
import com.inventory.entity.Quotation;
import com.inventory.enums.QuotationStatusItem;
import com.inventory.exception.ValidationException;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.QuotationItemRepository;
import com.inventory.repository.QuotationRepository;
import com.inventory.repository.SaleItemRepository;
import com.inventory.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ProductQuantityService productQuantityService;
    private final CustomerRemainingPaymentAmountService customerRemainingPaymentAmountService;
    private final CustomerRepository customerRepository;
    private final BatchProcessingService batchProcessingService;
    private final UtilityService utilityService;
    private final SaleDao saleDao;
    private final SaleItemRepository saleItemRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final QuotationRepository quotationRepository;
    private final SalesBillNumberGeneratorService salesBillNumberGeneratorService;
    private final SalePdfGenerationService salePdfGenerationService;
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> createSale(SaleRequestDto request) {
        try {
            validateSaleRequest(request);
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            
            // Handle update case
            if (request.getId() != null) {
                return handleSaleUpdate(request, currentUser);
            }
            
            // Existing create logic
            Sale sale = new Sale();
            sale.setCustomer(customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found")));
            sale.setSaleDate(request.getSaleDate());
            sale.setIsBlack(request.getIsBlack());
            sale.setInvoiceNumber(request.getInvoiceNumber());
            sale.setNumberOfItems(request.getProducts().size());
            sale.setClient(currentUser.getClient());
            sale.setCreatedBy(currentUser);
            if (request.getTransportMasterId() != null) {
                sale.setTransportMaster(entityManager.getReference(
                        com.inventory.entity.TransportMaster.class, request.getTransportMasterId()));
            }
            sale.setCaseNumber(request.getCaseNumber());
            sale.setReferenceName(request.getReferenceName());
            sale = saleRepository.save(sale);
            
            // Process items in batches
            List<SaleItem> items = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (SaleItemDto itemDto : request.getProducts()) {
                SaleItem item = createSaleItem(itemDto, sale);
                items.add(item);
                saleItemRepository.save(item);
                totalAmount = totalAmount.add(item.getFinalPrice());
               productQuantityService.updateProductQuantity(
                       item.getProduct().getId(),
                       item.getQuantity(),
                       false,
                       true,
                       null
               );
            }
            
            // Round the total amount
            totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP);
            sale.setTotalSaleAmount(totalAmount);
            sale = saleRepository.save(sale);
            
            // Update customer remaining payment amount (add sale amount)
            customerRemainingPaymentAmountService.updateCustomerRemainingPaymentAmount(
                sale.getCustomer().getId(), 
                totalAmount, 
                false,  // isPurchase
                true   // isSale
            );
            
            // Process items and update product quantities in batches
        //    batchProcessingService.processSaleItems(items);
            
            return ApiResponse.success("Sale created successfully");
        } catch(ValidationException ve) {
            ve.printStackTrace();
            throw ve;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Sale creation failed: " + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> createSaleFromQuotationItems(SaleDto request) {
        try {
            List<Long> quotationItemIds = request.getQuotationItemIds();
            if (quotationItemIds == null || quotationItemIds.isEmpty()) {
                throw new ValidationException("At least one quotation item id is required");
            }
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();

            // Load all quotation items
            List<QuotationItem> quotationItems = quotationItemRepository.findAllById(quotationItemIds);
            if (quotationItems.isEmpty()) {
                throw new ValidationException("Quotation items not found");
            }

            // Ensure all belong to same client and same quotation
            Long clientId = currentUser.getClient().getId();
            Quotation parentQuotation = null;
            for (QuotationItem qi : quotationItems) {
                if (qi.getClient() == null || !qi.getClient().getId().equals(clientId)) {
                    throw new ValidationException("Unauthorized access to quotation items");
                }
                if (parentQuotation == null) {
                    parentQuotation = qi.getQuotation();
                } else if (!parentQuotation.getId().equals(qi.getQuotation().getId())) {
                    throw new ValidationException("All items must belong to the same quotation");
                }
            }
            if (parentQuotation == null) {
                throw new ValidationException("Parent quotation not found");
            }

            // Prepare Sale
            Sale sale = new Sale();
            sale.setCustomer(parentQuotation.getCustomer());
            sale.setSaleDate(new java.util.Date());
            sale.setIsBlack(false);
            sale.setNumberOfItems(quotationItems.size());
            sale.setClient(currentUser.getClient());
            sale.setCreatedBy(currentUser);
            sale.setQuotation(parentQuotation);
            // Optional fields propagated from request if provided
            if (request.getTransportMasterId() != null) {
                sale.setTransportMaster(entityManager.getReference(
                        com.inventory.entity.TransportMaster.class, request.getTransportMasterId()));
            }
            sale.setCaseNumber(request.getCaseNumber());
            sale.setReferenceName(request.getReferenceName());

            // Generate invoice number
            String invoiceNumber = salesBillNumberGeneratorService.generateInvoiceNumber(currentUser.getClient());
            sale.setInvoiceNumber(invoiceNumber);

            sale = saleRepository.save(sale);

            BigDecimal totalAmount = BigDecimal.ZERO;

            for (QuotationItem qi : quotationItems) {
                qi.setQuotationItemStatus(QuotationStatusItem.B.value);
                quotationItemRepository.save(qi);

                Product product = qi.getProduct();
                SaleItem item = new SaleItem();
                item.setSale(sale);
                item.setProduct(product);
                item.setQuantity(qi.getQuantity());
                item.setRemarks(qi.getRemarks());
                item.setUnitPrice(qi.getUnitPrice());
                item.setFinalPrice(qi.getFinalPrice());
                item.setQuotationItem(qi);
                item.setClient(currentUser.getClient());
                item.setNumberOfRoll(qi.getNumberOfRoll());
                item.setWeightPerRoll(qi.getWeightPerRoll());

                saleItemRepository.save(item);
                totalAmount = totalAmount.add(item.getFinalPrice());

                // Reduce product stock, since this is a sale
                productQuantityService.updateProductQuantity(
                        product.getId(),
                        item.getQuantity(),
                        false,
                        true,
                        null
                );
            }

            totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP);
            sale.setTotalSaleAmount(totalAmount);
            sale.setUpdatedAt(OffsetDateTime.now());
            saleRepository.save(sale);

            // Update customer remaining payment amount (add sale amount)
            if (sale.getCustomer() != null) {
                customerRemainingPaymentAmountService.updateCustomerRemainingPaymentAmount(
                        sale.getCustomer().getId(),
                        totalAmount,
                        false,
                        true
                );
            }

            return ApiResponse.success("Sale created successfully from quotation items");
        } catch (ValidationException ve) {
            ve.printStackTrace();
            throw ve;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to create sale from quotation items: " + e.getMessage());
        }
    }

    public byte[] generateSalePdf(SaleDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Map<String, Object> saleData = saleDao.getSalePdfDetail(request.getId(), currentUser.getClient().getId());
            return salePdfGenerationService.generateSalePdf(saleData);
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            log.error("Error generating sale PDF: {}", e.getMessage(), e);
            throw new ValidationException("Failed to generate Sale PDF: " + e.getMessage());
        }
    }
    
    private SaleItem createSaleItem(SaleItemDto dto, Sale sale) {
        Product product = productRepository.findById(dto.getProductId())
            .orElseThrow(() -> new ValidationException("Product not found"));
            
        SaleItem item = new SaleItem();
        item.setProduct(product);
        item.setSale(sale);
        item.setQuantity(dto.getQuantity());
        item.setRemarks(dto.getRemarks());
        item.setUnitPrice(dto.getUnitPrice().setScale(2, RoundingMode.HALF_UP));
//        item.setDiscountPercentage(dto.getDiscountPercentage());
        if (dto.getNumberOfRoll() != null) {
            item.setNumberOfRoll(dto.getNumberOfRoll());
        }
        if (dto.getWeightPerRoll() != null) {
            item.setWeightPerRoll(dto.getWeightPerRoll());
        }
        
        // Calculate amounts with 2 decimal places
        BigDecimal subTotal = dto.getUnitPrice()
            .multiply((dto.getQuantity()))
            .setScale(2, RoundingMode.HALF_UP);
            
        BigDecimal discountAmount = calculateDiscountAmount(subTotal, dto.getDiscountPercentage())
            .setScale(2, RoundingMode.HALF_UP);
        
//        item.setDiscountAmount(discountAmount);
        item.setFinalPrice(subTotal.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP));
//        item.setRemainingQuantity(dto.getQuantity());
        item.setClient(sale.getClient());
        
        return item;
    }

    private BigDecimal calculateDiscountAmount(BigDecimal base, BigDecimal percentage) {
        return percentage != null ? 
            base.multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;
    }
    
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> searchSales(SaleDto searchParams) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            searchParams.setClientId(currentUser.getClient().getId());
            Page<Map<String, Object>> maps = saleDao.searchSales(searchParams);
            return maps;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Purchase retrieval failed: " + e.getMessage());
        }
    }
    
    private void validateSaleRequest(SaleRequestDto request) {
        if (request.getCustomerId() == null) {
            throw new ValidationException("Customer ID is required");
        }
        if (request.getSaleDate() == null) {
            throw new ValidationException("Sale date is required");
        }
//        if (request.getInvoiceNumber() == null || request.getInvoiceNumber().trim().isEmpty()) {
//            throw new ValidationException("Invoice number is required");
//        }
        if (request.getProducts() == null || request.getProducts().isEmpty()) {
            throw new ValidationException("At least one product is required");
        }
        
        request.getProducts().forEach(item -> {
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
    public ApiResponse<?> delete(Long id) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Sale not found"));
                
            if (!sale.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to delete this sale");
            }
            
            List<SaleItem> items = saleItemRepository.findBySaleId(id);

            for (SaleItem item : items) {
                try {
                    productQuantityService.updateProductQuantity(
                            item.getProduct().getId(),
                            item.getQuantity(),
                            true,  // true to add the quantity,
                            false,
                            null
                    );
                } catch (Exception e) {
                    log.error("Error reversing quantity for product {}: {}",
                            item.getProduct().getId(), e.getMessage());
                    throw new ValidationException("Failed to reverse product quantities: " + e.getMessage());
                }
            }

            // Reverse customer remaining payment amount (subtract sale amount)
            customerRemainingPaymentAmountService.reverseSalePaymentAmount(
                sale.getCustomer().getId(), 
                sale.getTotalSaleAmount()
            );

            saleItemRepository.deleteBySaleId(id);
            saleRepository.delete(sale);
            
            return ApiResponse.success("Sale deleted successfully");
        } catch (ValidationException ve) {
            ve.printStackTrace();
            throw ve;
        } catch (Exception e) {
            log.error("Error deleting sale: {}", e.getMessage(), e);
            throw new ValidationException("Failed to delete sale: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSaleDetail(SaleDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            return saleDao.getSaleDetail(request.getId(), currentUser.getClient().getId());
        } catch (Exception e) {
            log.error("Error fetching sale detail: {}", e.getMessage(), e);
            throw new ValidationException("Failed to fetch sale detail: " + e.getMessage());
        }
    }

    private ApiResponse<?> handleSaleUpdate(SaleRequestDto request, UserMaster currentUser) {
        Sale existingSale = saleRepository.findById(request.getId())
            .orElseThrow(() -> new ValidationException("sale not found"));
            
        if (!existingSale.getClient().getId().equals(currentUser.getClient().getId())) {
            throw new ValidationException("Unauthorized access to sale");
        }
        
        // Store original customer and amount for payment adjustment
        Customer originalCustomer = existingSale.getCustomer();
        BigDecimal originalAmount = existingSale.getTotalSaleAmount();
        
        // Reverse existing quantities
        List<SaleItem> existingItems = saleItemRepository.findBySaleId(request.getId());
        for (SaleItem item : existingItems) {
            productQuantityService.updateProductQuantity(
                item.getProduct().getId(),
                item.getQuantity(),
                    true,  // not a purchase
                    false,   // is a purchase (reverse)
                    null    // no blocking
            );
        }
        
        // Delete existing items
        saleItemRepository.deleteBySaleId(request.getId());
        
        // Get new customer
        Customer newCustomer = customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new ValidationException("Customer not found"));
        
        // Update sale details
        existingSale.setCustomer(newCustomer);
        existingSale.setSaleDate(request.getSaleDate());
        existingSale.setInvoiceNumber(request.getInvoiceNumber());
        existingSale.setNumberOfItems(request.getProducts().size());
        existingSale.setUpdatedBy(currentUser);
        if (request.getTransportMasterId() != null) {
            existingSale.setTransportMaster(entityManager.getReference(
                    com.inventory.entity.TransportMaster.class, request.getTransportMasterId()));
        } else {
            existingSale.setTransportMaster(null);
        }
        existingSale.setCaseNumber(request.getCaseNumber());
        existingSale.setReferenceName(request.getReferenceName());
        existingSale.setUpdatedAt(OffsetDateTime.now());
        // Process new items
        List<SaleItem> newItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (SaleItemDto itemDto : request.getProducts()) {
            SaleItem item = createSaleItem(itemDto, existingSale);
            newItems.add(item);
            saleItemRepository.save(item);
            totalAmount = totalAmount.add(item.getFinalPrice());

            productQuantityService.updateProductQuantity(
                item.getProduct().getId(),
                item.getQuantity(),
                false,  // false to subtract the quantity,
                true,
                null
            );
        }
        
        // Round the total amount
        totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP);
        existingSale.setTotalSaleAmount(totalAmount);
        saleRepository.save(existingSale);
        
        // Handle customer payment amount changes
        handleCustomerPaymentAmountUpdate(originalCustomer, newCustomer, originalAmount, totalAmount);
        
        // Process items and update product quantities in batches
//        batchProcessingService.processSaleItems(newItems);
        
        return ApiResponse.success("Sale updated successfully");
    }
    
    private void handleCustomerPaymentAmountUpdate(Customer originalCustomer, Customer newCustomer, BigDecimal originalAmount, BigDecimal newAmount) {
        // If customer changed, adjust payment amounts
        if (!originalCustomer.getId().equals(newCustomer.getId())) {
            // Reduce original customer's remaining payment amount (reverse sale)
            customerRemainingPaymentAmountService.updateCustomerRemainingPaymentAmount(
                originalCustomer.getId(), 
                originalAmount,
                true,  // isPurchase (reversing sale)
                false  // not a sale (reversing)
            );
            
            // Increase new customer's remaining payment amount (new sale)
            customerRemainingPaymentAmountService.updateCustomerRemainingPaymentAmount(
                newCustomer.getId(), 
                newAmount,
                false, // not a purchase
                true   // is a sale
            );
        } else {
            // Same customer, adjust by the difference
            BigDecimal amountDifference = newAmount.subtract(originalAmount);
            if (amountDifference.compareTo(BigDecimal.ZERO) != 0) {
                customerRemainingPaymentAmountService.updateCustomerRemainingPaymentAmount(
                    newCustomer.getId(), 
                    amountDifference,
                    amountDifference.compareTo(BigDecimal.ZERO) < 0, // isPurchase if negative (reducing sale)
                    amountDifference.compareTo(BigDecimal.ZERO) > 0  // isSale if positive (increasing sale)
                );
            }
        }
    }
}