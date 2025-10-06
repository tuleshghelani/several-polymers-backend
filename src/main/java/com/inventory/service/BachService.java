package com.inventory.service;

import com.inventory.dao.BachDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.BachDto;
import com.inventory.entity.*;
import com.inventory.dto.request.BachUpsertRequestDto;
import com.inventory.exception.ValidationException;
import com.inventory.repository.BachRepository;
import com.inventory.repository.MachineMasterRepository;
import com.inventory.repository.MixerRepository;
import com.inventory.repository.ProductionRepository;
import com.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
            Batch b = new Batch();
            b.setDate(dto.getDate());
            b.setShift(dto.getShift().trim());
            b.setName(generateBachName(dto.getDate()));
            b.setOperator(dto.getOperator());
            b.setResignBagUse(dto.getResignBagUse());
            b.setCpwBagUse(dto.getCpwBagUse());
            MachineMaster m = machineMasterRepository.findById(dto.getMachineId())
                    .orElseThrow(() -> new ValidationException("Machine not found", HttpStatus.NOT_FOUND));
            if (!m.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            b.setMachine(m);
            b.setCreatedBy(currentUser);
            b.setClient(currentUser.getClient());

            Map<String, java.math.BigDecimal> stocks = getResignCpwStocks(currentUser.getClient().getId());
            b.setResignBagOpeningStock(stocks.getOrDefault("RESIGN", java.math.BigDecimal.ZERO));
            b.setCpwBagOpeningStock(stocks.getOrDefault("CPW", java.math.BigDecimal.ZERO));
            bachRepository.save(b);

            // Adjust inventory for RESIGN and CPW bag uses (subtract use from stock)
            adjustBagUseOnCreate(currentUser, b.getResignBagUse(), b.getCpwBagUse());
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
            Batch b = bachRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Bach not found", HttpStatus.NOT_FOUND));
            if (!b.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            if (dto.getDate() != null) b.setDate(dto.getDate());
            if (StringUtils.hasText(dto.getShift())) b.setShift(dto.getShift().trim());
            if (dto.getDate() != null) {
                b.setName(generateBachName(dto.getDate()));
            }
            if (StringUtils.hasText(dto.getOperator())) b.setOperator(dto.getOperator());
            java.math.BigDecimal oldResignUse = b.getResignBagUse();
            java.math.BigDecimal oldCpwUse = b.getCpwBagUse();
            if (dto.getResignBagUse() != null) b.setResignBagUse(dto.getResignBagUse());
            if (dto.getCpwBagUse() != null) b.setCpwBagUse(dto.getCpwBagUse());
            Map<String, java.math.BigDecimal> stocks = getResignCpwStocks(currentUser.getClient().getId());
            b.setResignBagOpeningStock(stocks.getOrDefault("RESIGN", java.math.BigDecimal.ZERO));
            b.setCpwBagOpeningStock(stocks.getOrDefault("CPW", java.math.BigDecimal.ZERO));
            if (dto.getMachineId() != null) {
                MachineMaster m = machineMasterRepository.findById(dto.getMachineId())
                        .orElseThrow(() -> new ValidationException("Machine not found", HttpStatus.NOT_FOUND));
                if (!m.getClient().getId().equals(currentUser.getClient().getId())) {
                    throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
                }
                b.setMachine(m);
            }
            bachRepository.save(b);

            // If changed, add back old and subtract new
            adjustBagUseOnUpdate(currentUser, oldResignUse, b.getResignBagUse(), oldCpwUse, b.getCpwBagUse());
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
            Batch b = bachRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Bach not found", HttpStatus.NOT_FOUND));
            if (!b.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            
            // Revert RESIGN/CPW bag uses back to inventory
            revertBagUseOnDelete(currentUser, b.getResignBagUse(), b.getCpwBagUse());
            
            // Revert quantities before deleting the bach
            revertMixerQuantities(b.getId());
            revertProductionQuantities(b.getId());
            
            // Delete related records first
            mixerRepository.deleteByBatchId(b.getId());
            productionRepository.deleteByBatchId(b.getId());
            
            // Then delete the batch record
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
            Batch b = bachRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Bach not found", HttpStatus.NOT_FOUND));
            if (!b.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            BachDto dto = new BachDto();
            dto.setId(b.getId());
            dto.setDate(b.getDate());
            dto.setShift(b.getShift());
            dto.setName(b.getName());
            dto.setOperator(b.getOperator());
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

    public ApiResponse<BachDto> getFullDetails(BachDto request) {
        try {
            if (request.getId() == null) {
                throw new ValidationException("Bach ID is required", HttpStatus.BAD_REQUEST);
            }
            
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Batch b = bachRepository.findById(request.getId())
                    .orElseThrow(() -> new ValidationException("Bach not found", HttpStatus.NOT_FOUND));
            if (!b.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            
            BachDto response = new BachDto();
            
            // Basic bach information
            response.setId(b.getId());
            response.setDate(b.getDate());
            response.setShift(b.getShift());
            response.setName(b.getName());
            response.setOperator(b.getOperator());
            response.setResignBagUse(b.getResignBagUse());
            response.setResignBagOpeningStock(b.getResignBagOpeningStock());
            response.setCpwBagUse(b.getCpwBagUse());
            response.setCpwBagOpeningStock(b.getCpwBagOpeningStock());
            response.setMachineId(b.getMachine() != null ? b.getMachine().getId() : null);
            response.setMachineName(b.getMachine() != null ? b.getMachine().getName() : null);
            response.setClientId(b.getClient() != null ? b.getClient().getId() : null);
            response.setClientName(b.getClient() != null ? b.getClient().getName() : null);
            response.setCreatedBy(b.getCreatedBy() != null ? 
                (b.getCreatedBy().getFirstName() != null ? b.getCreatedBy().getFirstName() : "") + 
                (b.getCreatedBy().getLastName() != null ? " " + b.getCreatedBy().getLastName() : "") : null);
            response.setCreatedAt(b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
            response.setUpdatedAt(null); // Bach entity doesn't have updatedAt field
            
            // Get mixer items with full product details
            List<Mixer> mixers = mixerRepository.findByBatchId(request.getId());
            response.setMixerItems(mixers.stream()
                .map(this::mapMixerToDetailDto)
                .collect(Collectors.toList()));
            
            // Get production items with full product details
            List<Production> productions = productionRepository.findByBatchId(request.getId());
            response.setProductionItems(productions.stream()
                .map(this::mapProductionToDetailDto)
                .collect(Collectors.toList()));
            
            return ApiResponse.success("Bach full details fetched", response);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to fetch bach full details", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public byte[] exportBachMixerProductionExcel(BachDto filter) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            filter.setClientId(currentUser.getClient().getId());

            List<Long> batchIds = bachDao.findBatchIdsForExport(filter);

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Batch Report");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Batch ID", "Date", "Shift", "Name", "Operator", "Machine", 
                "RESIGN Use", "RESIGN Opening", "CPW Use", "CPW Opening", 
                "Mixer Details", "Production Details"
            };
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Long batchId : batchIds) {
                Batch b = bachRepository.findById(batchId)
                        .orElse(null);
                if (b == null || !b.getClient().getId().equals(currentUser.getClient().getId())) continue;

                // Load details
                List<Mixer> mixers = mixerRepository.findByBatchId(batchId);
                List<Production> productions = productionRepository.findByBatchId(batchId);

                Row row = sheet.createRow(rowNum++);
                
                // Basic batch info
                row.createCell(0).setCellValue(b.getId() != null ? b.getId().toString() : "");
                row.createCell(1).setCellValue(b.getDate() != null ? b.getDate().toString() : "");
                row.createCell(2).setCellValue(b.getShift() != null ? b.getShift() : "");
                row.createCell(3).setCellValue(b.getName() != null ? b.getName() : "");
                row.createCell(4).setCellValue(b.getOperator() != null ? b.getOperator() : "");
                row.createCell(5).setCellValue(b.getMachine() != null ? b.getMachine().getName() : "");
                row.createCell(6).setCellValue(b.getResignBagUse() != null ? b.getResignBagUse().toString() : "");
                row.createCell(7).setCellValue(b.getResignBagOpeningStock() != null ? b.getResignBagOpeningStock().toString() : "");
                row.createCell(8).setCellValue(b.getCpwBagUse() != null ? b.getCpwBagUse().toString() : "");
                row.createCell(9).setCellValue(b.getCpwBagOpeningStock() != null ? b.getCpwBagOpeningStock().toString() : "");

                // Mixer details with all product information - each product on new line
                StringBuilder mixerDetails = new StringBuilder();
                if (mixers.isEmpty()) {
                    mixerDetails.append("No mixer items");
                } else {
                    for (int i = 0; i < mixers.size(); i++) {
                        Mixer m = mixers.get(i);
                        if (m.getProduct() != null) {
                            mixerDetails.append("MIXER ITEM ").append(i + 1).append(":\n");
                            mixerDetails.append("Product: ").append(m.getProduct().getName() != null ? m.getProduct().getName() : "").append("\n");
                            mixerDetails.append("Description: ").append(m.getProduct().getDescription() != null ? m.getProduct().getDescription() : "").append("\n");
                            mixerDetails.append("Measurement: ").append(m.getProduct().getMeasurement() != null ? m.getProduct().getMeasurement() : "").append("\n");
                            mixerDetails.append("Weight: ").append(m.getProduct().getWeight() != null ? m.getProduct().getWeight().toString() : "").append("\n");
                            mixerDetails.append("Purchase Amount: ").append(m.getProduct().getPurchaseAmount() != null ? m.getProduct().getPurchaseAmount().toString() : "").append("\n");
                            mixerDetails.append("Sale Amount: ").append(m.getProduct().getSaleAmount() != null ? m.getProduct().getSaleAmount().toString() : "").append("\n");
                            mixerDetails.append("Remaining Qty: ").append(m.getProduct().getRemainingQuantity() != null ? m.getProduct().getRemainingQuantity().toString() : "").append("\n");
                            mixerDetails.append("Tax %: ").append(m.getProduct().getTaxPercentage() != null ? m.getProduct().getTaxPercentage().toString() : "").append("\n");
                            mixerDetails.append("Status: ").append(m.getProduct().getStatus() != null ? m.getProduct().getStatus() : "").append("\n");
                            mixerDetails.append("Category: ").append(m.getProduct().getCategory() != null ? m.getProduct().getCategory().getName() : "").append("\n");
                            mixerDetails.append("Quantity Used: ").append(m.getQuantity() != null ? m.getQuantity().toString() : "").append("\n");
                            if (i < mixers.size() - 1) {
                                mixerDetails.append("\n=====================================\n");
                            }
                        }
                    }
                }
                row.createCell(10).setCellValue(mixerDetails.toString());

                // Production details with all product information - each product on new line
                StringBuilder productionDetails = new StringBuilder();
                if (productions.isEmpty()) {
                    productionDetails.append("No production items");
                } else {
                    for (int i = 0; i < productions.size(); i++) {
                        Production p = productions.get(i);
                        if (p.getProduct() != null) {
                            productionDetails.append("PRODUCTION ITEM ").append(i + 1).append(":\n");
                            productionDetails.append("Product: ").append(p.getProduct().getName() != null ? p.getProduct().getName() : "").append("\n");
                            productionDetails.append("Description: ").append(p.getProduct().getDescription() != null ? p.getProduct().getDescription() : "").append("\n");
                            productionDetails.append("Measurement: ").append(p.getProduct().getMeasurement() != null ? p.getProduct().getMeasurement() : "").append("\n");
                            productionDetails.append("Weight: ").append(p.getProduct().getWeight() != null ? p.getProduct().getWeight().toString() : "").append("\n");
                            productionDetails.append("Purchase Amount: ").append(p.getProduct().getPurchaseAmount() != null ? p.getProduct().getPurchaseAmount().toString() : "").append("\n");
                            productionDetails.append("Sale Amount: ").append(p.getProduct().getSaleAmount() != null ? p.getProduct().getSaleAmount().toString() : "").append("\n");
                            productionDetails.append("Remaining Qty: ").append(p.getProduct().getRemainingQuantity() != null ? p.getProduct().getRemainingQuantity().toString() : "").append("\n");
                            productionDetails.append("Tax %: ").append(p.getProduct().getTaxPercentage() != null ? p.getProduct().getTaxPercentage().toString() : "").append("\n");
                            productionDetails.append("Status: ").append(p.getProduct().getStatus() != null ? p.getProduct().getStatus() : "").append("\n");
                            productionDetails.append("Category: ").append(p.getProduct().getCategory() != null ? p.getProduct().getCategory().getName() : "").append("\n");
                            productionDetails.append("Quantity Produced: ").append(p.getQuantity() != null ? p.getQuantity().toString() : "").append("\n");
                            productionDetails.append("Number of Rolls: ").append(p.getNumberOfRoll() != null ? p.getNumberOfRoll().toString() : "").append("\n");
                            if (i < productions.size() - 1) {
                                productionDetails.append("\n=====================================\n");
                            }
                        }
                    }
                }
                row.createCell(11).setCellValue(productionDetails.toString());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ValidationException("Failed to export bach report to Excel", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new ValidationException("Failed to export bach report", HttpStatus.INTERNAL_SERVER_ERROR);
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

            Batch batch;
            boolean creating = (req.getId() == null);
            if (creating) {
                batch = new Batch();
                batch.setCreatedBy(currentUser);
                batch.setClient(currentUser.getClient());
                batch.setDate(req.getDate());
                batch.setShift(req.getShift().trim());
                batch.setName(generateBachName(req.getDate()));
                batch.setOperator(req.getOperator());
                batch.setResignBagUse(req.getResignBagUse());
                batch.setCpwBagUse(req.getCpwBagUse());
                batch.setMachine(machine);
                Map<String, java.math.BigDecimal> stocks = getResignCpwStocks(currentUser.getClient().getId());
                batch.setResignBagOpeningStock(stocks.getOrDefault("RESIGN", java.math.BigDecimal.ZERO));
                batch.setCpwBagOpeningStock(stocks.getOrDefault("CPW", java.math.BigDecimal.ZERO));
                batch = bachRepository.save(batch);

                // Adjust inventory on create path
                adjustBagUseOnCreate(currentUser, batch.getResignBagUse(), batch.getCpwBagUse());
            } else {
                batch = bachRepository.findById(req.getId())
                        .orElseThrow(() -> new ValidationException("Bach not found", HttpStatus.NOT_FOUND));
                if (!batch.getClient().getId().equals(currentUser.getClient().getId())) {
                    throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
                }
                boolean dateChanged = req.getDate() != null && !req.getDate().equals(batch.getDate());
                if (req.getDate() != null) batch.setDate(req.getDate());
                if (StringUtils.hasText(req.getShift())) batch.setShift(req.getShift().trim());
                if (StringUtils.hasText(req.getOperator())) batch.setOperator(req.getOperator());
                java.math.BigDecimal oldResignUse = batch.getResignBagUse();
                java.math.BigDecimal oldCpwUse = batch.getCpwBagUse();
                if (req.getResignBagUse() != null) batch.setResignBagUse(req.getResignBagUse());
                if (req.getCpwBagUse() != null) batch.setCpwBagUse(req.getCpwBagUse());
                batch.setMachine(machine);
                Map<String, java.math.BigDecimal> stocks = getResignCpwStocks(currentUser.getClient().getId());
                batch.setResignBagOpeningStock(stocks.getOrDefault("RESIGN", java.math.BigDecimal.ZERO));
                batch.setCpwBagOpeningStock(stocks.getOrDefault("CPW", java.math.BigDecimal.ZERO));
                if (dateChanged) {
                    batch.setName(generateBachName(batch.getDate()));
                }
                batch = bachRepository.save(batch);

                // Adjust inventory on update path
                adjustBagUseOnUpdate(currentUser, oldResignUse, batch.getResignBagUse(), oldCpwUse, batch.getCpwBagUse());
            }

            // Handle mixer items with quantity updates
            if (req.getMixer() != null) {
                // If updating, first revert previous mixer quantities
                if (!creating) {
                    revertMixerQuantities(batch.getId());
                }
                
                mixerRepository.deleteByBatchId(batch.getId());
                for (BachUpsertRequestDto.MixerItem item : req.getMixer()) {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new ValidationException("Product not found", HttpStatus.NOT_FOUND));
                    
                    // Create mixer record
                    Mixer m = new Mixer();
                    m.setBatch(batch);
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
                    revertProductionQuantities(batch.getId());
                }
                
                productionRepository.deleteByBatchId(batch.getId());
                for (BachUpsertRequestDto.ProductionItem item : req.getProduction()) {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new ValidationException("Product not found", HttpStatus.NOT_FOUND));
                    
                    // Create production record
                    Production p = new Production();
                    p.setBatch(batch);
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

    private Map<String, java.math.BigDecimal> getResignCpwStocks(Long clientId) {
        try {
            List<String> codes = Arrays.asList("RESIGN", "CPW");
            List<Product> products = productRepository.findByProductCodeInAndClient_Id(codes, clientId);
            Map<String, java.math.BigDecimal> result = new HashMap<>();
            result.put("RESIGN", java.math.BigDecimal.ZERO);
            result.put("CPW", java.math.BigDecimal.ZERO);
            for (Product p : products) {
                if (p.getProductCode() != null && p.getRemainingQuantity() != null) {
                    result.put(p.getProductCode(), p.getRemainingQuantity());
                }
            }
            return result;
        } catch (Exception e) {
            Map<String, java.math.BigDecimal> fallback = new HashMap<>();
            fallback.put("RESIGN", java.math.BigDecimal.ZERO);
            fallback.put("CPW", java.math.BigDecimal.ZERO);
            return fallback;
        }
    }

    private void adjustBagUseOnCreate(UserMaster currentUser, java.math.BigDecimal resignUse, java.math.BigDecimal cpwUse) {
        try {
            if (resignUse != null && resignUse.compareTo(java.math.BigDecimal.ZERO) > 0) {
                productRepository.findByProductCodeInAndClient_Id(java.util.Arrays.asList("RESIGN"), currentUser.getClient().getId())
                        .stream().findFirst().ifPresent(p -> {
                            productQuantityService.updateProductQuantity(
                                    p.getId(), resignUse, false, true, null
                            );
                        });
            }
            if (cpwUse != null && cpwUse.compareTo(java.math.BigDecimal.ZERO) > 0) {
                productRepository.findByProductCodeInAndClient_Id(java.util.Arrays.asList("CPW"), currentUser.getClient().getId())
                        .stream().findFirst().ifPresent(p -> {
                            productQuantityService.updateProductQuantity(
                                    p.getId(), cpwUse, false, true, null
                            );
                        });
            }
        } catch (Exception ignored) {}
    }

    private void adjustBagUseOnUpdate(UserMaster currentUser, java.math.BigDecimal oldResignUse, java.math.BigDecimal newResignUse,
                                      java.math.BigDecimal oldCpwUse, java.math.BigDecimal newCpwUse) {
        try {
            java.math.BigDecimal oldR = oldResignUse != null ? oldResignUse : java.math.BigDecimal.ZERO;
            java.math.BigDecimal newR = newResignUse != null ? newResignUse : java.math.BigDecimal.ZERO;
            if (oldR.compareTo(newR) != 0) {
                productRepository.findByProductCodeInAndClient_Id(java.util.Arrays.asList("RESIGN"), currentUser.getClient().getId())
                        .stream().findFirst().ifPresent(p -> {
                            if (oldR.compareTo(java.math.BigDecimal.ZERO) > 0) {
                                productQuantityService.updateProductQuantity(p.getId(), oldR, true, false, null);
                            }
                            if (newR.compareTo(java.math.BigDecimal.ZERO) > 0) {
                                productQuantityService.updateProductQuantity(p.getId(), newR, false, true, null);
                            }
                        });
            }

            java.math.BigDecimal oldC = oldCpwUse != null ? oldCpwUse : java.math.BigDecimal.ZERO;
            java.math.BigDecimal newC = newCpwUse != null ? newCpwUse : java.math.BigDecimal.ZERO;
            if (oldC.compareTo(newC) != 0) {
                productRepository.findByProductCodeInAndClient_Id(java.util.Arrays.asList("CPW"), currentUser.getClient().getId())
                        .stream().findFirst().ifPresent(p -> {
                            if (oldC.compareTo(java.math.BigDecimal.ZERO) > 0) {
                                productQuantityService.updateProductQuantity(p.getId(), oldC, true, false, null);
                            }
                            if (newC.compareTo(java.math.BigDecimal.ZERO) > 0) {
                                productQuantityService.updateProductQuantity(p.getId(), newC, false, true, null);
                            }
                        });
            }
        } catch (Exception ignored) {}
    }

    /**
     * Adds back RESIGN and CPW bag uses to inventory when a bach is deleted
     */
    private void revertBagUseOnDelete(UserMaster currentUser, java.math.BigDecimal resignUse, java.math.BigDecimal cpwUse) {
        try {
            if (resignUse != null && resignUse.compareTo(java.math.BigDecimal.ZERO) > 0) {
                productRepository.findByProductCodeInAndClient_Id(java.util.Arrays.asList("RESIGN"), currentUser.getClient().getId())
                        .stream().findFirst().ifPresent(p -> {
                            productQuantityService.updateProductQuantity(
                                    p.getId(), resignUse, true, false, null
                            );
                        });
            }
            if (cpwUse != null && cpwUse.compareTo(java.math.BigDecimal.ZERO) > 0) {
                productRepository.findByProductCodeInAndClient_Id(java.util.Arrays.asList("CPW"), currentUser.getClient().getId())
                        .stream().findFirst().ifPresent(p -> {
                            productQuantityService.updateProductQuantity(
                                    p.getId(), cpwUse, true, false, null
                            );
                        });
            }
        } catch (Exception ignored) {}
    }

    /**
     * Reverts mixer quantities by adding them back to product stock
     * This is called when updating a bach to undo previous mixer consumption
     */
    private void revertMixerQuantities(Long batchId) {
        try {
            var existingMixers = mixerRepository.findByBatchId(batchId);
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
            System.err.println("Error reverting mixer quantities for bach " + batchId + ": " + e.getMessage());
        }
    }

    /**
     * Reverts production quantities by subtracting them from product stock
     * This is called when updating a bach to undo previous production output
     */
    private void revertProductionQuantities(Long batchId) {
        try {
            var existingProductions = productionRepository.findByBatchId(batchId);
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
            System.err.println("Error reverting production quantities for bach " + batchId + ": " + e.getMessage());
        }
    }

    /**
     * Maps Mixer entity to MixerDetailDto with full product information
     */
    private BachDto.MixerDetailDto mapMixerToDetailDto(Mixer mixer) {
        BachDto.MixerDetailDto dto = new BachDto.MixerDetailDto();
        
        dto.setId(mixer.getId());
        dto.setProductId(mixer.getProduct() != null ? mixer.getProduct().getId() : null);
        dto.setProductName(mixer.getProduct() != null ? mixer.getProduct().getName() : null);
        // dto.setProductDescription(mixer.getProduct() != null ? mixer.getProduct().getDescription() : null);
        // dto.setProductMeasurement(mixer.getProduct() != null ? mixer.getProduct().getMeasurement() : null);
        // dto.setProductWeight(mixer.getProduct() != null ? mixer.getProduct().getWeight() : null);
        // dto.setProductPurchaseAmount(mixer.getProduct() != null ? mixer.getProduct().getPurchaseAmount() : null);
        // dto.setProductSaleAmount(mixer.getProduct() != null ? mixer.getProduct().getSaleAmount() : null);
        // dto.setProductRemainingQuantity(mixer.getProduct() != null ? mixer.getProduct().getRemainingQuantity() : null);
        // dto.setProductTaxPercentage(mixer.getProduct() != null ? mixer.getProduct().getTaxPercentage() : null);
        // dto.setProductStatus(mixer.getProduct() != null ? mixer.getProduct().getStatus() : null);
        dto.setQuantity(mixer.getQuantity());
        // dto.setCategoryName(mixer.getProduct() != null && mixer.getProduct().getCategory() != null ? 
        //     mixer.getProduct().getCategory().getName() : null);
        
        return dto;
    }

    /**
     * Maps Production entity to ProductionDetailDto with full product information
     */
    private BachDto.ProductionDetailDto mapProductionToDetailDto(Production production) {
        BachDto.ProductionDetailDto dto = new BachDto.ProductionDetailDto();
        
        dto.setId(production.getId());
        dto.setProductId(production.getProduct() != null ? production.getProduct().getId() : null);
        dto.setProductName(production.getProduct() != null ? production.getProduct().getName() : null);
        // dto.setProductDescription(production.getProduct() != null ? production.getProduct().getDescription() : null);
        // dto.setProductMeasurement(production.getProduct() != null ? production.getProduct().getMeasurement() : null);
        // dto.setProductWeight(production.getProduct() != null ? production.getProduct().getWeight() : null);
        // dto.setProductPurchaseAmount(production.getProduct() != null ? production.getProduct().getPurchaseAmount() : null);
        // dto.setProductSaleAmount(production.getProduct() != null ? production.getProduct().getSaleAmount() : null);
        // dto.setProductRemainingQuantity(production.getProduct() != null ? production.getProduct().getRemainingQuantity() : null);
        // dto.setProductTaxPercentage(production.getProduct() != null ? production.getProduct().getTaxPercentage() : null);
        // dto.setProductStatus(production.getProduct() != null ? production.getProduct().getStatus() : null);
        dto.setQuantity(production.getQuantity());
        dto.setNumberOfRoll(production.getNumberOfRoll());
        // dto.setCategoryName(production.getProduct() != null && production.getProduct().getCategory() != null ? 
        //     production.getProduct().getCategory().getName() : null);
        
        return dto;
    }
}


