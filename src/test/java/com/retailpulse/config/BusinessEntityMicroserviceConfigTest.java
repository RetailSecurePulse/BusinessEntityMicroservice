package com.retailpulse.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BusinessEntityMicroserviceConfigTest {

    private BusinessEntityMicroserviceConfig config;

    @BeforeEach
    void setUp() {
        config = new BusinessEntityMicroserviceConfig();
        ReflectionTestUtils.setField(config, "originURL", "http://localhost:3000");
        ReflectionTestUtils.setField(config, "keySetUri", "http://localhost:9000/oauth2/jwks");
    }

    @Test
    void corsConfigurationSourceUsesConfiguredOriginAndStandardHeaders() {
        CorsConfigurationSource source = ReflectionTestUtils.invokeMethod(config, "corsConfigurationSource");

        assert source != null;
        CorsConfiguration corsConfiguration = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/businessEntity"));

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.getAllowedOriginPatterns())
                .containsExactly("http://localhost:3000", "http://localhost", "http://localhost:*", "https://localhost", "https://localhost:*");
        assertThat(corsConfiguration.getAllowedMethods()).containsExactly("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS");
        assertThat(corsConfiguration.getAllowedHeaders()).containsExactly("Authorization", "Content-Type");
        assertThat(corsConfiguration.getExposedHeaders()).containsExactly("Authorization");
        assertThat(corsConfiguration.getAllowCredentials()).isTrue();
    }

    @Test
    void jwtGrantedAuthoritiesConverterReadsRolesClaimWithRolePrefix() {
        JwtGrantedAuthoritiesConverter converter =
                ReflectionTestUtils.invokeMethod(config, "jwtGrantedAuthoritiesConverter");

        assert converter != null;
        assertThat(converter.convert(jwtWithRoles()))
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_MANAGER");
    }

    @Test
    void jwtAuthenticationConverterUsesConfiguredAuthoritiesConverter() {
        JwtAuthenticationConverter converter =
                ReflectionTestUtils.invokeMethod(config, "jwtAuthenticationConverter");

        assert converter != null;
        AbstractAuthenticationToken authentication = converter.convert(jwtWithRoles());

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_ADMIN", "ROLE_MANAGER");
    }

    @Test
    void securityFilterChainEnablesOauthWhenAuthEnabled() {
        ReflectionTestUtils.setField(config, "authEnabled", true);
        DefaultSecurityFilterChain filterChain = mock(DefaultSecurityFilterChain.class);
        HttpSecurity http = stubHttpSecurity(filterChain);

        SecurityFilterChain result = config.securityFilterChain(http);

        assertThat(result).isSameAs(filterChain);
        verify(http).oauth2ResourceServer(anyOauth2Customizer());
        verify(http).authorizeHttpRequests(anyAuthorizeCustomizer());
        verify(http).cors(anyCorsCustomizer());
        verify(http, never()).csrf(anyCsrfCustomizer());
        verify(http).build();
    }

    @Test
    void securityFilterChainDisablesCsrfWhenAuthIsDisabled() {
        ReflectionTestUtils.setField(config, "authEnabled", false);
        DefaultSecurityFilterChain filterChain = mock(DefaultSecurityFilterChain.class);
        HttpSecurity http = stubHttpSecurity(filterChain);

        SecurityFilterChain result = config.securityFilterChain(http);

        assertThat(result).isSameAs(filterChain);
        verify(http, never()).oauth2ResourceServer(anyOauth2Customizer());
        verify(http).authorizeHttpRequests(anyAuthorizeCustomizer());
        verify(http).csrf(anyCsrfCustomizer());
        verify(http).cors(anyCorsCustomizer());
        verify(http).build();
    }

    private static Jwt jwtWithRoles() {
        return new Jwt(
                "jwt-token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("roles", java.util.List.of("ADMIN", "MANAGER"))
        );
    }

    private static HttpSecurity stubHttpSecurity(DefaultSecurityFilterChain filterChain) {
        HttpSecurity http = mock(HttpSecurity.class);
        doReturn(http).when(http).oauth2ResourceServer(anyOauth2Customizer());
        doReturn(http).when(http).authorizeHttpRequests(anyAuthorizeCustomizer());
        doReturn(http).when(http).csrf(anyCsrfCustomizer());
        doReturn(http).when(http).cors(anyCorsCustomizer());
        when(http.build()).thenReturn(filterChain);
        return http;
    }

    private static Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>> anyOauth2Customizer() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>
    anyAuthorizeCustomizer() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static Customizer<CorsConfigurer<HttpSecurity>> anyCorsCustomizer() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static Customizer<CsrfConfigurer<HttpSecurity>> anyCsrfCustomizer() {
        return org.mockito.ArgumentMatchers.any();
    }
}
