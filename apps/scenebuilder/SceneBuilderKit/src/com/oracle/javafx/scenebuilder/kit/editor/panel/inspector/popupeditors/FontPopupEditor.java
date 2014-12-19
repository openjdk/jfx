/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.popupeditors;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.AutoSuggestEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.BoundedDoubleEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.EditorUtils;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;

/**
 * Font popup editor.
 */
public class FontPopupEditor extends PopupEditor {

    @FXML
    private StackPane familySp;
    @FXML
    private StackPane styleSp;
    @FXML
    private StackPane sizeSp;

    private Parent root;
    private Font font = Font.getDefault();
    private FamilyEditor familyEditor;
    private StyleEditor styleEditor;
    private BoundedDoubleEditor sizeEditor;
    private EditorController editorController;

    public FontPopupEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses, EditorController editorController) {
        super(propMeta, selectedClasses);
        initialize(editorController);
    }

    private void initialize(EditorController editorController) {
        this.editorController = editorController;
    }
    
    private void setStyle() {
        styleEditor.reset("", "", new ArrayList<>(getStyles(EditorUtils.toString(familyEditor.getValue()), false, editorController)));//NOI18N
        styleEditor.setUpdateFromModel(true);
        styleEditor.setValue(font.getStyle());
        styleEditor.setUpdateFromModel(false);
    }

    private void commit() {
        if (isUpdateFromModel()) {
            return;
        }
        font = getFont();
        assert font != null;
//        System.out.println("Committing: " + font + " - preview: " + getValueAsString());
        commitValue(font);
    }

    private Font getFont() {
        Font oldFont = font;
        Object sizeObj = sizeEditor.getValue();
        assert sizeObj instanceof Double;
        Font newFont = getFont(EditorUtils.toString(familyEditor.getValue()), EditorUtils.toString(styleEditor.getValue()),
                (Double) sizeObj, editorController);
        if (newFont != null) {
            return newFont;
        } else {
            return oldFont;
        }
    }

    @Override
    public Object getValue() {
        return font;
    }

    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses, EditorController editorController) {
        super.reset(propMeta, selectedClasses);
        this.editorController = editorController;
    }

    //
    // Interface from PopupEditor
    // Methods called by PopupEditor.
    //
    @Override
    public void initializePopupContent() {
        root = EditorUtils.loadPopupFxml("FontPopupEditor.fxml", this); //NOI18N
        // Add the editors in the scene graph
        familyEditor = new FamilyEditor("", "", getFamilies(editorController), editorController);//NOI18N
        familySp.getChildren().add(familyEditor.getValueEditor());
        styleEditor = new StyleEditor("", "", new ArrayList<>(), editorController);//NOI18N
        styleSp.getChildren().add(styleEditor.getValueEditor());
        sizeEditor = new BoundedDoubleEditor("", "", getPredefinedFontSizes(), 1.0, 96.0, true);//NOI18N
        commitOnFocusLost(sizeEditor);
        sizeSp.getChildren().add(sizeEditor.getValueEditor());

        familyEditor.valueProperty().addListener((ChangeListener<Object>) (ov, oldVal, newVal) -> {
            if (familyEditor.isUpdateFromModel()) {
                // nothing to do
                return;
            }
            commit();
            setStyle();
        });

        styleEditor.valueProperty().addListener((ChangeListener<Object>) (ov, oldVal, newVal) -> {
            if (styleEditor.isUpdateFromModel()) {
                // nothing to do
                return;
            }
            commit();
        });

        sizeEditor.valueProperty().addListener((ChangeListener<Object>) (ov, oldVal, newVal) -> {
            if (sizeEditor.isUpdateFromModel()) {
                // nothing to do
                return;
            }
            commit();
        });

        sizeEditor.transientValueProperty().addListener((ChangeListener<Object>) (ov, oldVal, newVal) -> transientValue(getFont()));
    }

    @Override
    public String getPreviewString(Object value) {
        // value should never be null
        assert value instanceof Font;
        Font fontVal = (Font) value;
        if (isIndeterminate()) {
            return "-"; //NOI18N
        } else {
            String size = EditorUtils.valAsStr(fontVal.getSize());
            return fontVal.getFamily() + " " + size + "px" //NOI18N
                    + (!fontVal.getName().equals(fontVal.getFamily()) && !"Regular".equals(fontVal.getStyle()) ? //NOI18N
                    " (" + fontVal.getStyle() + ")" : ""); //NOI18N
        }
    }

    @Override
    public void setPopupContentValue(Object value) {
        assert value instanceof Font;
        font = (Font) value;
        familyEditor.setUpdateFromModel(true);
        familyEditor.setValue(font.getFamily());
        familyEditor.setUpdateFromModel(false);
        setStyle();
        sizeEditor.setUpdateFromModel(true);
        sizeEditor.setValue(font.getSize());
        sizeEditor.setUpdateFromModel(false);
    }

    @Override
    public Node getPopupContentNode() {
        return root;
    }

    private static class FamilyEditor extends AutoSuggestEditor {

        private List<String> families;
        private String family = null;

        public FamilyEditor(String name, String defaultValue, List<String> families, EditorController editorController) {
            super(name, defaultValue, families);
            initialize(families, editorController);
        }
        
        private void initialize(List<String> families, EditorController editorController) {
            this.families = families;
            EventHandler<ActionEvent> onActionListener = event -> {
                if (Objects.equals(family, getTextField().getText())) {
                    // no change
                    return;
                }
                family = getTextField().getText();
                if (family.isEmpty() || !FamilyEditor.this.families.contains(family)) {
                    editorController.getMessageLog().logWarningMessage(
                            "inspector.font.invalidfamily", family); //NOI18N
                    return;
                }
//                    System.out.println("Setting family from '" + valueProperty().get() + "' to '" + value + "'");
                valueProperty().setValue(family);
            };

            setTextEditorBehavior(this, getTextField(), onActionListener);
            commitOnFocusLost(this);
        }

        @Override
        public Object getValue() {
            return getTextField().getText();
        }

        @SuppressWarnings("unused")
        public List<String> getFamilies() {
            return families;
        }
    }

    private static class StyleEditor extends AutoSuggestEditor {
        
        private String style = null;

        public StyleEditor(String name, String defaultValue, List<String> suggestedList, EditorController editorController) {
            super(name, defaultValue, suggestedList);
            initialize(editorController);
        }
        
        private void initialize(EditorController editorController) {
            EventHandler<ActionEvent> onActionListener = event -> {
                if (Objects.equals(style, getTextField().getText())) {
                    // no change
                    return;
                }
                style = getTextField().getText();
                if (style.isEmpty() || !getSuggestedList().contains(style)) {
                    editorController.getMessageLog().logWarningMessage(
                            "inspector.font.invalidstyle", style); //NOI18N
                    return;
                }
                valueProperty().setValue(style);
            };

            setTextEditorBehavior(this, getTextField(), onActionListener);
            commitOnFocusLost(this);
        }

        @Override
        public Object getValue() {
            return getTextField().getText();
        }
    }

    private static void commitOnFocusLost(AutoSuggestEditor autoSuggestEditor) {
        autoSuggestEditor.getTextField().focusedProperty().addListener((ChangeListener<Boolean>) (ov, oldVal, newVal) -> {
            if (!newVal) {
                autoSuggestEditor.getCommitListener().handle(null);
            }
        });
    }

    /*
     *
     * Utilities methods for Font handling
     *
     */
    private static WeakReference<Map<String, Map<String, Font>>> fontCache
            = new WeakReference<>(null);

    // Automagically discover which font will require the work around for RT-23021.
    private static volatile Map<String, String> pathologicalFonts = null;

    private static final Comparator<Font> fontComparator
            = (t, t1) -> {
        int cmp = t.getName().compareTo(t1.getName());
        if (cmp != 0) {
            return cmp;
        }
        return t.toString().compareTo(t1.toString());
    };

    public static Set<Font> getAllFonts() {
        Font f = Font.getDefault();
        double defSize = f.getSize();
        Set<Font> allFonts = new TreeSet<>(fontComparator);
        for (String familly : Font.getFamilies()) {
            //System.out.println("*** FAMILY: " + familly); //NOI18N
            for (String name : Font.getFontNames(familly)) {
                Font font = new Font(name, defSize);
                allFonts.add(font);
                //System.out.println("\t\""+name+"\" -- name=\""+font.getName()+"\", familly=\""+font.getFamily()+"\", style=\""+font.getStyle()+"\""); //NOI18N
            }
        }
        // some font will not appear with the code above: we also need to use getAllNames!
        for (String name : Font.getFontNames()) {
            Font font = new Font(name, defSize);
            allFonts.add(font);
        }
        return allFonts;
    }

    public static List<String> getFamilies(EditorController editorController) {
//        System.out.println("Getting font families...");
        return new ArrayList<>(getFontMap(editorController).keySet());
    }

    public static Set<String> getStyles(String family, boolean canBeUnknown, EditorController editorController) {
        Map<String, Font> styles = getFontMap(editorController).get(family);
        if (styles == null) {
            assert !canBeUnknown;
            styles = Collections.emptyMap();
        }
        return styles.keySet();
    }

    public static Font getFont(String family, String style, EditorController editorController) {
        Map<String, Font> styles = getFontMap(editorController).get(family);
        if (styles == null) {
            styles = Collections.emptyMap();
        }

        if (styles.get(style) == null) {
            // The requested style does not exist for this font:
            // pick up the first style
            style = styles.keySet().iterator().next();
        }
        return styles.get(style);
    }

    public static Font getFont(String family, String style, double size, EditorController editorController) {
        final Font font = getFont(family, style, editorController);
        if (font == null) {
            return null;
        }
        return getFont(font, size);
    }

    public static Font getFont(Font font, double size) {
        if (font == null) {
            assert false;
            font = Font.getDefault();
        }
        if (Math.abs(font.getSize() - size) < .0000001) {
            return font;
        }

        return new Font(getPersistentName(font), size);
    }

    public static Map<String, String> getPathologicalFonts() {
        if (pathologicalFonts == null) {
            final double size = Font.getDefault().getSize();
            final String defaultName = Font.getDefault().getName();
            Map<String, String> problems = new HashMap<>();
            final Set<String> allNames = new HashSet<>(Font.getFontNames());
            for (String familly : Font.getFamilies()) {
                allNames.addAll(Font.getFontNames(familly));
            }
            for (String name : allNames) {
                Font f = new Font(name, size);
                if (f.getName().equals(name)) {
                    continue;
                }
                if (f.getName().equals(defaultName) || f.getName().equals("System")) { //NOI18N
                    continue; //NOI18N
                }
                final Font f2 = new Font(f.getName(), size);
                if (f2.getName().equals(f.getName())) {
                    continue;
                }
                problems.put(f.getName(), name);
            }
            pathologicalFonts = Collections.unmodifiableMap(problems);
        }
        return pathologicalFonts;
    }

    public static String getPersistentName(Font font) {
        // The block below is an ugly workaround for 
        // RT-23021: Inconsitent naming for fonts in the 'Tahoma' family.
        final Map<String, String> problems = getPathologicalFonts();
        if (problems.containsKey(font.getName())) { // e.g. font.getName() is "Tahoma Bold" //NOI18N
            final Font test = new Font(font.getName(), font.getSize());
            if (test.getName().equals(font.getName())) {
                // OK
                return font.getName();
            } else {
                final String alternateName = problems.get(font.getName()); // e.g: "Tahoma Negreta" //NOI18N
                assert alternateName != null;
                final Font test2 = new Font(alternateName, font.getSize()); //NOI18N
                if (test2.getName().equals(font.getName())) {
                    // OK
                    return alternateName; // e.g: "Tahoma Negreta" //NOI18N
                }
            }
        }
        return font.getName();
    }

    private static Map<String, Map<String, Font>> getFontMap(EditorController editorController) {
        Map<String, Map<String, Font>> fonts = fontCache.get();
        if (fonts == null) {
            fonts = makeFontMap(editorController);
            fontCache = new WeakReference<>(fonts);
        }
        return fonts;
    }

    private static Map<String, Map<String, Font>> makeFontMap(EditorController editorController) {
        final Set<Font> fonts = getAllFonts();
        final Map<String, Map<String, Set<Font>>> fontTree = new TreeMap<>();

        for (Font f : fonts) {
            Map<String, Set<Font>> familyStyleMap = fontTree.get(f.getFamily());
            if (familyStyleMap == null) {
                familyStyleMap = new TreeMap<>();
                fontTree.put(f.getFamily(), familyStyleMap);
            }
            Set<Font> styleFonts = familyStyleMap.get(f.getStyle());
            if (styleFonts == null) {
                styleFonts = new HashSet<>();
                familyStyleMap.put(f.getStyle(), styleFonts);
            }
            styleFonts.add(f);
        }

        final Map<String, Map<String, Font>> res = new TreeMap<>();
        for (Map.Entry<String, Map<String, Set<Font>>> e1 : fontTree.entrySet()) {
            final String family = e1.getKey();
            final Map<String, Set<Font>> styleMap = e1.getValue();
            final Map<String, Font> resMap = new TreeMap<>();
            for (Map.Entry<String, Set<Font>> e2 : styleMap.entrySet()) {
                final String style = e2.getKey();
                final Set<Font> fontSet = e2.getValue();
                int size = fontSet.size();
                assert 1 <= size;
                if (1 < size) {
                    editorController.getMessageLog().logWarningMessage(
                            "inspector.font.samefamilystyle", styleMap.get(style)); //NOI18N
                }
                resMap.put(style, styleMap.get(style).iterator().next());
            }
            res.put(family, Collections.<String, Font>unmodifiableMap(resMap));
        }
        return Collections.<String, Map<String, Font>>unmodifiableMap(res);
    }

    private static List<String> getPredefinedFontSizes() {
        String[] predefinedFontSizes
                = {"9", "10", "11", "12", "13", "14", "18", "24", "36", "48", "64", "72", "96"};//NOI18N
        return Arrays.asList(predefinedFontSizes);
    }

}
