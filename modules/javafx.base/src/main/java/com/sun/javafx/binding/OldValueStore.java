package com.sun.javafx.binding;

public interface OldValueStore<T> {
    T getOldValue();
    void putOldValue(T value);
}
