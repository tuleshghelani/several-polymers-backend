package com.inventory.service;

import com.inventory.dao.MachineMasterDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.MachineDto;
import com.inventory.entity.MachineMaster;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.MachineMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MachineMasterService {
    private final MachineMasterRepository machineMasterRepository;
    private final MachineMasterDao machineMasterDao;
    private final UtilityService utilityService;

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> create(MachineDto dto) {
        validate(dto);
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Optional<MachineMaster> existing = machineMasterRepository.findByNameAndClient_Id(dto.getName().trim(), currentUser.getClient().getId());
            if (existing.isPresent()) {
                throw new ValidationException("Machine name already exists", HttpStatus.BAD_REQUEST);
            }
            MachineMaster m = new MachineMaster();
            m.setName(dto.getName().trim());
            m.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim() : "A");
            m.setCreatedBy(currentUser);
            m.setClient(currentUser.getClient());
            machineMasterRepository.save(m);
            return ApiResponse.success("Machine created successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to create machine", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> update(Long id, MachineDto dto) {
        validate(dto);
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            MachineMaster m = machineMasterRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Machine not found", HttpStatus.NOT_FOUND));
            if (!m.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            if (StringUtils.hasText(dto.getName())) {
                Optional<MachineMaster> existing = machineMasterRepository.findByNameAndClient_Id(dto.getName().trim(), currentUser.getClient().getId());
                if (existing.isPresent() && !existing.get().getId().equals(m.getId())) {
                    throw new ValidationException("Machine name already exists", HttpStatus.BAD_REQUEST);
                }
                m.setName(dto.getName().trim());
            }
            if (StringUtils.hasText(dto.getStatus())) {
                m.setStatus(dto.getStatus().trim());
            }
            m.setUpdatedAt(OffsetDateTime.now());
            m.setUpdatedBy(currentUser);
            machineMasterRepository.save(m);
            return ApiResponse.success("Machine updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update machine", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> delete(Long id) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            MachineMaster m = machineMasterRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Machine not found", HttpStatus.NOT_FOUND));
            if (!m.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            machineMasterRepository.delete(m);
            return ApiResponse.success("Machine deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to delete machine", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ApiResponse<Map<String, Object>> search(MachineDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            Map<String, Object> resp = machineMasterDao.search(dto);
            return ApiResponse.success("Machines fetched successfully", resp);
        } catch (Exception e) {
            throw new ValidationException("Failed to fetch machines", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ApiResponse<?> getDetails(Long id) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            MachineMaster m = machineMasterRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("Machine not found", HttpStatus.NOT_FOUND));
            if (!m.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("Unauthorized", HttpStatus.FORBIDDEN);
            }
            MachineDto dto = new MachineDto();
            dto.setId(m.getId());
            dto.setName(m.getName());
            dto.setStatus(m.getStatus());
            return ApiResponse.success("Machine details fetched", dto);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to fetch machine details", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ApiResponse<List<MachineDto>> getMachineList() {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            List<MachineMaster> machines = machineMasterRepository.findByClient_IdOrderByNameAsc(currentUser.getClient().getId());
            
            List<MachineDto> machineList = machines.stream()
                .map(machine -> {
                    MachineDto dto = new MachineDto();
                    dto.setId(machine.getId());
                    dto.setName(machine.getName());
                    dto.setStatus(machine.getStatus());
                    return dto;
                })
                .collect(Collectors.toList());
            
            return ApiResponse.success("Machine list fetched successfully", machineList);
        } catch (Exception e) {
            throw new ValidationException("Failed to fetch machine list", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validate(MachineDto dto) {
        if (dto == null || !StringUtils.hasText(dto.getName())) {
            throw new ValidationException("Machine name is required", HttpStatus.BAD_REQUEST);
        }
    }
}


