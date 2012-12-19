/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tools.ant;

import org.apache.tools.ant.types.DataType;

/**
 * Defines javascript callback that can be used to customize user experience.
 *
 * Example:
 * <pre>
 *        &lt;callback name="onGetSplash">customGetSplash&lt;/callback>
 * </pre>
 * This defines callback for event 'onGetSplash'. When event is triggered
 * javascript function customGetSplash will be executed.
 * <p>
 * Note that callback could be defined with javascript code in place (and not just function name):
 * <pre>
 *    &lt;callback name="onLoadHandler">
 *       function () {perfLog(0, "onLoad called");}
 *    &lt;/callback>
 * </pre>
 *
 * @ant.type name="callback" category="javafx"
 */
public final class Callback extends DataType {
    private String name;
    private String cmd;
    private boolean isText;

    public Callback() {
    }

    public Callback(String name, String cmd) {
        setName(name);
        this.cmd = cmd;
    }

    /**
     * Name of the event for callback.
     *
     * @ant.required
     */
    public void setName(String s) {
        name = s;
    }

    public void addText(String cmd) {
        this.cmd = getProject().replaceProperties(cmd.trim());
    }

    public String getCmd() {
        return cmd;
    }

    public String getName() {
        return name;
    }

    public boolean isText() {
        return !cmd.startsWith("function");
    }
}
