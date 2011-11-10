/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.control;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertTrue;

/**
 */
@RunWith(Parameterized.class)
public class FireButtonBaseTest {
    @SuppressWarnings("rawtypes")
    @Parameterized.Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][]{
                {Button.class},
                {CheckBox.class},
                {Hyperlink.class},
                {RadioButton.class},
                {MenuButton.class},
                {SplitMenuButton.class},
                {ToggleButton.class}
        });
    }

    private ButtonBase btn;
    private Class type;

    public FireButtonBaseTest(Class type) {
        this.type = type;
    }

    @Before public void setup() throws Exception {
        btn = (ButtonBase) type.newInstance();
    }

    @Test public void onActionCalledWhenButtonIsFired() {
        final EventHandlerStub handler = new EventHandlerStub();
        btn.setOnAction(handler);
        btn.fire();
        assertTrue(handler.called);
    }

    @Test public void onActionCalledWhenNullWhenButtonIsFiredIsNoOp() {
        btn.fire(); // should throw no exceptions, if it does, the test fails
    }

    public static final class EventHandlerStub implements EventHandler<ActionEvent> {
        boolean called = false;
        @Override public void handle(ActionEvent event) {
            called = true;
        }
    };
}
