/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.MenuButtonBehavior;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.input.MouseEvent;

import com.sun.javafx.scene.control.behavior.SplitMenuButtonBehavior;

/**
 * Default skin implementation for the {@link SplitMenuButton} control.
 *
 * @see SplitMenuButton
 * @since 9
 */
public class SplitMenuButtonSkin extends MenuButtonSkinBase<SplitMenuButton> {

    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private final SplitMenuButtonBehavior behavior;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new SplitMenuButtonSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public SplitMenuButtonSkin(final SplitMenuButton control) {
        super(control);

        // install default input map for the MenuButton-like controls
        this.behavior = new SplitMenuButtonBehavior(control);
//        setInputMap(control, behavior.getInputMap());

        /*
         * The arrow button is the only thing that acts like a MenuButton on
         * this control.
         */
        behaveLikeButton = true;
        // TODO: do we need to consume all mouse events?
        // they only bubble to the skin which consumes them by default
        arrowButton.addEventHandler(MouseEvent.ANY, event -> event.consume());
        arrowButton.setOnMousePressed(e -> {
            getBehavior().mousePressed(e, false);
            e.consume();
        });
        arrowButton.setOnMouseReleased(e -> {
            getBehavior().mouseReleased(e, false);
            e.consume();
        });

        label.setLabelFor(control);
    }



    /* *************************************************************************
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



    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    @Override SplitMenuButtonBehavior getBehavior() {
        return behavior;
    }
}
