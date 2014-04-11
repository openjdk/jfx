/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.javafx.scene.control.behavior;

import javafx.scene.control.DatePicker;
import javafx.scene.input.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.sun.javafx.scene.control.skin.DatePickerSkin;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.F4;
import static javafx.scene.input.KeyCode.SPACE;
import static javafx.scene.input.KeyCode.UP;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;


public class DatePickerBehavior extends ComboBoxBaseBehavior<LocalDate> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     *
     */
    public DatePickerBehavior(final DatePicker datePicker) {
        super(datePicker, DATE_PICKER_BINDINGS);
    }

    /***************************************************************************
     *                                                                         *
     * Key event handling                                                      *
     *                                                                         *
     **************************************************************************/

    /**
     * Opens the Date Picker Popup
     */
    protected static final String OPEN_ACTION = "Open";

    /**
     * Closes the Date Picker Popup
     */
    protected static final String CLOSE_ACTION = "Close";


    protected static final List<KeyBinding> DATE_PICKER_BINDINGS = new ArrayList<KeyBinding>();
    static {
        DATE_PICKER_BINDINGS.add(new KeyBinding(F4, KEY_RELEASED, "togglePopup"));
        DATE_PICKER_BINDINGS.add(new KeyBinding(UP, "togglePopup").alt());
        DATE_PICKER_BINDINGS.add(new KeyBinding(DOWN, "togglePopup").alt());
    }

    @Override protected void callAction(String name) {
        switch (name) {
          case OPEN_ACTION:
              show(); break;

          case CLOSE_ACTION:
              hide(); break;

          case "togglePopup":
              if (getControl().isShowing()) {
                  hide();
              } else {
                  show();
              }
              break;

          default:
            super.callAction(name);
        }
    }

    @Override public void onAutoHide() {
        // when we click on some non-interactive part of the
        // calendar - we do not want to hide.
        DatePicker datePicker = (DatePicker)getControl();
        DatePickerSkin cpSkin = (DatePickerSkin)datePicker.getSkin();
        cpSkin.syncWithAutoUpdate();
        // if the DatePicker is no longer showing, then invoke the super method
        // to keep its show/hide state in sync.
        if (!datePicker.isShowing()) super.onAutoHide();
    }

}
