package io.github.juliantcook.vertx.guicescopes;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.OutOfScopeException;
import com.google.inject.ProvisionException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class VerticleScopeTest {

    private final Injector injector = Guice.createInjector(new VertxGuiceScopesModule());

    @Test
    void instancesAreCreatedPerVerticle() {
        var vertx = Vertx.vertx();
        var verticle = injector.getInstance(SomeVerticle.class);
        waitFor(vertx.deployVerticle(verticle));
        assertSame(verticle.someVerticleScopedClass, verticle.someVerticleScopedClass2);

        var verticle2 = injector.getInstance(SomeVerticle.class);
        waitFor(vertx.deployVerticle(verticle2));
        assertNotSame(verticle.someVerticleScopedClass, verticle2.someVerticleScopedClass);
        waitFor(vertx.close());
    }

    @Test
    void instancesCanOnlyBeCreatedInVerticle() {
        var exception = assertThrows(
                ProvisionException.class,
                () -> injector.getInstance(SomeVerticleScopedClass.class));
        assertInstanceOf(OutOfScopeException.class, exception.getCause());
    }

    @Test
    @DisplayName("Verticle scoped classes should be instantiated after the verticle has started")
    void instantiateLazily() {
        var exception = assertThrows(
                ProvisionException.class,
                () -> injector.getInstance(WronglyConfiguredVerticle.class));
        assertInstanceOf(OutOfScopeException.class, exception.getCause());
    }

    @Test
    @DisplayName("Verticle scoped classes are shared with a verticle's worker threads")
    void workerThreads() {
        var vertx = Vertx.vertx();

        var verticle = injector.getInstance(VerticleWithWorkers.class);
        waitFor(vertx.deployVerticle(verticle));
        assertSame(verticle.someVerticleScopedClass1, verticle.someVerticleScopedClass2);

        waitFor(vertx.close());
    }

    private void waitFor(Future<?> future) {
        try {
            future.toCompletionStage().toCompletableFuture().get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}

@VerticleScoped
class SomeVerticleScopedClass {

}

@ScopedVerticle
class SomeVerticle extends AbstractVerticle {

    private final Provider<SomeVerticleScopedClass> someVerticleScopedClassProvider;
    public SomeVerticleScopedClass someVerticleScopedClass;
    public SomeVerticleScopedClass someVerticleScopedClass2;

    @Inject
    SomeVerticle(Provider<SomeVerticleScopedClass> someVerticleScopedClassProvider) {
        this.someVerticleScopedClassProvider = someVerticleScopedClassProvider;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        someVerticleScopedClass = someVerticleScopedClassProvider.get();
        someVerticleScopedClass2 = someVerticleScopedClassProvider.get();
        startPromise.complete();
    }
}

@ScopedVerticle
class WronglyConfiguredVerticle extends AbstractVerticle {
    @Inject
    WronglyConfiguredVerticle(SomeVerticleScopedClass verticleScopedClass) {
        // verticle scoped class is being instantiated too soon, so we should not get here.
    }
}

@ScopedVerticle
class VerticleWithWorkers extends AbstractVerticle {

    private final Provider<SomeVerticleScopedClass> someVerticleScopedClassProvider;
    public SomeVerticleScopedClass someVerticleScopedClass1;
    public SomeVerticleScopedClass someVerticleScopedClass2;

    @Inject
    VerticleWithWorkers(Provider<SomeVerticleScopedClass> someVerticleScopedClassProvider) {
        this.someVerticleScopedClassProvider = someVerticleScopedClassProvider;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        someVerticleScopedClass1 = someVerticleScopedClassProvider.get();
        vertx.executeBlocking(promise -> {
            someVerticleScopedClass2 = someVerticleScopedClassProvider.get();
            promise.complete();
        }).onComplete(v -> startPromise.complete());
    }
}
