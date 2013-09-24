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

package com.javafx.experiments.dukepad.mediaPlayer;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.File;
import java.io.FileFilter;

/**
 * MediaPlayer UI
 */
public class MediaPlayerUI extends Accordion {
    private static final FileFilter DIR_FILTER = new FileFilter() {
        @Override public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };

    public MediaPlayerUI() {
        setPadding(new Insets(0, 0, 0, 33));
        File mediaDir = new File("media");
        if (!mediaDir.exists() || !mediaDir.isDirectory()) {
            mediaDir = new File(new File(System.getProperty("user.dir",".")).getParentFile().getParentFile(),"media");
        }
        if (!mediaDir.exists() || !mediaDir.isDirectory()) {
            System.err.println("Could not find media directory: "+mediaDir.getAbsolutePath());
        } else {
            final File[] subDirs = mediaDir.listFiles(DIR_FILTER);

//            new Timeline(new KeyFrame(Duration.seconds(1), new EventHandler<ActionEvent>() {
//                @Override public void handle(ActionEvent event) {
//                    for(File subDir: subDirs) {
//                        final MediaFolder mediaFolder = new MediaFolder(subDir);
//                        getPanes().add(mediaFolder);
//                    }
//                }
//            })).play();
            for(File subDir: subDirs) {
                final MediaFolder mediaFolder = new MediaFolder(subDir);
                getPanes().add(mediaFolder);
            }
//            final File md = mediaDir;
//            new Thread(() -> {
//                try {
//                    synchronized (this) {
//                        wait(2000);
//                    }
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                for(File subDir: md.listFiles(DIR_FILTER)) {
//                    final MediaFolder mediaFolder = new MediaFolder(subDir);
//                    Platform.runLater(() -> getPanes().add(mediaFolder) );
//                }
//            }).start();
        }
        // open last one, usually photos
        if (!getPanes().isEmpty()) setExpandedPane(getPanes().get(getPanes().size()-1));
    }
}
