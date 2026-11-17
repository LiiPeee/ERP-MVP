package br.com.erpcore.application.support;

import br.com.erpcore.application.quote.port.out.PublicQuoteUrls;

public final class FakePublicQuoteUrls implements PublicQuoteUrls {

    public static final String BASE = "https://oficina.exemplo.test/o/";

    @Override
    public String forToken(String token) {
        return BASE + token;
    }

    /** O token que vai no final do endereço, como o cliente o apresentaria. */
    public static String tokenOf(String url) {
        return url.substring(BASE.length());
    }
}
