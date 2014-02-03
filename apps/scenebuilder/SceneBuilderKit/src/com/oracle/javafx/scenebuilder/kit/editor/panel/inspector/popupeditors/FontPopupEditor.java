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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
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

    private final Parent root;
    private Font font = Font.getDefault();
    private FamilyEditor familyEditor;
    private StyleEditor styleEditor;
    private BoundedDoubleEditor sizeEditor;

    @SuppressWarnings("LeakingThisInConstructor")
    public FontPopupEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);
        root = EditorUtils.loadPopupFxml("FontPopupEditor.fxml", this); //NOI18N
        initialize();
    }

    // Method to please FindBugs
    private void initialize() {
        // Add the editors in the scene graph
        familyEditor = new FamilyEditor("", "", new ArrayList<>());//NOI18N
        familySp.getChildren().add(familyEditor.getValueEditor());
        styleEditor = new StyleEditor("", "", new ArrayList<>());//NOI18N
        styleSp.getChildren().add(styleEditor.getValueEditor());
        sizeEditor = new BoundedDoubleEditor("", "", getPredefinedFontSizes(), 1.0, 96.0, true);//NOI18N
        commitOnFocusLost(sizeEditor);
        sizeSp.getChildren().add(sizeEditor.getValueEditor());

        familyEditor.valueProperty().addListener(new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<? extends Object> ov, Object oldVal, Object newVal) {
                commit();
                setStyle();
            }
        });

        ChangeListener<Object> valueListener = new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<? extends Object> ov, Object oldVal, Object newVal) {
                commit();
            }
        };
        styleEditor.valueProperty().addListener(valueListener);
        sizeEditor.valueProperty().addListener(valueListener);

        setCommitListener(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                commit();
            }
        });

        // Font families are costly to initialize, so this is delayed when the popup is opened
        showingProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean previousValue, Boolean newValue) {
                if (newValue && familyEditor.getFamilies().isEmpty()) {
                    familyEditor.setFamilies(getFamilies());
                }
            }
        });

        // Plug to the menu button.
        plugEditor(this, root);
    }

    private void setStyle() {
        styleEditor.reset("", "", new ArrayList<>(getStyles(familyEditor.getValue(), false)));//NOI18N
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
        commitValue(font, getValueAsString());
    }

    private Font getFont() {
        Font oldFont = font;
        Object sizeObj = sizeEditor.getValue();
        assert sizeObj instanceof Double;
        Font newFont = getFont(familyEditor.getValue(), styleEditor.getValue(), (Double) sizeObj);
        if (newFont != null) {
            return newFont;
        } else {
            return oldFont;
        }
    }

    private String getValueAsString() {
        if (isIndeterminate()) {
            return "-"; //NOI18N
        } else {
            assert font != null;
            String size = EditorUtils.valAsStr(font.getSize());
            return font.getFamily() + " " + size + "px" //NOI18N
                    + (!font.getName().equals(font.getFamily()) && !"Regular".equals(font.getStyle()) ? //NOI18N
                    " (" + font.getStyle() + ")" : ""); //NOI18N
        }
    }

    @Override
    public Object getValue() {
        return font;
    }

    //
    // Interface PopupEditor.InputValue.
    // Methods called by PopupEditor.
    //
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

        // Update the menu button string
        displayValueAsString(getValueAsString());
    }

    @Override
    public void resetPopupContent() {
        // Nothing to do here, since the font is never null
    }

    private static class FamilyEditor extends AutoSuggestEditor {

        private List<String> families;

        @SuppressWarnings("LeakingThisInConstructor")
        public FamilyEditor(String name, String defaultValue, List<String> families) {
            super(name, defaultValue, families);
            this.families = families;
            EventHandler<ActionEvent> onActionListener = new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    String value = getTextField().getText();
                    if (value.isEmpty() || !FamilyEditor.this.families.contains(value)) {
                        System.err.println("Invalid value for Family: " + value);//NOI18N
//                        handleInvalidValue(value);
                        return;
                    }
//                    System.out.println("Setting family from '" + valueProperty().get() + "' to '" + value + "'");
                    valueProperty().setValue(value);
                }
            };

            setTextEditorBehavior(this, getTextField(), onActionListener);
            commitOnFocusLost(this);
        }

        @Override
        public String getValue() {
            return getTextField().getText();
        }

        public void setFamilies(List<String> families) {
            super.resetSuggestedList(families);
            this.families = families;
        }
        
        public List<String> getFamilies() {
            return families;
        }
    }

    private static class StyleEditor extends AutoSuggestEditor {

        @SuppressWarnings("LeakingThisInConstructor")
        public StyleEditor(String name, String defaultValue, List<String> suggestedList) {
            super(name, defaultValue, suggestedList);
            EventHandler<ActionEvent> onActionListener = new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    String value = getTextField().getText();
                    if (value.isEmpty() || !getSuggestedList().contains(value)) {
                        System.err.println("Invalid value for Style: " + value);//NOI18N
                        return;
                    }
                    valueProperty().setValue(value);
                }
            };

            setTextEditorBehavior(this, getTextField(), onActionListener);
            commitOnFocusLost(this);
        }

        @Override
        public String getValue() {
            return getTextField().getText();
        }
    }

    private static void commitOnFocusLost(AutoSuggestEditor autoSuggestEditor) {
        autoSuggestEditor.getTextField().focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) {
                if (!newVal) {
                    autoSuggestEditor.getCommitListener().handle(null);
                }
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
            = new Comparator<Font>() {
                @Override
                public int compare(Font t, Font t1) {
                    int cmp = t.getName().compareTo(t1.getName());
                    if (cmp != 0) {
                        return cmp;
                    }
                    return t.toString().compareTo(t1.toString());
                }
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

    public static List<String> getFamilies() {
//        System.out.println("Getting font families...");
        return new ArrayList<>(getFontMap().keySet());
    }

    public static Set<String> getStyles(String family, boolean canBeUnknown) {
        Map<String, Font> styles = getFontMap().get(family);
        if (styles == null) {
            assert !canBeUnknown;
            styles = Collections.emptyMap();
        }
        return styles.keySet();
    }

    public static Font getFont(String family, String style) {
        Map<String, Font> styles = getFontMap().get(family);
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

    public static Font getFont(String family, String style, double size) {
        final Font font = getFont(family, style);
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

    private static Map<String, Map<String, Font>> getFontMap() {
        Map<String, Map<String, Font>> fonts = fontCache.get();
        if (fonts == null) {
            fonts = makeFontMap();
            fontCache = new WeakReference<>(fonts);
        }
        return fonts;
    }

    private static Map<String, Map<String, Font>> makeFontMap() {
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
                    System.out.println("Warning: several fonts have the same family and style: " + styleMap.get(style)); //NOI18N
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
