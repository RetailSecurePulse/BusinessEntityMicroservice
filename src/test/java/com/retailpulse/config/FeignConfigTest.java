package com.retailpulse.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeignConfigTest {

    @Mock
    private Tracer tracer;

    private FeignConfig feignConfig;

    @BeforeEach
    void setUp() {
        feignConfig = new FeignConfig(tracer);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void feignLoggerLevelReturnsFull() {
        assertThat(feignConfig.feignLoggerLevel()).isEqualTo(Logger.Level.FULL);
    }

    @Test
    void interceptorAddsTracingAndJwtAuthenticationTokenHeaders() {
        mockTraceContext("trace-123", "span-456");
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt("jwt-token")));
        RequestTemplate template = requestTemplate();

        RequestInterceptor interceptor = feignConfig.oauth2BearerForwardingInterceptor();
        interceptor.apply(template);

        assertThat(headerValues(template, "X-B3-TraceId")).containsExactly("trace-123");
        assertThat(headerValues(template, "X-B3-SpanId")).containsExactly("span-456");
        assertThat(headerValues(template, HttpHeaders.AUTHORIZATION)).containsExactly("Bearer jwt-token");
    }

    @Test
    void interceptorUsesBearerTokenAuthenticationWhenAvailable() {
        when(tracer.currentSpan()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(bearerAuthentication("bearer-token"));
        RequestTemplate template = requestTemplate();

        feignConfig.oauth2BearerForwardingInterceptor().apply(template);

        assertThat(headerValues(template, HttpHeaders.AUTHORIZATION)).containsExactly("Bearer bearer-token");
    }

    @Test
    void interceptorFallsBackToServletAuthorizationHeader() {
        when(tracer.currentSpan()).thenReturn(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer forwarded-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        RequestTemplate template = requestTemplate();

        feignConfig.oauth2BearerForwardingInterceptor().apply(template);

        assertThat(headerValues(template, HttpHeaders.AUTHORIZATION)).containsExactly("Bearer forwarded-token");
    }

    @Test
    void interceptorLeavesAuthorizationHeaderUnsetWhenNoTokenExists() {
        when(tracer.currentSpan()).thenReturn(null);
        RequestTemplate template = requestTemplate();

        feignConfig.oauth2BearerForwardingInterceptor().apply(template);

        assertThat(template.headers()).doesNotContainKey(HttpHeaders.AUTHORIZATION);
    }

    private void mockTraceContext(String traceId, String spanId) {
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(traceId);
        when(traceContext.spanId()).thenReturn(spanId);
    }

    private static Jwt jwt(String tokenValue) {
        return new Jwt(
                tokenValue,
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("sub", "user-1")
        );
    }

    private static BearerTokenAuthentication bearerAuthentication(String tokenValue) {
        DefaultOAuth2AuthenticatedPrincipal principal =
                new DefaultOAuth2AuthenticatedPrincipal(Map.of("sub", "user-1"), AuthorityUtils.NO_AUTHORITIES);
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenValue,
                Instant.now(),
                Instant.now().plusSeconds(300)
        );
        return new BearerTokenAuthentication(principal, token, AuthorityUtils.NO_AUTHORITIES);
    }

    private static RequestTemplate requestTemplate() {
        RequestTemplate template = new RequestTemplate();
        template.method("GET");
        template.uri("/inventory");
        return template;
    }

    private static Collection<String> headerValues(RequestTemplate template, String headerName) {
        return template.headers().get(headerName);
    }
}
