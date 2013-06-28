/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import com.sun.javafx.scene.control.behavior.PasswordFieldBehavior;

/**
 * Password field skin.
 */
public class PasswordFieldSkin extends TextFieldSkin {
    public static final char BULLET = '\u2022';

    public PasswordFieldSkin(PasswordField passwordField) {
        super(passwordField, new PasswordFieldBehavior(passwordField));
    }

    @Override protected String maskText(String txt) {
        TextField textField = getSkinnable();

        int n = textField.getLength();
        StringBuilder passwordBuilder = new StringBuilder(n);
        for (int i=0; i<n; i++) {
            passwordBuilder.append(BULLET);
        }

        return passwordBuilder.toString();
    }
}
