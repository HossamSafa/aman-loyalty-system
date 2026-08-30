package com.aman.acceptance.loyalty.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "loyalty.otp")
public class OtpProperties {
    private int length;
    private int ttlSeconds;
    private int maxAttempts;
}