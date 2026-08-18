package io.github.aniketdeshkar.context.autoconfigure;

import io.github.aniketdeshkar.context.RequestContextCodec;
import io.github.aniketdeshkar.context.async.RequestContextTaskDecorator;
import io.github.aniketdeshkar.context.kafka.KafkaRequestContextPropagator;
import io.github.aniketdeshkar.context.otel.OpenTelemetryRequestContextBridge;
import io.github.aniketdeshkar.context.web.RequestContextRestClientInterceptor;
import io.github.aniketdeshkar.context.web.RequestContextServletFilter;
import io.github.aniketdeshkar.context.web.RequestContextWebClientFilter;
import java.util.UUID;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(
    prefix = "spring.context-propagation",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(ContextPropagationProperties.class)
public class ContextPropagationAutoConfiguration {
  @Bean
  @ConditionalOnMissingBean
  RequestContextCodec requestContextCodec() {
    return new RequestContextCodec(() -> UUID.randomUUID().toString());
  }

  @Bean
  @ConditionalOnMissingBean
  RequestContextTaskDecorator requestContextTaskDecorator() {
    return new RequestContextTaskDecorator();
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnClass(name = "org.springframework.web.client.RestClient")
  RequestContextRestClientInterceptor requestContextRestClientInterceptor(
      RequestContextCodec codec) {
    return new RequestContextRestClientInterceptor(codec);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
  RequestContextWebClientFilter requestContextWebClientFilter(RequestContextCodec codec) {
    return new RequestContextWebClientFilter(codec);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnClass(name = "org.apache.kafka.clients.producer.ProducerRecord")
  KafkaRequestContextPropagator kafkaRequestContextPropagator(RequestContextCodec codec) {
    return new KafkaRequestContextPropagator(codec);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnClass(name = "io.opentelemetry.api.trace.Span")
  OpenTelemetryRequestContextBridge openTelemetryRequestContextBridge() {
    return new OpenTelemetryRequestContextBridge();
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
  RequestContextServletFilter requestContextServletFilter(RequestContextCodec codec) {
    return new RequestContextServletFilter(codec);
  }
}
