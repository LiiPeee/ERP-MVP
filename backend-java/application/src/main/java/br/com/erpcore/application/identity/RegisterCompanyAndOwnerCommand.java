package br.com.erpcore.application.identity;

public record RegisterCompanyAndOwnerCommand(String companyName, String ownerName, String email, String password,
                                             String correlationId) {

    @Override
    public String toString() {
        return "RegisterCompanyAndOwnerCommand[redacted]";
    }
}
