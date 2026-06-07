package com.amberfilm.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
  private final String code;
  private final HttpStatus status;

  public ApiException(String code, String message, HttpStatus status) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public String code() {
    return code;
  }

  public HttpStatus status() {
    return status;
  }

  public static ApiException badRequest(String code, String message) {
    return new ApiException(code, message, HttpStatus.BAD_REQUEST);
  }

  public static ApiException unauthorized(String message) {
    return new ApiException("AUTH_REQUIRED", message, HttpStatus.UNAUTHORIZED);
  }

  public static ApiException notFound(String message) {
    return new ApiException("NOT_FOUND", message, HttpStatus.NOT_FOUND);
  }
}

