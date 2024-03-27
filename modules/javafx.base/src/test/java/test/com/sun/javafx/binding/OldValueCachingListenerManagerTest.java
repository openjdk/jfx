package test.com.sun.javafx.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sun.javafx.binding.OldValueCachingListenerManager;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class OldValueCachingListenerManagerTest implements ObservableValue<String> {
    private final List<String> notifications = new ArrayList<>();

    private final ChangeListener<String> cl1 = (obs, o, n) -> notifications.add("CL1: " + o + " -> " + n);
    private final ChangeListener<String> cl2 = (obs, o, n) -> notifications.add("CL2: " + o + " -> " + n);

    private Object data;
    private String value = "A";

    private final OldValueCachingListenerManager<String, OldValueCachingListenerManagerTest> helper = new OldValueCachingListenerManager<>() {
        @Override
        protected Object getData(OldValueCachingListenerManagerTest instance) {
            return data;
        }

        @Override
        protected void setData(OldValueCachingListenerManagerTest instance, Object data) {
            OldValueCachingListenerManagerTest.this.data = data;
        }
    };

    @Test
    void should() {
        helper.addListener(this, cl1);

        assertNotNull(data);

        value = "B";

        helper.fireValueChanged(this);

        assertEquals(List.of("CL1: A -> B"), notifications);

        helper.addListener(this, cl2);

        notifications.clear();
        value = "C";

        helper.fireValueChanged(this);

        assertEquals(List.of("CL1: B -> C", "CL2: B -> C"), notifications);

        helper.removeListener(this, cl1);

        notifications.clear();
        value = "D";

        helper.fireValueChanged(this);

        assertEquals(List.of("CL2: C -> D"), notifications);

        notifications.clear();
        value = "E";

        helper.fireValueChanged(this);

        assertEquals(List.of("CL2: D -> E"), notifications);
    }

    @Override
    public void addListener(InvalidationListener listener) {
    }

    @Override
    public void removeListener(InvalidationListener listener) {
    }

    @Override
    public void addListener(ChangeListener<? super String> listener) {
    }

    @Override
    public void removeListener(ChangeListener<? super String> listener) {
    }

    @Override
    public String getValue() {
        return value;
    }
}
