package br.com.erpcore.adapter.web.quote;

import br.com.erpcore.adapter.web.correlation.CorrelationIds;
import br.com.erpcore.adapter.web.quote.QuoteDtos.CreateQuoteRequest;
import br.com.erpcore.adapter.web.quote.QuoteDtos.ReviseQuoteItemsRequest;
import br.com.erpcore.adapter.web.security.Principals;
import br.com.erpcore.application.quote.CancelQuoteCommand;
import br.com.erpcore.application.quote.CancelQuoteUseCase;
import br.com.erpcore.application.quote.ConfirmQuoteCommand;
import br.com.erpcore.application.quote.ConfirmQuoteUseCase;
import br.com.erpcore.application.quote.CreateQuoteDraftCommand;
import br.com.erpcore.application.quote.CreateQuoteDraftUseCase;
import br.com.erpcore.application.quote.GetQuoteQuery;
import br.com.erpcore.application.quote.GetQuoteUseCase;
import br.com.erpcore.application.quote.ListQuotesQuery;
import br.com.erpcore.application.quote.ListQuotesUseCase;
import br.com.erpcore.application.quote.QuoteQueue;
import br.com.erpcore.application.quote.QuoteSentResult;
import br.com.erpcore.application.quote.QuoteView;
import br.com.erpcore.application.quote.ReviseQuoteItemsCommand;
import br.com.erpcore.application.quote.ReviseQuoteItemsUseCase;
import br.com.erpcore.application.quote.SendQuoteCommand;
import br.com.erpcore.application.quote.SendQuoteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Os orçamentos do ponto de vista do dono da oficina. A empresa e o usuário vêm só do token. */
@RestController
@RequestMapping(path = "/api/quotes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Quotes")
@SecurityRequirement(name = "bearerAuth")
final class QuoteController {

    private final CreateQuoteDraftUseCase createDraft;
    private final ReviseQuoteItemsUseCase reviseItems;
    private final ConfirmQuoteUseCase confirm;
    private final SendQuoteUseCase resend;
    private final CancelQuoteUseCase cancel;
    private final GetQuoteUseCase get;
    private final ListQuotesUseCase list;

    QuoteController(CreateQuoteDraftUseCase createDraft, ReviseQuoteItemsUseCase reviseItems,
                    ConfirmQuoteUseCase confirm, SendQuoteUseCase resend, CancelQuoteUseCase cancel, GetQuoteUseCase get,
                    ListQuotesUseCase list) {
        this.createDraft = createDraft;
        this.reviseItems = reviseItems;
        this.confirm = confirm;
        this.resend = resend;
        this.cancel = cancel;
        this.get = get;
        this.list = list;
    }

    @GetMapping
    @Operation(summary = "A fila: o que precisa do dono, o que espera o cliente e o que já fechou")
    QuoteQueue queue(@AuthenticationPrincipal Jwt token) {
        return list.execute(new ListQuotesQuery(Principals.of(token)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um rascunho de orçamento; o total é calculado pelo servidor")
    QuoteView createDraft(@AuthenticationPrincipal Jwt token, @Valid @RequestBody CreateQuoteRequest body) {
        return createDraft.execute(new CreateQuoteDraftCommand(Principals.of(token), body.contactId(), body.vehicleId(),
                body.inputs(), body.note(), body.validityDays(), CorrelationIds.current()));
    }

    @GetMapping("/{quoteId}")
    @Operation(summary = "Um orçamento (telefone do cliente mascarado)")
    QuoteView get(@AuthenticationPrincipal Jwt token, @PathVariable UUID quoteId) {
        return get.execute(new GetQuoteQuery(Principals.of(token), quoteId));
    }

    @PutMapping(path = "/{quoteId}/items", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Troca todos os itens de um rascunho; a lista nova substitui a antiga")
    QuoteView reviseItems(@AuthenticationPrincipal Jwt token, @PathVariable UUID quoteId,
                          @Valid @RequestBody ReviseQuoteItemsRequest body) {
        return reviseItems.execute(new ReviseQuoteItemsCommand(Principals.of(token), quoteId, body.inputs(),
                CorrelationIds.current()));
    }

    @PostMapping("/{quoteId}/confirm")
    @Operation(summary = "Confirma e envia ao cliente: gera o link de aprovação (aparece só nesta resposta)")
    QuoteSentResult confirm(@AuthenticationPrincipal Jwt token, @PathVariable UUID quoteId) {
        return confirm.execute(new ConfirmQuoteCommand(Principals.of(token), quoteId, CorrelationIds.current()));
    }

    @PostMapping("/{quoteId}/resend")
    @Operation(summary = "Reenvia um orçamento já enviado, com um link novo; o estado não muda")
    QuoteSentResult resend(@AuthenticationPrincipal Jwt token, @PathVariable UUID quoteId) {
        return resend.execute(new SendQuoteCommand(Principals.of(token), quoteId, CorrelationIds.current()));
    }

    @PostMapping("/{quoteId}/cancel")
    @Operation(summary = "Cancela o orçamento; o link deixa de funcionar")
    QuoteView cancel(@AuthenticationPrincipal Jwt token, @PathVariable UUID quoteId) {
        return cancel.execute(new CancelQuoteCommand(Principals.of(token), quoteId, CorrelationIds.current()));
    }
}
