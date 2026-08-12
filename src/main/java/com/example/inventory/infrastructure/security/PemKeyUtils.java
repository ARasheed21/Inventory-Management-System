package com.example.inventory.infrastructure.security;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.core.io.ClassPathResource;

public final class PemKeyUtils {

    private PemKeyUtils() {
    }

    public static PublicKey loadPublicKey(String resourcePath) throws Exception {
        byte[] keyBytes = readPem(resourcePath);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public static PrivateKey loadPrivateKey(String resourcePath) throws Exception {
        byte[] keyBytes = readPem(resourcePath);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private static byte[] readPem(String resourcePath) throws Exception {
        String content;
        if (resourcePath != null && resourcePath.startsWith("classpath:")) {
            String r = resourcePath.substring("classpath:".length());
            try (InputStream is = new ClassPathResource(r).getInputStream()) {
                content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } else {
            try (InputStream is = new ClassPathResource(resourcePath).getInputStream()) {
                content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        content = content.replaceAll("-----BEGIN (.*)-----", "");
        content = content.replaceAll("-----END (.*)-----", "");
        content = content.replaceAll("\\s", "");
        return Base64.getDecoder().decode(content);
    }
}
