/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.SimpleStringProperty;

import com.sun.javafx.scene.control.skin.PopupControlSkin;
import com.sun.javafx.scene.control.skin.TooltipSkin;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author lubermud
 */
public class PopupControlTest {
    private PopupControl popup;

    @Before public void setup() {
        popup = new PopupControl();
        // PopupControl normally gets its stylesheet from the owner scene.
        popup.getScene().getStylesheets().add(
            PopupControlSkin.class.getResource("caspian/caspian.css").toExternalForm()
        );
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void usePrefSizeShouldBeNegativeInfinity() {
        assertEquals(PopupControl.USE_PREF_SIZE, Double.NEGATIVE_INFINITY, 0.0D);
    }

    @Test public void useComputedSizeShouldBeNegativeOne() {
        assertEquals(PopupControl.USE_COMPUTED_SIZE, -1.0D, 0.0D);
    }

    @Test public void defaultGetId() {
        assertNull(popup.getId());
    }

    @Test public void setIdShouldWork() {
        popup.setId("Hello");
        assertEquals("Hello", popup.getId());
    }

    @Test public void idPropertyShouldWork() {
        assertNull(popup.idProperty().get());
    }

    @Test public void idPropertyShouldWork2() {
        popup.idProperty().set("Hello");
        assertEquals("Hello", popup.getId());
    }

    @Test public void idPropertyShouldBeBindable() {
        SimpleStringProperty other = new SimpleStringProperty("Hello");
        popup.idProperty().bind(other);
        assertEquals("Hello", popup.getId());
    }

    @Test public void getStyleClassNotNull() {
        assertNotNull(popup.getStyleClass());
    }

    @Test public void getStyleClassAddable() {
        popup.getStyleClass().add("Hello");
        popup.getStyleClass().add("Goodbye");
        assertEquals(2, popup.getStyleClass().size());
    }

    @Test public void getStyleClassStringable1() {
        assertEquals("", popup.getStyleClass().toString());
    }

    @Test public void getStyleClassStringable2() {
        popup.getStyleClass().add("Hello");
        assertEquals("Hello", popup.getStyleClass().toString());
    }
    
    @Test public void getStyleClassStringable3() {
        popup.getStyleClass().add("Hello");
        popup.getStyleClass().add("Goodbye");
        assertEquals("Hello Goodbye", popup.getStyleClass().toString());
    }

    @Test public void styleSetNullGetNull() {
        popup.setStyle(null);
        assertNull(popup.getStyle());
    }

    @Test public void styleSettable() {
        popup.setStyle("Hello");
        assertEquals("Hello", popup.getStyle());
    }

    @Test public void stylePropertySettable() {
        popup.styleProperty().set("Hello");
        assertEquals("Hello", popup.getStyle());
    }

    @Test public void stylePropertyBindable() {
        SimpleStringProperty other = new SimpleStringProperty("Hello");
        popup.styleProperty().bind(other);
        assertEquals("Hello", popup.getStyle());
    }

    @Test public void defaultSkinIsNull() {
        assertNull(popup.getSkin());
    }

    @Test public void getSkinPropertyBean() {
        assertEquals(popup.bridge, popup.skinProperty().getBean());
    }

    @Test public void getSkinPropertyName() {
        assertEquals("skin", popup.skinProperty().getName());
    }

    @Test public void setAndGetSpecifiedSkin() {
        PopupControlSkin skin = new PopupControlSkin();
        popup.setSkin(skin);
        assertEquals(skin, popup.getSkin());
    }

    @Test public void setAndGetNullSkin() {
        popup.setSkin(null);
        assertNull(popup.getSkin());
    }

    @Test public void getNullMinWidth() {
        assertEquals(PopupControl.USE_COMPUTED_SIZE, popup.getMinWidth(), 0.0D);
    }

    @Test public void getNotNullMinWidth() {
        popup.minWidthProperty();
        assertNotNull(popup.getMinWidth());
    }

    @Test public void setAndGetMinWidth() {
        popup.setMinWidth( 3.0D );
        assertEquals( 3.0D, popup.getMinWidth(), 0.0D );
    }

    @Test public void setTwiceAndGetMinWidth() {
        popup.setMinWidth( 3.0D );
        popup.setMinWidth( 6.0D );
        assertEquals( 6.0D, popup.getMinWidth(), 0.0D );
    }

    @Test public void getMinWidthBean() {
        assertEquals( popup, popup.minWidthProperty().getBean() );
    }

    @Test public void getMinWidthName() {
        assertEquals( "minWidth", popup.minWidthProperty().getName() );
    }

    @Test public void minWidthBindable() {
        DoubleProperty other = new DoublePropertyBase( 3.0D ) {
            @Override public Object getBean() { return popup; }
            @Override public String getName() { return "minWidth"; }
        };
        popup.minWidthProperty().bind(other);
        assertEquals( 3.0D, popup.getMinWidth(), 0.0D );
    }

    @Test public void getNullMinHeight() {
        assertEquals(PopupControl.USE_COMPUTED_SIZE, popup.getMinHeight(), 0.0D);
    }

    @Test public void getNotNullMinHeight() {
        popup.minHeightProperty();
        assertNotNull(popup.getMinHeight());
    }

    @Test public void setAndGetMinHeight() {
        popup.setMinHeight( 3.0D );
        assertEquals( 3.0D, popup.getMinHeight(), 0.0D );
    }

    @Test public void setTwiceAndGetMinHeight() {
        popup.setMinHeight( 3.0D );
        popup.setMinHeight( 6.0D );
        assertEquals( 6.0D, popup.getMinHeight(), 0.0D );
    }

    @Test public void getMinHeightBean() {
        assertEquals( popup, popup.minHeightProperty().getBean() );
    }

    @Test public void getMinHeightName() {
        assertEquals( "minHeight", popup.minHeightProperty().getName() );
    }

    @Test public void minHeightBindable() {
        DoubleProperty other = new DoublePropertyBase( 3.0D ) {
            @Override public Object getBean() { return popup; }
            @Override public String getName() { return "minHeight"; }
        };
        popup.minHeightProperty().bind(other);
        assertEquals( 3.0D, popup.getMinHeight(), 0.0D );
    }

    @Test public void setMinSizeAndGetMinWidth() {
        popup.setMinSize( 3.0D, 6.0D );
        assertEquals( 3.0D, popup.getMinWidth(), 0.0D );
    }

    @Test public void setMinSizeAndGetMinHeight() {
        popup.setMinSize( 3.0D, 6.0D );
        assertEquals( 6.0D, popup.getMinHeight(), 0.0D );
    }

    @Test public void getNullPrefWidth() {
        assertEquals(PopupControl.USE_COMPUTED_SIZE, popup.getPrefWidth(), 0.0D);
    }

    @Test public void getNotNullPrefWidth() {
        popup.prefWidthProperty();
        assertNotNull(popup.getPrefWidth());
    }

    @Test public void setAndGetPrefWidth() {
        popup.setPrefWidth( 3.0D );
        assertEquals( 3.0D, popup.getPrefWidth(), 0.0D );
    }

    @Test public void setTwiceAndGetPrefWidth() {
        popup.setPrefWidth( 3.0D );
        popup.setPrefWidth( 6.0D );
        assertEquals( 6.0D, popup.getPrefWidth(), 0.0D );
    }

    @Test public void getPrefWidthBean() {
        assertEquals( popup, popup.prefWidthProperty().getBean() );
    }

    @Test public void getPrefWidthName() {
        assertEquals( "prefWidth", popup.prefWidthProperty().getName() );
    }

    @Test public void prefWidthBindable() {
        DoubleProperty other = new DoublePropertyBase( 3.0D ) {
            @Override public Object getBean() { return popup; }
            @Override public String getName() { return "prefWidth"; }
        };
        popup.prefWidthProperty().bind(other);
        assertEquals( 3.0D, popup.getPrefWidth(), 0.0D );
    }

    @Test public void getNullPrefHeight() {
        assertEquals(PopupControl.USE_COMPUTED_SIZE, popup.getPrefHeight(), 0.0D);
    }

    @Test public void getNotNullPrefHeight() {
        popup.prefHeightProperty();
        assertNotNull(popup.getPrefHeight());
    }

    @Test public void setAndGetPrefHeight() {
        popup.setPrefHeight( 3.0D );
        assertEquals( 3.0D, popup.getPrefHeight(), 0.0D );
    }

    @Test public void setTwiceAndGetPrefHeight() {
        popup.setPrefHeight( 3.0D );
        popup.setPrefHeight( 6.0D );
        assertEquals( 6.0D, popup.getPrefHeight(), 0.0D );
    }

    @Test public void getPrefHeightBean() {
        assertEquals( popup, popup.prefHeightProperty().getBean() );
    }

    @Test public void getPrefHeightName() {
        assertEquals( "prefHeight", popup.prefHeightProperty().getName() );
    }

    @Test public void prefHeightBindable() {
        DoubleProperty other = new DoublePropertyBase( 3.0D ) {
            @Override public Object getBean() { return popup; }
            @Override public String getName() { return "prefHeight"; }
        };
        popup.prefHeightProperty().bind(other);
        assertEquals( 3.0D, popup.getPrefHeight(), 0.0D );
    }

    @Test public void setPrefSizeAndGetPrefWidth() {
        popup.setPrefSize( 3.0D, 6.0D );
        assertEquals( 3.0D, popup.getPrefWidth(), 0.0D );
    }

    @Test public void setPrefSizeAndGetPrefHeight() {
        popup.setPrefSize( 3.0D, 6.0D );
        assertEquals( 6.0D, popup.getPrefHeight(), 0.0D );
    } 
    
    @Test public void getNullMaxWidth() {
        assertEquals(PopupControl.USE_COMPUTED_SIZE, popup.getMaxWidth(), 0.0D);
    }

    @Test public void getNotNullMaxWidth() {
        popup.maxWidthProperty();
        assertNotNull(popup.getMaxWidth());
    }

    @Test public void setAndGetMaxWidth() {
        popup.setMaxWidth( 3.0D );
        assertEquals( 3.0D, popup.getMaxWidth(), 0.0D );
    }

    @Test public void setTwiceAndGetMaxWidth() {
        popup.setMaxWidth( 3.0D );
        popup.setMaxWidth( 6.0D );
        assertEquals( 6.0D, popup.getMaxWidth(), 0.0D );
    }

    @Test public void getMaxWidthBean() {
        assertEquals( popup, popup.maxWidthProperty().getBean() );
    }

    @Test public void getMaxWidthName() {
        assertEquals( "maxWidth", popup.maxWidthProperty().getName() );
    }

    @Test public void maxWidthBindable() {
        DoubleProperty other = new DoublePropertyBase( 3.0D ) {
            @Override public Object getBean() { return popup; }
            @Override public String getName() { return "maxWidth"; }
        };
        popup.maxWidthProperty().bind(other);
        assertEquals( 3.0D, popup.getMaxWidth(), 0.0D );
    }

    @Test public void getNullMaxHeight() {
        assertEquals(PopupControl.USE_COMPUTED_SIZE, popup.getMaxHeight(), 0.0D);
    }

    @Test public void getNotNullMaxHeight() {
        popup.maxHeightProperty();
        assertNotNull(popup.getMaxHeight());
    }

    @Test public void setAndGetMaxHeight() {
        popup.setMaxHeight( 3.0D );
        assertEquals( 3.0D, popup.getMaxHeight(), 0.0D );
    }

    @Test public void setTwiceAndGetMaxHeight() {
        popup.setMaxHeight( 3.0D );
        popup.setMaxHeight( 6.0D );
        assertEquals( 6.0D, popup.getMaxHeight(), 0.0D );
    }

    @Test public void getMaxHeightBean() {
        assertEquals( popup, popup.maxHeightProperty().getBean() );
    }

    @Test public void getMaxHeightName() {
        assertEquals( "maxHeight", popup.maxHeightProperty().getName() );
    }

    @Test public void maxHeightBindable() {
        DoubleProperty other = new DoublePropertyBase( 3.0D ) {
            @Override public Object getBean() { return popup; }
            @Override public String getName() { return "maxHeight"; }
        };
        popup.maxHeightProperty().bind(other);
        assertEquals( 3.0D, popup.getMaxHeight(), 0.0D );
    }

    @Test public void setMaxSizeAndGetMaxWidth() {
        popup.setMaxSize( 3.0D, 6.0D );
        assertEquals( 3.0D, popup.getMaxWidth(), 0.0D );
    }

    @Test public void setMaxSizeAndGetMaxHeight() {
        popup.setMaxSize( 3.0D, 6.0D );
        assertEquals( 6.0D, popup.getMaxHeight(), 0.0D );
    }

    @Test public void nullSkinNodeAndUseComputedSizeYieldsZeroComputedMinWidth() {
        double anyNum = -2;
        popup.setSkin(null);
        popup.setMinWidth(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(0.0D, popup.minWidth(anyNum), 0.0D);
    }

    @Test public void nullSkinNodeAndSpecifiedSizeYieldsSpecifiedComputedMinWidth() {
        double anyNum = -2;
        popup.setSkin(null);
        popup.setMinWidth(3.0D);
        assertEquals(3.0D, popup.minWidth(anyNum), 0.0D);
    }

    @Test public void popupControlSkinNodeAndUseComputedSizeYieldsZeroComputedMinWidth() {
        double anyNum = -2;
        popup.setSkin(new PopupControlSkin());
        popup.setMinWidth(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(0.0D, popup.minWidth(anyNum), 0.0D);
    }

    @Test public void tooltipSkinNodeAndUseComputedSizeYieldsSomeMinWidth() {
        double anyNum = 10;
        Tooltip tooltip = new Tooltip("Hello");
        TooltipSkin skin = new TooltipSkin(tooltip);
        popup.setSkin(skin);
        popup.getScene().getRoot().impl_processCSS(true);

        popup.setMinWidth(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(popup.getSkin().getNode().minWidth(anyNum), popup.minWidth(anyNum), 0.0D);
    }

    @Test public void specifiedSkinNodeAndSpecifiedSizeYieldsSpecifiedComputedMinWidth() {
        double anyNum = -2;
        popup.setSkin(new PopupControlSkin());
        popup.setMinWidth(3.0D);
        assertEquals(3.0D, popup.minWidth(anyNum), 0.0D);
    }

    @Test public void nullSkinNodeAndUseComputedSizeYieldsZeroComputedMinHeight() {
        double anyNum = -2;
        popup.setSkin(null);
        popup.setMinHeight(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(0.0D, popup.minHeight(anyNum), 0.0D);
    }

    @Test public void nullSkinNodeAndSpecifiedSizeYieldsSpecifiedComputedMinHeight() {
        double anyNum = -2;
        popup.setSkin(null);
        popup.setMinHeight(3.0D);
        assertEquals(3.0D, popup.minHeight(anyNum), 0.0D);
    }

    @Test public void popupControlSkinNodeAndUseComputedSizeYieldsZeroComputedMinHeight() {
        double anyNum = -2;
        popup.setSkin(new PopupControlSkin());
        popup.setMinHeight(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(0.0D, popup.minHeight(anyNum), 0.0D);
    }

    @Test public void tooltipSkinNodeAndUseComputedSizeYieldsSomeMinHeight() {
        double anyNum = 10;
        Tooltip tooltip = new Tooltip("Hello");
        TooltipSkin skin = new TooltipSkin(tooltip);
        popup.setSkin(skin);
        popup.getScene().getRoot().impl_processCSS(true);

        popup.setMinHeight(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(popup.getSkin().getNode().minHeight(anyNum), popup.minHeight(anyNum), 0.0D);
    }

    @Test public void specifiedSkinNodeAndSpecifiedSizeYieldsSpecifiedComputedMinHeight() {
        double anyNum = -2;
        popup.setSkin(new PopupControlSkin());
        popup.setMinHeight(3.0D);
        assertEquals(3.0D, popup.minHeight(anyNum), 0.0D);
    }

    @Test public void nullSkinNodeAndUseComputedSizeYieldsZeroComputedPrefWidth() {
        double anyNum = -2;
        popup.setSkin(null);
        popup.setPrefWidth(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(0.0D, popup.prefWidth(anyNum), 0.0D);
    }

    @Test public void nullSkinNodeAndSpecifiedSizeYieldsSpecifiedComputedPrefWidth() {
        double anyNum = -2;
        popup.setSkin(null);
        popup.setPrefWidth(3.0D);
        assertEquals(3.0D, popup.prefWidth(anyNum), 0.0D);
    }

    @Test public void popupControlSkinNodeAndUseComputedSizeYieldsZeroComputedPrefWidth() {
        double anyNum = -2;
        popup.setSkin(new PopupControlSkin());
        popup.setPrefWidth(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(0.0D, popup.prefWidth(anyNum), 0.0D);
    }

    @Test public void tooltipSkinNodeAndUseComputedSizeYieldsSomePrefWidth() {
        double anyNum = 10;
        Tooltip tooltip = new Tooltip("Hello");
        TooltipSkin skin = new TooltipSkin(tooltip);
        popup.setSkin(skin);
        popup.getScene().getRoot().impl_processCSS(true);

        popup.setPrefWidth(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(popup.getSkin().getNode().prefWidth(anyNum), popup.prefWidth(anyNum), 0.0D);
    }

    @Test public void specifiedSkinNodeAndSpecifiedSizeYieldsSpecifiedComputedPrefWidth() {
        double anyNum = -2;
        popup.setSkin(new PopupControlSkin());
        popup.setPrefWidth(3.0D);
        assertEquals(3.0D, popup.prefWidth(anyNum), 0.0D);
    }

    @Test public void nullSkinNodeAndUseComputedSizeYieldsZeroComputedPrefHeight() {
        double anyNum = -2;
        popup.setSkin(null);
        popup.setPrefHeight(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(0.0D, popup.prefHeight(anyNum), 0.0D);
    }

    @Test public void nullSkinNodeAndSpecifiedSizeYieldsSpecifiedComputedPrefHeight() {
        double anyNum = -2;
        popup.setSkin(null);
        popup.setPrefHeight(3.0D);
        assertEquals(3.0D, popup.prefHeight(anyNum), 0.0D);
    }

    @Test public void popupControlSkinNodeAndUseComputedSizeYieldsZeroComputedPrefHeight() {
        double anyNum = -2;
        popup.setSkin(new PopupControlSkin());
        popup.setPrefHeight(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(0.0D, popup.prefHeight(anyNum), 0.0D);
    }

    @Test public void tooltipSkinNodeAndUseComputedSizeYieldsSomePrefHeight() {
        double anyNum = 10;
        Tooltip tooltip = new Tooltip("Hello");
        TooltipSkin skin = new TooltipSkin(tooltip);
        popup.setSkin(skin);
        popup.getScene().getRoot().impl_processCSS(true);

        popup.setPrefHeight(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(popup.getSkin().getNode().prefHeight(anyNum), popup.prefHeight(anyNum), 0.0D);
    }

    @Test public void specifiedSkinNodeAndSpecifiedSizeYieldsSpecifiedComputedPrefHeight() {
        double anyNum = -2;
        popup.setSkin(new PopupControlSkin());
        popup.setPrefHeight(3.0D);
        assertEquals(3.0D, popup.prefHeight(anyNum), 0.0D);
    }

    @Test public void nullSkinNodeAndUseComputedSizeYieldsZeroComputedMaxWidth() {
        double anyNum = -2;
        popup.setSkin(null);
        popup.setMaxWidth(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(0.0D, popup.maxWidth(anyNum), 0.0D);
    }

    @Test public void nullSkinNodeAndSpecifiedSizeYieldsSpecifiedComputedMaxWidth() {
        double anyNum = -2;
        popup.setSkin(null);
        popup.setMaxWidth(3.0D);
        assertEquals(3.0D, popup.maxWidth(anyNum), 0.0D);
    }

    @Test public void popupControlSkinNodeAndUseComputedSizeYieldsZeroComputedMaxWidth() {
        double anyNum = -2;
        popup.setSkin(new PopupControlSkin());
        popup.setMaxWidth(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(0.0D, popup.maxWidth(anyNum), 0.0D);
    }

    @Test public void tooltipSkinNodeAndUseComputedSizeYieldsSomeMaxWidth() {
        double anyNum = 10;
        Tooltip tooltip = new Tooltip("Hello");
        TooltipSkin skin = new TooltipSkin(tooltip);
        popup.setSkin(skin);
        popup.getScene().getRoot().impl_processCSS(true);

        popup.setMaxWidth(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(popup.getSkin().getNode().maxWidth(anyNum), popup.maxWidth(anyNum), 0.0D);
    }

    @Test public void specifiedSkinNodeAndSpecifiedSizeYieldsSpecifiedComputedMaxWidth() {
        double anyNum = -2;
        popup.setSkin(new PopupControlSkin());
        popup.setMaxWidth(3.0D);
        assertEquals(3.0D, popup.maxWidth(anyNum), 0.0D);
    }

    @Test public void nullSkinNodeAndUseComputedSizeYieldsZeroComputedMaxHeight() {
        double anyNum = -2;
        popup.setSkin(null);
        popup.setMaxHeight(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(0.0D, popup.maxHeight(anyNum), 0.0D);
    }

    @Test public void nullSkinNodeAndSpecifiedSizeYieldsSpecifiedComputedMaxHeight() {
        double anyNum = -2;
        popup.setSkin(null);
        popup.setMaxHeight(3.0D);
        assertEquals(3.0D, popup.maxHeight(anyNum), 0.0D);
    }

    @Test public void popupControlSkinNodeAndUseComputedSizeYieldsZeroComputedMaxHeight() {
        double anyNum = -2;
        popup.setSkin(new PopupControlSkin());
        popup.setMaxHeight(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(0.0D, popup.maxHeight(anyNum), 0.0D);
    }

    @Test public void tooltipSkinNodeAndUseComputedSizeYieldsSomeMaxHeight() {
        double anyNum = 10;
        Tooltip tooltip = new Tooltip("Hello");
        TooltipSkin skin = new TooltipSkin(tooltip);
        popup.setSkin(skin);
        popup.getScene().getRoot().impl_processCSS(true);

        popup.setMaxHeight(PopupControl.USE_COMPUTED_SIZE);
        assertEquals(popup.getSkin().getNode().maxHeight(anyNum), popup.maxHeight(anyNum), 0.0D);
    }

    @Test public void specifiedSkinNodeAndSpecifiedSizeYieldsSpecifiedComputedMaxHeight() {
        double anyNum = -2;
        popup.setSkin(new PopupControlSkin());
        popup.setMaxHeight(3.0D);
        assertEquals(3.0D, popup.maxHeight(anyNum), 0.0D);
    }

    //TODO: test computePref____ methods

}
