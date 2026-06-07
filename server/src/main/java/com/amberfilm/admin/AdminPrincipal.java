package com.amberfilm.admin;

public record AdminPrincipal(long id, String username, String role, String tokenDigest) {
  public boolean canWrite() {
    return "super_admin".equals(role) || "operator".equals(role);
  }
}
