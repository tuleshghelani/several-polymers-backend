package com.inventory.service;

import com.inventory.dao.ProductionDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.ProductionDto;
import com.inventory.entity.Batch;
import com.inventory.entity.Product;
import com.inventory.entity.Production;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.BachRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.ProductionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductionService {
    private final ProductionRepository productionRepository;
    private final BachRepository bachRepository;
    private final ProductRepository productRepository;
    private final ProductionDao productionDao;
    private final UtilityService utilityService;

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> create(ProductionDto dto) {
        try {
            validate(dto);
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Batch batch = bachRepository.findById(dto.getBatchId())
                    .orElseThrow(() -> new ValidationException("Bach not found", HttpStatus.NOT_FOUND));
            if (!batch.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new ValidationException("Product not found", HttpStatus.NOT_FOUND));

            Production p = new Production();
            p.setBatch(batch);
            p.setProduct(product);
            p.setQuantity(dto.getQuantity());
            p.setNumberOfRoll(dto.getNumberOfRoll());
            p.setIsWastage(dto.getIsWastage() != null ? dto.getIsWastage() : false);
            p.setClient(currentUser.getClient());
            productionRepository.save(p);
            return ApiResponse.success("Production created successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to create production", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> update(Long id, ProductionDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Production p = productionRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Production not found", HttpStatus.NOT_FOUND));
            if (!p.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            if (dto.getBatchId() != null) {
                Batch batch = bachRepository.findById(dto.getBatchId())
                        .orElseThrow(() -> new ValidationException("Bach not found", HttpStatus.NOT_FOUND));
                if (!batch.getClient().getId().equals(currentUser.getClient().getId())) {
                    throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
                }
                p.setBatch(batch);
            }
            if (dto.getProductId() != null) {
                Product product = productRepository.findById(dto.getProductId())
                        .orElseThrow(() -> new ValidationException("Product not found", HttpStatus.NOT_FOUND));
                p.setProduct(product);
            }
            if (dto.getQuantity() != null) p.setQuantity(dto.getQuantity());
            if (dto.getNumberOfRoll() != null) p.setNumberOfRoll(dto.getNumberOfRoll());
            if (dto.getIsWastage() != null) p.setIsWastage(dto.getIsWastage());
            productionRepository.save(p);
            return ApiResponse.success("Production updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update production", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> delete(Long id) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Production p = productionRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Production not found", HttpStatus.NOT_FOUND));
            if (!p.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            productionRepository.delete(p);
            return ApiResponse.success("Production deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to delete production", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ApiResponse<Map<String, Object>> search(ProductionDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            Map<String, Object> resp = productionDao.search(dto);
            return ApiResponse.success("Production fetched", resp);
        } catch (Exception e) {
            throw new ValidationException("Failed to fetch production", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ApiResponse<?> getDetails(Long id) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Production p = productionRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Production not found", HttpStatus.NOT_FOUND));
            if (!p.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            ProductionDto dto = new ProductionDto();
            dto.setId(p.getId());
            dto.setBatchId(p.getBatch() != null ? p.getBatch().getId() : null);
            dto.setProductId(p.getProduct() != null ? p.getProduct().getId() : null);
            dto.setQuantity(p.getQuantity());
            dto.setNumberOfRoll(p.getNumberOfRoll());
            dto.setIsWastage(p.getIsWastage());
            return ApiResponse.success("Production details fetched", dto);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to fetch production details", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validate(ProductionDto dto) {
        if (dto == null || dto.getBatchId() == null || dto.getProductId() == null || dto.getQuantity() == null) {
            throw new ValidationException("batchId, productId and quantity are required", HttpStatus.BAD_REQUEST);
        }
    }
}


