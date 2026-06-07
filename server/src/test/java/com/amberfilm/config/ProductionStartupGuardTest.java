package com.amberfilm.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionStartupGuardTest {
  @Test
  void devProfileAllowsLocalDefaults() {
    MockEnvironment environment = new MockEnvironment();
    ProductionStartupGuard guard = new ProductionStartupGuard(
        environment,
        "jdbc:h2:file:./data/amber-film",
        "org.h2.Driver",
        true,
        "dev-secret-change-me",
        "dev-admin-token",
        "*",
        "local",
        "dev-secret-change-me",
        true,
        true);

    assertThatCode(() -> guard.run(null)).doesNotThrowAnyException();
  }

  @Test
  void prodProfileRejectsUnsafeDefaults() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");
    ProductionStartupGuard guard = new ProductionStartupGuard(
        environment,
        "jdbc:h2:file:./data/amber-film",
        "org.h2.Driver",
        true,
        "dev-secret-change-me",
        "dev-admin-token",
        "*",
        "local",
        "",
        true,
        true);

    assertThatThrownBy(() -> guard.run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("生产配置不安全")
        .hasMessageContaining("DB_URL 不能使用 H2")
        .hasMessageContaining("AMBER_TOKEN_SECRET 必须使用生产密钥")
        .hasMessageContaining("AMBER_ALLOWED_ORIGIN_PATTERNS 生产环境不能使用 *")
        .hasMessageContaining("AMBER_STORAGE_PROVIDER 生产环境不能使用 local/mock-local");
  }

  @Test
  void prodProfileAllowsHardenedConfig() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("production");
    ProductionStartupGuard guard = new ProductionStartupGuard(
        environment,
        "jdbc:mysql://mysql:3306/amber_film",
        "com.mysql.cj.jdbc.Driver",
        false,
        "prod-secret-with-enough-entropy",
        "prod-admin-token-with-enough-entropy",
        "https://app.example.com",
        "minio",
        "prod-storage-signing-secret",
        false,
        false);

    assertThatCode(() -> guard.run(null)).doesNotThrowAnyException();
  }
}
