package io.github.aniketdeshkar.context;

public final class RequestContextHeaders {
  public static final String CORRELATION_ID = "X-Correlation-Id";
  public static final String REQUEST_ID = "X-Request-Id";
  public static final String TENANT_ID = "X-Tenant-Id";
  public static final String USER_ID = "X-User-Id";
  public static final String DEADLINE = "X-Request-Deadline";
  public static final String LOCALE = "X-Request-Locale";

  private RequestContextHeaders() {}
}
