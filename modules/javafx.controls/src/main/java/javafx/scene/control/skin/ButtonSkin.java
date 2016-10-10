/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.skin;

import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.skin.Utils;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import com.sun.javafx.scene.control.behavior.ButtonBehavior;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;

/**
 * Default skin implementation for the {@link Button} control.
 *
 * @see Button
 * @since 9
 */
public class ButtonSkin extends LabeledSkinBase<Button> {

    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private KeyCodeCombination defaultAcceleratorKeyCodeCombination;
    private KeyCodeCombination cancelAcceleratorKeyCodeCombination;
    private final BehaviorBase<Button> behavior;



    /***************************************************************************
     *                                                                         *
     * Listeners                                                               *
     *                                                                         *
     **************************************************************************/

    Runnable defaultButtonRunnable = () -> {
        if (getSkinnable().getScene() != null
                && NodeHelper.isTreeVisible(getSkinnable())
                && !getSkinnable().isDisabled()) {
            getSkinnable().fire();
        }
    };

    Runnable cancelButtonRunnable = () -> {
        if (getSkinnable().getScene() != null
                && NodeHelper.isTreeVisible(getSkinnable())
                && !getSkinnable().isDisabled()) {
            getSkinnable().fire();
        }
    };



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new ButtonSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public ButtonSkin(Button control) {
        super(control);

        // install default input map for the Button control
        behavior = new ButtonBehavior<>(control);
//        control.setInputMap(behavior.getInputMap());

        // Register listeners
        registerChangeListener(control.defaultButtonProperty(), o -> setDefaultButton(getSkinnable().isDefaultButton()));
        registerChangeListener(control.cancelButtonProperty(), o -> setCancelButton(getSkinnable().isCancelButton()));
        registerChangeListener(control.focusedProperty(), o -> {
            if (!getSkinnable().isFocused()) {
                ContextMenu cm = getSkinnable().getContextMenu();
                if (cm != null) {
                    if (cm.isShowing()) {
                        cm.hide();
                        Utils.removeMnemonics(cm, getSkinnable().getScene());
                    }
                }
            }
        });
        registerChangeListener(control.parentProperty(), o -> {
            if (getSkinnable().getParent() == null && getSkinnable().getScene() != null) {
                if (getSkinnable().isDefaultButton()) {
                    getSkinnable().getScene().getAccelerators().remove(defaultAcceleratorKeyCodeCombination);
                }
                if (getSkinnable().isCancelButton()) {
                    getSkinnable().getScene().getAccelerators().remove(cancelAcceleratorKeyCodeCombination);
                }
            }
        });

        // set visuals
        if (getSkinnable().isDefaultButton()) {
            /*
            ** were we already the defaultButton, before the listener was added?
            ** don't laugh, it can happen....
            */
            setDefaultButton(true);
        }

        if (getSkinnable().isCancelButton()) {
            /*
            ** were we already the defaultButton, before the listener was added?
            ** don't laugh, it can happen....
            */
            setCancelButton(true);
        }
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }



    /***************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private void setDefaultButton(boolean value) {
        Scene scene = getSkinnable().getScene();
        if (scene != null) {
            KeyCode acceleratorCode = KeyCode.ENTER;
            defaultAcceleratorKeyCodeCombination = new KeyCodeCombination(acceleratorCode);

            Runnable oldDefault = scene.getAccelerators().get(defaultAcceleratorKeyCodeCombination);
            if (!value) {
                /**
                 * first check of there's a default button already
                 */
                if (defaultButtonRunnable.equals(oldDefault)) {
                    /**
                     * is it us?
                     */
                    scene.getAccelerators().remove(defaultAcceleratorKeyCodeCombination);
                }
            }
            else {
                if (!defaultButtonRunnable.equals(oldDefault)) {
                    scene.getAccelerators().remove(defaultAcceleratorKeyCodeCombination);
                    scene.getAccelerators().put(defaultAcceleratorKeyCodeCombination, defaultButtonRunnable);
                }
            }
        }
    }

    private void setCancelButton(boolean value) {
        Scene scene = getSkinnable().getScene();
        if (scene != null) {
            KeyCode acceleratorCode = KeyCode.ESCAPE;
            cancelAcceleratorKeyCodeCombination = new KeyCodeCombination(acceleratorCode);

            Runnable oldCancel = scene.getAccelerators().get(cancelAcceleratorKeyCodeCombination);
            if (!value) {
                /**
                 * first check of there's a cancel button already
                 */
                if (cancelButtonRunnable.equals(oldCancel)) {
                    /**
                     * is it us?
                     */
                    scene.getAccelerators().remove(cancelAcceleratorKeyCodeCombination);
                }
            }
            else {
                if (!cancelButtonRunnable.equals(oldCancel)) {
                    scene.getAccelerators().remove(cancelAcceleratorKeyCodeCombination);
                    scene.getAccelerators().put(cancelAcceleratorKeyCodeCombination, cancelButtonRunnable);
                }
            }
        }
    }
}
