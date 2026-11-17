package br.com.erpcore.application.contact.port.out;

import br.com.erpcore.domain.company.CompanyId;
import br.com.erpcore.domain.contact.Contact;
import br.com.erpcore.domain.contact.ContactId;
import br.com.erpcore.domain.shared.PhoneNumber;
import java.util.List;
import java.util.Optional;

/** Toda consulta leva a oficina: o filtro por empresa acontece aqui e, no banco, é reforçado pela RLS. */
public interface ContactRepository {

    /**
     * Insere ou atualiza. Dois clientes da mesma oficina não podem ter o mesmo telefone; a tentativa lança
     * {@link br.com.erpcore.domain.contact.PhoneAlreadyRegisteredException}.
     */
    void save(Contact contact);

    Optional<Contact> findById(CompanyId companyId, ContactId id);

    /** Busca exata pelo telefone (no banco, por índice cego; o telefone nunca é comparado em claro). */
    Optional<Contact> findByPhone(CompanyId companyId, PhoneNumber phone);

    /** Busca por parte do nome, sem diferenciar caixa nem acento, do mais novo ao mais antigo, com teto. */
    List<Contact> searchByName(CompanyId companyId, String fragment, int limit);
}
