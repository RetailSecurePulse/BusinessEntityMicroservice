package com.retailpulse.service;

import com.retailpulse.dto.request.BusinessEntityRequestDto;
import com.retailpulse.dto.response.BusinessEntityResponseDto;
import com.retailpulse.entity.BusinessEntity;
import com.retailpulse.repository.BusinessEntityRepository;
import com.retailpulse.service.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessEntityServiceTest {

    @Mock
    private BusinessEntityRepository businessEntityRepository;

    private BusinessEntityService businessEntityService;

    @BeforeEach
    void setUp() {
        businessEntityService = new BusinessEntityService(businessEntityRepository);
    }

    @Test
    void getAllBusinessEntitiesMapsRepositoryEntitiesToResponseDtos() {
        when(businessEntityRepository.findAll()).thenReturn(List.of(
                businessEntity(1L, "HQ", "Yangon", "WAREHOUSE", false, true),
                businessEntity(2L, "Branch", "Mandalay", "STORE", true, false)
        ));

        List<BusinessEntityResponseDto> results = businessEntityService.getAllBusinessEntities();

        assertThat(results).containsExactly(
                new BusinessEntityResponseDto(1L, "HQ", "Yangon", "WAREHOUSE", false, true),
                new BusinessEntityResponseDto(2L, "Branch", "Mandalay", "STORE", true, false)
        );
    }

    @Test
    void getBusinessEntityByIdReturnsMappedDtoWhenEntityExists() {
        when(businessEntityRepository.findById(5L))
                .thenReturn(Optional.of(businessEntity(5L, "HQ", "Yangon", "WAREHOUSE", true, true)));

        BusinessEntityResponseDto result = businessEntityService.getBusinessEntityById(5L);

        assertThat(result).isEqualTo(new BusinessEntityResponseDto(5L, "HQ", "Yangon", "WAREHOUSE", true, true));
    }

    @Test
    void getBusinessEntityByIdThrowsWhenEntityDoesNotExist() {
        when(businessEntityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> businessEntityService.getBusinessEntityById(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Business Entity not found with id: 99")
                .extracting("errorCode")
                .isEqualTo("BUSINESS_ENTITY_NOT_FOUND");
    }

    @Test
    void saveBusinessEntityPersistsNewEntityAndReturnsResponseDto() {
        BusinessEntityRequestDto request = new BusinessEntityRequestDto("New Shop", "Naypyidaw", "STORE", true);
        when(businessEntityRepository.save(any(BusinessEntity.class)))
                .thenReturn(businessEntity(7L, "New Shop", "Naypyidaw", "STORE", true, true));

        BusinessEntityResponseDto result = businessEntityService.saveBusinessEntity(request);

        ArgumentCaptor<BusinessEntity> entityCaptor = ArgumentCaptor.forClass(BusinessEntity.class);
        verify(businessEntityRepository).save(entityCaptor.capture());

        BusinessEntity savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getId()).isNull();
        assertThat(savedEntity.getName()).isEqualTo("New Shop");
        assertThat(savedEntity.getLocation()).isEqualTo("Naypyidaw");
        assertThat(savedEntity.getType()).isEqualTo("STORE");
        assertThat(savedEntity.isExternal()).isTrue();
        assertThat(savedEntity.isActive()).isTrue();
        assertThat(result).isEqualTo(new BusinessEntityResponseDto(7L, "New Shop", "Naypyidaw", "STORE", true, true));
    }

    @Test
    void updateBusinessEntityOnlyChangesProvidedNonBlankFields() {
        BusinessEntity existing = businessEntity(3L, "Old Name", "Old Location", "WAREHOUSE", true, true);
        when(businessEntityRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(businessEntityRepository.save(existing))
                .thenReturn(businessEntity(3L, "Old Name", "New Location", "WAREHOUSE", false, true));

        BusinessEntityResponseDto result = businessEntityService.updateBusinessEntity(
                3L,
                new BusinessEntityRequestDto("", "New Location", null, false)
        );

        assertThat(existing.getName()).isEqualTo("Old Name");
        assertThat(existing.getLocation()).isEqualTo("New Location");
        assertThat(existing.getType()).isEqualTo("WAREHOUSE");
        assertThat(existing.isExternal()).isFalse();
        assertThat(result).isEqualTo(new BusinessEntityResponseDto(3L, "Old Name", "New Location", "WAREHOUSE", false, true));
    }

    @Test
    void updateBusinessEntityThrowsWhenEntityDoesNotExist() {
        when(businessEntityRepository.findById(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> businessEntityService.updateBusinessEntity(
                4L,
                new BusinessEntityRequestDto("Name", "Location", "STORE", true)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Business Entity not found with id: 4")
                .extracting("errorCode")
                .isEqualTo("BUSINESS_ENTITY_NOT_FOUND");
    }

    @Test
    void updateBusinessEntityThrowsWhenEntityIsAlreadyDeleted() {
        when(businessEntityRepository.findById(8L))
                .thenReturn(Optional.of(businessEntity(8L, "Deleted", "Yangon", "STORE", false, false)));

        assertThatThrownBy(() -> businessEntityService.updateBusinessEntity(
                8L,
                new BusinessEntityRequestDto("Name", "Location", "STORE", true)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot update a deleted business entity with id: 8")
                .extracting("errorCode")
                .isEqualTo("BUSINESS_ENTITY_DELETED");
    }

    @Test
    void deleteBusinessEntityMarksEntityInactiveAndReturnsResponse() {
        BusinessEntity existing = businessEntity(6L, "Delete Me", "Yangon", "STORE", true, true);
        when(businessEntityRepository.findById(6L)).thenReturn(Optional.of(existing));
        when(businessEntityRepository.save(existing))
                .thenReturn(businessEntity(6L, "Delete Me", "Yangon", "STORE", true, false));

        BusinessEntityResponseDto result = businessEntityService.deleteBusinessEntity(6L);

        assertThat(existing.isActive()).isFalse();
        assertThat(result).isEqualTo(new BusinessEntityResponseDto(6L, "Delete Me", "Yangon", "STORE", true, false));
    }

    @Test
    void deleteBusinessEntityThrowsWhenEntityDoesNotExist() {
        when(businessEntityRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> businessEntityService.deleteBusinessEntity(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Business Entity not found with id: 10")
                .extracting("errorCode")
                .isEqualTo("BUSINESS_ENTITY_NOT_FOUND");
    }

    @Test
    void deleteBusinessEntityThrowsWhenAlreadyInactive() {
        when(businessEntityRepository.findById(11L))
                .thenReturn(Optional.of(businessEntity(11L, "Inactive", "Yangon", "STORE", false, false)));

        assertThatThrownBy(() -> businessEntityService.deleteBusinessEntity(11L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Business Entity with id 11 is already deleted.")
                .extracting("errorCode")
                .isEqualTo("BUSINESS_ENTITY_DELETED");
    }

    private static BusinessEntity businessEntity(
            Long id,
            String name,
            String location,
            String type,
            boolean external,
            boolean active
    ) {
        BusinessEntity businessEntity = new BusinessEntity(name, location, type, external);
        businessEntity.setId(id);
        businessEntity.setActive(active);
        return businessEntity;
    }
}
