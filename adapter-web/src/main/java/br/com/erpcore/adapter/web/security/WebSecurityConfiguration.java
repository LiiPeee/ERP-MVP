package br.com.erpcore.adapter.web.security;

import br.com.erpcore.adapter.web.WebProperties;
import br.com.erpcore.adapter.web.error.ProblemJson;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.CrossOriginOpenerPolicyHeaderWriter.CrossOriginOpenerPolicy;
import org.springframework.security.web.header.writers.CrossOriginResourcePolicyHeaderWriter.CrossOriginResourcePolicy;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * A cadeia de segurança da API: sem sessão, token de acesso (JWT) em todas as rotas que não estejam na lista de
 * públicas, CSRF só onde o cookie é a credencial (refresh e logout), CORS fechado, cabeçalhos restritivos e
 * negação de tudo que não foi previsto.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
class WebSecurityConfiguration {

    /** Sem nenhum recurso de terceiros: tudo vem da própria origem. */
    static final String CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self'; style-src 'self'; "
            + "img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'self'; "
            + "frame-ancestors 'none'; form-action 'self'";

    private static final String PERMISSIONS_POLICY = "camera=(), microphone=(), geolocation=(), payment=()";
    private static final long HSTS_SECONDS = 31_536_000;

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, JwtDecoder jwtDecoder, WebProperties properties,
                                    @Value("${springdoc.api-docs.enabled:false}") boolean apiDocsEnabled) throws Exception {
        AuthenticationEntryPoint unauthenticated = (request, response, error) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            ProblemJson.write(response, 401, "UNAUTHENTICATED", "Não autenticado");
        };
        AccessDeniedHandler denied = (request, response, error) ->
                ProblemJson.write(response, 403, "ACCESS_DENIED", "Acesso negado");
        RequestMatcher cookieAuthenticated = new OrRequestMatcher(
                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/auth/refresh"),
                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/auth/logout"));

        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfCookie(properties))
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .requireCsrfProtectionMatcher(cookieAuthenticated))
                .authorizeHttpRequests(requests -> {
                    PublicRoutes.ALL.forEach(route -> requests.requestMatchers(route.method(), route.pattern()).permitAll());
                    if (apiDocsEnabled) {
                        // O contrato OpenAPI só é público quando alguém o liga de propósito (testes e geração do cliente).
                        requests.requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**").permitAll();
                    }
                    requests.requestMatchers("/api/**").authenticated();
                    requests.anyRequest().denyAll();
                })
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        .authenticationEntryPoint(unauthenticated)
                        .accessDeniedHandler(denied))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthenticated).accessDeniedHandler(denied))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(HSTS_SECONDS))
                        .permissionsPolicyHeader(permissions -> permissions.policy(PERMISSIONS_POLICY))
                        .crossOriginOpenerPolicy(policy -> policy.policy(CrossOriginOpenerPolicy.SAME_ORIGIN))
                        .crossOriginResourcePolicy(policy -> policy.policy(CrossOriginResourcePolicy.SAME_ORIGIN)))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .addFilterAfter(new TenantLogContextFilter(), BearerTokenAuthenticationFilter.class);

        if (properties.allowedOrigins().isEmpty()) {
            http.cors(AbstractHttpConfigurer::disable);
        } else {
            http.cors(cors -> cors.configurationSource(corsSource(properties.allowedOrigins())));
        }
        return http.build();
    }

    /** O cookie XSRF-TOKEN: o JavaScript do próprio site o lê e o devolve no cabeçalho X-XSRF-TOKEN. */
    private static CookieCsrfTokenRepository csrfCookie(WebProperties properties) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/api/auth");
        repository.setCookieCustomizer(cookie -> cookie.secure(properties.cookieSecure()).sameSite("Strict"));
        return repository;
    }

    private static CorsConfigurationSource corsSource(List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Correlation-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
