package com.charbel.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Configuration
@ConfigurationProperties(prefix = "app")
@Validated
public class AppProps {

    private Jwt jwt = new Jwt();
    private Frontend frontend = new Frontend();
    private Mail mail = new Mail();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Frontend getFrontend() {
        return frontend;
    }

    public void setFrontend(Frontend frontend) {
        this.frontend = frontend;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    @Validated
    public static class Jwt {
        @NotBlank
        private String secret;
        private Long expirationMS;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public Long getExpirationMs() { return expirationMS; }
        public void setExpirationMs(Long expirationMS) { this.expirationMS = expirationMS; }
    }

    @Validated
    public static class Frontend {
        @NotBlank
        private String baseUrl;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class Mail {
        private String from = "no-reply@wisemoney.local";;

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
    }
}