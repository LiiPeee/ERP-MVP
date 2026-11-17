package br.com.erpcore.application.identity.port.out;

public interface AccessTokenIssuer {

    IssuedAccessToken issue(AccessTokenSubject subject);
}
