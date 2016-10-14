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

package javafx.scene;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import javafx.scene.image.Image;
import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.cursor.CursorType;
import com.sun.javafx.cursor.StandardCursorFrame;

/**
 * A class to encapsulate the bitmap representation of the mouse cursor.
 * @since JavaFX 2.0
 */
public abstract class Cursor {
    /**
     * The default cursor type (gets set if no cursor is defined).
     */
    public static final Cursor DEFAULT =
            new StandardCursor("DEFAULT", CursorType.DEFAULT);

    /**
     * The crosshair cursor type.
     */
    public static final Cursor CROSSHAIR =
            new StandardCursor("CROSSHAIR", CursorType.CROSSHAIR);

    /**
     * The text cursor type.
     */
    public static final Cursor TEXT =
            new StandardCursor("TEXT", CursorType.TEXT);

    /**
     * The wait cursor type.
     */
    public static final Cursor WAIT =
            new StandardCursor("WAIT", CursorType.WAIT);

    /**
     * The south-west-resize cursor type.
     */
    public static final Cursor SW_RESIZE =
            new StandardCursor("SW_RESIZE", CursorType.SW_RESIZE);

    /**
     * The south-east-resize cursor type.
     */
    public static final Cursor SE_RESIZE =
            new StandardCursor("SE_RESIZE", CursorType.SE_RESIZE);

    /**
     * The north-west-resize cursor type.
     */
    public static final Cursor NW_RESIZE =
            new StandardCursor("NW_RESIZE", CursorType.NW_RESIZE);

    /**
     * The north-east-resize cursor type.
     */
    public static final Cursor NE_RESIZE =
            new StandardCursor("NE_RESIZE", CursorType.NE_RESIZE);

    /**
     * The north-resize cursor type.
     */
    public static final Cursor N_RESIZE =
            new StandardCursor("N_RESIZE", CursorType.N_RESIZE);

    /**
     * The south-resize cursor type.
     */
    public static final Cursor S_RESIZE =
            new StandardCursor("S_RESIZE", CursorType.S_RESIZE);

    /**
     * The west-resize cursor type.
     */
    public static final Cursor W_RESIZE =
            new StandardCursor("W_RESIZE", CursorType.W_RESIZE);

    /**
     * The east-resize cursor type.
     */
    public static final Cursor E_RESIZE =
            new StandardCursor("E_RESIZE", CursorType.E_RESIZE);

    /**
     * A cursor with a hand which is open
     */
    public static final Cursor OPEN_HAND =
            new StandardCursor("OPEN_HAND", CursorType.OPEN_HAND);

    /**
     * A cursor with a hand that is closed, often used when
     * "grabbing", for example, when panning.
     */
    public static final Cursor CLOSED_HAND =
            new StandardCursor("CLOSED_HAND", CursorType.CLOSED_HAND);

    /**
     * The hand cursor type, resembling a pointing hand, often
     * used to indicate that something can be clicked, such as
     * a hyperlink.
     */
    public static final Cursor HAND =
            new StandardCursor("HAND", CursorType.HAND);

    /**
     * The move cursor type.
     */
    public static final Cursor MOVE =
            new StandardCursor("MOVE", CursorType.MOVE);

    /**
     * The disappear cursor type. This is often used when dragging
     * something, such that when the user releases the mouse, the
     * item will disappear. On Mac, this is used when dragging items
     * off a toolbar or in other such situations.
     */
    public static final Cursor DISAPPEAR =
            new StandardCursor("DISAPPEAR", CursorType.DISAPPEAR);

    /**
     * The horizontal cursor type.
     */
    public static final Cursor H_RESIZE =
            new StandardCursor("H_RESIZE", CursorType.H_RESIZE);

    /**
     * The vertical cursor type.
     */
    public static final Cursor V_RESIZE =
            new StandardCursor("V_RESIZE", CursorType.V_RESIZE);

    /**
     * The none cursor type. On platforms that don't support
     * custom cursors, this will be the same as {@code DEFAULT}.
     */
    public static final Cursor NONE =
            new StandardCursor("NONE", CursorType.NONE);

    private String name = "CUSTOM";

    Cursor() { }
    Cursor(String name) {
        this.name = name;
    }

    abstract CursorFrame getCurrentFrame();

    /**
     * Activates the cursor. Cursor should be activated to make sure
     * that the {@code getCurrentFrame} returns up-to date values.
     */
    void activate() {
        // no activation necessary for standard cursors
    }

    /**
     * Deactivates the cursor. Cursor should be deactivated, when no longer in
     * use to make it collectible by GC.
     */
    void deactivate() {
    }

    /**
     * Returns a string representation for the cursor.
     * @return a string representation for the cursor.
     */
    @Override public String toString() {
        return name;
    }

    // PENDING_DOC_REVIEW
    /**
     * Returns a cursor for the specified identifier. The identifier can be
     * either a name of some standard cursor or a valid URL string. If the
     * identifier names a standard cursor the corresponding cursor is returned.
     * In the case of a URL string, the method returns a new {@code ImageCursor}
     * created for that URL.
     *
     * @param identifier the cursor identifier
     * @return the cursor for the identifier
     * @throws IllegalArgumentException if the cursor identifier is not a
     *      valid URL string nor any standard cursor name
     */
    public static Cursor cursor(final String identifier) {
        if (identifier == null) {
            throw new NullPointerException(
                    "The cursor identifier must not be null");
        }

        if (isUrl(identifier)) {
            return new ImageCursor(new Image(identifier));
        }

        String uName = identifier.toUpperCase(Locale.ROOT);
        if (uName.equals(DEFAULT.name)) {
            return DEFAULT;
        } else if(uName.equals(CROSSHAIR.name)) {
            return CROSSHAIR;
        } else if (uName.equals(TEXT.name)) {
            return TEXT;
        } else if (uName.equals(WAIT.name)) {
            return WAIT;
        } else if (uName.equals(MOVE.name)) {
            return MOVE;
        } else if (uName.equals(SW_RESIZE.name)) {
            return SW_RESIZE;
        } else if (uName.equals(SE_RESIZE.name)) {
            return SE_RESIZE;
        } else if (uName.equals(NW_RESIZE.name)) {
            return NW_RESIZE;
        } else if (uName.equals(NE_RESIZE.name)) {
            return NE_RESIZE;
        } else if (uName.equals(N_RESIZE.name)) {
            return N_RESIZE;
        } else if (uName.equals(S_RESIZE.name)) {
            return S_RESIZE;
        } else if (uName.equals(W_RESIZE.name)) {
            return W_RESIZE;
        } else if (uName.equals(E_RESIZE.name)) {
            return E_RESIZE;
        } else if (uName.equals(OPEN_HAND.name)) {
            return OPEN_HAND;
        } else if (uName.equals(CLOSED_HAND.name)) {
            return CLOSED_HAND;
        } else if (uName.equals(HAND.name)) {
            return HAND;
        } else if (uName.equals(H_RESIZE.name)) {
            return H_RESIZE;
        } else if (uName.equals(V_RESIZE.name)) {
            return V_RESIZE;
        } else if (uName.equals(DISAPPEAR.name)) {
            return DISAPPEAR;
        } else if (uName.equals(NONE.name)) {
            return NONE;
        }

        throw new IllegalArgumentException("Invalid cursor specification");
    }

    private static boolean isUrl(final String identifier) {
        try {
            new URL(identifier);
        } catch (final MalformedURLException e) {
            return false;
        }

        return true;
    }

    private static final class StandardCursor extends Cursor {
        private final CursorFrame singleFrame;

        public StandardCursor(final String name, final CursorType type) {
            super(name);
            singleFrame = new StandardCursorFrame(type);
        }

        @Override
        CursorFrame getCurrentFrame() {
            return singleFrame;
        }
    }
}
