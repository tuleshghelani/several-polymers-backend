package com.inventory.service;

import com.inventory.dao.EnquiryMasterDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.EnquiryMasterDto;
import com.inventory.entity.EnquiryMaster;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.EnquiryMasterRepository;
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
public class EnquiryMasterService {
    private final EnquiryMasterRepository enquiryMasterRepository;
    private final EnquiryMasterDao enquiryMasterDao;
    private final UtilityService utilityService;
    private final Logger logger = LoggerFactory.getLogger(EnquiryMasterService.class);

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> create(EnquiryMasterDto dto) {
        validate(dto);
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();

            EnquiryMaster e = new EnquiryMaster();
            e.setName(StringUtils.hasText(dto.getName()) ? dto.getName().trim() : null);
            e.setMobile(StringUtils.hasText(dto.getMobile()) ? dto.getMobile().trim() : null);
            e.setMail(StringUtils.hasText(dto.getMail()) ? dto.getMail().trim() : null);
            e.setSubject(StringUtils.hasText(dto.getSubject()) ? dto.getSubject().trim() : null);
            e.setAddress(StringUtils.hasText(dto.getAddress()) ? dto.getAddress().trim() : null);
            e.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);
            e.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim() : "A");
            e.setClient(currentUser.getClient());
            e.setCreatedBy(currentUser);

            enquiryMasterRepository.save(e);
            return ApiResponse.success("Enquiry created successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to create enquiry");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> update(EnquiryMasterDto dto) {
        if (dto.getId() == null) {
            throw new ValidationException("Id is required for update");
        }
        validate(dto);
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            EnquiryMaster e = enquiryMasterRepository.findById(dto.getId())
                    .orElseThrow(() -> new ValidationException("Enquiry not found"));

            if (!Objects.equals(e.getClient().getId(), currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to update this enquiry");
            }

            e.setName(StringUtils.hasText(dto.getName()) ? dto.getName().trim() : null);
            e.setMobile(StringUtils.hasText(dto.getMobile()) ? dto.getMobile().trim() : null);
            e.setMail(StringUtils.hasText(dto.getMail()) ? dto.getMail().trim() : null);
            e.setSubject(StringUtils.hasText(dto.getSubject()) ? dto.getSubject().trim() : null);
            e.setAddress(StringUtils.hasText(dto.getAddress()) ? dto.getAddress().trim() : null);
            e.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);
            e.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim() : "A");
            e.setUpdatedBy(currentUser);
            e.setUpdatedAt(OffsetDateTime.now());

            enquiryMasterRepository.save(e);
            return ApiResponse.success("Enquiry updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update enquiry");
        }
    }

    @Transactional
    public ApiResponse<?> delete(Long id) {
        try {
            EnquiryMaster e = enquiryMasterRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Enquiry not found", HttpStatus.UNPROCESSABLE_ENTITY));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if (!Objects.equals(e.getClient().getId(), currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to delete this enquiry", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            try {
                enquiryMasterRepository.delete(e);
                return ApiResponse.success("Enquiry deleted successfully");
            } catch (DataIntegrityViolationException ex) {
                throw new ValidationException("Cannot delete enquiry. It is referenced by other records.", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Failed to delete enquiry: " + ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public ApiResponse<List<Map<String, Object>>> getEnquiries(EnquiryMasterDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            List<Map<String, Object>> list = enquiryMasterDao.getEnquiries(dto);
            return ApiResponse.success("Enquiries retrieved successfully", list);
        } catch (Exception e) {
            throw new ValidationException("Failed to retrieve enquiries");
        }
    }

    public ApiResponse<Map<String, Object>> searchEnquiries(EnquiryMasterDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            Map<String, Object> result = enquiryMasterDao.searchEnquiries(dto);
            return ApiResponse.success("Enquiries retrieved successfully", result);
        } catch (Exception e) {
            logger.error("Failed to retrieve enquiries", e);
            throw new ValidationException("Failed to retrieve enquiries");
        }
    }

    public ApiResponse<Map<String, Object>> getDetails(Long id) {
        if (id == null) {
            throw new ValidationException("Id is required");
        }
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            EnquiryMaster e = enquiryMasterRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Enquiry not found"));

            if (!Objects.equals(e.getClient().getId(), currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to view this enquiry");
            }

            Map<String, Object> data = new HashMap<>(15);
            data.put("id", e.getId());
            data.put("name", e.getName());
            data.put("mobile", e.getMobile());
            data.put("mail", e.getMail());
            data.put("subject", e.getSubject());
            data.put("address", e.getAddress());
            data.put("description", e.getDescription());
            data.put("status", e.getStatus());
            data.put("clientId", e.getClient() != null ? e.getClient().getId() : null);
            data.put("createdAt", e.getCreatedAt());
            data.put("updatedAt", e.getUpdatedAt());
            data.put("createdById", e.getCreatedBy() != null ? e.getCreatedBy().getId() : null);
            data.put("createdByName", e.getCreatedBy() != null ? e.getCreatedBy().getFirstName() + " " + e.getCreatedBy().getLastName() : null);
            data.put("updatedById", e.getUpdatedBy() != null ? e.getUpdatedBy().getId() : null);
            data.put("updatedByName", e.getUpdatedBy() != null ? e.getUpdatedBy().getFirstName() + " " + e.getUpdatedBy().getLastName() : null);

            return ApiResponse.success("Enquiry details retrieved successfully", data);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to retrieve enquiry details", e);
            throw new ValidationException("Failed to retrieve enquiry details");
        }
    }

    private void validate(EnquiryMasterDto dto) {
        if (StringUtils.hasText(dto.getName()) && dto.getName().trim().length() > 256) {
            throw new ValidationException("Name too long");
        }
        if (StringUtils.hasText(dto.getMobile()) && dto.getMobile().trim().length() > 32) {
            throw new ValidationException("Mobile number too long");
        }
        if (StringUtils.hasText(dto.getMail()) && dto.getMail().trim().length() > 256) {
            throw new ValidationException("Email too long");
        }
        if (StringUtils.hasText(dto.getStatus()) && dto.getStatus().trim().length() > 256) {
            throw new ValidationException("Status too long");
        }
    }
}
