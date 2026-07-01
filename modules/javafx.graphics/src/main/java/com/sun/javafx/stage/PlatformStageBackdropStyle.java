/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.stage;

import javafx.stage.StageBackdropStyle;
import java.util.Map;

/**
 * A PlatformStageBackdropStyle tracks the name and an optional map detailing
 * the options available for this backdrop style.
 */
public final class PlatformStageBackdropStyle implements StageBackdropStyle {
    public String name;
    public Map<String, Class<?>> options;

    public PlatformStageBackdropStyle(String name) {
        this.name = name;
        this.options = Map.of();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Class<?>> getAvailableOptions() {
        return options;
    }

    public void setAvailableOptions(Map<String, Class<?>> o) {
        options = o;
    }

    @Override
    public String toString() {
        String klassName = getClass().getName();
        String simpleName = klassName.substring(klassName.lastIndexOf('.')+1);
        StringBuilder sbuf = new StringBuilder(simpleName);
        sbuf.append("[name=");
        sbuf.append(getName());
        sbuf.append("]");
        return sbuf.toString();
    }
}
