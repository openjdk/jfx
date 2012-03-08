/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package javafx.scene.layout;

import static org.junit.Assert.assertEquals;
import javafx.geometry.BoundingBox;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;

import org.junit.Before;
import org.junit.Test;

public class GridPaneDesignInfoTest {
    GridPane gridpane;
    GridPaneDesignInfo designInfo;

    @Before public void setUp() {
        this.designInfo = new GridPaneDesignInfo();
        this.gridpane = new GridPane();
    }
    
    @Test public void testGridPaneGetRowCount() {
        MockResizable child1 = new MockResizable(100,100);
        MockResizable child2 = new MockResizable(100,100);
        MockResizable child3 = new MockResizable(100,100);

        gridpane.addRow(0, child1,child2,child3);

        assertEquals(1, designInfo.getRowCount(gridpane));
        assertEquals(3, designInfo.getColumnCount(gridpane));
    }

    @Test public void testGridPaneGetColumnCount() {
        MockResizable child1 = new MockResizable(100,100);
        MockResizable child2 = new MockResizable(100,100);
        MockResizable child3 = new MockResizable(100,100);

        gridpane.addColumn(0, child1,child2,child3);

        assertEquals(1, designInfo.getColumnCount(gridpane));
        assertEquals(3, designInfo.getRowCount(gridpane));
    }

    @Test public void testGridPaneGetCellBounds() {
        MockResizable child1 = new MockResizable(75, 50, 75, 50, 75, 50);
        MockResizable child2 = new MockResizable(90, 50, 90, 50, 90, 50);
        MockResizable child3 = new MockResizable(115, 100, 115, 100, 115, 100);
        MockResizable child4 = new MockResizable(75, 60, 75, 60, 75, 60);
        MockResizable child5 = new MockResizable(75, 40, 75, 40, 75, 40);
        MockResizable child6 = new MockResizable(110, 60, 110, 60, 110, 60);

        gridpane.addColumn(0, child1, child2, child3, child4, child5, child6);

        GridPane.setConstraints(child1, 0, 0);
        GridPane.setConstraints(child2, 2, 0);
        GridPane.setConstraints(child3, 1, 1);
        GridPane.setConstraints(child4, 3, 1);
        GridPane.setConstraints(child5, 4, 1);
        GridPane.setConstraints(child6, 3, 2);
        GridPane.setHalignment(child3, HPos.LEFT);
        GridPane.setValignment(child3, VPos.TOP);

        GridPane.setRowSpan(child3, 2);
        GridPane.setColumnSpan(child3, 2);
        gridpane.setHgap(5);
        gridpane.setVgap(2);

        ColumnConstraints column1 = new ColumnConstraints(75);
        ColumnConstraints column2 = new ColumnConstraints(25);
        ColumnConstraints column3 = new ColumnConstraints(90);
        ColumnConstraints column4 = new ColumnConstraints(75);
        ColumnConstraints column5 = new ColumnConstraints(110);
        gridpane.getColumnConstraints().addAll(column1, column2, column3, column4, column5);

        RowConstraints row1 = new RowConstraints(50);
        RowConstraints row2 = new RowConstraints(60);
        RowConstraints row3 = new RowConstraints(40);
        RowConstraints row4 = new RowConstraints(50);
        gridpane.getRowConstraints().addAll(row1, row2, row3, row4);
        gridpane.layout();

        assertEquals(new BoundingBox(0, 0, 75, 50), designInfo.getCellBounds(gridpane, 0, 0));
        assertEquals(new BoundingBox(110, 0, 90, 50), designInfo.getCellBounds(gridpane, 2, 0));
        assertEquals(new BoundingBox(80, 52, 25, 60), designInfo.getCellBounds(gridpane, 1, 1));
        assertEquals(new BoundingBox(205, 52, 75, 60), designInfo.getCellBounds(gridpane, 3, 1));
        assertEquals(new BoundingBox(205, 114, 75, 40), designInfo.getCellBounds(gridpane, 3, 2));
        assertEquals(new BoundingBox(285, 52, 110, 60), designInfo.getCellBounds(gridpane, 4, 1));
    }
    
    @Test public void testGridPaneGetCellBoundsWithCENTERAlignment() {
        gridpane.setAlignment(Pos.CENTER);
        MockResizable child1 = new MockResizable(75, 50, 75, 50, 75, 50);
        MockResizable child2 = new MockResizable(90, 50, 90, 50, 90, 50);
        MockResizable child3 = new MockResizable(115, 100, 115, 100, 115, 100);
        MockResizable child4 = new MockResizable(75, 60, 75, 60, 75, 60);
        MockResizable child5 = new MockResizable(75, 40, 75, 40, 75, 40);
        MockResizable child6 = new MockResizable(110, 60, 110, 60, 110, 60);

        gridpane.addColumn(0, child1, child2, child3, child4, child5, child6);

        GridPane.setConstraints(child1, 0, 0);
        GridPane.setConstraints(child2, 2, 0);
        GridPane.setConstraints(child3, 1, 1);
        GridPane.setConstraints(child4, 3, 1);
        GridPane.setConstraints(child5, 4, 1);
        GridPane.setConstraints(child6, 3, 2);
        GridPane.setHalignment(child3, HPos.LEFT);
        GridPane.setValignment(child3, VPos.TOP);

        GridPane.setRowSpan(child3, 2);
        GridPane.setColumnSpan(child3, 2);
        gridpane.setHgap(5);
        gridpane.setVgap(2);

        ColumnConstraints column1 = new ColumnConstraints(75);
        ColumnConstraints column2 = new ColumnConstraints(25);
        ColumnConstraints column3 = new ColumnConstraints(90);
        ColumnConstraints column4 = new ColumnConstraints(75);
        ColumnConstraints column5 = new ColumnConstraints(110);
        gridpane.getColumnConstraints().addAll(column1, column2, column3, column4, column5);

        RowConstraints row1 = new RowConstraints(50);
        RowConstraints row2 = new RowConstraints(60);
        RowConstraints row3 = new RowConstraints(40);
        RowConstraints row4 = new RowConstraints(50);
        gridpane.getRowConstraints().addAll(row1, row2, row3, row4);
        gridpane.layout();

        assertEquals(new BoundingBox(-197.5, -103, 75, 50), designInfo.getCellBounds(gridpane, 0, 0));
        assertEquals(new BoundingBox(-87.5, -103, 90, 50), designInfo.getCellBounds(gridpane, 2, 0));
        assertEquals(new BoundingBox(-117.5, -51, 25, 60), designInfo.getCellBounds(gridpane, 1, 1));
        assertEquals(new BoundingBox(7.5, -51, 75, 60), designInfo.getCellBounds(gridpane, 3, 1));
        assertEquals(new BoundingBox(7.5, 11, 75, 40), designInfo.getCellBounds(gridpane, 3, 2));
        assertEquals(new BoundingBox(87.5, -51, 110, 60), designInfo.getCellBounds(gridpane, 4, 1));
    }     
}
