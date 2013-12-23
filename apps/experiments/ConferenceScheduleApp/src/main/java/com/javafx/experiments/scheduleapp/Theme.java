/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates.
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
package com.javafx.experiments.scheduleapp;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * This is a very simple theme of constants for styles used though out the 
 * application. This allows us to avoid the cost of CSS.
 */
public class Theme {
    public static final String DEFAULT_FONT_NAME = "Helvetica";
    public static final Image RIGHT_ARROW = new Image(
            Theme.class.getResource("images/popover-arrow.png").toExternalForm());
    public static final Image STAR = new Image(
            Theme.class.getResource("images/star.png").toExternalForm());
    public static Image SHADOW_PIC = new Image(
            Theme.class.getResource("images/pic-shadow.png").toExternalForm());
    public static Image DUKE_48 = new Image(
            Theme.class.getResource("images/duke48.png").toExternalForm());
    public static final Image TICK_IMAGE = new Image(
            Theme.class.getResource("images/tick.png").toExternalForm());
    public static final Font HUGE_FONT = Font.font(DEFAULT_FONT_NAME, FontWeight.BOLD, 40);
    public static final Font LARGE_FONT = Font.font(DEFAULT_FONT_NAME, FontWeight.BOLD, 20);
    public static final Font LARGE_LIGHT_FONT = Font.font(DEFAULT_FONT_NAME, FontWeight.NORMAL, 18);
    public static final Font BASE_FONT = Font.font(DEFAULT_FONT_NAME, FontWeight.BOLD, 14);
    public static final Font LIGHT_FONT = Font.font(DEFAULT_FONT_NAME, FontWeight.NORMAL, 14);
    public static final Font SMALL_FONT = Font.font(DEFAULT_FONT_NAME, FontWeight.BOLD, 10);
    public static final Color BLUE = Color.web("#00a8cc");
    public static final Color PINK = Color.web("#ea0068");
    public static final Color GRAY = Color.web("#5f5f5f");
    public static final Color VLIGHT_GRAY = Color.web("#9b9b9b");
    public static final Color DARK_GREY = Color.web("#363636");
}
