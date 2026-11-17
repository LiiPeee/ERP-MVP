package br.com.erpcore.application.quote;

import static br.com.erpcore.application.support.ApplicationFixture.CORRELATION_ID;

import br.com.erpcore.application.contact.ContactView;
import br.com.erpcore.application.contact.CreateContactCommand;
import br.com.erpcore.application.identity.AuthenticatedPrincipal;
import br.com.erpcore.application.identity.RegisterCompanyAndOwnerResult;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.support.ApplicationFixture;
import br.com.erpcore.application.support.FakePublicQuoteUrls;
import br.com.erpcore.application.vehicle.RegisterVehicleCommand;
import br.com.erpcore.application.vehicle.VehicleView;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.quote.QuoteId;
import java.util.List;
import java.util.UUID;

/** Uma oficina com um cliente e um veículo, pronta para montar orçamentos. Cada teste cria o seu. */
abstract class QuoteScenario {

    protected final ApplicationFixture fixture = new ApplicationFixture();
    protected final RegisterCompanyAndOwnerResult owner = fixture.registerOwner();
    protected final AuthenticatedPrincipal principal = fixture.principalOf(owner);
    protected final ContactView client = fixture.createContactUseCase()
            .execute(new CreateContactCommand(principal, "Cliente Fictício", "11987654321", null, CORRELATION_ID));
    protected final VehicleView car = fixture.registerVehicleUseCase()
            .execute(new RegisterVehicleCommand(principal, client.id(), "ABC1234", "Fiat", "Uno", 2012, 123_456, CORRELATION_ID));

    protected static List<QuoteItemInput> items() {
        return List.of(new QuoteItemInput("Pastilha de freio", "PART", "2", "89.90"),
                new QuoteItemInput("Mão de obra", "LABOR", "1.5", "80.00"));
    }

    /** 2 x 89,90 + 1,5 x 80,00. */
    protected static final String ITEMS_TOTAL = "299.80";

    protected QuoteView draft() {
        return draft(principal, client.id(), car.id(), items(), null);
    }

    protected QuoteView draft(AuthenticatedPrincipal who, UUID contactId, UUID vehicleId, List<QuoteItemInput> inputItems,
                              Integer validityDays) {
        return fixture.createQuoteDraftUseCase().execute(new CreateQuoteDraftCommand(who, contactId, vehicleId,
                inputItems, "Barulho ao frear", validityDays, CORRELATION_ID));
    }

    protected QuoteSentResult sendNew() {
        return confirm(draft().id());
    }

    protected QuoteSentResult confirm(UUID quoteId) {
        return fixture.confirmQuoteUseCase().execute(new ConfirmQuoteCommand(principal, quoteId, CORRELATION_ID));
    }

    protected static String tokenOf(QuoteSentResult sent) {
        return FakePublicQuoteUrls.tokenOf(sent.publicUrl());
    }

    protected QuoteView get(UUID quoteId) {
        return fixture.getQuoteUseCase().execute(new GetQuoteQuery(principal, quoteId));
    }

    /** Quantos orçamentos estão gravados na oficina, em qualquer estado (lido numa transação, como o fake exige). */
    protected int storedQuotesCount() {
        CompanyId companyId = new CompanyId(owner.companyId());
        return fixture.unitOfWork.execute(new AccessScope.Anonymous(),
                () -> fixture.quotes.findByStatuses(companyId, java.util.EnumSet.allOf(br.com.erpcore.domain.quote.QuoteStatus.class), 100).size());
    }

    /** Põe um rascunho do dono em "aguardando confirmação", como a IA fará nas fases seguintes. */
    protected void requestOwnerConfirmation(UUID quoteId) {
        CompanyId companyId = new CompanyId(owner.companyId());
        fixture.unitOfWork.execute(new AccessScope.Anonymous(), () -> {
            Quote quote = fixture.quotes.findById(companyId, new QuoteId(quoteId)).orElseThrow();
            quote.requestOwnerConfirmation();
            fixture.quotes.save(quote);
            return null;
        });
    }
}
