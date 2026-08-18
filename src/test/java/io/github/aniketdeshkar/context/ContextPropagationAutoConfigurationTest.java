package io.github.aniketdeshkar.context;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aniketdeshkar.context.async.RequestContextTaskDecorator;
import io.github.aniketdeshkar.context.autoconfigure.ContextPropagationAutoConfiguration;
import io.github.aniketdeshkar.context.kafka.KafkaRequestContextPropagator;
import io.github.aniketdeshkar.context.otel.OpenTelemetryRequestContextBridge;
import io.github.aniketdeshkar.context.web.RequestContextRestClientInterceptor;
import io.github.aniketdeshkar.context.web.RequestContextWebClientFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ContextPropagationAutoConfigurationTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ContextPropagationAutoConfiguration.class));

  @Test
  void providesBoundaryAdapters() {
    runner.run(
        context -> {
          assertThat(context).hasSingleBean(RequestContextCodec.class);
          assertThat(context).hasSingleBean(RequestContextTaskDecorator.class);
          assertThat(context).hasSingleBean(RequestContextRestClientInterceptor.class);
          assertThat(context).hasSingleBean(RequestContextWebClientFilter.class);
          assertThat(context).hasSingleBean(KafkaRequestContextPropagator.class);
          assertThat(context).hasSingleBean(OpenTelemetryRequestContextBridge.class);
        });
  }

  @Test
  void canBeDisabled() {
    runner
        .withPropertyValues("spring.context-propagation.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(RequestContextCodec.class));
  }
}
