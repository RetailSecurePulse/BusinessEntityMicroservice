package com.retailpulse;

import com.retailpulse.annotation.AuditLog;
import com.retailpulse.controller.ErrorResponse;
import com.retailpulse.controller.exception.ApplicationException;
import com.retailpulse.dto.BusinessEntityDto;
import com.retailpulse.dto.request.BusinessEntityRequestDto;
import com.retailpulse.dto.response.BusinessEntityResponseDto;
import com.retailpulse.entity.AuditLogEntity;
import com.retailpulse.entity.BusinessEntity;
import com.retailpulse.service.BusinessEntityService;
import com.retailpulse.service.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ModelAndExceptionSmokeTest {

    @Test
    void recordDtosExposeTheirValues() {
        BusinessEntityDto dto = new BusinessEntityDto("HQ", "Bishan", "WAREHOUSE");
        BusinessEntityRequestDto requestDto = new BusinessEntityRequestDto("Branch", "Yishun", "STORE", true);
        BusinessEntityResponseDto responseDto = new BusinessEntityResponseDto(1L, "Branch", "Yishun", "STORE", true, true);

        assertThat(dto.name()).isEqualTo("HQ");
        assertThat(dto.location()).isEqualTo("Bishan");
        assertThat(dto.type()).isEqualTo("WAREHOUSE");
        assertThat(requestDto.name()).isEqualTo("Branch");
        assertThat(requestDto.location()).isEqualTo("Yishun");
        assertThat(requestDto.type()).isEqualTo("STORE");
        assertThat(requestDto.external()).isTrue();
        assertThat(responseDto.id()).isEqualTo(1L);
        assertThat(responseDto.active()).isTrue();
    }

    @Test
    void errorResponseSupportsConstructionAndMutation() {
        ErrorResponse errorResponse = new ErrorResponse("ERR", "original");
        errorResponse.setCode("UPDATED");
        errorResponse.setMessage("changed");

        assertThat(errorResponse.getCode()).isEqualTo("UPDATED");
        assertThat(errorResponse.getMessage()).isEqualTo("changed");
    }

    @Test
    void applicationExceptionRetainsErrorCodeAndMessage() {
        ApplicationException exception = new ApplicationException("INVALID_REQUEST", "Missing field");

        assertThat(exception.getErrorCode()).isEqualTo("INVALID_REQUEST");
        assertThat(exception.getMessage()).isEqualTo("Missing field");
    }

    @Test
    void businessExceptionRetainsErrorCodeAndMessage() {
        BusinessException exception = new BusinessException("BUSINESS_ENTITY_NOT_FOUND", "Not found");

        assertThat(exception.getErrorCode()).isEqualTo("BUSINESS_ENTITY_NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo("Not found");
    }

    @Test
    void businessEntityNoArgsConstructorStartsWithExpectedDefaults() {
        BusinessEntity businessEntity = new BusinessEntity();

        assertThat(businessEntity.isActive()).isTrue();
        assertThat(businessEntity.isExternal()).isFalse();

        businessEntity.setId(10L);
        businessEntity.setName("HQ");
        businessEntity.setLocation("Bishan");
        businessEntity.setType("WAREHOUSE");
        businessEntity.setExternal(true);

        assertThat(businessEntity.getId()).isEqualTo(10L);
        assertThat(businessEntity.getName()).isEqualTo("HQ");
        assertThat(businessEntity.getLocation()).isEqualTo("Bishan");
        assertThat(businessEntity.getType()).isEqualTo("WAREHOUSE");
        assertThat(businessEntity.isExternal()).isTrue();
    }

    @Test
    void businessEntityAllArgsConstructorSetsSuppliedFields() {
        BusinessEntity businessEntity = new BusinessEntity("Branch", "AngMoKio", "STORE", true);

        assertThat(businessEntity.getName()).isEqualTo("Branch");
        assertThat(businessEntity.getLocation()).isEqualTo("AngMoKio");
        assertThat(businessEntity.getType()).isEqualTo("STORE");
        assertThat(businessEntity.isExternal()).isTrue();
        assertThat(businessEntity.isActive()).isTrue();
    }

    @Test
    void auditLogEntityConstructorSetsSuppliedFields() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 4, 16, 20, 45);
        AuditLogEntity auditLogEntity = new AuditLogEntity("alice", "CREATE_BUSINESS_ENTITY", "SUCCESS", "127.0.0.1", timestamp);

        assertThat(auditLogEntity.getActor()).isEqualTo("alice");
        assertThat(auditLogEntity.getAction()).isEqualTo("CREATE_BUSINESS_ENTITY");
        assertThat(auditLogEntity.getStatus()).isEqualTo("SUCCESS");
        assertThat(auditLogEntity.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(auditLogEntity.getTimestamp()).isEqualTo(timestamp);
        assertThat(auditLogEntity.getDetails()).isNull();
    }

    @Test
    void businessEntityServiceMutatingMethodsKeepAuditLogAnnotations() throws Exception {
        assertAuditAction("saveBusinessEntity", "CREATE_BUSINESS_ENTITY", BusinessEntityRequestDto.class);
        assertAuditAction("updateBusinessEntity", "UPDATE_BUSINESS_ENTITY", Long.class, BusinessEntityRequestDto.class);
        assertAuditAction("deleteBusinessEntity", "DELETE_BUSINESS_ENTITY", Long.class);
    }

    private static void assertAuditAction(String methodName, String action, Class<?>... parameterTypes) throws Exception {
        Method method = BusinessEntityService.class.getDeclaredMethod(methodName, parameterTypes);
        AuditLog auditLog = method.getAnnotation(AuditLog.class);

        assertThat(auditLog).isNotNull();
        assertThat(auditLog.action()).isEqualTo(action);
    }
}
