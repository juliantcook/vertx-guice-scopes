package io.github.juliantcook.vertx.guicescopes;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

public class VertxGuiceScopesModule extends AbstractModule {

    private final VerticleScopeImpl verticleScope = new VerticleScopeImpl();

    @Override
    protected void configure() {
        bindScope(VerticleScoped.class, verticleScope);
        bindInterceptor(
                Matchers.annotatedWith(ScopedVerticle.class),
                Matchers.any(),
                new ScopedVerticleInterceptor(verticleScope));
    }
}
