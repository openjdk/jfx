package com.sun.javafx.util;

import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;

/**
 * A module-wide Cleaner utility for registering cleanup actions on objects
 * that become phantom-reachable. This class maintains a single shared
 * {@link Cleaner} instance for the module, avoiding multiple daemon threads.
 * <p>
 * Usage example:
 * <pre>
 *     FXCleaner.register(resource, () -> resource.dispose());
 * </pre>
 */
public class FXCleaner {
    private static final Cleaner CLEANER = Cleaner.create();

    /**
     * Registers a cleanup action to be run when {@code obj} becomes
     * phantom-reachable.
     *
     * @param obj the object to monitor, cannot be {@code null}
     * @param action the cleanup action to run, cannot be {@code null}
     * @return a {@link Cleanable} that can be used to cancel the cleanup, never {@code null}
     * @throws NullPointerException when any argument is {@code null}
     */
    public static Cleanable register(Object obj, Runnable action) {
        return CLEANER.register(obj, action);
    }
}
