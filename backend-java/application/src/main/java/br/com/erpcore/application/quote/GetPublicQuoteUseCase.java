package br.com.erpcore.application.quote;

import br.com.erpcore.application.profile.port.out.CompanyProfileRepository;
import br.com.erpcore.application.company.port.out.CompanyRepository;
import br.com.erpcore.application.quote.port.out.QuoteLinkCodec;
import br.com.erpcore.application.quote.port.out.QuoteLinkRepository;
import br.com.erpcore.application.quote.port.out.QuoteRepository;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.application.vehicle.port.out.VehicleRepository;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.company.CompanyProfile;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.quote.QuoteLink;
import br.com.erpcore.domain.quote.QuoteLinkUnavailableException;
import br.com.erpcore.domain.quote.QuoteStatus;
import br.com.erpcore.domain.vehicle.Vehicle;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;

/**
 * A página pública do orçamento. Abrir é só consulta: não gasta o link nem muda nada (leitores de prévia do WhatsApp
 * e rastreadores abrem links sozinhos). Só orçamento enviado ou já respondido aparece; qualquer outro caso é
 * "indisponível", igual a link inexistente.
 */
public final class GetPublicQuoteUseCase {

    private static final Set<QuoteStatus> VISIBLE = Set.of(QuoteStatus.SENT, QuoteStatus.APPROVED, QuoteStatus.REJECTED);

    private final UnitOfWork unitOfWork;
    private final QuoteRepository quotes;
    private final QuoteLinkRepository links;
    private final VehicleRepository vehicles;
    private final CompanyProfileRepository profiles;
    private final CompanyRepository companies;
    private final Clock clock;
    private final PublicQuoteAccess access;

    public GetPublicQuoteUseCase(UnitOfWork unitOfWork, QuoteRepository quotes, QuoteLinkRepository links,
                                 QuoteLinkCodec codec, VehicleRepository vehicles, CompanyProfileRepository profiles,
                                 CompanyRepository companies, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.quotes = quotes;
        this.links = links;
        this.vehicles = vehicles;
        this.profiles = profiles;
        this.companies = companies;
        this.clock = clock;
        this.access = new PublicQuoteAccess(unitOfWork, links, codec, clock);
    }

    public PublicQuoteView execute(GetPublicQuoteQuery query) {
        PublicQuoteAccess.Located located = access.locate(query.token());
        return unitOfWork.execute(access.scopeFor(located), () -> {
            Instant now = clock.instant();
            QuoteLink link = links.findByHash(located.hash()).filter(found -> found.isActiveAt(now))
                    .orElseThrow(QuoteLinkUnavailableException::new);
            Quote quote = quotes.findById(located.companyId(), link.quoteId()).orElseThrow(QuoteLinkUnavailableException::new);
            QuoteStatus status = quote.statusAt(now);
            if (!VISIBLE.contains(status)) {
                throw new QuoteLinkUnavailableException();
            }
            Vehicle vehicle = vehicles.findById(located.companyId(), quote.vehicleId())
                    .orElseThrow(QuoteLinkUnavailableException::new);
            return new PublicQuoteView(shopName(located.companyId()), publicVehicleLabel(vehicle),
                    quote.items().stream().map(QuoteItemView::from).toList(), quote.total().toString(), status.name(),
                    quote.expiresAt().orElseThrow());
        });
    }

    private String shopName(CompanyId companyId) {
        return profiles.find(companyId).map(CompanyProfile::displayName).orElseGet(() -> companies.findById(companyId)
                .orElseThrow(QuoteLinkUnavailableException::new).name().value());
    }

    /** "Fiat Uno 2012 · placa final 234": o suficiente para o cliente reconhecer o carro. */
    private static String publicVehicleLabel(Vehicle vehicle) {
        return vehicle.brand() + " " + vehicle.model() + " " + vehicle.year() + " · placa final "
                + vehicle.plate().lastCharacters();
    }
}
