package br.com.erpcore.domain.shared;

/** Raiz das exceções de regra de negócio. Só o adaptador web as traduz para HTTP. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
