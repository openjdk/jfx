/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.gridpane;

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.tring.Quad;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.util.CardinalPoint;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Path;

/**
 *
 */
class GridPaneMosaic {
    
    public final static double NORTH_TRAY_SIZE = 22;
    public final static double SOUTH_TRAY_SIZE = NORTH_TRAY_SIZE;
    public final static double WEST_TRAY_SIZE = 24;
    public final static double EAST_TRAY_SIZE = WEST_TRAY_SIZE;
    
    private final Group topGroup = new Group();
    private final Path gridPath = new Path();
    private final Group northTrayGroup = new Group();
    private final Group southTrayGroup = new Group();
    private final Group westTrayGroup = new Group();
    private final Group eastTrayGroup = new Group();
    
    private final Quad gridAreaQuad = new Quad();
    private final List<Quad> gridHoleQuads = new ArrayList<>();

    private GridPane gridPane;
    private final Set<Integer> selectedColumnIndexes = new HashSet<>();
    private final Set<Integer> selectedRowIndexes = new HashSet<>();
    
    public GridPaneMosaic() {
        final List<Node> topChildren = topGroup.getChildren();
        topChildren.add(gridPath);
        topChildren.add(northTrayGroup);
        topChildren.add(southTrayGroup);
        topChildren.add(westTrayGroup);
        topChildren.add(eastTrayGroup);
        gridAreaQuad.addToPath(gridPath);
        
        gridPath.setMouseTransparent(true);
        gridPath.getStyleClass().add("gap");
    }

    public Group getTopGroup() {
        return topGroup;
    }

    public GridPane getGridPane() {
        return gridPane;
    }

    public void setGridPane(GridPane gridPane) {
        this.gridPane = gridPane;
        update();
    }
    
    public void setSelectedColumnIndexes(Set<Integer> indexes) {
        selectedColumnIndexes.clear();
        selectedColumnIndexes.addAll(indexes);
        update();
    }
    
    public void setSelectedRowIndexes(Set<Integer> indexes) {
        selectedRowIndexes.clear();
        selectedRowIndexes.addAll(indexes);
        update();
    }
    
    public void update() {
        final int columnCount = Deprecation.getGridPaneColumnCount(gridPane);
        final int rowCount = Deprecation.getGridPaneRowCount(gridPane);

        final int actualColumnCount, actualRowCount;
        if ((columnCount == 0) || (rowCount == 0)) {
            actualColumnCount = 0;
            actualRowCount = 0;
        } else {
            actualColumnCount = columnCount;
            actualRowCount = rowCount;
        }
        
        gridAreaQuad.setBounds(gridPane.getLayoutBounds());
        adjustHoleItems(actualColumnCount, actualRowCount);
        adjustTrayItems(northTrayGroup.getChildren(), "north", actualColumnCount);
        adjustTrayItems(southTrayGroup.getChildren(), "south", actualColumnCount);
        adjustTrayItems(westTrayGroup.getChildren(), "west", actualRowCount);
        adjustTrayItems(eastTrayGroup.getChildren(), "east", actualRowCount);
        
        if (actualColumnCount >= 1) {
            assert actualRowCount >= 1;
            
            updateHoleBounds();
            updateNorthTrayBounds();
            updateSouthTrayBounds();
            updateWestTrayBounds();
            updateEastTrayBounds();

            updateSelection(northTrayGroup.getChildren(), selectedColumnIndexes);
            updateSelection(southTrayGroup.getChildren(), selectedColumnIndexes);
            updateSelection(westTrayGroup.getChildren(), selectedRowIndexes);
            updateSelection(eastTrayGroup.getChildren(), selectedRowIndexes);
        }
    }
    
    
    public List<Node> getNorthTrayNodes() {
        return northTrayGroup.getChildren();
    }
    
    public List<Node> getSouthTrayNodes() {
        return southTrayGroup.getChildren();
    }
    
    public List<Node> getWestTrayNodes() {
        return westTrayGroup.getChildren();
    }
    
    public List<Node> getEastTrayNodes() {
        return eastTrayGroup.getChildren();
    }
    
    
    /*
     * Private
     */
    
    
    private void adjustHoleItems(int actualColumnCount, int actualRowCount) {
        final int holeCount = actualColumnCount * actualRowCount;
        
        while (gridHoleQuads.size() < holeCount) {
            final Quad holeQuad = new Quad(false /* clockwise */); // Counterclockwise !!
            holeQuad.addToPath(gridPath);
            gridHoleQuads.add(holeQuad);
        }
        while (holeCount < gridHoleQuads.size()) {
            final int cellIndex = gridHoleQuads.size()-1;
            gridHoleQuads.get(cellIndex).removeFromPath(gridPath);
            gridHoleQuads.remove(cellIndex);
        }
    }
    
    private void adjustTrayItems(List<Node> trayChildren, String direction, int targetCount) {
        
        while (trayChildren.size() < targetCount) {
            final int trayIndex = trayChildren.size();
            trayChildren.add(makeTrayLabel(trayIndex, direction));
        }
        while (targetCount < trayChildren.size()) {
            final int trayIndex = trayChildren.size()-1;
            trayChildren.remove(trayIndex);
        }
    }
    
    private Label makeTrayLabel(int num, String direction) {
        final Label result = new Label();
        result.getStyleClass().add("tray");
        result.getStyleClass().add(direction);
        result.setText(String.valueOf(num));
        result.setMinWidth(Region.USE_PREF_SIZE);
        result.setMaxWidth(Region.USE_PREF_SIZE);
        result.setMinHeight(Region.USE_PREF_SIZE);
        result.setMaxHeight(Region.USE_PREF_SIZE);
        
        return result;
    }
    
    
    private void updateHoleBounds() {
        final int columnCount = Deprecation.getGridPaneColumnCount(gridPane);
        final int rowCount = Deprecation.getGridPaneRowCount(gridPane);
        
        for (int c = 0; c < columnCount; c++) {
            for (int r = 0; r < rowCount; r++) {
                final int holeIndex = c * rowCount + r;
                final Bounds cb = Deprecation.getGridPaneCellBounds(gridPane, c, r);
                gridHoleQuads.get(holeIndex).setBounds(cb);
            }
        }
    }
    
    
    private void updateNorthTrayBounds() {
        final List<Node> northTrayChildren = northTrayGroup.getChildren();
        final int columnCount = Deprecation.getGridPaneColumnCount(gridPane);
        assert northTrayChildren.size() == columnCount;
        
        for (int c = 0; c < columnCount; c++) {
            updateNorthTrayBounds(c, (Label)northTrayChildren.get(c));
        }
    }
    
    
    private void updateNorthTrayBounds(int column, Label label) {
        final Bounds gb = gridPane.getLayoutBounds();
        final Bounds cb = Deprecation.getGridPaneCellBounds(gridPane, column, 0);


        /*
         *            x0                x1
         *            +-----------------+
         *            |     north(c)    |
         * y0  ....---+-----------------+---...
         *            |    padding.top  |
         *     ....---+-----------------+---...
         *            |                 |
         *            |    cell(c, 0)   |
         *            |                 |
         * y1  ....---+-----------------+---...
         */

        final double x0 = cb.getMinX();
        final double x1 = cb.getMaxX();
        final double y0 = gb.getMinY();
        final double y1 = cb.getMaxY();
        assert x0 <= x1;
        assert y0 <= y1;
        
        label.setPrefWidth(x1 - x0);
        label.setPrefHeight(NORTH_TRAY_SIZE);
        
        final Bounds area = new BoundingBox(x0, y0, x1-x0, y1-y0);
        relocateNode(label, area, CardinalPoint.N);
    }
    
    
    private void updateSouthTrayBounds() {
        final List<Node> trayChildren = southTrayGroup.getChildren();
        final int columnCount = Deprecation.getGridPaneColumnCount(gridPane);
        assert trayChildren.size() == columnCount;
        
        for (int c = 0; c < columnCount; c++) {
            updateSouthTrayBounds(c, (Label)trayChildren.get(c));
        }
    }
    
    
    private void updateSouthTrayBounds(int column, Label label) {
        final Bounds gb = gridPane.getLayoutBounds();
        final Bounds cb = Deprecation.getGridPaneCellBounds(gridPane, column, 0);


        /*
         *            x0                x1
         * y0  ....---+-----------------+---...
         *            |                 |
         *            |   cell(c, n-1)  |
         *            |                 |
         *     ....---+-----------------+---...
         *            |  padding.bottom |
         * y1  ....---+-----------------+---...
         *            |     south(c)    |
         *            +-----------------+
         */

        final double x0 = cb.getMinX();
        final double x1 = cb.getMaxX();
        final double y0 = cb.getMinY();
        final double y1 = gb.getMaxY();
        assert x0 <= x1;
        assert y0 <= y1;

        label.setPrefWidth(x1 - x0);
        label.setPrefHeight(SOUTH_TRAY_SIZE);
        
        final Bounds area = new BoundingBox(x0, y0, x1-x0, y1-y0);
        relocateNode(label, area, CardinalPoint.S);
    }
    
    
    private void updateWestTrayBounds() {
        final List<Node> trayChildren = westTrayGroup.getChildren();
        final int rowCount = Deprecation.getGridPaneRowCount(gridPane);
        assert trayChildren.size() == rowCount;
        
        for (int r = 0; r < rowCount; r++) {
            updateWestTrayBounds(r, (Label)trayChildren.get(r));
        }
    }
    
    
    private void updateWestTrayBounds(int row, Label label) {
        final Bounds gb = gridPane.getLayoutBounds();
        final Bounds cb = Deprecation.getGridPaneCellBounds(gridPane, 0,row);


        /*
         *         x0                    x1
         *         .   .                 . 
         *         .   .                 . 
         *         .   .                 . 
         *         |   |                 |      
         * y0 +----+---+-----------------+...
         *    |    |   |                 |
         *    |    |   |                 |
         *    |    |   |                 |
         *    |    |   |   cell(0, row)  |
         *    |    |   |                 |
         *    |    |   |                 |
         *    |    |   |                 |
         * y1 +----+---+-----------------+...
         *         |   |                 |      
         *         .   .                 . 
         *         .   .                 . 
         *         .   .                 . 
         *      ^    ^
         *      |    |
         *      |    padding.left
         *      |
         *      west(r)
         */

        final double x0 = gb.getMinX();
        final double x1 = cb.getMaxX();
        final double y0 = cb.getMinY();
        final double y1 = cb.getMaxY();
        assert x0 <= x1;
        assert y0 <= y1;

        label.setPrefWidth(y1 - y0);
        label.setPrefHeight(WEST_TRAY_SIZE);
        
        final Bounds area = new BoundingBox(x0, y0, x1-x0, y1-y0);
        relocateNode(label, area, CardinalPoint.W);
    }
    
    
    
    
    private void updateEastTrayBounds() {
        final List<Node> trayChildren = eastTrayGroup.getChildren();
        final int rowCount = Deprecation.getGridPaneRowCount(gridPane);
        assert trayChildren.size() == rowCount;
        
        for (int r = 0; r < rowCount; r++) {
            updateEastTrayBounds(r, (Label)trayChildren.get(r));
        }
    }
    
    
    private void updateEastTrayBounds(int row, Label label) {
        final Bounds gb = gridPane.getLayoutBounds();
        final Bounds cb = Deprecation.getGridPaneCellBounds(gridPane, 0,row);


        /*
         *             x0                    x1
         *             .                 .   . 
         *             .                 .   . 
         *             .                 .   . 
         *             |                 |   |      
         * y0          +-----------------+---+----+...
         *             |                 |   |    |
         *             |                 |   |    |
         *             |                 |   |    |
         *             |   cell(0, row)  |   |    |
         *             |                 |   |    |
         *             |                 |   |    |
         *             |                 |   |    |
         * y1          +-----------------+---+----+...
         *             |                 |      
         *             .                 . 
         *             .                 . 
         *             .                 . 
         *                                  ^    ^
         *                                  |    |
         *                                  |    west(r)
         *                                  |    
         *                                  padding.right
         */

        final double x0 = cb.getMinX();
        final double x1 = gb.getMaxX();
        final double y0 = cb.getMinY();
        final double y1 = cb.getMaxY();
        assert x0 <= x1;
        assert y0 <= y1;

        label.setPrefWidth(y1 - y0);
        label.setPrefHeight(EAST_TRAY_SIZE);
        
        final Bounds area = new BoundingBox(x0, y0, x1-x0, y1-y0);
        relocateNode(label, area, CardinalPoint.E);
    }
    
    
    private void relocateNode(Label node, Bounds area, CardinalPoint cp) {
        assert node != null;
        
        final double nodeW = node.getPrefWidth();
        final double nodeH = node.getPrefHeight();
        final double areaW = area.getWidth();
        final double areaH = area.getHeight();
                
        /*
         * From
         *
         *      +----------+
         *      |   node   |--------------------+
         *      +----------+                    |
         *           |                          |
         *           |                          |
         *           |           area           |
         *           |                          |
         *           |                          |
         *           |                          |
         *           +--------------------------+
         *
         *
         * to North
         *                   +----------+
         *                   |   node   |
         *           +-------+----------+-------+
         *           |                          |
         *           |                          |
         *           |                          |   rotation   = 0°
         *           |           area           |   translateX = +areaW/2
         *           |                          |   translateY = -nodeH/2
         *           |                          |
         *           |                          |
         *           +--------------------------+
         * 
         * to South
         *           +--------------------------+
         *           |                          |
         *           |                          |
         *           |                          |   rotation   = 0°
         *           |           area           |   translateX = +areaW/2
         *           |                          |   translateY = +areaW+nodeH/2
         *           |                          |
         *           |                          |
         *           +-------+----------+-------+
         *                   |   node   |
         *                   +----------+
         *
         * to West
         *           +--------------------------+
         *           |                          |
         *      +----+                          |
         *      |    |                          |   rotation   = -90°
         *      |node|           area           |   translateX = -nodeH/2
         *      |    |                          |   translateY = +areaH/2
         *      +----+                          |
         *           |                          |
         *           +--------------------------+
         *
         * to East
         *           +--------------------------+
         *           |                          |
         *           |                          |----+
         *           |                          |    |   rotation   = +90°
         *           |           area           |node|   translateX = +areaW+nodeH/2
         *           |                          |    |   translateY = +areaH/2
         *           |                          |----+
         *           |                          |
         *           +--------------------------+
         */
        
        final double rotation, translateX, translateY;
        switch(cp) {
            case N:
                rotation = 0.0;
                translateX = +areaW/2.0;
                translateY = -nodeH/2.0;
                break;
            case S:
                rotation = 0.0;
                translateX = +areaW/2.0;
                translateY = +areaH + nodeH/2.0;
                break;
            case W:
                rotation = -90.0;
                translateX = -nodeH/2.0;
                translateY = +areaH/2.0;
                break;
            case E:
                rotation = +90.0;
                translateX = +areaW + nodeH/2.0;
                translateY = +areaH/2.0;
                break;
            default:
                assert false;
                rotation = translateX = translateY = 0;
                break;
        }
        
        final double nodeCenterX = nodeW / 2.0;
        final double nodeCenterY = nodeH / 2.0;
        final double layoutDX = area.getMinX() - nodeCenterX + translateX;
        final double layoutDY = area.getMinY() - nodeCenterY + translateY;
        
        node.setLayoutX(layoutDX);
        node.setLayoutY(layoutDY);
        node.setRotate(rotation);
    }
    
    
    
    private void updateSelection(List<Node> trayChildren, Set<Integer> selectedIndexes) {
        final String selectedClass = "selected";
        
        for (int i = 0, count = trayChildren.size(); i < count; i++) {
            final List<String> trayStyleClasses = trayChildren.get(i).getStyleClass();
            if (selectedIndexes.contains(i)) {
                if (trayStyleClasses.contains(selectedClass) == false) {
                    trayStyleClasses.add(selectedClass);
                }
            } else {
                if (trayStyleClasses.contains(selectedClass)) {
                    trayStyleClasses.remove(selectedClass);
                }
            }
        }
    }
    
}
