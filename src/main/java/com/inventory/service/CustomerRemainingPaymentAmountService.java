package com.inventory.service;

import com.inventory.entity.Customer;
import com.inventory.exception.ValidationException;
import com.inventory.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerRemainingPaymentAmountService {
    private final CustomerRepository customerRepository;
    private final ConcurrentHashMap<Long, Lock> customerLocks = new ConcurrentHashMap<>();
    
    private Lock getCustomerLock(Long customerId) {
        return customerLocks.computeIfAbsent(customerId, k -> new ReentrantLock());
    }
    
    @Transactional
    public void updateCustomerRemainingPaymentAmount(Long customerId, BigDecimal amountChange, Boolean isPurchase, Boolean isSale) {
        Lock lock = getCustomerLock(customerId);
        lock.lock();
        try {
            Customer customer = getAndValidateCustomer(customerId);
            
            if (Boolean.TRUE.equals(isPurchase)) {
                handlePurchase(customer, amountChange);
            } else if (Boolean.TRUE.equals(isSale)) {
                handleSale(customer, amountChange);
            }
            
            customerRepository.save(customer);
            logPaymentAmountUpdate(customer, amountChange, isPurchase, isSale);
            
        } catch (Exception e) {
            log.error("Error updating customer remaining payment amount: {}", e.getMessage(), e);
            throw e;
        } finally {
            lock.unlock();
        }
    }
    
    @Transactional
    public void reversePurchasePaymentAmount(Long customerId, BigDecimal amountToReverse) {
        Lock lock = getCustomerLock(customerId);
        lock.lock();
        try {
            Customer customer = getAndValidateCustomer(customerId);
            BigDecimal newRemainingPaymentAmount = customer.getRemainingPaymentAmount().add(amountToReverse);
            customer.setRemainingPaymentAmount(newRemainingPaymentAmount);
            customerRepository.save(customer);
            log.info("Customer {} reverse purchase payment applied. Reversed: {}, Remaining Payment Amount: {}",
                customer.getId(), amountToReverse, customer.getRemainingPaymentAmount());
        } catch (Exception e) {
            log.error("Error reversing purchase payment amount: {}", e.getMessage(), e);
            throw e;
        } finally {
            lock.unlock();
        }
    }
    
    @Transactional
    public void reverseSalePaymentAmount(Long customerId, BigDecimal amountToReverse) {
        Lock lock = getCustomerLock(customerId);
        lock.lock();
        try {
            Customer customer = getAndValidateCustomer(customerId);
            BigDecimal newRemainingPaymentAmount = customer.getRemainingPaymentAmount().subtract(amountToReverse);
            customer.setRemainingPaymentAmount(newRemainingPaymentAmount);
            customerRepository.save(customer);
            log.info("Customer {} reverse sale payment applied. Reversed: {}, Remaining Payment Amount: {}",
                customer.getId(), amountToReverse, customer.getRemainingPaymentAmount());
        } catch (Exception e) {
            log.error("Error reversing sale payment amount: {}", e.getMessage(), e);
            throw e;
        } finally {
            lock.unlock();
        }
    }
    
    @Transactional
    public void updatePurchasePaymentAmount(Long customerId, BigDecimal oldAmount, BigDecimal newAmount) {
        Lock lock = getCustomerLock(customerId);
        lock.lock();
        try {
            Customer customer = getAndValidateCustomer(customerId);
            
            // Add back the old amount and subtract the new amount
            BigDecimal netChange = oldAmount.subtract(newAmount);
            BigDecimal newRemainingPaymentAmount = customer.getRemainingPaymentAmount().add(netChange);
            
            customer.setRemainingPaymentAmount(newRemainingPaymentAmount);
            customerRepository.save(customer);
            
            log.info("Customer {} purchase payment amount updated. Old: {}, New: {}, Net Change: {}, Remaining Payment Amount: {}",
                customer.getId(), oldAmount, newAmount, netChange, customer.getRemainingPaymentAmount());
                
        } catch (Exception e) {
            log.error("Error updating purchase payment amount: {}", e.getMessage(), e);
            throw e;
        } finally {
            lock.unlock();
        }
    }
    
    @Transactional
    public void updateSalePaymentAmount(Long customerId, BigDecimal oldAmount, BigDecimal newAmount) {
        Lock lock = getCustomerLock(customerId);
        lock.lock();
        try {
            Customer customer = getAndValidateCustomer(customerId);
            
            // Subtract the old amount and add the new amount
            BigDecimal netChange = newAmount.subtract(oldAmount);
            BigDecimal newRemainingPaymentAmount = customer.getRemainingPaymentAmount().add(netChange);
            
            customer.setRemainingPaymentAmount(newRemainingPaymentAmount);
            customerRepository.save(customer);
            
            log.info("Customer {} sale payment amount updated. Old: {}, New: {}, Net Change: {}, Remaining Payment Amount: {}",
                customer.getId(), oldAmount, newAmount, netChange, customer.getRemainingPaymentAmount());
                
        } catch (Exception e) {
            log.error("Error updating sale payment amount: {}", e.getMessage(), e);
            throw e;
        } finally {
            lock.unlock();
        }
    }
    
    private Customer getAndValidateCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ValidationException("Customer not found"));
            
        if (customer.getRemainingPaymentAmount() == null) {
            customer.setRemainingPaymentAmount(BigDecimal.ZERO);
        }
        
        return customer;
    }
    
    private void handlePurchase(Customer customer, BigDecimal amountChange) {
        BigDecimal newRemainingPaymentAmount = customer.getRemainingPaymentAmount().subtract(amountChange);
        customer.setRemainingPaymentAmount(newRemainingPaymentAmount);
    }
    
    private void handleSale(Customer customer, BigDecimal amountChange) {
        BigDecimal newRemainingPaymentAmount = customer.getRemainingPaymentAmount().add(amountChange);
        customer.setRemainingPaymentAmount(newRemainingPaymentAmount);
    }
    
    private void logPaymentAmountUpdate(Customer customer, BigDecimal amountChange, Boolean isPurchase, Boolean isSale) {
        log.info("Customer {} remaining payment amount updated. Change: {}, Purchase: {}, Sale: {}, Remaining Payment Amount: {}",
            customer.getId(), amountChange, isPurchase, isSale, customer.getRemainingPaymentAmount());
    }
}
