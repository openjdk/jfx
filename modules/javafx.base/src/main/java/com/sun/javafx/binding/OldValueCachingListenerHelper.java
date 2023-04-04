package com.sun.javafx.binding;

import java.util.Objects;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * Helper to manage a single data field of type {@link Object} to store zero,
 * one or more {@link InvalidationListener}s and {@link ChangeListener}s. This
 * helper helps to minimize the storage requirements for keeping track of these
 * listeners.
 * <p>
 *
 * When there are no listeners, the field will be {@code null}. When there is
 * only a single invalidation listener, the field will contain only that
 * listener (change listeners are wrapped to track old value). When there are more
 * than one listeners, the field will hold a {@link OldValueCachingListenerList}. It
 * is recommend to never inspect this field directly but always use this helper to
 * interact with it.
 * <p>
 *
 * This is a variant of {@link ListenerHelper} which caches the old value. This
 * has means that a single {@link ChangeListener} will require a wrapper and
 * that an extra field is needed within listener list.
 */
public class OldValueCachingListenerHelper {

    /**
     * Adds an invalidation listener.
     *
     * @param <T> the type of values
     * @param data data previously obtained from this helper, can be {@code null}
     * @param observableValue the observable value to which the listeners belong, can be {@code null}
     * @param listener a listener to add, cannot be {@code null}
     * @return an updated data object to store, can be {@code null}
     * @throws NullPointerException when listener is {@code null}
     */
    public static <T> Object addListener(Object data, ObservableValue<T> observableValue, InvalidationListener listener) {
        Objects.requireNonNull(listener);

        observableValue.getValue();  // always validate when adding an invalidation listener (required by tests)

        if (data == null) {
            return listener;
        }

        if (data instanceof OldValueCachingListenerList list) {
            list.add(listener);

            return data;
        }

        if (data instanceof ChangeListenerWrapper<?> wrapper) {
            OldValueCachingListenerList list = new OldValueCachingListenerList(wrapper.listener, listener);

            list.putOldValue(wrapper.value);

            return list;
        }

        return new OldValueCachingListenerList(data, listener);
    }

    /**
     * Adds a change listener.
     *
     * @param <T> the type of values
     * @param data data previously obtained from this helper, can be {@code null}
     * @param observableValue the observable value to which the listeners belong, can be {@code null}
     * @param listener a listener to add, cannot be {@code null}
     * @return an updated data object to store, can be {@code null}
     * @throws NullPointerException when listener is {@code null}
     */
    public static <T> Object addListener(Object data, ObservableValue<T> observableValue, ChangeListener<? super T> listener) {
        Objects.requireNonNull(listener);

        if (data == null) {
            return new ChangeListenerWrapper<>(listener, observableValue.getValue());
        }

        if (data instanceof OldValueCachingListenerList list) {
            if (!list.hasChangeListeners()) {
                list.putOldValue(observableValue.getValue());
            }

            list.add(listener);

            return list;
        }

        if (data instanceof ChangeListenerWrapper<?> wrapper) {
            OldValueCachingListenerList list = new OldValueCachingListenerList(wrapper.listener, listener);

            list.putOldValue(wrapper.value);

            return list;
        }

        OldValueCachingListenerList list = new OldValueCachingListenerList(data, listener);

        list.putOldValue(observableValue.getValue());

        return list;
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

        if (data == null || data.equals(listener) || (data instanceof ChangeListenerWrapper<?> wrapper && wrapper.listener.equals(listener))) {
            return null;
        }

        if (data instanceof OldValueCachingListenerList list) {
            list.remove(listener);

            if (list.size() == 1) {
                Object leftOverListener = list.get(0);

                if (leftOverListener instanceof ChangeListener<?> cl) {
                    return new ChangeListenerWrapper<>(cl, list.getOldValue());
                }

                return leftOverListener;
            }

            if (!list.hasChangeListeners()) {
                list.putOldValue(null);  // clear to avoid references
            }
        }

        return data; // there only was a listener of a different type, or it remained a list, no change
    }

    /**
     * Notifies the listeners managed in the given data of changes. This function
     * returns {@code true} if this notification was not a nested notification,
     * otherwise {@code false}.
     * <p>
     *
     * Note: this function does not return an updated data field, even though the
     * data field may change during this call when listeners are added or removed.
     * This is because the notifier code does not have access to the most recent
     * version of the data field to consolidate these changes correctly.
     * <p>
     *
     * Instead, after each call of this function,
     * {@link #consolidate(Object, boolean)} <b>must</b> be called with the result
     * of this function. This allows for correctly updating the data field when
     * provided with the current version, which was possibly modified during
     * notification.
     *
     * @param <T> the type of the values in the {@link ObservableValue}
     * @param data data previously obtained from this helper, can be {@code null}
     * @param observableValue an {@link ObservableValue}, cannot be {@code null}
     * @return {@code true} if this was not a nested notification, otherwise {@code false}
     */
    public static <T> boolean fireValueChanged(Object data, ObservableValue<T> observableValue) {
        if (data == null) {
            return false;
        }

        OldValueCachingListenerList list = data instanceof OldValueCachingListenerList l ? l : null;
        boolean topLevel = list != null && !list.isLocked();

        if (topLevel) {
            list.setProgress(0);
        }

        @SuppressWarnings("unchecked")
        OldValueStore<T> oldValueStore = data instanceof OldValueStore<?> ? (OldValueStore<T>) data
                : (OldValueStore<T>) DUMMY_STORE;
        T oldValue = oldValueStore.getOldValue();
        int progress = list == null ? 0 : list.getProgress();
        int listenerCount = list != null ? list.size() : 1;

        // when nested, only notify as many listeners as were already notified:
        int max = topLevel ? listenerCount : progress + 1;

        if (list == null) {
            callListener(observableValue, data, oldValueStore, oldValue);
        }
        else {
            for (int i = 0; i < max; i++) {
                Object listener = list.get(i);

                if (listener == null) {
                    continue;
                }

                list.setProgress(i);

                callListener(observableValue, listener, oldValueStore, oldValue);
            }
        }

        return topLevel;
    }

    /**
     * Must be called after {@link #fireValueChanged(Object, ObservableValue)} to
     * update the data field.
     *
     * @param data data previously obtained from this helper, can be {@code null}
     * @param topLevel the return value of {@link #fireValueChanged(Object, ObservableValue)}
     * @return an updated data object to store, can be {@code null}
     */
    public static Object consolidate(Object data, boolean topLevel) {
        if (topLevel && data instanceof OldValueCachingListenerList list) {
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

    private static <T> void callListener(ObservableValue<T> observableValue, Object listener, OldValueStore<T> oldValueStore, T oldValue) {
        try {
            if (listener instanceof InvalidationListener il) {
                il.invalidated(observableValue);
            }
            else {
                @SuppressWarnings("unchecked")
                ChangeListener<T> cl = (ChangeListener<T>) listener;
                T value = observableValue.getValue();  // Required as an earlier listener may have changed the value, and we always need the current value

                // Note 1: old value must be store before calling the listener, as nested loop will need to know what the old value was
                // Note 2: old value should even be stored if it was "equals", as it may be a different reference
                oldValueStore.putOldValue(value);

                if (!Objects.equals(value, oldValue)) {
                    cl.changed(observableValue, oldValue, value);  // Note, don't get old value from the store at this point, it must be the same for all listeners
                }
            }
        }
        catch (Exception e) {
            Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }
    }

    static class ChangeListenerWrapper<T> implements ChangeListener<T>, OldValueStore<Object> {
        private final ChangeListener<T> listener;

        private Object value;

        ChangeListenerWrapper(ChangeListener<T> listener, Object value) {
            this.listener = listener;
            this.value = value;
        }

        @Override
        public Object getOldValue() {
            return value;
        }

        @Override
        public void putOldValue(Object value) {
            this.value = value;
        }

        @Override
        public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
            listener.changed(observable, oldValue, newValue);
        }
    }

    private static final OldValueStore<?> DUMMY_STORE = new OldValueStore<Object>() {

        @Override
        public Object getOldValue() {
            return null;
        }

        @Override
        public void putOldValue(Object value) {
        }

    };
}