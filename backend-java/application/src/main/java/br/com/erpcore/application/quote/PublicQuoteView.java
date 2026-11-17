package br.com.erpcore.application.quote;

import java.time.Instant;
import java.util.List;

/**
 * O que o cliente vê pelo link: o mínimo. Sem telefone, sem nome do cliente e sem a placa completa (só o final).
 */
public record PublicQuoteView(String shopName, String vehicleLabel, List<QuoteItemView> items, String total,
                              String status, Instant expiresAt) {
}
