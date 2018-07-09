/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Defines the core scenegraph APIs for the JavaFX UI toolkit
 * (such as layout containers, application lifecycle, shapes,
 * transformations, canvas, input, painting, image handling, and effects),
 * as well as APIs for animation, css, concurrency, geometry, printing, and
 * windowing.
 *
 * @moduleGraph
 * @since 9
 */
module javafx.graphics {
    requires java.desktop;
    requires java.xml;
    requires jdk.unsupported;

    requires transitive javafx.base;

    exports javafx.animation;
    exports javafx.application;
    exports javafx.concurrent;
    exports javafx.css;
    exports javafx.css.converter;
    exports javafx.geometry;
    exports javafx.print;
    exports javafx.scene;
    exports javafx.scene.canvas;
    exports javafx.scene.effect;
    exports javafx.scene.image;
    exports javafx.scene.input;
    exports javafx.scene.layout;
    exports javafx.scene.paint;
    exports javafx.scene.robot;
    exports javafx.scene.shape;
    exports javafx.scene.text;
    exports javafx.scene.transform;
    exports javafx.stage;

    exports com.sun.glass.ui to
        javafx.media,
        javafx.web;
    exports com.sun.glass.utils to
        javafx.media,
        javafx.web;
    exports com.sun.javafx.application to
        java.base,
        javafx.controls,
        javafx.swing,
        javafx.web;
    exports com.sun.javafx.css to
        javafx.controls;
    exports com.sun.javafx.cursor to
        javafx.swing;
    exports com.sun.javafx.embed to
        javafx.swing;
    exports com.sun.javafx.font to
        javafx.web;
    exports com.sun.javafx.geom to
        javafx.controls,
        javafx.media,
        javafx.swing,
        javafx.web;
    exports com.sun.javafx.geom.transform to
        javafx.controls,
        javafx.media,
        javafx.swing,
        javafx.web;
    exports com.sun.javafx.iio to
        javafx.web;
    exports com.sun.javafx.menu to
        javafx.controls;
    exports com.sun.javafx.scene to
        javafx.controls,
        javafx.media,
        javafx.swing,
        javafx.web;
    exports com.sun.javafx.scene.input to
        javafx.controls,
        javafx.swing,
        javafx.web;
    exports com.sun.javafx.scene.layout to
        javafx.controls,
        javafx.web;
    exports com.sun.javafx.scene.text to
        javafx.controls,
        javafx.web;
    exports com.sun.javafx.scene.traversal to
        javafx.controls,
        javafx.web;
    exports com.sun.javafx.sg.prism to
        javafx.media,
        javafx.swing,
        javafx.web;
    exports com.sun.javafx.stage to
        javafx.controls,
        javafx.swing;
    exports com.sun.javafx.text to
        javafx.web;
    exports com.sun.javafx.tk to
        javafx.controls,
        javafx.media,
        javafx.swing,
        javafx.web;
    exports com.sun.javafx.util to
        javafx.controls,
        javafx.fxml,
        javafx.media,
        javafx.swing,
        javafx.web;
    exports com.sun.prism to
        javafx.media,
        javafx.web;
    exports com.sun.prism.image to
        javafx.web;
    exports com.sun.prism.paint to
        javafx.web;
    exports com.sun.scenario.effect to
        javafx.web;
    exports com.sun.scenario.effect.impl to
        javafx.web;
    exports com.sun.scenario.effect.impl.prism to
        javafx.web;
}
