package com.sun.javafx.scene.control.behavior;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.Test;
import static org.junit.Assert.*;

public class KeyBindingTest {

    @Test public void getSpecificity() {
        final KeyCode code = KeyCode.ENTER;

        // Expected answer:
        // 1 pt for matching key code
        // 1 pt for matching key event type
        // 1 pt for matching no alt
        // 1 pt for matching no meta
        // 1 pt for matching shift or control
        // 0 pt for the other optional value of control/shift
        //
        // Total = 5.
        //
        int expect = 5;

        KeyBinding uut = new KeyBinding(code, "ShiftEnter").shift().ctrl(OptionalBoolean.ANY);

        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, null,
                null, code, true, false, false, false);

        assertEquals(expect, uut.getSpecificity(null, event)); // Gets 6 (fx 2.2, fx 8)

        uut = new KeyBinding(code, "CtrlEnter").shift(OptionalBoolean.ANY).ctrl();

        event = new KeyEvent(KeyEvent.KEY_PRESSED, null,
                null, code, false, true, false, false);

        assertEquals(expect, uut.getSpecificity(null, event)); // Gets 2 (fx 2.2, fx 8)
    }
} 