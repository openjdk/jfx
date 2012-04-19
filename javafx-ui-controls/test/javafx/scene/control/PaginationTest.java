/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.pgstub.StubToolkit;
import static javafx.scene.control.ControlTestUtils.*;
import com.sun.javafx.scene.control.Pagination;
import com.sun.javafx.scene.control.skin.PaginationSkin;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Test;

public class PaginationTest {
    private Pagination pagination;
    private Toolkit tk;
    private Scene scene;
    private Stage stage;
    private StackPane root;

    @Before public void setup() {
        pagination = new Pagination(1);
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
    }

    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/

    @Test public void defaultConstructorShouldSetStyleClassTo_pagination() {
        assertStyleClassContains(pagination, "pagination");
    }

    @Test public void defaultNumberOfVisiblePages() {
        assertEquals(pagination.getNumberOfVisiblePages(), 10);
    }

    @Test public void defaultItemsPerPage() {
        assertEquals(pagination.getItemsPerPage(), 10);
    }

    @Test public void defaultNumberOfItems() {
        assertEquals(pagination.getNumberOfItems(), 1);
    }

    @Test public void defaultPageIndex() {
        assertEquals(pagination.getPageIndex(), 0);
    }

    @Test public void defaultPageFactory() {
        assertNull(pagination.getPageFactory());
    }

    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/

    @Test public void checkNumberOfVisiblePagesPropertyBind() {
        IntegerProperty intPr = new SimpleIntegerProperty(200);
        pagination.numberOfVisiblePagesProperty().bind(intPr);
        assertEquals("number of visible pages cannot be bound", pagination.numberOfVisiblePagesProperty().getValue(), 200, 0);
        intPr.setValue(105);
        assertEquals("number of visible pages cannot be bound", pagination.numberOfVisiblePagesProperty().getValue(), 105, 0);
    }

    @Test public void checkItemsPerPagePropertyBind() {
        IntegerProperty intPr = new SimpleIntegerProperty(200);
        pagination.itemsPerPageProperty().bind(intPr);
        assertEquals("items per page cannot be bound", pagination.itemsPerPageProperty().getValue(), 200, 0);
        intPr.setValue(105);
        assertEquals("items per page cannot be bound", pagination.itemsPerPageProperty().getValue(), 105, 0);
    }

    @Test public void checkNumberOfItemsPropertyBind() {
        IntegerProperty intPr = new SimpleIntegerProperty(200);
        pagination.numberOfItemsProperty().bind(intPr);
        assertEquals("number of items cannot be bound", pagination.numberOfItemsProperty().getValue(), 200, 0);
        intPr.setValue(105);
        assertEquals("number of items cannot be bound", pagination.numberOfItemsProperty().getValue(), 105, 0);
    }

    @Test public void checkPageIndexPropertyBind() {
        IntegerProperty intPr = new SimpleIntegerProperty(10);
        pagination.pageIndexProperty().bind(intPr);
        assertEquals("page index cannot be bound", pagination.pageIndexProperty().getValue(), 10, 0);
        intPr.setValue(20);
        assertEquals("page index cannot be bound", pagination.pageIndexProperty().getValue(), 20, 0);
    }

    @Test public void checkPageFactoryPropertyBind() {
        Callback callback = new Callback() {
            @Override
            public Object call(Object arg0) {
                return null;
            }
        };
        ObjectProperty objPr = new SimpleObjectProperty(callback);
        pagination.pageFactoryProperty().bind(objPr);
        assertSame("page factory cannot be bound", pagination.pageFactoryProperty().getValue(), callback);
    }

    /*********************************************************************
     * CSS related Tests                                                 *
     ********************************************************************/
    @Test public void whenNumberOfVisiblePagesIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(pagination.numberOfVisiblePagesProperty());
        assertTrue(styleable.isSettable(pagination));
        IntegerProperty intPr = new SimpleIntegerProperty(10);
        pagination.numberOfVisiblePagesProperty().bind(intPr);
        assertFalse(styleable.isSettable(pagination));
    }

    @Test public void whenNumberOfVisiblePagesIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(pagination.numberOfVisiblePagesProperty());
        styleable.set(pagination, 100);
        assertTrue(styleable.isSettable(pagination));
    }

    @Test public void canSpecifyNumberOfVisiblePagesViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(pagination.numberOfVisiblePagesProperty());
        styleable.set(pagination, 100);
        assertSame(100, pagination.getNumberOfVisiblePages());
    }

    /********************************************************************
     * Miscellaneous Tests                                              *
     ********************************************************************/

    @Test public void setPageIndexAndNavigateWithKeyBoard() {
        pagination.setNumberOfItems(100);
        pagination.setPageFactory(new Callback<Integer, Node>() {
            @Override
            public Node call(Integer pageIndex) {
                Node n = createPage(pageIndex);
                return n;
            }
        });
        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        tk.firePulse();
        assertTrue(pagination.isFocused());

        KeyEventFirer keyboard = new KeyEventFirer(pagination);
        keyboard.doRightArrowPress();
        tk.firePulse();

        assertEquals(1, pagination.getPageIndex());

        keyboard.doRightArrowPress();
        tk.firePulse();

        assertEquals(2, pagination.getPageIndex());
    }

    @Test public void setPageIndexAndNavigateWithMouse() {
        pagination.setNumberOfItems(100);
        pagination.setPageFactory(new Callback<Integer, Node>() {
            @Override
            public Node call(Integer pageIndex) {
                Node n = createPage(pageIndex);
                return n;
            }
        });

        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        root.impl_reapplyCSS();
        root.layout();
        tk.firePulse();
        assertTrue(pagination.isFocused());

        double xval = (pagination.localToScene(pagination.getLayoutBounds())).getMinX();
        double yval = (pagination.localToScene(pagination.getLayoutBounds())).getMinY();

        scene.impl_processMouseEvent(
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+170, yval+380));
        tk.firePulse();

        assertEquals(2, pagination.getPageIndex());
    }

    @Test public void setPageIndexAndVerifyCallback() {
        pagination.setNumberOfItems(100);
        pagination.setPageFactory(new Callback<Integer, Node>() {
            @Override
            public Node call(Integer pageIndex) {
                Node n = createPage(pageIndex);
                assertTrue(pageIndex == 0 || pageIndex == 4);
                return n;
            }
        });

        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        pagination.setPageIndex(4);
    }

    @Test public void setNumberOfItemsAndCountNumberOfPages() {
        pagination.setNumberOfItems(50);
        pagination.setPageFactory(new Callback<Integer, Node>() {
            @Override
            public Node call(Integer pageIndex) {
                Node n = createPage(pageIndex);
                return n;
            }
        });

        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        assertEquals(5, pagination.getNumberOfVisiblePages());
    }

    @Test public void setItemsPerPageAndCountNumberOfPages() {
        pagination.setNumberOfItems(50);
        pagination.setItemsPerPage(25);
        pagination.setPageFactory(new Callback<Integer, Node>() {
            @Override
            public Node call(Integer pageIndex) {
                Node n = createPage(pageIndex);
                return n;
            }
        });

        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        assertEquals(2, pagination.getNumberOfVisiblePages());
    }

    @Test public void setNumberOfVisiblePagesToZero() {
        pagination.setNumberOfVisiblePages(0);

        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        assertEquals(1, pagination.getNumberOfVisiblePages());
    }

    @Test public void setNumberOfVisiblePagesGreaterThanTotalNumberOfPages() {
        pagination.setNumberOfVisiblePages(1000);
        pagination.setNumberOfItems(100);
        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        assertEquals(10, pagination.getNumberOfVisiblePages());
    }

    @Test public void setItemsPerPagesToZero() {        
        pagination.setNumberOfItems(100);
        pagination.setItemsPerPage(0);
        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        assertEquals(1, pagination.getItemsPerPage());
    }

    @Test public void setPageIndexLessThanZero() {
        pagination.setNumberOfItems(100);        
        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        pagination.setPageIndex(5);
        pagination.setPageIndex(-1);
        assertEquals(5, pagination.getPageIndex());
    }

    @Test public void setPageIndexGreaterThanTotalNumberOfPages() {
        pagination.setNumberOfItems(100);        
        root.setPrefSize(400, 400);
        root.getChildren().add(pagination);
        show();

        pagination.setPageIndex(5);
        pagination.setPageIndex(500);
        assertEquals(5, pagination.getPageIndex());
    }

    public VBox createPage(int pageIndex) {
        VBox box = new VBox(5);
        int page = pageIndex * pagination.getItemsPerPage();
        for (int i = page; i < page + pagination.getItemsPerPage(); i++) {
            Label l = new Label("PAGE INDEX " + pageIndex);
            box.getChildren().add(l);
        }
        return box;
    }
}