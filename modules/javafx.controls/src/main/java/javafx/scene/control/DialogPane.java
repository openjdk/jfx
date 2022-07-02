/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.control;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.sun.javafx.scene.control.skin.Utils;
import com.sun.javafx.scene.control.skin.resources.ControlResources;
import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.StyleableStringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import com.sun.javafx.css.StyleManager;
import javafx.css.converter.StringConverter;

/**
 * DialogPane should be considered to be the root node displayed within a
 * {@link Dialog} instance. In this role, the DialogPane is responsible for the
 * placement of {@link #headerProperty() headers}, {@link #graphicProperty() graphics},
 * {@link #contentProperty() content}, and {@link #getButtonTypes() buttons}.
 * The default implementation of DialogPane (that is, the DialogPane class itself)
 * handles the layout via the normal {@link #layoutChildren()} method. This
 * method may be overridden by subclasses wishing to handle the layout in an
 * alternative fashion).
 *
 * <p>In addition to the {@link #headerProperty() header} and
 * {@link #contentProperty() content} properties, there exists
 * {@link #headerTextProperty() header text} and
 * {@link #contentTextProperty() content text} properties. The way the *Text
 * properties work is that they are a lower precedence compared to the Node
 * properties, but they are far more convenient for developers in the common case,
 * as it is likely the case that a developer more often than not simply wants to
 * set a string value into the header or content areas of the DialogPane.
 *
 * <p>It is important to understand the implications of setting non-null values
 * in the {@link #headerProperty() header} and {@link #headerTextProperty() headerText}
 * properties. The key points are as follows:
 *
 * <ol>
 *   <li>The {@code header} property takes precedence over the {@code headerText}
 *       property, so if both are set to non-null values, {@code header} will be
 *       used and {@code headerText} will be ignored.</li>
 *   <li>If {@code headerText} is set to a non-null value, and a
 *       {@link #graphicProperty() graphic} has also been set, the default position
 *       for the graphic shifts from being located to the left of the content area
 *       to being to the right of the header text.</li>
 *   <li>If {@code header} is set to a non-null value, and a
 *       {@link #graphicProperty() graphic} has also been set, the graphic is
 *       removed from its default position (to the left of the content area),
 *       and <strong>is not</strong> placed to the right of the custom header
 *       node. If the graphic is desired, it should be manually added in to the
 *       layout of the custom header node manually.</li>
 * </ol>
 *
 * <p>DialogPane operates on the concept of {@link ButtonType}. A ButtonType is
 * a descriptor of a single button that should be represented visually in the
 * DialogPane. Developers who create a DialogPane therefore must specify the
 * button types that they want to display, and this is done via the
 * {@link #getButtonTypes()} method, which returns a modifiable
 * {@link ObservableList}, which users can add to and remove from as desired.
 *
 * <p>The {@link ButtonType} class defines a number of pre-defined button types,
 * such as {@link ButtonType#OK} and {@link ButtonType#CANCEL}. Many users of the
 * JavaFX dialogs API will find that these pre-defined button types meet their
 * needs, particularly due to their built-in support for
 * {@link ButtonData#isDefaultButton() default} and
 * {@link ButtonData#isCancelButton() cancel} buttons, as well as the benefit of
 * the strings being translated into all languages which JavaFX is translated to.
 * For users that want to define their own {@link ButtonType} (most commonly to
 * define a button with custom text), they may do so via the constructors available
 * on the {@link ButtonType} class.
 *
 * <p>Developers will quickly find that the amount of configurability offered
 * via the {@link ButtonType} class is minimal. This is intentional, but does not
 * mean that developers can not modify the buttons created by the {@link ButtonType}
 * that have been specified. To do this, developers simply call the
 * {@link #lookupButton(ButtonType)} method with the ButtonType (assuming it has
 * already been set in the {@link #getButtonTypes()} list. The returned Node is
 * typically of type {@link Button}, but this depends on if the
 * {@link #createButton(ButtonType)} method has been overridden.
 *
 * <p>The DialogPane class offers a few methods that can be overridden by
 * subclasses, to more easily enable custom functionality. These methods include
 * the following:
 *
 * <ul>
 *   <li>{@link #createButton(ButtonType)}
 *   <li>{@link #createDetailsButton()}
 *   <li>{@link #createButtonBar()}
 * </ul>
 *
 * <p>These methods are documented, so please take note of the expectations
 * placed on any developer who wishes to override these methods with their own
 * functionality.
 *
 * @see Dialog
 * @since JavaFX 8u40
 */
@DefaultProperty("buttonTypes")
public class DialogPane extends Pane {

    /* ************************************************************************
     *
     * Static fields
     *
     **************************************************************************/

    /**
     * Creates a Label node that works well within a Dialog.
     * @param text The text to display
     */
    static Label createContentLabel(String text) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMaxHeight(Double.MAX_VALUE);
        label.getStyleClass().add("content");
        label.setWrapText(true);
        label.setPrefWidth(360);
        return label;
    }



    /* ************************************************************************
     *
     * Private fields
     *
     **************************************************************************/

    private final GridPane headerTextPanel;
    private final Label contentLabel;
    private final StackPane graphicContainer;
    private final Node buttonBar;

    private final ObservableList<ButtonType> buttons = FXCollections.observableArrayList();

    private final Map<ButtonType, Node> buttonNodes = new WeakHashMap<>();

    private Node detailsButton;

    // this is not a property - we have a package-scope setDialog method that
    // sets this field. It is set by Dialog if the DialogPane is set inside a Dialog.
    private Dialog<?> dialog;



    /* ************************************************************************
     *
     * Constructors
     *
     **************************************************************************/

    /**
     * Creates a new DialogPane instance with a style class of 'dialog-pane'.
     */
    public DialogPane() {
        getStyleClass().add("dialog-pane");

        headerTextPanel = new GridPane();
        getChildren().add(headerTextPanel);

        graphicContainer = new StackPane();

        contentLabel = createContentLabel("");
        getChildren().add(contentLabel);

        // Add this listener before calling #createButtonBar, so that the listener added in #createButtonBar will run
        // after this one.
        buttons.addListener((ListChangeListener<ButtonType>) c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (ButtonType cmd : c.getRemoved()) {
                        buttonNodes.remove(cmd);
                    }
                }
                if (c.wasAdded()) {
                    for (ButtonType cmd : c.getAddedSubList()) {
                        if (! buttonNodes.containsKey(cmd)) {
                            buttonNodes.put(cmd, createButton(cmd));
                        }
                    }
                }
            }
        });

        buttonBar = createButtonBar();
        if (buttonBar != null) {
            getChildren().add(buttonBar);
        }
    }



    /* ************************************************************************
     *
     * Properties
     *
     **************************************************************************/

    // --- graphic
    private final ObjectProperty<Node> graphicProperty = new StyleableObjectProperty<Node>() {
        // The graphic is styleable by css, but it is the
        // imageUrlProperty that handles the style value.
        @Override public CssMetaData getCssMetaData() {
            return StyleableProperties.GRAPHIC;
        }

        @Override public Object getBean() {
            return DialogPane.this;
        }

        @Override public String getName() {
            return "graphic";
        }

        WeakReference<Node> graphicRef = new WeakReference<>(null);

        protected void invalidated() {
            Node oldGraphic = graphicRef.get();
            if (oldGraphic != null) {
                getChildren().remove(oldGraphic);
            }

            Node newGraphic = getGraphic();
            graphicRef = new WeakReference<>(newGraphic);
            updateHeaderArea();
        }
    };

    /**
     * The dialog graphic, presented either in the header, if one is showing, or
     * to the left of the {@link #contentProperty() content}.
     *
     * @return An ObjectProperty wrapping the current graphic.
     */
    public final ObjectProperty<Node> graphicProperty() {
        return graphicProperty;
    }

    public final Node getGraphic() {
        return graphicProperty.get();
    }

    /**
     * Sets the dialog graphic, which will be displayed either in the header, if
     * one is showing, or to the left of the {@link #contentProperty() content}.
     *
     * @param graphic
     *            The new dialog graphic, or null if no graphic should be shown.
     */
    public final void setGraphic(Node graphic) {
        this.graphicProperty.set(graphic);
    }


    // --- imageUrl (this is NOT public API, except via CSS)
    // Note that this code is a copy/paste from Labeled
    private StyleableStringProperty imageUrl = null;
    /**
     * The imageUrl property is set from CSS and then the graphic property is
     * set from the invalidated method. This ensures that the same image isn't
     * reloaded.
     */
    private StyleableStringProperty imageUrlProperty() {
        if (imageUrl == null) {
            imageUrl = new StyleableStringProperty() {
                //
                // If imageUrlProperty is invalidated, this is the origin of the style that
                // triggered the invalidation. This is used in the invalidated() method where the
                // value of super.getStyleOrigin() is not valid until after the call to set(v) returns,
                // by which time invalidated will have been called.
                // This value is initialized to USER in case someone calls set on the imageUrlProperty, which
                // is possible:
                //     CssMetaData metaData = ((StyleableProperty)dialogPane.graphicProperty()).getCssMetaData();
                //     StyleableProperty prop = metaData.getStyleableProperty(dialogPane);
                //     prop.set(someUrl);
                //
                // TODO: Note that prop != dialogPane, which violates the contract between StyleableProperty and CssMetaData.
                //
                StyleOrigin origin = StyleOrigin.USER;

                @Override
                public void applyStyle(StyleOrigin origin, String v) {
                    this.origin = origin;

                    // Don't want applyStyle to throw an exception which would leave this.origin set to the wrong value
                    if (graphicProperty == null || graphicProperty.isBound() == false) super.applyStyle(origin, v);

                    // Origin is only valid for this invocation of applyStyle, so reset it to USER in case someone calls set.
                    this.origin = StyleOrigin.USER;
                }

                @Override
                protected void invalidated() {
                    // need to call super.get() here since get() is overridden to return the graphicProperty's value
                    final String url = super.get();

                    if (url == null) {
                        ((StyleableProperty<Node>)(WritableValue<Node>)graphicProperty()).applyStyle(origin, null);
                    } else {
                        // RT-34466 - if graphic's url is the same as this property's value, then don't overwrite.
                        final Node graphicNode = DialogPane.this.getGraphic();
                        if (graphicNode instanceof ImageView) {
                            final ImageView imageView = (ImageView)graphicNode;
                            final Image image = imageView.getImage();
                            if (image != null) {
                                final String imageViewUrl = image.getUrl();
                                if (url.equals(imageViewUrl)) return;
                            }

                        }

                        final Image img = StyleManager.getInstance().getCachedImage(url);

                        if (img != null) {
                            //
                            // Note that it is tempting to try to re-use existing ImageView simply by setting
                            // the image on the current ImageView, if there is one. This would effectively change
                            // the image, but not the ImageView which means that no graphicProperty listeners would
                            // be notified. This is probably not what we want.
                            //

                            //
                            // Have to call applyStyle on graphicProperty so that the graphicProperty's
                            // origin matches the imageUrlProperty's origin.
                            //
                            ((StyleableProperty<Node>)(WritableValue<Node>)graphicProperty()).applyStyle(origin, new ImageView(img));
                        }
                    }
                }

                @Override
                public String get() {
                    //
                    // The value of the imageUrlProperty is that of the graphicProperty.
                    // Return the value in a way that doesn't expand the graphicProperty.
                    //
                    final Node graphic = getGraphic();
                    if (graphic instanceof ImageView) {
                        final Image image = ((ImageView)graphic).getImage();
                        if (image != null) {
                            return image.getUrl();
                        }
                    }
                    return null;
                }

                @Override
                public StyleOrigin getStyleOrigin() {
                    //
                    // The origin of the imageUrlProperty is that of the graphicProperty.
                    // Return the origin in a way that doesn't expand the graphicProperty.
                    //
                    return graphicProperty != null ? ((StyleableProperty<Node>)(WritableValue<Node>)graphicProperty).getStyleOrigin() : null;
                }

                @Override
                public Object getBean() {
                    return DialogPane.this;
                }

                @Override
                public String getName() {
                    return "imageUrl";
                }

                @Override
                public CssMetaData<DialogPane,String> getCssMetaData() {
                    return StyleableProperties.GRAPHIC;
                }

            };
        }
        return imageUrl;
    }


    // --- header
    private final ObjectProperty<Node> header = new SimpleObjectProperty<Node>(null) {
        WeakReference<Node> headerRef = new WeakReference<>(null);
        @Override protected void invalidated() {
            Node oldHeader = headerRef.get();
            if (oldHeader != null) {
                getChildren().remove(oldHeader);
            }

            Node newHeader = getHeader();
            headerRef = new WeakReference<>(newHeader);
            updateHeaderArea();
        }
    };

    /**
     * Node which acts as the dialog pane header.
     *
     * @return the header of the dialog pane.
     */
    public final Node getHeader() {
        return header.get();
    }

    /**
     * Assigns the dialog pane header. Any Node can be used.
     *
     * @param header The new header of the DialogPane.
     */
    public final void setHeader(Node header) {
        this.header.setValue(header);
    }

    /**
     * Property representing the header area of the dialog pane. Note that if this
     * header is set to a non-null value, that it will take up the entire top
     * area of the DialogPane. It will also result in the DialogPane switching its
     * layout to the 'header' layout - as outlined in the {@link DialogPane} class
     * javadoc.
     * @return the property representing the header area of the dialog pane
     */
    public final ObjectProperty<Node> headerProperty() {
        return header;
    }



    // --- header text
    private final StringProperty headerText = new SimpleStringProperty(this, "headerText") {
        @Override protected void invalidated() {
            updateHeaderArea();
            requestLayout();
        }
    };

    /**
     * Sets the string to show in the dialog header area. Note that the header text
     * is lower precedence than the {@link #headerProperty() header node}, meaning
     * that if both the header node and the headerText properties are set, the
     * header text will not be displayed in a default DialogPane instance.
     *
     * <p>When headerText is set to a non-null value, this will result in the
     * DialogPane switching its layout to the 'header' layout - as outlined in
     * the {@link DialogPane} class javadoc.</p>
     * @param headerText the string to show in the dialog header area
     */
    public final void setHeaderText(String headerText) {
        this.headerText.set(headerText);
    }

    /**
     * Returns the currently-set header text for this DialogPane.
     * @return the currently-set header text for this DialogPane
     */
    public final String getHeaderText() {
        return headerText.get();
    }

    /**
     * A property representing the header text for the dialog pane. The header text
     * is lower precedence than the {@link #headerProperty() header node}, meaning
     * that if both the header node and the headerText properties are set, the
     * header text will not be displayed in a default DialogPane instance.
     *
     * <p>When headerText is set to a non-null value, this will result in the
     * DialogPane switching its layout to the 'header' layout - as outlined in
     * the {@link DialogPane} class javadoc.</p>
     * @return the property representing the header text for the dialog pane
     */
    public final StringProperty headerTextProperty() {
        return headerText;
    }


    // --- content
    private final ObjectProperty<Node> content = new SimpleObjectProperty<Node>(null) {
        WeakReference<Node> contentRef = new WeakReference<>(null);
        @Override protected void invalidated() {
            Node oldContent = contentRef.get();
            if (oldContent != null) {
                getChildren().remove(oldContent);
            }

            Node newContent = getContent();
            contentRef = new WeakReference<>(newContent);
            updateContentArea();
        }
    };

    /**
     * Returns the dialog content as a Node (even if it was set as a String
     * using {@link #setContentText(String)} - this was simply transformed into a
     * {@link Node} (most probably a {@link Label}).
     *
     * @return dialog's content
     */
    public final Node getContent() {
        return content.get();
    }

    /**
     * Assign dialog content. Any Node can be used
     *
     * @param content
     *            dialog's content
     */
    public final void setContent(Node content) {
        this.content.setValue(content);
    }

    /**
     * Property representing the content area of the dialog.
     * @return the property representing the content area of the dialog
     */
    public final ObjectProperty<Node> contentProperty() {
        return content;
    }


    // --- content text
    private final StringProperty contentText = new SimpleStringProperty(this, "contentText") {
        @Override protected void invalidated() {
            updateContentArea();
            requestLayout();
        }
    };

    /**
     * Sets the string to show in the dialog content area. Note that the content text
     * is lower precedence than the {@link #contentProperty() content node}, meaning
     * that if both the content node and the contentText properties are set, the
     * content text will not be displayed in a default DialogPane instance.
     * @param contentText the string to show in the dialog content area
     */
    public final void setContentText(String contentText) {
        this.contentText.set(contentText);
    }

    /**
     * Returns the currently-set content text for this DialogPane.
     * @return the currently-set content text for this DialogPane
     */
    public final String getContentText() {
        return contentText.get();
    }

    /**
     * A property representing the content text for the dialog pane. The content text
     * is lower precedence than the {@link #contentProperty() content node}, meaning
     * that if both the content node and the contentText properties are set, the
     * content text will not be displayed in a default DialogPane instance.
     * @return the property representing the content text for the dialog pane
     */
    public final StringProperty contentTextProperty() {
        return contentText;
    }


    // --- expandable content
    private final ObjectProperty<Node> expandableContentProperty = new SimpleObjectProperty<Node>(null) {
        WeakReference<Node> expandableContentRef = new WeakReference<>(null);
        @Override protected void invalidated() {
            Node oldExpandableContent = expandableContentRef.get();
            if (oldExpandableContent != null) {
                getChildren().remove(oldExpandableContent);
            }

            Node newExpandableContent = getExpandableContent();
            expandableContentRef = new WeakReference<Node>(newExpandableContent);
            if (newExpandableContent != null) {
                newExpandableContent.setVisible(isExpanded());
                newExpandableContent.setManaged(isExpanded());

                if (!newExpandableContent.getStyleClass().contains("expandable-content")) { //$NON-NLS-1$
                    newExpandableContent.getStyleClass().add("expandable-content"); //$NON-NLS-1$
                }

                getChildren().add(newExpandableContent);
            }
        }
    };

    /**
     * A property that represents the dialog expandable content area. Any Node
     * can be placed in this area, but it will only be shown when the user
     * clicks the 'Show Details' expandable button. This button will be added
     * automatically when the expandable content property is non-null.
     * @return the property that represents the dialog expandable content area
     */
    public final ObjectProperty<Node> expandableContentProperty() {
        return expandableContentProperty;
    }

    /**
     * Returns the dialog expandable content node, if one is set, or null
     * otherwise.
     * @return the dialog expandable content node
     */
    public final Node getExpandableContent() {
        return expandableContentProperty.get();
    }

    /**
     * Sets the dialog expandable content node, or null if no expandable content
     * needs to be shown.
     * @param content the dialog expandable content node
     */
    public final void setExpandableContent(Node content) {
        this.expandableContentProperty.set(content);
    }


    // --- expanded
    private final BooleanProperty expandedProperty = new SimpleBooleanProperty(this, "expanded", false) {
        protected void invalidated() {
            final Node expandableContent = getExpandableContent();

            if (expandableContent != null) {
                expandableContent.setVisible(isExpanded());
            }

            requestLayout();
        }
    };

    /**
     * Represents whether the dialogPane is expanded.
     * @return the property representing whether the dialogPane is expanded
     */
    public final BooleanProperty expandedProperty() {
        return expandedProperty;
    }

    /**
     * Returns whether or not the dialogPane is expanded.
     *
     * @return true if dialogPane is expanded.
     */
    public final boolean isExpanded() {
        return expandedProperty().get();
    }

    /**
     * Sets whether the dialogPane is expanded. This only makes sense when there
     * is {@link #expandableContentProperty() expandable content} to show.
     *
     * @param value true if dialogPane should be expanded.
     */
    public final void setExpanded(boolean value) {
        expandedProperty().set(value);
    }



    /* ************************************************************************
     *
     * Public API
     *
     **************************************************************************/

    // --- button types
    /**
     * Observable list of button types used for the dialog button bar area
     * (created via the {@link #createButtonBar()} method). Modifying the contents
     * of this list will immediately change the buttons displayed to the user
     * within the dialog pane.
     *
     * @return The {@link ObservableList} of {@link ButtonType button types}
     *         available to the user.
     */
    public final ObservableList<ButtonType> getButtonTypes() {
        return buttons;
    }

    /**
     * This method provides a way in which developers may retrieve the actual
     * Node for a given {@link ButtonType} (assuming it is part of the
     * {@link #getButtonTypes() button types} list).
     *
     * @param buttonType The {@link ButtonType} for which a Node representation is requested.
     * @return The Node used to represent the button type, as created by
     *         {@link #createButton(ButtonType)}, and only if the button type
     *         is part of the {@link #getButtonTypes() button types} list, otherwise null.
     */
    public final Node lookupButton(ButtonType buttonType) {
        return buttonNodes.get(buttonType);
    }

    /**
     * This method can be overridden by subclasses to provide the button bar.
     * Note that by overriding this method, the developer must take on multiple
     * responsibilities:
     *
     * <ol>
     *   <li>The developer must immediately iterate through all
     *   {@link #getButtonTypes() button types} and call
     *   {@link #createButton(ButtonType)} for each of them in turn.
     *   <li>The developer must add a listener to the
     *   {@link #getButtonTypes() button types} list, and when this list changes
     *   update the button bar as appropriate.
     *   <li>Similarly, the developer must watch for changes to the
     *   {@link #expandableContentProperty() expandable content} property,
     *   adding and removing the details button (created via
     *   {@link #createDetailsButton()} method).
     * </ol>
     *
     * <p>The default implementation of this method creates and returns a new
     * {@link ButtonBar} instance.
     * @return the created button bar
     */
    protected Node createButtonBar() {
        ButtonBar buttonBar = new ButtonBar();
        buttonBar.setMaxWidth(Double.MAX_VALUE);

        updateButtons(buttonBar);
        getButtonTypes().addListener((ListChangeListener<? super ButtonType>) c -> updateButtons(buttonBar));
        expandableContentProperty().addListener(o -> updateButtons(buttonBar));

        return buttonBar;
    }

    /**
     * This method can be overridden by subclasses to create a custom button that
     * will subsequently inserted into the DialogPane button area (created via
     * the {@link #createButtonBar()} method, but mostly commonly it is an instance
     * of {@link ButtonBar}.
     *
     * @param buttonType The {@link ButtonType} to create a button from.
     * @return A JavaFX {@link Node} that represents the given {@link ButtonType},
     *         most commonly an instance of {@link Button}.
     */
    protected Node createButton(ButtonType buttonType) {
        final Button button = new Button(buttonType.getText());
        final ButtonData buttonData = buttonType.getButtonData();
        ButtonBar.setButtonData(button, buttonData);
        button.setDefaultButton(buttonData.isDefaultButton());
        button.setCancelButton(buttonData.isCancelButton());
        button.addEventHandler(ActionEvent.ACTION, ae -> {
            if (ae.isConsumed()) return;
            if (dialog != null) {
                dialog.setResultAndClose(buttonType, true);
            }
        });

        return button;
    }

    /**
     * This method can be overridden by subclasses to create a custom details button.
     *
     * <p>To override this method you must do two things:
     * <ol>
     *   <li>The button will need to have its own code set to handle mouse / keyboard
     *       interaction and to toggle the state of the
     *       {@link #expandedProperty() expanded} property.
     *   <li>If your button changes its visuals based on whether the dialog pane
     *       is expanded or collapsed, you should add a listener to the
     *       {@link #expandedProperty() expanded} property, so that you may update
     *       the button visuals.
     * </ol>
     * @return the created details button
     */
    protected Node createDetailsButton() {
        final Hyperlink detailsButton = new Hyperlink();
        final String moreText = ControlResources.getString("Dialog.detail.button.more"); //$NON-NLS-1$
        final String lessText = ControlResources.getString("Dialog.detail.button.less"); //$NON-NLS-1$

        InvalidationListener expandedListener = o -> {
            final boolean isExpanded = isExpanded();
            detailsButton.setText(isExpanded ? lessText : moreText);
            detailsButton.getStyleClass().setAll("details-button", (isExpanded ? "less" : "more")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        };

        // we call the listener immediately to ensure the state is correct at start up
        expandedListener.invalidated(null);
        expandedProperty().addListener(expandedListener);

        detailsButton.setOnAction(ae -> setExpanded(!isExpanded()));
        return detailsButton;
    }

    private double oldHeight = -1;

    /** {@inheritDoc} */
    @Override protected void layoutChildren() {
        final boolean hasHeader = hasHeader();

        // snapped insets code commented out to resolve RT-39738
        final double w = Math.max(minWidth(-1), getWidth());// - (snappedLeftInset() + snappedRightInset());

        final double minHeight = minHeight(w);
        final double prefHeight = prefHeight(w);
        final double maxHeight = maxHeight(w);
        final double currentHeight = getHeight();
        final double dialogHeight = dialog == null ? 0 : dialog.dialog.getSceneHeight();
        double h;

        if (prefHeight > currentHeight && prefHeight > minHeight && (prefHeight <= dialogHeight || dialogHeight == 0)) {
            h = Utils.boundedSize(prefHeight, minHeight, maxHeight);
            resize(w, h);
        } else {
            boolean isDialogGrowing = currentHeight > oldHeight;

            if (isDialogGrowing) {
                double _h = currentHeight < prefHeight ?
                        Math.min(prefHeight, currentHeight) : Math.max(prefHeight, dialogHeight);
                h = Utils.boundedSize(_h, minHeight, maxHeight);
            } else {
                h = Utils.boundedSize(Math.min(currentHeight, dialogHeight), minHeight, maxHeight);
            }
            resize(w, h);
        }

        h -= (snappedTopInset() + snappedBottomInset());

        oldHeight = h;

        final double leftPadding = snappedLeftInset();
        final double topPadding = snappedTopInset();
        final double rightPadding = snappedRightInset();

        // create the nodes up front so we can work out sizing
        final Node header = getActualHeader();
        final Node content = getActualContent();
        final Node graphic = getActualGraphic();
        final Node expandableContent = getExpandableContent();

        final double graphicPrefWidth = hasHeader || graphic == null ? 0 : graphic.prefWidth(-1);
        final double headerPrefHeight = hasHeader ? header.prefHeight(w) : 0;
        final double buttonBarPrefHeight = buttonBar == null ? 0 : buttonBar.prefHeight(w);
        final double graphicPrefHeight = hasHeader || graphic == null ? 0 : graphic.prefHeight(-1);

        final double expandableContentPrefHeight;
        final double contentAreaHeight;
        final double contentAndGraphicHeight;

        final double availableContentWidth = w - graphicPrefWidth - leftPadding - rightPadding;

        if (isExpanded()) {
            // precedence goes to content and then expandable content
            contentAreaHeight = isExpanded() ? content.prefHeight(availableContentWidth) : 0;
            contentAndGraphicHeight = hasHeader ? contentAreaHeight : Math.max(graphicPrefHeight, contentAreaHeight);
            expandableContentPrefHeight = h - (headerPrefHeight + contentAndGraphicHeight + buttonBarPrefHeight);
        } else {
            // content gets the lowest precedence
            expandableContentPrefHeight = isExpanded() ? expandableContent.prefHeight(w) : 0;
            contentAreaHeight = h - (headerPrefHeight + expandableContentPrefHeight + buttonBarPrefHeight);
            contentAndGraphicHeight = hasHeader ? contentAreaHeight : Math.max(graphicPrefHeight, contentAreaHeight);
        }

        double x = leftPadding;
        double y = topPadding;

        if (! hasHeader) {
            if (graphic != null) {
                graphic.resizeRelocate(x, y, graphicPrefWidth, graphicPrefHeight);
                x += graphicPrefWidth;
            }
        } else {
            header.resizeRelocate(x, y, w - (leftPadding + rightPadding), headerPrefHeight);
            y += headerPrefHeight;
        }

        content.resizeRelocate(x, y, availableContentWidth, contentAreaHeight);
        y += hasHeader ? contentAreaHeight : contentAndGraphicHeight;

        if (expandableContent != null) {
            expandableContent.resizeRelocate(leftPadding, y, w - rightPadding, expandableContentPrefHeight);
            y += expandableContentPrefHeight;
        }

        if (buttonBar != null) {
            buttonBar.resizeRelocate(leftPadding,
                                     y,
                                     w - (leftPadding + rightPadding),
                                     buttonBarPrefHeight);
        }
    }

    /** {@inheritDoc} */
    @Override protected double computeMinWidth(double height) {
        double headerMinWidth = hasHeader() ? getActualHeader().minWidth(height) + 10 : 0;
        double contentMinWidth = getActualContent().minWidth(height);
        double buttonBarMinWidth = buttonBar == null ? 0 : buttonBar.minWidth(height);
        double graphicMinWidth = getActualGraphic().minWidth(height);

        double expandableContentMinWidth = 0;
        final Node expandableContent = getExpandableContent();
        if (isExpanded() && expandableContent != null) {
            expandableContentMinWidth = expandableContent.minWidth(height);
        }

        double minWidth = snappedLeftInset() +
                (hasHeader() ? 0 : graphicMinWidth) +
                Math.max(Math.max(headerMinWidth, expandableContentMinWidth), Math.max(contentMinWidth, buttonBarMinWidth)) +
                snappedRightInset();

        return snapSizeX(minWidth);
    }

    /** {@inheritDoc} */
    @Override protected double computeMinHeight(double width) {
        final boolean hasHeader = hasHeader();

        double headerMinHeight = hasHeader ? getActualHeader().minHeight(width) : 0;
        double buttonBarMinHeight = buttonBar == null ? 0 : buttonBar.minHeight(width);

        Node graphic = getActualGraphic();
        double graphicMinWidth = hasHeader ? 0 : graphic.minWidth(-1);
        double graphicMinHeight = hasHeader ? 0 : graphic.minHeight(width);

        // min height of a label is based on one line (wrapping is ignored)
        Node content = getActualContent();
        double contentAvailableWidth = width == Region.USE_COMPUTED_SIZE ? Region.USE_COMPUTED_SIZE :
                hasHeader ? width : (width - graphicMinWidth);
        double contentMinHeight = content.minHeight(contentAvailableWidth);

        double expandableContentMinHeight = 0;
        final Node expandableContent = getExpandableContent();
        if (isExpanded() && expandableContent != null) {
            expandableContentMinHeight = expandableContent.minHeight(width);
        }

        double minHeight = snappedTopInset() +
                headerMinHeight +
                Math.max(graphicMinHeight, contentMinHeight) +
                expandableContentMinHeight +
                buttonBarMinHeight +
                snappedBottomInset();

        return snapSizeY(minHeight);
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height) {
        double headerPrefWidth = hasHeader() ? getActualHeader().prefWidth(height) + 10 : 0;
        double contentPrefWidth = getActualContent().prefWidth(height);
        double buttonBarPrefWidth = buttonBar == null ? 0 : buttonBar.prefWidth(height);
        double graphicPrefWidth = getActualGraphic().prefWidth(height);

        double expandableContentPrefWidth = 0;
        final Node expandableContent = getExpandableContent();
        if (isExpanded() && expandableContent != null) {
            expandableContentPrefWidth = expandableContent.prefWidth(height);
        }

        double prefWidth = snappedLeftInset() +
               (hasHeader() ? 0 : graphicPrefWidth) +
               Math.max(Math.max(headerPrefWidth, expandableContentPrefWidth), Math.max(contentPrefWidth, buttonBarPrefWidth)) +
               snappedRightInset();

        return snapSizeX(prefWidth);
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width) {
        final boolean hasHeader = hasHeader();

        double headerPrefHeight = hasHeader ? getActualHeader().prefHeight(width) : 0;
        double buttonBarPrefHeight = buttonBar == null ? 0 : buttonBar.prefHeight(width);

        Node graphic = getActualGraphic();
        double graphicPrefWidth = hasHeader ? 0 : graphic.prefWidth(-1);
        double graphicPrefHeight = hasHeader ? 0 : graphic.prefHeight(width);

        Node content = getActualContent();
        double contentAvailableWidth = width == Region.USE_COMPUTED_SIZE ? Region.USE_COMPUTED_SIZE :
                hasHeader ? width : (width - graphicPrefWidth);
        double contentPrefHeight = content.prefHeight(contentAvailableWidth);

        double expandableContentPrefHeight = 0;
        final Node expandableContent = getExpandableContent();
        if (isExpanded() && expandableContent != null) {
            expandableContentPrefHeight = expandableContent.prefHeight(width);
        }

        double prefHeight = snappedTopInset() +
               headerPrefHeight +
               Math.max(graphicPrefHeight, contentPrefHeight) +
               expandableContentPrefHeight +
               buttonBarPrefHeight +
               snappedBottomInset();

        return snapSizeY(prefHeight);
    }



    /* ************************************************************************
     *
     * Private implementation
     * @param buttonBar
     *
     **************************************************************************/

    private void updateButtons(ButtonBar buttonBar) {
        buttonBar.getButtons().clear();

        // show details button if expandable content is present
        if (hasExpandableContent()) {
            if (detailsButton == null) {
                detailsButton = createDetailsButton();
            }
            ButtonBar.setButtonData(detailsButton, ButtonData.HELP_2);
            buttonBar.getButtons().add(detailsButton);
            ButtonBar.setButtonUniformSize(detailsButton, false);
        }

        boolean hasDefault = false;
        for (ButtonType cmd : getButtonTypes()) {
            Node button = buttonNodes.get(cmd);

            // keep only first default button
            if (button instanceof Button) {
                ButtonData buttonType = cmd.getButtonData();

                ((Button)button).setDefaultButton(!hasDefault && buttonType != null && buttonType.isDefaultButton());
                ((Button)button).setCancelButton(buttonType != null && buttonType.isCancelButton());

                hasDefault |= buttonType != null && buttonType.isDefaultButton();
            }
            buttonBar.getButtons().add(button);
        }
    }

    private Node getActualContent() {
        Node content = getContent();
        return content == null ? contentLabel : content;
    }

    private Node getActualHeader() {
        Node header = getHeader();
        return header == null ? headerTextPanel : header;
    }

    private Node getActualGraphic() {
        return headerTextPanel;
    }

    private void updateHeaderArea() {
        Node header = getHeader();
        if (header != null) {
            if (! getChildren().contains(header)) {
                getChildren().add(header);
            }

            headerTextPanel.setVisible(false);
            headerTextPanel.setManaged(false);
        } else {
            final String headerText = getHeaderText();

            headerTextPanel.getChildren().clear();
            headerTextPanel.getStyleClass().clear();

            // recreate the headerTextNode and add it to the children list.
            headerTextPanel.setMaxWidth(Double.MAX_VALUE);

            if (headerText != null && ! headerText.isEmpty()) {
                headerTextPanel.getStyleClass().add("header-panel"); //$NON-NLS-1$
            }

            // on left of header is the text
            Label headerLabel = new Label(headerText);
            headerLabel.setWrapText(true);
            headerLabel.setAlignment(Pos.CENTER_LEFT);
            headerLabel.setMaxWidth(Double.MAX_VALUE);
            headerLabel.setMaxHeight(Double.MAX_VALUE);
            headerTextPanel.add(headerLabel, 0, 0);

            // on the right of the header is a graphic, if one is specified
            graphicContainer.getChildren().clear();

            if (! graphicContainer.getStyleClass().contains("graphic-container")) { //$NON-NLS-1$)
                graphicContainer.getStyleClass().add("graphic-container"); //$NON-NLS-1$
            }

            final Node graphic = getGraphic();
            if (graphic != null) {
                graphicContainer.getChildren().add(graphic);
            }
            headerTextPanel.add(graphicContainer, 1, 0);

            // column constraints
            ColumnConstraints textColumn = new ColumnConstraints();
            textColumn.setFillWidth(true);
            textColumn.setHgrow(Priority.ALWAYS);
            ColumnConstraints graphicColumn = new ColumnConstraints();
            graphicColumn.setFillWidth(false);
            graphicColumn.setHgrow(Priority.NEVER);
            headerTextPanel.getColumnConstraints().setAll(textColumn , graphicColumn);

            headerTextPanel.setVisible(true);
            headerTextPanel.setManaged(true);
        }
    }

    private void updateContentArea() {
        Node content = getContent();
        if (content != null) {
            if (! getChildren().contains(content)) {
                getChildren().add(content);
            }

            if (! content.getStyleClass().contains("content")) {
                content.getStyleClass().add("content");
            }

            contentLabel.setVisible(false);
            contentLabel.setManaged(false);
        } else {
            final String contentText = getContentText();
            final boolean visible = contentText != null && !contentText.isEmpty();
            contentLabel.setText(visible ? contentText : "");
            contentLabel.setVisible(visible);
            contentLabel.setManaged(visible);
        }
    }

    boolean hasHeader() {
        return getHeader() != null || isTextHeader();
    }

    private boolean isTextHeader() {
        String headerText = getHeaderText();
        return headerText != null && !headerText.isEmpty();
    }

    boolean hasExpandableContent() {
        return getExpandableContent() != null;
    }

    void setDialog(Dialog<?> dialog) {
        this.dialog = dialog;
    }



    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static class StyleableProperties {

        private static final CssMetaData<DialogPane,String> GRAPHIC =
            new CssMetaData<DialogPane,String>("-fx-graphic",
                StringConverter.getInstance()) {

            @Override
            public boolean isSettable(DialogPane n) {
                // Note that we care about the graphic, not imageUrl
                return n.graphicProperty == null || !n.graphicProperty.isBound();
            }

            @Override
            public StyleableProperty<String> getStyleableProperty(DialogPane n) {
                return n.imageUrlProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Region.getClassCssMetaData());
            Collections.addAll(styleables,
                GRAPHIC
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /** {@inheritDoc} */
    @Override public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }
}
