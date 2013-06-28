/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario;

import java.util.Map;

import com.sun.scenario.animation.AbstractMasterTimer;

/**
 * A simple java accessor to the current Toolkit.
 * This class is ONLY NECESSARY because javafx-anim is separate from javafx-ui-common.
 * When they are combined, we can remove this class.
 */
public abstract class ToolkitAccessor {
    private static ToolkitAccessor instance;

    public static void setInstance(ToolkitAccessor accessor) {
        instance = accessor;
    }

    private static ToolkitAccessor getInstance() {
        if (instance == null) {
            // running in stand alone case without toolkit
            try {
                Class<?> cl = Class
                        .forName("com.sun.scenario.StandaloneAccessor");
                setInstance((ToolkitAccessor) cl.newInstance());
            } catch (Throwable th) {
                // ignore
            }
        }
        return instance;
    }

    public static Map<Object, Object> getContextMap() {
        return getInstance().getContextMapImpl();
    }

    public static AbstractMasterTimer getMasterTimer() {
        return getInstance().getMasterTimerImpl();
    }

    public abstract Map<Object, Object> getContextMapImpl();

    public abstract AbstractMasterTimer getMasterTimerImpl();

}
