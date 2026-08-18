package io.github.aniketdeshkar.context.web;

import io.github.aniketdeshkar.context.RequestContextCodec;
import io.github.aniketdeshkar.context.RequestContextHolder;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public final class RequestContextRestClientInterceptor implements ClientHttpRequestInterceptor {
  private final RequestContextCodec codec;

  public RequestContextRestClientInterceptor(RequestContextCodec codec) {
    this.codec = Objects.requireNonNull(codec, "codec");
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    RequestContextHolder.current()
        .ifPresent(
            context ->
                codec
                    .encode(context)
                    .forEach((name, value) -> request.getHeaders().set(name, value)));
    return execution.execute(request, body);
  }
}
