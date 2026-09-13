/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/AppFramework
package com.oracle.demo.richtext.util;

import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleButton;

/**
 * An AbstractAction equivalent for FX, using method references.
 * <p>
 * Usage:
 * <pre>
 *    public final FxAction backAction = new FxAction(this::actionBack);
 * </pre>
 *
 * @author Andy Goryachev
 */
public class FxAction implements EventHandler<ActionEvent> {
    private final SimpleBooleanProperty selectedProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty disabledProperty = new SimpleBooleanProperty();
    private Runnable onAction;
    private Consumer<Boolean> onSelected;

    public FxAction(Runnable onAction, Consumer<Boolean> onSelected, boolean enabled) {
        this.onAction = onAction;
        this.onSelected = onSelected;
        setEnabled(enabled);

        if (onSelected != null) {
            selectedProperty.addListener((src, prev, cur) -> fireSelected(cur));
        }
    }

    public FxAction(Runnable onAction, Consumer<Boolean> onSelected) {
        this(onAction, onSelected, true);
    }

    public FxAction(Runnable onAction, boolean enabled) {
        this(onAction, null, enabled);
    }

    public FxAction(Runnable onAction) {
        this.onAction = onAction;
    }

    public FxAction() {
    }

    public void setOnAction(Runnable r) {
        onAction = r;
    }

    protected final void invokeAction() {
        if (onAction != null) {
            try {
                onAction.run();
            } catch (Throwable e) {
                //log.error(e);
                e.printStackTrace();
            }
        }
    }

    public void attach(ButtonBase b) {
        b.setOnAction(this);
        b.disableProperty().bind(disabledProperty());

        if (b instanceof ToggleButton) {
            ((ToggleButton)b).selectedProperty().bindBidirectional(selectedProperty());
        }
    }

    public void attach(MenuItem m) {
        m.setOnAction(this);
        m.disableProperty().bind(disabledProperty());

        if (m instanceof CheckMenuItem) {
            ((CheckMenuItem)m).selectedProperty().bindBidirectional(selectedProperty());
        } else if (m instanceof RadioMenuItem) {
            ((RadioMenuItem)m).selectedProperty().bindBidirectional(selectedProperty());
        }
    }

    public final BooleanProperty selectedProperty() {
        return selectedProperty;
    }

    public final boolean isSelected() {
        return selectedProperty.get();
    }

    public final void setSelected(boolean on, boolean fire) {
        if (selectedProperty.get() != on) {
            selectedProperty.set(on);
            if (fire) {
                fire();
            }
        }
    }

    public final BooleanProperty disabledProperty() {
        return disabledProperty;
    }

    public final boolean isDisabled() {
        return disabledProperty.get();
    }

    public final void setDisabled(boolean on) {
        disabledProperty.set(on);
    }

    public final boolean isEnabled() {
        return !isDisabled();
    }

    public final void setEnabled(boolean on) {
        disabledProperty.set(!on);
    }

    public final void enable() {
        setEnabled(true);
    }

    public final void disable() {
        setEnabled(false);
    }

    /** fire onAction handler only if this action is enabled */
    public void fire() {
        if (isEnabled()) {
            handle(null);
        }
    }

    /** execute an action regardless of whether its enabled or not */
    public void execute() {
        try {
            invokeAction();
        } catch (Throwable e) {
            //log.error(e);
            e.printStackTrace();
        }
    }

    protected void fireSelected(boolean on) {
        try {
            onSelected.accept(on);
        } catch (Throwable e) {
            //log.error(e);
            e.printStackTrace();
        }
    }

    /** override to obtain the ActionEvent */
    @Override
    public void handle(ActionEvent ev) {
        if (isEnabled()) {
            if (ev != null) {
                if (ev.getSource() instanceof Menu) {
                    if (ev.getSource() != ev.getTarget()) {
                        // selection of a cascading child menu triggers action event for the parent
                        // for some unknown reason.  ignore this.
                        return;
                    }
                }

                ev.consume();
            }

            execute();

            // close popup menu, if applicable
            if (ev != null) {
                Object src = ev.getSource();
                if (src instanceof Menu) {
                    ContextMenu p = ((Menu)src).getParentPopup();
                    if (p != null) {
                        p.hide();
                    }
                }
            }
        }
    }
}