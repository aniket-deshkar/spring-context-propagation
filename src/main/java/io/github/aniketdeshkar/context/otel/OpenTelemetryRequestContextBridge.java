package io.github.aniketdeshkar.context.otel;

import io.github.aniketdeshkar.context.RequestContext;
import io.opentelemetry.api.trace.Span;

public final class OpenTelemetryRequestContextBridge {
  public void enrichCurrentSpan(RequestContext context) {
    Span span = Span.current();
    span.setAttribute("request.correlation_id", context.correlationId());
    span.setAttribute("request.id", context.requestId());
    if (context.tenantId() != null) {
      span.setAttribute("enduser.tenant.id", context.tenantId());
    }
    if (context.userId() != null) {
      span.setAttribute("enduser.id", context.userId());
    }
    if (!context.locale().toLanguageTag().isBlank()) {
      span.setAttribute("request.locale", context.locale().toLanguageTag());
    }
    if (context.deadline() != null) {
      span.setAttribute("request.deadline", context.deadline().toString());
    }
  }
}
