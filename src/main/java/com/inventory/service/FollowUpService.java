package com.inventory.service;

import com.inventory.dao.FollowUpDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.FollowUpDto;
import com.inventory.entity.FollowUp;
import com.inventory.entity.EnquiryMaster;
import com.inventory.entity.Client;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.FollowUpRepository;
import com.inventory.repository.EnquiryMasterRepository;
import com.inventory.repository.ClientRepository;
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
public class FollowUpService {
    private final FollowUpRepository followUpRepository;
    private final FollowUpDao followUpDao;
    private final EnquiryMasterRepository enquiryMasterRepository;
    private final ClientRepository clientRepository;
    private final UtilityService utilityService;
    private final Logger logger = LoggerFactory.getLogger(FollowUpService.class);

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> create(FollowUpDto dto) {
        validate(dto);
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();

            FollowUp f = new FollowUp();
            f.setFollowUpStatus(StringUtils.hasText(dto.getFollowUpStatus()) ? dto.getFollowUpStatus().trim() : "A");
            f.setNextActionDate(dto.getNextActionDate());
            f.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);
            
            // Set enquiry
            if (dto.getEnquiryId() != null) {
                EnquiryMaster enquiry = enquiryMasterRepository.findById(dto.getEnquiryId())
                        .orElseThrow(() -> new ValidationException("Enquiry not found"));
                f.setEnquiry(enquiry);
            }
            
            // Set client
            if (dto.getClientId() != null) {
                Client client = clientRepository.findById(dto.getClientId())
                        .orElseThrow(() -> new ValidationException("Client not found"));
                f.setClient(client);
            } else {
                f.setClient(currentUser.getClient());
            }
            
            f.setCreatedBy(currentUser);

            followUpRepository.save(f);
            
            // Update status logic: Only latest follow-up should have 'S' status, others should be 'C'
            updateFollowUpStatusesForEnquiry(dto.getEnquiryId(), currentUser.getClient().getId());
            
            return ApiResponse.success("Follow-up created successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to create follow-up");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> update(FollowUpDto dto) {
        if (dto.getId() == null) {
            throw new ValidationException("Id is required for update");
        }
        validate(dto);
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            FollowUp f = followUpRepository.findById(dto.getId())
                    .orElseThrow(() -> new ValidationException("Follow-up not found"));

            if (!Objects.equals(f.getClient().getId(), currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to update this follow-up");
            }

            f.setFollowUpStatus(StringUtils.hasText(dto.getFollowUpStatus()) ? dto.getFollowUpStatus().trim() : "A");
            f.setNextActionDate(dto.getNextActionDate());
            f.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);
            
            // Update enquiry if provided
            if (dto.getEnquiryId() != null) {
                EnquiryMaster enquiry = enquiryMasterRepository.findById(dto.getEnquiryId())
                        .orElseThrow(() -> new ValidationException("Enquiry not found"));
                f.setEnquiry(enquiry);
            }
            
            // Update client if provided
            if (dto.getClientId() != null) {
                Client client = clientRepository.findById(dto.getClientId())
                        .orElseThrow(() -> new ValidationException("Client not found"));
                f.setClient(client);
            }
            
            f.setUpdatedBy(currentUser);
            f.setUpdatedAt(OffsetDateTime.now());

            followUpRepository.save(f);
            
            // Update status logic: Only latest follow-up should have 'S' status, others should be 'C'
            updateFollowUpStatusesForEnquiry(f.getEnquiry().getId(), currentUser.getClient().getId());
            
            return ApiResponse.success("Follow-up updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update follow-up");
        }
    }

    @Transactional
    public ApiResponse<?> delete(Long id) {
        try {
            FollowUp f = followUpRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Follow-up not found", HttpStatus.UNPROCESSABLE_ENTITY));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if (!Objects.equals(f.getClient().getId(), currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to delete this follow-up", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            try {
                followUpRepository.delete(f);
                return ApiResponse.success("Follow-up deleted successfully");
            } catch (DataIntegrityViolationException ex) {
                throw new ValidationException("Cannot delete follow-up. It is referenced by other records.", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Failed to delete follow-up: " + ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public ApiResponse<List<Map<String, Object>>> getFollowUps(FollowUpDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            List<Map<String, Object>> list = followUpDao.getFollowUps(dto);
            return ApiResponse.success("Follow-ups retrieved successfully", list);
        } catch (Exception e) {
            throw new ValidationException("Failed to retrieve follow-ups");
        }
    }

    public ApiResponse<Map<String, Object>> searchFollowUps(FollowUpDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            Map<String, Object> result = followUpDao.searchFollowUps(dto);
            return ApiResponse.success("Follow-ups retrieved successfully", result);
        } catch (Exception e) {
            logger.error("Failed to retrieve follow-ups", e);
            throw new ValidationException("Failed to retrieve follow-ups");
        }
    }

    public ApiResponse<Map<String, Object>> getDetails(Long id) {
        if (id == null) {
            throw new ValidationException("Id is required");
        }
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            FollowUp f = followUpRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Follow-up not found"));

            if (!Objects.equals(f.getClient().getId(), currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to view this follow-up");
            }

            Map<String, Object> data = new HashMap<>(15);
            data.put("id", f.getId());
            data.put("followUpStatus", f.getFollowUpStatus());
            data.put("nextActionDate", f.getNextActionDate());
            data.put("description", f.getDescription());
            data.put("enquiryId", f.getEnquiry() != null ? f.getEnquiry().getId() : null);
            data.put("enquiryName", f.getEnquiry() != null ? f.getEnquiry().getName() : null);
            data.put("clientId", f.getClient() != null ? f.getClient().getId() : null);
            data.put("clientName", f.getClient() != null ? f.getClient().getName() : null);
            data.put("createdAt", f.getCreatedAt());
            data.put("updatedAt", f.getUpdatedAt());
            data.put("createdById", f.getCreatedBy() != null ? f.getCreatedBy().getId() : null);
            data.put("createdByName", f.getCreatedBy() != null ? f.getCreatedBy().getFirstName() + " " + f.getCreatedBy().getLastName() : null);
            data.put("updatedById", f.getUpdatedBy() != null ? f.getUpdatedBy().getId() : null);
            data.put("updatedByName", f.getUpdatedBy() != null ? f.getUpdatedBy().getFirstName() + " " + f.getUpdatedBy().getLastName() : null);

            return ApiResponse.success("Follow-up details retrieved successfully", data);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to retrieve follow-up details", e);
            throw new ValidationException("Failed to retrieve follow-up details");
        }
    }

    private void validate(FollowUpDto dto) {
        if (dto.getNextActionDate() == null) {
            throw new ValidationException("Next action date is required");
        }
        if (dto.getEnquiryId() == null) {
            throw new ValidationException("Enquiry ID is required");
        }
        if (StringUtils.hasText(dto.getFollowUpStatus()) && dto.getFollowUpStatus().trim().length() > 4) {
            throw new ValidationException("Follow-up status too long");
        }
    }

    /**
     * Updates follow-up statuses for a specific enquiry.
     * Only the latest follow-up should have status 'S', all others should be 'C'.
     * This ensures only one active follow-up per enquiry.
     */
    private void updateFollowUpStatusesForEnquiry(Long enquiryId, Long clientId) {
        try {
            // Get all follow-ups for this enquiry, ordered by ID descending (latest first)
            List<FollowUp> followUps = followUpRepository.findByEnquiryIdAndClientIdOrderByIdDesc(enquiryId, clientId);
            
            if (followUps.isEmpty()) {
                return;
            }
            
            // Update all follow-ups to 'C' status first
            for (FollowUp followUp : followUps) {
                if (!"C".equals(followUp.getFollowUpStatus())) {
                    followUp.setFollowUpStatus("C");
                    followUp.setUpdatedAt(OffsetDateTime.now());
                    followUpRepository.save(followUp);
                }
            }
            
            // Set the latest (first in the list) follow-up to 'S' status
            FollowUp latestFollowUp = followUps.get(0);
            if (!"S".equals(latestFollowUp.getFollowUpStatus())) {
                latestFollowUp.setFollowUpStatus("S");
                latestFollowUp.setUpdatedAt(OffsetDateTime.now());
                followUpRepository.save(latestFollowUp);
            }
            
            logger.info("Updated follow-up statuses for enquiry {}: {} follow-ups processed", enquiryId, followUps.size());
        } catch (Exception e) {
            logger.error("Failed to update follow-up statuses for enquiry {}: {}", enquiryId, e.getMessage());
            // Don't throw exception here to avoid breaking the main operation
        }
    }
}
