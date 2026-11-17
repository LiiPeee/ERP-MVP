package br.com.erpcore.adapter.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/**
 * O Spring Security apaga o cookie CSRF quando uma requisição é autenticada (pensado para sessões, que trocam de
 * identificador no login). Aqui não há sessão: a autenticação é por token de acesso em cada chamada, então o
 * cookie seria apagado a cada {@code GET /api/me} e o navegador ficaria sem o valor para mandar no cabeçalho.
 * Este repositório ignora a ordem de apagar e delega o resto.
 */
final class KeepingCsrfTokenRepository implements CsrfTokenRepository {

    private final CsrfTokenRepository delegate;

    KeepingCsrfTokenRepository(CsrfTokenRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return delegate.generateToken(request);
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        if (token != null) {
            delegate.saveToken(token, request, response);
        }
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        return delegate.loadToken(request);
    }
}
