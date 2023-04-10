package com.sun.javafx.binding;

import java.util.Objects;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * Manages a single data field of type {@link Object} to store zero,
 * one or more {@link InvalidationListener}s and {@link ChangeListener}s. This
 * helps to minimize the storage requirements for keeping track of these
 * listeners.<p>
 *
 * When there are no listeners, the field will be {@code null}. When there is
 * only a single invalidation listener, the field will contain only that
 * listener (change listeners are wrapped to track old value). When there are more
 * than one listeners, the field will hold a {@link ListenerList}. It is recommended
 * to never inspect this field directly but always use this manager to
 * interact with it.
 *
 * @param <T> the type of the values
 * @param <I> the type of the instance providing listener data
 */
public abstract class ListenerManager<T, I extends ObservableValue<T>> {

    /**
     * Gets the listener data under management.
     *
     * @param instance the instance it is located in, never {@code null}
     * @return the listener data, can be {@code null}
     */
    protected abstract Object getData(I instance);

    /**
     * Sets the listener data under management.
     *
     * @param instance the instance it is located in, never {@code null}
     * @param data the data to set, can be {@code null}
     */
    protected abstract void setData(I instance, Object data);

    /**
     * Adds an invalidation listener.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param listener a listener to add, cannot be {@code null}
     * @throws NullPointerException when listener is {@code null}
     */
    public void addListener(I instance, InvalidationListener listener) {
        Objects.requireNonNull(listener);

        instance.getValue();  // always trigger validation when adding an invalidation listener (required by tests)

        Object data = getData(instance);

        if (data == null) {
            setData(instance, listener);
        }
        else if (data instanceof ListenerList list) {
            list.add(listener);
        }
        else {
            setData(instance, new ListenerList(data, listener));
        }
    }

    /**
     * Adds a change listener.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param listener a listener to add, cannot be {@code null}
     * @throws NullPointerException when listener is {@code null}
     */
    public void addListener(I instance, ChangeListener<? super T> listener) {
        Objects.requireNonNull(listener);

        instance.getValue();  // always trigger validation when adding a change listener (required by tests)

        Object data = getData(instance);

        if (data == null) {
            setData(instance, listener);
        }
        else if (data instanceof ListenerList list) {
            list.add(listener);
        }
        else {
            setData(instance, new ListenerList(data, listener));
        }
    }

    /**
     * Removes a listener.
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param listener a listener to remove, cannot be {@code null}
     * @throws NullPointerException when listener is {@code null}
     */
    public void removeListener(I instance, Object listener) {
        Objects.requireNonNull(listener);

        Object data = getData(instance);

        if (data == null || data.equals(listener)) {
            setData(instance, null);  // TODO not needed when already null
        }
        else if (data instanceof ListenerList list) {
            list.remove(listener);

            if (list.size() == 1) {
                setData(instance, list.get(0));
            }
        }
    }

    /**
     * Notifies the listeners managed in the given instance.<p>
     *
     * @param instance the instance to which the listeners belong, cannot be {@code null}
     * @param oldValue the previous value before this change occurred, can be {@code null}
     */
    public void fireValueChanged(I instance, T oldValue) {
        Object data = getData(instance);

        if (data instanceof ListenerList list) {
            callMultipleListeners(instance, list, oldValue);
        }
        else if (data instanceof InvalidationListener il) {
            callInvalidationListener(instance, il);
        }
        else if (data instanceof ChangeListener) {
            @SuppressWarnings("unchecked")
            ChangeListener<T> cl = (ChangeListener<T>) data;

            callChangeListener(instance, cl, oldValue);
        }
    }

    private void callMultipleListeners(I instance, ListenerList list, T oldValue) {
        boolean topLevel = !list.isLocked();

        // when nested, only notify as many listeners as were already notified:
        int max = topLevel ? list.size() : list.getProgress() + 1;

        int count = list.invalidationListenersSize();
        int maxInvalidations = Math.min(count, max);

        for (int i = 0; i < maxInvalidations; i++) {
            InvalidationListener listener = list.getInvalidationListener(i);

            if (listener == null) {
                continue;
            }

            list.setProgress(i);

            callInvalidationListener(instance, listener);
        }

        if (count < max) {
            max -= count;

            for (int i = 0; i < max; i++) {
                ChangeListener<T> listener = list.getChangeListener(i);

                if (listener == null) {
                    continue;
                }

                T newValue = instance.getValue();  // Required as an earlier listener may have changed the value, and current value is always needed

                if (Objects.equals(newValue, oldValue)) {
                    break;
                }

                list.setProgress(i + count);

                try {
                    listener.changed(instance, oldValue, newValue);  // Old value must be the same for all listeners in this loop
                }
                catch (Exception e) {
                    Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                }
            }
        }

        if (topLevel && list.isLocked()) {
            unlock(instance);
        }
    }

    private void callChangeListener(I instance, ChangeListener<T> changeListener, T oldValue) {
        T newValue = instance.getValue();  // Required as an earlier listener may have changed the value, and current value is always needed

        if (!Objects.equals(newValue, oldValue)) {
            try {
                changeListener.changed(instance, oldValue, newValue);
            }
            catch (Exception e) {
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            }
        }
    }

    private void callInvalidationListener(I instance, InvalidationListener listener) {
        try {
            listener.invalidated(instance);
        }
        catch (Exception e) {
            Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }
    }

    private void unlock(I instance) {
        Object data = getData(instance);

        if (data instanceof ListenerList list) {
            list.unlock();

            // TODO need to do something potentially with oldValue here
            // TODO bugs, we forget to wrap it in ChangeListenerWrapper?
            if (list.size() == 1) {
                setData(instance, list.get(0));
            }
            else if(list.size() == 0) {
                setData(instance, null);
            }
        }
    }
}