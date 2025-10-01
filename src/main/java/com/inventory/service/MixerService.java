package com.inventory.service;

import com.inventory.dao.MixerDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.MixerDto;
import com.inventory.entity.Batch;
import com.inventory.entity.Mixer;
import com.inventory.entity.Product;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.BachRepository;
import com.inventory.repository.MixerRepository;
import com.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MixerService {
    private final MixerRepository mixerRepository;
    private final BachRepository bachRepository;
    private final ProductRepository productRepository;
    private final MixerDao mixerDao;
    private final UtilityService utilityService;

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> create(MixerDto dto) {
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

            Mixer m = new Mixer();
            m.setBatch(batch);
            m.setProduct(product);
            m.setQuantity(dto.getQuantity());
            m.setClient(currentUser.getClient());
            mixerRepository.save(m);
            return ApiResponse.success("Mixer created successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to create mixer", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> update(Long id, MixerDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Mixer m = mixerRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Mixer not found", HttpStatus.NOT_FOUND));
            if (!m.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            if (dto.getBatchId() != null) {
                Batch batch = bachRepository.findById(dto.getBatchId())
                        .orElseThrow(() -> new ValidationException("Bach not found", HttpStatus.NOT_FOUND));
                if (!batch.getClient().getId().equals(currentUser.getClient().getId())) {
                    throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
                }
                m.setBatch(batch);
            }
            if (dto.getProductId() != null) {
                Product product = productRepository.findById(dto.getProductId())
                        .orElseThrow(() -> new ValidationException("Product not found", HttpStatus.NOT_FOUND));
                m.setProduct(product);
            }
            if (dto.getQuantity() != null) m.setQuantity(dto.getQuantity());
            mixerRepository.save(m);
            return ApiResponse.success("Mixer updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update mixer", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> delete(Long id) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Mixer m = mixerRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Mixer not found", HttpStatus.NOT_FOUND));
            if (!m.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            mixerRepository.delete(m);
            return ApiResponse.success("Mixer deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to delete mixer", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ApiResponse<Map<String, Object>> search(MixerDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            Map<String, Object> resp = mixerDao.search(dto);
            return ApiResponse.success("Mixers fetched", resp);
        } catch (Exception e) {
            throw new ValidationException("Failed to fetch mixers", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ApiResponse<?> getDetails(Long id) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Mixer m = mixerRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Mixer not found", HttpStatus.NOT_FOUND));
            if (!m.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            MixerDto dto = new MixerDto();
            dto.setId(m.getId());
            dto.setBatchId(m.getBatch() != null ? m.getBatch().getId() : null);
            dto.setProductId(m.getProduct() != null ? m.getProduct().getId() : null);
            dto.setQuantity(m.getQuantity());
            return ApiResponse.success("Mixer details fetched", dto);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to fetch mixer details", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validate(MixerDto dto) {
        if (dto == null || dto.getBatchId() == null || dto.getProductId() == null || dto.getQuantity() == null) {
            throw new ValidationException("batchId, productId and quantity are required", HttpStatus.BAD_REQUEST);
        }
    }
}


