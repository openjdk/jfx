/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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
import com.oracle.javafx.scenebuilder.kit.metadata.util.ColorEncoder;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;

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
    private final Group hgapLinesGroup = new Group();
    private final Group vgapLinesGroup = new Group();
    private final Group northTrayGroup = new Group();
    private final Group southTrayGroup = new Group();
    private final Group westTrayGroup = new Group();
    private final Group eastTrayGroup = new Group();
    private final Rectangle targetCellShadow = new Rectangle();
    private final Line targetGapShadowV = new Line();
    private final Line targetGapShadowH= new Line();
    private final Group hgapSensorsGroup = new Group();
    private final Group vgapSensorsGroup = new Group();
    
    private final Quad gridAreaQuad = new Quad();
    private final List<Quad> gridHoleQuads = new ArrayList<>();

    private final String baseStyleClass;
    private final boolean shouldShowTrays;
    private final boolean shouldCreateSensors;
    
    private GridPane gridPane;
    private int columnCount;
    private int rowCount;
    private List<Bounds> cellBounds = new ArrayList<>();
    private final Set<Integer> selectedColumnIndexes = new HashSet<>();
    private final Set<Integer> selectedRowIndexes = new HashSet<>();
    private int targetColumnIndex = -1;
    private int targetRowIndex = -1;
    private int targetGapColumnIndex = -1;
    private int targetGapRowIndex = -1;
    private Color trayColor;
    
    public GridPaneMosaic(String baseStyleClass, boolean shouldShowTrays, boolean shouldCreateSensors) {
        assert baseStyleClass != null;
        
        this.baseStyleClass = baseStyleClass;
        this.shouldShowTrays = shouldShowTrays;
        this.shouldCreateSensors = shouldCreateSensors;
        
        final List<Node> topChildren = topGroup.getChildren();
        topChildren.add(gridPath);              // Mouse transparent
        topChildren.add(hgapLinesGroup);        // Mouse transparent
        topChildren.add(vgapLinesGroup);        // Mouse transparent
        topChildren.add(northTrayGroup);
        topChildren.add(southTrayGroup);
        topChildren.add(westTrayGroup);
        topChildren.add(eastTrayGroup);
        topChildren.add(targetCellShadow);      // Mouse transparent
        topChildren.add(targetGapShadowV);      // Mouse transparent
        topChildren.add(targetGapShadowH);      // Mouse transparent
        topChildren.add(hgapSensorsGroup);
        topChildren.add(vgapSensorsGroup);
        gridAreaQuad.addToPath(gridPath);
        
        gridPath.setMouseTransparent(true);
        gridPath.getStyleClass().add("gap");
        gridPath.getStyleClass().add(baseStyleClass);
        
        hgapLinesGroup.setMouseTransparent(true);
        vgapLinesGroup.setMouseTransparent(true);
        
        targetCellShadow.setMouseTransparent(true);
        targetCellShadow.getStyleClass().add("gap");
        targetCellShadow.getStyleClass().add("selected");
        targetCellShadow.getStyleClass().add(baseStyleClass);
        
        targetGapShadowV.setMouseTransparent(true);
        targetGapShadowV.getStyleClass().add("gap");
        targetGapShadowV.getStyleClass().add("hilit");
        targetGapShadowV.getStyleClass().add(baseStyleClass);
        
        targetGapShadowH.setMouseTransparent(true);
        targetGapShadowH.getStyleClass().add("gap");
        targetGapShadowH.getStyleClass().add("hilit");
        targetGapShadowH.getStyleClass().add(baseStyleClass);
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
    
    public void setTrayColor(Color trayColor) {
        this.trayColor = trayColor;
        if (shouldShowTrays) {
            updateTrayColor();
        }
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

    public void setTargetCell(int targetColumnIndex, int targetRowIndex) {
        assert (targetColumnIndex == -1) == (targetRowIndex == -1);
        this.targetColumnIndex = targetColumnIndex;
        this.targetRowIndex = targetRowIndex;
        this.targetGapColumnIndex = -1;
        this.targetGapRowIndex = -1;
        update();
    }
    
    public void setTargetGap(int targetGapColumnIndex, int targetGapRowIndex) {
        assert (-1 <= targetGapColumnIndex) && (targetGapColumnIndex <= columnCount);
        assert (-1 <= targetGapRowIndex) && (targetGapRowIndex <= rowCount);
        
        this.targetGapColumnIndex = targetGapColumnIndex;
        this.targetGapRowIndex = targetGapRowIndex;
        this.targetColumnIndex = -1;
        this.targetRowIndex = -1;
        update();
    }
    
    public void update() {
        
        columnCount = Deprecation.getGridPaneColumnCount(gridPane);
        rowCount = Deprecation.getGridPaneRowCount(gridPane);
        if ((columnCount == 0) || (rowCount == 0)) {
            columnCount = rowCount = 0;
        }
        this.cellBounds.clear();
        for (int c = 0; c < columnCount; c++) {
            for (int r = 0; r < rowCount; r++) {
                this.cellBounds.add(Deprecation.getGridPaneCellBounds(gridPane, c, r));
            }
        }
        
        gridAreaQuad.setBounds(gridPane.getLayoutBounds());
        adjustHoleItems();
        adjustHGapLines();
        adjustVGapLines();
        if (shouldShowTrays) {
            adjustTrayItems(northTrayGroup.getChildren(), "north", columnCount);
            adjustTrayItems(southTrayGroup.getChildren(), "south", columnCount);
            adjustTrayItems(westTrayGroup.getChildren(), "west", rowCount);
            adjustTrayItems(eastTrayGroup.getChildren(), "east", rowCount);
        }
        if (shouldCreateSensors) {
            final int hgapSensorCount = Math.max(0, columnCount-1);
            final int vgapSensorCount = Math.max(0, rowCount-1);
            adjustGapSensors(hgapSensorsGroup.getChildren(), Cursor.H_RESIZE, hgapSensorCount);
            adjustGapSensors(vgapSensorsGroup.getChildren(), Cursor.V_RESIZE, vgapSensorCount);
        }
        
        if (columnCount >= 1) {
            assert rowCount >= 1;
            
            updateHoleBounds();
            updateHGapLines();
            updateVGapLines();
            
            if (shouldShowTrays) {
                updateNorthTrayBounds();
                updateSouthTrayBounds();
                updateWestTrayBounds();
                updateEastTrayBounds();

                updateSelection(northTrayGroup.getChildren(), selectedColumnIndexes);
                updateSelection(southTrayGroup.getChildren(), selectedColumnIndexes);
                updateSelection(westTrayGroup.getChildren(), selectedRowIndexes);
                updateSelection(eastTrayGroup.getChildren(), selectedRowIndexes);
                
                updateTrayColor();
            }

            if (shouldCreateSensors) {
                updateHGapSensors();
                updateVGapSensors();
            }
        
            
            updateTargetCell();
            updateTargetGap();
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

    public List<Node> getHgapSensorNodes() {
        return hgapSensorsGroup.getChildren();
    }

    public List<Node> getVgapSensorNodes() {
        return vgapSensorsGroup.getChildren();
    }
    
    
    /*
     * Private
     */
    
    
    private void adjustHoleItems() {
        final int holeCount = columnCount * rowCount;
        
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
    
    
    private void adjustHGapLines() {
        final int hgapLineCount;
        if (gridPane.getHgap() == 0) {
            hgapLineCount = Math.max(0, columnCount-1);
        } else {
            hgapLineCount = 0;
        }
        final List<Node> children = hgapLinesGroup.getChildren();
        while (children.size() < hgapLineCount) {
            children.add(makeGapLine());
        }
        while (children.size() > hgapLineCount) {
            children.remove(0);
        }
    }
    
    private void adjustVGapLines() {
        final int vgapLineCount;
        if (gridPane.getVgap() == 0) {
            vgapLineCount = Math.max(0, rowCount-1);
        } else {
            vgapLineCount = 0;
        }
        final List<Node> children = vgapLinesGroup.getChildren();
        while (children.size() < vgapLineCount) {
            children.add(makeGapLine());
        }
        while (children.size() > vgapLineCount) {
            children.remove(0);
        }
    }
    
    private Line makeGapLine() {
        final Line result = new Line();
        result.getStyleClass().add("gap");
        result.getStyleClass().add("empty");
        result.getStyleClass().add(baseStyleClass);
        return result;
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
        result.getStyleClass().add(baseStyleClass);
        result.setText(String.valueOf(num));
        result.setMinWidth(Region.USE_PREF_SIZE);
        result.setMaxWidth(Region.USE_PREF_SIZE);
        result.setMinHeight(Region.USE_PREF_SIZE);
        result.setMaxHeight(Region.USE_PREF_SIZE);

        if (trayColor != null) {
            final String webColor = ColorEncoder.encodeColorToRGBA(trayColor);
            final String style = "-fx-background-color:"+ webColor +";";//NOI18N
            result.setStyle(style);
        }
        
        return result;
    }
    
    private void updateTrayColor() {
        final String style;
        
        if (trayColor == null) {
            style = "";//NOI18N
        } else {
            final String webColor = ColorEncoder.encodeColorToRGBA(trayColor);
            style = "-fx-background-color:"+ webColor +";";//NOI18N
        }

        adjustTrayStyle(northTrayGroup.getChildren(), style);
        adjustTrayStyle(southTrayGroup.getChildren(), style);
        adjustTrayStyle(westTrayGroup.getChildren(), style);
        adjustTrayStyle(eastTrayGroup.getChildren(), style);
    }
    
    
    private void adjustTrayStyle(List<Node> trayChildren, String style) {
        
        for (Node tray : trayChildren) {
            assert tray instanceof Label;
            final Label trayLabel = (Label) tray;
            trayLabel.setStyle(style);
        }
    }
    
    
    private void adjustGapSensors(List<Node> gapSensors, Cursor cursor, int targetCount) {
        while (gapSensors.size() < targetCount) {
            gapSensors.add(makeGapSensor(cursor));
        }
        while (targetCount < gapSensors.size()) {
            final int gapIndex = gapSensors.size()-1;
            gapSensors.remove(gapIndex);
        }
    }
    
    private Line makeGapSensor(Cursor cursor) {
        final Line result = new Line();
        result.setCursor(cursor);
        result.setStroke(Color.TRANSPARENT);

        return result;
    }
    
    
    
    private void updateHGapSensors() {
        final List<Node> children = hgapSensorsGroup.getChildren();
        final int sensorCount = children.size();
        assert (sensorCount == 0) || (sensorCount == columnCount-1);
        for (int i = 0; i < sensorCount; i++) {
            /*
             *                       x0  xm   x1
             *   y0  +----------------+       +-----------------+
             *       |   topLeftCell  |       |   topRightCell  |
             *       +----------------+       +-----------------+
             * 
             *       ...
             * 
             *       +----------------+       +-----------------+
             *       | bottomLeftCell |       |                 |
             *   y1  +----------------+       +-----------------+
             */
            
            final Bounds topLeftCellBounds = getCellBounds(i, 0);
            final Bounds topRightCellBounds = getCellBounds(i+1, 0);
            final Bounds bottomLeftCellBounds = getCellBounds(i, rowCount-1);
            final double x0 = topLeftCellBounds.getMaxX();
            final double x1 = topRightCellBounds.getMinX();
            final double xm = (x0 + x1) / 2.0;
            final double y0 = topLeftCellBounds.getMinY();
            final double y1 = bottomLeftCellBounds.getMaxY();
            final double strokeWidth = Math.max(8.0, x1 - x0);
            final Line line = (Line) children.get(i);
            line.setStartX(xm);
            line.setStartY(y0);
            line.setEndX(xm);
            line.setEndY(y1);
            line.setStrokeWidth(strokeWidth);
        }
    }
    
    private void updateVGapSensors() {
        final List<Node> children = vgapSensorsGroup.getChildren();
        final int sensorCount = children.size();
        assert (sensorCount == 0) || (sensorCount == rowCount-1);
        for (int i = 0; i < sensorCount; i++) {
            
            /*
             *       x0                                        x1
             *       +----------------+       +-----------------+
             *       |   topLeftCell  |  ...  |   topRightCell  |
             *   y0  +----------------+       +-----------------+
             *   ym 
             *   y1  +----------------+       +-----------------+
             *       | bottomLeftCell |  ...  |                 |
             *       +----------------+       +-----------------+
             */
            
            final Bounds topLeftCellBounds = getCellBounds(0, i);
            final Bounds bottomLeftCellBounds = getCellBounds(0, i+1);
            final Bounds topRightCellBounds = getCellBounds(columnCount-1, i);
            final double x0 = topLeftCellBounds.getMinX();
            final double x1 = topRightCellBounds.getMaxX();
            final double y0 = topLeftCellBounds.getMaxY();
            final double y1 = bottomLeftCellBounds.getMinY();
            final double ym = (y0 + y1) / 2.0;
            final double strokeWidth = Math.max(8.0, y1 - y0);
            final Line line = (Line) children.get(i);
            line.setStartX(x0);
            line.setStartY(ym);
            line.setEndX(x1);
            line.setEndY(ym);
            line.setStrokeWidth(strokeWidth);
        }
    }
    
    private void updateHoleBounds() {
        for (int c = 0; c < columnCount; c++) {
            for (int r = 0; r < rowCount; r++) {
                final Bounds cb = getCellBounds(c, r);
                gridHoleQuads.get(getCellIndex(c, r)).setBounds(cb);
            }
        }
    }
    
    private void updateHGapLines() {
        final List<Node> children = hgapLinesGroup.getChildren();
        final int lineCount = children.size();
        assert (lineCount == 0) || (lineCount == columnCount-1);
        for (int i = 0; i < lineCount; i++) {
            final Bounds topLeftCellBounds = getCellBounds(i, 0);
            final Bounds topRightCellBounds = getCellBounds(i+1, 0);
            final Bounds bottomLeftCellBounds = getCellBounds(i, rowCount-1);
            final double startX = (topLeftCellBounds.getMaxX() + topRightCellBounds.getMinX()) / 2.0;
            final double startY = topLeftCellBounds.getMinY();
            final double endY = bottomLeftCellBounds.getMaxY();
            final double snappedX = Math.round(startX) + 0.5;
            final Line line = (Line) children.get(i);
            line.setStartX(snappedX);
            line.setStartY(startY);
            line.setEndX(snappedX);
            line.setEndY(endY);
        }
    }
    
    private void updateVGapLines() {
        final List<Node> children = vgapLinesGroup.getChildren();
        final int lineCount = children.size();
        assert (lineCount == 0) || (lineCount == rowCount-1);
        for (int i = 0; i < lineCount; i++) {
            final Bounds topLeftCellBounds = getCellBounds(0, i);
            final Bounds bottomLeftCellBounds = getCellBounds(0, i+1);
            final Bounds topRightCellBounds = getCellBounds(columnCount-1, i);
            final double startX = topLeftCellBounds.getMinX();
            final double startY = (topLeftCellBounds.getMaxY() + bottomLeftCellBounds.getMinY()) / 2.0;
            final double endX = topRightCellBounds.getMaxX();
            final double snappedY = Math.round(startY) + 0.5;
            final Line line = (Line) children.get(i);
            line.setStartX(startX);
            line.setStartY(snappedY);
            line.setEndX(endX);
            line.setEndY(snappedY);
        }
    }
    
    
    private void updateNorthTrayBounds() {
        final List<Node> northTrayChildren = northTrayGroup.getChildren();
        assert northTrayChildren.size() == columnCount;
        
        for (int c = 0; c < columnCount; c++) {
            updateNorthTrayBounds(c, (Label)northTrayChildren.get(c));
        }
    }
    
    
    private void updateNorthTrayBounds(int column, Label label) {
        final Bounds gb = gridPane.getLayoutBounds();
        final Bounds cb = getCellBounds(column, 0);


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
        assert trayChildren.size() == columnCount;
        
        for (int c = 0; c < columnCount; c++) {
            updateSouthTrayBounds(c, (Label)trayChildren.get(c));
        }
    }
    
    
    private void updateSouthTrayBounds(int column, Label label) {
        final Bounds gb = gridPane.getLayoutBounds();
        final Bounds cb = getCellBounds(column, 0);


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
        assert trayChildren.size() == rowCount;
        
        for (int r = 0; r < rowCount; r++) {
            updateWestTrayBounds(r, (Label)trayChildren.get(r));
        }
    }
    
    
    private void updateWestTrayBounds(int row, Label label) {
        final Bounds gb = gridPane.getLayoutBounds();
        final Bounds cb = getCellBounds(0,row);


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
        assert trayChildren.size() == rowCount;
        
        for (int r = 0; r < rowCount; r++) {
            updateEastTrayBounds(r, (Label)trayChildren.get(r));
        }
    }
    
    
    private void updateEastTrayBounds(int row, Label label) {
        final Bounds gb = gridPane.getLayoutBounds();
        final Bounds cb = getCellBounds(0,row);


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
    
    
    private void updateTargetCell() {
        if (targetColumnIndex == -1) {
            assert targetRowIndex == -1;
            targetCellShadow.setVisible(false);
        } else {
            targetCellShadow.setVisible(true);
            final Bounds tb = getCellBounds(targetColumnIndex, targetRowIndex);
            targetCellShadow.setX(tb.getMinX());
            targetCellShadow.setY(tb.getMinY());
            targetCellShadow.setWidth(tb.getWidth());
            targetCellShadow.setHeight(tb.getHeight());
        }
    }
    
    
    private Bounds getCellBounds(int c, int r) {
        final int cellIndex = getCellIndex(c, r);
        assert cellIndex < cellBounds.size();
        return cellBounds.get(cellIndex);
    }
    
    private int getCellIndex(int c, int r) {
        return c * rowCount + r;
    }
    
    
    private static final double MIN_STROKE_WIDTH = 8;
    
    private void updateTargetGap() {
        
        /*
         * targetGapShadowV
         */
        if (targetGapColumnIndex == -1) {
            targetGapShadowV.setVisible(false);
        } else {
            targetGapShadowV.setVisible(true);
            
            final double startX, startY, endY, strokeWidth;
            if (targetGapColumnIndex < columnCount) {
                final Bounds topCellBounds = getCellBounds(targetGapColumnIndex, 0);
                final Bounds bottomCellBounds = getCellBounds(targetGapColumnIndex, rowCount-1);
                startY = topCellBounds.getMinY();
                endY = bottomCellBounds.getMaxY();
                if (targetGapColumnIndex == 0) {
                    startX = topCellBounds.getMinX();
                    strokeWidth = MIN_STROKE_WIDTH;
                } else {
                    assert targetGapColumnIndex >= 1;
                    final Bounds leftTopCellBounds = getCellBounds(targetGapColumnIndex-1, 0);
                    startX = (leftTopCellBounds.getMaxX() + topCellBounds.getMinX()) / 2.0;
                    strokeWidth = Math.abs(leftTopCellBounds.getMaxX() - topCellBounds.getMinX());
                }
            } else {
                final Bounds topCellBounds = getCellBounds(columnCount-1, 0);
                final Bounds bottomCellBounds = getCellBounds(columnCount-1, rowCount-1);
                startX = topCellBounds.getMaxX();
                startY = topCellBounds.getMinY();
                endY = bottomCellBounds.getMaxY();
                strokeWidth = MIN_STROKE_WIDTH;
            }
            targetGapShadowV.setStartX(startX);
            targetGapShadowV.setStartY(startY);
            targetGapShadowV.setEndX(startX);
            targetGapShadowV.setEndY(endY);
            targetGapShadowV.setStrokeWidth(Math.max(strokeWidth, MIN_STROKE_WIDTH));
        }
        
        /*
         * targetGapShadowH
         */
        if (targetGapRowIndex == -1) {
            targetGapShadowH.setVisible(false);
        } else {
            targetGapShadowH.setVisible(true);
            
            final double startX, endX, startY, strokeWidth;
            if (targetGapRowIndex < rowCount) {
                final Bounds leftCellBounds = getCellBounds(0, targetGapRowIndex);
                final Bounds rightCellBounds = getCellBounds(columnCount-1, targetGapRowIndex);
                startX = leftCellBounds.getMinX();
                endX = rightCellBounds.getMaxX();
                if (targetGapRowIndex == 0) {
                    startY = leftCellBounds.getMinY();
                    strokeWidth = MIN_STROKE_WIDTH;
                } else {
                    assert targetGapRowIndex >= 1;
                    final Bounds aboveLeftCellBounds = getCellBounds(0, targetGapRowIndex-1);
                    startY = (aboveLeftCellBounds.getMaxY() + leftCellBounds.getMinY()) / 2.0;
                    strokeWidth = Math.abs(aboveLeftCellBounds.getMaxY() - leftCellBounds.getMinY());
                }
            } else {
                final Bounds leftCellBounds = getCellBounds(0, rowCount-1);
                final Bounds rightCellBounds = getCellBounds(columnCount-1, rowCount-1);
                startX = leftCellBounds.getMinX();
                endX = rightCellBounds.getMaxX();
                startY = leftCellBounds.getMaxY();
                strokeWidth = MIN_STROKE_WIDTH;
            }
            targetGapShadowH.setStartX(startX);
            targetGapShadowH.setStartY(startY);
            targetGapShadowH.setEndX(endX);
            targetGapShadowH.setEndY(startY);
            targetGapShadowH.setStrokeWidth(Math.max(strokeWidth, MIN_STROKE_WIDTH));
        }
        
    }
}
