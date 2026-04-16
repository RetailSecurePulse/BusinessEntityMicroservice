package com.retailpulse.controller;

import com.retailpulse.controller.exception.ApplicationException;
import com.retailpulse.dto.request.BusinessEntityRequestDto;
import com.retailpulse.dto.response.BusinessEntityResponseDto;
import com.retailpulse.service.BusinessEntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessEntityControllerTest {

    @Mock
    private BusinessEntityService businessEntityService;

    private BusinessEntityController businessEntityController;

    @BeforeEach
    void setUp() {
        businessEntityController = new BusinessEntityController(businessEntityService);
    }

    @Test
    void getAllBusinessEntitiesReturnsOkResponseWithBody() {
        List<BusinessEntityResponseDto> dtos = List.of(
                new BusinessEntityResponseDto(1L, "HQ", "Yangon", "WAREHOUSE", false, true)
        );
        when(businessEntityService.getAllBusinessEntities()).thenReturn(dtos);

        ResponseEntity<List<BusinessEntityResponseDto>> response = businessEntityController.getAllBusinessEntities();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dtos);
    }

    @Test
    void getBusinessByIdReturnsOkResponseWithBody() {
        BusinessEntityResponseDto dto = new BusinessEntityResponseDto(2L, "Branch", "Mandalay", "STORE", true, true);
        when(businessEntityService.getBusinessEntityById(2L)).thenReturn(dto);

        ResponseEntity<BusinessEntityResponseDto> response = businessEntityController.getBusinessById(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void createBusinessEntityDelegatesToServiceForValidRequest() {
        BusinessEntityRequestDto request = new BusinessEntityRequestDto("Shop", "Yangon", "STORE", true);
        BusinessEntityResponseDto dto = new BusinessEntityResponseDto(3L, "Shop", "Yangon", "STORE", true, true);
        when(businessEntityService.saveBusinessEntity(request)).thenReturn(dto);

        ResponseEntity<BusinessEntityResponseDto> response = businessEntityController.createBusinessEntity(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void createBusinessEntityRejectsInvalidRequest() {
        BusinessEntityRequestDto request = new BusinessEntityRequestDto(" ", "Yangon", "STORE", null);

        assertThatThrownBy(() -> businessEntityController.createBusinessEntity(request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("Name, location, type, and external are required fields")
                .extracting("errorCode")
                .isEqualTo("INVALID_REQUEST");
    }

    @Test
    void updateBusinessEntityReturnsUpdatedDto() {
        BusinessEntityRequestDto request = new BusinessEntityRequestDto("Updated", "Yangon", "STORE", false);
        BusinessEntityResponseDto dto = new BusinessEntityResponseDto(4L, "Updated", "Yangon", "STORE", false, true);
        when(businessEntityService.updateBusinessEntity(4L, request)).thenReturn(dto);

        ResponseEntity<BusinessEntityResponseDto> response = businessEntityController.updateBusinessEntity(4L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void deleteBusinessEntityReturnsOkAndInvokesService() {
        ResponseEntity<Void> response = businessEntityController.deleteBusinessEntity(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();
        verify(businessEntityService).deleteBusinessEntity(5L);
    }
}
