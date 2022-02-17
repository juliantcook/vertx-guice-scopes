package io.github.juliantcook.vertx.guicescopes;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class ScopedVerticleInterceptor implements MethodInterceptor {

    private final VerticleScopeImpl verticleScope;

    ScopedVerticleInterceptor(VerticleScopeImpl verticleScope) {
        this.verticleScope = verticleScope;
    }

    /**
     * Intercept start/stop methods on {@link io.vertx.core.Verticle}.
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (invocation.getMethod().getName().equals("start") && invocation.getArguments().length == 1) {
            verticleScope.enter();
        } else if (invocation.getMethod().getName().equals("stop") && invocation.getArguments().length == 1) {
            verticleScope.exit();
        }
        return invocation.proceed();
    }
}
