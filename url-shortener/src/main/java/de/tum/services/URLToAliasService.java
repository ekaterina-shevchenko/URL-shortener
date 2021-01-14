package de.tum.services;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.util.Base64;

public class URLToAliasService {
    private static final URLToAliasService INSTANCE = new URLToAliasService();
    private URLToAliasService() {}

    public static URLToAliasService getInstance() {
        return INSTANCE;
    }

    @SneakyThrows
    public String generateAlias(String url) {

            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] hash = md5.digest(url.getBytes());

            String key = Base64.getEncoder().encodeToString(hash);
            if (key.startsWith("/")) {
                key = key.replaceFirst("/", "=");
            }
            return key.substring(0,6);
    }
}
