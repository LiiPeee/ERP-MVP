package br.com.erpcore.application.quote.port.out;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.quote.QuoteId;

/** Só identificadores e o endereço do link: o adaptador busca o telefone do cliente por conta própria. */
public record QuoteDeliveryRequest(CompanyId companyId, QuoteId quoteId, ContactId contactId, String publicUrl) {

    @Override
    public String toString() {
        return "QuoteDeliveryRequest[quoteId=" + quoteId.value() + "]";
    }
}
