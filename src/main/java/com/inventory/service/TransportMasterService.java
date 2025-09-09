package com.inventory.service;

import com.inventory.dao.TransportMasterDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.TransportMasterDto;
import com.inventory.entity.TransportMaster;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.TransportMasterRepository;

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
public class TransportMasterService {
    private final TransportMasterRepository transportMasterRepository;
    private final TransportMasterDao transportMasterDao;
    private final UtilityService utilityService;
    private final Logger logger = LoggerFactory.getLogger(TransportMasterService.class);

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> create(TransportMasterDto dto) {
        validate(dto);
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();

            Optional<TransportMaster> existing = transportMasterRepository
                    .findByNameAndClient_Id(dto.getName().trim(), currentUser.getClient().getId());
            if (existing.isPresent()) {
                throw new ValidationException("Transport name already exists");
            }

            TransportMaster t = new TransportMaster();
            t.setName(dto.getName().trim());
            t.setMobile(StringUtils.hasText(dto.getMobile()) ? dto.getMobile().trim() : null);
            t.setGst(StringUtils.hasText(dto.getGst()) ? dto.getGst().trim() : null);
            t.setRemarks(StringUtils.hasText(dto.getRemarks()) ? dto.getRemarks().trim() : null);
            t.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim() : "A");
            t.setClient(currentUser.getClient());
            t.setCreatedBy(currentUser);

            transportMasterRepository.save(t);
            return ApiResponse.success("Transport created successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to create transport");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> update(TransportMasterDto dto) {
        if (dto.getId() == null) {
            throw new ValidationException("Id is required for update");
        }
        validate(dto);
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            TransportMaster t = transportMasterRepository.findById(dto.getId())
                    .orElseThrow(() -> new ValidationException("Transport not found"));

            if (!Objects.equals(t.getClient().getId(), currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to update this transport");
            }

            Optional<TransportMaster> byName = transportMasterRepository
                    .findByNameAndIdNotInAndClient_Id(dto.getName().trim(), Collections.singletonList(t.getId()), currentUser.getClient().getId());
            if (byName.isPresent()) {
                throw new ValidationException("Transport name already exists");
            }   

            t.setName(dto.getName().trim());
            t.setMobile(StringUtils.hasText(dto.getMobile()) ? dto.getMobile().trim() : null);
            t.setGst(StringUtils.hasText(dto.getGst()) ? dto.getGst().trim() : null);
            t.setRemarks(StringUtils.hasText(dto.getRemarks()) ? dto.getRemarks().trim() : null);
            t.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim() : "A");
            t.setUpdatedBy(currentUser);
            t.setUpdatedAt(OffsetDateTime.now());

            transportMasterRepository.save(t);
            return ApiResponse.success("Transport updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update transport");
        }
    }

    @Transactional
    public ApiResponse<?> delete(Long id) {
        try {
            TransportMaster t = transportMasterRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Transport not found", HttpStatus.UNPROCESSABLE_ENTITY));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if (!Objects.equals(t.getClient().getId(), currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to delete this transport", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            try {
                transportMasterRepository.delete(t);
                return ApiResponse.success("Transport deleted successfully");
            } catch (DataIntegrityViolationException e) {
                throw new ValidationException("Cannot delete transport. It is referenced by other records.", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to delete transport: " + e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public ApiResponse<List<Map<String, Object>>> getTransports(TransportMasterDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            List<Map<String, Object>> list = transportMasterDao.getTransports(dto);
            return ApiResponse.success("Transports retrieved successfully", list);
        } catch (Exception e) {
            throw new ValidationException("Failed to retrieve transports");
        }
    }

    public ApiResponse<Map<String, Object>> searchTransports(TransportMasterDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            Map<String, Object> result = transportMasterDao.searchTransports(dto);
            return ApiResponse.success("Transports retrieved successfully", result);
        } catch (Exception e) {
            logger.error("Failed to retrieve transports", e);
            throw new ValidationException("Failed to retrieve transports");
        }
    }

    public ApiResponse<Map<String, Object>> getDetails(Long id) {
        if (id == null) {
            throw new ValidationException("Id is required");
        }
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            TransportMaster t = transportMasterRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Transport not found"));

            if (!Objects.equals(t.getClient().getId(), currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to view this transport");
            }

            Map<String, Object> data = new HashMap<>(12);
            data.put("id", t.getId());
            data.put("name", t.getName());
            data.put("mobile", t.getMobile());
            data.put("gst", t.getGst());
            data.put("remarks", t.getRemarks());
            data.put("status", t.getStatus());
            data.put("clientId", t.getClient() != null ? t.getClient().getId() : null);
            data.put("createdAt", t.getCreatedAt());
            data.put("updatedAt", t.getUpdatedAt());
            data.put("createdById", t.getCreatedBy() != null ? t.getCreatedBy().getId() : null);
            data.put("createdByName", t.getCreatedBy() != null ? t.getCreatedBy().getFirstName() + " " + t.getCreatedBy().getLastName() : null);
            data.put("updatedById", t.getUpdatedBy() != null ? t.getUpdatedBy().getId() : null);
            data.put("updatedByName", t.getUpdatedBy() != null ? t.getUpdatedBy().getFirstName() + " " + t.getUpdatedBy().getLastName() : null);

            return ApiResponse.success("Transport details retrieved successfully", data);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to retrieve transport details", e);
            throw new ValidationException("Failed to retrieve transport details");
        }
    }

    private void validate(TransportMasterDto dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new ValidationException("Transport name is required");
        }
        if (dto.getName().trim().length() > 255) {
            throw new ValidationException("Transport name too long");
        }
        if (StringUtils.hasText(dto.getMobile()) && dto.getMobile().trim().length() > 20) {
            throw new ValidationException("Mobile too long");
        }
        if (StringUtils.hasText(dto.getGst()) && dto.getGst().trim().length() > 30) {
            throw new ValidationException("GST too long");
        }
    }
}
