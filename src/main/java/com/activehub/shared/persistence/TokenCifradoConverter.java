package com.activehub.shared.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cifra en la base las credenciales de terceros (tokens OAuth de Mercado Pago) con AES-256-GCM.
 *
 * <p>Formato de la columna: {@code base64(iv[12] || ciphertext || tag[16])}. IV aleatorio por
 * valor, asi que cifrar dos veces el mismo token da dos textos distintos. GCM autentica: una
 * fila manipulada o descifrada con otra clave falla, no devuelve basura.
 *
 * <p>La clave sale de {@code MP_TOKEN_ENCRYPTION_KEY} (32 bytes en base64). Es un
 * {@code @Component} para que Hibernate lo obtenga de Spring (SpringBeanContainer) con la
 * clave inyectada. <b>Sin clave la aplicacion levanta igual</b> — como el resto de las
 * credenciales externas —, pero cifrar o descifrar un token lanza {@link IllegalStateException}:
 * no se guarda nunca un token en claro "porque falto configurar".
 */
@Component
@Converter
public class TokenCifradoConverter implements AttributeConverter<String, String> {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec clave;
    private final SecureRandom random = new SecureRandom();

    public TokenCifradoConverter(@Value("${app.mercadopago.token-encryption-key:}") String claveBase64) {
        this.clave = claveBase64 == null || claveBase64.isBlank() ? null : crearClave(claveBase64);
    }

    private static SecretKeySpec crearClave(String claveBase64) {
        byte[] bytes = Base64.getDecoder().decode(claveBase64.trim());
        if (bytes.length != 32) {
            throw new IllegalStateException(
                    "MP_TOKEN_ENCRYPTION_KEY debe ser de 32 bytes en base64 (AES-256); tiene " + bytes.length);
        }
        return new SecretKeySpec(bytes, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, claveRequerida(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el token", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String columna) {
        if (columna == null) {
            return null;
        }
        try {
            byte[] datos = Base64.getDecoder().decode(columna);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, claveRequerida(), new GCMParameterSpec(TAG_BITS, datos, 0, IV_BYTES));
            byte[] plano = cipher.doFinal(datos, IV_BYTES, datos.length - IV_BYTES);
            return new String(plano, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No se pudo descifrar el token", e);
        }
    }

    private SecretKeySpec claveRequerida() {
        if (clave == null) {
            throw new IllegalStateException("Falta MP_TOKEN_ENCRYPTION_KEY para cifrar tokens de Mercado Pago");
        }
        return clave;
    }
}
