package br.com.erpcore.adapter.persistence.contact;

import br.com.erpcore.adapter.persistence.support.FieldColumns;
import br.com.erpcore.adapter.persistence.support.UniqueViolations;
import br.com.erpcore.application.contact.port.out.ContactRepository;
import br.com.erpcore.application.shared.port.out.FieldContext;
import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.contact.ContactName;
import br.com.erpcore.domain.contact.PhoneAlreadyRegisteredException;
import br.com.erpcore.domain.identity.Email;
import br.com.erpcore.domain.shared.PhoneNumber;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

/**
 * Telefone e e-mail ficam cifrados; o telefone ganha um índice cego para busca exata e para a regra de um telefone
 * por oficina. O nome fica em claro e a busca usa uma cópia dele sem acento e sem maiúsculas.
 */
public final class JpaContactRepository implements ContactRepository {

    private static final String PHONE_FIELD = "contact.phone";
    private static final String EMAIL_FIELD = "contact.email";
    private static final String PHONE_UNIQUE_CONSTRAINT = "contact_phone_unique_per_company";

    private final ContactJpaRepository jpa;
    private final FieldColumns columns;

    public JpaContactRepository(ContactJpaRepository jpa, FieldColumns columns) {
        this.jpa = jpa;
        this.columns = columns;
    }

    @Override
    public void save(Contact contact) {
        FieldContext phoneContext = new FieldContext(contact.companyId(), PHONE_FIELD);
        FieldContext emailContext = new FieldContext(contact.companyId(), EMAIL_FIELD);
        Optional<ContactJpaEntity> current = jpa.findById(contact.id().value());
        byte[] phone = columns.reuseOrEncrypt(phoneContext, current.map(ContactJpaEntity::getPhoneCiphertext).orElse(null),
                contact.phone().value());
        byte[] email = contact.email().map(value -> columns.reuseOrEncrypt(emailContext,
                current.map(ContactJpaEntity::getEmailCiphertext).orElse(null), value.value())).orElse(null);
        try {
            jpa.saveAndFlush(new ContactJpaEntity(contact.id().value(), contact.companyId().value(), contact.name().value(),
                    fold(contact.name().value()), phone, columns.blindIndex(phoneContext, contact.phone().value()), email,
                    contact.createdAt()));
        } catch (DataIntegrityViolationException failure) {
            if (UniqueViolations.isViolationOf(failure, PHONE_UNIQUE_CONSTRAINT)) {
                throw new PhoneAlreadyRegisteredException();
            }
            throw failure;
        }
    }

    @Override
    public Optional<Contact> findById(CompanyId companyId, ContactId id) {
        return jpa.findByIdAndCompanyId(id.value(), companyId.value()).map(this::toDomain);
    }

    @Override
    public Optional<Contact> findByPhone(CompanyId companyId, PhoneNumber phone) {
        String index = columns.blindIndex(new FieldContext(companyId, PHONE_FIELD), phone.value());
        return jpa.findByCompanyIdAndPhoneBlindIndex(companyId.value(), index).map(this::toDomain);
    }

    @Override
    public List<Contact> searchByName(CompanyId companyId, String fragment, int limit) {
        String pattern = "%" + escapeLike(fold(fragment.trim())) + "%";
        return jpa.searchByName(companyId.value(), pattern, PageRequest.of(0, limit)).stream().map(this::toDomain).toList();
    }

    private Contact toDomain(ContactJpaEntity entity) {
        CompanyId companyId = new CompanyId(entity.getCompanyId());
        PhoneNumber phone = new PhoneNumber(columns.decrypt(new FieldContext(companyId, PHONE_FIELD), entity.getPhoneCiphertext()));
        Optional<Email> email = Optional.ofNullable(entity.getEmailCiphertext())
                .map(cipher -> Email.of(columns.decrypt(new FieldContext(companyId, EMAIL_FIELD), cipher)));
        return new Contact(new ContactId(entity.getId()), companyId, ContactName.of(entity.getName()), phone, email,
                entity.getCreatedAt());
    }

    /** Sem acento e sem maiúsculas, para a busca por nome. */
    static String fold(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** O que o usuário digita é texto, não padrão: %, _ e \ valem por si. */
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
