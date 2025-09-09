package com.inventory.service;

import com.inventory.dao.BrandDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.BrandDto;
import com.inventory.entity.Brand;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BrandService {
    private final BrandRepository brandRepository;
    private final BrandDao brandDao;
    private final UtilityService utilityService;
    private final Logger logger = LoggerFactory.getLogger(BrandService.class);

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> create(BrandDto dto) {
        validate(dto);
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();

            Optional<Brand> existing = brandRepository
                    .findByNameAndClient_Id(dto.getName().trim(), currentUser.getClient().getId());
            if (existing.isPresent()) {
                throw new ValidationException("Brand name already exists");
            }

            Brand b = new Brand();
            b.setName(dto.getName().trim());
            b.setRemarks(StringUtils.hasText(dto.getRemarks()) ? dto.getRemarks().trim() : null);
            b.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim() : "A");
            b.setClient(currentUser.getClient());
            b.setCreatedBy(currentUser);

            brandRepository.save(b);
            return ApiResponse.success("Brand created successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to create brand");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> update(BrandDto dto) {
        if (dto.getId() == null) {
            throw new ValidationException("Id is required for update");
        }
        validate(dto);
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Brand b = brandRepository.findById(dto.getId())
                    .orElseThrow(() -> new ValidationException("Brand not found"));

            if (!Objects.equals(b.getClient().getId(), currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to update this brand");
            }

            Optional<Brand> byName = brandRepository
                    .findByNameAndIdNotInAndClient_Id(dto.getName().trim(), Collections.singletonList(b.getId()), currentUser.getClient().getId());
            if (byName.isPresent()) {
                throw new ValidationException("Brand name already exists");
            }

            b.setName(dto.getName().trim());
            b.setRemarks(StringUtils.hasText(dto.getRemarks()) ? dto.getRemarks().trim() : null);
            b.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim() : "A");
            b.setUpdatedBy(currentUser);
            b.setUpdatedAt(OffsetDateTime.now());

            brandRepository.save(b);
            return ApiResponse.success("Brand updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update brand");
        }
    }

    @Transactional
    public ApiResponse<?> delete(Long id) {
        try {
            Brand b = brandRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Brand not found", HttpStatus.UNPROCESSABLE_ENTITY));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if (!Objects.equals(b.getClient().getId(), currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to delete this brand", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            try {
                brandRepository.delete(b);
                return ApiResponse.success("Brand deleted successfully");
            } catch (DataIntegrityViolationException e) {
                throw new ValidationException("Cannot delete brand. It is referenced by other records.", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to delete brand: " + e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public ApiResponse<List<Map<String, Object>>> getBrands(BrandDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            List<Map<String, Object>> list = brandDao.getBrands(dto);
            return ApiResponse.success("Brands retrieved successfully", list);
        } catch (Exception e) {
            throw new ValidationException("Failed to retrieve brands");
        }
    }

    public ApiResponse<Map<String, Object>> searchBrands(BrandDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            Map<String, Object> result = brandDao.searchBrands(dto);
            return ApiResponse.success("Brands retrieved successfully", result);
        } catch (Exception e) {
            logger.error("Failed to retrieve brands", e);
            throw new ValidationException("Failed to retrieve brands");
        }
    }

    public ApiResponse<Map<String, Object>> getDetails(Long id) {
        if (id == null) {
            throw new ValidationException("Id is required");
        }
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Brand b = brandRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Brand not found"));

            if (!Objects.equals(b.getClient().getId(), currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to view this brand");
            }

            Map<String, Object> data = new HashMap<>(10);
            data.put("id", b.getId());
            data.put("name", b.getName());
            data.put("remarks", b.getRemarks());
            data.put("status", b.getStatus());
            data.put("clientId", b.getClient() != null ? b.getClient().getId() : null);
            data.put("createdAt", b.getCreatedAt());
            data.put("updatedAt", b.getUpdatedAt());
            data.put("createdById", b.getCreatedBy() != null ? b.getCreatedBy().getId() : null);
            data.put("createdByName", b.getCreatedBy() != null ? b.getCreatedBy().getFirstName() + " " + b.getCreatedBy().getLastName() : null);
            data.put("updatedById", b.getUpdatedBy() != null ? b.getUpdatedBy().getId() : null);
            data.put("updatedByName", b.getUpdatedBy() != null ? b.getUpdatedBy().getFirstName() + " " + b.getUpdatedBy().getLastName() : null);

            return ApiResponse.success("Brand details retrieved successfully", data);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to retrieve brand details", e);
            throw new ValidationException("Failed to retrieve brand details");
        }
    }

    private void validate(BrandDto dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new ValidationException("Brand name is required");
        }
        if (dto.getName().trim().length() > 255) {
            throw new ValidationException("Brand name too long");
        }
    }
}


