package br.com.erpcore.adapter.web.vehicle;

import br.com.erpcore.adapter.web.correlation.CorrelationIds;
import br.com.erpcore.adapter.web.security.Principals;
import br.com.erpcore.application.vehicle.ListVehiclesQuery;
import br.com.erpcore.application.vehicle.ListVehiclesUseCase;
import br.com.erpcore.application.vehicle.RegisterVehicleCommand;
import br.com.erpcore.application.vehicle.RegisterVehicleUseCase;
import br.com.erpcore.application.vehicle.VehicleView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/contacts/{contactId}/vehicles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Veículos")
@SecurityRequirement(name = "bearerAuth")
final class VehicleController {

    private static final int MAX_TEXT = 100;

    private final RegisterVehicleUseCase registerVehicle;
    private final ListVehiclesUseCase listVehicles;

    VehicleController(RegisterVehicleUseCase registerVehicle, ListVehiclesUseCase listVehicles) {
        this.registerVehicle = registerVehicle;
        this.listVehicles = listVehicles;
    }

    @GetMapping
    @Operation(summary = "Os veículos de um cliente; a placa volta mascarada")
    List<VehicleView> list(@AuthenticationPrincipal Jwt token, @PathVariable UUID contactId) {
        return listVehicles.execute(new ListVehiclesQuery(Principals.of(token), contactId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um veículo do cliente")
    VehicleView register(@AuthenticationPrincipal Jwt token, @PathVariable UUID contactId,
                         @Valid @RequestBody RegisterRequest body) {
        return registerVehicle.execute(new RegisterVehicleCommand(Principals.of(token), contactId, body.plate(),
                body.brand(), body.model(), body.year() == null ? 0 : body.year(), body.odometerKm(),
                CorrelationIds.current()));
    }

    /**
     * Só o tamanho é conferido aqui; as regras de cada campo são do domínio. O ano é {@code Integer}: ausente vira 0 e
     * o domínio recusa com o nome do campo, depois de conferir a empresa (um primitivo faria o corpo vazio morrer em
     * 400 antes da autorização).
     */
    record RegisterRequest(@Size(max = MAX_TEXT) String plate, @Size(max = MAX_TEXT) String brand,
                           @Size(max = MAX_TEXT) String model, Integer year, Integer odometerKm) {

        @Override
        public String toString() {
            return "RegisterRequest[redacted]";
        }
    }
}
