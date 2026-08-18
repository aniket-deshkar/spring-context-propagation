package io.github.aniketdeshkar.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aniketdeshkar.context.async.RequestContextTaskDecorator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

@SuppressWarnings("PMD.UnusedLocalVariable")
class RequestContextHolderTest {
  @AfterEach
  void verifyClean() {
    assertThat(RequestContextHolder.current()).isEmpty();
    MDC.clear();
  }

  @Test
  void scopePopulatesMdcAndCleansUp() {
    RequestContext context = context("one");
    try (RequestContextHolder.Scope ignored = RequestContextHolder.open(context)) {
      assertThat(RequestContextHolder.requireCurrent()).isEqualTo(context);
      assertThat(MDC.get("correlationId")).isEqualTo("correlation-one");
      assertThat(MDC.get("tenantId")).isEqualTo("tenant-one");
    }
    assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
  }

  @Test
  void nestedScopeRestoresPriorContextAndMdc() {
    try (RequestContextHolder.Scope outer = RequestContextHolder.open(context("outer"))) {
      try (RequestContextHolder.Scope inner = RequestContextHolder.open(context("inner"))) {
        assertThat(RequestContextHolder.requireCurrent().requestId()).isEqualTo("request-inner");
      }
      assertThat(RequestContextHolder.requireCurrent().requestId()).isEqualTo("request-outer");
      assertThat(MDC.get("requestId")).isEqualTo("request-outer");
    }
  }

  @Test
  void scopesMustCloseInLifoOrder() {
    RequestContextHolder.Scope outer = RequestContextHolder.open(context("outer"));
    RequestContextHolder.Scope inner = RequestContextHolder.open(context("inner"));
    assertThatThrownBy(outer::close)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("LIFO");
    inner.close();
    outer.close();
  }

  @Test
  void executorTaskSeesCapturedRequestAndTraceThenCleansWorker() throws Exception {
    RequestContextTaskDecorator decorator = new RequestContextTaskDecorator();
    ContextKey<String> traceKey = ContextKey.named("test-trace");
    Runnable decorated;
    java.util.concurrent.atomic.AtomicReference<RequestContext> seen =
        new java.util.concurrent.atomic.AtomicReference<>();
    java.util.concurrent.atomic.AtomicReference<String> trace =
        new java.util.concurrent.atomic.AtomicReference<>();
    try (var traceScope = Context.current().with(traceKey, "trace-one").makeCurrent();
        var requestScope = RequestContextHolder.open(context("async"))) {
      decorated =
          decorator.decorate(
              () -> {
                seen.set(RequestContextHolder.requireCurrent());
                trace.set(Context.current().get(traceKey));
              });
    }

    try (var executor = Executors.newSingleThreadExecutor()) {
      executor.submit(decorated).get();
      Future<Boolean> clean = executor.submit(() -> RequestContextHolder.current().isEmpty());
      assertThat(clean.get()).isTrue();
    }
    assertThat(seen.get().tenantId()).isEqualTo("tenant-async");
    assertThat(trace.get()).isEqualTo("trace-one");
  }

  @Test
  void virtualThreadTaskUsesTheSameExplicitLifecycle() throws Exception {
    RequestContextTaskDecorator decorator = new RequestContextTaskDecorator();
    Runnable decorated;
    java.util.concurrent.atomic.AtomicReference<String> seen =
        new java.util.concurrent.atomic.AtomicReference<>();
    try (var scope = RequestContextHolder.open(context("virtual"))) {
      decorated =
          decorator.decorate(() -> seen.set(RequestContextHolder.requireCurrent().correlationId()));
    }
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      executor.submit(decorated).get();
    }
    assertThat(seen.get()).isEqualTo("correlation-virtual");
  }

  static RequestContext context(String suffix) {
    return new RequestContext(
        "correlation-" + suffix,
        "request-" + suffix,
        "tenant-" + suffix,
        "user-" + suffix,
        Locale.CANADA_FRENCH,
        Instant.parse("2030-01-01T00:00:00Z"));
  }
}
