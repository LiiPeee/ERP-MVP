package br.com.erpcore.adapter.web.error;

import br.com.erpcore.adapter.web.auth.RefreshCookies;
import br.com.erpcore.adapter.web.correlation.CorrelationIds;
import br.com.erpcore.adapter.web.ratelimit.TooManyRequestsException;
import br.com.erpcore.domain.identity.AccessDeniedException;
import br.com.erpcore.domain.identity.EmailAlreadyRegisteredException;
import br.com.erpcore.domain.identity.InvalidCredentialsException;
import br.com.erpcore.domain.identity.InvalidRefreshTokenException;
import br.com.erpcore.domain.shared.DomainValidationException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Único lugar que traduz exceções para HTTP. Toda resposta de erro é um problema padronizado (RFC 9457) com um
 * código estável e o correlation_id, sem stack trace, nome de classe nem texto interno.
 */
@RestControllerAdvice
final class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final RefreshCookies cookies;

    ApiExceptionHandler(RefreshCookies cookies) {
        this.cookies = cookies;
    }

    @ExceptionHandler(DomainValidationException.class)
    ResponseEntity<ProblemDetail> invalidField(DomainValidationException error) {
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_CONTENT.value(), "Dado inválido", error.code());
        problem.setProperty("field", error.field());
        return ResponseEntity.unprocessableContent().body(problem);
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ResponseEntity<ProblemDetail> emailTaken() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(problem(409, "E-mail já cadastrado", "EMAIL_ALREADY_REGISTERED"));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ProblemDetail> invalidCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(problem(401, "Credenciais inválidas", "INVALID_CREDENTIALS"));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<ProblemDetail> invalidRefreshToken(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookies.clear().toString());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(problem(401, "Sessão inválida ou expirada", "INVALID_REFRESH_TOKEN"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> accessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem(403, "Acesso negado", "ACCESS_DENIED"));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    ResponseEntity<ProblemDetail> tooManyRequests(TooManyRequestsException error) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(error.retryAfterSeconds()))
                .body(problem(429, "Muitas tentativas. Aguarde um pouco.", "TOO_MANY_REQUESTS"));
    }

    /** Erros de autenticação e de autorização do Spring Security seguem para o tratamento da própria cadeia. */
    @ExceptionHandler({AuthenticationException.class, org.springframework.security.access.AccessDeniedException.class})
    void rethrowSecurityErrors(RuntimeException error) {
        throw error;
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception error) {
        LOG.error("Unhandled error", error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problem(500, "Erro interno", "INTERNAL_ERROR"));
    }

    /** Erros do próprio Spring MVC (JSON inválido, método errado, rota inexistente) saem no mesmo formato. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception error, Object body, HttpHeaders headers,
                                                             HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = switch (status.value()) {
            case 400 -> problem(400, "Requisição inválida", "INVALID_REQUEST");
            case 404 -> problem(404, "Não encontrado", "NOT_FOUND");
            case 405 -> problem(405, "Método não permitido", "METHOD_NOT_ALLOWED");
            case 406 -> problem(406, "Formato não aceito", "NOT_ACCEPTABLE");
            case 415 -> problem(415, "Tipo de conteúdo não suportado", "UNSUPPORTED_MEDIA_TYPE");
            default -> problem(status.value(), "Erro", "ERROR");
        };
        return ResponseEntity.status(status).headers(headers).body(problem);
    }

    private static ProblemDetail problem(int status, String title, String code) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setProperty("code", code);
        problem.setProperty("correlationId", CorrelationIds.current());
        return problem;
    }
}
