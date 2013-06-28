/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.beans.design.author;

import com.sun.javafx.beans.design.DisplayItem;

/**
 * <P>A DisplayAction represents a menu item or dialog button - depending on where and how it is
 * used.  It has a 'displayName' and a 'description' so that it can display localized text to the
 * user.  DisplayActions may be enabled or disabled ('enabled' property) to appear grayed-out as
 * menu items or buttons.  A DisplyAction can be 'invoked', and thus execute custom behavior when
 * it is activated from a menu-pick or button-click.</P>
 *
 * <P>DisplayAction extends the DisplayItem interface, thus it includes 'displayName', 'description',
 * 'largeIcon', and 'smallIcon' properties.</P>
 *
 * <P><B>IMPLEMENTED BY THE COMPONENT AUTHOR</B> - This interface is designed to be implemented by
 * the component (bean) author.  The BasicDisplayAction class can be used for convenience.</P>
 *
 * @author Joe Nuxoll
 * @version 1.0
 */
public interface DisplayAction extends DisplayItem {

    public static final DisplayAction[] EMPTY_ARRAY = {};

    /**
     * Returns <B>true</B> if this DisplayAction should be displayed as enabled, or <B>false</B> if
     * it should be disabled.
     *
     * @return <B>true</B> if this DisplayAction should be displayed as enabled, or <B>false</B> if
     *         it should be disabled.
     */
    public boolean isEnabled();

    /**
     * This method is called when a DisplayAction is selected from a context menu or 'clicked' when
     * it is displayed as a button.
     *
     * @return A standard Result object
     */
    public Result invoke();
}
