package br.com.erpcore.adapter.web.auth;

import br.com.erpcore.adapter.web.auth.AuthDtos.LoginRequest;
import br.com.erpcore.adapter.web.auth.AuthDtos.RegisterRequest;
import br.com.erpcore.adapter.web.auth.AuthDtos.RegisterResponse;
import br.com.erpcore.adapter.web.auth.AuthDtos.SessionResponse;
import br.com.erpcore.adapter.web.correlation.CorrelationIds;
import br.com.erpcore.adapter.web.ratelimit.RateLimits;
import br.com.erpcore.application.identity.LoginCommand;
import br.com.erpcore.application.identity.LoginUseCase;
import br.com.erpcore.application.identity.LogoutCommand;
import br.com.erpcore.application.identity.LogoutUseCase;
import br.com.erpcore.application.identity.RefreshSessionCommand;
import br.com.erpcore.application.identity.RefreshSessionUseCase;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerCommand;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerResult;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerUseCase;
import br.com.erpcore.application.identity.SessionTokens;
import br.com.erpcore.domain.identity.InvalidRefreshTokenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Auth")
final class AuthController {

    private final RegisterCompanyAndOwnerUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshSessionUseCase refreshUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RateLimits rateLimits;
    private final RefreshCookies cookies;

    AuthController(RegisterCompanyAndOwnerUseCase registerUseCase, LoginUseCase loginUseCase,
                   RefreshSessionUseCase refreshUseCase, LogoutUseCase logoutUseCase, RateLimits rateLimits,
                   RefreshCookies cookies) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshUseCase = refreshUseCase;
        this.logoutUseCase = logoutUseCase;
        this.rateLimits = rateLimits;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastra uma empresa e o usuário dono")
    @ResponseStatus(HttpStatus.CREATED)
    RegisterResponse register(@Valid @RequestBody RegisterRequest body, HttpServletRequest request) {
        rateLimits.checkRegister(request.getRemoteAddr());
        RegisterCompanyAndOwnerResult result = registerUseCase.execute(new RegisterCompanyAndOwnerCommand(
                body.companyName(), body.ownerName(), body.email(), body.password(), CorrelationIds.current()));
        return new RegisterResponse(result.companyId(), result.userId());
    }

    @PostMapping("/login")
    @Operation(summary = "Entra com e-mail e senha: devolve o token de acesso e grava o refresh token em cookie")
    ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
                                          HttpServletResponse response) {
        rateLimits.checkLogin(request.getRemoteAddr(), body.email());
        SessionTokens tokens = loginUseCase.execute(
                new LoginCommand(body.email(), body.password(), CorrelationIds.current()));
        return session(tokens, response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova a sessão com o refresh token do cookie (exige o cabeçalho CSRF)")
    ResponseEntity<SessionResponse> refresh(
            @CookieValue(name = RefreshCookies.NAME, required = false) String refreshToken, HttpServletRequest request,
            HttpServletResponse response) {
        rateLimits.checkRefresh(request.getRemoteAddr());
        if (refreshToken == null) {
            throw new InvalidRefreshTokenException();
        }
        return session(refreshUseCase.execute(new RefreshSessionCommand(refreshToken, CorrelationIds.current())), response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerra a sessão e apaga o cookie (exige o cabeçalho CSRF)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@CookieValue(name = RefreshCookies.NAME, required = false) String refreshToken,
                HttpServletResponse response) {
        logoutUseCase.execute(new LogoutCommand(refreshToken, CorrelationIds.current()));
        response.addHeader(HttpHeaders.SET_COOKIE, cookies.clear().toString());
    }

    /**
     * O cookie é acrescentado direto à resposta do servlet: um cabeçalho Set-Cookie posto no ResponseEntity
     * substituiria o cookie XSRF-TOKEN que o filtro de CSRF já escreveu.
     */
    private ResponseEntity<SessionResponse> session(SessionTokens tokens, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookies.issue(tokens.refreshToken(), tokens.refreshTokenExpiresAt()).toString());
        return ResponseEntity.ok(new SessionResponse(tokens.accessToken(), tokens.accessTokenExpiresAt()));
    }
}
