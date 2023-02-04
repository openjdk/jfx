/*
 *  Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package javafx.scene.control.skin;

import com.sun.javafx.scene.control.ListenerHelper;
import com.sun.javafx.scene.control.behavior.SceneDecorationBehaviour;
import com.sun.javafx.stage.WindowHelper;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SceneDecoration;
import javafx.scene.control.SkinBase;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SceneDecorationSkin extends SkinBase<SceneDecoration> {
    private final Stage stage;
    private final ContainerRegion container;
    private final SceneDecorationBehaviour behaviour;

    private HeaderRegion headerRegion = null;

    private static final PseudoClass PSEUDO_CLASS_FOCUSED =
            PseudoClass.getPseudoClass("focused");

    private static final PseudoClass PSEUDO_CLASS_MAXIMIZED =
            PseudoClass.getPseudoClass("maximized");

    private static final PseudoClass PSEUDO_CLASS_FULL_SCREEN =
            PseudoClass.getPseudoClass("full-screen");

    private static final PseudoClass PSEUDO_CLASS_SOLID =
            PseudoClass.getPseudoClass("solid");


    public SceneDecorationSkin(SceneDecoration control, Stage stage) {
        super(control);
        this.stage = stage;

        behaviour = new SceneDecorationBehaviour(control);
        container = new ContainerRegion();

        getChildren().add(container);

        ListenerHelper lh = ListenerHelper.get(this);

        lh.addChangeListener(this::updateChildren, true, control.showIconProperty(), control.showTitleProperty(),
                control.headerLeftProperty(), control.headerRightProperty(), control.headerButtonsPositionProperty(),
                stage.resizableProperty(), stage.fullScreenProperty(), control.contentProperty());

        lh.addListChangeListener(stage.getIcons(), lc -> updateChildren());

        lh.addChangeListener(stage.focusedProperty(),
                e -> getSkinnable().pseudoClassStateChanged(PSEUDO_CLASS_FOCUSED, e));

        lh.addChangeListener(stage.maximizedProperty(),
                e -> getSkinnable().pseudoClassStateChanged(PSEUDO_CLASS_MAXIMIZED, e));

        lh.addChangeListener(stage.fullScreenProperty(),
                e -> getSkinnable().pseudoClassStateChanged(PSEUDO_CLASS_FULL_SCREEN, e));

        lh.addEventHandler(stage, WindowEvent.WINDOW_SHOWN,
                e -> pseudoClassStateChanged(PSEUDO_CLASS_SOLID, stage.getStyle() != StageStyle.TRANSPARENT));

        lh.addEventHandler(stage, WindowEvent.WINDOW_SHOWN, e-> updateShadow());

        lh.addChangeListener(this::updateShadow, control.shadowInsetsProperty());
    }

    private void updateShadow() {
        WindowHelper.getWindowAccessor().setShadowInsets(stage, getSkinnable().getShadowInsets());
    }

    private void updateChildren() {
        container.getChildren().clear();

        if (headerRegion == null && !stage.isFullScreen()) {
            headerRegion = new HeaderRegion();
            behaviour.setHeaderRegion(headerRegion);
        }

        if (headerRegion != null && !stage.isFullScreen()) {
            container.getChildren().add(headerRegion);
            headerRegion.update();
        }

        var content = getSkinnable().getContent();

        if (content != null) {
            container.getChildren().add(content);
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        behaviour.dispose();
    }

    static class ContainerRegion extends VBox {
        public ContainerRegion() {
            getStyleClass().setAll("container");
        }
    }

    class HeaderRegion extends Region {
        private HeaderLeftRegion leftRegion = null;
        private HeaderRightRegion rightRegion = null;
        private HeaderButtonsRegion headerButtons = null;

        private IconRegion icon = null;
        private TitleRegion title = null;

        HeaderRegion() {
            getStyleClass().setAll("header");
        }

        private void update() {
            System.out.println("HeaderRegion -> update");
            getChildren().clear();
            updateIcon();
            updateLeft();
            updateTitle();
            updateRight();
            updateButtons();

            requestLayout();
        }

        private void updateButtons() {
            if (getSkinnable().isShowHeaderButtons()) {
                if (headerButtons == null) {
                    headerButtons = new HeaderButtonsRegion();
                }
                getChildren().add(headerButtons);
                headerButtons.update();
            } else {
                headerButtons = null;
            }
        }

        private void updateTitle() {
            if (getSkinnable().isShowTitle()) {
                if (title == null) {
                    title = new TitleRegion();
                }
                getChildren().add(title);
            } else {
                title = null;
            }
        }

        private void updateIcon() {
            if (getSkinnable().isShowIcon() && !stage.getIcons().isEmpty()) {
                if (icon == null) {
                    icon = new IconRegion();
                }
                getChildren().add(icon);
                icon.update();
            } else {
                icon = null;
            }
        }

        private void updateRight() {
            if (getSkinnable().getHeaderRight() != null) {
                if (rightRegion == null) {
                    rightRegion = new HeaderRightRegion();
                }
                getChildren().add(rightRegion);
                rightRegion.update();
            } else {
                rightRegion = null;
            }
        }

        private void updateLeft() {
            if (getSkinnable().getHeaderLeft() != null) {
                if (leftRegion == null) {
                    leftRegion = new HeaderLeftRegion();
                }
                getChildren().add(leftRegion);
                leftRegion.update();
            } else {
                leftRegion = null;
            }
        }

        private double getY(double h) {
            return (getHeight() - h) / 2;
        }

        @Override
        protected void layoutChildren() {
            double left = snappedLeftInset();
            double right = snappedRightInset();
            double spacing = getSkinnable().getTitleBarSpacing();

            double w = getWidth() - snappedLeftInset() - snappedRightInset();
            double mh = getHeight() - snappedTopInset() - snappedBottomInset();
            double minWidth = snappedLeftInset() + snappedRightInset();

            boolean buttonsOnLeft = false;
            Bounds iconBounds = null;

            if (headerButtons != null) {
                headerButtons.setMaxHeight(mh);
                headerButtons.autosize();

                if (getSkinnable().getHeaderButtonsPosition() == HPos.LEFT) {
                    headerButtons.relocate(left, getY(headerButtons.getHeight()));
                    left += headerButtons.getWidth() + spacing;
                    buttonsOnLeft = true;
                }

                minWidth += headerButtons.getWidth() + spacing;
            }

            if (icon != null && icon.getImage() != null) {
                double imgH = icon.getImage().getHeight();

                if (imgH > mh) {
                    icon.setFitHeight(mh);
                }

                icon.relocate(left, getY(icon.getFitHeight()));

                iconBounds = icon.localToParent(icon.getBoundsInLocal());
                left += iconBounds.getWidth() + spacing;

                minWidth += iconBounds.getWidth() + spacing;
            }

            // Title logic
            //  - It's always fitted on the middle
            //  - It should resize considering Icon and Buttons
            if (title != null) {
                double tmw = w;

                tmw -= (headerButtons != null) ? headerButtons.getWidth() : 0;
                tmw -= (iconBounds != null) ? iconBounds.getWidth() : 0;

                title.setMaxWidth((tmw < 0) ? 0 : tmw);
                title.setMaxHeight(mh);
                title.autosize();

                title.relocate((getWidth() - title.getWidth()) / 2, getY(title.getHeight()));

                minWidth += title.getMinWidth();
            }

            if (leftRegion != null) {
                leftRegion.setMaxHeight(mh);

                if (title != null) {
                    Bounds titleBounds = title.localToParent(title.getBoundsInLocal());
                    double mw = titleBounds.getMinX() - left;
                    leftRegion.setMaxWidth(mw);
                }

                double x = snappedLeftInset()
                        + ((iconBounds != null) ? iconBounds.getWidth() + spacing : 0D)
                        + ((buttonsOnLeft) ? headerButtons.getWidth() + spacing : 0D);

                leftRegion.autosize();
                leftRegion.relocate(x, getY(leftRegion.getHeight()));

                left += leftRegion.getWidth() + spacing;
                minWidth += leftRegion.getMinWidth() + spacing;
            }

            if (headerButtons != null
                    && getSkinnable().getHeaderButtonsPosition() == HPos.RIGHT) {

                double hbw = headerButtons.getWidth();
                headerButtons.setMaxHeight(mh);
                headerButtons.autosize();
                headerButtons.relocate(getWidth() - right - hbw, getY(headerButtons.getHeight()));
                right += hbw;
            }

            if (rightRegion != null) {
                rightRegion.setMaxHeight(mh);

                if (title != null) {
                    Bounds titleBounds = title.localToParent(title.getBoundsInLocal());
                    double mw = w - titleBounds.getMaxX() - right;
                    rightRegion.setMaxWidth(mw);
                }

                rightRegion.autosize();
                rightRegion.relocate(getWidth() - right - rightRegion.getWidth() - spacing, getY(rightRegion.getHeight()));

                minWidth += rightRegion.getMinWidth() + spacing;
            }

            setMinWidth(minWidth);
        }
    }

    class TitleRegion extends Label {
        TitleRegion() {
            setMouseTransparent(true);
            getStyleClass().add("title");
            textProperty().bind(stage.titleProperty());
        }
    }

    class IconRegion extends ImageView {
        IconRegion() {
            setManaged(false);
            getStyleClass().add("icon");
            setPreserveRatio(true);
        }

        private void update() {
            double height = headerRegion.getHeight();

            setImage(null);

            //find best height
            stage.getIcons().stream()
                    .min((f1, f2) -> (int) ((f1.getHeight() - height) - (f2.getHeight() - height)))
                    .ifPresent(this::setImage);
        }
    }

    class HeaderButtonsRegion extends HBox {
        private final HeaderButton iconify;
        private final HeaderButton maximize;
        private final HeaderButton close;

        HeaderButtonsRegion() {
            setManaged(false);
            getStyleClass().setAll("header-buttons");

            iconify = new HeaderButton("iconify");
            maximize = new HeaderButton("maximize");
            close = new HeaderButton("close");

            iconify.setOnAction(e -> stage.setIconified(!stage.isIconified()));
            maximize.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));
            close.setOnAction(e -> stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)));
        }

        private void update() {
            getChildren().clear();

            List<Node> buttons = new ArrayList<>();
            buttons.add(iconify);

            if (stage.isResizable()) {
                buttons.add(maximize);
            }

            buttons.add(close);

            if (getSkinnable().getHeaderButtonsPosition() == HPos.LEFT) {
                Collections.reverse(buttons);
            }

            getChildren().addAll(buttons);
        }
    }

    class HeaderLeftRegion extends StackPane {
        HeaderLeftRegion() {
            getStyleClass().setAll("left");
        }

        private void update() {
            getChildren().clear();
            if (getSkinnable().getHeaderLeft() != null) {
                getChildren().setAll(getSkinnable().getHeaderLeft());
            }
        }
    }

    class HeaderRightRegion extends StackPane {
        HeaderRightRegion() {
            getStyleClass().setAll("right");
        }

        private void update() {
            getChildren().clear();
            if (getSkinnable().getHeaderRight() != null) {
                getChildren().setAll(getSkinnable().getHeaderRight());
            }
        }
    }

    static class HeaderButton extends Button {
        HeaderButton(final String css) {
            getStyleClass().add(css);

            StackPane icon = new StackPane();
            icon.getStyleClass().add("icon");
            icon.setId(css);
            setGraphic(icon);
        }
    }
}