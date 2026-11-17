package br.com.erpcore.application.profile;

import br.com.erpcore.application.identity.AuthenticatedPrincipal;

/** Telefone, endereço, horário e assinatura podem vir nulos ou em branco: ficam ausentes. */
public record UpdateCompanyProfileCommand(AuthenticatedPrincipal principal, String displayName, String businessPhone,
                                          String address, String openingHours, int defaultValidityDays,
                                          String signatureMessage, String correlationId) {

    @Override
    public String toString() {
        return "UpdateCompanyProfileCommand[redacted]";
    }
}
