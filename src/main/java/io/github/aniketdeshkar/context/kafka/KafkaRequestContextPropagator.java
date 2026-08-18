package io.github.aniketdeshkar.context.kafka;

import io.github.aniketdeshkar.context.RequestContext;
import io.github.aniketdeshkar.context.RequestContextCodec;
import io.github.aniketdeshkar.context.RequestContextHolder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

public final class KafkaRequestContextPropagator {
  private final RequestContextCodec codec;

  public KafkaRequestContextPropagator(RequestContextCodec codec) {
    this.codec = Objects.requireNonNull(codec, "codec");
  }

  public <K, V> ProducerRecord<K, V> inject(ProducerRecord<K, V> record) {
    RequestContextHolder.current()
        .ifPresent(
            context ->
                codec
                    .encode(context)
                    .forEach(
                        (name, value) -> {
                          record.headers().remove(name);
                          record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
                        }));
    return record;
  }

  public RequestContext extract(ConsumerRecord<?, ?> record) {
    return codec.decode(read(record.headers()));
  }

  public RequestContextHolder.Scope open(ConsumerRecord<?, ?> record) {
    return RequestContextHolder.open(extract(record));
  }

  private static Map<String, String> read(Headers headers) {
    Map<String, String> values = new LinkedHashMap<>();
    for (Header header : headers) {
      values.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
    }
    return values;
  }
}
