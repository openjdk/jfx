/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.pages;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import com.oracle.tools.fx.monkey.media.Notes;
import com.oracle.tools.fx.monkey.options.DoubleOption;
import com.oracle.tools.fx.monkey.options.IntOption;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * AudioClip Page.
 */
public class AudioClipPage extends TestPaneBase {

    private final SimpleStringProperty sourceURI = new SimpleStringProperty();
    private final SimpleDoubleProperty balance = new SimpleDoubleProperty(1.0);
    private final SimpleIntegerProperty cycleCount = new SimpleIntegerProperty(1);
    private final SimpleDoubleProperty pan = new SimpleDoubleProperty(1.0);
    private final SimpleIntegerProperty priority = new SimpleIntegerProperty(1);
    private final SimpleDoubleProperty rate = new SimpleDoubleProperty(1.0);
    private final SimpleDoubleProperty volume = new SimpleDoubleProperty(1.0);

    public AudioClipPage() {
        super("AudioClipPage");

        OptionPane op = new OptionPane();
        op.section("AudioClip");
        op.option("Source URI:", Options.createSourceUriOption(this, "source", sourceURI));
        op.option(new HBox(5, button("Play", this::play)));
        op.option("Balance:", DoubleOption.of("balance", balance, 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0));
        op.option("Cycle Count:", new IntOption("cycleCount", 0, Integer.MAX_VALUE, cycleCount));
        op.option("Pan:", DoubleOption.of("pan", pan, 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0));
        op.option("Priority:", new IntOption("priority", 0, Integer.MAX_VALUE, priority));
        op.option("Rate:", DoubleOption.of("rate", rate, 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0));
        op.option("Volume:", DoubleOption.of("volume", volume, 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0));

        // all notes?
        VBox p = new VBox(
            button("Play Note1.wav", () -> playNote("Note1.wav")),
            button("Play Note2.wav", () -> playNote("Note2.wav")),
            button("Play Note3.wav", () -> playNote("Note3.wav")),
            button("Play yo.mp3", () -> playNote("yo.mp3"))
        );

        setContent(p);
        setOptions(op);
    }

    private Button button(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction((ev) -> action.run());
        return b;
    }

    private void playNote(String name) {
        play(Notes.getNoteURI(name));
    }

    private void play() {
        String uri = sourceURI.get();
        play(uri);
    }

    private void play(String uri) {
        if (Utils.isBlank(uri)) {
            return;
        }

        AudioClip c = new AudioClip(uri);
        c.setBalance(balance.get());
        c.setCycleCount(cycleCount.get());
        c.setPan(pan.get());
        c.setPriority(priority.get());
        c.setRate(rate.get());
        c.setVolume(volume.get());
        c.play();
        System.out.println("AudioClip.source=" + uri);
    }
}
