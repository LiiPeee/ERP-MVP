package br.com.erpcore.application.contact;

import br.com.erpcore.application.contact.port.out.ContactRepository;
import br.com.erpcore.application.identity.OwnerGuard;
import br.com.erpcore.application.shared.port.out.AccessScope;
import br.com.erpcore.application.shared.port.out.UnitOfWork;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.identity.UserId;
import java.util.List;

/** Busca por nome apenas, com teto de resultados (contra raspagem). Telefone e e-mail voltam mascarados. */
public final class ListContactsUseCase {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 50;

    private final UnitOfWork unitOfWork;
    private final ContactRepository contacts;
    private final OwnerGuard guard;

    public ListContactsUseCase(UnitOfWork unitOfWork, ContactRepository contacts, OwnerGuard guard) {
        this.unitOfWork = unitOfWork;
        this.contacts = contacts;
        this.guard = guard;
    }

    public List<ContactView> execute(ListContactsQuery query) {
        CompanyId companyId = new CompanyId(query.principal().companyId());
        UserId userId = new UserId(query.principal().userId());
        int limit = query.limit() <= 0 ? DEFAULT_LIMIT : Math.min(query.limit(), MAX_LIMIT);
        String fragment = query.nameFragment() == null ? "" : query.nameFragment();
        return unitOfWork.execute(new AccessScope.Tenant(userId, companyId), () -> {
            guard.require(query.principal());
            return contacts.searchByName(companyId, fragment, limit).stream().map(ContactView::from).toList();
        });
    }
}
