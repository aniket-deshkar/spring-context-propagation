package io.github.aniketdeshkar.context.web;

import io.github.aniketdeshkar.context.RequestContext;
import java.util.Optional;
import java.util.function.Function;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

public final class ReactorRequestContext {
  private static final Class<RequestContext> KEY = RequestContext.class;

  private ReactorRequestContext() {}

  public static Function<Context, Context> write(RequestContext requestContext) {
    return context -> context.put(KEY, requestContext);
  }

  public static Optional<RequestContext> read(ContextView context) {
    return context.getOrEmpty(KEY);
  }
}
