package com.inventory.service;

import com.inventory.dao.PurchaseDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.PurchaseDto;
import com.inventory.dto.PurchaseItemDto;
import com.inventory.dto.PurchaseRequestDto;
import com.inventory.entity.Customer;
import com.inventory.entity.Product;
import com.inventory.entity.Purchase;
import com.inventory.entity.PurchaseItem;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.PurchaseItemRepository;
import com.inventory.repository.PurchaseRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final ProductQuantityService productQuantityService;
    private final CustomerRepository customerRepository;
    private final BatchProcessingService batchProcessingService;
    private final UtilityService utilityService;
    private final PurchaseDao purchaseDao;
    private final PurchaseItemRepository purchaseItemRepository;
    private final CustomerRemainingPaymentAmountService customerRemainingPaymentAmountService;

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> createPurchase(PurchaseRequestDto request) {
        try {
            validatePurchaseRequest(request);
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            
            // Handle update case
            if (request.getId() != null) {
                return handlePurchaseUpdate(request, currentUser);
            }
            
            // Existing create logic
            Purchase purchase = new Purchase();
            purchase.setCustomer(customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found")));
            purchase.setPurchaseDate(request.getPurchaseDate());
            purchase.setInvoiceNumber(request.getInvoiceNumber());
            purchase.setNumberOfItems(request.getProducts().size());
            purchase.setClient(currentUser.getClient());
            purchase.setCreatedBy(currentUser);
            purchase = purchaseRepository.save(purchase);
            
            // Process items in batches
            List<PurchaseItem> items = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (PurchaseItemDto itemDto : request.getProducts()) {
                PurchaseItem item = createPurchaseItem(itemDto, purchase);
                items.add(item);
                purchaseItemRepository.save(item);
                totalAmount = totalAmount.add(item.getFinalPrice());
               productQuantityService.updateProductQuantity(
                       item.getProduct().getId(),
                       item.getQuantity(),
                       true,
                       false,
                       null
               );
            }

            
            // Update customer remaining payment amount (add sale amount)
            customerRemainingPaymentAmountService.updateCustomerRemainingPaymentAmount(
                purchase.getCustomer().getId(), 
                totalAmount, 
                true,  // isPurchase
                false   // isSale
            );
            
            // Round the total amount
            totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP);
            purchase.setTotalPurchaseAmount(totalAmount);
            purchase = purchaseRepository.save(purchase);
            
            // Process items and update product quantities in batches
            // batchProcessingService.processPurchaseItems(items);
            
            return ApiResponse.success("Purchase created successfully");
        } catch(ValidationException ve) {
            ve.printStackTrace();
            throw ve;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Purchase creation failed: " + e.getMessage());
        }
    }
    
    private PurchaseItem createPurchaseItem(PurchaseItemDto dto, Purchase purchase) {
        Product product = productRepository.findById(dto.getProductId())
            .orElseThrow(() -> new ValidationException("Product not found"));
            
        PurchaseItem item = new PurchaseItem();
        item.setProduct(product);
        item.setPurchase(purchase);
        item.setQuantity(dto.getQuantity());
        item.setRemarks(dto.getRemarks());
        item.setUnitPrice(dto.getUnitPrice().setScale(2, RoundingMode.HALF_UP));
//        item.setDiscountPercentage(dto.getDiscountPercentage());
        
        // Calculate amounts with 2 decimal places
        BigDecimal subTotal = dto.getUnitPrice()
            .multiply((dto.getQuantity()))
            .setScale(2, RoundingMode.HALF_UP);
            
        BigDecimal discountAmount = calculateDiscountAmount(subTotal, dto.getDiscountPercentage())
            .setScale(2, RoundingMode.HALF_UP);
        
//        item.setDiscountAmount(discountAmount);
        item.setFinalPrice(subTotal.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP));
//        item.setRemainingQuantity(dto.getQuantity());
        item.setClient(purchase.getClient());
        
        return item;
    }

    private BigDecimal calculateDiscountAmount(BigDecimal base, BigDecimal percentage) {
        return percentage != null ? 
            base.multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;
    }
    
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> searchPurchases(PurchaseDto searchParams) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            searchParams.setClientId(currentUser.getClient().getId());
            Page<Map<String, Object>> maps = purchaseDao.searchPurchases(searchParams);
            return maps;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Purchase retrieval failed: " + e.getMessage());
        }
    }
    
    private void validatePurchaseRequest(PurchaseRequestDto request) {
        if (request.getCustomerId() == null) {
            throw new ValidationException("Customer ID is required");
        }
        if (request.getPurchaseDate() == null) {
            throw new ValidationException("Purchase date is required");
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
            Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Purchase not found"));
                
            if (!purchase.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to delete this purchase");
            }
            
            List<PurchaseItem> items = purchaseItemRepository.findByPurchaseId(id);

            for (PurchaseItem item : items) {
                try {
                    productQuantityService.updateProductQuantity(
                            item.getProduct().getId(),
                            item.getQuantity(),
                            false,  // false to subtract the quantity,
                            true,
                            null
                    );
                } catch (Exception e) {
                    log.error("Error reversing quantity for product {}: {}",
                            item.getProduct().getId(), e.getMessage());
                    throw new ValidationException("Failed to reverse product quantities: " + e.getMessage());
                }
            }

            purchaseItemRepository.deleteByPurchaseId(id);
            purchaseRepository.delete(purchase);
            
            return ApiResponse.success("Purchase deleted successfully");
        } catch (ValidationException ve) {
            ve.printStackTrace();
            throw ve;
        } catch (Exception e) {
            log.error("Error deleting purchase: {}", e.getMessage(), e);
            throw new ValidationException("Failed to delete purchase: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPurchaseDetail(PurchaseDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            return purchaseDao.getPurchaseDetail(request.getId(), currentUser.getClient().getId());
        } catch (Exception e) {
            log.error("Error fetching purchase detail: {}", e.getMessage(), e);
            throw new ValidationException("Failed to fetch purchase detail: " + e.getMessage());
        }
    }

    private ApiResponse<?> handlePurchaseUpdate(PurchaseRequestDto request, UserMaster currentUser) {
        Purchase existingPurchase = purchaseRepository.findById(request.getId())
            .orElseThrow(() -> new ValidationException("Purchase not found"));
            
        if (!existingPurchase.getClient().getId().equals(currentUser.getClient().getId())) {
            throw new ValidationException("Unauthorized access to purchase");
        }
        
        // Store original customer and amount for payment adjustment
        var originalCustomer = existingPurchase.getCustomer();
        var originalAmount = existingPurchase.getTotalPurchaseAmount();
        
        // Reverse existing quantities
        List<PurchaseItem> existingItems = purchaseItemRepository.findByPurchaseId(request.getId());
        for (PurchaseItem item : existingItems) {
            productQuantityService.updateProductQuantity(
                item.getProduct().getId(),
                item.getQuantity(),
                false,  // not a purchase
                true,   // is a sale (reverse)
                null    // no blocking
            );
        }
        
        // Delete existing items
        purchaseItemRepository.deleteByPurchaseId(request.getId());
        
        // Get new customer
        var newCustomer = customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new ValidationException("Customer not found"));
        
        // Update purchase details
        existingPurchase.setCustomer(newCustomer);
        existingPurchase.setPurchaseDate(request.getPurchaseDate());
        existingPurchase.setInvoiceNumber(request.getInvoiceNumber());
        existingPurchase.setNumberOfItems(request.getProducts().size());
        existingPurchase.setUpdatedBy(currentUser);
        existingPurchase.setUpdatedAt(OffsetDateTime.now());
        
        // Process new items
        List<PurchaseItem> newItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (PurchaseItemDto itemDto : request.getProducts()) {
            PurchaseItem item = createPurchaseItem(itemDto, existingPurchase);
            newItems.add(item);
            purchaseItemRepository.save(item);
            totalAmount = totalAmount.add(item.getFinalPrice());
            
            productQuantityService.updateProductQuantity(
                item.getProduct().getId(),
                item.getQuantity(),
                true,  // false to subtract the quantity,
                false,
                null
            );
        }
        
        // Round the total amount
        totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP);
        existingPurchase.setTotalPurchaseAmount(totalAmount);
        purchaseRepository.save(existingPurchase);
        
        // Process items and update product quantities in batches
//        batchProcessingService.processPurchaseItems(newItems);
        
        // Handle customer payment amount changes
        handleCustomerPaymentAmountUpdate(originalCustomer, newCustomer, originalAmount, totalAmount);
        
        return ApiResponse.success("Purchase updated successfully");
    }
    
    private void handleCustomerPaymentAmountUpdate(Customer originalCustomer, Customer newCustomer, BigDecimal originalAmount, BigDecimal newAmount) {
        // If customer changed, adjust payment amounts
        if (!originalCustomer.getId().equals(newCustomer.getId())) {
            // Reduce original customer's remaining payment amount (reverse purchase)
            customerRemainingPaymentAmountService.updateCustomerRemainingPaymentAmount(
                originalCustomer.getId(), 
                originalAmount,
                false, // not a purchase (reversing)
                true   // is a sale (reversing)
            );
            
            // Increase new customer's remaining payment amount (new purchase)
            customerRemainingPaymentAmountService.updateCustomerRemainingPaymentAmount(
                newCustomer.getId(), 
                newAmount,
                true,  // is a purchase
                false  // not a sale
            );
        } else {
            // Same customer, adjust by the difference
            BigDecimal amountDifference = newAmount.subtract(originalAmount);
            if (amountDifference.compareTo(BigDecimal.ZERO) != 0) {
                customerRemainingPaymentAmountService.updateCustomerRemainingPaymentAmount(
                    newCustomer.getId(), 
                    amountDifference,
                    amountDifference.compareTo(BigDecimal.ZERO) > 0, // isPurchase if positive
                    amountDifference.compareTo(BigDecimal.ZERO) < 0  // isSale if negative
                );
            }
        }
    }
}