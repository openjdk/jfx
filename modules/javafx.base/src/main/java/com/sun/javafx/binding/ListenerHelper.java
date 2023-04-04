package com.sun.javafx.binding;

import java.util.Objects;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * Helper to manage a single data field of type {@link Object} to store zero,
 * one or more {@link InvalidationListener}s and {@link ChangeListener}s. This
 * helper helps to minimize the storage requirements for keeping track of these
 * listeners.<p>
 *
 * When there are no listeners, the field will be {@code null}. When there is
 * only a single listener (of any type), the field will contain only that listener
 * (no wrapper required). When there are more than one listeners, the field will
 * hold a {@link ListenerList}. It is recommend to never inspect this field
 * directly but always use this helper to interact with it.
 */
public class ListenerHelper {

    /**
     * Adds a listener.
     *
     * @param data data previously obtained from this helper, can be {@code null}
     * @param listener a listener to add, cannot be {@code null}
     * @return an updated data object to store, can be {@code null}
     * @throws NullPointerException when listener is {@code null}
     */
    public static Object addListener(Object data, Object listener) {
        Objects.requireNonNull(listener);

        if (data == null) {
            return listener;
        }

        if (data instanceof ListenerList list) {
            list.add(listener);

            return data;
        }

        return new ListenerList(data, listener);
    }

    /**
     * Removes a listener.
     *
     * @param data data previously obtained from this helper, can be {@code null}
     * @param listener a listener to remove, cannot be {@code null}
     * @return an updated data object to store, can be {@code null}
     * @throws NullPointerException when listener is {@code null}
     */
    public static Object removeListener(Object data, Object listener) {
        Objects.requireNonNull(listener);

        if (data == null || data.equals(listener)) {
            return null;
        }

        if (data instanceof ListenerList list) {
            list.remove(listener);

            if (list.size() == 1) {
                return list.get(0);
            }
        }

        return data; // there only was a listener of a different type, or it remained a list, no change
    }

    /**
     * Notifies the listeners managed in the given data of changes. This function
     * returns {@code true} if this notification was not a nested notification,
     * otherwise {@code false}.<p>
     *
     * Note: this function does not return an updated data field, even though the
     * data field may change during this call when listeners are added or removed.
     * This is because the notifier code does not have access to the most recent
     * version of the data field to consolidate these changes correctly.<p>
     *
     * Instead, after each call of this function, {@link #consolidate(Object, boolean)}
     * <b>must</b> be called with the result of this function. This allows for correctly
     * updating the data field when provided with the current version, which was possibly
     * modified during notification.
     *
     * @param <T> the type of the values in the {@link ObservableValue}
     * @param data data previously obtained from this helper, can be {@code null}
     * @param observableValue an {@link ObservableValue}, cannot be {@code null}
     * @param oldValue the previous value before this change occurred, can be {@code null}
     * @return {@code true} if this was not a nested notification, otherwise {@code false}
     */
    public static <T> boolean fireValueChanged(Object data, ObservableValue<T> observableValue, T oldValue) {
        if (data == null) {
            return false;
        }

        ListenerList list = data instanceof ListenerList l ? l : null;
        boolean topLevel = list != null && !list.isLocked();

        if (topLevel) {
            list.setProgress(0);
        }

        int progress = list == null ? 0 : list.getProgress();
        int listenerCount = list != null ? list.size() : 1;

        // when nested, only notify as many listeners as were already notified:
        int max = topLevel ? listenerCount : progress + 1;

        if (list == null) {
            callListener(observableValue, data, oldValue);
        }
        else {
            for (int i = 0; i < max; i++) {
                Object listener = list.get(i);

                if (listener == null) {
                    continue;
                }

                list.setProgress(i);

                callListener(observableValue, listener, oldValue);
            }
        }

        return topLevel;
    }

    /**
     * Must be called after {@link #fireValueChanged(Object, ObservableValue, Object)}
     * to update the data field.
     *
     * @param data data previously obtained from this helper, can be {@code null}
     * @param topLevel the return value of {@link #fireValueChanged(Object, ObservableValue, Object)}
     * @return an updated data object to store, can be {@code null}
     */
    public static Object consolidate(Object data, boolean topLevel) {
        if (topLevel && data instanceof ListenerList list) {
            list.unlock();

            if (list.size() == 1) {
                return list.get(0);
            }
            if (list.size() == 0) {
                return null;
            }
        }

        return data;
    }

    private static <T> void callListener(ObservableValue<T> observableValue, Object listener, T oldValue) {
        try {
            if (listener instanceof InvalidationListener il) {
                il.invalidated(observableValue);
            }
            else {
                T newValue = observableValue.getValue();

                if (!Objects.equals(oldValue, newValue)) {
                    @SuppressWarnings("unchecked")
                    ChangeListener<T> cl = (ChangeListener<T>) listener;

                    cl.changed(observableValue, oldValue, newValue);
                }
            }
        }
        catch (Exception e) {
            Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }
    }
}