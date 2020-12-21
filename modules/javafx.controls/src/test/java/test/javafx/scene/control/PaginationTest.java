/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import com.sun.javafx.scene.SceneHelper;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import test.com.sun.javafx.pgstub.StubToolkit;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventGenerator;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;

public class PaginationTest {
    private Pagination pagination;
    private Toolkit tk;
    private Scene scene;
    private Stage stage;
    private StackPane root;

    @Before public void setup() {
        pagination = new Pagination();
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
        root = new StackPane();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
    }

    /*********************************************************************
     * Helper methods                                                    *
     ********************************************************************/
    private void show() {
        stage.show();
        stage.requestFocus();
    }

    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/

    @Test public void defaultConstructorShouldSetStyleClassTo_pagination() {
        assertStyleClassContains(pagination, "pagination");
    }

    @Test public void defaultCurrentPageIndex() {
        assertEquals(pagination.getCurrentPageIndex(), 0);
    }

    @Test public void defaultPageCount() {
        assertEquals(pagination.getPageCount(), Pagination.INDETERMINATE);
    }

    @Test public void defaultPageFactory() {
        assertNull(pagination.getPageFactory());
    }

    @Test public void defaultMaxPageIndicatorCount() {
        assertEquals(pagination.getMaxPageIndicatorCount(), 10);
    }

    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/

    @Test public void checkMaxPageIndicatorCountPropertyBind() {
        IntegerProperty intPr = new SimpleIntegerProperty(200);
        pagination.maxPageIndicatorCountProperty().bind(intPr);
        assertEquals("number of visible pages cannot be bound", pagination.maxPageIndicatorCountProperty().getValue(), 200.0, 0.0);
        intPr.setValue(105);
        assertEquals("number of visible pages cannot be bound", pagination.maxPageIndicatorCountProperty().getValue(), 105.0, 0.0);
    }

    @Test(expected = java.lang.UnsupportedOperationException.class) public void checkPageIndexPropertyBind() {
        IntegerProperty intPr = new SimpleIntegerProperty(10);
        pagination.currentPageIndexProperty().bind(intPr);
    }

    @Test public void checkPageFactoryPropertyBind() {
        Callback callback = arg0 -> null;
        ObjectProperty objPr = new SimpleObjectProperty(callback);
        pagination.pageFactoryProperty().bind(objPr);
        assertSame("page factory cannot be bound", pagination.pageFactoryProperty().getValue(), callback);
    }

    /*********************************************************************
     * CSS related Tests                                                 *
     ********************************************************************/
    @Test public void whenMaxPageIndicatorCountIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)pagination.maxPageIndicatorCountProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(pagination));
        IntegerProperty intPr = new SimpleIntegerProperty(10);
        pagination.maxPageIndicatorCountProperty().bind(intPr);
        assertFalse(styleable.isSettable(pagination));
    }

    @Test public void whenMaxPageIndicatorCountIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)pagination.maxPageIndicatorCountProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(pagination));
    }

    @Test public void canSpecifyMaxPageIndicatorCountViaCSS() {
        ((StyleableProperty)pagination.maxPageIndicatorCountProperty()).applyStyle(null, 100);
        assertSame(100, pagination.getMaxPageIndicatorCount());
    }

    /********************************************************************
     * Miscellaneous Tests                                              *
     ********************************************************************/

    @Test public void setCurrentPageIndexAndNavigateWithKeyBoard() {
        pagination.setPageCount(25);
        pagination.setPageFactory(pageIndex -> {
            Node n = createPage(pageIndex);
            return n;
        });
        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        tk.firePulse();
        assertTrue(pagination.isFocused());

        KeyEventFirer keyboard = new KeyEventFirer(pagination);
        keyboard.doRightArrowPress();
        tk.firePulse();

        assertEquals(1, pagination.getCurrentPageIndex());

        keyboard.doRightArrowPress();
        tk.firePulse();

        assertEquals(2, pagination.getCurrentPageIndex());
    }

    @Ignore @Test public void setCurrentPageIndexAndNavigateWithMouse() {
        pagination.setPageCount(25);
        pagination.setPageFactory(pageIndex -> {
            Node n = createPage(pageIndex);
            return n;
        });

        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        root.applyCss();
        root.layout();
        tk.firePulse();
        assertTrue(pagination.isFocused());

        double xval = (pagination.localToScene(pagination.getLayoutBounds())).getMinX();
        double yval = (pagination.localToScene(pagination.getLayoutBounds())).getMinY();

        SceneHelper.processMouseEvent(scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+170, yval+380));
        tk.firePulse();

        assertEquals(3, pagination.getCurrentPageIndex());
    }

    @Test public void setCurrentPageIndexAndVerifyCallback() {
        pagination.setPageCount(25);
        pagination.setPageFactory(pageIndex -> {
            Node n = createPage(pageIndex);
            assertTrue(pageIndex == 0 || pageIndex == 4);
            return n;
        });

        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        pagination.setCurrentPageIndex(4);
    }

    @Test public void setCountToZero() {
        pagination.setPageCount(0);

        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        assertEquals(Integer.MAX_VALUE, pagination.getPageCount());
    }

    @Test public void setCurrentPageIndexLessThanZero() {
        pagination.setPageCount(100);
        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        pagination.setCurrentPageIndex(5);
        pagination.setCurrentPageIndex(-1);
        assertEquals(0, pagination.getCurrentPageIndex());
    }

    @Test public void setCurrentPageIndexGreaterThanPageCount() {
        pagination.setPageCount(100);
        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        pagination.setCurrentPageIndex(5);
        pagination.setCurrentPageIndex(100);
        assertEquals(99, pagination.getCurrentPageIndex());
    }

    @Test public void setMaxPageIndicatorCountLessThanZero() {
        pagination.setPageCount(100);
        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        pagination.setMaxPageIndicatorCount(-1);
        assertEquals(10, pagination.getMaxPageIndicatorCount());
    }

    @Test public void setMaxPageIndicatorCountGreaterThanPageCount() {
        pagination.setPageCount(100);
        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        pagination.setMaxPageIndicatorCount(101);
        assertEquals(10, pagination.getMaxPageIndicatorCount());
    }

    @Test public void pageCountIsLessThanMaxPageIndicatorCount_RT21660() {
        pagination.setPageCount(5);
        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        pagination.setCurrentPageIndex(4);
        tk.firePulse();
        assertTrue(pagination.isFocused());

        KeyEventFirer keyboard = new KeyEventFirer(pagination);
        keyboard.doRightArrowPress();
        tk.firePulse();

        assertEquals(4, pagination.getCurrentPageIndex());
    }

    @Test public void divideByZeroErrorWhenSizeIsSmall_RT22687() {
        pagination.setPageCount(15);
        root.setMaxSize(100, 200);
        root.getChildren().add(pagination);
        show();

        try {
            KeyEventFirer keyboard = new KeyEventFirer(pagination);
            keyboard.doRightArrowPress();
            tk.firePulse();
        } catch (Exception e) {
            fail();
        }
        assertEquals(1, pagination.getCurrentPageIndex());
    }

    public VBox createPage(int pageIndex) {
        VBox box = new VBox(5);
        int page = pageIndex * 10;
        for (int i = page; i < page + 10; i++) {
            Label l = new Label("PAGE INDEX " + pageIndex);
            box.getChildren().add(l);
        }
        return box;
    }
}
