/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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

import com.oracle.javafx.scenebuilder.app.DocumentWindowController;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Defines preferences for Scene Builder.
 */
public class PreferencesController {

    // JAVA PREFERENCES KEYS DEFINITIONS
    static final String SB_RELEASE_NODE = "SB_2.0"; //NOI18N

    // GLOBAL PREFERENCES
    static final String ROOT_CONTAINER_HEIGHT = "ROOT_CONTAINER_HEIGHT"; //NOI18N
    static final String ROOT_CONTAINER_WIDTH = "ROOT_CONTAINER_WIDTH"; //NOI18N

    static final String BACKGROUND_IMAGE = "BACKGROUND_IMAGE"; //NOI18N
    static final String ALIGNMENT_GUIDES_COLOR = "ALIGNMENT_GUIDES_COLOR"; //NOI18N
    static final String PARENT_RING_COLOR = "PARENT_RING_COLOR"; //NOI18N

    static final String TOOL_THEME = "TOOL_THEME"; //NOI18N
    static final String LIBRARY_DISPLAY_OPTION = "LIBRARY_DISPLAY_OPTION"; //NOI18N
    static final String HIERARCHY_DISPLAY_OPTION = "HIERARCHY_DISPLAY_OPTION"; //NOI18N
    static final String CSS_TABLE_COLUMNS_ORDERING_REVERSED = "CSS_TABLE_COLUMNS_ORDERING_REVERSED"; //NOI18N

    static final String RECENT_ITEMS = "RECENT_ITEMS"; //NOI18N
    static final String RECENT_ITEMS_SIZE = "RECENT_ITEMS_SIZE"; //NOI18N

    static final String DOCUMENTS = "DOCUMENTS"; //NOI18N

    // DOCUMENT SPECIFIC PREFERENCES
    static final String PATH = "path"; //NOI18N
    static final String X_POS = "X"; //NOI18N
    static final String Y_POS = "Y"; //NOI18N
    static final String STAGE_HEIGHT = "height"; //NOI18N
    static final String STAGE_WIDTH = "width"; //NOI18N
    static final String BOTTOM_VISIBLE = "bottomVisible";//NOI18N
    static final String LEFT_VISIBLE = "leftVisible"; //NOI18N
    static final String RIGHT_VISIBLE = "rightVisible"; //NOI18N
    static final String LIBRARY_VISIBLE = "libraryVisible"; //NOI18N
    static final String DOCUMENT_VISIBLE = "documentVisible"; //NOI18N
    static final String INSPECTOR_SECTION_ID = "inspectorSectionId"; //NOI18N
    static final String LEFT_DIVIDER_HPOS = "leftDividerHPos"; //NOI18N
    static final String RIGHT_DIVIDER_HPOS = "rightDividerHPos"; //NOI18N
    static final String BOTTOM_DIVIDER_VPOS = "bottomDividerVPos"; //NOI18N
    static final String LEFT_DIVIDER_VPOS = "leftDividerVPos"; //NOI18N
    static final String SCENE_STYLE_SHEETS = "sceneStyleSheets"; //NOI18N
    static final String I18N_RESOURCE = "I18NResource"; //NOI18N

    private static PreferencesController singleton;

    private final Preferences applicationRootPreferences;
    private final Preferences documentsRootPreferences;
    private final PreferencesRecordGlobal recordGlobal;
    private final Map<DocumentWindowController, PreferencesRecordDocument> recordDocuments = new HashMap<>();

    private PreferencesController() {
        applicationRootPreferences = Preferences.userNodeForPackage(
                PreferencesController.class).node(SB_RELEASE_NODE);

        // Preferences global to the SB application
        recordGlobal = new PreferencesRecordGlobal(applicationRootPreferences);

        // Preferences specific to the document
        // Create the root node for all documents preferences
        documentsRootPreferences = applicationRootPreferences.node(DOCUMENTS);

        // Cleanup document preferences at start time : 
        // We keep only document preferences for the documents defined in RECENT_ITEMS
        final String items = applicationRootPreferences.get(RECENT_ITEMS, null); //NOI18N
        if (items != null) {
            // Remove document preferences node if needed
            try {
                final String[] childrenNames = documentsRootPreferences.childrenNames();
                // Check among the document root chidlren if there is a child
                // which path matches the specified one
                for (String child : childrenNames) {
                    final Preferences documentPreferences = documentsRootPreferences.node(child);
                    final String nodePath = documentPreferences.get(PATH, null);
                    // Each document node defines a path
                    // If path is null or empty, this means preferences DB has been corrupted
                    if (nodePath == null || nodePath.isEmpty()) {
                        documentPreferences.removeNode();
                    } else if (items.contains(nodePath) == false) {
                        documentPreferences.removeNode();
                    }
                }
            } catch (BackingStoreException ex) {
                Logger.getLogger(PreferencesRecordGlobal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static synchronized PreferencesController getSingleton() {
        if (singleton == null) {
            singleton = new PreferencesController();
            singleton.getRecordGlobal().readFromJavaPreferences();
        }
        return singleton;
    }

    public PreferencesRecordGlobal getRecordGlobal() {
        return recordGlobal;
    }

    public PreferencesRecordDocument getRecordDocument(final DocumentWindowController dwc) {
        final PreferencesRecordDocument recordDocument;
        if (recordDocuments.containsKey(dwc)) {
            recordDocument = recordDocuments.get(dwc);
        } else {
            recordDocument = new PreferencesRecordDocument(documentsRootPreferences, dwc);
            recordDocuments.put(dwc, recordDocument);
        }
        return recordDocument;
    }
}
