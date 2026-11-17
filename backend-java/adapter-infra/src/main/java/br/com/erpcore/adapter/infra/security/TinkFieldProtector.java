package br.com.erpcore.adapter.infra.security;

import br.com.erpcore.application.shared.port.out.FieldContext;
import br.com.erpcore.application.shared.port.out.FieldProtectionException;
import br.com.erpcore.application.shared.port.out.FieldProtector;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.InsecureSecretKeyAccess;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.Mac;
import com.google.crypto.tink.RegistryConfiguration;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AesGcmKey;
import com.google.crypto.tink.aead.AesGcmParameters;
import com.google.crypto.tink.mac.HmacKey;
import com.google.crypto.tink.mac.HmacParameters;
import com.google.crypto.tink.mac.MacConfig;
import com.google.crypto.tink.util.SecretBytes;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

/**
 * {@link FieldProtector} sobre o Tink: AES-256-GCM com os dados associados (empresa e campo) autenticados, e HMAC-SHA256
 * para o índice cego. O texto cifrado começa com um prefixo que identifica a chave usada, então a troca de chave
 * não impede de ler o que foi gravado antes. Nada de criptografia feita à mão.
 */
public final class TinkFieldProtector implements FieldProtector {

    private static final int AES_KEY_BYTES = 32;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BYTES = 16;
    private static final int MAC_TAG_BYTES = 32;
    private static final int FIXED_MAC_KEY_ID = 1;

    private final Aead aead;
    private final Mac mac;

    public TinkFieldProtector(FieldEncryptionSettings settings) {
        try {
            AeadConfig.register();
            MacConfig.register();
            this.aead = buildAead(settings);
            this.mac = buildMac(settings.blindIndexKey());
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("field encryption could not be initialised", failure);
        }
    }

    @Override
    public byte[] encrypt(FieldContext context, String plaintext) {
        try {
            return aead.encrypt(plaintext.getBytes(StandardCharsets.UTF_8), associatedData(context));
        } catch (GeneralSecurityException failure) {
            throw new FieldProtectionException("field could not be encrypted", failure);
        }
    }

    @Override
    public String decrypt(FieldContext context, byte[] ciphertext) {
        try {
            return new String(aead.decrypt(ciphertext, associatedData(context)), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | RuntimeException failure) {
            throw new FieldProtectionException("field could not be decrypted", failure);
        }
    }

    @Override
    public String blindIndex(FieldContext context, String normalizedValue) {
        try {
            byte[] input = (context.associatedData() + "|" + normalizedValue).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(mac.computeMac(input));
        } catch (GeneralSecurityException failure) {
            throw new FieldProtectionException("blind index could not be computed", failure);
        }
    }

    private static byte[] associatedData(FieldContext context) {
        return context.associatedData().getBytes(StandardCharsets.UTF_8);
    }

    private static Aead buildAead(FieldEncryptionSettings settings) throws GeneralSecurityException {
        KeysetHandle.Builder keyset = KeysetHandle.newBuilder();
        Set<Integer> usedIds = new HashSet<>();
        Set<String> usedKeys = new HashSet<>();
        for (var entry : settings.encryptionKeys().entrySet()) {
            int id = keyId(entry.getKey());
            if (!usedIds.add(id) || !usedKeys.add(HexFormat.of().formatHex(entry.getValue()))) {
                throw new IllegalArgumentException("encryption keys must be distinct");
            }
            AesGcmParameters parameters = AesGcmParameters.builder().setKeySizeBytes(AES_KEY_BYTES)
                    .setIvSizeBytes(GCM_IV_BYTES).setTagSizeBytes(GCM_TAG_BYTES)
                    .setVariant(AesGcmParameters.Variant.TINK).build();
            AesGcmKey key = AesGcmKey.builder().setParameters(parameters)
                    .setKeyBytes(SecretBytes.copyFrom(entry.getValue(), InsecureSecretKeyAccess.get()))
                    .setIdRequirement(id).build();
            KeysetHandle.Builder.Entry imported = KeysetHandle.importKey(key).withFixedId(id);
            if (entry.getKey().equals(settings.activeKeyId())) {
                imported.makePrimary();
            }
            keyset.addEntry(imported);
        }
        return keyset.build().getPrimitive(RegistryConfiguration.get(), Aead.class);
    }

    private static Mac buildMac(byte[] blindKey) throws GeneralSecurityException {
        HmacParameters parameters = HmacParameters.builder().setKeySizeBytes(blindKey.length).setTagSizeBytes(MAC_TAG_BYTES)
                .setHashType(HmacParameters.HashType.SHA256).setVariant(HmacParameters.Variant.NO_PREFIX).build();
        HmacKey key = HmacKey.builder().setParameters(parameters)
                .setKeyBytes(SecretBytes.copyFrom(blindKey, InsecureSecretKeyAccess.get())).build();
        KeysetHandle handle = KeysetHandle.newBuilder()
                .addEntry(KeysetHandle.importKey(key).withFixedId(FIXED_MAC_KEY_ID).makePrimary()).build();
        return handle.getPrimitive(RegistryConfiguration.get(), Mac.class);
    }

    /** O Tink identifica a chave por um inteiro de 32 bits; aqui vem do nome da chave, de forma estável. */
    private static int keyId(String keyName) throws GeneralSecurityException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(keyName.getBytes(StandardCharsets.UTF_8));
        return ByteBuffer.wrap(Arrays.copyOf(digest, Integer.BYTES)).getInt();
    }
}
