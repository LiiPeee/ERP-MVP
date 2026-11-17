package br.com.erpcore.application.support;

import br.com.erpcore.application.contact.port.out.ContactRepository;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.contact.PhoneAlreadyRegisteredException;
import br.com.erpcore.domain.shared.PhoneNumber;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class InMemoryContactRepository implements ContactRepository {

    private final InMemoryUnitOfWork unitOfWork;
    private final Map<ContactId, Contact> storage = new LinkedHashMap<>();

    public InMemoryContactRepository(InMemoryUnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    @Override
    public void save(Contact contact) {
        unitOfWork.requireActive();
        boolean phoneTakenByAnother = storage.values().stream()
                .anyMatch(other -> other.companyId().equals(contact.companyId())
                        && other.phone().equals(contact.phone()) && !other.id().equals(contact.id()));
        if (phoneTakenByAnother) {
            throw new PhoneAlreadyRegisteredException();
        }
        storage.put(contact.id(), contact);
    }

    @Override
    public Optional<Contact> findById(CompanyId companyId, ContactId id) {
        unitOfWork.requireActive();
        return Optional.ofNullable(storage.get(id)).filter(contact -> contact.companyId().equals(companyId));
    }

    @Override
    public Optional<Contact> findByPhone(CompanyId companyId, PhoneNumber phone) {
        unitOfWork.requireActive();
        return storage.values().stream()
                .filter(contact -> contact.companyId().equals(companyId) && contact.phone().equals(phone))
                .findFirst();
    }

    @Override
    public List<Contact> searchByName(CompanyId companyId, String fragment, int limit) {
        unitOfWork.requireActive();
        String needle = fold(fragment.trim());
        return storage.values().stream()
                .filter(contact -> contact.companyId().equals(companyId))
                .filter(contact -> needle.isEmpty() || fold(contact.name().value()).contains(needle))
                .sorted(Comparator.comparing(Contact::createdAt).reversed())
                .limit(limit)
                .toList();
    }

    /** Sem caixa e sem acento, como a busca do banco faz. */
    private static String fold(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    public List<Contact> all() {
        return List.copyOf(storage.values());
    }
}
