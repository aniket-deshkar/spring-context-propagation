package io.github.aniketdeshkar.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RequestContextCodecTest {
  @Test
  void roundTripsAllSupportedFields() {
    RequestContextCodec codec = new RequestContextCodec(() -> "generated");
    RequestContext original = RequestContextHolderTest.context("codec");
    assertThat(codec.decode(codec.encode(original))).isEqualTo(original);
  }

  @Test
  void generatesMissingRequiredIdentifiers() {
    java.util.concurrent.atomic.AtomicInteger ids = new java.util.concurrent.atomic.AtomicInteger();
    RequestContext decoded =
        new RequestContextCodec(() -> "generated-" + ids.incrementAndGet()).decode(Map.of());
    assertThat(decoded.correlationId()).isEqualTo("generated-1");
    assertThat(decoded.requestId()).isEqualTo("generated-2");
  }

  @Test
  void rejectsInvalidDeadline() {
    assertThatThrownBy(
            () ->
                new RequestContextCodec(() -> "id")
                    .decode(Map.of(RequestContextHeaders.DEADLINE, "tomorrow")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("deadline");
  }

  @Test
  void rejectsHeaderInjectionCharacters() {
    assertThatThrownBy(
            () -> new RequestContext("safe\r\ninjected", "request", null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("safe for propagation");
  }

  @Test
  void deadlineBoundaryIsExpired() {
    Instant now = Instant.parse("2026-08-18T00:00:00Z");
    RequestContext context = new RequestContext("c", "r", null, null, null, now);
    assertThat(context.isExpired(Clock.fixed(now, ZoneOffset.UTC))).isTrue();
    assertThat(context.remaining(Clock.fixed(now, ZoneOffset.UTC))).isZero();
  }
}
