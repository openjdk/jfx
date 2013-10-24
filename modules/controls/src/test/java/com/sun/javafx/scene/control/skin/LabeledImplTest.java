/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import com.sun.javafx.pgstub.StubImageLoaderFactory;
import com.sun.javafx.pgstub.StubPlatformImageInfo;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.css.StyleConverter;
import javafx.css.CssMetaData;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javafx.beans.value.WritableValue;
import javafx.css.Styleable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.OverrunStyle;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javax.swing.GroupLayout;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class LabeledImplTest {

    @BeforeClass
    public static void configureImageLoaderFactory() {
        final StubImageLoaderFactory imageLoaderFactory =
                ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory();
        imageLoaderFactory.reset();
        imageLoaderFactory.registerImage(LabeledImpl.class.getResource("caspian/center-btn.png").toExternalForm(),
                new StubPlatformImageInfo(32, 32));
    }

    private static final Labeled LABELED = new Label("label");
    private static final LabeledImpl LABELED_IMPL = new LabeledImpl(LABELED);
    
    private static class Configuration {
        final WritableValue source;
        final WritableValue mirror;
        final Object value;
        Configuration(WritableValue source, WritableValue mirror, Object value) {
            this.source = source;
            this.mirror = mirror;
            this.value = value;
        }        
    }
    
    private static Configuration config(CssMetaData styleable) {
        WritableValue source = styleable.getStyleableProperty(LABELED);
        WritableValue mirror   = styleable.getStyleableProperty(LABELED_IMPL);
        Object value = null;
        if (source != null && mirror != null) {
            final String prop = styleable.getProperty();
            if ("-fx-cursor".equals(prop)) {
                value = Cursor.HAND;                
            } else if ("-fx-effect".equals(prop)) {
                value = new ColorAdjust(.5, .5, .5, .5);
            } else if ("-fx-focus-traversable".equals(prop)) {
                value = Boolean.FALSE;
            } else if ("-fx-opacity".equals(prop)) {
                value = .5;
            } else if ("-fx-blend-mode".equals(prop)) {
                value = BlendMode.RED;
            } else if ("-fx-rotate".equals(prop)) {
                value = .5;
            } else if ("-fx-scale-x".equals(prop)) {
                value = .5;
            } else if ("-fx-scale-y".equals(prop)) {
                value = .5;
            } else if ("-fx-scale-z".equals(prop)) {
                value = .5;
            } else if ("-fx-translate-x".equals(prop)) {
                value = .5;
            } else if ("-fx-translate-y".equals(prop)) {
                value = .5;
            } else if ("-fx-translate-z".equals(prop)) {
                value = .5;
            } else if ("visibility".equals(prop)) {
                value = Boolean.FALSE;
            } else if ("-fx-font".equals(prop)) {
                value = Font.font("Amble", 15);
            } else if ("-fx-alignment".equals(prop)) {
                value = Pos.TOP_CENTER;
            } else if ("-fx-text-alignment".equals(prop)) {
                value = TextAlignment.RIGHT;
            } else if ("-fx-text-fill".equals(prop)) {
                value = Color.RED;
            } else if ("-fx-text-overrun".equals(prop)) {
                value = OverrunStyle.LEADING_WORD_ELLIPSIS;
            } else if ("-fx-wrap-text".equals(prop)) {
                value = Boolean.TRUE;
            } else if ("-fx-graphic".equals(prop)) {
                value = LabeledImpl.class.getResource("caspian/center-btn.png").toExternalForm();
            } else if ("-fx-underline".equals(prop)) {
                value = Boolean.TRUE;
            } else if ("-fx-content-display".equals(prop)) {
                value = ContentDisplay.GRAPHIC_ONLY;
            } else if ("-fx-label-padding".equals(prop)) {
                value = new Insets(1,2,3,4);
            } else if ("-fx-graphic-text-gap".equals(prop)) {
                value = .5;
            } else if ("-fx-ellipsis-string".equals(prop)) {
                value = "...";
            } else if ("-fx-line-spacing".equals(prop)) {
                value = 0.0;
            } else {
                fail(prop + " not accounted for");
                return null;
            }          
            
            return new Configuration(source, mirror, value);
        }

        fail();
        return null;
    }
    
    private final Configuration configuration;

    @Parameters
    public static Collection<Configuration[]> data() {

        Collection<Configuration[]> data = new ArrayList<Configuration[]>();
        
        List<CssMetaData<? extends Styleable, ?>> styleables = LabeledImpl.StyleableProperties.STYLEABLES_TO_MIRROR;
        for(CssMetaData<? extends Styleable, ?> styleable : styleables) {
            
            // LabeledImpl doesn't track -fx-skin since the Labeled
            // isn't necessarily a Label
            if ("-fx-skin".equals(styleable.getProperty())) continue;
            
            Configuration[] config = new Configuration[] { config(styleable) };
            if (config != null) data.add(config);
        }
        
        data.add( new Configuration[] { 
            new Configuration(LABELED.textProperty(), LABELED_IMPL.textProperty(), "TEST 1 2 3")
        });
        
        return data;
    }
    
    @Test 
    public void testMirrorReflectsSource() {
        final WritableValue source = configuration.source;
        final WritableValue mirror = configuration.mirror;
        final Object expected = configuration.value;
        
        source.setValue(expected);
        assertEquals(mirror.toString(), expected, mirror.getValue());
    }

    public LabeledImplTest(Configuration configuration) {
        this.configuration = configuration;
    }

    static {
    }
}
