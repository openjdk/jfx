/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.notes;

import com.javafx.experiments.dukepad.core.BaseDukeApplication;
import com.javafx.experiments.dukepad.core.DukeApplication;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import java.io.IOException;

/**
 * Notes App
 */
public class NotesApp extends BaseDukeApplication implements BundleActivator {
    private static final Image appIcon = new Image(NotesApp.class.getResource("/images/ico-notes.png").toExternalForm());

    /** Get name of application */
    @Override public String getName() {
        return "Notes";
    }

    /** Create icon instance */
    @Override public Node createHomeIcon() {
        return new ImageView(appIcon);
    }

    /** Create the UI, new UI chould be created each time and not held on to */
    @Override protected Node createUI() {
//        try {
//            return FXMLLoader.load(NotesApp.class.getResource("Notes.fxml"));
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
        // Disable for now
        return null;
    }

    /** Called when app is loaded at platform startup */
    @Override public void start(BundleContext bundleContext) throws Exception {
        // Register application service
        bundleContext.registerService(DukeApplication.class,this,null);
    }

    /** Called when app is unloaded at platform shutdown */
    @Override public void stop(BundleContext bundleContext) throws Exception {}

    @Override public void startApp() {
        super.startApp();
        setFullScreen(false);
    }

    @Override public boolean supportsHalfScreenMode() {
        return true;
    }
}
