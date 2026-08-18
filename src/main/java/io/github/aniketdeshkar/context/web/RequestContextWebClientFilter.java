package io.github.aniketdeshkar.context.web;

import io.github.aniketdeshkar.context.RequestContextCodec;
import io.github.aniketdeshkar.context.RequestContextHolder;
import java.util.Objects;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

public final class RequestContextWebClientFilter implements ExchangeFilterFunction {
  private final RequestContextCodec codec;

  public RequestContextWebClientFilter(RequestContextCodec codec) {
    this.codec = Objects.requireNonNull(codec, "codec");
  }

  @Override
  public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
    java.util.Optional<io.github.aniketdeshkar.context.RequestContext> captured =
        RequestContextHolder.current();
    return Mono.deferContextual(
        reactorContext -> {
          ClientRequest.Builder builder = ClientRequest.from(request);
          ReactorRequestContext.read(reactorContext)
              .or(() -> captured)
              .ifPresent(
                  context ->
                      codec.encode(context).forEach((name, value) -> builder.header(name, value)));
          return next.exchange(builder.build());
        });
  }
}
