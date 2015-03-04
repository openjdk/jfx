/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.calculator;

import com.javafx.experiments.dukepad.core.Fonts;
import com.sun.javafx.util.Utils;
import com.sun.javafx.scene.control.skin.ButtonSkin;
import com.sun.javafx.scene.control.skin.TextFieldSkin;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * This is used in place of CSS because the CSS processing was just too slow.
 */
public class CalculatorTheme {
    private static final Color SHADOW_C1 = new Color(1, 1, 1, .07);
    private static final Color SHADOW_C2 = new Color(1, 1, 1, .7);
    private static final Color SHADOW_C3 = new Color(1, 1, 1, .75);

    private static final Color DARK_TEXT_COLOR = Color.BLACK;
    private static final Color MID_TEXT_COLOR = Color.web("#333");
    private static final Color LIGHT_TEXT_COLOR = Color.WHITE;
    private static final Color FOCUS_COLOR = Color.web("#039ED3");
    private static final Color FAINT_FOCUS_COLOR = Color.web("#039ED322");

    private static final Color BASE = Color.web("#ececec");
    private static final Color LIGHT_BASE = Color.web("#e4e4e4");
    private static final Color DARK_BASE = Color.web("#3d4148");
    private static final Color BLUE_BASE = Color.web("#237eb8");
    private static final Color ORANGE_BASE = Color.web("#e35f15");

    private static final Font FONT = Fonts.dosisSemiBold(50);
    private static final Font DARK_FONT = Fonts.dosisSemiBold(65);

    private static final Insets DARK_PADDING = new Insets(-7, 0, 7, 0);
    private static final Insets BLUE_PADDING = new Insets(-4, 0, 4, 0);

    private static final Insets SHADOW_INSETS = new Insets(0, 0, -1, 0);
    private static final Insets OUTER_INSETS = Insets.EMPTY;
    private static final Insets INNER_INSETS = new Insets(1);
    private static final Insets BODY_INSETS = new Insets(2);

    private static final CornerRadii SHADOW_RADII = new CornerRadii(3);
    private static final CornerRadii OUTER_RADII = SHADOW_RADII;
    private static final CornerRadii INNER_RADII = new CornerRadii(2);
    private static final CornerRadii BODY_RADII = new CornerRadii(1);

    public static final Background LIGHT_BUTTON_BACKGROUND = new Background(
            new BackgroundFill(computeShadowHighlight(LIGHT_BASE), SHADOW_RADII, SHADOW_INSETS),
            new BackgroundFill(computeOuterBorder(LIGHT_BASE), OUTER_RADII, OUTER_INSETS),
            new BackgroundFill(computeInnerBorder(LIGHT_BASE), INNER_RADII, INNER_INSETS),
            new BackgroundFill(computeBodyColor(LIGHT_BASE), BODY_RADII, BODY_INSETS)
    );
    public static final Background DARK_BUTTON_BACKGROUND = new Background(
            new BackgroundFill(computeShadowHighlight(DARK_BASE), SHADOW_RADII, SHADOW_INSETS),
            new BackgroundFill(computeOuterBorder(DARK_BASE), OUTER_RADII, OUTER_INSETS),
            new BackgroundFill(computeInnerBorder(DARK_BASE), INNER_RADII, INNER_INSETS),
            new BackgroundFill(computeBodyColor(DARK_BASE), BODY_RADII, BODY_INSETS)
    );
    public static final Background BLUE_BUTTON_BACKGROUND = new Background(
            new BackgroundFill(computeShadowHighlight(BLUE_BASE), SHADOW_RADII, SHADOW_INSETS),
            new BackgroundFill(computeOuterBorder(BLUE_BASE), OUTER_RADII, OUTER_INSETS),
            new BackgroundFill(computeInnerBorder(BLUE_BASE), INNER_RADII, INNER_INSETS),
            new BackgroundFill(computeBodyColor(BLUE_BASE), BODY_RADII, BODY_INSETS)
    );
    public static final Background ORANGE_BUTTON_BACKGROUND = new Background(
            new BackgroundFill(computeShadowHighlight(ORANGE_BASE), SHADOW_RADII, SHADOW_INSETS),
            new BackgroundFill(computeOuterBorder(ORANGE_BASE), OUTER_RADII, OUTER_INSETS),
            new BackgroundFill(computeInnerBorder(ORANGE_BASE), INNER_RADII, INNER_INSETS),
            new BackgroundFill(computeBodyColor(ORANGE_BASE), BODY_RADII, BODY_INSETS)
    );

    private static final Paint LIGHT_BUTTON_FILL = computeTextFillColor(LIGHT_BASE);
    private static final Paint DARK_BUTTON_FILL = computeTextFillColor(DARK_BASE);
    private static final Paint BLUE_BUTTON_FILL = computeTextFillColor(BLUE_BASE);
    private static final Paint ORANGE_BUTTON_FILL = computeTextFillColor(ORANGE_BASE);


    private static final Color LIGHT_BASE__PRESSED = Utils.deriveColor(LIGHT_BASE, -.06);
    private static final Color DARK_BASE__PRESSED = Utils.deriveColor(DARK_BASE, -.06);
    private static final Color BLUE_BASE__PRESSED = Utils.deriveColor(BLUE_BASE, -.06);
    private static final Color ORANGE_BASE__PRESSED = Utils.deriveColor(ORANGE_BASE, -.06);

    public static final Background LIGHT_BUTTON_BACKGROUND__PRESSED = new Background(
            new BackgroundFill(computeShadowHighlight(LIGHT_BASE__PRESSED), SHADOW_RADII, SHADOW_INSETS),
            new BackgroundFill(computeOuterBorder(LIGHT_BASE__PRESSED), OUTER_RADII, OUTER_INSETS),
            new BackgroundFill(computeInnerBorder(LIGHT_BASE__PRESSED), INNER_RADII, INNER_INSETS),
            new BackgroundFill(computeBodyColor(LIGHT_BASE__PRESSED), BODY_RADII, BODY_INSETS)
    );
    public static final Background DARK_BUTTON_BACKGROUND__PRESSED = new Background(
            new BackgroundFill(computeShadowHighlight(DARK_BASE__PRESSED), SHADOW_RADII, SHADOW_INSETS),
            new BackgroundFill(computeOuterBorder(DARK_BASE__PRESSED), OUTER_RADII, OUTER_INSETS),
            new BackgroundFill(computeInnerBorder(DARK_BASE__PRESSED), INNER_RADII, INNER_INSETS),
            new BackgroundFill(computeBodyColor(DARK_BASE__PRESSED), BODY_RADII, BODY_INSETS)
    );
    public static final Background BLUE_BUTTON_BACKGROUND__PRESSED = new Background(
            new BackgroundFill(computeShadowHighlight(BLUE_BASE__PRESSED), SHADOW_RADII, SHADOW_INSETS),
            new BackgroundFill(computeOuterBorder(BLUE_BASE__PRESSED), OUTER_RADII, OUTER_INSETS),
            new BackgroundFill(computeInnerBorder(BLUE_BASE__PRESSED), INNER_RADII, INNER_INSETS),
            new BackgroundFill(computeBodyColor(BLUE_BASE__PRESSED), BODY_RADII, BODY_INSETS)
    );
    public static final Background ORANGE_BUTTON_BACKGROUND__PRESSED = new Background(
            new BackgroundFill(computeShadowHighlight(ORANGE_BASE__PRESSED), SHADOW_RADII, SHADOW_INSETS),
            new BackgroundFill(computeOuterBorder(ORANGE_BASE__PRESSED), OUTER_RADII, OUTER_INSETS),
            new BackgroundFill(computeInnerBorder(ORANGE_BASE__PRESSED), INNER_RADII, INNER_INSETS),
            new BackgroundFill(computeBodyColor(ORANGE_BASE__PRESSED), BODY_RADII, BODY_INSETS)
    );

    public static void styleLightButton(Button button) {
        styleButton(button, LIGHT_BUTTON_BACKGROUND, LIGHT_BUTTON_BACKGROUND__PRESSED, LIGHT_BUTTON_FILL);
    }

    public static void styleDarkButton(Button button) {
        styleButton(button, DARK_BUTTON_BACKGROUND, DARK_BUTTON_BACKGROUND__PRESSED, DARK_BUTTON_FILL);
        button.setFont(DARK_FONT);
        button.setPadding(DARK_PADDING);
    }

    public static void styleBlueButton(Button button) {
        styleButton(button, BLUE_BUTTON_BACKGROUND, BLUE_BUTTON_BACKGROUND__PRESSED, BLUE_BUTTON_FILL);
        button.setPadding(BLUE_PADDING);
    }

    public static void styleOrangeButton(Button button) {
        styleButton(button, ORANGE_BUTTON_BACKGROUND, ORANGE_BUTTON_BACKGROUND__PRESSED, ORANGE_BUTTON_FILL);
        button.setTextFill(Color.WHITE);
    }

    public static void styleEqualsButton(Button button) {
        styleOrangeButton(button);
        button.setFont(DARK_FONT);
        button.setPadding(DARK_PADDING);
    }

    public static void styleBackground(Region parent) {
        parent.setBackground(new Background(new BackgroundFill(BASE, null, null)));
    }

    public static void styleTextField(TextField textField) {
        final Color controlInnerBackground = Utils.deriveColor(BASE, .8);
        final Color textInnerColor = Utils.ladder(controlInnerBackground, new Stop[]{
                new Stop(.45, LIGHT_TEXT_COLOR),
                new Stop(.46, LIGHT_TEXT_COLOR),
                new Stop(.59, DARK_TEXT_COLOR),
                new Stop(.60, MID_TEXT_COLOR)
        });
        final Color textBoxBorder = Utils.ladder(BASE, new Stop[] {
                new Stop(.1, Color.BLACK),
                new Stop(.3, Utils.deriveColor(BASE, -.15))
        });
        final Paint backgroundColor = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Utils.deriveColor(textBoxBorder, -.1)),
                new Stop(1, textBoxBorder)
        );
        final Paint backgroundColor2 = new LinearGradient(0, 0, 0, 5, false, CycleMethod.NO_CYCLE,
                new Stop(0, Utils.deriveColor(controlInnerBackground, -.09)),
                new Stop(1, controlInnerBackground)
        );
        textField.setFont(FONT);
        textField.setBackground(new Background(
                new BackgroundFill(backgroundColor, new CornerRadii(3), Insets.EMPTY),
                new BackgroundFill(backgroundColor2, new CornerRadii(2), new Insets(1))
        ));
        textField.setPadding(new Insets(.333333 * FONT.getSize(), .583 * FONT.getSize(), .333333 * FONT.getSize(), .583 * FONT.getSize()));
        TextFieldSkin skin = new TextFieldSkin(textField) {
            {
                textFill.set(textInnerColor);
            }
        };
        textField.setSkin(skin);
    }

    private static void styleButton(final Button button, final Background background, final Background pressed, Paint textFill) {
        button.setBackground(background);
        button.setTextFill(textFill);
        button.setAlignment(Pos.CENTER);
        button.setTextAlignment(TextAlignment.CENTER);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setPadding(Insets.EMPTY);
        button.setFont(FONT);
        button.pressedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    button.setBackground(pressed);
                } else {
                    button.setBackground(background);
                }
            }
        });
        ButtonSkin skin = new ButtonSkin(button);
        button.setSkin(skin);
        // Workaround a bug
        ((Text)skin.getChildren().get(0)).fillProperty().bind(button.textFillProperty());
    }

    /*
    .button:hover {
        -fx-color: -fx-hover-base;
    }
    -fx-hover-base: ladder(
        -fx-base,
        derive(-fx-base,20%) 20%,
        derive(-fx-base,30%) 35%,
        derive(-fx-base,40%) 50%
     );
     */

    /*
    .button:focused {
        -fx-background-color: -fx-focus-color, -fx-inner-border, -fx-body-color, -fx-faint-focus-color, -fx-body-color;
        -fx-background-insets: -0.2, 1, 2, -1.4, 2.6;
        -fx-background-radius: 3, 2, 1, 4, 1;
    }
     */

    /*
        .background {
            -fx-background-color: -fx-background;
        }
     */

    private static Color deriveBackground(Color base) {
        return Utils.deriveColor(base, .264);
    }

    private static Paint computeShadowHighlight(Color base) {
        /*
        -fx-shadow-highlight-color: ladder(
            -fx-background,
            rgba(255,255,255,0.07) 0%,
            rgba(255,255,255,0.07) 20%,
            rgba(255,255,255,0.07) 70%,
            rgba(255,255,255,0.7) 90%,
            rgba(255,255,255,0.75) 100%
          );
         */
        return Utils.ladder(deriveBackground(base), new Stop[] {
                new Stop(0, SHADOW_C1),
                new Stop(.2, SHADOW_C1),
                new Stop(.7, SHADOW_C1),
                new Stop(.9, SHADOW_C2),
                new Stop(1, SHADOW_C3)
        });
    }

    private static Paint computeOuterBorder(Color color) {
        /* derive(-fx-color,-23%); */
        return Utils.deriveColor(color, -.23);
    }

    private static Paint computeInnerBorder(Color color) {
        /*
        -fx-inner-border: linear-gradient(to bottom,
                    ladder(
                        -fx-color,
                        derive(-fx-color,30%) 0%,
                        derive(-fx-color,20%) 40%,
                        derive(-fx-color,25%) 60%,
                        derive(-fx-color,55%) 80%,
                        derive(-fx-color,55%) 90%,
                        derive(-fx-color,75%) 100%
                    ),
                    ladder(
                        -fx-color,
                        derive(-fx-color,20%) 0%,
                        derive(-fx-color,10%) 20%,
                        derive(-fx-color,5%) 40%,
                        derive(-fx-color,-2%) 60%,
                        derive(-fx-color,-5%) 100%
                    ));
         */
        return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Utils.ladder(color, new Stop[] {
                        new Stop(0, Utils.deriveColor(color, .3)),
                        new Stop(.4, Utils.deriveColor(color, .2)),
                        new Stop(.6, Utils.deriveColor(color, .25)),
                        new Stop(.8, Utils.deriveColor(color, .55)),
                        new Stop(.9, Utils.deriveColor(color, .55)),
                        new Stop(1, Utils.deriveColor(color, .75))
                })),
                new Stop(1, Utils.ladder(color, new Stop[] {
                        new Stop(0, Utils.deriveColor(color, .2)),
                        new Stop(.2, Utils.deriveColor(color, .1)),
                        new Stop(.4, Utils.deriveColor(color, .05)),
                        new Stop(.6, Utils.deriveColor(color, -.02)),
                        new Stop(1, Utils.deriveColor(color, -.05))
                }))
        );
    }

    private static Paint computeBodyColor(Color color) {
        /*
            -fx-body-color: linear-gradient(to bottom,
                    ladder(
                        -fx-color,
                        derive(-fx-color,8%) 75%,
                        derive(-fx-color,10%) 80%
                    ),
                    derive(-fx-color,-8%));
         */
        return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Utils.ladder(color, new Stop[] {
                        new Stop(.75, Utils.deriveColor(color, .08)),
                        new Stop(.80, Utils.deriveColor(color, .10))
                })),
                new Stop(1, Utils.deriveColor(color, -.08))
        );

    }

    private static Color computeTextFillColor(Color color) {
        /*
            -fx-text-base-color: ladder(
                -fx-color,
                -fx-light-text-color 45%,
                -fx-dark-text-color  46%,
                -fx-dark-text-color  59%,
                -fx-mid-text-color   60%
            );
         */
        return Utils.ladder(color, new Stop[] {
                new Stop(.45, LIGHT_TEXT_COLOR),
                new Stop(.46, LIGHT_TEXT_COLOR),
                new Stop(.59, DARK_TEXT_COLOR),
                new Stop(.60, MID_TEXT_COLOR)
        });
    }
}
