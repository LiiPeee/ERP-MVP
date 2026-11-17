package br.com.erpcore.adapter.web.quote;

import br.com.erpcore.adapter.web.correlation.CorrelationIds;
import br.com.erpcore.adapter.web.quote.QuoteDtos.RejectRequest;
import br.com.erpcore.adapter.web.ratelimit.RateLimits;
import br.com.erpcore.application.quote.ApproveQuoteByLinkUseCase;
import br.com.erpcore.application.quote.DecideQuoteByLinkCommand;
import br.com.erpcore.application.quote.GetPublicQuoteQuery;
import br.com.erpcore.application.quote.GetPublicQuoteUseCase;
import br.com.erpcore.application.quote.PublicQuoteDecision;
import br.com.erpcore.application.quote.PublicQuoteView;
import br.com.erpcore.application.quote.RejectQuoteByLinkUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * O que o cliente faz com o link do orçamento, sem login: o token no caminho é a credencial. Ler (GET) nunca
 * consome nada, porque o WhatsApp e buscadores abrem links para fazer pré-visualização; só a decisão (POST) vale.
 * Os limites rodam antes de qualquer consulta ao banco.
 */
@RestController
@RequestMapping(path = "/api/public/quotes/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Link público do orçamento")
final class PublicQuoteController {

    private final GetPublicQuoteUseCase getQuote;
    private final ApproveQuoteByLinkUseCase approve;
    private final RejectQuoteByLinkUseCase reject;
    private final RateLimits rateLimits;

    PublicQuoteController(GetPublicQuoteUseCase getQuote, ApproveQuoteByLinkUseCase approve,
                          RejectQuoteByLinkUseCase reject, RateLimits rateLimits) {
        this.getQuote = getQuote;
        this.approve = approve;
        this.reject = reject;
        this.rateLimits = rateLimits;
    }

    @GetMapping
    @Operation(summary = "O orçamento como o cliente vê: só o necessário para decidir")
    PublicQuoteView view(@PathVariable String token, HttpServletRequest request) {
        rateLimits.checkPublicLink(request.getRemoteAddr(), token);
        return getQuote.execute(new GetPublicQuoteQuery(token));
    }

    @PostMapping("/approve")
    @Operation(summary = "O cliente aprova. Repetir não muda nada; depois de recusar, é recusado")
    PublicQuoteDecision approve(@PathVariable String token, HttpServletRequest request) {
        rateLimits.checkPublicLink(request.getRemoteAddr(), token);
        return approve.execute(new DecideQuoteByLinkCommand(token, null, CorrelationIds.current()));
    }

    @PostMapping(path = "/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "O cliente recusa, com motivo opcional. Repetir não muda nada; depois de aprovar, é recusado")
    PublicQuoteDecision reject(@PathVariable String token, @Valid @RequestBody(required = false) RejectRequest body,
                               HttpServletRequest request) {
        rateLimits.checkPublicLink(request.getRemoteAddr(), token);
        return reject.execute(new DecideQuoteByLinkCommand(token, body == null ? null : body.reason(),
                CorrelationIds.current()));
    }
}
