/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.application;

import com.sun.javafx.tk.Toolkit;
import javafx.application.Theme;
import java.lang.reflect.Constructor;
import java.util.Map;

class ThemeUtil {

    static Theme tryLoad(String uri) {
        if (uri == null || uri.length() <= 6) {
            return null;
        }

        if ("theme:".equalsIgnoreCase(uri.substring(0, 6))) {
            return newInstance(uri.substring(6));
        }

        return null;
    }

    private static Theme newInstance(String className) {
        Class<?> themeClass = loadClass(className);

        if (!Theme.class.isAssignableFrom(themeClass)) {
            throw new RuntimeException(className + " cannot be loaded because it does not implement " + Theme.class.getName());
        }

        Constructor<?> constructor;

        try {
            constructor = themeClass.getConstructor(Map.class);
        } catch (NoSuchMethodException ex) {
            try {
                constructor = themeClass.getConstructor();
            } catch (NoSuchMethodException ex2) {
                throw new RuntimeException(
                    className + " must have a no-arg constructor or a single-argument constructor accepting a java.util.Map");
            }
        }

        try {
            if (constructor.getParameterCount() == 1) {
                return (Theme)constructor.newInstance(Toolkit.getToolkit().getPlatformThemeProperties());
            }

            return (Theme)constructor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Unable to instantiate " + className, ex);
        }
    }

    private static Class<?> loadClass(String className) {
        try {
            ClassLoader classLoader = ThemeUtil.class.getClassLoader();
            return Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException ex) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader != null) {
                try {
                    return Class.forName(className, true, classLoader);
                } catch (ClassNotFoundException ignored) {
                    throw new RuntimeException(ex);
                }
            }

            throw new RuntimeException(ex);
        }
    }

}
