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

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssContentMaker.BeanPropertyState;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssContentMaker.CssPropertyState;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssContentMaker.CssPropertyState.CssStyle;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssContentMaker.NodeCssState;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssContentMaker.NodeCssState.CssProperty;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssContentMaker.PropertyState;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssValuePresenterFactory.CssValuePresenter;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.SelectionPath.Item;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.InspectorPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlPanelController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.SelectionPath.Path;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.CssInternal;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import com.sun.javafx.css.ParsedValueImpl;
import com.sun.javafx.css.Rule;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Callable;
import javafx.animation.FadeTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.ParsedValue;
import javafx.css.PseudoClass;
import javafx.css.StyleOrigin;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.Duration;

/**
 * Controller for the CSS Panel.
 *
 */
public class CssPanelController extends AbstractFxmlPanelController {

    @FXML
    private StackPane cssPanelHost;

    @FXML
    private StackPane cssSearchPanelHost;

    @FXML
    private TableColumn<CssProperty, CssProperty> beanApiColumn;
    @FXML
    private TableColumn<CssProperty, CssProperty> builtinColumn;
    @FXML
    private TableColumn<CssProperty, CssProperty> fxThemeColumn;
    @FXML
    private TableColumn<CssProperty, CssProperty> inlineColumn;
    @FXML
    private TableColumn<CssProperty, CssProperty> propertiesColumn;
    @FXML
    private TableColumn<CssProperty, CssProperty> defaultColumn;
    @FXML
    private ToggleButton pick;
    @FXML
    private ToggleButton edit;
    @FXML
    SelectionPath selectionPath;
    @FXML
    private VBox root;
    @FXML
    private VBox header;
    @FXML
    WebView textPane;
    @FXML
    private VBox rulesBox;
    @FXML
    private ScrollPane rulesPane;
    @FXML
    private TableColumn<CssProperty, CssProperty> stylesheetsColumn;
    @FXML
    private TableView<CssProperty> table;

    private TreeView<Node> rulesTree;

    private boolean advanced = false;
    private boolean styledOnly = false;

    private ObservableList<CssProperty> model;
    private ObservableList<TableColumn<CssProperty, ?>> initialOrder;
    private ObservableList<TableColumn<CssProperty, ?>> reversedOrder;

    private View currentView = View.TABLE;

    private StackPane message;

    private String searchPattern;
    private static final Image cogIcon = new Image(
            InspectorPanelController.class.getResource("images/cog.png").toExternalForm()); //NOI18N
    private static final Image lookups = new Image(
            CssPanelController.class.getResource("images/css-lookup-icon.png").toExternalForm()); //NOI18N
    private static final Image pickImg = new Image(
            CssPanelController.class.getResource("images/css-cursor.png").toExternalForm()); //NOI18N
    private static final Image editImg = new Image(
            CssPanelController.class.getResource("images/css-button-arrow.png").toExternalForm()); //NOI18N

    private static final String NO_MATCHING_RULES = I18N.getString("csspanel.no.matching.rule");

    public enum View {

        TABLE, RULES, TEXT;
    }

    private Object selectedObject; // Can be either an FXOMObject (selection mode), or a Node (pick mode)
    private Selection selection;
    @SuppressWarnings("rawtypes")
    private Callable onSelectionModeSwitch;
    private final EditorController editorController;
    private final Delegate applicationDelegate;
    private final ObjectProperty<NodeCssState> cssStateProperty = new SimpleObjectProperty<>();

    /*
     * Should be implemented by the application
     */
    public static abstract class Delegate {

        public abstract void revealInspectorEditor(ValuePropertyMetadata propMeta);
    }

    /*
     * AbstractPanelController
     */
    @Override
    protected void fxomDocumentDidChange(FXOMDocument oldDocument) {
        if (isCssPanelLoaded() && hasFxomDocument()) {
            selection = editorController.getSelection();
            refresh();
        }
    }

    @Override
    protected void sceneGraphRevisionDidChange() {
        if (isCssPanelLoaded() && hasFxomDocument()) {
            refresh();
        }
    }

    @Override
    protected void jobManagerRevisionDidChange() {
        // FXOMDocument has been modified by a job.
        // getEditorController().getJobManager().getLastJob()
        // is the job responsible of the change.
        // Since sceneGraphRevisionDidChange() will be called in this case, nothing to do here.
    }

    @Override
    protected void editorSelectionDidChange() {
        if (isCssPanelLoaded() && hasFxomDocument()) {
            selection = editorController.getSelection();
            if (!isMultipleSelection()) {
                if (isPickMode()) {
                    selectedObject = selection.findHitNode();
                } else {
                    selectedObject = getFXOMInstance(selection);
                }
            }
            refresh();
        }
    }

    /*
     * AbstractFxmlPanelController
     */
    @Override
    protected void controllerDidLoadFxml() {
        // Remove scrollPane for rules
        root.getChildren().remove(rulesPane);
        root.getChildren().remove(textPane);
        root.getChildren().remove(table);

        pick.setGraphic(new ImageView(pickImg));
        pick.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                editorController.setPickModeEnabled(true);
            }
        });
        edit.setGraphic(new ImageView(editImg));
        edit.setSelected(true);
        editorController.pickModeEnabledProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) {
                setPickMode(newVal);
            }
        });
        
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        initialOrder = FXCollections.observableArrayList(table.getColumns());
        reversedOrder = FXCollections.observableArrayList(table.getColumns());
        FXCollections.reverse(reversedOrder);

        disableColumnReordering();
        final Callback<TableColumn.CellDataFeatures<CssProperty, CssProperty>, ObservableValue<CssProperty>> valueFactory
                = new ValueFactory();

        propertiesColumn.setCellValueFactory(valueFactory);
        propertiesColumn.setCellFactory(new PropertiesCellFactory());

        builtinColumn.setCellValueFactory(valueFactory);
        builtinColumn.setCellFactory(new BuiltinCellFactory());

        fxThemeColumn.setCellValueFactory(valueFactory);
        fxThemeColumn.setCellFactory(new FxThemeCellFactory());

        beanApiColumn.setCellValueFactory(valueFactory);
        beanApiColumn.setCellFactory(new ModelCellFactory());

        stylesheetsColumn.setCellValueFactory(valueFactory);
        stylesheetsColumn.setCellFactory(new AuthorCellFactory());

        inlineColumn.setCellValueFactory(valueFactory);
        inlineColumn.setCellFactory(new InlineCellFactory());

        defaultColumn.setCellValueFactory(valueFactory);
        defaultColumn.setCellFactory(new DefaultCellFactory());

        cssStateProperty.addListener(new ChangeListener<NodeCssState>() {
            @Override
            public void changed(ObservableValue<? extends NodeCssState> arg0, NodeCssState oldValue, NodeCssState newValue) {
                fillPropertiesTable();
            }
        });

        ChangeListener<Item> selectionListener = new ChangeListener<Item>() {
            @Override
            public void changed(ObservableValue<? extends Item> arg0, Item oldvalue, Item newValue) {
                if (newValue != null && newValue.getItem() != null) {
                    Node selectedSubNode = CssUtils.getNode(newValue.getItem());
                    selectedObject = selectedSubNode;
                    refresh();
                    // TODO: use the hit point method from Eric
                    selection.select(getFXOMInstance(selection), Point2D.ZERO);
                    // Switch to pick mode
                    setPickMode(true);
                }
            }
        };
        selectionPath.selected().addListener(selectionListener);

        selectedObject = getFXOMInstance(selection);
        // View table by default
        changeView(CssPanelController.View.TABLE);

        editorSelectionDidChange();
    }

    private static class ValueFactory implements Callback<TableColumn.CellDataFeatures<CssProperty, CssProperty>, ObservableValue<CssProperty>> {

        @Override
        public ObservableValue<CssProperty> call(TableColumn.CellDataFeatures<CssProperty, CssProperty> param) {
            ObjectProperty<CssProperty> val = new SimpleObjectProperty<>();
            val.setValue(param.getValue());
            return val;
        }
    }

    private static class PropertiesCellFactory implements Callback<TableColumn<CssProperty, CssProperty>, TableCell<CssProperty, CssProperty>> {

        @Override
        public TableCell<CssProperty, CssProperty> call(TableColumn<CssProperty, CssProperty> param) {
            return new CssPropertyTableCell();
        }
    }

    private class BuiltinCellFactory implements Callback<TableColumn<CssProperty, CssProperty>, TableCell<CssProperty, CssProperty>> {

        @Override
        public TableCell<CssProperty, CssProperty> call(TableColumn<CssProperty, CssProperty> param) {
            return new BuiltinValueTableCell();
        }
    }

    private class FxThemeCellFactory implements Callback<TableColumn<CssProperty, CssProperty>, TableCell<CssProperty, CssProperty>> {

        @Override
        public TableCell<CssProperty, CssProperty> call(TableColumn<CssProperty, CssProperty> param) {
            return new FxThemeValueTableCell();
        }
    }

    private class ModelCellFactory implements Callback<TableColumn<CssProperty, CssProperty>, TableCell<CssProperty, CssProperty>> {

        @Override
        public TableCell<CssProperty, CssProperty> call(TableColumn<CssProperty, CssProperty> param) {
            return new ModelValueTableCell();
        }
    }

    private class AuthorCellFactory implements Callback<TableColumn<CssProperty, CssProperty>, TableCell<CssProperty, CssProperty>> {

        @Override
        public TableCell<CssProperty, CssProperty> call(TableColumn<CssProperty, CssProperty> param) {
            return new AuthorValueTableCell();
        }
    }

    private class InlineCellFactory implements Callback<TableColumn<CssProperty, CssProperty>, TableCell<CssProperty, CssProperty>> {

        @Override
        public TableCell<CssProperty, CssProperty> call(TableColumn<CssProperty, CssProperty> param) {
            return new InlineValueTableCell();
        }
    }

    private class DefaultCellFactory implements Callback<TableColumn<CssProperty, CssProperty>, TableCell<CssProperty, CssProperty>> {

        @Override
        public TableCell<CssProperty, CssProperty> call(TableColumn<CssProperty, CssProperty> param) {
            return new DefaultValueTableCell();
        }
    }

    /*
     *
     * Public
     *
     */
    public CssPanelController(EditorController c, Delegate delegate) {
        super(CssPanelController.class.getResource("CssPanel.fxml"), I18N.getBundle(), c);
        this.editorController = c;
        this.applicationDelegate = delegate;
    }

    public String getSearchPattern() {
        return searchPattern;
    }

    public void setSearchPattern(String searchPattern) {
        this.searchPattern = searchPattern;
        searchPatternDidChange();
    }

    public void addSelectionListener(ChangeListener<Item> selectionListener) {
        selectionPath.selected().addListener(selectionListener);
    }

    public void setSelectionPath(Path path) {
        selectionPath.setSelectionPath(path);
    }

    public void viewMessage(String mess) {
        root.getChildren().removeAll(message, header, table, rulesPane, textPane);
//        mainMenu.setDisable(true);
//        searchBox.setDisable(true);
        message = new StackPane();
        message.getChildren().add(new Label(mess));
        root.getChildren().add(message);
    }

    public final Node getRulesPane() {
        return rulesPane;
    }

    public final Node getTextPane() {
        return textPane;
    }

    public void setContent(ObservableList<CssProperty> model, NodeCssState state) {
        changeView(currentView);
        initializeRulesTextPanes(state);
        this.model = FXCollections.observableArrayList(model);
        updateTable(model);
    }

    public void clearContent() {
        table.getItems().clear();
    }

    public void filter(String pattern) {
        ObservableList<CssProperty> filtered;
        if (pattern == null || pattern.trim().length() == 0) {
            filtered = model;
        } else {
            filtered = FXCollections.observableArrayList();
            for (CssProperty p : model) {
                if (p.propertyName().get().contains(pattern.trim())) {
                    filtered.add(p);
                }
            }
        }
        updateTable(filtered);
    }

    public static void attachStyleProperty(Object component, TreeItem<Node> parent, CssPropertyState cssProp, CssStyle style,
            boolean applied, boolean isLookup) {
        if (isLookup) {
            String cssValue = CssValueConverter.toCssString(style.getCssProperty(), style.getCssRule(), style.getParsedValue());
            TreeItem<Node> item = new TreeItem<>(getContent(component, style.getCssProperty(), cssValue, style.getParsedValue(), applied));
            parent.getChildren().add(item);
        } else {
            attachStylePropertyNoLookup(component, parent, cssProp, style, applied);
        }
    }

    @SuppressWarnings("rawtypes")
    public void setOnSelectionModeSwitch(Callable callable) {
        onSelectionModeSwitch = callable;
    }

    public void selectionModeSwitch() {
        // Switch Content View selection mode (css/standard)
        try {
            onSelectionModeSwitch.call();
        } catch (Exception ex) {
            System.out.println("Cannot switch selection mode. " + ex);
        }

    }

    public void changeView(View view) {
        switch (view) {
            case TABLE: {
                root.getChildren().removeAll(message, header, table, rulesPane, textPane);
                root.getChildren().addAll(header, table);
                break;
            }
            case RULES: {
                root.getChildren().removeAll(message, header, rulesPane, table, textPane);
                root.getChildren().addAll(header, rulesPane);
                break;
            }
            case TEXT: {
                root.getChildren().removeAll(message, header, textPane, table, rulesPane);
                root.getChildren().addAll(header, textPane);
                break;
            }
        }
        currentView = view;
    }

    public void copyStyleablePath() {
        final ClipboardContent content = new ClipboardContent();
        content.putString(selectionPath.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }

    public void splitDefaultsAction() {
        advanced = !advanced;
        unmerge();
    }

    public void showStyledOnly() {
        styledOnly = !styledOnly;
        showStyled(model);
    }

    /*
     *
     * FXML methods
     *
     */
    public void initialize() {

    }

    @FXML
    void onSelectionModeSwitchAction(ActionEvent event) {
        selectionModeSwitch();
    }

    /*
     *
     * Private
     *
     */
    private void refresh() {
        setCSSContent();
    }

    private static String getFirstStandardClassName(final Class<?> type) {
        Class<?> clazz = type;
        while (clazz != null) {
            if (clazz.getName().startsWith("javafx")) {//NOI18N
                return clazz.getSimpleName();
            }
            clazz = clazz.getSuperclass();
        }
        return type.getName();
    }

    private void collectCss() {
        if (selectedObject != null) {
            NodeCssState state = CssContentMaker.getCssState(selectedObject);
            cssStateProperty.setValue(state);
        }
    }

    private void setCSSContent() {
        if (selectedObject instanceof Skinnable) {
            if (((Skinnable) selectedObject).getSkin() == null) {
                return;
            }
        }

        clearContent();
        if (isMultipleSelection()) {
            viewMessage(I18N.getString("csspanel.multiselection"));
        }

        if (selectedObject != null) { // Update content.
            fillSelectionContent();
            collectCss();
        }
    }

    private boolean isMultipleSelection() {
        if (selection.getGroup() instanceof ObjectSelectionGroup) {
            return ((ObjectSelectionGroup) selection.getGroup()).getItems().size() > 1;
        } else {
            // GridSelectionGroup: consider the GridPane only
            return false;
        }
    }

    private boolean isCssPanelLoaded() {
        return root != null;
    }

    private boolean hasFxomDocument() {
        return getEditorController().getFxomDocument() != null;
    }

    private Node getSelectedRootNode() {
        Object selObject = getFXOMInstance(selection).getSceneGraphObject();
        // TODO : non-node handling
        assert selObject instanceof Node;
        return (Node) selObject;
    }
    
    private boolean isPickMode() {
        return pick.isSelected();
    }

    private void setPickMode(boolean pickMode) {
        pick.setSelected(pickMode);
    }

    private void fillSelectionContent() {
        assert selectedObject != null;
        // Start from the Component;
        Node componentRootNode = getSelectedRootNode();
        Item rootItem = new Item(componentRootNode, createItemName(componentRootNode), createOptional(componentRootNode));//NOI18N

        // Seems we can skip the skin now, which is not in the scene graph anymore.
//        if (componentRootNode instanceof Skinnable) {
//            assert ((Skinnable) componentRootNode).getSkin() != null;
//            Node skinNode = ((Skinnable) componentRootNode).getSkin().getNode();
//            if (skinNode instanceof Parent) {
//                addSubStructure(componentRootNode, rootItem, (Parent) skinNode);
//            }
//        } else {
        if (componentRootNode instanceof Parent) {
            addSubStructure(componentRootNode, rootItem, (Parent) componentRootNode);
        }
//        }
        Object sceneGraphObject = CssUtils.getSceneGraphObject(selectedObject);
        List<Item> items = SelectionPath.lookupPath(rootItem, sceneGraphObject);
        setSelectionPath(new Path(items));
    }

    private void addSubStructure(Node componentRootNode, Item parentItem, Node node) {
        FXOMDocument fxomDoc = getEditorController().getFxomDocument();
        assert fxomDoc != null;
        Node enclosingNode = getEnclosingNode(fxomDoc, node);
        // The componentRootNode can be a skin structure (Tab, Column), in this case the enclosingNode 
        // is not == to the componentRootNode. That is why we need to compare the enclosingNode of both
        // n and componentRootNode nodes.
        Node componentRootNodeEnclosingNode = getEnclosingNode(fxomDoc, componentRootNode);
        // this is a skin's node and not a node from a component located inside 
        // the skin (eg: SplitPane content being a Button is not part of the SplitPane Skin.
        boolean isOtherComponentNode = enclosingNode != componentRootNodeEnclosingNode;
        if (componentRootNode != node && !node.getStyleClass().isEmpty() && !isOtherComponentNode && !(node instanceof Skin)) {
            Item ni = new Item(node, createItemName(node), createOptional(node));//NOI18N
            parentItem.getChildren().add(ni);
            parentItem = ni;
        }
        if (node instanceof Parent && !isOtherComponentNode) {
            Parent parentNode = (Parent) node;
            for (Node child : parentNode.getChildrenUnmodifiable()) {
                addSubStructure(componentRootNode, parentItem, child);
            }
        }
    }

    private Node getEnclosingNode(FXOMDocument fxomDoc, Node n) {
        Node node = n;
        FXOMObject enclosingFXOMObj = fxomDoc.searchWithSceneGraphObject(node);
        while (enclosingFXOMObj == null) {
            node = node.getParent();
            if (node == null) {
                return null;
            }
            enclosingFXOMObj = fxomDoc.searchWithSceneGraphObject(node);
        }
        Object enclosingObj = enclosingFXOMObj.getSceneGraphObject();
        assert enclosingObj instanceof Node;
        return (Node) enclosingObj;
    }

    private void fillPropertiesTable() {
        NodeCssState state = cssStateProperty.getValue();
        fillContent(state);
        filter(searchPattern);
    }

    private void fillContent(NodeCssState state) {
        ObservableList<CssProperty> cssModel = FXCollections.observableArrayList();
        Collection<CssProperty> styleables = state.getAllStyleables();
        for (CssProperty sp : styleables) {
            cssModel.add(sp);
            for (CssProperty sub : sp.getSubProperties()) {
                cssModel.add(sub);
            }
        }
        setContent(cssModel, state);
    }

    private void attachNotAppliedStyles(TreeItem<Node> ti, PropertyState css) {
        for (CssStyle style : css.getNotAppliedStyles()) {
            attachStyle(css, style, ti, false);
        }
    }

    private void attachSubProperties(TreeItem<Node> parent, PropertyState ss) {
        for (PropertyState sub : ss.getSubProperties()) {
            attachProperty(parent, sub);
        }
        attachNotAppliedStyles(parent, ss);
    }

    private void attachProperty(TreeItem<Node> parent, PropertyState ss) {
        boolean hasSubs = !ss.getSubProperties().isEmpty();
        if (hasSubs) {
            if (ss instanceof CssPropertyState) {
                CssPropertyState cssProp = (CssPropertyState) ss;
                if (cssProp.getStyle() != null) {
                    // Need to add the container, not the sub properties
                    Node content = getContent(selectedObject, ss.getCssProperty(), ss.getCssValue(), ss.getFxValue(), true);
                    TreeItem<Node> ti = newTreeItem(content, ss);
                    parent.getChildren().add(ti);
                    attachStyles(cssProp, ti);
                    attachNotAppliedStyles(parent, ss);
                } else {
                    attachSubProperties(parent, ss);
                }
            } else {
                attachSubProperties(parent, ss);
            }
        } else {
            Node content = getContent(selectedObject, ss.getCssProperty(), ss.getCssValue(), ss.getFxValue(), true);
            TreeItem<Node> ti = newTreeItem(content, ss);
            parent.getChildren().add(ti);
            if (ss instanceof CssPropertyState) {
                CssPropertyState css = (CssPropertyState) ss;
                attachStyles(css, ti);
            } else {
                if (ss instanceof BeanPropertyState) {
                    BeanPropertyState beanProp = (BeanPropertyState) ss;
                    String source = beanProp.getPropertyMeta().getName().toString();
                    StringBuilder contentBuilder = new StringBuilder();
                    contentBuilder.append(source);
                    ti.getChildren().add(newTreeItem(new Label(contentBuilder.toString()), ss));
                }
            }
            attachNotAppliedStyles(ti, ss);
        }
    }

    private void searchPatternDidChange() {
        filter(searchPattern);
    }

    private void updateTable(ObservableList<CssProperty> currentModel) {
        showStyled(currentModel);
        unmerge();
    }

    private void disableColumnReordering() {
        for (TableColumn<CssProperty, ?> column : table.getColumns()) {
            Deprecation.setTableColumnReordable(column, false);
        }
    }

    private void initializeRulesTextPanes(NodeCssState state) {
        rulesBox.getChildren().clear();
        List<CssContentMaker.NodeCssState.MatchingRule> rulesList = state.getMatchingRules();
        HtmlStyler htmlStyler = new HtmlStyler();
        rulesTree = new TreeView<>();
        CopyHandler.attachContextMenu(rulesTree);
        rulesTree.setShowRoot(false);
        TreeItem<Node> ruleRoot = new TreeItem<>(new Text(""));//NOI18N
        rulesTree.setRoot(ruleRoot);
        for (CssContentMaker.NodeCssState.MatchingRule rule : rulesList) {
            List<CssContentMaker.NodeCssState.MatchingDeclaration> lst = rule.getDeclarations();

            String selector = rule.getSelector();

            String txt = selector + " { ";//NOI18N
            String source = nonNull(getSource(rule.getRule()));
            Text text = CopyHandler.makeCopyableNode(new Text(txt + source), txt);
            TreeItem<Node> start = new TreeItem<>(text);
            ruleRoot.getChildren().add(start);
            htmlStyler.cssRuleStart(selector, source);

            for (CssContentMaker.NodeCssState.MatchingDeclaration p : lst) {
                CssPropertyState prop = p.getProp();
                attachStyleProperty(state.getNode(), ruleRoot, prop, p.getStyle(), p.isApplied(), p.isLookup());
                CssStyle style = p.getStyle();
                String cssValue = CssValueConverter.toCssString(style.getCssProperty(), style.getCssRule(), style.getParsedValue());
                htmlStyler.addProperty(p.getStyle().getCssProperty(), cssValue, p.isApplied());
            }
            TreeItem<Node> end = new TreeItem<>(CopyHandler.createCopyableText("}"));//NOI18N
            ruleRoot.getChildren().add(end);
            setTreeHeight(rulesTree);
            htmlStyler.cssRuleEnd();
        }
        if (ruleRoot.getChildren().isEmpty()) {
            rulesBox.getChildren().add(new Label(NO_MATCHING_RULES));
        } else {
            rulesBox.getChildren().add(rulesTree);
        }
        if (htmlStyler.isEmpty()) {
            htmlStyler.addMessage(NO_MATCHING_RULES);
        }
        textPane.getEngine().loadContent(htmlStyler.getHtmlString());
    }

    void close(ChangeListener<Item> selectionListener) {
        selectionPath.selected().removeListener(selectionListener);
    }

    private void unmerge() {
        defaultColumn.setVisible(!advanced);
        fxThemeColumn.setVisible(advanced);
        builtinColumn.setVisible(advanced);
    }

    private void showStyled(ObservableList<CssProperty> currentModel) {
        if (styledOnly) {
            currentModel = extractStyled(currentModel);
        }
        table.getItems().setAll(currentModel);
    }

    private static ObservableList<CssProperty> extractStyled(ObservableList<CssProperty> currentModel) {
        ObservableList<CssProperty> ret = FXCollections.observableArrayList();
        for (CssProperty prop : currentModel) {
            if (prop.isAuthorSource() || prop.isInlineSource() || prop.isModelSource()) {
                ret.add(prop);
            }
        }
        return ret;
    }

    private static String nodeIdentifier(Node n) {
        if (n.getId() != null && !n.getId().equals("")) {//NOI18N
            return n.getId();
        } else {
            return n.getClass().getSimpleName();
        }
    }

    /*
     *
     * TABLE CELLS CONTENT
     *
     */
    // "Properties" column
    private static class CssPropertyTableCell extends TableCell<CssProperty, CssProperty> {

        CssPropertyTableCell() {
            getStyleClass().add("property-background");//NOI18N
        }

        @Override
        public void updateItem(final CssProperty item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                Hyperlink hl = new Hyperlink();
                hl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                hl.setOnAction(new LinkActionListener(item));
                hl.setAlignment(Pos.CENTER_LEFT);
                if (item.getMainProperty() != null) {
                    hl.setText("     " + item.propertyName().get());//NOI18N
                } else {
                    hl.setText(item.propertyName().get());
                }
                setGraphic(hl);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        }
    }

    private static class LinkActionListener implements EventHandler<ActionEvent> {

        final CssProperty item;

        public LinkActionListener(CssProperty item) {
            this.item = item;
        }

        @Override
        public void handle(ActionEvent event) {
            try {
                // XXX jfdenise, for now can't do better than opening the file, no Anchor per property...
                // Retrieve defining class
                EditorPlatform.open("http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html#" + //NOI18N
                        item.getTarget().getClass().getSimpleName().toLowerCase(Locale.ROOT));
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    // Css value cell content
    private abstract class CssValueTableCell extends TableCell<CssProperty, CssProperty> {

        private final List<Value> values = new ArrayList<>();
        private VBox valueBox;
        private MenuButton navigationMenuButton;
        private FadeTransition fadeTransition;
        private final MenuItem revealInInspectorMenuItem = new MenuItem(I18N.getString("csspanel.reveal.inspector"));
        private final MenuItem revealInFileBrowserMenuItem = new MenuItem();
        private MenuItem openStylesheetMenuItem = new MenuItem();

        // TODO : UI should be in an fxml file
        private class Value extends AnchorPane {

            private final VBox vbox;
            private Label sourceLabel;
            private MenuButton navigationMenuButton;

            private Value(Node value) {
                vbox = new VBox(2);
                getStyleClass().add("value");//NOI18N
                setAlignment(Pos.CENTER_LEFT);
                vbox.getChildren().add(value);
                getChildren().add(vbox);
                AnchorPane.setTopAnchor(vbox, 0.0);
                AnchorPane.setLeftAnchor(vbox, 0.0);
                AnchorPane.setRightAnchor(vbox, 0.0);
                setOnMouseEntered(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent arg0) {
                        if ((navigationMenuButton != null) && !navigationMenuButton.isShowing()) {
                            fadeMenuButtonTo(1);
                        }
                    }
                });
                setOnMouseExited(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent arg0) {
                        if ((navigationMenuButton != null) && !navigationMenuButton.isShowing()) {
                            fadeMenuButtonTo(0);
                        }
                    }
                });
            }

            private void setSource(Label sourceLabel) {
                this.sourceLabel = sourceLabel;
                vbox.getChildren().add(sourceLabel);
            }

            private void setNavigation(Label navigationLabel, MenuButton navigationMenuButton) {
                this.navigationMenuButton = navigationMenuButton;
                vbox.getChildren().add(navigationLabel);
                if (navigationMenuButton != null) {
                    getChildren().add(navigationMenuButton);
                    AnchorPane.setTopAnchor(navigationMenuButton, 0.0);
                    AnchorPane.setRightAnchor(navigationMenuButton, 0.0);
                }
            }

            private void showSource(boolean show) {
                if (sourceLabel != null) {
                    sourceLabel.setVisible(show);
                }
                if (navigationMenuButton != null) {
                    navigationMenuButton.setVisible(show);
                }
            }

            private void fadeMenuButtonTo(double toValue) {
                fadeTransition.stop();
                fadeTransition.setFromValue(navigationMenuButton.getOpacity());
                fadeTransition.setToValue(toValue);
                fadeTransition.play();
            }
        }

        private CssValueTableCell() {
            //  showSources.selectedProperty().addListener(new WeakChangeListener<>(sourceListener));
        }

        /**
         * WARNING: TableCell instances are reused by TableView. It must be
         * stateless. In our case, value, sourceLabel and navigationLabel MUST
         * be cleared each time updateItem is called.
         *
         * @param item
         * @param empty
         */
        @Override
        public void updateItem(CssProperty item, boolean empty) {
            super.updateItem(item, empty);
            values.clear();
            setGraphic(null);
            if (!empty) {
                if (getStyle(item) != null && !getStyle(item).isUsed()) {//eg: -fx-backgroundfills
                    return;
                }
                // A new node MUST be constructed on each call, otherwise TableView looses the UI<->model relationship
                valueBox = new VBox(2);
                Value currentValue;
                valueBox.setAlignment(Pos.CENTER);
                PropertyState cssState = getPropertyState(item);
                if (cssState != null) {
                    Node n;
                    n = createValueUI(item, cssState, cssState.getFxValue(), getStyle(item));
                    if (n == null) {
                        Label label = new Label(cssState.getCssValue());
                        n = label;
                    }
                    currentValue = new Value(n);
                    if (isWinner(item)) {
                        currentValue.getStyleClass().add("winner-background");//NOI18N
                    }
                    values.add(currentValue);
                    handleSource(currentValue, item, getStyle(item));
                }

                handleNotApplied(item);

                displayValues();
            }
        }

        private void displayValues() {
            for (Value v : values) {
                VBox.setVgrow(v, Priority.ALWAYS);
                valueBox.getChildren().add(v);
            }
            setGraphic(valueBox);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        protected abstract PropertyState getPropertyState(CssProperty item);

        protected abstract CssStyle getStyle(CssProperty item);

        protected abstract boolean isWinner(CssProperty item);

        protected abstract StyleOrigin getOrigin(CssProperty item);

        protected String getNavigation(CssProperty item, CssStyle style) {
            return getNavigationInfo(item, style, getOrigin(item));
        }

        private void handleNotApplied(CssProperty item) {
            CssPropertyState ps = item.getWinner();
            if (ps != null) {
                for (CssStyle style : ps.getNotAppliedStyles()) {
                    if (style.getOrigin() == getOrigin(item) && !CssContentMaker.containsPseudoState(style.getSelector())) {
                        Node n = createValueUI(item, style);
                        if (n == null) {
                            n = getLabel(style);
                        }
                        Value currentValue = new Value(n);
                        values.add(currentValue);
                        handleSource(currentValue, item, style);
                    }
                }
            }
            // Case where an API call has been made, not applied fxTheme (if any) are hidden
            if (getOrigin(item) == StyleOrigin.USER_AGENT) {
                List<CssPropertyState.CssStyle> styles = item.getFxThemeHiddenByModel();
                for (CssPropertyState.CssStyle style : styles) {
                    Node n = createValueUI(item, style);
                    if (n == null) {
                        String l = CssValueConverter.toCssString(style.getCssProperty(), style.getCssRule(), style.getParsedValue());
                        Label label = new Label(l);
                        n = label;
                    }
                    Value currentValue = new Value(n);
                    values.add(currentValue);
                    handleSource(currentValue, item, style);
                }
            }
        }

        private void handleSource(Value currentValue, final CssProperty item, final CssStyle style) {
            if (style != null && !style.isUsed()) {//eg: -fx-background-fills;
                return;
            }
            final StyleOrigin origin = getOrigin(item);
            String source = getSourceInfo(item, style, origin);
            if (source != null) {
                Label sourceLabel = new Label(source);
                sourceLabel.getStyleClass().add("note-label");//NOI18N
                currentValue.setSource(sourceLabel);
            }
            String nav = getNavigation(item, style);
            if (nav != null) {
                Label navigationLabel = new Label(nav);
                navigationLabel.getStyleClass().add("note-label");//NOI18N
                if (origin != null && origin != StyleOrigin.USER_AGENT) {// No arrow for builtin and fxTheme
                    createNavigationMenuButton();
                    openStylesheetMenuItem
                            = new MenuItem(MessageFormat.format(I18N.getString("csspanel.open.stylesheet"), nav));
                    if ((origin == StyleOrigin.USER) || (origin == StyleOrigin.INLINE)) {
                        // Inspector or Inline columns
                        navigationMenuButton.getItems().add(revealInInspectorMenuItem);
                        revealInInspectorMenuItem.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                navigate(item, getPropertyState(item), style, origin);
                            }
                        });
                        if (CssPanelController.this.applicationDelegate == null) {
                            // disable the menu item in this case
                            revealInInspectorMenuItem.setDisable(true);
                        }
                    } else if (origin == StyleOrigin.AUTHOR) {
                        // Stylesheets column
                        navigationMenuButton.getItems().add(openStylesheetMenuItem);
                        navigationMenuButton.getItems().add(revealInFileBrowserMenuItem);
                        revealInFileBrowserMenuItem.setText(EditorPlatform.IS_MAC
                                ? MessageFormat.format(I18N.getString("csspanel.reveal.finder"), nav)
                                : MessageFormat.format(I18N.getString("csspanel.reveal.explorer"), nav));
                        revealInFileBrowserMenuItem.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                navigate(item, getPropertyState(item), style, origin);
                            }
                        });
                        openStylesheetMenuItem.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                open(item, getPropertyState(item), style, origin);
                            }
                        });
                    }
                }
                currentValue.setNavigation(navigationLabel, navigationMenuButton);
            }
            currentValue.showSource(true);//showSources.isSelected()
        }

        private void createNavigationMenuButton() {
            navigationMenuButton = new MenuButton();
            navigationMenuButton.setPrefWidth(5);
            navigationMenuButton.setMinWidth(5);
            navigationMenuButton.setOpacity(0);
            navigationMenuButton.setGraphic(new ImageView(cogIcon));
            navigationMenuButton.getStyleClass().addAll("cog-button"); //NOI18N
            fadeTransition = new FadeTransition(Duration.millis(500), navigationMenuButton);
        }
    }

    // "API defaults" column
    private class BuiltinValueTableCell extends CssValueTableCell {

        @Override
        protected PropertyState getPropertyState(CssProperty item) {
            return item.builtinState().get();
        }

        @Override
        protected boolean isWinner(CssProperty item) {
            return item.isBuiltinSource();
        }

        @Override
        protected StyleOrigin getOrigin(CssProperty item) {
            return null;
        }

        @Override
        protected CssStyle getStyle(CssProperty item) {
            return null;
        }
    }

    // "FX Theme defaults" column
    private class FxThemeValueTableCell extends CssValueTableCell {

        @Override
        protected PropertyState getPropertyState(CssProperty item) {
            return item.fxThemeState().get();
        }

        @Override
        protected boolean isWinner(CssProperty item) {
            return item.isFxThemeSource();
        }

        @Override
        protected StyleOrigin getOrigin(CssProperty item) {
            return StyleOrigin.USER_AGENT;
        }

        @Override
        protected CssStyle getStyle(CssProperty item) {
            CssPropertyState ps = item.fxThemeState().get();
            return ps == null ? null : ps.getStyle();
        }
    }

    // "Inspector" column
    private class ModelValueTableCell extends CssValueTableCell {

        @Override
        protected PropertyState getPropertyState(CssProperty item) {
            return item.modelState().get();
        }

        @Override
        protected boolean isWinner(CssProperty item) {
            return item.isModelSource();
        }

        @Override
        protected StyleOrigin getOrigin(CssProperty item) {
            return StyleOrigin.USER;
        }

        @Override
        protected CssStyle getStyle(CssProperty item) {
            return null;
        }
    }

    // "Stylesheets" column
    private class AuthorValueTableCell extends CssValueTableCell {

        @Override
        protected PropertyState getPropertyState(CssProperty item) {
            return item.authorState().get();
        }

        @Override
        protected boolean isWinner(CssProperty item) {
            return item.isAuthorSource();
        }

        @Override
        protected StyleOrigin getOrigin(CssProperty item) {
            return StyleOrigin.AUTHOR;
        }

        @Override
        protected CssStyle getStyle(CssProperty item) {
            CssPropertyState ps = item.authorState().get();
            return ps == null ? null : ps.getStyle();
        }
    }

    // "Inline Styles" column
    private class InlineValueTableCell extends CssValueTableCell {

        @Override
        protected PropertyState getPropertyState(CssProperty item) {
            return item.inlineState().get();
        }

        @Override
        protected boolean isWinner(CssProperty item) {
            return item.isInlineSource();
        }

        @Override
        protected StyleOrigin getOrigin(CssProperty item) {
            return StyleOrigin.INLINE;
        }

        @Override
        protected CssStyle getStyle(CssProperty item) {
            CssPropertyState ps = item.inlineState().get();
            return ps == null ? null : ps.getStyle();
        }
    }

    /**
     * XXX jfdenise, handle case where the Fx Theme is not the winning style. The
     * complex case is that we need to return a propertyState BUT we don't know
     * if Fx Theme has been overriden, then we do return null.
     */
    // "Defaults" column
    private class DefaultValueTableCell extends CssValueTableCell {

        @Override
        protected PropertyState getPropertyState(CssProperty item) {
            PropertyState ret = item.fxThemeState().get();
            if (ret == null) {
                // Do we have an override
                boolean foundNotApplied = false;
                PropertyState winner = item.getWinner();
                if (winner != null) {
                    for (CssStyle np : winner.getNotAppliedStyles()) {
                        // Not applied handling will had the value.
                        if (np.getOrigin() == StyleOrigin.USER_AGENT) {
                            foundNotApplied = true;
                            break;
                        }
                    }
                }
                if (!foundNotApplied) {
                    ret = item.builtinState().get();
                }
            }
            return ret;
        }

        @Override
        protected CssStyle getStyle(CssProperty item) {
            CssStyle style = null;
            CssPropertyState fxTheme = item.fxThemeState().get();
            if (fxTheme == null) {
                // Do we have an override
                PropertyState winner = item.getWinner();
                if (winner != null) {
                    for (CssStyle np : winner.getNotAppliedStyles()) {
                        // Not applied handling will had the value.
                        if (np.getOrigin() == StyleOrigin.USER_AGENT) {
                            style = np;
                            break;
                        }
                    }
                }

            } else {
                style = fxTheme.getStyle();
            }
            return style;
        }

        @Override
        protected boolean isWinner(CssProperty item) {
            return item.isFxThemeSource() || item.isBuiltinSource();
        }

        @Override
        protected StyleOrigin getOrigin(CssProperty item) {
            PropertyState state = getPropertyState(item);
            // If null is returned, means that there is a not applied for fxTheme
            if (state instanceof CssPropertyState || state == null) {
                return StyleOrigin.USER_AGENT;
            } else {
                return null;
            }
        }

        // Special case, display some info when merged.
        @Override
        protected String getNavigation(CssProperty item, CssStyle style) {
            PropertyState ps = getPropertyState(item);
            if (ps == null || ps instanceof CssPropertyState) {
                return I18N.getString("csspanel.fxtheme.defaults.navigation")
                        + " (" + CssInternal.getThemeName(style.getStyle()) + ".css)";//NOI18N
            } else {
                return I18N.getString("csspanel.api.defaults.navigation");
            }
        }
    }

    /*
     *
     *
     * NAVIGATION (to inspector, file explorer, css editor, ...
     *
     *
     */
    private void open(CssProperty item, PropertyState state, CssStyle style, StyleOrigin origin) {
        navigate(item, state, style, origin, true);
    }

    private void navigate(CssProperty item, PropertyState state, CssStyle style, StyleOrigin origin) {
        navigate(item, state, style, origin, false);
    }

    private void navigate(CssProperty item, PropertyState state, CssStyle style, StyleOrigin origin, boolean open) {

        if (origin == StyleOrigin.USER) {// Navigate to property
            PropertyName propName = ((BeanPropertyState) state).getPropertyMeta().getName();
            // Navigate to inspector property
            if (applicationDelegate != null) {
                applicationDelegate.revealInspectorEditor(getValuePropertyMeta(propName));
            }
        }
        if (style != null) {
            if (style.getOrigin() == StyleOrigin.AUTHOR) {// Navigate to file
                URL url = style.getUrl();
                String path = url.toExternalForm();
                if (path.toLowerCase(Locale.ROOT).startsWith("file:/")) { //NOI18N
                    try {
                        if (open) {
                            EditorPlatform.open(path);
                        } else {
                            File f = new File(url.toURI());
                            EditorPlatform.revealInFileBrowser(f);
                        }
                    } catch (URISyntaxException | IOException ex) {
                        System.out.println(ex.getMessage() + ": " + ex);
                    }
                }
            } else {
                if (style.getOrigin() == StyleOrigin.INLINE) {
                    // Navigate to inspector style property
                    if (applicationDelegate != null) {
                        applicationDelegate.revealInspectorEditor(
                                getValuePropertyMeta(new PropertyName("style"))); //NOI18N
                    }
                }
            }
        }
    }

    private static String getNavigationInfo(
            CssProperty item, CssStyle cssStyle, StyleOrigin origin) {
        if (origin == StyleOrigin.USER_AGENT) {
             return CssInternal.getThemeName(cssStyle.getStyle()) + ".css"; //NOI18N
        }
        if (origin == StyleOrigin.USER) {
            BeanPropertyState state = (BeanPropertyState) item.modelState().get();
            return item.getTarget().getClass().getSimpleName() + "."
                    + state.getPropertyMeta().getName().getName();//NOI18N
        }
        if (origin == StyleOrigin.AUTHOR) {
            if (cssStyle != null) {
                URL url = cssStyle.getUrl();
                String name = null;
                if (url != null) {
                    name = url.toExternalForm();
                    if (name.toLowerCase(Locale.ROOT).startsWith("file:/")) { //NOI18N
                        try {
                            File f = new File(url.toURI());
                            name = f.getName();
                        } catch (URISyntaxException ex) {
                            System.out.println(ex.getMessage() + ": " + ex);
                        }
                    }
                }
                return name;
            }
        }
        if (origin == StyleOrigin.INLINE) {
            Node n = item.getSourceNodeForInline();
            if (n != null) {
                return nodeIdentifier(n);
            }
            return null;
        }

        return null;
    }

    /*
     *
     * Private static
     *
     */
    private static FXOMInstance getFXOMInstance(Selection selection) {
        FXOMInstance fxomInstance = null;
        if (selection == null) {
            return null;
        }
        if (selection.getGroup() instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
            assert osg.getItems().size() == 1;
            for (FXOMObject item : osg.getItems()) {
                if (item instanceof FXOMInstance) {
                    fxomInstance = (FXOMInstance) item;
                }
            }
        }

        // In case of GridSelectionGroup, nothing to show (?)
        return fxomInstance;
    }

    private ValuePropertyMetadata getValuePropertyMeta(PropertyName propName) {
        ValuePropertyMetadata valuePropMeta = null;
        if (selectedObject instanceof FXOMInstance) {
            valuePropMeta = Metadata.getMetadata().queryValueProperty(
                    (FXOMInstance) selectedObject, propName);
        }
        return valuePropMeta;
    }

    private static String getPseudoStates(Node node) {
        StringBuilder pseudoClasses = new StringBuilder();
        Set<PseudoClass> pseudoClassSet = node.getPseudoClassStates();
        for (PseudoClass pc : pseudoClassSet) {
            pseudoClasses.append(":").append(pc.getPseudoClassName()); //NOI18N
        }
        return pseudoClasses.toString();
    }

    // Best effort to express a potential selector. There is more than one...
    private static String localSelector(Node node) {
        String ret = "";//NOI18N
        String pseudoClasses = getPseudoStates(node);
        if (!node.getStyleClass().isEmpty()) {
            ret = "." + node.getStyleClass().get(node.getStyleClass().size() - 1) + pseudoClasses;//NOI18N
        } else if (node.getId() != null && !node.getId().equals("")) {//NOI18N
            ret = "#" + node.getId() + pseudoClasses;//NOI18N
        }
        return ret;
    }

    private static String createItemName(Node n) {
        return localSelector(n);
    }

    private static String createOptional(Node n) {
        return "(" + getFirstStandardClassName(n.getClass()) + ")";//NOI18N
    }

    private static Node getContent(Object component, String property, String cssValue, Object value, boolean applied) {
        HBox hbox = new HBox();
        Node l = createPropertyLabel(property + ": ", applied);//NOI18N
        hbox.getChildren().add(l);
        // Custom content. Mainly for paints and images
        Node n = getCustomContent(value);
        if (n != null) {
            hbox.getChildren().add(n);
        } else {
            Node cssValueNode = createLabel(cssValue + ";", applied);//NOI18N
            hbox.getChildren().add(cssValueNode);
        }
        return CopyHandler.makeCopyableNode(hbox, property + ": " + cssValue + ";");//NOI18N
    }

    private static Node getCustomContent(Object value) {
        Node ret = null;
        if (value instanceof ParsedValue) {
            ParsedValue<?, ?> pv = (ParsedValue) value;
            value = CssValueConverter.convert(pv);
        }
        if (value != null) {
            if (value.getClass().isArray()) {
                HBox hbox = new HBox(5);
                int size = Array.getLength(value);
                for (int i = 0; i < size; i++) {
                    Node n = getCustomContent(Array.get(value, i));
                    if (n != null) {
                        hbox.getChildren().add(n);
                        if (i < size - 1) {
                            hbox.getChildren().add(new Label(", "));//NOI18N
                        }
                    }
                }
                if (!hbox.getChildren().isEmpty()) {
                    ret = hbox;
                }
            } else {
                if (value instanceof Collection) {
                    HBox hbox = new HBox(5);
                    Collection<?> collection = (Collection<?>) value;
                    Iterator<?> it = collection.iterator();
                    while (it.hasNext()) {
                        Object obj = it.next();
                        Node n = getCustomContent(obj);
                        if (n != null) {
                            hbox.getChildren().add(n);
                            if (it.hasNext()) {
                                hbox.getChildren().add(new Label(", "));//NOI18N
                            }
                        }
                    }
                    if (!hbox.getChildren().isEmpty()) {
                        ret = hbox;
                    }
                } else {// Leaf value
                    CssValuePresenter<?> presenter = CssValuePresenterFactory.getInstance().newValuePresenter(value);
                    Node customPresenter = presenter.getCustomPresenter();
                    ret = customPresenter;
                }
            }
        }
        return ret;
    }

    private static Node createLabel(String text, String styleclass, boolean isApplied) {
        Node node = new Label(text);
        if (styleclass != null) {
            node.getStyleClass().add(styleclass);
        }
        if (!isApplied) {
            node = createLine((Label) node);
        }
        return node;
    }

    private static Node createLabel(String text, boolean isApplied) {
        return createLabel(text, null, isApplied);
    }

    private static Node createPropertyLabel(String text, boolean isApplied) {
        return createLabel(text, "css-panel-property", isApplied);//NOI18N
    }

    private static Node createLine(Node node) {
        StackPane sp = new StackPane();
        sp.getChildren().add(node);
        Separator s = new Separator(Orientation.HORIZONTAL);
        s.setValignment(VPos.CENTER);
        s.getStyleClass().add("notAppliedStyleLine");//NOI18N
        sp.getChildren().add(s);
        return sp;
    }

    private static TreeItem<Node> attachSource(PropertyState css, CssStyle cssStyle, TreeItem<Node> parent, boolean applied) {
        String source = getSource(cssStyle);
        TreeItem<Node> srcItem = null;
        if (source != null) {
            HBox hbox = new HBox(5);
            if (cssStyle.getOrigin() == StyleOrigin.INLINE) {
            } else {
                hbox.getChildren().add(new Label(cssStyle.getSelector()));
                hbox.getChildren().add(new Label("{"));//NOI18N
            }
            hbox.getChildren().add(createLabel(cssStyle.getCssProperty() + ": ", applied));//NOI18N
            Node n = getCustomContent(cssStyle.getParsedValue());
            if (n != null) {
                hbox.getChildren().add(n);
            }
            Node label2 = createLabel(CssValueConverter.toCssString(cssStyle.getCssProperty(), cssStyle.getCssRule(), cssStyle.getParsedValue())
                    + ";", applied);//NOI18N
            hbox.getChildren().add(label2);
            if (cssStyle.getOrigin() != StyleOrigin.INLINE) {
                hbox.getChildren().add(new Label("}"));//NOI18N
            }
            Label label = new Label(source);
            label.setTooltip(new Tooltip(source));
            hbox.getChildren().add(label);
            srcItem = newTreeItem(hbox, css);
            parent.getChildren().add(srcItem);
        }
        return srcItem;
    }

    private static void attachStyle(PropertyState css, CssStyle style, TreeItem<Node> parent, boolean applied) {
        attachStyle(css, style, parent, applied, null);
    }

    private static void attachStyle(PropertyState css, CssStyle style, TreeItem<Node> parent, boolean applied, ArrayList<String> cssPropertyList) {
        TreeItem<Node> sourceItem = attachSource(css, style, parent, applied);
        if (cssPropertyList != null) {
            cssPropertyList.add(style.getCssProperty());
        }
        if (sourceItem != null) {
            // Do we have a chain of lookups?
            for (CssStyle lookup : style.getLookupChain()) {

                if ((cssPropertyList != null) && cssPropertyList.contains(lookup.getCssProperty())) {
                    // This css property has already been attached
                    continue;
                }
                attachStyle(css, lookup, sourceItem, applied, cssPropertyList);
            }
        }
    }

    public static void attachLookupStyles(Object component, CssPropertyState css, CssStyle lookupRoot, TreeItem<Node> parent) {
        // Some lookup that comes from the SB itself, skip them.
        // This is expected, these lookups are superceeded by the 
        // CssUtils.createCSSFrontier
        ArrayList<String> cssPropertyList = new ArrayList<>();
        // cssPropertyList will allow to check that the same css property is not added multiple times
        attachStyle(css, lookupRoot, parent, true, cssPropertyList);
    }

    private static void attachStyles(CssPropertyState css, TreeItem<Node> parent) {
        if (css.getStyle() != null) {
            attachStyle(css, css.getStyle(), parent, true);
        }
    }

    private static class HtmlStyler {

        private final static String INIT_STRING = "<html><body>"; //NOI18N
        private final static String END_STRING = "</body></html>"; //NOI18N
        private final StringBuilder builder = new StringBuilder();
        private String html;

        HtmlStyler() {
            builder.append(INIT_STRING);//NOI18N
        }

        public void check() {
            if (html != null) {
                throw new IllegalArgumentException("Locked, html already generated");//NOI18N
            }
        }

        public void cssRuleStart(String selector, String source) {
            check();
            builder.append("<p>");//NOI18N
            builder.append("<b>").append(selector).append("</b>");//NOI18N
            builder.append("&nbsp;<b>{</b>&nbsp;").append("/*&nbsp;").append(source).append("&nbsp;*/");//NOI18N
        }

        public void cssRuleEnd() {
            check();
            builder.append("<br>");//NOI18N
            builder.append("<b>}</b>");//NOI18N
            builder.append("</p>");//NOI18N
        }

        public void addProperty(String name, String content, boolean applied) {
            check();
            String sepName = name + ":&nbsp;";//NOI18N
            String propName = "<b>" + (applied ? sepName : "<strike>" + sepName + "</strike>") + "</b>";//NOI18N
            builder.append("<br>");//NOI18N
            content = applied ? content : "<strike>" + content + "</strike>";//NOI18N
            builder.append("<span style=\"margin-left:10px;\">").append(propName).append(content).append(";").append("</span>");//NOI18N
        }

        public void addMessage(String mess) {
            check();
            builder.append(mess);
        }

        public String getHtmlString() {
            if (html == null) {
                builder.append(END_STRING);//NOI18N
                html = builder.toString();
            }
            return html;
        }

        public boolean isEmpty() {
            return builder.toString().equals(INIT_STRING);
        }
    }

    public void copyRules() {
        CopyHandler.copy(rulesTree);
    }

    private static class CopyHandler {

        private static String getContent(TreeView<Node> tv) {
            StringBuilder builder = new StringBuilder();
            for (TreeItem<Node> item : tv.getSelectionModel().getSelectedItems()) {
                Node n = item.getValue();
                String str = (String) n.getProperties().get(CSS_TEXT);
                if (str != null) {
                    builder.append(str);
                }
            }
            return builder.toString();
        }

        private static void copy(final TreeView<Node> tv) {
            final String cssContent = getContent(tv);
            final ClipboardContent content = new ClipboardContent();
            content.putString(cssContent);
            Clipboard.getSystemClipboard().setContent(content);
        }

        private static void attachContextMenu(final TreeView<Node> tv) {
            ContextMenu ctxMenu = new ContextMenu();
            final MenuItem cssContentAction = new MenuItem(I18N.getString("csspanel.copy"));
            ctxMenu.setOnShowing(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent arg0) {
                }
            });
            tv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            cssContentAction.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent arg0) {
                    copy(tv);
                }
            });

            ctxMenu.getItems().add(cssContentAction);
            tv.setContextMenu(ctxMenu);
        }
        private static final String CSS_TEXT = "CSS_TEXT";//NOI18N

        private static <T extends Node> T makeCopyableNode(T node, String str) {
            node.getProperties().put(CSS_TEXT, str + "\n");//NOI18N
            return node;
        }

        private static Text createCopyableText(String str) {
            Text text = new Text(str);
            text.getProperties().put(CSS_TEXT, str + "\n");//NOI18N
            return text;
        }
    }

    private static void setTreeHeight(TreeView<?> tv) {
        // XXX jfdenise how to properly compute the height of the tree
        int minHeight = 70;
        int topLevelItems = tv.getRoot().getChildren().size();
        int computed = topLevelItems == 0 ? 15 : (25 * topLevelItems);
        tv.setPrefHeight(Math.max(minHeight, computed));
    }

    private static String nonNull(String string) {
        if (string == null) {
            return ""; //NOI18N
        } else {
            return string;
        }
    }

    private static TreeItem<Node> newTreeItem(Node content, PropertyState ss) {
        TreeItem<Node> ti = new TreeItem<>(content);
        return ti;
    }

    private static String getSource(CssStyle style) {
        URL url = style.getUrl();
        String source = null;
        if (url != null) {
            source = getSource(url, style.getOrigin());
        }
        return source;
    }

    private static String getSource(Rule rule) {
        URL url = null;
        StyleOrigin origin = null;
        // Workaround!
        if (rule != null) {
            try {
                url = new URL(rule.getStylesheet().getUrl());
            } catch (MalformedURLException ex) {
                System.out.println("Invalid URL: " + ex);
            }
            origin = CssInternal.getOrigin(rule);
        }
        return getSource(url, origin);
    }

    private static String getSource(URL url, StyleOrigin origin) {
        String source = null;
        if (url != null) {
            if (origin == StyleOrigin.USER_AGENT) {
                source = I18N.getString("csspanel.fxtheme.origin");
            } else {
                if (origin == StyleOrigin.USER) {
                    source = I18N.getString("csspanel.api.origin")
                            + " " //NOI18N
                            + I18N.getString("csspanel.node.property");
                } else {
                    if (origin == StyleOrigin.AUTHOR) {
                        source = url.toExternalForm();
                    }
                }
            }
        }
        return source;
    }

    private static String getSourceInfo(CssProperty item, CssStyle style, StyleOrigin origin) {
        if (origin == StyleOrigin.USER_AGENT) {
            if (style != null) {
                return style.getSelector();
            }
            return null;
        }
        if (origin == StyleOrigin.USER) {
            BeanPropertyState state = (BeanPropertyState) item.modelState().get();
            return state.getPropertyMeta().getName().getName();
        }
        if (origin == StyleOrigin.AUTHOR) {
            if (style != null) {
                return style.getSelector();
            }
            return null;
        }
        if (origin == StyleOrigin.INLINE) {
            boolean inherited = item.isInlineInherited();
            return "style" + (inherited ? " (" + I18N.getString("csspanel.inherited") + ")" : "");//NOI18N
        }

        return null;
    }

    private static Node createValueUI(CssProperty item, CssStyle style) {
        ParsedValue<?, ?> value = null;
        if (style != null && !style.getLookupChain().isEmpty()) {
            if (style.getLookupChain().size() == 1) {
                value = style.getLookupChain().get(0).getParsedValue();
            } else {
                value = style.getParsedValue();
            }
        } else {
            if (style != null) {
                value = style.getParsedValue();
            }
        }

        return createValueUI(item, null, value, style);
    }

    private static Node createValueUI(CssProperty item, PropertyState ps, Object value, CssStyle style) {
        ParsedValue<?, ?>[] parsedValues = null;
        if (style != null) {
            ParsedValue<?, ?> pv = style.getParsedValue();
            Object v = pv.getValue();
            if (v instanceof ParsedValue<?, ?>[]) {//Means lookups
                parsedValues = (ParsedValue<?, ?>[]) v;
            }
        }
        return createValueUI(item, ps, value, style, parsedValues);
    }

    private static Node createValueUI(CssProperty item, PropertyState ps, Object value, CssStyle style, ParsedValue<?, ?>[] parsedValues) {
        Node ret = null;
        if (value instanceof ParsedValue) {
            ParsedValue<?, ?> pv = (ParsedValue) value;
            value = CssValueConverter.convert(pv);
        }
        if (value != null) {
            if (value.getClass().isArray()) {
                HBox hbox = new HBox(5);
                int size = Array.getLength(value);
                int lookupIndex = 0;
                for (int i = 0; i < size; i++) {
                    Object v = Array.get(value, i);
                    Node n = getLeaf(v);
                    if (n == null) {
                        break;
                    }
                    boolean lookup = false;
                    if (parsedValues != null) {
                        ParsedValue<?, ?> pv = parsedValues[i];
                        lookup = ((ParsedValueImpl) pv).isContainsLookups();
                    }
                    if (lookup) {
                        assert style != null;
                        CssStyle lookupRoot = null;
                        if (style.getLookupChain().size() - 1 < lookupIndex) {
                            // We are in NOT APPLIED case, no lookup in matching Styles.
                            // This is an RT bug logged.
                            // XXX jfdenise, we can reconstruct the lookup chain based on the ParsedValue
                            // We need to access private field ParsedValue.resolved
                            // That is a null if this is the leaf of the Lookup
                            // That is a ParsedValue with a getvalue that is a ParsedValue
                            // to introspect.
//                            ParsedValue<?, ?> pv = parsedValues[i];
//                            if(pv.getValue() instanceof String){
//                                // OK, this is a lookup name.
//                                Object obj = pv.convert(null);
//                            } else {
//                                
//                            }
                        } else {
                            lookupRoot = style.getLookupChain().get(lookupIndex);
                        }

                        lookupIndex += 1;
                        Node lookupUI = createLookupUI(item, ps, style, lookupRoot, n);
                        hbox.getChildren().add(lookupUI);
                    } else {
                        hbox.getChildren().add(n);
                    }
                    if (i < size - 1) {
                        hbox.getChildren().add(new Label(","));//NOI18N
                    }
                }
                if (!hbox.getChildren().isEmpty()) {
                    ret = hbox;
                }
            } else {
                if (value instanceof Collection) {
                    HBox hbox = new HBox(5);
                    int lookupIndex = 0;
                    Collection<?> collection = (Collection<?>) value;
                    Iterator<?> it = collection.iterator();
                    int index = 0;
                    while (it.hasNext()) {
                        Object v = it.next();
                        Node n = getLeaf(v);
                        if (n == null) {
                            break;
                        }
                        boolean lookup = false;
                        if (parsedValues != null) {
                            ParsedValue<?, ?> pv = parsedValues[index];
                            lookup = ((ParsedValueImpl) pv).isContainsLookups();
                        }
                        if (lookup) {
                            CssStyle lookupRoot = null;
                            assert style != null;
                            if (style.getLookupChain().size() - 1 < lookupIndex) {
                                // We are in NOT APPLIED case, no lookup in matching Styles.
                                // This is an RT bug logged.
                                // XXX jfdenise, we can reconstruct the lookup chain based on the ParsedValue
                                // We need to access private field ParsedValue.resolved
                                // That is a null if this is the leaf of the Lookup
                                // That is a ParsedValue with a getvalue that is a ParsedValue
                                // to introspect.
//                            ParsedValue<?, ?> pv = parsedValues[i];
//                            if(pv.getValue() instanceof String){
//                                // OK, this is a lookup name.
//                                Object obj = pv.convert(null);
//                            } else {
//                                
//                            }
                            } else {
                                lookupRoot = style.getLookupChain().get(lookupIndex);
                            }
                            Node lookupUI = createLookupUI(item, ps, style, lookupRoot, n);
                            hbox.getChildren().add(lookupUI);
                            lookupIndex += 1;
                        } else {
                            hbox.getChildren().add(n);
                        }
                        if (it.hasNext()) {
                            hbox.getChildren().add(new Label(","));//NOI18N
                        }
                        index++;
                    }
                    if (!hbox.getChildren().isEmpty()) {
                        ret = hbox;
                    }
                } else {// Leaf value
                    Node n = getLeaf(value);
                    if (n == null && style != null) {
                        n = getLabel(style);
                    }
                    if (style != null && !style.getLookupChain().isEmpty()) {
                        ret = createLookupUI(item, ps, style, style.getLookupChain().get(0), n);
                    } else {
                        ret = n;
                    }
                }
            }
        }
        return ret;
    }

    private static Label getLabel(CssStyle style) {
        String l = CssValueConverter.toCssString(style.getCssProperty(), style.getCssRule(), style.getParsedValue());
        return new Label(l);
    }

    private static Node createLookupUI(final CssProperty item, final PropertyState ps, final CssStyle style, final CssStyle lookupRoot, Node n) {

        // TODO: make an fxml file for this : ContextMenu with CustomMenuItem
        return null;

//        final HBox lookupContainer = new HBox();
//        lookupContainer.setMaxWidth(Region.USE_PREF_SIZE);
//        ImageView imgView = new ImageView();
//        imgView.setImage(lookups);
//        lookupContainer.getChildren().addAll(n, imgView);
//        final DropDownPopup popup = new DropDownPopup();
//        popup.setAnchor(lookupContainer);
//        popup.setContentMaker(new Callable<Node>() {
//            @Override
//            public Node call() throws Exception {
//                TreeView<Node> tv = new TreeView<>();
//                tv.setPrefSize(400, 100);
//                tv.getStylesheets().add(CssUtils.CSS_VIEWER_CSS);
//                Object val = null;
//                String cssVal = null;
//                if (ps instanceof CssPropertyState) {
//                    val = ((CssPropertyState) ps).getFxValue();
//                    cssVal = ((CssPropertyState) ps).getCssValue();
//                } else {
//                    if (style != null) {
//                        val = style.getParsedValue();
//                        cssVal = CssValueConverter.toCssString(style.getCssProperty(), style.getCssRule(), style.getParsedValue());
//                    }
//                }
//                assert val != null;
//                TreeItem<Node> root = new TreeItem<>();
//                tv.setRoot(root);
//                tv.setShowRoot(false);
//                if (ps != null) {
//                    assert ps instanceof CssPropertyState;
//                    attachLookupStyles(item.getTarget(), ((CssPropertyState) ps), lookupRoot, root);
//                } else {
//                    attachLookupStyles(item.getTarget(), null, lookupRoot, root);
//                }
//                return tv;
//            }
//        });
//        lookupContainer.setOnMouseClicked(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent event) {
//                if (!popup.isShowing()) {
//                    popup.doShow();
//                }
//            }
//        });
//        return lookupContainer;
    }

    private static Node getLeaf(Object value) {
        CssValuePresenterFactory.CssValuePresenter<?> presenter = CssValuePresenterFactory.getInstance().newValuePresenter(value);
        Node customPresenter = presenter.getCustomPresenter();
        return customPresenter;
    }

    private static void attachStylePropertyNoLookup(Object component, TreeItem<Node> parent,
            CssPropertyState ps, CssStyle style, boolean applied) {
        CssStyle cssStyle = applied ? ps.getStyle() : style;
        Object value = applied ? ps.getFxValue() : style.getParsedValue();
        String cssValue = CssValueConverter.toCssString(cssStyle.getCssProperty(), cssStyle.getCssRule(), cssStyle.getParsedValue());
        TreeItem<Node> item = new TreeItem<>(getContent(component, ps.getCssProperty(), cssValue, value, applied));
        parent.getChildren().add(item);
    }

}
