package br.com.erpcore.adapter.infra.quote;

import br.com.erpcore.application.quote.port.out.PublicQuoteUrls;

/** O endereço do link vem da configuração ({@code erp.public.base-url}); o domínio definitivo ainda é decisão em aberto. */
public final class ConfiguredPublicQuoteUrls implements PublicQuoteUrls {

    private static final String LINK_PATH = "/o/";

    private final String baseUrl;

    public ConfiguredPublicQuoteUrls(String baseUrl) {
        if (baseUrl == null || !(baseUrl.startsWith("https://") || baseUrl.startsWith("http://"))) {
            throw new IllegalArgumentException("the public base URL must start with http:// or https://");
        }
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public String forToken(String token) {
        return baseUrl + LINK_PATH + token;
    }
}
