package br.com.erpcore.domain.quote;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.shared.DomainValidationException;
import br.com.erpcore.domain.shared.Money;
import br.com.erpcore.domain.shared.Validation;
import br.com.erpcore.domain.vehicle.VehicleId;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * O orçamento. Dono das regras: total calculado aqui, máquina de estados, prazo e decisão do cliente.
 * <p>
 * Prazo: ao enviar, vale até o <b>fim do dia</b> (no fuso da oficina) que está {@code validityDays} dias à frente.
 * Enviado em 01/07 com 7 dias, vale até 08/07 23:59:59 e vence em 09/07 00:00 local.
 * <p>
 * Decisão do cliente: repetir a mesma decisão não muda nada; a decisão oposta depois de decidido é recusada.
 */
public final class Quote {

    public static final int NOTE_MAX_LENGTH = 500;
    public static final int REASON_MAX_LENGTH = 300;
    public static final int MAX_ITEMS = 50;
    public static final int MAX_VALIDITY_DAYS = 90;
    public static final Money MAX_TOTAL = Money.of("10000000.00");

    private final QuoteId id;
    private final CompanyId companyId;
    private final ContactId contactId;
    private final VehicleId vehicleId;
    private final int validityDays;
    private final String note;
    private final Instant createdAt;
    private List<QuoteItem> items = List.of();
    private QuoteStatus status = QuoteStatus.DRAFT;
    private Instant sentAt;
    private Instant expiresAt;
    private Instant decidedAt;
    private String decisionReason;

    private Quote(QuoteId id, CompanyId companyId, ContactId contactId, VehicleId vehicleId, int validityDays,
                  String note, Instant createdAt) {
        this.id = id;
        this.companyId = companyId;
        this.contactId = contactId;
        this.vehicleId = vehicleId;
        this.validityDays = validityDays;
        this.note = note;
        this.createdAt = createdAt;
    }

    public static Quote draft(QuoteId id, CompanyId companyId, ContactId contactId, VehicleId vehicleId,
                              int validityDays, String note, Instant now) {
        if (validityDays < 1 || validityDays > MAX_VALIDITY_DAYS) {
            throw new DomainValidationException("validityDays", Validation.OUT_OF_RANGE);
        }
        return new Quote(id, companyId, contactId, vehicleId, validityDays, normalizedNote(note), now);
    }

    // ---------------------------------------------------------------- montagem

    /** Troca todos os itens. Só enquanto o orçamento ainda não foi enviado. */
    public void reviseItems(List<QuoteItem> newItems) {
        if (status != QuoteStatus.DRAFT && status != QuoteStatus.AWAITING_OWNER_CONFIRMATION) {
            throw new InvalidQuoteTransitionException(status, "reviseItems");
        }
        if (newItems.size() > MAX_ITEMS) {
            throw new DomainValidationException("items", Validation.OUT_OF_RANGE);
        }
        if (sum(newItems).compareTo(MAX_TOTAL) > 0) {
            throw new DomainValidationException("total", Validation.OUT_OF_RANGE);
        }
        items = List.copyOf(newItems);
    }

    /** Quem montou o rascunho (a IA, nas fases seguintes) pede que o dono confirme. */
    public void requestOwnerConfirmation() {
        requireStatus(QuoteStatus.DRAFT, "requestOwnerConfirmation");
        requireReadyToSend();
        status = QuoteStatus.AWAITING_OWNER_CONFIRMATION;
    }

    /** O dono confirma e o orçamento sai para o cliente. É o único ato humano que envia (princípio 0b). */
    public void confirmAndSend(Instant now, ZoneId zone) {
        if (status != QuoteStatus.DRAFT && status != QuoteStatus.AWAITING_OWNER_CONFIRMATION) {
            throw new InvalidQuoteTransitionException(status, "confirmAndSend");
        }
        requireReadyToSend();
        status = QuoteStatus.SENT;
        sentAt = now;
        expiresAt = now.atZone(zone).toLocalDate().plusDays(validityDays + 1L).atStartOfDay(zone).toInstant();
    }

    // ---------------------------------------------------------------- decisão do cliente

    public void approve(Instant now) {
        if (status == QuoteStatus.APPROVED) {
            return;
        }
        requireOpenForDecision(now, "approve");
        status = QuoteStatus.APPROVED;
        decidedAt = now;
    }

    public void reject(Instant now, String reason) {
        if (status == QuoteStatus.REJECTED) {
            return;
        }
        requireOpenForDecision(now, "reject");
        decisionReason = normalizedReason(reason);
        status = QuoteStatus.REJECTED;
        decidedAt = now;
    }

    // ---------------------------------------------------------------- cancelamento e prazo

    public void cancel(Instant now) {
        if (status == QuoteStatus.CANCELLED) {
            return;
        }
        if (status.isFinal()) {
            throw new InvalidQuoteTransitionException(status, "cancel");
        }
        status = QuoteStatus.CANCELLED;
        decidedAt = now;
    }

    /** Marca como vencido um orçamento enviado cujo prazo acabou. Devolve se houve mudança. */
    public boolean expireIfDue(Instant now) {
        if (status != QuoteStatus.SENT || !isPastExpiry(now)) {
            return false;
        }
        status = QuoteStatus.EXPIRED;
        return true;
    }

    /** O estado como o cliente o vê agora: enviado fora do prazo já é vencido, mesmo antes de a tarefa rodar. */
    public QuoteStatus statusAt(Instant now) {
        return status == QuoteStatus.SENT && isPastExpiry(now) ? QuoteStatus.EXPIRED : status;
    }

    // ---------------------------------------------------------------- consulta

    public Money total() {
        return sum(items);
    }

    public QuoteId id() {
        return id;
    }

    public CompanyId companyId() {
        return companyId;
    }

    public ContactId contactId() {
        return contactId;
    }

    public VehicleId vehicleId() {
        return vehicleId;
    }

    public int validityDays() {
        return validityDays;
    }

    public String note() {
        return note;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public List<QuoteItem> items() {
        return items;
    }

    public QuoteStatus status() {
        return status;
    }

    public Optional<Instant> sentAt() {
        return Optional.ofNullable(sentAt);
    }

    public Optional<Instant> expiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    public Optional<Instant> decidedAt() {
        return Optional.ofNullable(decidedAt);
    }

    public Optional<String> decisionReason() {
        return Optional.ofNullable(decisionReason);
    }

    // ---------------------------------------------------------------- internos

    private void requireStatus(QuoteStatus expected, String action) {
        if (status != expected) {
            throw new InvalidQuoteTransitionException(status, action);
        }
    }

    private void requireOpenForDecision(Instant now, String action) {
        requireStatus(QuoteStatus.SENT, action);
        if (isPastExpiry(now)) {
            throw new QuoteUnavailableException();
        }
    }

    private void requireReadyToSend() {
        if (items.isEmpty()) {
            throw new DomainValidationException("items", Validation.REQUIRED);
        }
        if (!total().isPositive()) {
            throw new DomainValidationException("total", Validation.OUT_OF_RANGE);
        }
    }

    private boolean isPastExpiry(Instant now) {
        return !now.isBefore(expiresAt);
    }

    private static Money sum(List<QuoteItem> lines) {
        return lines.stream().map(QuoteItem::lineTotal).reduce(Money.zero(), Money::add);
    }

    /** A observação é opcional; sem texto, fica vazia. */
    private static String normalizedNote(String raw) {
        return raw == null || raw.isBlank() ? "" : Validation.requireTrimmedText("note", raw, NOTE_MAX_LENGTH);
    }

    /** O motivo da recusa é opcional; sem texto, não há motivo. */
    private static String normalizedReason(String raw) {
        return raw == null || raw.isBlank() ? null : Validation.requireTrimmedText("reason", raw, REASON_MAX_LENGTH);
    }
}
