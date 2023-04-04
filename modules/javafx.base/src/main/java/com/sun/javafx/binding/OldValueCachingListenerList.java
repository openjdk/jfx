package com.sun.javafx.binding;

public class OldValueCachingListenerList extends ListenerList implements OldValueStore<Object> {
    private Object value;

    public OldValueCachingListenerList(Object listener1, Object listener2) {
        super(listener1, listener2);
    }

    @Override
    public Object getOldValue() {
        return value;
    }

    @Override
    public void putOldValue(Object value) {
        this.value = value;
    }
}
