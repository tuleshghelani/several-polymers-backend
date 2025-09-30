package com.inventory.service;

import com.inventory.dao.BachDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.BachDto;
import com.inventory.entity.Bach;
import com.inventory.entity.MachineMaster;
import com.inventory.entity.UserMaster;
import com.inventory.dto.request.BachUpsertRequestDto;
import com.inventory.exception.ValidationException;
import com.inventory.repository.BachRepository;
import com.inventory.repository.MachineMasterRepository;
import com.inventory.repository.MixerRepository;
import com.inventory.repository.ProductionRepository;
import com.inventory.entity.Mixer;
import com.inventory.entity.Product;
import com.inventory.entity.Production;
import com.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class BachService {
    private final BachRepository bachRepository;
    private final MachineMasterRepository machineMasterRepository;
    private final BachDao bachDao;
    private final UtilityService utilityService;
    private final MixerRepository mixerRepository;
    private final ProductionRepository productionRepository;
    private final ProductRepository productRepository;
    private final ProductQuantityService productQuantityService;

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> create(BachDto dto) {
        try {
            validateCreate(dto);
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Bach b = new Bach();
            b.setDate(dto.getDate());
            b.setShift(dto.getShift().trim());
            b.setName(generateBachName(dto.getDate()));
            b.setResignBagUse(dto.getResignBagUse());
            b.setResignBagOpeningStock(dto.getResignBagOpeningStock());
            b.setCpwBagUse(dto.getCpwBagUse());
            b.setCpwBagOpeningStock(dto.getCpwBagOpeningStock());
            MachineMaster m = machineMasterRepository.findById(dto.getMachineId())
                    .orElseThrow(() -> new ValidationException("Machine not found", HttpStatus.NOT_FOUND));
            if (!m.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            b.setMachine(m);
            b.setCreatedBy(currentUser);
            b.setClient(currentUser.getClient());
            bachRepository.save(b);
            return ApiResponse.success("Bach created successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to create bach", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> update(Long id, BachDto dto) {
        try {
            if (id == null) throw new ValidationException("Id is required", HttpStatus.BAD_REQUEST);
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Bach b = bachRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Bach not found", HttpStatus.NOT_FOUND));
            if (!b.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            if (dto.getDate() != null) b.setDate(dto.getDate());
            if (StringUtils.hasText(dto.getShift())) b.setShift(dto.getShift().trim());
            if (dto.getDate() != null) {
                b.setName(generateBachName(dto.getDate()));
            }
            if (dto.getResignBagUse() != null) b.setResignBagUse(dto.getResignBagUse());
            if (dto.getResignBagOpeningStock() != null) b.setResignBagOpeningStock(dto.getResignBagOpeningStock());
            if (dto.getCpwBagUse() != null) b.setCpwBagUse(dto.getCpwBagUse());
            if (dto.getCpwBagOpeningStock() != null) b.setCpwBagOpeningStock(dto.getCpwBagOpeningStock());
            if (dto.getMachineId() != null) {
                MachineMaster m = machineMasterRepository.findById(dto.getMachineId())
                        .orElseThrow(() -> new ValidationException("Machine not found", HttpStatus.NOT_FOUND));
                if (!m.getClient().getId().equals(currentUser.getClient().getId())) {
                    throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
                }
                b.setMachine(m);
            }
            bachRepository.save(b);
            return ApiResponse.success("Bach updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update bach", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> delete(Long id) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Bach b = bachRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Bach not found", HttpStatus.NOT_FOUND));
            if (!b.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            
            // Revert quantities before deleting the bach
            revertMixerQuantities(b.getId());
            revertProductionQuantities(b.getId());
            
            bachRepository.delete(b);
            return ApiResponse.success("Bach deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to delete bach", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ApiResponse<Map<String, Object>> search(BachDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            Map<String, Object> resp = bachDao.search(dto);
            return ApiResponse.success("Bachs fetched", resp);
        } catch (Exception e) {
            throw new ValidationException("Failed to fetch bachs", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ApiResponse<?> getDetails(Long id) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Bach b = bachRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Bach not found", HttpStatus.NOT_FOUND));
            if (!b.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            BachDto dto = new BachDto();
            dto.setId(b.getId());
            dto.setDate(b.getDate());
            dto.setShift(b.getShift());
            dto.setName(b.getName());
            dto.setResignBagUse(b.getResignBagUse());
            dto.setResignBagOpeningStock(b.getResignBagOpeningStock());
            dto.setCpwBagUse(b.getCpwBagUse());
            dto.setCpwBagOpeningStock(b.getCpwBagOpeningStock());
            dto.setMachineId(b.getMachine() != null ? b.getMachine().getId() : null);
            return ApiResponse.success("Bach details fetched", dto);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to fetch bach details", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateCreate(BachDto dto) {
        if (dto == null || dto.getDate() == null || !StringUtils.hasText(dto.getShift()) || dto.getMachineId() == null) {
            throw new ValidationException("date, shift and machineId are required", HttpStatus.BAD_REQUEST);
        }
    }

    private String generateBachName(java.time.LocalDate date) {
        UserMaster currentUser = utilityService.getCurrentLoggedInUser();
        long count = bachRepository.countByClientAndDate(currentUser.getClient().getId(), date);
        long next = count + 1;
        String seq = String.format("%03d", next);
        return date.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + seq;
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> upsert(BachUpsertRequestDto req) {
        try {
            if (req == null || req.getDate() == null || !StringUtils.hasText(req.getShift()) || req.getMachineId() == null) {
                throw new ValidationException("date, shift and machineId are required", HttpStatus.BAD_REQUEST);
            }
            if (req.getMixer() == null || req.getMixer().isEmpty()) {
                throw new ValidationException("At least one mixer item is required", HttpStatus.BAD_REQUEST);
            }
            if (req.getProduction() == null || req.getProduction().isEmpty()) {
                throw new ValidationException("At least one production item is required", HttpStatus.BAD_REQUEST);
            }
            // per-item validations
            for (BachUpsertRequestDto.MixerItem mi : req.getMixer()) {
                if (mi.getProductId() == null) {
                    throw new ValidationException("Mixer product_id is required", HttpStatus.BAD_REQUEST);
                }
                if (mi.getQuantity() == null) {
                    throw new ValidationException("Mixer quantity is required", HttpStatus.BAD_REQUEST);
                }
            }
            for (BachUpsertRequestDto.ProductionItem pi : req.getProduction()) {
                if (pi.getProductId() == null) {
                    throw new ValidationException("Production product_id is required", HttpStatus.BAD_REQUEST);
                }
                if (pi.getQuantity() == null) {
                    throw new ValidationException("Production quantity is required", HttpStatus.BAD_REQUEST);
                }
            }
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            MachineMaster machine = machineMasterRepository.findById(req.getMachineId())
                    .orElseThrow(() -> new ValidationException("Machine not found", HttpStatus.NOT_FOUND));
            if (!machine.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }

            Bach bach;
            boolean creating = (req.getId() == null);
            if (creating) {
                bach = new Bach();
                bach.setCreatedBy(currentUser);
                bach.setClient(currentUser.getClient());
                bach.setDate(req.getDate());
                bach.setShift(req.getShift().trim());
                bach.setName(generateBachName(req.getDate()));
                bach.setResignBagUse(req.getResignBagUse());
                bach.setResignBagOpeningStock(req.getResignBagOpeningStock());
                bach.setCpwBagUse(req.getCpwBagUse());
                bach.setCpwBagOpeningStock(req.getCpwBagOpeningStock());
                bach.setMachine(machine);
                bach = bachRepository.save(bach);
            } else {
                bach = bachRepository.findById(req.getId())
                        .orElseThrow(() -> new ValidationException("Bach not found", HttpStatus.NOT_FOUND));
                if (!bach.getClient().getId().equals(currentUser.getClient().getId())) {
                    throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
                }
                boolean dateChanged = req.getDate() != null && !req.getDate().equals(bach.getDate());
                if (req.getDate() != null) bach.setDate(req.getDate());
                if (StringUtils.hasText(req.getShift())) bach.setShift(req.getShift().trim());
                if (req.getResignBagUse() != null) bach.setResignBagUse(req.getResignBagUse());
                if (req.getResignBagOpeningStock() != null) bach.setResignBagOpeningStock(req.getResignBagOpeningStock());
                if (req.getCpwBagUse() != null) bach.setCpwBagUse(req.getCpwBagUse());
                if (req.getCpwBagOpeningStock() != null) bach.setCpwBagOpeningStock(req.getCpwBagOpeningStock());
                bach.setMachine(machine);
                if (dateChanged) {
                    bach.setName(generateBachName(bach.getDate()));
                }
                bach = bachRepository.save(bach);
            }

            // Handle mixer items with quantity updates
            if (req.getMixer() != null) {
                // If updating, first revert previous mixer quantities
                if (!creating) {
                    revertMixerQuantities(bach.getId());
                }
                
                mixerRepository.deleteByBachId(bach.getId());
                for (BachUpsertRequestDto.MixerItem item : req.getMixer()) {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new ValidationException("Product not found", HttpStatus.NOT_FOUND));
                    
                    // Create mixer record
                    Mixer m = new Mixer();
                    m.setBach(bach);
                    m.setProduct(product);
                    m.setQuantity(item.getQuantity());
                    m.setClient(currentUser.getClient());
                    mixerRepository.save(m);
                    
                    // Update product quantity (subtract for mixer consumption)
                    productQuantityService.updateProductQuantity(
                        product.getId(), 
                        item.getQuantity(), 
                        false, // isPurchase
                        true,  // isSale (consumption)
                        null   // isBlock
                    );
                }
            }

            // Handle production items with quantity updates
            if (req.getProduction() != null) {
                // If updating, first revert previous production quantities
                if (!creating) {
                    revertProductionQuantities(bach.getId());
                }
                
                productionRepository.deleteByBachId(bach.getId());
                for (BachUpsertRequestDto.ProductionItem item : req.getProduction()) {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new ValidationException("Product not found", HttpStatus.NOT_FOUND));
                    
                    // Create production record
                    Production p = new Production();
                    p.setBach(bach);
                    p.setProduct(product);
                    p.setQuantity(item.getQuantity());
                    p.setNumberOfRoll(item.getNumberOfRoll());
                    p.setClient(currentUser.getClient());
                    productionRepository.save(p);
                    
                    // Update product quantity (add for production output)
                    productQuantityService.updateProductQuantity(
                        product.getId(), 
                        item.getQuantity(), 
                        true,  // isPurchase (production)
                        false, // isSale
                        null   // isBlock
                    );
                }
            }
            return ApiResponse.success(creating ? "Bach created successfully" : "Bach updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to upsert bach", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Reverts mixer quantities by adding them back to product stock
     * This is called when updating a bach to undo previous mixer consumption
     */
    private void revertMixerQuantities(Long bachId) {
        try {
            var existingMixers = mixerRepository.findByBachId(bachId);
            for (Mixer mixer : existingMixers) {
                if (mixer.getProduct() != null && mixer.getQuantity() != null) {
                    // Add back the quantity that was previously consumed
                    productQuantityService.updateProductQuantity(
                        mixer.getProduct().getId(),
                        mixer.getQuantity(),
                        true,  // isPurchase (adding back)
                        false, // isSale
                        null   // isBlock
                    );
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the transaction
            // This ensures the main operation can continue
            System.err.println("Error reverting mixer quantities for bach " + bachId + ": " + e.getMessage());
        }
    }

    /**
     * Reverts production quantities by subtracting them from product stock
     * This is called when updating a bach to undo previous production output
     */
    private void revertProductionQuantities(Long bachId) {
        try {
            var existingProductions = productionRepository.findByBachId(bachId);
            for (Production production : existingProductions) {
                if (production.getProduct() != null && production.getQuantity() != null) {
                    // Subtract the quantity that was previously produced
                    productQuantityService.updateProductQuantity(
                        production.getProduct().getId(),
                        production.getQuantity(),
                        false, // isPurchase
                        true,  // isSale (removing production)
                        null   // isBlock
                    );
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the transaction
            // This ensures the main operation can continue
            System.err.println("Error reverting production quantities for bach " + bachId + ": " + e.getMessage());
        }
    }
}


