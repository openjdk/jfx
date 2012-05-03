/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.event.ActionEvent;
import javafx.scene.Cursor;
import javafx.scene.Node;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.css.StyleableProperty;


/**
 * <p>An HTML like label which can be a graphic and/or text which responds to rollovers and clicks.
 * When a hyperlink is clicked/pressed {@link #isVisited} becomes {@code true}.  A Hyperlink behaves
 * just like a {@link Button}.  When a hyperlink is pressed and released
 * a {@link ActionEvent} is sent, and your application can perform some action based on this event.
 * </p>
 *
 * <p>Example:</p>
 * {@code Hyperlink link = new Hyperlink("www.oracle.com"); }
 */
public class Hyperlink extends ButtonBase {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a hyperlink with no label.
     */
    public Hyperlink() {
        initialize();
    }

    /**
     * Create a hyperlink with the specified text as its label.
     *
     * @param text A text string for its label.
     */
    public Hyperlink(String text) {
        super(text);
        initialize();
    }

    /**
     * Create a hyperlink with the specified text and graphic as its label.
     *
     * @param text A text string for its label.
     * @param graphic A graphic for its label
     */
    public Hyperlink(String text, Node graphic) {
        super(text, graphic);
        initialize();
    }

    private void initialize() {
        // Initialize the style class to be 'hyperlink'.
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        // cursor is styleable through css. Calling setCursor
        // makes it look to css like the user set the value and css will not 
        // override. Initializing cursor by calling set on the 
        // StyleableProperty ensures that css will be able to override the value.        
        final StyleableProperty prop = StyleableProperty.getStyleableProperty(cursorProperty());
        prop.set(this, Cursor.HAND);
    }
    
    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    /**
     * Indicates whether this link has already been "visited".
     */
    public final BooleanProperty visitedProperty() {
        if (visited == null) {
            visited = new BooleanPropertyBase() {
                @Override protected void invalidated() {
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_VISITED);
                }

                @Override
                public Object getBean() {
                    return Hyperlink.this;
                }

                @Override
                public String getName() {
                    return "visited";
                }
            };
        }
        return visited;
    }
    private BooleanProperty visited;
    public final void setVisited(boolean value) {
        visitedProperty().set(value);
    }
    public final boolean isVisited() {
        return visited == null ? false : visited.get();
    }

    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Implemented to invoke the {@link ActionEvent} if one is defined. This
     * function will also {@link #setVisited} to true.
     */
    @Override public void fire() {
        // Avoid causing an exception in the case that visited was bound
        if (visited == null || !visited.isBound()) {
            setVisited(true);
        }
        fireEvent(new ActionEvent());
    }

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "hyperlink";
    private static final String PSEUDO_CLASS_VISITED = "visited";

    private static final long VISITED_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("visited");

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public long impl_getPseudoClassState() {
        long mask = super.impl_getPseudoClassState();
        if (isVisited()) mask |= VISITED_PSEUDOCLASS_STATE;
        return mask;
    }

     /**
      * Hyperlink uses HAND as the default value for cursor. 
      * This method provides a way for css to get the correct initial value.
      * @treatAsPrivate implementation detail
      */
    @Deprecated @Override
    protected /*do not make final*/ Cursor impl_cssGetCursorInitialValue() {
        return Cursor.HAND;
    }
    
}
