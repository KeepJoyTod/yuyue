package com.amberfilm.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionStartupGuard implements ApplicationRunner {
  private final Environment environment;
  private final String datasourceUrl;
  private final String datasourceDriver;
  private final boolean h2ConsoleEnabled;
  private final String tokenSecret;
  private final String adminToken;
  private final String allowedOriginPatterns;
  private final String storageProvider;
  private final String storageSigningSecret;
  private final boolean devSmsEnabled;
  private final boolean devPaymentCallbackEnabled;

  public ProductionStartupGuard(
      Environment environment,
      @Value("${spring.datasource.url}") String datasourceUrl,
      @Value("${spring.datasource.driver-class-name}") String datasourceDriver,
      @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled,
      @Value("${amber.auth.token-secret}") String tokenSecret,
      @Value("${amber.admin.token}") String adminToken,
      @Value("${amber.web.allowed-origin-patterns:*}") String allowedOriginPatterns,
      @Value("${amber.storage.provider:local}") String storageProvider,
      @Value("${amber.storage.signing-secret:}") String storageSigningSecret,
      @Value("${amber.auth.dev-sms-enabled:true}") boolean devSmsEnabled,
      @Value("${amber.payment.dev-callback-enabled:true}") boolean devPaymentCallbackEnabled) {
    this.environment = environment;
    this.datasourceUrl = datasourceUrl;
    this.datasourceDriver = datasourceDriver;
    this.h2ConsoleEnabled = h2ConsoleEnabled;
    this.tokenSecret = tokenSecret;
    this.adminToken = adminToken;
    this.allowedOriginPatterns = allowedOriginPatterns;
    this.storageProvider = storageProvider;
    this.storageSigningSecret = storageSigningSecret;
    this.devSmsEnabled = devSmsEnabled;
    this.devPaymentCallbackEnabled = devPaymentCallbackEnabled;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!isProductionProfile()) {
      return;
    }
    List<String> errors = new ArrayList<>();
    if (datasourceUrl != null && datasourceUrl.startsWith("jdbc:h2:")) {
      errors.add("DB_URL 不能使用 H2");
    }
    if ("org.h2.Driver".equals(datasourceDriver)) {
      errors.add("DB_DRIVER 不能使用 H2 Driver");
    }
    if (h2ConsoleEnabled) {
      errors.add("H2 console 生产环境必须关闭");
    }
    if (isBlankOrDefault(tokenSecret, "dev-secret-change-me")) {
      errors.add("AMBER_TOKEN_SECRET 必须使用生产密钥");
    }
    if (isBlankOrDefault(adminToken, "dev-admin-token")) {
      errors.add("AMBER_ADMIN_TOKEN 必须使用生产密钥");
    }
    if (isWildcardCors(allowedOriginPatterns)) {
      errors.add("AMBER_ALLOWED_ORIGIN_PATTERNS 生产环境不能使用 *");
    }
    if (isBlankOrDefault(storageProvider, "local") || "mock-local".equals(storageProvider)) {
      errors.add("AMBER_STORAGE_PROVIDER 生产环境不能使用 local/mock-local");
    }
    if (storageSigningSecret == null || storageSigningSecret.isBlank()) {
      errors.add("AMBER_STORAGE_SIGNING_SECRET 必须使用生产密钥");
    }
    if (devSmsEnabled) {
      errors.add("AMBER_AUTH_DEV_SMS_ENABLED 生产环境必须为 false");
    }
    if (devPaymentCallbackEnabled) {
      errors.add("AMBER_PAYMENT_DEV_CALLBACK_ENABLED 生产环境必须为 false");
    }
    if (!errors.isEmpty()) {
      throw new IllegalStateException("生产配置不安全: " + String.join("; ", errors));
    }
  }

  private boolean isProductionProfile() {
    return Arrays.stream(environment.getActiveProfiles())
        .anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
  }

  private boolean isBlankOrDefault(String value, String defaultValue) {
    return value == null || value.isBlank() || defaultValue.equals(value);
  }

  private boolean isWildcardCors(String value) {
    return value == null || Arrays.stream(value.split(","))
        .map(String::trim)
        .anyMatch("*"::equals);
  }
}
