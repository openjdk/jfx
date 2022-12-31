/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.layout;

import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;
import static javafx.scene.layout.Region.USE_PREF_SIZE;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.geometry.VPos;


/**
 * Defines optional layout constraints for a row in a {@link GridPane}.
 * If a RowConstraints object is added for a row in a gridpane, the gridpane
 * will use those constraint values when computing the row's height and layout.
 * <p>
 * For example, to create a GridPane with 10 rows 50 pixels tall:
 * <pre>{@code
 *     GridPane gridpane = new GridPane();
 *     for (int i = 0; i < 10; i++) {
 *         RowConstraints row = new RowConstraints(50);
 *         gridpane.getRowConstraints().add(row);
 *     }
 * }</pre>
 * Or, to create a GridPane where rows take 25%, 50%, 25% of its height:
 * <pre>{@code
 *     GridPane gridpane = new GridPane();
 *     RowConstraints row1 = new RowConstraints();
 *     row1.setPercentHeight(25);
 *     RowConstraints row2 = new RowConstraints();
 *     row2.setPercentHeight(50);
 *     RowConstraints row3 = new RowConstraints();
 *     row3.setPercentHeight(25);
 *     gridpane.getRowConstraints().addAll(row1,row2,row3);
 * }</pre>
 *
 * Note that adding an empty RowConstraints object has the effect of not setting
 * any constraints, leaving the GridPane to compute the row's layout based
 * solely on its content's size preferences and constraints.
 *
 * @since JavaFX 2.0
 */
public class RowConstraints extends ConstraintsBase {

    /**
     * Creates a row constraints object with no properties set.
     */
    public RowConstraints() {
        super();
    }

    /**
     * Creates a row constraint object with a fixed height.
     * This is a convenience for setting the preferred height constraint to the
     * fixed value and the minHeight and maxHeight constraints to the USE_PREF_SIZE
     * flag to ensure the row is always that height.
     *
     * @param height the height of the row
     */
    public RowConstraints(double height) {
        this();
        setMinHeight(USE_PREF_SIZE);
        setPrefHeight(height);
        setMaxHeight(USE_PREF_SIZE);
    }

    /**
     * Creates a row constraint object with a fixed size range.
     * This is a convenience for setting the minimum, preferred, and maximum
     * height constraints.
     *
     * @param minHeight the minimum height
     * @param prefHeight the preferred height
     * @param maxHeight the maximum height
     */
    public RowConstraints(double minHeight, double prefHeight, double maxHeight) {
        this();
        setMinHeight(minHeight);
        setPrefHeight(prefHeight);
        setMaxHeight(maxHeight);
    }

    /**
     * Creates a row constraint object with a fixed size range, vertical
     * grow priority, vertical alignment, and vertical fill behavior.
     *
     * @param minHeight the minimum height
     * @param prefHeight the preferred height
     * @param maxHeight the maximum height
     * @param vgrow the vertical grow priority
     * @param valignment the vertical alignment
     * @param fillHeight the vertical fill behavior
     */
    public RowConstraints(double minHeight, double prefHeight, double maxHeight, Priority vgrow, VPos valignment, boolean fillHeight) {
        this(minHeight, prefHeight, maxHeight);
        setVgrow(vgrow);
        setValignment(valignment);
        setFillHeight(fillHeight);
    }

    /**
     * The minimum height for the row.
     * This property is ignored if percentHeight is set.
     * <p>
     * The default value is USE_COMPUTED_SIZE, which means the minimum height
     * will be computed to be the largest minimum height of the row's content.
     */
    private DoubleProperty minHeight;

    public final void setMinHeight(double value) {
        minHeightProperty().set(value);
    }

    public final double getMinHeight() {
        return minHeight == null ? USE_COMPUTED_SIZE : minHeight.get();
    }

    public final DoubleProperty minHeightProperty() {
        if (minHeight == null) {
            minHeight = new DoublePropertyBase(USE_COMPUTED_SIZE) {

                @Override
                protected void invalidated() {
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return RowConstraints.this;
                }

                @Override
                public String getName() {
                    return "minHeight";
                }
            };
        }
        return minHeight;
    }

    /**
     * The preferred height for the row.
     * This property is ignored if percentHeight is set.
     * <p>
     * The default value is USE_COMPUTED_SIZE, which means the preferred height
     * will be computed to be the largest preferred height of the row's content.
     */
    private DoubleProperty prefHeight;

    public final void setPrefHeight(double value) {
        prefHeightProperty().set(value);
    }

    public final double getPrefHeight() {
        return prefHeight == null ? USE_COMPUTED_SIZE : prefHeight.get();
    }

    public final DoubleProperty prefHeightProperty() {
        if (prefHeight == null) {
            prefHeight = new DoublePropertyBase(USE_COMPUTED_SIZE) {

                @Override
                protected void invalidated() {
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return RowConstraints.this;
                }

                @Override
                public String getName() {
                    return "prefHeight";
                }
            };
        }
        return prefHeight;
    }

    /**
     * The maximum height for the row.
     * This property is ignored if percentHeight is set.
     * <p>
     * The default value is USE_COMPUTED_SIZE, which means the maximum height
     * will be computed to be the smallest maximum height of the row's content.
     */
    private DoubleProperty maxHeight;

    public final void setMaxHeight(double value) {
        maxHeightProperty().set(value);
    }

    public final double getMaxHeight() {
        return maxHeight == null ? USE_COMPUTED_SIZE : maxHeight.get();
    }

    public final DoubleProperty maxHeightProperty() {
        if (maxHeight == null) {
            maxHeight = new DoublePropertyBase(USE_COMPUTED_SIZE) {

                @Override
                protected void invalidated() {
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return RowConstraints.this;
                }

                @Override
                public String getName() {
                    return "maxHeight";
                }
            };
        }
        return maxHeight;
    }

    /**
     * The height percentage of the row.  If set to a value greater than 0, the
     * row will be resized to that percentage of the available gridpane height and
     * the other size constraints (minHeight, prefHeight, maxHeight, vgrow) will
     * be ignored.
     *
     * The default value is -1, which means the percentage will be ignored.
     */
    private DoubleProperty percentHeight;

    public final void setPercentHeight(double value) {
        percentHeightProperty().set(value);
    }

    public final double getPercentHeight() {
        return percentHeight == null ? -1 : percentHeight.get();
    }

    public final DoubleProperty percentHeightProperty() {
        if (percentHeight == null) {
            percentHeight = new DoublePropertyBase(-1) {

                @Override
                protected void invalidated() {
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return RowConstraints.this;
                }

                @Override
                public String getName() {
                    return "percentHeight";
                }
            };
        }
        return percentHeight;
    }

    /**
     * The vertical grow priority for the row.  If set, the gridpane will
     * use this priority to determine whether the row should be given any
     * additional height if the gridpane is resized larger than its preferred height.
     * This property is ignored if percentHeight is set.
     * <p>
     * This default value is null, which means that the row's grow priority
     * will be derived from largest grow priority set on a content node.
     */
    private ObjectProperty<Priority> vgrow;

    public final void setVgrow(Priority value) {
        vgrowProperty().set(value);
    }

    public final Priority getVgrow() {
        return vgrow == null ? null : vgrow.get();
    }

    public final ObjectProperty<Priority> vgrowProperty() {
        if (vgrow == null) {
            vgrow = new ObjectPropertyBase<>() {

                @Override
                protected void invalidated() {
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return RowConstraints.this;
                }

                @Override
                public String getName() {
                    return "vgrow";
                }
            };
        }
        return vgrow;
    }

    /**
     * The vertical alignment for the row. If set, will be the default
     * vertical alignment for nodes contained within the row.
     * If this property is set to VPos.BASELINE, then the fillHeight property
     * will be ignored and nodes will always be resized to their preferred heights.
     */
    private ObjectProperty<VPos> valignment;

    public final void setValignment(VPos value) {
        valignmentProperty().set(value);
    }

    public final VPos getValignment() {
        return valignment == null ? null : valignment.get();
    }

    public final ObjectProperty<VPos> valignmentProperty() {
        if (valignment == null) {
            valignment = new ObjectPropertyBase<>() {

                @Override
                protected void invalidated() {
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return RowConstraints.this;
                }

                @Override
                public String getName() {
                    return "valignment";
                }
            };
        }
        return valignment;
    }

    /**
     * The vertical fill policy for the row.  The gridpane will
     * use this property to determine whether nodes contained within the row
     * should be expanded to fill the row's height or kept to their preferred heights.
     * <p>
     * The default value is true.
     */
    private BooleanProperty fillHeight;

    public final void setFillHeight(boolean value) {
        fillHeightProperty().set(value);
    }

    public final boolean isFillHeight() {
        return fillHeight == null ? true : fillHeight.get();
    }

    public final BooleanProperty fillHeightProperty() {
        if (fillHeight == null) {
            fillHeight = new BooleanPropertyBase(true) {
                @Override
                protected void invalidated() {
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return RowConstraints.this;
                }

                @Override
                public String getName() {
                    return "fillHeight";
                }
            };
        }
        return fillHeight;
    }

    /**
     * Returns a string representation of this {@code RowConstraints} object.
     * @return a string representation of this {@code RowConstraints} object.
     */
    @Override public String toString() {
        return "RowConstraints percentHeight="+getPercentHeight()+
                " minHeight="+getMinHeight()+
                " prefHeight="+getPrefHeight()+
                " maxHeight="+getMaxHeight()+
                " vgrow="+getVgrow()+
                " fillHeight="+isFillHeight()+
                " valignment="+getValignment();
    }
}
