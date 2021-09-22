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

package fxmediaplayer.control;

import fxmediaplayer.FXMediaPlayerControlInterface;
import fxmediaplayer.FXMediaPlayerInterface;
import javafx.scene.control.TabPane;
import javafx.scene.media.MediaPlayer;

public class MediaPlayerTabControl implements FXMediaPlayerControlInterface {

    private FXMediaPlayerInterface FXMediaPlayer = null;
    private TabPane tabControl = null;
    private MediaPlayerControlTab controlTab = null;
    private MediaPlayerSpectrumTab spectrumTab = null;
    private MediaPlayerEqualizerTab equalizerTab = null;
    private MediaPlayerEffectsTab effectsTab = null;
    private MediaPlayerMarkersTab markersTab = null;
    private MediaPlayerPlayListTab playListTab = null;

    public MediaPlayerTabControl(FXMediaPlayerInterface FXMediaPlayer) {
        this.FXMediaPlayer = FXMediaPlayer;
    }

    public TabPane getTabControl() {
        if (tabControl == null) {
            tabControl = new TabPane();
            tabControl.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            tabControl.setPrefHeight(200);

            controlTab = new MediaPlayerControlTab(FXMediaPlayer);
            spectrumTab = new MediaPlayerSpectrumTab(FXMediaPlayer);
            equalizerTab = new MediaPlayerEqualizerTab(FXMediaPlayer);
            effectsTab = new MediaPlayerEffectsTab(FXMediaPlayer);
            markersTab = new MediaPlayerMarkersTab(FXMediaPlayer);
            playListTab = new MediaPlayerPlayListTab(FXMediaPlayer);

            tabControl.getTabs().add(controlTab.getControlTab());
            tabControl.getTabs().add(spectrumTab.getSpectrumTab());
            tabControl.getTabs().add(equalizerTab.getEqualizerTab());
            tabControl.getTabs().add(effectsTab.getColorAdjustTab());
            tabControl.getTabs().add(markersTab.getMarkersTab());
            tabControl.getTabs().add(playListTab.getPlayListTab());
        }

        return tabControl;
    }

    @Override
    public void onMediaPlayerChanged(MediaPlayer oldMediaPlayer) {
        controlTab.onMediaPlayerChanged(oldMediaPlayer);
        spectrumTab.onMediaPlayerChanged(oldMediaPlayer);
        equalizerTab.onMediaPlayerChanged(oldMediaPlayer);
        effectsTab.onMediaPlayerChanged(oldMediaPlayer);
        markersTab.onMediaPlayerChanged(oldMediaPlayer);
    }
}
