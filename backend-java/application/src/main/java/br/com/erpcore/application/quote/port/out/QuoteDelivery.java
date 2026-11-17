package br.com.erpcore.application.quote.port.out;

/**
 * Entrega o link do orçamento ao cliente. Nesta fase só existe o fake; o WhatsApp vem na Fase 2. A entrega
 * acontece depois do commit: se falhar, o orçamento continua enviado e o dono pode reenviar.
 */
public interface QuoteDelivery {

    void deliver(QuoteDeliveryRequest request);
}
