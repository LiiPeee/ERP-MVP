package br.com.erpcore.adapter.web.profile;

import br.com.erpcore.adapter.web.correlation.CorrelationIds;
import br.com.erpcore.adapter.web.security.Principals;
import br.com.erpcore.application.profile.CompanyProfileView;
import br.com.erpcore.application.profile.GetCompanyProfileQuery;
import br.com.erpcore.application.profile.GetCompanyProfileUseCase;
import br.com.erpcore.application.profile.UpdateCompanyProfileCommand;
import br.com.erpcore.application.profile.UpdateCompanyProfileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/company-profile", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Company profile")
@SecurityRequirement(name = "bearerAuth")
final class CompanyProfileController {

    private static final int MAX_TEXT = 300;

    private final GetCompanyProfileUseCase getProfile;
    private final UpdateCompanyProfileUseCase updateProfile;

    CompanyProfileController(GetCompanyProfileUseCase getProfile, UpdateCompanyProfileUseCase updateProfile) {
        this.getProfile = getProfile;
        this.updateProfile = updateProfile;
    }

    @GetMapping
    @Operation(summary = "O perfil da oficina que aparece para o cliente (telefone mascarado)")
    CompanyProfileView get(@AuthenticationPrincipal Jwt token) {
        return getProfile.execute(new GetCompanyProfileQuery(Principals.of(token)));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualiza o perfil da oficina")
    CompanyProfileView update(@AuthenticationPrincipal Jwt token, @Valid @RequestBody UpdateCompanyProfileRequest body) {
        return updateProfile.execute(new UpdateCompanyProfileCommand(Principals.of(token), body.displayName(),
                body.businessPhone(), body.address(), body.openingHours(),
                body.defaultValidityDays() == null ? 0 : body.defaultValidityDays(), body.signatureMessage(),
                CorrelationIds.current()));
    }

    /** Só o tamanho é conferido aqui; as regras de cada campo são do domínio. */
    record UpdateCompanyProfileRequest(@Size(max = MAX_TEXT) String displayName, @Size(max = MAX_TEXT) String businessPhone,
                         @Size(max = MAX_TEXT) String address, @Size(max = MAX_TEXT) String openingHours,
                         Integer defaultValidityDays, @Size(max = MAX_TEXT) String signatureMessage) {

        @Override
        public String toString() {
            return "UpdateCompanyProfileRequest[redacted]";
        }
    }
}
