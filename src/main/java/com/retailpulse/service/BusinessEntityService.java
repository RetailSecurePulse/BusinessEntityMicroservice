package com.retailpulse.service;

import com.retailpulse.annotation.AuditLog;
import com.retailpulse.dto.request.BusinessEntityRequestDto;
import com.retailpulse.dto.response.BusinessEntityResponseDto;
import com.retailpulse.entity.BusinessEntity;
import com.retailpulse.repository.BusinessEntityRepository;
import com.retailpulse.service.exception.BusinessException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
@CacheConfig(cacheNames = {"businessEntity", "businessEntityList"})
public class BusinessEntityService {
    private static final String BUSINESS_ENTITY_NOT_FOUND = "BUSINESS_ENTITY_NOT_FOUND";
    private static final String BUSINESS_ENTITY_DELETED = "BUSINESS_ENTITY_DELETED";
    private static final String HAS_PRODUCT_IN_INVENTORY = "HAS_PRODUCT_IN_INVENTORY";
    private static final String BUSINESS_ENTITY_NOT_FOUND_DESC = "Business Entity not found with id: ";

    private final BusinessEntityRepository businessEntityRepository;
//    private final InventoryRepository inventoryRepository;

    @Autowired
    public BusinessEntityService(BusinessEntityRepository businessEntityRepository) {
        this.businessEntityRepository = businessEntityRepository;
//        this.inventoryRepository = inventoryRepository;
    }

    @Cacheable(value = "businessEntityList")
    public List<BusinessEntityResponseDto> getAllBusinessEntities() {
        return businessEntityRepository.findAll().stream()
                .map(businessEntity -> new BusinessEntityResponseDto(
                        businessEntity.getId(),
                        businessEntity.getName(),
                        businessEntity.getLocation(),
                        businessEntity.getType(),
                        businessEntity.isExternal(),
                        businessEntity.isActive()
                ))
                .toList();
    }

    @Cacheable(value = "businessEntity", key = "#id")
    public BusinessEntityResponseDto getBusinessEntityById(Long id) {
        BusinessEntity businessEntity = businessEntityRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BUSINESS_ENTITY_NOT_FOUND, BUSINESS_ENTITY_NOT_FOUND_DESC + id));

        return new BusinessEntityResponseDto(businessEntity.getId(),
                businessEntity.getName(),
                businessEntity.getLocation(),
                businessEntity.getType(),
                businessEntity.isExternal(),
                businessEntity.isActive());
    }

    @AuditLog(action = "CREATE_BUSINESS_ENTITY")
    @Caching(
        put = { @CachePut(value = "businessEntity", key = "#result.id") },
        evict = { @CacheEvict(value = "businessEntityList", allEntries = true) }
    )
    public BusinessEntityResponseDto saveBusinessEntity(BusinessEntityRequestDto request) {
        BusinessEntity businessEntity = new BusinessEntity(request.name(), request.location(), request.type(), request.external());
        BusinessEntity savedBusinessEntity = businessEntityRepository.save(businessEntity);
        return new BusinessEntityResponseDto(
                savedBusinessEntity.getId(),
                savedBusinessEntity.getName(),
                savedBusinessEntity.getLocation(),
                savedBusinessEntity.getType(),
                savedBusinessEntity.isExternal(),
                savedBusinessEntity.isActive()
        );
    }

    @AuditLog(action = "UPDATE_BUSINESS_ENTITY")
    @Caching(
        put = { @CachePut(value = "businessEntity", key = "#id") },
        evict = { @CacheEvict(value = "businessEntityList", allEntries = true) }
    )
    public BusinessEntityResponseDto updateBusinessEntity(Long id, BusinessEntityRequestDto businessEntityDetails) {
        BusinessEntity businessEntity = businessEntityRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BUSINESS_ENTITY_NOT_FOUND, BUSINESS_ENTITY_NOT_FOUND_DESC + id));

        if (!businessEntity.isActive()) {
            throw new BusinessException(BUSINESS_ENTITY_DELETED, "Cannot update a deleted business entity with id: " + id);
        }

        // Update fields from the incoming details if provided
        updateField(businessEntityDetails.name(), businessEntity::setName);
        updateField(businessEntityDetails.location(), businessEntity::setLocation);
        updateField(businessEntityDetails.type(), businessEntity::setType);
        updateField(businessEntityDetails.external(), businessEntity::setExternal);

        BusinessEntity updatedBusinessEntity = businessEntityRepository.save(businessEntity);

        return new BusinessEntityResponseDto(
                updatedBusinessEntity.getId(),
                updatedBusinessEntity.getName(),
                updatedBusinessEntity.getLocation(),
                updatedBusinessEntity.getType(),
                updatedBusinessEntity.isExternal(),
                updatedBusinessEntity.isActive());
    }

    // Generic helper method for updating fields
    private <T> void updateField(T newValue, Consumer<T> updater) {
        if(newValue == null) {
            return;
        }
        if (newValue instanceof String && ((String) newValue).isEmpty()) {
            return;
        }
        updater.accept(newValue);
    }

    @AuditLog(action = "DELETE_BUSINESS_ENTITY")
    @Caching(
        evict = {
            @CacheEvict(value = "businessEntity", key = "#id"),
            @CacheEvict(value = "businessEntityList", allEntries = true)
        }
    )
    public BusinessEntityResponseDto deleteBusinessEntity(Long id) {
        BusinessEntity businessEntity = businessEntityRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BUSINESS_ENTITY_NOT_FOUND, BUSINESS_ENTITY_NOT_FOUND_DESC + id));

        if (!businessEntity.isActive()) {
            throw new BusinessException(BUSINESS_ENTITY_DELETED, "Business Entity with id " + id + " is already deleted.");
        }

        // Check if Inventory has products; If yes, cannot delete
        if (hasProductsInInventory(businessEntity.getId())) {
            throw new BusinessException(HAS_PRODUCT_IN_INVENTORY, "Cannot delete Business Entity with id " + id + " as it has associated products in the inventory.");
        }

        businessEntity.setActive(false);

        BusinessEntity updatedBusinessEntity = businessEntityRepository.save(businessEntity);

        return new BusinessEntityResponseDto(
                updatedBusinessEntity.getId(),
                updatedBusinessEntity.getName(),
                updatedBusinessEntity.getLocation(),
                updatedBusinessEntity.getType(),
                updatedBusinessEntity.isExternal(),
                updatedBusinessEntity.isActive());
    }

    // todo - Finish this after inventory microservice is setup
    private boolean hasProductsInInventory(@NotNull Long id) {
        // Call InventoryService to check
//        List<Inventory> inventories = inventoryRepository.findByBusinessEntityId(id);
//        return inventories.stream().anyMatch(inventory -> inventory.getQuantity() > 0);

        return false;
    }

}
