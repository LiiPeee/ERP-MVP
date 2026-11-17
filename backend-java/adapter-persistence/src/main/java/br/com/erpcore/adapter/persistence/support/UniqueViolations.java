package br.com.erpcore.adapter.persistence.support;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;

/** Reconhece qual restrição de unicidade do banco foi violada, para devolver o erro de negócio certo. */
public final class UniqueViolations {

    private UniqueViolations() {
    }

    public static boolean isViolationOf(DataIntegrityViolationException failure, String constraintName) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(failure);
        return root.getMessage() != null && root.getMessage().contains(constraintName);
    }
}
