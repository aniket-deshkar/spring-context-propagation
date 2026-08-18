# Contributing

Create a focused branch and run `./mvnw verify`. Every boundary adapter change must test successful propagation, failure cleanup, and worker reuse.

Keep `RequestContext` small, immutable, and free of credentials or mutable domain data. Never commit secrets, trace exports, build output, or IDE metadata.
