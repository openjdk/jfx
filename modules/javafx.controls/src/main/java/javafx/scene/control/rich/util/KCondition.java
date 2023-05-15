/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.control.rich.util;

/**
 * Condition used to build input key mappings.
 * <p>
 * The KCondition values are used as keys in a hash table, so when the platform sends a key event with multiple
 * modifiers, some modifiers are dropped in order to make the {@link KeyBinding2} -> function possible.
 * The mapping is as follows:
 * <pre>
 * KCondition    Mac         Windows/Linux
 * ALT           OPTION      ALT
 * COMMAND       COMMAND     (ignored)
 * CTRL          CTRL        CTRL
 * META          COMMAND     META
 * OPTION        OPTION      (ignored)
 * SHIFT         SHIFT       SHIFT
 * SHORTCUT      COMMAND     CTRL
 * WINDOWS       (ignored)   META
 * </pre>
 */
public enum KCondition {
    // modifier keys
    /** ALT modifier, mapped to OPTION on Mac, ALT on Windows/Linux */
    ALT,
    /** COMMAND modifier, mapped to COMMAND on Mac only */
    COMMAND,
    /** CTRL modifier */
    CTRL,
    /** META modifier, mapped to COMMAND on Mac, META on Windows/Linux */
    META,
    /** OPTION modifier, mapped to OPTION on Mac only */
    OPTION,
    /** SHIFT modifier */
    SHIFT,
    /** SHORTCUT modifier, mapped to COMMAND on Mac, CTRL on Windows/Linux */
    SHORTCUT,
    /** Windows key modifier (⊞), mapped to WINDOWS on Windows only */
    WINDOWS,

    // event types
    /** a key press event */
    KEY_PRESS,
    /** a key release event */
    KEY_RELEASE,
    /** a key typed event */
    KEY_TYPED,
    /** any key event */
    KEY_ANY,

    // platform specificity
    /** specifies Windows platform */
    FOR_WIN,
    /** specifies non-Windows platform */
    NOT_FOR_WIN,
    /** specifies Mac platform */
    FOR_MAC,
    /** specifies non-Mac platform */
    NOT_FOR_MAC,
}