package com.retailpulse.aspect;

import com.retailpulse.annotation.AuditLog;
import com.retailpulse.entity.AuditLogEntity;
import com.retailpulse.repository.AuditLogRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    private AuditAspect auditAspect;

    @BeforeEach
    void setUp() {
        auditAspect = new AuditAspect(auditLogRepository);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("alice", "pw"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void auditSavesSuccessLogAndReturnsJoinPointResult() throws Throwable {
        AuditLog auditLog = auditLogAnnotation("auditedSuccess");
        when(proceedingJoinPoint.proceed()).thenReturn("ok");

        Object result = auditAspect.audit(proceedingJoinPoint, auditLog);

        assertThat(result).isEqualTo("ok");

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity savedLog = captor.getValue();
        assertThat(savedLog.getActor()).isEqualTo("alice");
        assertThat(savedLog.getAction()).isEqualTo("CREATE_BUSINESS_ENTITY");
        assertThat(savedLog.getStatus()).isEqualTo("SUCCESS");
        assertThat(savedLog.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(savedLog.getTimestamp()).isNotNull();
    }

    @Test
    void auditSavesFailureLogAndRethrowsException() throws Throwable {
        AuditLog auditLog = auditLogAnnotation("auditedFailure");
        RuntimeException failure = new RuntimeException("boom");
        when(proceedingJoinPoint.proceed()).thenThrow(failure);

        assertThatThrownBy(() -> auditAspect.audit(proceedingJoinPoint, auditLog))
                .isSameAs(failure);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntity savedLog = captor.getValue();
        assertThat(savedLog.getActor()).isEqualTo("alice");
        assertThat(savedLog.getAction()).isEqualTo("DELETE_BUSINESS_ENTITY");
        assertThat(savedLog.getStatus()).isEqualTo("FAILURE: boom");
        assertThat(savedLog.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(savedLog.getTimestamp()).isNotNull();
    }

    private static AuditLog auditLogAnnotation(String methodName) {
        try {
            Method method = AuditAspectFixture.class.getDeclaredMethod(methodName);
            return method.getAnnotation(AuditLog.class);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static class AuditAspectFixture {
        @AuditLog(action = "CREATE_BUSINESS_ENTITY")
        void auditedSuccess() {
        }

        @AuditLog(action = "DELETE_BUSINESS_ENTITY")
        void auditedFailure() {
        }
    }
}
