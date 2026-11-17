package br.com.erpcore.application.quote;

import br.com.erpcore.application.contact.port.out.ContactRepository;
import br.com.erpcore.application.vehicle.port.out.VehicleRepository;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.contact.ContactNotFoundException;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.quote.QuoteStatus;
import br.com.erpcore.domain.vehicle.Vehicle;
import br.com.erpcore.domain.vehicle.VehicleNotFoundException;
import java.time.Clock;
import java.time.Instant;

/** Monta as visões do dono a partir do orçamento, do cliente e do veículo. Precisa rodar dentro da transação. */
final class QuoteViews {

    private final ContactRepository contacts;
    private final VehicleRepository vehicles;
    private final Clock clock;

    QuoteViews(ContactRepository contacts, VehicleRepository vehicles, Clock clock) {
        this.contacts = contacts;
        this.vehicles = vehicles;
        this.clock = clock;
    }

    QuoteView view(Quote quote) {
        Contact contact = contactOf(quote);
        Vehicle vehicle = vehicleOf(quote);
        QuoteStatus status = quote.statusAt(clock.instant());
        return new QuoteView(quote.id().value(), status.name(), contact.id().value(), contact.name().value(),
                contact.phone().masked(), vehicle.id().value(), vehicleLabel(vehicle),
                quote.items().stream().map(QuoteItemView::from).toList(), quote.total().toString(), quote.note(),
                quote.validityDays(), quote.createdAt(), quote.sentAt().orElse(null), quote.expiresAt().orElse(null),
                quote.decidedAt().orElse(null), quote.decisionReason().orElse(null));
    }

    QuoteSummary summary(Quote quote) {
        Contact contact = contactOf(quote);
        Vehicle vehicle = vehicleOf(quote);
        Instant now = clock.instant();
        return new QuoteSummary(quote.id().value(), quote.statusAt(now).name(), contact.name().value(),
                vehicleLabel(vehicle), quote.total().toString(), quote.createdAt(), quote.sentAt().orElse(null),
                quote.expiresAt().orElse(null));
    }

    /** "Fiat Uno 2012 · ****234": o veículo reconhecível, com a placa mascarada. */
    static String vehicleLabel(Vehicle vehicle) {
        return vehicle.brand() + " " + vehicle.model() + " " + vehicle.year() + " · " + vehicle.plate().masked();
    }

    private Contact contactOf(Quote quote) {
        return contacts.findById(quote.companyId(), quote.contactId()).orElseThrow(ContactNotFoundException::new);
    }

    private Vehicle vehicleOf(Quote quote) {
        return vehicles.findById(quote.companyId(), quote.vehicleId()).orElseThrow(VehicleNotFoundException::new);
    }
}
