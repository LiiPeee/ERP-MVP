package br.com.erpcore.adapter.infra.logging;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Última barreira contra dado pessoal em log: troca e-mail, CPF, CNPJ e tokens por marcadores. O código não
 * deve logar esses valores em primeiro lugar; isto cobre o que escapar (mensagem de exceção de biblioteca,
 * por exemplo). Prefere errar para o lado de mascarar demais.
 */
public final class PersonalDataMasker {

    private record Rule(Pattern pattern, String replacement) {
    }

    // Os documentos sem pontuação só valem isolados: junto de letras ou hífen (um UUID, por exemplo) não são CPF.
    // A ordem importa: o token (JWT) vem antes de e-mail, e os documentos com pontuação antes dos sem pontuação.
    private static final List<Rule> RULES = List.of(
            new Rule(Pattern.compile("eyJ[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]*"), "[token]"),
            new Rule(Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"), "[email]"),
            new Rule(Pattern.compile("\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}"), "[cnpj]"),
            new Rule(Pattern.compile("(?<![\\w-])\\d{14}(?![\\w-])"), "[cnpj]"),
            new Rule(Pattern.compile("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}"), "[cpf]"),
            new Rule(Pattern.compile("(?<![\\w-])\\d{11}(?![\\w-])"), "[cpf]"),
            new Rule(Pattern.compile("(?<![A-Za-z0-9_-])[A-Za-z0-9_-]{43,}(?![A-Za-z0-9_-])"), "[token]"));

    private PersonalDataMasker() {
    }

    public static String mask(String text) {
        if (text == null) {
            return null;
        }
        String masked = text;
        for (Rule rule : RULES) {
            masked = rule.pattern().matcher(masked).replaceAll(rule.replacement());
        }
        return masked;
    }
}
