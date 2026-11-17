package br.com.erpcore.adapter.web.contact;

import br.com.erpcore.adapter.web.correlation.CorrelationIds;
import br.com.erpcore.adapter.web.security.Principals;
import br.com.erpcore.application.contact.ContactView;
import br.com.erpcore.application.contact.CreateContactCommand;
import br.com.erpcore.application.contact.CreateContactUseCase;
import br.com.erpcore.application.contact.ListContactsQuery;
import br.com.erpcore.application.contact.ListContactsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/contacts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Contacts")
@SecurityRequirement(name = "bearerAuth")
final class ContactController {

    private static final int MAX_NAME = 120;
    private static final int MAX_TEXT = 254;

    private final CreateContactUseCase createContact;
    private final ListContactsUseCase listContacts;

    ContactController(CreateContactUseCase createContact, ListContactsUseCase listContacts) {
        this.createContact = createContact;
        this.listContacts = listContacts;
    }

    /**
     * A busca é só por nome (telefone e e-mail nunca vão na URL) e o resultado tem teto. Sem o parâmetro, devolve os
     * clientes mais recentes. A query string não é registrada em log.
     */
    @GetMapping
    @Operation(operationId = "listContacts", summary = "Busca clientes pelo nome (no máximo 50); telefone e e-mail voltam mascarados")
    List<ContactView> list(@AuthenticationPrincipal Jwt token,
                           @RequestParam(name = "name", required = false) @Size(max = MAX_NAME) String name,
                           @RequestParam(name = "limit", defaultValue = "0") int limit) {
        return listContacts.execute(new ListContactsQuery(Principals.of(token), name, limit));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createContact", summary = "Cadastra um cliente (telefone obrigatório, e-mail opcional)")
    ContactView create(@AuthenticationPrincipal Jwt token, @Valid @RequestBody CreateContactRequest body) {
        return createContact.execute(new CreateContactCommand(Principals.of(token), body.name(), body.phone(),
                body.email(), CorrelationIds.current()));
    }

    /** Só o tamanho é conferido aqui; as regras de cada campo são do domínio. */
    record CreateContactRequest(@Size(max = MAX_NAME) String name, @Size(max = MAX_TEXT) String phone,
                         @Size(max = MAX_TEXT) String email) {

        @Override
        public String toString() {
            return "CreateContactRequest[redacted]";
        }
    }
}
