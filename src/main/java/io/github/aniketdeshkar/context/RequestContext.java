package io.github.aniketdeshkar.context;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

public record RequestContext(
    String correlationId,
    String requestId,
    String tenantId,
    String userId,
    Locale locale,
    Instant deadline) {
  public RequestContext {
    correlationId = required(correlationId, "correlationId");
    requestId = required(requestId, "requestId");
    tenantId = optional(tenantId, "tenantId");
    userId = optional(userId, "userId");
    locale = locale == null ? Locale.ROOT : locale;
  }

  public boolean isExpired(Clock clock) {
    return deadline != null && !deadline.isAfter(clock.instant());
  }

  public Duration remaining(Clock clock) {
    if (deadline == null) {
      return Duration.ofMillis(Long.MAX_VALUE);
    }
    Duration remaining = Duration.between(clock.instant(), deadline);
    return remaining.isNegative() ? Duration.ZERO : remaining;
  }

  private static String required(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return safe(value, name);
  }

  private static String optional(String value, String name) {
    return value == null || value.isBlank() ? null : safe(value, name);
  }

  private static String safe(String value, String name) {
    if (value.length() > 128
        || value.chars().anyMatch(character -> character < 0x20 || character == 0x7f)) {
      throw new IllegalArgumentException(name + " is not safe for propagation");
    }
    return value;
  }
}
