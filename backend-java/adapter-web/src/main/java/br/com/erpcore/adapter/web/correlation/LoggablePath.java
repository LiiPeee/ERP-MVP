package br.com.erpcore.adapter.web.correlation;

/**
 * O caminho como pode ir para o log. O token do link público é uma credencial: quem lê o log com ele poderia
 * aprovar ou recusar o orçamento. Por isso o segmento do token vira {@code {token}}, mesmo que o valor seja inválido.
 */
final class LoggablePath {

    private static final String PUBLIC_API = "/api/public/quotes/";
    private static final String CLIENT_PAGE = "/o/";
    private static final String HIDDEN = "{token}";

    private LoggablePath() {
    }

    static String of(String path) {
        if (path == null) {
            return "";
        }
        if (path.startsWith(PUBLIC_API) && path.length() > PUBLIC_API.length()) {
            return PUBLIC_API + HIDDEN + afterSegment(path, PUBLIC_API.length());
        }
        if (path.startsWith(CLIENT_PAGE) && path.length() > CLIENT_PAGE.length()) {
            return CLIENT_PAGE + HIDDEN + afterSegment(path, CLIENT_PAGE.length());
        }
        return path;
    }

    private static String afterSegment(String path, int segmentStart) {
        int end = path.indexOf('/', segmentStart);
        return end < 0 ? "" : path.substring(end);
    }
}
