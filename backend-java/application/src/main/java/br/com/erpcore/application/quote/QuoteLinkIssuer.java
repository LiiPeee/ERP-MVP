package br.com.erpcore.application.quote;

import br.com.erpcore.application.quote.port.out.NewQuoteLinkSecret;
import br.com.erpcore.application.quote.port.out.PublicQuoteUrls;
import br.com.erpcore.application.quote.port.out.QuoteDelivery;
import br.com.erpcore.application.quote.port.out.QuoteDeliveryRequest;
import br.com.erpcore.application.quote.port.out.QuoteLinkCodec;
import br.com.erpcore.application.quote.port.out.QuoteLinkRepository;
import br.com.erpcore.domain.identity.TokenHash;
import br.com.erpcore.domain.quote.Quote;
import br.com.erpcore.domain.quote.QuoteLink;
import java.time.Instant;

/** Cria o link público de um orçamento enviado e o entrega. Comum a "confirmar e enviar" e a "reenviar". */
final class QuoteLinkIssuer {

    private final QuoteLinkRepository links;
    private final QuoteLinkCodec codec;
    private final PublicQuoteUrls urls;
    private final QuoteDelivery delivery;

    QuoteLinkIssuer(QuoteLinkRepository links, QuoteLinkCodec codec, PublicQuoteUrls urls, QuoteDelivery delivery) {
        this.links = links;
        this.codec = codec;
        this.urls = urls;
        this.delivery = delivery;
    }

    /** Dentro da transação: grava só o hash e devolve o endereço, que existe apenas neste resultado. */
    Issued issue(Quote quote, Instant now) {
        NewQuoteLinkSecret secret = codec.generate();
        Instant expiresAt = quote.expiresAt().orElseThrow();
        links.save(QuoteLink.issue(quote.id(), quote.companyId(), new TokenHash(secret.hash()), now, expiresAt));
        return new Issued(quote, urls.forToken(secret.value()), expiresAt);
    }

    /** Depois do commit. Se o canal falhar, o envio já valeu: devolve falso em vez de desfazer. */
    boolean deliver(Issued issued) {
        try {
            delivery.deliver(new QuoteDeliveryRequest(issued.quote().companyId(), issued.quote().id(),
                    issued.quote().contactId(), issued.publicUrl()));
            return true;
        } catch (RuntimeException channelFailure) {
            return false;
        }
    }

    QuoteSentResult result(Issued issued, boolean delivered) {
        return new QuoteSentResult(issued.quote().id().value(), issued.publicUrl(), issued.expiresAt(), delivered);
    }

    record Issued(Quote quote, String publicUrl, Instant expiresAt) {

        @Override
        public String toString() {
            return "Issued[quoteId=" + quote.id().value() + "]";
        }
    }
}
