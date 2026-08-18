package io.github.aniketdeshkar.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.aniketdeshkar.context.kafka.KafkaRequestContextPropagator;
import io.github.aniketdeshkar.context.web.ReactorRequestContext;
import io.github.aniketdeshkar.context.web.RequestContextRestClientInterceptor;
import io.github.aniketdeshkar.context.web.RequestContextServletFilter;
import io.github.aniketdeshkar.context.web.RequestContextWebClientFilter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

@SuppressWarnings("PMD.UnusedLocalVariable")
class BoundaryPropagationTest {
  private final RequestContextCodec codec = new RequestContextCodec(() -> "generated-id");

  @AfterEach
  void clean() {
    assertThat(RequestContextHolder.current()).isEmpty();
  }

  @Test
  void servletFilterInstallsAndAlwaysCleansContext() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(RequestContextHeaders.CORRELATION_ID, "correlation-http");
    request.addHeader(RequestContextHeaders.REQUEST_ID, "request-http");
    request.addHeader(RequestContextHeaders.TENANT_ID, "tenant-http");
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RequestContextServletFilter(codec)
        .doFilter(
            request,
            response,
            (ignoredRequest, ignoredResponse) ->
                assertThat(RequestContextHolder.requireCurrent().tenantId())
                    .isEqualTo("tenant-http"));

    assertThat(response.getHeader(RequestContextHeaders.CORRELATION_ID))
        .isEqualTo("correlation-http");
  }

  @Test
  void servletFilterCleansContextAfterFailure() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    assertThatThrownBy(
            () ->
                new RequestContextServletFilter(codec)
                    .doFilter(
                        request,
                        response,
                        (ignoredRequest, ignoredResponse) -> {
                          throw new IllegalStateException("boom");
                        }))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("boom");
  }

  @Test
  void restClientInterceptorInjectsCurrentContext() throws Exception {
    MockClientHttpRequest request =
        new MockClientHttpRequest(HttpMethod.GET, java.net.URI.create("https://example.test"));
    try (var scope = RequestContextHolder.open(RequestContextHolderTest.context("rest"))) {
      new RequestContextRestClientInterceptor(codec)
          .intercept(
              request,
              new byte[0],
              (outgoing, body) -> new MockClientHttpResponse(new byte[0], HttpStatus.OK));
    }
    assertThat(request.getHeaders().getFirst(RequestContextHeaders.TENANT_ID))
        .isEqualTo("tenant-rest");
  }

  @Test
  void webClientFilterReadsContextAtSubscription() {
    AtomicReference<ClientRequest> sent = new AtomicReference<>();
    RequestContextWebClientFilter filter = new RequestContextWebClientFilter(codec);
    Mono<ClientResponse> exchange;
    try (var scope = RequestContextHolder.open(RequestContextHolderTest.context("webclient"))) {
      exchange =
          filter.filter(
              ClientRequest.create(HttpMethod.GET, java.net.URI.create("https://example.test"))
                  .build(),
              request -> {
                sent.set(request);
                return Mono.just(ClientResponse.create(HttpStatus.OK).build());
              });
      exchange.block();
    }
    assertThat(sent.get().headers().getFirst(RequestContextHeaders.USER_ID))
        .isEqualTo("user-webclient");
  }

  @Test
  void webClientFilterReadsExplicitReactorContext() {
    AtomicReference<ClientRequest> sent = new AtomicReference<>();
    RequestContextWebClientFilter filter = new RequestContextWebClientFilter(codec);
    filter
        .filter(
            ClientRequest.create(HttpMethod.GET, java.net.URI.create("https://example.test"))
                .build(),
            request -> {
              sent.set(request);
              return Mono.just(ClientResponse.create(HttpStatus.OK).build());
            })
        .contextWrite(ReactorRequestContext.write(RequestContextHolderTest.context("reactive")))
        .block();

    assertThat(sent.get().headers().getFirst(RequestContextHeaders.TENANT_ID))
        .isEqualTo("tenant-reactive");
  }

  @Test
  void kafkaHeadersRoundTripAndConsumerScopeCleansUp() {
    KafkaRequestContextPropagator propagator = new KafkaRequestContextPropagator(codec);
    ProducerRecord<String, String> producer = new ProducerRecord<>("events", "key", "value");
    try (var scope = RequestContextHolder.open(RequestContextHolderTest.context("kafka"))) {
      propagator.inject(producer);
    }
    ConsumerRecord<String, String> consumer = new ConsumerRecord<>("events", 0, 1, "key", "value");
    producer.headers().forEach(header -> consumer.headers().add(header));

    try (var scope = propagator.open(consumer)) {
      assertThat(RequestContextHolder.requireCurrent().correlationId())
          .isEqualTo("correlation-kafka");
    }
    assertThat(
            new String(
                producer.headers().lastHeader(RequestContextHeaders.REQUEST_ID).value(),
                StandardCharsets.UTF_8))
        .isEqualTo("request-kafka");
  }
}
