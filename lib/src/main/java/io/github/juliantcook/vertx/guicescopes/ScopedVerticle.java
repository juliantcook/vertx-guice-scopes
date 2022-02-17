package io.github.juliantcook.vertx.guicescopes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used on {@link io.vertx.core.Verticle}s which have {@link VerticleScoped} dependencies.
 */
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
public @interface ScopedVerticle {
}
