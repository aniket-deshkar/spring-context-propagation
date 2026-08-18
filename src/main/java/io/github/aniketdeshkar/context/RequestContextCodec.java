package io.github.aniketdeshkar.context;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class RequestContextCodec {
  private final Supplier<String> idSupplier;

  public RequestContextCodec(Supplier<String> idSupplier) {
    this.idSupplier = Objects.requireNonNull(idSupplier, "idSupplier");
  }

  public RequestContext decode(Map<String, String> headers) {
    String correlationId = valueOrId(headers.get(RequestContextHeaders.CORRELATION_ID));
    String requestId = valueOrId(headers.get(RequestContextHeaders.REQUEST_ID));
    return new RequestContext(
        correlationId,
        requestId,
        headers.get(RequestContextHeaders.TENANT_ID),
        headers.get(RequestContextHeaders.USER_ID),
        parseLocale(headers.get(RequestContextHeaders.LOCALE)),
        parseDeadline(headers.get(RequestContextHeaders.DEADLINE)));
  }

  public Map<String, String> encode(RequestContext context) {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put(RequestContextHeaders.CORRELATION_ID, context.correlationId());
    headers.put(RequestContextHeaders.REQUEST_ID, context.requestId());
    put(headers, RequestContextHeaders.TENANT_ID, context.tenantId());
    put(headers, RequestContextHeaders.USER_ID, context.userId());
    if (!Locale.ROOT.equals(context.locale())) {
      headers.put(RequestContextHeaders.LOCALE, context.locale().toLanguageTag());
    }
    if (context.deadline() != null) {
      headers.put(RequestContextHeaders.DEADLINE, context.deadline().toString());
    }
    return Map.copyOf(headers);
  }

  private String valueOrId(String value) {
    String result = value == null || value.isBlank() ? idSupplier.get() : value;
    if (result == null || result.isBlank()) {
      throw new IllegalStateException("idSupplier returned a blank value");
    }
    return result;
  }

  private static Instant parseDeadline(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException exception) {
      throw new IllegalArgumentException("invalid request deadline", exception);
    }
  }

  private static Locale parseLocale(String value) {
    return value == null || value.isBlank() ? Locale.ROOT : Locale.forLanguageTag(value);
  }

  private static void put(Map<String, String> target, String name, String value) {
    if (value != null) {
      target.put(name, value);
    }
  }
}
