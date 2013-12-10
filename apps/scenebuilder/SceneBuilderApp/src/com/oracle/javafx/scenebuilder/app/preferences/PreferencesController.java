/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.app.preferences;

import java.util.prefs.Preferences;

/**
 * Defines preferences for Scene Builder. Theses preferences are common to all
 * SB projects.
 *
 */
public class PreferencesController {

    // JAVA PREFERENCES KEYS DEFINITIONS
    static final String SB_RELEASE_NODE = "SB_2.0"; //NOI18N

    static final String DOCUMENT_HEIGHT = "DOCUMENT_HEIGHT"; //NOI18N
    static final String DOCUMENT_WIDTH = "DOCUMENT_WIDTH"; //NOI18N

    static final String BACKGROUND_IMAGE = "BACKGROUND_IMAGE"; //NOI18N
    static final String ALIGNMENT_GUIDES_COLOR = "ALIGNMENT_GUIDES_COLOR"; //NOI18N
    static final String PARENT_RING_COLOR = "PARENT_RING_COLOR"; //NOI18N

    static final String HIERARCHY_DISPLAY_OPTION = "HIERARCHY_DISPLAY_OPTION"; //NOI18N
    static final String CSS_ANALYZER_COLUMN_ORDER = "CSS_ANALYZER_COLUMN_ORDER"; //NOI18N

    static final String RECENT_ITEMS = "RECENT_ITEMS"; //NOI18N
    static final String RECENT_ITEMS_SIZE = "RECENT_ITEMS_SIZE"; //NOI18N

    private static final PreferencesController singleton = new PreferencesController();

    private final Preferences javaPreferences;
    private final PreferencesRecordGlobal recordGlobal;

    private PreferencesController() {
        javaPreferences = Preferences.userNodeForPackage(
                PreferencesController.class).node(SB_RELEASE_NODE);

        // Preferences global to the SB application
        recordGlobal = new PreferencesRecordGlobal(javaPreferences);
    }

    public static PreferencesController getSingleton() {
        return singleton;
    }

    public PreferencesRecordGlobal getRecordGlobal() {
        return recordGlobal;
    }
}
