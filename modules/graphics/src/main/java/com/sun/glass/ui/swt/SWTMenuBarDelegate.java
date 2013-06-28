/*
 * Copyright (c) 2012, 2013, Oracle  and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

import com.sun.glass.ui.delegate.MenuBarDelegate;
import com.sun.glass.ui.delegate.MenuDelegate;

final class SWTMenuBarDelegate implements MenuBarDelegate {
    Menu menuBar;

    public boolean createMenuBar() {
        return (menuBar = Display.getDefault().getMenuBar()) != null;
    }

    //TODO - work around for possible bug in Quantum?
    //TODO = add 0, 1, 2, 3 => remove 3, 2, 1, leaving 0
    //TODO - for that point on, indexing starts from one
    boolean firstRemove = true;
    
    public boolean insert(MenuDelegate menu, int pos) {
        //System.out.println("INSERT: " + pos);
        if (!firstRemove) --pos;
        if (menuBar == null) return false;
        ((SWTMenuDelegate)menu).item = new org.eclipse.swt.widgets.MenuItem(menuBar, SWT.CASCADE, pos);
        ((SWTMenuDelegate)menu).setTitle(((SWTMenuDelegate)menu).title);
        ((SWTMenuDelegate)menu).setEnabled(((SWTMenuDelegate)menu).enabled);
        ((SWTMenuDelegate)menu).item.setMenu(((SWTMenuDelegate)menu).menu);
        return true;
    }

    public boolean remove(MenuDelegate menu, int pos) {
        //System.out.println("REMOVE: " + pos);
        if (!firstRemove) --pos;
        if (0 <= pos && pos < menuBar.getItemCount()) {
            menuBar.getItem(pos).dispose();
        }
        if (firstRemove && pos == 1) {
            --pos;
            firstRemove = false;
            //System.out.println("REMOVE (work around): " + pos);
            menuBar.getItem(pos).dispose();
        }
        return true;
    }

    public long getNativeMenu() {
        //TODO - return menu bar handle from the operating system
        return 1L;
    }
}
