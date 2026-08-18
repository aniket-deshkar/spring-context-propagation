package io.github.aniketdeshkar.context.web;

import io.github.aniketdeshkar.context.RequestContext;
import io.github.aniketdeshkar.context.RequestContextCodec;
import io.github.aniketdeshkar.context.RequestContextHeaders;
import io.github.aniketdeshkar.context.RequestContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.web.filter.OncePerRequestFilter;

public final class RequestContextServletFilter extends OncePerRequestFilter {
  private final RequestContextCodec codec;

  public RequestContextServletFilter(RequestContextCodec codec) {
    this.codec = Objects.requireNonNull(codec, "codec");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    final RequestContext context;
    try {
      context = codec.decode(headers(request));
    } catch (IllegalArgumentException exception) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request context");
      return;
    }
    response.setHeader(RequestContextHeaders.CORRELATION_ID, context.correlationId());
    response.setHeader(RequestContextHeaders.REQUEST_ID, context.requestId());
    try (RequestContextHolder.Scope ignored = RequestContextHolder.open(context)) {
      filterChain.doFilter(request, response);
    }
  }

  private static Map<String, String> headers(HttpServletRequest request) {
    Map<String, String> result = new LinkedHashMap<>();
    copy(request, result, RequestContextHeaders.CORRELATION_ID);
    copy(request, result, RequestContextHeaders.REQUEST_ID);
    copy(request, result, RequestContextHeaders.TENANT_ID);
    copy(request, result, RequestContextHeaders.USER_ID);
    copy(request, result, RequestContextHeaders.DEADLINE);
    copy(request, result, RequestContextHeaders.LOCALE);
    return result;
  }

  private static void copy(HttpServletRequest request, Map<String, String> target, String name) {
    String value = request.getHeader(name);
    if (value != null) {
      target.put(name, value);
    }
  }
}
