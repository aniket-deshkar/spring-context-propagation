package io.github.aniketdeshkar.context;

import java.util.Map;
import java.util.Optional;
import org.slf4j.MDC;

public final class RequestContextHolder {
  private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

  private RequestContextHolder() {}

  public static Optional<RequestContext> current() {
    Frame frame = CURRENT.get();
    return frame == null ? Optional.empty() : Optional.of(frame.context);
  }

  public static RequestContext requireCurrent() {
    return current().orElseThrow(() -> new IllegalStateException("no RequestContext is active"));
  }

  public static Scope open(RequestContext context) {
    if (context == null) {
      throw new IllegalArgumentException("context must not be null");
    }
    Frame previous = CURRENT.get();
    Frame installed = new Frame(context, previous, MDC.getCopyOfContextMap());
    CURRENT.set(installed);
    applyMdc(context);
    return new Scope(installed, Thread.currentThread());
  }

  private static void applyMdc(RequestContext context) {
    MDC.put("correlationId", context.correlationId());
    MDC.put("requestId", context.requestId());
    putOrRemove("tenantId", context.tenantId());
    putOrRemove("userId", context.userId());
  }

  private static void putOrRemove(String key, String value) {
    if (value == null) {
      MDC.remove(key);
    } else {
      MDC.put(key, value);
    }
  }

  private static void restoreMdc(Map<String, String> values) {
    if (values == null) {
      MDC.clear();
    } else {
      MDC.setContextMap(values);
    }
  }

  private record Frame(RequestContext context, Frame previous, Map<String, String> previousMdc) {}

  public static final class Scope implements AutoCloseable {
    private final Frame installed;
    private final Thread owner;
    private boolean closed;

    private Scope(Frame installed, Thread owner) {
      this.installed = installed;
      this.owner = owner;
    }

    @Override
    public void close() {
      if (closed) {
        return;
      }
      if (Thread.currentThread() != owner) {
        throw new IllegalStateException("RequestContext scope must close on its owner thread");
      }
      if (CURRENT.get() != installed) {
        throw new IllegalStateException("RequestContext scopes must close in LIFO order");
      }
      if (installed.previous == null) {
        CURRENT.remove();
      } else {
        CURRENT.set(installed.previous);
      }
      restoreMdc(installed.previousMdc);
      closed = true;
    }
  }
}
