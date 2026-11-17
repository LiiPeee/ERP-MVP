package br.com.erpcore.application.contract;

import static br.com.erpcore.application.contract.ContractData.NOW;
import static br.com.erpcore.application.contract.ContractData.uuid;

import br.com.erpcore.domain.company.Company;
import br.com.erpcore.domain.company.CompanyProfile;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.contact.ContactName;
import br.com.erpcore.domain.shared.PhoneNumber;
import br.com.erpcore.domain.identity.Email;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.quote.QuoteId;
import br.com.erpcore.domain.quote.QuoteItem;
import br.com.erpcore.domain.quote.QuoteItemKind;
import br.com.erpcore.domain.quote.QuoteLink;
import br.com.erpcore.domain.quote.QuoteStatus;
import br.com.erpcore.domain.shared.Money;
import br.com.erpcore.domain.shared.Quantity;
import br.com.erpcore.domain.vehicle.LicensePlate;
import br.com.erpcore.domain.vehicle.Vehicle;
import br.com.erpcore.domain.vehicle.VehicleId;
import java.time.Instant;
import java.util.List;

/** Dados sintéticos e claramente falsos para os contratos de contato, veículo e orçamento. */
public final class BusinessContractData {

    public static final Instant SENT_AT = Instant.parse("2026-07-01T15:00:00Z");

    private BusinessContractData() {
    }

    public static CompanyProfile profile(Company company, int validityDays) {
        return CompanyProfile.of(company.id(), "Oficina Fictícia " + validityDays,
                PhoneNumber.of("1133334444"), "Rua Fictícia, 100", "Seg a sex, 8h às 18h", validityDays, "Obrigado!");
    }

    /** O número cresce o telefone e a data de criação, então a ordem de criação é previsível. */
    public static Contact contact(long number, Company company) {
        return Contact.create(new ContactId(uuid(number)), company.id(), ContactName.of("Cliente Fictício " + number),
                PhoneNumber.of("119" + String.format("%08d", number)), null, NOW.plusSeconds(number));
    }

    public static Contact contactWithEmail(long number, Company company) {
        return Contact.create(new ContactId(uuid(number)), company.id(), ContactName.of("Cliente Fictício " + number),
                PhoneNumber.of("119" + String.format("%08d", number)), Email.of("cliente" + number + "@exemplo.test"),
                NOW.plusSeconds(number));
    }

    public static Contact namedContact(long number, Company company, String name) {
        return Contact.create(new ContactId(uuid(number)), company.id(), ContactName.of(name),
                PhoneNumber.of("119" + String.format("%08d", number)), null, NOW.plusSeconds(number));
    }

    public static Vehicle vehicle(long number, Company company, Contact owner) {
        return Vehicle.register(new VehicleId(uuid(number)), company.id(), owner.id(),
                LicensePlate.of("ABC" + String.format("%04d", number)), "Fiat", "Uno " + number, 2012, 123_456,
                NOW.plusSeconds(number));
    }

    public static List<QuoteItem> items() {
        return List.of(
                QuoteItem.of("Pastilha de freio", QuoteItemKind.PART, Quantity.of("2"), Money.of("89.90")),
                QuoteItem.of("Mão de obra", QuoteItemKind.LABOR, Quantity.of("1.5"), Money.of("80.00")));
    }

    public static Quote draftQuote(long number, Company company, Contact contact, Vehicle vehicle) {
        Quote quote = Quote.draft(new QuoteId(uuid(number)), company.id(), contact.id(), vehicle.id(), 7,
                "Barulho ao frear " + number, NOW.plusSeconds(number));
        quote.reviseItems(items());
        return quote;
    }

    /** Orçamento enviado, com prazo e estado escolhidos pelo teste (reconstituído, sem passar pelas transições). */
    public static Quote quoteWith(long number, Company company, Contact contact, Vehicle vehicle, QuoteStatus status,
                                  Instant expiresAt) {
        return Quote.restore(new QuoteId(uuid(number)), company.id(), contact.id(), vehicle.id(), 7,
                "Obs " + number, NOW.plusSeconds(number), items(), status, SENT_AT, expiresAt, null, null);
    }

    public static QuoteLink link(long number, Quote quote) {
        return QuoteLink.issue(quote.id(), quote.companyId(), new TokenHash(String.format("%064d", number)), SENT_AT,
                SENT_AT.plusSeconds(7 * 24 * 3600L));
    }
}
