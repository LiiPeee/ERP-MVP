package br.com.erpcore.application.identity;

public record GetCurrentContextQuery(AuthenticatedPrincipal principal, String correlationId) {
}
