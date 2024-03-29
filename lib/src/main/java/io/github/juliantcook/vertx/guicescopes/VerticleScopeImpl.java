package io.github.juliantcook.vertx.guicescopes;

import com.google.common.collect.Maps;
import com.google.inject.*;
import io.vertx.core.Vertx;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

/**
 * Based on code in the Guice docs:
 * https://github.com/google/guice/wiki/CustomScopes
 */
public class VerticleScopeImpl implements Scope {

    private static final Provider<Object> SEEDED_KEY_PROVIDER =
            new Provider<Object>() {
                public Object get() {
                    throw new IllegalStateException("If you got here then it means that" +
                            " your code asked for scoped object which should have been" +
                            " explicitly seeded in this scope by calling" +
                            " SimpleScope.seed(), but was not.");
                }
            };

    private static final String INSTANCES_MAP_CONTEXT_KEY = "_vertx-guice-scoped-instances";

    public void enter() {
        var context = Vertx.currentContext();
        checkState(context.get(INSTANCES_MAP_CONTEXT_KEY) == null, "A scoping block is already in progress");
        context.put(INSTANCES_MAP_CONTEXT_KEY, Maps.newHashMap());
    }

    public void exit() {
        var context = Vertx.currentContext();
        checkState(context.get(INSTANCES_MAP_CONTEXT_KEY) != null, "No scoping block in progress");
        context.remove(INSTANCES_MAP_CONTEXT_KEY);
    }

    public <T> void seed(Key<T> key, T value) {
        Map<Key<?>, Object> scopedObjects = getScopedObjectMap(key);
        checkState(!scopedObjects.containsKey(key), "A value for the key %s was " +
                        "already seeded in this scope. Old value: %s New value: %s", key,
                scopedObjects.get(key), value);
        scopedObjects.put(key, value);
    }

    public <T> void seed(Class<T> clazz, T value) {
        seed(Key.get(clazz), value);
    }

    @Override
    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
        return new Provider<T>() {
            public T get() {
                Map<Key<?>, Object> scopedObjects = getScopedObjectMap(key);

                @SuppressWarnings("unchecked")
                T current = (T) scopedObjects.get(key);
                if (current == null && !scopedObjects.containsKey(key)) {
                    current = unscoped.get();

                    // don't remember proxies; these exist only to serve circular dependencies
                    if (Scopes.isCircularProxy(current)) {
                        return current;
                    }

                    scopedObjects.put(key, current);
                }
                return current;
            }
        };
    }

    private <T> Map<Key<?>, Object> getScopedObjectMap(Key<T> key) {
        return Optional.ofNullable(Vertx.currentContext())
                .map(ctx -> ((Map<Key<?>, Object>) ctx.get(INSTANCES_MAP_CONTEXT_KEY)))
                .orElseThrow(() -> new OutOfScopeException("Cannot access " + key
                        + " outside of a scoping block. " +
                        "@VerticleScoped objects should be instantiated within a @ScopedVerticle class after it has started."));
    }

    /**
     * Returns a provider that always throws exception complaining that the object in question must be seeded before it
     * can be injected.
     *
     * @return typed provider
     */
    @SuppressWarnings({"unchecked"})
    public static <T> Provider<T> seededKeyProvider() {
        return (Provider<T>) SEEDED_KEY_PROVIDER;
    }
}
