package io.github.aniketdeshkar.context.async;

import io.github.aniketdeshkar.context.RequestContext;
import io.github.aniketdeshkar.context.RequestContextHolder;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.Optional;
import org.springframework.core.task.TaskDecorator;

public final class RequestContextTaskDecorator implements TaskDecorator {
  @Override
  public Runnable decorate(Runnable runnable) {
    Optional<RequestContext> requestContext = RequestContextHolder.current();
    Context traceContext = Context.current();
    return () -> {
      try (Scope ignoredTrace = traceContext.makeCurrent()) {
        if (requestContext.isPresent()) {
          try (RequestContextHolder.Scope ignoredRequest =
              RequestContextHolder.open(requestContext.orElseThrow())) {
            runnable.run();
          }
        } else {
          runnable.run();
        }
      }
    };
  }
}
