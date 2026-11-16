package br.com.erpcore.application.shared.port.out;

import java.util.UUID;

public interface IdGenerator {

    UUID newId();
}
