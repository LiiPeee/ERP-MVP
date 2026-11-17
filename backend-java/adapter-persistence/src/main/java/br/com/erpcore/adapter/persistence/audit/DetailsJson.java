package br.com.erpcore.adapter.persistence.audit;

import java.util.Map;
import java.util.TreeMap;

/** Converte os detalhes de um evento (mapa de texto) em um objeto JSON, com as chaves em ordem. */
final class DetailsJson {

    private static final int FIRST_PRINTABLE = 0x20;

    private DetailsJson() {
    }

    static String toJson(Map<String, String> details) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : new TreeMap<>(details).entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            appendQuoted(json, entry.getKey());
            json.append(':');
            appendQuoted(json, entry.getValue());
        }
        return json.append('}').toString();
    }

    private static void appendQuoted(StringBuilder json, String text) {
        json.append('"');
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            switch (character) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < FIRST_PRINTABLE) {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        json.append('"');
    }
}
