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
package com.oracle.javafx.scenebuilder.kit.editor.panel.css;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.util.CssInternal;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import java.util.Set;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;

/**
 * CSS support for Scene Builder.
 *
 */
public class CssUtils {

    private CssUtils() {
        assert false;
    }

//    /**
//     * This method is used to create a barrier between the SB and the user scene graph.
//     * @param parent 
//     */
//    public static Group createCSSFrontier(){
//        Group grp = Deprecation.createGroupWithNullParentStylesheets();
//        /*
//         * Doing so, when caspian lookup resolution occurs for a Node D&D from a SB user, 
//         * if there is no redefinition in the Author space, 
//         * then this container is reached and the caspian lookups are resolved.
//         */
//        grp.getStyleClass().add("root"); //NOI18N
//        return grp;
//    }
//    
//    // Reseting is the process to set the value to its initial value.
//    private static <N extends Node> void resetCssProperty(N node, CssMetaData<N,?> p) {
//        try {            
//            Object val = getResetValue(node, p);
//            @SuppressWarnings("unchecked") //NOI18N
//            CssMetaData<Node, Object> sp = (CssMetaData<Node, Object>) p;
//            if (sp.isSettable(node)) {
//                sp.getStyleableProperty(node).applyStyle(StyleOrigin.USER_AGENT, val);
//            }
//        } catch (RuntimeException ex) {
//            Utils.println("Can't reset property " + p.getProperty() + " on " + node.getClass() + ": " + ex.toString()); //NOI18N
//        }
//    }
//
//    private static <N extends Node> Object getResetValue(N node, CssMetaData<N, ?> sp) {
//        // The best value depends on the nature of the property.
//        // If this is a styleable only property with no bean property associated
//        // then we need to use the initialValue. 
//        // If there is a Prop, then use it as the reset value.
//        // See DTL-4049 and DTL-4087
//        StyleableProperty<?> property = sp.getStyleableProperty(node);
//        Object value = sp.getInitialValue(node);
//        StyleOrigin orig = property.getStyleOrigin();
//        if (StyleOrigin.AUTHOR != orig && StyleOrigin.INLINE != orig) {
//            // Do we have a Prop?
//            String name = ((ReadOnlyProperty) property).getName();
//            try {
//                Prop.of(node, name);
//                value = property.getValue();
//            } catch (IllegalArgumentException ex) {
//                //OK, no prop, need initial value.
//            }
//        }
//        return value;
//    }
//    
//    /**
//     * Will try first to put the property in a state that makes it OK
//     * to receive USER AGENT styling.
//     * If this is not possible, will call prop.set(bean, value);
//     * @param bean
//     * @param prop
//     * @param value 
//     */
//    public static void setProperty(Object bean, Prop prop, Object value) {
//        if (bean != null) {
//            boolean set = false;
//            try {
//                ObservableValue<?> beanProp = prop.model(bean);
//                if (beanProp instanceof StyleableProperty) {
//                    Styleable styleable = getStyleable(bean);
//                    if(styleable != null){
//                        Node stylableNode = Deprecation.getNode(styleable);
//                        if (stylableNode == null) {
//                            stylableNode = getNode(bean);
//                        }
//                        @SuppressWarnings("unchecked") //NOI18N
//                        CssMetaData<Node, Object> sp = (CssMetaData<Node, Object>) ((StyleableProperty)beanProp).getCssMetaData();
//                        if (sp != null && stylableNode != null) {
//                            if (sp.isSettable(stylableNode)) {
//                                sp.getStyleableProperty(stylableNode).applyStyle(StyleOrigin.USER_AGENT, value);
//                            }
//                            set = true;
//                        }
//                    }
//                }
//            } catch (RuntimeException ex) {
//                Utils.println("can't set Bean property " + prop.name + " on " + bean.getClass() + " :" + ex.toString()); //NOI18N
//            }
//            
//            if(!set){
//                prop.set(bean, value);
//            }
//        }
//    }
    static String getBeanPropertyName(Node node, CssMetaData<?, ?> sp) {
        String property = null;
        try {
            @SuppressWarnings("unchecked")
            CssMetaData<Node, Object> raw = (CssMetaData<Node, Object>) sp;
            final StyleableProperty<Object> val = raw.getStyleableProperty(node);
            property = CssInternal.getBeanPropertyName(val);
        } catch (RuntimeException ex) {
            System.out.println("Can't retrieve property " + ex); //NOI18N
        }
        return property;
    }

//    private static void resetSkinNode(Node node) {
//        for (CssMetaData<?,?> p : node.getCssMetaData()) {
//            @SuppressWarnings("unchecked") //NOI18N
//            final CssMetaData<Node, ?> sp = (CssMetaData<Node, ?>) p;
//            resetCssProperty(node, sp);
//        }
//    }
//
//    private static void resetSubStructure(Node n) {
//        // We need to skip Elements tha tcan be present in the Skin (eg: Content
//        // of the Tab.
//        if (!n.getStyleClass().isEmpty() && Element.forNode(n) == null) {
//            resetSkinNode(n);
//        }
//        if (n instanceof Parent) {
//            Parent parentNode = (Parent) n;
//            for (Node child : parentNode.getChildrenUnmodifiable()) {
//                resetSubStructure(child);
//            }
//        }
//    }
//    // Reseting the style of a Node. Any property ruled by a Bean.property
//    // are not set. This is handled by the SB model (TargetPropertyValue class).
//    static void resetStyle(Node node) throws RuntimeException {
//        if (node.getScene() == null) {
//            return;
//        }
//        
//        // First reset the skin
//        if (node instanceof Control) {
//            Node skinNode = getSkinNode((Control) node);
//            if (skinNode != null) {
//                // We need to deep dive into the skin to reset properties.
//                // The caspian ruled properties would be aumaticaly reset
//                // but the (eg:shape Text used in LabeledSkin) non caspian ruled ones will keep their
//                // styling.
//               resetSubStructure(skinNode);
//            }
//        }
//        
//        // Then properties that are in the control that could suppercede the 
//        // previously set values.
//        @SuppressWarnings("rawtypes")
//        final List<CssMetaData<? extends Styleable, ?>> lst = node.getCssMetaData();
//        for(CssMetaData<?,?> stp : lst){
//            @SuppressWarnings("unchecked") //NOI18N
//            final CssMetaData<Node, ?> st = (CssMetaData<Node, ?>)stp;
//            
//            // Skip the skin
//            if(st.getProperty().equals("-fx-skin")) { //NOI18N
//                continue;
//            }
//            
//            @SuppressWarnings("unchecked") //NOI18N
//            StyleableProperty<?> val = st.getStyleableProperty(node);
//            boolean needsReset = false;
//            if(val == null){ // reset property that have no Bean property.
//                needsReset = true;
//            } else {
//                if(val instanceof ReadOnlyProperty){
//                    // Do we have a Prop?
//                    String name = ((ReadOnlyProperty)val).getName();
//                    try {
//                        Prop.of(node, name);
//                    }catch(IllegalArgumentException ex){
//                        //OK, no prop, need reset
//                        needsReset = true;
//                    }
//                } else {// No prop associated.
//                    needsReset = true;
//                }
//            }
//            if(needsReset){
//                resetCssProperty(node, st);
//            }
//        }
//        
//        // Clear the map from any collected value.
//        // TODO FX8: the statement below started to trigger NPE since FX8 b68,
//        // hence the temporary wrap in null check test. Still have to ensure
//        // it doesn't mask something suspicious.
//        if (Deprecation.getStyleMap(node) != null) {
//            Deprecation.getStyleMap(node).clear();
//        }
//    }
//
//    private static Node getSkinNode(Control control) {
//        Node n = null;
//        Skin<?> skin = control.getSkin();
//        if (skin != null) {
//            // can happen in unit test
//            n = skin.getNode();
//        }
//        return n;
//    }
//
//    public static void editCssRule(Frame frame, CssPropAuthorInfo info) {
//        try {
//            if (info.getMainUrl() == null) {
//                return;
//            }
//            File file = new File(info.getMainUrl().toURI());
//            Utils.editCssFile(file);
//        } catch (Exception ex) {
//            frame.printWarning("messagebar.cannot.edit", ex, info);
//        }
//    }
//    
//    public static File retrieveCssFile(Frame frame, ComponentPath path, String styleclass) {
//        // First check in the parent chain. The nearest first.
//        ComponentPath truncated = path.getNearestParentNodePath();
//        for(ComponentReference ref : truncated.getPath()){
//            if(ref.getChildComponent() instanceof Parent){
//                Parent p = (Parent) ref.getChildComponent();
//                // The last element has more priority in CSS application
//                for (int i = p.getStylesheets().size() - 1; i >= 0; i--) {
//                    String ss = p.getStylesheets().get(i);
//                    try {
//                        URL url = new URL(ss);
//                        Set<String> classes = getStyleClasses(url);
//                        if (classes.contains(styleclass)) {
//                            return new File(url.toURI());
//                        }
//                    } catch (Exception ex) {
//                        Utils.println("Exception parsing Stylesheet " + ex); //NOI18N
//                    }
//                }
//            }
//        }
//        List<StyleClasses> styleClasses = STYLECLASSES_SETS.get(getParent(frame));
//        File ret = null;
//        String url = null;
//        if (styleClasses != null) {
//            // The last element has more priority in CSS application
//            for (int i = styleClasses.size()-1; i >= 0; i--) {
//                StyleClasses sc = styleClasses.get(i);
//                if (sc.styleClasses.contains(styleclass)) {
//                    url = sc.url;
//                }
//            }
//            if (url != null) {
//                try {
//                    URI uri = new URI(url);
//                    ret = new File(uri);
//                } catch (Exception ex) {
//                    // XXX Issue with this URL.
//                    frame.printWarning("messagebar.cannot.retrieve.css.file", url);
//                }
//            }
//        }
//        return ret;
//    }
//
//    public static boolean isAuthorStyleClass(Frame frame, ComponentPath path, String item) {
//        return retrieveCssFile(frame, path, item) != null;
//    }
//
//    private static class StyleClasses {
//
//        String url;
//        Set<String> styleClasses;
//    }
//
//    /*
//     * Returns the CSS property value defined in the specified style sheet.
//     * Note that the method returns the first CSS property found in the style sheet :
//     * - no check is performed on the style class containing the property
//     * - if the property is defined in several style classes, the first one is returned
//     */
//    public static ParsedValue<?, ?> getValueFor(Stylesheet stylesheet, String property) {
//        for (Rule rule : stylesheet.getRules()) {
//            for (Declaration decl : rule.getDeclarations()) {
//                if (property.equals(decl.getProperty())) {
//                    return decl.getParsedValue();
//                }
//            }
//        }
//        return null;
//    }
//
//    public static ParsedValue<?, ?> getValueFor(String styleClass, String property) {
//        return getValueFor(STYLE_SHEET_TOOL_CSS, styleClass, property);
//    }
//    
//    public static final String TOOL;
//    static {
//        if (Utils.IS_MAC) {
//            TOOL = "tool-mac"; //NOI18N
//        } else if (Utils.IS_WINDOWS_XP) {
//            TOOL = "tool-win-xp"; //NOI18N
//        } else {
//            TOOL = "tool"; //NOI18N
//        }
//    }
//    
//    
//    public static final String THEME_CSS = Utils.getResourceURL(Frame.class, "css_stylesheets/SceneBuilderTheme.css"); //NOI18N
//    
//    public static final String TOOL_ROOT_CSS = Utils.getResourceURL(Frame.class, "css_stylesheets/ToolRoot.css"); //NOI18N
//    public static final String CONTENT_VIEW_CSS = Utils.getResourceURL(Frame.class, "css_stylesheets/ContentView.css"); //NOI18N
//    public static final String MESSAGE_BAR_CSS = Utils.getResourceURL(Frame.class, "css_stylesheets/MessageBar.css"); //NOI18N
//    public static final String LIBRARY_CSS = Utils.getResourceURL(Frame.class, "css_stylesheets/Library.css"); //NOI18N
//    public static final String HIERARCHY_CSS = Utils.getResourceURL(Frame.class, "css_stylesheets/Hierarchy.css"); //NOI18N
//    public static final String INSPECTOR_CSS = Utils.getResourceURL(Frame.class, "css_stylesheets/Inspector.css"); //NOI18N
//    public static final String CSS_VIEWER_CSS = Utils.getResourceURL(Frame.class, "css_stylesheets/CssViewer.css"); //NOI18N
//
//    public static final String POPUP_CSS = Utils.getResourceURL(Frame.class, "css_stylesheets/Popup.css"); //NOI18N
//    
//    public static final String SCENE_BUILDER_THEME_STYLECLASS = "SCENE_BUILDER_THEME"; //NOI18N
//    private static final String SCENE_BUILDER_WIN_FONT_STYLECLASS = "SCENE_BUILDER_WIN_FONT"; //NOI18N
//    private static final String SCENE_BUILDER_WINXP_FONT_STYLECLASS = "SCENE_BUILDER_WINXP_FONT"; //NOI18N
//    
//    public static final String CONTENT_AREA_ID = "JFX_SB_ContentArea"; //NOI18N
//    private static final String ROOT_STYLECLASS = "root"; //NOI18N
//    private static Stylesheet STYLE_SHEET_TOOL_CSS = null;
//    private static final Map<Class<?>, Set<String>> ALTERNATE_STYLECLASSES = new HashMap<>();
//
//    static Set<FileChooser.ExtensionFilter> SS_EXTENSIONS =
//            Collections.singleton(new FileChooser.ExtensionFilter(Utils.getI18N().getString("popup.style.sheets"), "*.css", "*.bss")); //NOI18N
//    
//    static {
//        try {
//            STYLE_SHEET_TOOL_CSS = CSSParser.getInstance().parse(new URL(TOOL_ROOT_CSS));
//        } catch (IOException ex) {
//            Utils.println("Failed to parse " + TOOL_ROOT_CSS, ex); //NOI18N
//        }
//        Set<String> alternates = new HashSet<>();
//        alternates.add("floating"); //NOI18N
//        ALTERNATE_STYLECLASSES.put(TabPane.class, alternates);
//    }
//    //Properties that are impacting CSS/Pages.
//    private static final Set<Prop> CSS_IMPACT = 
//            SetBuilder.<Prop>make().
//            add(PropUtils.Parent_STYLESHEETS).
//            add(PropUtils.Node_ID).add(PropUtils.Styleable_STYLE_CLASS).add(PropUtils.Node_STYLE).
//            add(PropUtils.Tab_ID).add(PropUtils.Tab_STYLE).
//            add(PropUtils.PopupControl_ID).add(PropUtils.PopupControl_STYLE).
//            add(PropUtils.Menu_ITEMS).buildUnmodifiable();
//    private static final WeakIdentityHashMap<Parent, List<StyleClasses>> STYLECLASSES_SETS = WeakIdentityHashMap.make();
//    
//    public static void trackNode(final Node node) {
//        attachMapToNode(node);
//    }
//    
//    public static void stopTrackingNode(Node node) {
//        @SuppressWarnings("rawtypes")
//        Map<StyleableProperty<?>, List<Style>> smap = Deprecation.getStyleMap(node);
//        assert smap != null;
//        smap.clear();
//    }
//    private static void resetCssState(Element elem) {
//        resetStyle(elem);
//    }
//
//    private static void resetStyle(Element elem) {
//        final Node n = elem.getNode();
//        if (n.getScene() == null) {
//            //System.out.println("RESET, not yet in scene, returning" + n); //NOI18N
//            return;
//        }
//        resetStyle(n);
//    }
//
////    private static void clearAuthorStyleSheets(Parent parent) {
////        for(String css : parent.getStylesheets()){
////            try{
////                removeStyleClasses(parent, new URL(css));
////            } catch (MalformedURLException ex) {
////                Utils.println("Cannot remove style classes", ex); //NOI18N
////            }
////        }
////        parent.getStylesheets().clear();       
////    }
//
////    public static void loadAuthorCssFiles(Frame frame) {
////        Parent contentParent = getParent(frame);
////        final Project project = frame.getProject();
////        try {
////            clearAuthorStyleSheets(contentParent);
////            for (Css css : project.getScreenData().getAuthorCss()) {
////                if (css.isActive()) {
////                    URL u = css.getFile().toURI().toURL();
////                    loadAuthorCss(frame, contentParent, u);
////                }
////            }
////        } catch (Exception ex) {
////            frame.printWarning("messagebar.cannot.load.css.files", ex);
////        }
////    }
//
//    private static Set<String> getStyleClasses(final URL url) throws Exception {
//        Set<String> styleClasses = new HashSet<>();
//        Stylesheet s;
//        try {
//            s = CSSParser.getInstance().parse(url);
//        } catch (IOException ex) {
//            Utils.println("Invalid Stylesheet " + url);
//            return styleClasses;
//        }
//        if (s == null) {
//            // The parsed CSS file was empty. No parsing occured.
//            return styleClasses;
//        }
//        for (Rule r : s.getRules()) {
//            for (Selector ss : r.getSelectors()) {
//                if (ss instanceof SimpleSelector) {
//                    SimpleSelector simple = (SimpleSelector) ss;
//                    styleClasses.addAll(simple.getStyleClasses());
//                } else {
//                    if (ss instanceof CompoundSelector) {
//                        CompoundSelector cs = (CompoundSelector) ss;
//                        for (Selector selector : cs.getSelectors()) {
//                            if (selector instanceof SimpleSelector) {
//                                SimpleSelector simple = (SimpleSelector) selector;
//                                styleClasses.addAll(simple.getStyleClasses());
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return styleClasses;
//    }
//
//    /*
//     * Returns the CSS property value defined by the specified style class in the style sheet.
//     * If the specifed style class is used in more than one rule, then the first one is returned.
//     */
//    public static ParsedValue<?, ?> getValueFor(Stylesheet stylesheet, String styleClass, String property) {
//        Rule rule = null;
//        for (Rule r : stylesheet.getRules()) {
//            for (Selector selector : r.getSelectors()) {
//                // Simple selector
//                if (selector instanceof SimpleSelector) {
//                    SimpleSelector simpleSelector = (SimpleSelector) selector;
//                    // Does the selector contain the style class
//                    if (simpleSelector.getStyleClasses() != null && simpleSelector.getStyleClasses().contains(styleClass)) {
//                        rule = r;
//                        break;
//                    }
//                } // Compound selector
//                else if (selector instanceof CompoundSelector) {
//                    CompoundSelector compoundSelector = (CompoundSelector) selector;
//                    for (SimpleSelector simpleSelector : compoundSelector.getSelectors()) {
//                        // Does the selector contain the style class
//                        if (simpleSelector.getStyleClasses() != null && simpleSelector.getStyleClasses().contains(styleClass)) {
//                            rule = r;
//                            break;
//                        }
//                    }
//                } else {
//                    // Should not occur
//                    throw new IllegalArgumentException("Unsupported !!!!"); //NOI18N
//                }
//            }
//        }
//        if (rule != null) {
//            for (Declaration decl : rule.getDeclarations()) {
//                if (property.equals(decl.getProperty())) {
//                    return decl.getParsedValue();
//                }
//            }
//        }
//        return null;
//    }   
//    
//    /**
//     * When a visual is dragged, we want it to be contained in a StageViewContent container
//     * Doing so, it complies with the style of the dropped content.
//     * @param visual
//     * @return The wrapped visual in a StageViewContent
//     */
//    public static Node styleDragVisual(Frame frame, Node visual) {
//        // Re-create the CSS context of the Scene.
//        Group fakeScene = new Group();
//        // XXX @see DTL-4761, styling is much more than that.
//        // We need to take into concideration drop targets (if we want to)
////        Parent p = getParent(frame);
////        fakeScene.getStylesheets().setAll(p.getStylesheets());
//        
//        fakeScene.getChildren().add(visual);
//        return fakeScene;
//    }
//
//    private static Parent getParent(Frame frame){
//        return frame.getStageView().getBackstagePane().getScenePreviewGroup();
//    }
//    
//    private static final class ContextMenuListener implements ChangeListener<ContextMenu> {
//        private final Node node;
//
//        public ContextMenuListener(Node node) {
//            this.node = node;
//        }
//        
//        @Override
//        public void changed(ObservableValue<? extends ContextMenu> arg0, ContextMenu oldValue, ContextMenu newValue) {
//            if (newValue != null) {
//                arg0.removeListener(this);
//                styleStyleable(node, newValue);
//            }
//        }
//    }
//    
//
//    private static final class SubObjectsVisitor implements ComponentPathEventHandler {
//
//        private final Node node;
//        private final Scene scene;
//        private boolean seenNonVisibleRoot;
//        private int insideNonVisibleNumLevels;
//
//        SubObjectsVisitor(Node node) {
//            this.node = node;
//            this.scene = node.getScene();
//            assert scene != null;
//        }
//
//        @Override
//        public Visit push(ComponentReference ref) {
//            // Do not visit nodes that are in the scene graph except for 
//            // the child of Skinnable (eg: TabPane)
//            // This occurs when visiting indexed childs located in the scene graph.
//            // These childs are root of visit so style their non visibles.
//            if (ref.getChildComponent() instanceof Node && ref.getChildComponent() != node) {
//                Node n = (Node) ref.getChildComponent();
//                if (n.getScene() != null) {
//                    return Visit.SKIP;
//                }
//            }
//            if (ref.getChildComponent() instanceof PopupControl
//                    || ref.getChildComponent() instanceof Menu) {
//                seenNonVisibleRoot = true;
//            }
//            if (seenNonVisibleRoot) {
//                insideNonVisibleNumLevels += 1;
//                styleObject(node, ref);
//            }
//            return Visit.DESCEND;
//        }
//
//        @Override
//        public void pop() {
//            if (insideNonVisibleNumLevels > 0) {
//                insideNonVisibleNumLevels -= 1;
//            }
//            if (insideNonVisibleNumLevels == 0) {
//                seenNonVisibleRoot = false;
//            }
//        }
//    }
//
//    /**
//     * MAIN ENTRY POINT FOR CSS RESET/APPLICATION/ERROR HANDING
//     *
//     * @param project
//     * @param e
//     */
//    public static void applyCss(final Project project, final Element e) {
//        // Project or Parent can be null for unit tests
//        if (project == null || e.getNode().getParent() == null) {
//            return;
//        }
//        
//        // Make synchronous call to processCSS to be done outside critical path.
//        // It has to be deferred, otherwise it interferes with wrap in SplitPane 
//        // Start to listen in the same tick, CSS can resolve lookup in a tick 
//        // running in between now and the following runLater.
//        startListeningToCssErrors();
//        Platform.runLater(new Runnable() {
//
//            @Override
//            public void run() {
//                doApplyCss(project, e);
//            }
//        });
//    }
//    
//    @CoverageCritical
//    private static void doApplyCss(final Project project, final Element e) {
//        try {
//            /*
//             * CSS is applied synchronously. This is required for error
//             * tracking. Otherwise we can't differentiate possible CSS errors
//             * fired by the SB css from User css.
//             */
//            resetCssState(e);
//            final Scene scene = e.getNode().getScene();
//            if (scene != null) {
//                Deprecation.processCSS(e.getNode(), true);
//            }
//            boolean projectDisplayed = project.isProjectDisplayed();
//            // Visit the remaining paths to find menu/tooltip/...
//            if (!projectDisplayed) {
//                // Do nothing: there will be a call to refreshCSS when the 
//                // project root is added to the scenegraph.
//            } else {
//                if (e.getNode() instanceof Skinnable) {
//                    final Skinnable skinnable = (Skinnable) e.getNode();
//                    if (skinnable.getSkin() == null) {
//                        assert false;
//                    }
//                }
//                if (scene == null) {
//                    // It ONLY happen for a node located inside a Custom Menu Item/ Tooltip,... We need the following logic.
//                    // At load time, for Nodes that would be located inside a skin not yet set (eg: content of a Tab, SplitPane).
//                    // The lookup is done but no styling occurs.
//                    List<ComponentPath> paths = project.lookupComponentPath(e.getNode());
//                    for (ComponentPath p : paths) {
//                        Node inSceneNode = getDeepestInSceneNode(p);
//                        for (ComponentReference ref : p.getPath()) {
//                            if (styleObject(inSceneNode, ref)) {
//                                break;
//                            }
//                        }
//                    }
//                } else {
//                    SubObjectsVisitor visitor = new SubObjectsVisitor(e.getNode());
//                    ComponentReference ref = new ComponentIndexedReference(null, e.getNode());
//                    Utils.visit(ref, visitor);
//                }
//            }
//        } catch (Throwable thr) {
//            Utils.println(thr.getMessage(), thr);
//        } finally {
//            stopListeningToCssErrors();
//        }
//    }
//    
    public static Node getFirstAncestorWithNonNullScene(Node node) {
        Node ancestor = node;
        while ((ancestor != null) && (ancestor.getScene() == null)) {
            ancestor = ancestor.getParent();
        }

        return ancestor;
    }

//    static Node getDeepestInSceneNode(ComponentPath path) {
//        Node inSceneNode = null;
//        // Lookup the deepest node in the scene.
//        for (ComponentReference ref : path.getPath()) {
//            if (ref.getChildComponent() instanceof Node) {
//                Node current = (Node) ref.getChildComponent();
//                if (current.getScene() == null) {
//                    // Stop.
//                    break;
//                } else {
//                    inSceneNode = current;
//                }
//            }
//        }
//        assert inSceneNode.getScene() != null;
//        return inSceneNode;
//    }
//    
//    private static boolean styleObject(final Node node, final ComponentReference ref) {
//        try {
//            Object obj = ref.getChildComponent();
//            Styleable styleable = obj instanceof Styleable ? (Styleable)obj : null;
//            if (styleable != null) {
//                // Can be null for Menu.
//                if ( Deprecation.getNode(styleable) != null) {
//                    styleStyleable(node, styleable);
//                    return true;
//                } else { // Workaround for Null node in MenuItem.
//                    if (ref.getChildComponent() instanceof MenuItem) {
//                        final MenuItem mi = (MenuItem) ref.getChildComponent();
//                        PopupControl pc = mi.getParentPopup();
//
//                        if (pc != null) { // can be null for Menu.
//                           styleStyleable(node, pc);
//                            return true;
//                        } else {
//                            mi.parentPopupProperty().addListener(new ContextMenuListener(node));
//                        }
//                    }
//                }
//            }
//        } catch (Throwable thr) {
//            Utils.println("Exception styling " + ref, thr); //NOI18N
//        }
//        return false;
//    }
    // Workaround null node for MenuItem and Tab.
    public static Node getNode(Object target) {
        if (target instanceof Node) {
            return (Node) target;
        }
        Styleable styleable = (target instanceof Styleable) ? (Styleable) target : null;
        Node node = null;
        if (styleable != null) {
            node = Deprecation.getNode(styleable);
            if (node == null) {
                if (target instanceof MenuItem) {
                    final MenuItem mi = (MenuItem) target;
                    PopupControl pc = mi.getParentPopup();

                    if (pc != null) { // can be null for Menu.
                        node = Deprecation.getNode(pc);
                    }
                } else {
                    if (target instanceof Tab) {
                        // Access the Skin Node
                        Tab tab = (Tab) target;
                        TabPane tp = tab.getTabPane();
                        Set<Node> tabs = tp.lookupAll(".tab"); //NOI18N
                        for (Node n : tabs) {
                            Tab result = (Tab) n.getProperties().get(Tab.class);
                            assert result != null;
                            if (result == tab) {
                                node = n;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return node;
    }

//    private static Styleable getStyleable(Object object) {
//        if(object instanceof Styleable){
//            return (Styleable) object;
//        } 
//        return null;
//    }
//    
//    private static void styleStyleable(Node ownerNode, Styleable styleable) {
//        Scene scene = ownerNode.getScene();
//        assert ownerNode.getScene() != null;
//        Parent parentNode = ownerNode.getParent();
//        styleStyleable(parentNode, styleable);
//    }
//    
//    private static void styleStyleable(Parent parent, Styleable control) {
//        Node node =  Deprecation.getNode(control);
//        if(node == null) {
//            return;
//        }
//        
//        trackNode(node);
//        double current = node.getOpacity();
//        node.setOpacity(0);
//        assert node != null;
//        try {
//            addToParent(parent, node);
//            Deprecation.processCSS(node, true);
//        } finally {
//            removeFromParent(parent, node);
//            node.setOpacity(current);
//        }
//    }
    static void addToParent(Parent p, Node node) {
        if (p instanceof Group) {
            ((Group) p).getChildren().add(node);
        } else {
            if (p instanceof Pane) {
                ((Pane) p).getChildren().add(node);
            }
        }
    }

    static void removeFromParent(Parent p, Node node) {
        if (p instanceof Group) {
            ((Group) p).getChildren().remove(node);
        } else {
            if (p instanceof Pane) {
                ((Pane) p).getChildren().remove(node);
            }
        }
    }

//    public static boolean needPageRefresh(Prop property) {
//        return CSS_IMPACT.contains(property) || Prop.isTooltip(property) || Prop.isContextMenu(property);
//    }
//    
//    private static class CssParsingListener implements ListChangeListener<CssError> {
//        @Override
//        public void onChanged(Change<? extends CssError> change) {
//            // This is fired by unwanted CSS application (FX RT or ContentView/D&D/Handles/...
//            if(!CSS_ADVERTISE) {
//                return;
//            }
//            while (change.next()) {
//                if (change.wasAdded()) {
//                    for(CssError error : change.getAddedSubList()){
//                        Project proj;
//                        Scene scene = error.getScene();
//                        if(scene == null) {
//                            // We have a single case where we asked for error and receive a null scene, this is the 
//                            // loading time case. The parsing is done before the project is associated
//                            // to any Frame (@see lookupImagesInCssFile).
//                            if(LOADING_PROJECT == null){
//                                assert Utils.isRunningUnitTests();
//                                continue;
//                            }
//                            proj = LOADING_PROJECT;
//                        } else {
//                            Frame frame = Frame.get(scene);
//                            if(frame == null) {
//                                assert Utils.isRunningUnitTests();
//                            }
//                            if(frame == null){
//                                Utils.println("No Frame to route CSS error " + error.getMessage());//NOI18N
//                                continue;
//                            }
//                            proj = frame.getProject();;
//                        }
//                        final Project project = proj;
//                        assert project != null;
//                        if(project == null){
//                            Utils.println("No Project to route CSS error " + error.getMessage());//NOI18N
//                            continue;
//                        }
//                        
//                        // CSS file added by user
//                        if(error instanceof StylesheetParsingError){
//                            StylesheetParsingError serror = (StylesheetParsingError) error;
//                            URL url = serror.getURL();
//                            if(url == null){
//                                Utils.println("No URL for Stylesheet CSS error " + error.getMessage());//NOI18N
//                                continue;
//                            }
//                            String strURL = url.toExternalForm();
//                            if(strURL.contains("com/oracle/javafx/authoring/css_stylesheets")){//NOI18N
//                                Utils.println("Error in SceneBuilder CSS " + error.getMessage());//NOI18N
//                                continue;
//                            }
//                            printMessage(project, serror);
//                        } else {                           
//                            if(error instanceof PropertySetError){// Semantic error, eg: invalid value type, unresolved lookup
//                                PropertySetError perror = (PropertySetError) error;
//                                Styleable faultyNode = perror.getStyleable();
//                                List<ComponentPath> cp = project.lookupComponentPath(faultyNode);
//                                if(cp.isEmpty()){
//                                    if(isSkinSubPart(project, faultyNode)){
//                                        printMessage(project, error);
//                                    } else {
//                                        // The node is not in the ContentView, this is a SceneBuilder CSS error.
//                                        Utils.println("Error in SceneBuilder CSS " + error.getMessage());//NOI18N
//                                        continue;
//                                    }
//                                } else {
//                                    printMessage(project, error);
//                                }
//                            } else {
//                                if(error instanceof InlineStyleParsingError){
//                                    printMessage(project, error);
//                                } else {
//                                    // Untyped CssError
//                                    printMessage(project, error);
//                                }
//                            }
//                        }
//                    }    
//                }
//            }
//        }
//    }
//    // Go up until we findout at least one ComponentPath
//    private static boolean isSkinSubPart(Project project, Styleable styleable){
//        Styleable parent = styleable.getStyleableParent();
//
//        if (parent == null) {
//            return false;
//        }
//
//        while (parent != null) {
//            List<ComponentPath> cp = project.lookupComponentPath(parent);
//            if (!cp.isEmpty()) {
//                return true;
//            }
//            parent = parent.getStyleableParent();
//        }
//        
//        return false;
//    }
//    
//    private static class CssInlineStyleListener implements ListChangeListener<CssError> {
//
//        List<CssError> errors = Utils.newList();
//
//        @Override
//        public void onChanged(ListChangeListener.Change<? extends CssError> change) {
//            StringBuilder builder = null;
//            while (change.next()) {
//                if (change.wasAdded()) {
//                    if (builder == null) {
//                        builder = new StringBuilder();
//                    }
//                    for (CssError error : change.getAddedSubList()) {
//                        if (error instanceof InlineStyleParsingError) {
//                            errors.add(error);
//                        }
//                    }
//                }
//            }
//        }
//
//        private List<CssError> getErrors() {
//            return errors;
//        }
//    }
//    
//    private static void printMessage(final Project project, final CssError error) {
//        printMessage(project, error.getMessage());
//    }
//    
//    private static void printMessage(final Project project, final StylesheetParsingError error) {
//        URL url = error.getURL();
//        String fileName = url.toExternalForm();
//        if(fileName.toLowerCase().startsWith("file:")){//NOI18N
//            fileName = new File(fileName).getName();
//        }
//        printMessage(project, fileName + " " + error.getMessage()); //NOI18N
//    }
//    
//    private static void printMessage(final Project project, final String message) {
//        if (Utils.isRunningUnitTests()) {
//            Utils.printWarning(project, message);
//        } else {
//            Platform.runLater(new Runnable() {
//                @Override
//                public void run() {
//                    Utils.printWarning(project, message);
//                }
//            });
//        }
//    }
//    
//    private static Project LOADING_PROJECT;
//    private static boolean CSS_ADVERTISE;
//    public static void startListeningToCssErrors() {
//        CSS_ADVERTISE = true;
//    }
//
//    public static void stopListeningToCssErrors() {
//        CSS_ADVERTISE = false;
//    }
//    
//    // Should be private, used by unit tests
//    static void startListeningToCssErrors(Project project) {
//        startListeningToCssErrors();
//        Frame frame = Frame.get(project);
//        if (frame == null) { // Loading the project
//            LOADING_PROJECT = project;
//        } else {
//            CssError.setCurrentScene(frame.getScene());
//        }
//    }
//    
//    // Should be private, used by unit tests
//    static void stopListeningToCssErrors(Project proj) {
//        stopListeningToCssErrors();
//        LOADING_PROJECT = null;
//        CssError.setCurrentScene(null);
//    }
//
//    private static final CssParsingListener cssListener = new CssParsingListener();
//    static {
//        StyleManager.errorsProperty().addListener(cssListener);
//    }
//    
//    public static void updateStylesheets(Project project, Parent parent, File file) {
//        startListeningToCssErrors();
//        try {
//            
//            // This is done outside any transaction.
//            // replace the file that has been updated. And only this file.
//            // Replacing the whole list fires unwanted file parsing.
//            for(int i = 0; i < parent.getStylesheets().size(); i++){
//                String url = parent.getStylesheets().get(i);
//                if(url.toLowerCase().startsWith("file:")){//NOI18N
//                    File f;
//                    try {
//                        f = new File(new URL(url).toURI());
//                    } catch (Exception ex) {
//                        Utils.println(ex.toString(), ex);
//                        continue;
//                    }
//                    if(file.equals(f)){
//                        parent.getStylesheets().remove(url);
//                        parent.getStylesheets().add(i, url);
//                    }
//                }
//            }
//            // Force CSS to apply in order to have check for validity of the stylesheets content.
//            Deprecation.processCSS(parent, true);
//        } finally {
//            stopListeningToCssErrors();
//        }
//    }
//
//    public static void loadAuthorCssFiles(Frame frame) {
//        Parent contentParent = getParent(frame);
//        // Add the root styleclass, no side effect if not in used.
//        contentParent.getStyleClass().setAll(ROOT_STYLECLASS);
//        clearFrame(frame);
//        final Project project = frame.getProject();
//        try {
//            Set<String> allImgs = Utils.newSet();
//            for (File file : project.getScreenData().getAuthorCss()) {
//                URL u = file.toURI().toURL();
//                loadAuthorCss(frame, contentParent, u);
//                Set<String> imgs = lookupImagesInCssFile(frame.getProject(), u.toURI(), false);
//                if (imgs != null) {
//                    allImgs.addAll(imgs);
//                }
//            }
//            frame.getProject().getWatcher().addCssImages(contentParent, allImgs);
//
//        } catch (Exception ex) {
//            frame.printWarning("messagebar.cannot.load.css.files", ex);
//        }
//    }
//    
//    public static void close(Frame frame) {
//        // Happens in unit tests.
//        if(frame.getStageView() == null || 
//                frame.getStageView().getBackstagePane() == null){
//            return;
//        }
//        clearFrame(frame);
//    }
//    private static void clearFrame(Frame frame){
//        Parent parent = getParent(frame);
//        parent.getStylesheets().clear();
//        STYLECLASSES_SETS.remove(parent);
//        frame.getProject().getWatcher().clearCssImages(parent);
//    }
//
//    private static void loadAuthorCss(Frame frame, Parent parent, URL cssURL) throws Exception {
//        parent.getStylesheets().add(cssURL.toExternalForm());
//        Set<String> styleClasses = getStyleClasses(cssURL);
//        List<StyleClasses> lst = STYLECLASSES_SETS.get(parent);
//        if (lst == null) {
//            lst = new ArrayList<>();
//            STYLECLASSES_SETS.put(parent, lst);
//        }
//        StyleClasses sc = new StyleClasses();
//        sc.url = cssURL.toExternalForm();
//        sc.styleClasses = styleClasses;
//        lst.add(sc);
//    }
//
//    public static Set<String> retrieveStyleClasses(Frame frame, ComponentPath path) {
//        List<StyleClasses> ret = STYLECLASSES_SETS.get(getParent(frame));
//        Set<String> fullSet = new HashSet<>();
//        if (ret != null) {
//            for (StyleClasses sc : ret) {
//                fullSet.addAll(sc.styleClasses);
//            }
//        }
//        // Compute stylesheets attached to the parent chain.
//        ComponentPath truncated = path.getNearestParentNodePath();
//        for (ComponentReference ref : truncated.getPath()) {
//            if (ref.getChildComponent() instanceof Parent) {
//                Parent p = (Parent) ref.getChildComponent();
//                for (String ss : p.getStylesheets()) {
//                    try {
//                        Set<String> classes = getStyleClasses(new URL(ss));
//                        fullSet.addAll(classes);
//                    } catch (Exception ex) {
//                        Utils.println("Can't parse Stylesheet " + ex); //NOI18N
//                    }
//                }
//            }
//        }
//        return fullSet;
//    }
//    
//    public static Set<String> retrieveAlternateStyleClasses(Class<?> type){
//        Set<String> alternates = ALTERNATE_STYLECLASSES.get(type);
//        Set<String> fullSet = new HashSet<>();
//        if(alternates != null){
//            fullSet.addAll(alternates);
//        }
//        return fullSet;
//    }
//
//    public static boolean checkStyle(final Project project, String style) {
//        if (style == null || style.equals("")) { //NOI18N
//            return true;
//        }
//        Stylesheet s = null;
//        // Synchronous listener to get errors synchronously.
//        // Required for synchronous validation
//        CssInlineStyleListener listener = new CssInlineStyleListener();
//        StyleManager.errorsProperty().addListener(listener);
//        startListeningToCssErrors(project);
//        try {
//            try {
//                s = CSSParser.getInstance().parseInlineStyle(new StyleableStub(style));
//            }catch(final RuntimeException ex){
//                // Parser exception that has not been tracked by the listener.
//                // Bug in CSS RT
//                Utils.println("Unexpected error parsing CSS style", ex); //NOI18N
//            }
//        } finally {
//            stopListeningToCssErrors(project);
//            StyleManager.errorsProperty().removeListener(listener);
//        }
//        
//        return s != null && listener.getErrors().isEmpty();
//    }
//    
//    public static void addStyleSheet(Frame frame){
//        FileChooserWrapper fileChooser = FileChooserWrapper.create().
//                extensionFilters(SS_EXTENSIONS).
//                title(Utils.getI18N().getString("window.title.add.style.sheet")).build();
//        File f = fileChooser.showOpenDialog(frame.getStage());
//        if(f!= null){
//            Project p = frame.getProject();
//            List<File> files = p.getStylesheets();
//            if(!files.contains(f)){
//                List<File> newLst = Utils.newList();
//                newLst.addAll(files);
//                newLst.add(f);
//                p.setStylesheets(newLst);
//            }
//        }
//    }
//    
//    public static void removeStyleSheet(Frame frame, File f) {
//        assert f != null;
//        Project p = frame.getProject();
//        List<File> files = p.getStylesheets();
//        List<File> newLst = Utils.newList();
//        newLst.addAll(files);
//        newLst.remove(f);
//        p.setStylesheets(newLst);
//    }
//    
//    private static final Set<String> IMG_PROPERTIES = Utils.newSet();
//    static {
//        // String
//        IMG_PROPERTIES.add("-fx-image");//NOI18N
//        // String
//        IMG_PROPERTIES.add("-fx-graphic");//NOI18N
//        // String[]
//        IMG_PROPERTIES.add("-fx-background-image");//NOI18N
//        // String[]
//        IMG_PROPERTIES.add("-fx-border-image-source");//NOI18N
//    }
//    
//    private static class StyleableStub implements Styleable {
//        private final String style;
//        private StyleableStub(String style){
//            this.style = style;
//        }
//        @Override 
//        public String getTypeSelector() {
//            return null;
//        }
//        
//        @Override
//        public String getId() {
//            return null;
//        }
//
//        @Override
//        public ObservableList<String> getStyleClass() {
//            return new SimpleListProperty<>();
//        }
//
//        @Override
//        public String getStyle() {
//            return style;
//        }
//
//        @Override
//        public Styleable getStyleableParent() {
//            return null;
//        }
//
//        @Override
//        @SuppressWarnings("rawtypes")
//        public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
//            return Collections.emptyList();
//        }
//
//        @Override
//        public ObservableSet<PseudoClass> getPseudoClassStates() {
//            return null; // TODO Return something useful
//        }
//    }
//    
//    public static Set<String> lookupImagesInStyle(String style) {
//        if (style != null) {
//            try {
//                Stylesheet s = CSSParser.getInstance().parseInlineStyle(new StyleableStub(style));
//                return lookupImagesInStylesheet(s);
//            } catch (RuntimeException ex) {
//                Utils.println(ex.getMessage());
//            }
//        }
//        return null;
//    }
//    
//    public static Set<String> lookupImagesInCssFile(Project project, URI uri, boolean isRemoval) {
//        if (uri != null) {
//            if (!isRemoval) {
//                startListeningToCssErrors(project);
//            }
//            try {
//                Stylesheet s = CSSParser.getInstance().parse(uri.toURL());
//                return lookupImagesInStylesheet(s);
//            } catch (Exception ex) {
//                Utils.println(ex.getMessage());
//            } finally {
//                if (!isRemoval) {
//                    stopListeningToCssErrors(project);
//                }
//            }
//        }
//        return null;
//    }
//    
//    private static Set<String> lookupImagesInStylesheet(Stylesheet s) {
//        Set<String> files = Utils.newSet();
//        try {
//            for (Rule r : s.getRules()) {
//                for (Declaration d : r.getDeclarations()) {
//                    if (IMG_PROPERTIES.contains(d.getProperty())) {
//                        Object obj = d.getParsedValue().convert(null);
//                        if (obj instanceof String) {
//                            try {
//                                files.add((String) obj);
//                            } catch (Exception ex) {
//                                Utils.println((String) obj, ex);
//                            }
//                        } else {
//                            if (obj instanceof String[]) {
//                                String[] array = (String[]) obj;
//                                for (String a : array) {
//                                    try {
//                                        files.add(a);
//                                    } catch (Exception ex) {
//                                        Utils.println(a, ex);
//                                    }
//                                }
//                            } else {
//                                Utils.println("Unknown type for CSS Img value " + obj);//NOI18N
//                            }
//                        }
//                    }
//                }
//            }
//        } catch (RuntimeException ex) {
//            Utils.println(ex.getMessage());//NOI18N
//        }
//        return files;
//    }
//
//    public static void workaroundForRT23223(Node node) {
//        if (Utils.IS_WINDOWS_XP) {
//            node.getStyleClass().add(CssUtils.SCENE_BUILDER_WINXP_FONT_STYLECLASS);
//        } else {
//            if(Utils.IS_WINDOWS){
//                node.getStyleClass().add(CssUtils.SCENE_BUILDER_WIN_FONT_STYLECLASS);
//            } // else fallback on platform font size.
//        }
//    }
//    
//    // Only applies to Windows 7 and Vista, XP keep its 11px menu.
//    public static void workaroundForRT19435(MenuItem menu){
//        if (Utils.IS_WINDOWS_7 || Utils.IS_WINDOWS_VISTA) {
//            menu.setStyle("-fx-font-size: 1.083em;");//NOI18N
//        } 
//    }
//    
//    
//    /**
//     * Returns the first stylable node above the specified node inside the
//     * specified component.
//     * For now, this routine returns the node itself except when it is a Skin
//     * node : in that case, the routine the associated skinnable node.
//     */
//    public static Node findEnclosingStylableNode(ComponentPath pickedPath, Node pickedNode) {
//        final Node result;
//        
//        assert pickedPath != null;
//        assert pickedNode != null;
//        
//        if ((pickedNode == pickedPath.getTargetChild()) || (pickedNode instanceof Skin)) {
//            // pickedNode matches the component or the Skin node
//            result = null;
//        } else {
//            result = pickedNode;
//        }
//        
//        return result;
//    }
    
    public static Object getSceneGraphObject(Object selectedObject) {
        if (selectedObject instanceof FXOMObject) {
            return ((FXOMObject) selectedObject).getSceneGraphObject();
        } else {
            return selectedObject;
        }
    }
    
    public static Node getSelectedNode(Object selectedObject) {
        return CssUtils.getNode(CssUtils.getSceneGraphObject(selectedObject));
    }

}
