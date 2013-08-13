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

package com.sun.javafx.test.objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.Scene;

public class TestGroup extends Group {
    private final String name;

    private final ObjectProperty<Scene> _sceneProperty =
            new SimpleObjectProperty<Scene>() {
                private Scene old_scene;

                @Override
                protected void invalidated() {
                    final Scene new_scene = get();

                    if (old_scene != new_scene) {
                        if (getScene() != new_scene) {
                            if (old_scene != null) {
                                old_scene.setRoot(null);
                            }
                            if (new_scene != null) {
                                new_scene.setRoot(TestGroup.this);
                            }
                        }

                        old_scene = new_scene;
                    }
                }
            };

    public TestGroup() {
        this("GROUP");
    }

    public TestGroup(final String name) {
        this.name = name;
    }

    public void set_scene(final Scene scene) {
        _sceneProperty.set(scene);
    }

    public Scene get_scene() {
        return _sceneProperty.get();
    }

    public ObjectProperty<Scene> _sceneProperty() {
        return _sceneProperty;
    }

    @Override
    public String toString() {
        return name;
    }
}
