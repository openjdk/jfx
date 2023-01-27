package javafx.scene.control;

import javafx.beans.property.*;
import javafx.css.*;
import javafx.css.converter.InsetsConverter;
import javafx.css.converter.SizeConverter;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.skin.SceneDecorationSkin;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SceneDecoration extends Control {
    private static final String DEFAULT_STYLE_CLASS = "decoration";

    private final Stage stage;


    public SceneDecoration(Stage stage) {
        this.stage = stage;
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
    }

    public SceneDecoration(Stage stage, Node content) {
        this(stage);
        setContent(content);
    }

    public Stage getStage() {
        return stage;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new SceneDecorationSkin(this, stage);
    }



    /* *************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/
    private static class StyleableProperties {
        private static final CssMetaData<SceneDecoration, Number> TITLE_BAR_SPACING =
                new CssMetaData<>("-fx-title-bar-spacing",
                        SizeConverter.getInstance(), 0d) {

                    @Override
                    public boolean isSettable(SceneDecoration node) {
                        return node.titleBarSpacing == null || !node.titleBarSpacing.isBound();
                    }

                    @Override
                    public StyleableProperty<Number> getStyleableProperty(SceneDecoration node) {
                        return (StyleableProperty<Number>) node.titleBarSpacingProperty();
                    }
                };

        private static final CssMetaData<SceneDecoration, Insets> SHADOW_INSETS =
                new CssMetaData<>("-fx-shadow-insets",
                        InsetsConverter.getInstance(), Insets.EMPTY) {

                    @Override
                    public boolean isSettable(SceneDecoration node) {
                        return node.shadowInsets == null || !node.shadowInsets.isBound();
                    }


                    @Override
                    public StyleableProperty<Insets> getStyleableProperty(SceneDecoration node) {
                        return (StyleableProperty<Insets>) node.shadowInsetsProperty();
                    }
                };
        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                    new ArrayList<>(Region.getClassCssMetaData());
            styleables.add(TITLE_BAR_SPACING);
            styleables.add(SHADOW_INSETS);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    public final DoubleProperty titleBarSpacingProperty() {
        if (titleBarSpacing == null) {
            titleBarSpacing = new StyleableDoubleProperty() {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData getCssMetaData () {
                    return SceneDecoration.StyleableProperties.TITLE_BAR_SPACING;
                }

                @Override
                public Object getBean() {
                    return SceneDecoration.this;
                }

                @Override
                public String getName() {
                    return "titleBarSpacing";
                }
            };
        }

        return titleBarSpacing;
    }

    private DoubleProperty titleBarSpacing;
    public final void setTitleBarSpacing(double value) { titleBarSpacingProperty().set(value); }
    public final double getTitleBarSpacing() { return titleBarSpacing == null ? 0 : titleBarSpacing.get(); }


    public final ObjectProperty<Insets> shadowInsetsProperty() {
        if (shadowInsets == null) {
            shadowInsets = new StyleableObjectProperty<Insets>(Insets.EMPTY) {
                private Insets lastValidValue = Insets.EMPTY;

                @Override
                public void invalidated() {
                    final Insets newValue = get();
                    if (newValue == null) {
                        set(lastValidValue);
                        throw new NullPointerException("cannot set labelPadding to null");
                    }
                    lastValidValue = newValue;
                    requestLayout();
                }

                @Override
                public CssMetaData<SceneDecoration, Insets> getCssMetaData() {
                    return StyleableProperties.SHADOW_INSETS;
                }

                @Override
                public Object getBean() {
                    return SceneDecoration.this;
                }

                @Override
                public String getName() {
                    return "shadowInsets";
                }
            };
        }

        return shadowInsets;
    }

    private ObjectProperty<Insets> shadowInsets;
    private void setShadowInsets(Insets value) { shadowInsetsProperty().set(value); }
    public final Insets getShadowInsets() { return shadowInsets == null ? Insets.EMPTY : shadowInsets.get(); }


    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return SceneDecoration.StyleableProperties.STYLEABLES;
    }

    @Override
    protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    private final ObjectProperty<HPos> headerButtonsPosition = new SimpleObjectProperty<>(HPos.RIGHT);

    public HPos getHeaderButtonsPosition() {
        return headerButtonsPosition.get();
    }

    public ObjectProperty<HPos> headerButtonsPositionProperty() {
        return headerButtonsPosition;
    }

    public void setHeaderButtonsPosition(HPos headerButtonsPosition) {
        this.headerButtonsPosition.set(headerButtonsPosition);
    }

    private ObjectProperty<Node> content;

    public final void setContent(Node value) {
        contentProperty().set(value);
    }

    public final Node getContent() {
        return content == null ? null : content.get();
    }

    public final ObjectProperty<Node> contentProperty() {
        if (content == null) {
            content = new SimpleObjectProperty<>(this, "content");
        }
        return content;
    }

    private final BooleanProperty showHeaderButtons = new SimpleBooleanProperty(this, "showHeaderButtons", true);

    public boolean isShowHeaderButtons() {
        return showHeaderButtons.get();
    }

    public BooleanProperty showHeaderButtonsProperty() {
        return showHeaderButtons;
    }

    public void setShowHeaderButtons(boolean showHeaderButtons) {
        this.showHeaderButtons.set(showHeaderButtons);
    }

    private final BooleanProperty showTitle = new SimpleBooleanProperty(this, "showTitle", true);

    public boolean isShowTitle() {
        return showTitle.get();
    }

    public BooleanProperty showTitleProperty() {
        return showTitle;
    }

    public void setShowTitle(boolean showTitle) {
        this.showTitle.set(showTitle);
    }

    private final BooleanProperty showIcon = new SimpleBooleanProperty(this, "showIcon", true);

    public boolean isShowIcon() {
        return showIcon.get();
    }

    public BooleanProperty showIconProperty() {
        return showIcon;
    }

    public void setShowIcon(boolean showIcon) {
        this.showIcon.set(showIcon);
    }

    private ObjectProperty<Node> headerLeft;

    public final ObjectProperty<Node> headerLeftProperty() {
        if (headerLeft == null) {
            headerLeft = new SimpleObjectProperty<>(this, "left");
        }
        return headerLeft;
    }

    public final void setHeaderLeft(Node value) {
        headerLeftProperty().set(value);
    }

    public final Node getHeaderLeft() {
        return headerLeft == null ? null : headerLeft.get();
    }

    private ObjectProperty<Node> headerRight;

    public final ObjectProperty<Node> headerRightProperty() {
        if (headerRight == null) {
            headerRight = new SimpleObjectProperty<>(this, "right");
        }
        return headerRight;
    }

    public final void setHeaderRight(Node value) {
        headerRightProperty().set(value);
    }

    public final Node getHeaderRight() {
        return headerRight == null ? null : headerRight.get();
    }
}