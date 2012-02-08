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

package com.sun.javafx.test.objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Test scene with name and possibility to set scene's window directly through
 * "_window" property. The later is used in property tests.
 */
public class TestScene extends Scene {
    private final String name;

    private final ObjectProperty<Window> _windowProperty =
            new ObjectPropertyBase<Window>() {
                private Window old_window;

                @Override
                protected void invalidated() {
                    final Window new_window = get();

                    if (old_window != new_window) {
                        if (getWindow() != new_window) {
                            if (new_window instanceof Stage) {
                                ((Stage)new_window).setScene(TestScene.this);
                            } else if (old_window instanceof Stage) {
                                ((Stage)old_window).setScene(null);
                            }
                        }

                        old_window = new_window;
                    }
                }

                @Override
                public Object getBean() {
                    return TestScene.this;
                }

                @Override
                public String getName() {
                    return "_window";
                }
            };

    public TestScene(final Parent root) {
        this("SCENE", root);
    }

    public TestScene(final String name, final Parent root) {
        super(root);
        this.name = name;
    }

    public void set_window(final Window window) {
        _windowProperty.set(window);
    }

    public Window get_window() {
        return _windowProperty.get();
    }

    public ObjectProperty<Window> _windowProperty() {
        return _windowProperty;
    }

    @Override
    public String toString() {
        return name;
    }
}
