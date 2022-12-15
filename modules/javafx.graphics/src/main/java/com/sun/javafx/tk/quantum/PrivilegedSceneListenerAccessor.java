package com.sun.javafx.tk.quantum;

import java.util.function.Consumer;

import com.sun.javafx.tk.TKSceneListener;

/**
 * Interface that allows access to {@link TKSceneListener} when it is
 * not {@code null}.
 */
public interface PrivilegedSceneListenerAccessor {
    void withSceneListener(Consumer<TKSceneListener> consumer);
}
