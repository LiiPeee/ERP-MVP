package br.com.erpcore.application.support;

import br.com.erpcore.application.identity.port.out.AccessTokenIssuer;
import br.com.erpcore.application.identity.port.out.AccessTokenSubject;
import br.com.erpcore.application.identity.port.out.IssuedAccessToken;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class FakeAccessTokenIssuer implements AccessTokenIssuer {

    public static final Duration LIFETIME = Duration.ofMinutes(15);

    private final Clock clock;
    private final List<AccessTokenSubject> issuedFor = new ArrayList<>();

    public FakeAccessTokenIssuer(Clock clock) {
        this.clock = clock;
    }

    @Override
    public IssuedAccessToken issue(AccessTokenSubject subject) {
        issuedFor.add(subject);
        String value = "access-token:" + subject.userId() + ":" + subject.companyId() + ":" + subject.role();
        return new IssuedAccessToken(value, clock.instant().plus(LIFETIME));
    }

    public List<AccessTokenSubject> issuedFor() {
        return List.copyOf(issuedFor);
    }
}
