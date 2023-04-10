package com.sun.javafx.binding;

public class OldValueCachingListenerList<T> extends ListenerList {
    private T value;

    public OldValueCachingListenerList(Object listener1, Object listener2) {
        super(listener1, listener2);
    }

    public T getLatestValue() {
        return value;
    }

    public void putLatestValue(T value) {
        this.value = value;
    }
}
