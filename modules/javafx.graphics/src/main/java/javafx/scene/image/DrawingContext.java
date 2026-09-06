/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.image;

import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;

/**
 * Interface providing basic drawing operations. An instance of this interface
 * is provided by {@link WritableImage} and {@link javafx.scene.canvas.Canvas} via {@link WritableImage#getDrawingContext()}
 * and {@link javafx.scene.canvas.Canvas#getGraphicsContext2D()}.
 * <p>
 * The provider of this interface may be associated with a {@link Node} which may
 * be attached to a {@link Scene}. The operations provided here must be called from the
 * JavaFX Application Thread. A provider that is not associated with any scene may permit
 * the operations to be called from any single other thread.
 * <p>
 * The {@code DrawingContext} maintains the following rendering attributes
 * which affect various subsets of the rendering methods:
 * <table class="overviewSummary" style="width:80%; margin-left:auto; margin-right:auto">
 *     <caption>List of Rendering Attributes</caption>
 *     <tr>
 *         <th class="colLast" style="width:15%" scope="col">Attribute</th>
 *         <th class="colLast" style="width:10%; text-align:center" scope="col">Save/Restore?</th>
 *         <th class="colLast" style="width:10%; text-align:center" scope="col">Default value</th>
 *         <th class="colLast" scope="col">Description</th>
 *     </tr>
 *     <tr><th colspan="3" scope="row"><a id="comm-attr">Common Rendering Attributes</a></th></tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #clipRect(double, double, double, double) Clip}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">No clipping</td>
 *         <td class="colLast">
 *             An intersection of clipping rectangles to which rendering is restricted.
 *         </td>
 *     </tr>
 *     <tr class="altColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setGlobalAlpha(double) Global Alpha}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@code 1.0}</td>
 *         <td class="colLast">
 *             An opacity value that controls the visibility or fading of each rendering operation.
 *         </td>
 *     </tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setGlobalBlendMode(javafx.scene.effect.BlendMode) Global Blend Mode}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@link BlendMode#SRC_OVER SRC_OVER}</td>
 *         <td class="colLast">
 *             A {@link BlendMode} enum value that controls how pixels from each rendering
 *             operation are composited into the existing image.
 *         </td>
 *     </tr>
 *     <tr class="altColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setTransform(javafx.scene.transform.Affine) Transform}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@code Identity}</td>
 *         <td class="colLast">
 *             A 3x2 2D affine transformation matrix that controls how coordinates are
 *             mapped onto the logical pixels of the drawing surface.
 *         </td>
 *     </tr>
 *     <tr><th colspan="3" scope="row"><a id="fill-attr">Fill Attributes</a></th></tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setFill(javafx.scene.paint.Paint) Fill Paint}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@link Color#BLACK BLACK}</td>
 *         <td class="colLast">
 *             The {@link Paint} to be applied to the interior of shapes in a
 *             fill operation.
 *         </td>
 *     </tr>
 *     <tr><th colspan="3" scope="row"><a id="strk-attr">Stroke Attributes</a></th></tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setStroke(javafx.scene.paint.Paint) Stroke Paint}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@link Color#BLACK BLACK}</td>
 *         <td class="colLast">
 *             The {@link Paint} to be applied to the boundary of shapes in a
 *             stroke operation.
 *         </td>
 *     </tr>
 *     <tr class="altColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setLineWidth(double) Line Width}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@code 1.0}</td>
 *         <td class="colLast">
 *             The width of the stroke applied to the boundary of shapes in a
 *             stroke operation.
 *         </td>
 *     </tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setLineCap(javafx.scene.shape.StrokeLineCap) Line Cap}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@link StrokeLineCap#SQUARE SQUARE}</td>
 *         <td class="colLast">
 *             The style of the end caps applied to the beginnings and ends of each
 *             dash and/or subpath in a stroke operation.
 *         </td>
 *     </tr>
 *     <tr class="altColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setLineJoin(javafx.scene.shape.StrokeLineJoin) Line Join}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@link StrokeLineJoin#MITER MITER}</td>
 *         <td class="colLast">
 *             The style of the joins applied between individual segments in the boundary
 *             paths of shapes in a stroke operation.
 *         </td>
 *     </tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setMiterLimit(double) Miter Limit}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@code 10.0}</td>
 *         <td class="colLast">
 *             The ratio limit of how far a {@link StrokeLineJoin#MITER MITER} line join
 *             may extend in the direction of a sharp corner between segments in the
 *             boundary path of a shape, relative to the line width, before it is truncated
 *             to a {@link StrokeLineJoin#BEVEL BEVEL} join in a stroke operation.
 *         </td>
 *     </tr>
 *     <tr class="altColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setLineDashes(double...) Line Dashes}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@code null}</td>
 *         <td class="colLast">
 *             The array of dash lengths to be applied to the segments in the boundary
 *             of shapes in a stroke operation.
 *         </td>
 *     </tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setLineDashOffset(double) Dash Offset}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@code 0.0}</td>
 *         <td class="colLast">
 *             The distance offset into the array of dash lengths at which to start the
 *             dashing of the segments in the boundary of shapes in a stroke operation.
 *         </td>
 *     </tr>
 *     <tr><th colspan="3" scope="row"><a id="text-attr">Text Attributes</a></th></tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setFont(javafx.scene.text.Font) Font}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@link Font#getDefault() Default Font}</td>
 *         <td class="colLast">
 *             The font used for all fill and stroke text operations.
 *         </td>
 *     </tr>
 *     <tr class="altColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setTextAlign(javafx.scene.text.TextAlignment) Text Align}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@link TextAlignment#LEFT LEFT}</td>
 *         <td class="colLast">
 *             The horizontal alignment of text with respect to the {@code X} coordinate
 *             specified in the text operation.
 *         </td>
 *     </tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setTextBaseline(javafx.geometry.VPos) Text Baseline}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@link VPos#BASELINE BASELINE}</td>
 *         <td class="colLast">
 *             The vertical position of the text relative to the {@code Y} coordinate
 *             specified in the text operation.
 *         </td>
 *     </tr>
 *     <tr class="altColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setFontSmoothingType(javafx.scene.text.FontSmoothingType) Font Smoothing}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@link FontSmoothingType#GRAY GRAY}</td>
 *         <td class="colLast">
 *             The type of smoothing (antialiasing) applied to the glyphs in the font
 *             for all fill text operations.
 *         </td>
 *     </tr>
 *     <tr><th colspan="3" scope="row"><a id="path-attr">Path Attributes</a></th></tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #beginPath() Current Path}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:10%; text-align:center">Empty path</td>
 *         <td class="colLast">
 *             The path constructed using various path construction methods to be used
 *             in various path filling, stroking, or clipping operations.
 *         </td>
 *     </tr>
 *     <tr class="altColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setFillRule(javafx.scene.shape.FillRule) Fill Rule}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@link FillRule#NON_ZERO NON_ZERO}</td>
 *         <td class="colLast">
 *             The method used to determine the interior of paths for a path fill or
 *            clip operation.
 *         </td>
 *     </tr>
 *     <tr><th colspan="3" scope="row"><a id="image-attr">Image Attributes</a></th></tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:15%">{@link #setImageSmoothing(boolean) Image Smoothing}</th>
 *         <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:10%; text-align:center">{@code true}</td>
 *         <td class="colLast">
 *             A boolean state which enables or disables image smoothing for
 *             {@link #drawImage(javafx.scene.image.Image, double, double) drawImage(all forms)}.
 *         </td>
 *     </tr>
 * </table>
 * <p>
 * <a id="attr-ops-table">
 * The various rendering methods on the {@code DrawingContext} use the
 * following sets of rendering attributes:
 * </a>
 * <table class="overviewSummary" style="width:80%; margin-left:auto; margin-right:auto">
 *     <caption>Rendering Attributes Table</caption>
 *     <tr>
 *         <th scope="col" class="colLast" style="width:22%">Method</th>
 *         <th scope="col" class="colLast" style="width:13%; text-align:center"><a href="#comm-attr">Common Rendering Attributes</a></th>
 *         <th scope="col" class="colLast" style="width:13%; text-align:center"><a href="#fill-attr">Fill Attributes</a></th>
 *         <th scope="col" class="colLast" style="width:13%; text-align:center"><a href="#strk-attr">Stroke Attributes</a></th>
 *         <th scope="col" class="colLast" style="width:13%; text-align:center"><a href="#text-attr">Text Attributes</a></th>
 *         <th scope="col" class="colLast" style="width:13%; text-align:center"><a href="#path-attr">Path Attributes</a></th>
 *         <th scope="col" class="colLast" style="width:13%; text-align:center"><a href="#image-attr">Image Attributes</a></th>
 *     </tr>
 *     <tr><th colspan="1" scope="row">Basic Shape Rendering</th></tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:22%">
 *             {@link #fillRect(double, double, double, double) fillRect()},
 *             {@link #fillRoundRect(double, double, double, double, double, double) fillRoundRect()},
 *             {@link #fillOval(double, double, double, double) fillOval()},
 *             {@link #fillArc(double, double, double, double, double, double, javafx.scene.shape.ArcType) fillArc()}
 *         </th>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *     </tr>
 *     <tr class="altColor">
 *         <th scope="row" class="colLast" style="width:22%">
 *             {@link #strokeLine(double, double, double, double) strokeLine()},
 *             {@link #strokeRect(double, double, double, double) strokeRect()},
 *             {@link #strokeRoundRect(double, double, double, double, double, double) strokeRoundRect()},
 *             {@link #strokeOval(double, double, double, double) strokeOval()},
 *             {@link #strokeArc(double, double, double, double, double, double, javafx.scene.shape.ArcType) strokeArc()}
 *         </th>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *     </tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:22%">
 *             {@link #clearRect(double, double, double, double) clearRect()}
 *         </th>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#base-fn-1">[1]</a></td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *     </tr>
 *     <tr class="altColor">
 *         <th scope="row" class="colLast" style="width:22%">
 *             {@link #fillPolygon(double[], double[], int) fillPolygon()}
 *         </th>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#base-fn-2">[2]</a></td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *     </tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:22%">
 *             {@link #strokePolygon(double[], double[], int) strokePolygon()},
 *             {@link #strokePolyline(double[], double[], int) strokePolyline()}
 *         </th>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *     </tr>
 *     <tr>
 *         <th scope="row" colspan="7">
 *             <a id="base-fn-1">[1]</a> Only the Transform and Clip apply to clearRect()<br>
 *             <a id="base-fn-2">[2]</a> Only the Fill Rule applies to fillPolygon()
 *         </th>
 *     </tr>
 *     <tr><th scope="row" colspan="1">Path Construction</th></tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:22%">
 *             {@link #beginPath() beginPath()},
 *             {@link #moveTo(double, double) moveTo()},
 *             {@link #lineTo(double, double) lineTo()},
 *             {@link #quadraticCurveTo(double, double, double, double) quadraticCurveTo()},
 *             {@link #bezierCurveTo(double, double, double, double, double, double) bezierCurveTo()},
 *             {@link #arcTo(double, double, double, double, double) arcTo()},
 *             {@link #arc(double, double, double, double, double, double) arc()},
 *             {@link #rect(double, double, double, double) rect()},
 *             {@link #appendSVGPath(java.lang.String) appendSVGPath()},
 *             {@link #closePath() closePath()}
 *         </th>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#path-fn-4">[4]</a></td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *     </tr>
 *     <tr><th scope="row" colspan="1">Path Rendering</th></tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:22%">
 *             {@link #fill() fill()}
 *         </th>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#path-fn-5">[5]</a></td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *     </tr>
 *     <tr class="altColor">
 *         <th scope="row" class="colLast" style="width:22%">
 *             {@link #stroke() stroke()}
 *         </th>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *     </tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:22%">
 *             {@link #clip() clip()}
 *         </th>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#path-fn-5">[5]</a></td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *     </tr>
 *     <tr class="altColor">
 *         <th scope="row" class="colLast" style="width:22%">
 *             {@link #isPointInPath(double, double) isPointInPath()}
 *         </th>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#path-fn-5">[5]</a></td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *     </tr>
 *     <tr>
 *         <th scope="row" colspan="7">
 *             <a id="path-fn-4">[4]</a> Transform applied only during path construction<br>
 *             <a id="path-fn-5">[5]</a> Fill Rule only used for fill() and clip()
 *         </th>
 *     </tr>
 *     <tr><th scope="row" colspan="1">Text Rendering</th></tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:22%">
 *             {@link #fillText(java.lang.String, double, double) fillText()},
 *             {@link #fillText(java.lang.String, double, double, double) fillText(with maxWidth)}
 *         </th>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#text-fn-3">[3]</a></td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *     </tr>
 *     <tr class="altColor">
 *         <th scope="row" class="colLast" style="width:22%">
 *             {@link #strokeText(java.lang.String, double, double) strokeText()},
 *             {@link #strokeText(java.lang.String, double, double, double) strokeText(with maxWidth)}
 *         </th>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#text-fn-3">[3]</a></td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *     </tr>
 *     <tr>
 *         <th scope="row" colspan="7">
 *             <a id="text-fn-3">[3]</a> The Font Smoothing attribute only applies to filled text
 *         </th>
 *     </tr>
 *     <tr><th scope="row" colspan="1">Image Rendering</th></tr>
 *     <tr class="rowColor">
 *         <th scope="row" class="colLast" style="width:22%">
 *             {@link #drawImage(javafx.scene.image.Image, double, double) drawImage(all forms)}
 *         </th>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 *         <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 *     </tr>
 * </table>
 * <p>
 * Example:
 *
 * <pre>
 * import javafx.scene.*;
 * import javafx.scene.image.*;
 * import javafx.scene.paint.*;
 *
 * WritableImage writableImage = new WritableImage(250,250);
 * ImageView root = new ImageView(writableImage);
 * Scene s = new Scene(root, 300, 300, Color.BLACK);
 *
 * DrawingContext c = writableImage.getDrawingContext();
 *
 * c.setFill(Color.BLUE);
 * c.fillRect(75,75,100,100);
 * </pre>
 *
 * @see WritableImage
 * @since 28
 */
public interface DrawingContext {

    /**
     * Gets the current stroke.
     * The default value is {@link Color#BLACK BLACK}.
     * The stroke paint is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return the {@code Paint} to be used as the stroke {@code Paint}.
     */
    Paint getStroke();

    /**
     * Sets the current stroke paint attribute.
     * The default value is {@link Color#BLACK BLACK}.
     * The stroke paint is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * A {@code null} value will be ignored and the current value will remain unchanged.
     *
     * @param p The Paint to be used as the stroke Paint or null.
     */
    void setStroke(Paint p);

    /**
     * Gets the current fill paint attribute.
     * The default value is {@link Color#BLACK BLACK}.
     * The fill paint is a <a href="#fill-attr">fill attribute</a>
     * used for any of the fill methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return The {@code Paint} to be used as the fill {@code Paint}.
     */
    Paint getFill();

    /**
     * Sets the current fill paint attribute.
     * The default value is {@link Color#BLACK BLACK}.
     * The fill paint is a <a href="#fill-attr">fill attribute</a>
     * used for any of the fill methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * A {@code null} value will be ignored and the current value will remain unchanged.
     *
     * @param p The {@code Paint} to be used as the fill {@code Paint} or null.
     */
    void setFill(Paint p);

    /**
     * Gets the current global alpha.
     * The default value is {@code 1.0}.
     * The global alpha is a <a href="#comm-attr">common attribute</a>
     * used for nearly all rendering methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return the current global alpha.
     */
    double getGlobalAlpha();

    /**
     * Sets the global alpha of the current state.
     * The default value is {@code 1.0}.
     * Any valid double can be set, but only values in the range
     * {@code [0.0, 1.0]} are valid and the nearest value in that
     * range will be used for rendering.
     * The global alpha is a <a href="#comm-attr">common attribute</a>
     * used for nearly all rendering methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param alpha the new alpha value, clamped to {@code [0.0, 1.0]}
     *              during actual use.
     */
    void setGlobalAlpha(double alpha);

    /**
     * Gets the global blend mode.
     * The default value is {@link BlendMode#SRC_OVER SRC_OVER}.
     * The blend mode is a <a href="#comm-attr">common attribute</a>
     * used for nearly all rendering methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return the global {@code BlendMode} of the current state.
     */
    BlendMode getGlobalBlendMode();

    /**
     * Sets the global blend mode.
     * The default value is {@link BlendMode#SRC_OVER SRC_OVER}.
     * A {@code null} value will be ignored and the current value will remain unchanged.
     * The blend mode is a <a href="#comm-attr">common attribute</a>
     * used for nearly all rendering methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param op the {@code BlendMode} that will be set or null.
     * @throws UnsupportedOperationException if the given blend mode is not supported by this implementation
     */
    void setGlobalBlendMode(BlendMode op);

    /**
     * Get the filling rule attribute for determining the interior of paths
     * in fill and clip operations.
     * The default value is {@code FillRule.NON_ZERO}.
     * The fill rule is a <a href="#path-attr">path attribute</a>
     * used for any of the fill or clip path methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return current fill rule.
     */
    FillRule getFillRule();

    /**
     * Set the filling rule attribute for determining the interior of paths
     * in fill or clip operations.
     * The default value is {@code FillRule.NON_ZERO}.
     * A {@code null} value will be ignored and the current value will remain unchanged.
     * The fill rule is a <a href="#path-attr">path attribute</a>
     * used for any of the fill or clip path methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param fillRule {@code FillRule} with a value of Even_odd or Non_zero or null.
     */
    void setFillRule(FillRule fillRule);

    /**
     * Gets the current line width.
     * The default value is {@code 1.0}.
     * The line width is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return value between 0 and infinity.
     */
    double getLineWidth();

    /**
     * Sets the current line width.
     * The default value is {@code 1.0}.
     * The line width is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * An infinite or non-positive value outside of the range {@code (0, +inf)}
     * will be ignored and the current value will remain unchanged.
     *
     * @param lw value in the range {0-positive infinity}, with any other value
     * being ignored and leaving the value unchanged.
     */
    void setLineWidth(double lw);

    /**
     * Gets the current stroke line cap.
     * The default value is {@link StrokeLineCap#SQUARE SQUARE}.
     * The line cap is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return {@code StrokeLineCap} with a value of Butt, Round, or Square.
     */
    StrokeLineCap getLineCap();

    /**
     * Sets the current stroke line cap.
     * The default value is {@link StrokeLineCap#SQUARE SQUARE}.
     * The line cap is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * A {@code null} value will be ignored and the current value will remain unchanged.
     *
     * @param cap {@code StrokeLineCap} with a value of Butt, Round, or Square or null.
     */
    void setLineCap(StrokeLineCap cap);

    /**
     * Gets the current stroke line join.
     * The default value is {@link StrokeLineJoin#MITER}.
     * The line join is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return {@code StrokeLineJoin} with a value of Miter, Bevel, or Round.
     */
    StrokeLineJoin getLineJoin();

    /**
     * Sets the current stroke line join.
     * The default value is {@link StrokeLineJoin#MITER}.
     * The line join is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * A {@code null} value will be ignored and the current value will remain unchanged.
     *
     * @param join {@code StrokeLineJoin} with a value of Miter, Bevel, or Round or null.
     */
    void setLineJoin(StrokeLineJoin join);

    /**
     * Gets the current miter limit.
     * The default value is {@code 10.0}.
     * The miter limit is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return the miter limit value in the range {@code 0.0-positive infinity}
     */
    double getMiterLimit();

    /**
     * Sets the current miter limit.
     * The default value is {@code 10.0}.
     * The miter limit is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * An infinite or non-positive value outside of the range {@code (0, +inf)}
     * will be ignored and the current value will remain unchanged.
     *
     * @param ml miter limit value between 0 and positive infinity with
     * any other value being ignored and leaving the value unchanged.
     */
    void setMiterLimit(double ml);

    /**
     * Sets the current stroke line dash pattern to a normalized copy of
     * the argument.
     * The default value is {@code null}.
     * The line dash array is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * If the array is {@code null} or empty or contains all {@code 0} elements
     * then dashing will be disabled and the current dash array will be set
     * to {@code null}.
     * If any of the elements of the array are a negative, infinite, or NaN
     * value outside the range {@code [0, +inf)} then the entire array will
     * be ignored and the current dash array will remain unchanged.
     * If the array is an odd length then it will be treated as if it
     * were two copies of the array appended to each other.
     *
     * @param dashes the array of finite non-negative dash lengths
     */
    void setLineDashes(double... dashes);

    /**
     * Gets a copy of the current line dash array.
     * The default value is {@code null}.
     * The array may be normalized by the validation tests in the
     * {@link #setLineDashes(double...)} method.
     * The line dash array is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return a copy of the current line dash array.
     */
    double[] getLineDashes();

    /**
     * Sets the line dash offset.
     * The default value is {@code 0.0}.
     * The line dash offset is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * An infinite or NaN value outside of the range {@code (-inf, +inf)}
     * will be ignored and the current value will remain unchanged.
     *
     * @param dashOffset the line dash offset in the range {@code (-inf, +inf)}
     */
    void setLineDashOffset(double dashOffset);

    /**
     * Gets the current line dash offset.
     * The default value is {@code 0.0}.
     * The line dash offset is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return the line dash offset in the range {@code (-inf, +inf)}
     */
    double getLineDashOffset();

    /**
     * Gets the current image smoothing state.
     *
     * @defaultValue {@code true}
     * @return image smoothing state
     */
    boolean isImageSmoothing();

    /**
     * Sets the image smoothing state.
     * Image smoothing is an <a href="#image-attr">Image attribute</a>
     * used to enable or disable image smoothing for
     * {@link #drawImage(javafx.scene.image.Image, double, double) drawImage(all forms)}
     * as specified in the <a href="#attr-ops-table">Rendering Attributes Table</a>.<br>
     * If image smoothing is {@code true}, images will be scaled using a higher
     * quality filtering when transforming or scaling the source image to fit
     * in the destination rectangle.<br>
     * If image smoothing is {@code false}, images will be scaled without filtering
     * (or by using a lower quality filtering) when transforming or scaling the
     * source image to fit in the destination rectangle.
     *
     * @defaultValue {@code true}
     * @param imageSmoothing {@code true} to enable or {@code false} to disable smoothing
     */
    void setImageSmoothing(boolean imageSmoothing);

    /**
     * Returns a copy of the current transform.
     *
     * @return a copy of the transform of the current state.
     */
    Affine getTransform();

    /**
     * Copies the current transform into the supplied object, creating
     * a new {@link Affine} object if it is null, and returns the object
     * containing the copy.
     *
     * @param xform A transform object that will be used to hold the result.
     * If xform is non null, then this method will copy the current transform
     * into that object. If xform is null a new transform object will be
     * constructed. In either case, the return value is a copy of the current
     * transform.
     *
     * @return A copy of the current transform.
     */
    Affine getTransform(Affine xform);

    /**
     * Sets the current transform.
     * <p>
     * Implementations may throw {@link UnsupportedOperationException} when a
     * transform containing rotation or shear is set while a clip is active.
     *
     * @param mxx the X coordinate scaling element of the 3x4 matrix
     * @param myx the Y coordinate shearing element of the 3x4 matrix
     * @param mxy the X coordinate shearing element of the 3x4 matrix
     * @param myy the Y coordinate scaling element of the 3x4 matrix
     * @param mxt the X coordinate translation element of the 3x4 matrix
     * @param myt the Y coordinate translation element of the 3x4 matrix
     * @throws UnsupportedOperationException if a transform containing rotation
     *         or shear is set while a clip is active and the implementation does
     *         not support such clips
     */
    void setTransform(
        double mxx, double myx,
        double mxy, double myy,
        double mxt, double myt
    );

    /**
     * Sets the current transform. Only 2D transforms are supported. The only
     * values used are the X and Y scaling, translation, and shearing components
     * of a transform.
     * <p>
     * A {@code null} value will be ignored and the current value will remain unchanged.
     * <p>
     * Implementations may throw {@link UnsupportedOperationException} when a
     * transform containing rotation or shear is set while a clip is active.
     *
     * @param xform The affine to be copied and used as the current transform.
     * @throws UnsupportedOperationException if a transform containing rotation
     *         or shear is set while a clip is active and the implementation does
     *         not support such clips
     */
    void setTransform(Affine xform);

    /**
     * Translates the current transform by x, y.
     * The transform is a <a href="#comm-attr">common attribute</a> used for
     * nearly all rendering operations as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param x value to translate along the x axis.
     * @param y value to translate along the y axis.
     */
    void translate(double x, double y);

    /**
     * Scales the current transform by x, y.
     * The transform is a <a href="#comm-attr">common attribute</a> used for
     * nearly all rendering operations as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param x value to scale in the x axis.
     * @param y value to scale in the y axis.
     */
    void scale(double x, double y);

    /**
     * Rotates the current transform in degrees.
     * The transform is a <a href="#comm-attr">common attribute</a> used for
     * nearly all rendering operations as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * <p>
     * Implementations may throw {@link UnsupportedOperationException} when the
     * resulting transform contains rotation or shear while a clip is active.
     *
     * @param degrees value in degrees to rotate the current transform.
     * @throws UnsupportedOperationException if the resulting transform contains
     *         rotation or shear while a clip is active and the implementation does
     *         not support such clips
     */
    void rotate(double degrees);

    /**
     * Concatenates the input with the current transform.
     * The transform is a <a href="#comm-attr">common attribute</a> used for
     * nearly all rendering operations as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * <p>
     * Implementations may throw {@link UnsupportedOperationException} when the
     * resulting transform contains rotation or shear while a clip is active.
     *
     * @param mxx the X coordinate scaling element of the 3x4 matrix
     * @param myx the Y coordinate shearing element of the 3x4 matrix
     * @param mxy the X coordinate shearing element of the 3x4 matrix
     * @param myy the Y coordinate scaling element of the 3x4 matrix
     * @param mxt the X coordinate translation element of the 3x4 matrix
     * @param myt the Y coordinate translation element of the 3x4 matrix
     * @throws UnsupportedOperationException if the resulting transform contains
     *         rotation or shear while a clip is active and the implementation does
     *         not support such clips
     */
    void transform(
        double mxx, double myx,
        double mxy, double myy,
        double mxt, double myt
    );

    /**
     * Concatenates the input with the current transform. Only 2D transforms are
     * supported. The only values used are the X and Y scaling, translation, and
     * shearing components of a transform.
     * <p>
     * A {@code null} value will be ignored and the current value will remain unchanged.
     * <p>
     * Implementations may throw {@link UnsupportedOperationException} when the
     * resulting transform contains rotation or shear while a clip is active.
     *
     * @param xform The affine to be concatenated with the current transform or null.
     * @throws UnsupportedOperationException if the resulting transform contains
     *         rotation or shear while a clip is active and the implementation does
     *         not support such clips
     */
    void transform(Affine xform);

    /**
     * Intersects the current clip with the specified rectangle and applies it to
     * subsequent rendering operations as a clipping mask.
     * The current clip is a <a href="#comm-attr">common attribute</a>
     * used for nearly all rendering operations as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * <p>
     * The rectangle is specified in user coordinates and is transformed by the
     * current transform.
     * <p>
     * Implementations may throw {@link UnsupportedOperationException} when the
     * current transform contains rotation or shear, since the clip could not be
     * represented as an axis-aligned rectangle. Implementations that support such
     * clips exactly will not throw.
     *
     * @param x the X position of the upper left corner of the rectangle
     * @param y the Y position of the upper left corner of the rectangle
     * @param w the width of the rectangle
     * @param h the height of the rectangle
     * @throws UnsupportedOperationException if the current transform contains
     *         rotation or shear and the implementation does not support such clips
     */
    void clipRect(double x, double y, double w, double h);

    /**
     * Saves the following attributes onto a stack.
     * <ul>
     *     <li>Global Alpha</li>
     *     <li>Global Blend Operation</li>
     *     <li>Transform</li>
     *     <li>Fill Paint</li>
     *     <li>Stroke Paint</li>
     *     <li>Line Width</li>
     *     <li>Line Cap</li>
     *     <li>Line Join</li>
     *     <li>Miter Limit</li>
     *     <li>Line Dashes</li>
     *     <li>Dash Offset</li>
     *     <li>Clip</li>
     *     <li>Font</li>
     *     <li>Text Align</li>
     *     <li>Text Baseline</li>
     *     <li>Font Smoothing Type</li>
     *     <li>Image Smoothing</li>
     *     <li>Fill Rule</li>
     * </ul>
     * This method does NOT alter the current state in any way.
     */
    void save();

    /**
     * Pops the state off of the stack, setting the following attributes to their
     * value at the time when that state was pushed onto the stack. If the stack
     * is empty then nothing is changed.
     * <ul>
     *     <li>Global Alpha</li>
     *     <li>Global Blend Operation</li>
     *     <li>Transform</li>
     *     <li>Fill Paint</li>
     *     <li>Stroke Paint</li>
     *     <li>Line Width</li>
     *     <li>Line Cap</li>
     *     <li>Line Join</li>
     *     <li>Miter Limit</li>
     *     <li>Line Dashes</li>
     *     <li>Dash Offset</li>
     *     <li>Clip</li>
     *     <li>Font</li>
     *     <li>Text Align</li>
     *     <li>Text Baseline</li>
     *     <li>Font Smoothing Type</li>
     *     <li>Image Smoothing</li>
     *     <li>Fill Rule</li>
     * </ul>
     */
    void restore();

    /**
     * Strokes a line using the current stroke paint.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#strk-attr">stroke</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param x1 the X coordinate of the starting point of the line.
     * @param y1 the Y coordinate of the starting point of the line.
     * @param x2 the X coordinate of the ending point of the line.
     * @param y2 the Y coordinate of the ending point of the line.
     */
    void strokeLine(double x1, double y1, double x2, double y2);

    /**
     * Strokes a rectangle using the current stroke paint.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#strk-attr">stroke</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param x the X position of the upper left corner of the rectangle.
     * @param y the Y position of the upper left corner of the rectangle.
     * @param w the width of the rectangle.
     * @param h the height of the rectangle.
     */
    void strokeRect(double x, double y, double w, double h);

    /**
     * Clears a portion of the drawing surface with a transparent color value.
     * <p>
     * This method is not affected by any of the rendering attributes.
     *
     * @param x X position of the upper left corner of the rectangle.
     * @param y Y position of the upper left corner of the rectangle.
     * @param w width of the rectangle.
     * @param h height of the rectangle.
     */
    void clearRect(double x, double y, double w, double h);

    /**
     * Fills a rectangle using the current fill paint.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#fill-attr">fill</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param x the X position of the upper left corner of the rectangle.
     * @param y the Y position of the upper left corner of the rectangle.
     * @param w the width of the rectangle.
     * @param h the height of the rectangle.
     */
    void fillRect(double x, double y, double w, double h);

    /**
     * Strokes a rounded rectangle using the current stroke paint.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#strk-attr">stroke</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param x the X coordinate of the upper left bound of the oval.
     * @param y the Y coordinate of the upper left bound of the oval.
     * @param w the width at the center of the oval.
     * @param h the height at the center of the oval.
     * @param arcWidth the arc width of the rectangle corners.
     * @param arcHeight the arc height of the rectangle corners.
     */
    void strokeRoundRect(double x, double y, double w, double h, double arcWidth, double arcHeight);

    /**
     * Fills a rounded rectangle using the current fill paint.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#fill-attr">fill</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param x the X coordinate of the upper left bound of the oval.
     * @param y the Y coordinate of the upper left bound of the oval.
     * @param w the width at the center of the oval.
     * @param h the height at the center of the oval.
     * @param arcWidth the arc width of the rectangle corners.
     * @param arcHeight the arc height of the rectangle corners.
     */
    void fillRoundRect(double x, double y, double w, double h, double arcWidth, double arcHeight);

    /**
     * Strokes an oval using the current stroke paint.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#strk-attr">stroke</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param x the X coordinate of the upper left bound of the oval.
     * @param y the Y coordinate of the upper left bound of the oval.
     * @param w the width at the center of the oval.
     * @param h the height at the center of the oval.
     */
    void strokeOval(double x, double y, double w, double h);

    /**
     * Fills an oval using the current fill paint.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#fill-attr">fill</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param x the X coordinate of the upper left bound of the oval.
     * @param y the Y coordinate of the upper left bound of the oval.
     * @param w the width at the center of the oval.
     * @param h the height at the center of the oval.
     */
    void fillOval(double x, double y, double w, double h);

    /**
     * Strokes an Arc using the current stroke paint. A {@code null} ArcType or
     * zero width or height will cause the render command to be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#strk-attr">stroke</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param x the X coordinate of the arc.
     * @param y the Y coordinate of the arc.
     * @param w the width of the arc.
     * @param h the height of the arc.
     * @param startAngle the starting angle of the arc in degrees.
     * @param arcExtent arcExtent the angular extent of the arc in degrees.
     * @param closure closure type (Round, Chord, Open) or null
     */
    void strokeArc(double x, double y, double w, double h, double startAngle, double arcExtent, ArcType closure);

    /**
     * Fills an arc using the current fill paint. A {@code null} ArcType or
     * zero width or height will cause the render command to be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#fill-attr">fill</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param x the X coordinate of the arc.
     * @param y the Y coordinate of the arc.
     * @param w the width of the arc.
     * @param h the height of the arc.
     * @param startAngle the starting angle of the arc in degrees.
     * @param arcExtent the angular extent of the arc in degrees.
     * @param closure closure type (Round, Chord, Open) or null.
     */
    void fillArc(double x, double y, double w, double h, double startAngle, double arcExtent, ArcType closure);

    /**
     * Strokes a polyline with the given points using the currently set stroke
     * paint attribute.
     * A {@code null} value for any of the arrays will be ignored and nothing will be drawn.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#strk-attr">stroke</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param xPoints array containing the x coordinates of the polyline's points or null.
     * @param yPoints array containing the y coordinates of the polyline's points or null.
     * @param nPoints the number of points that make the polyline.
     */
    void strokePolyline(double xPoints[], double yPoints[], int nPoints);

    /**
     * Strokes a polygon with the given points using the currently set stroke paint.
     * A {@code null} value for any of the arrays will be ignored and nothing will be drawn.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#strk-attr">stroke</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param xPoints array containing the x coordinates of the polygon's points or null.
     * @param yPoints array containing the y coordinates of the polygon's points or null.
     * @param nPoints the number of points that make the polygon.
     */
    void strokePolygon(double[] xPoints, double[] yPoints, int nPoints);

    /**
     * Fills a polygon with the given points using the currently set fill paint.
     * A {@code null} value for any of the arrays will be ignored and nothing will be drawn.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>,
     * <a href="#fill-attr">fill</a>,
     * or <a href="#path-attr">Fill Rule</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param xPoints array containing the x coordinates of the polygon's points or null.
     * @param yPoints array containing the y coordinates of the polygon's points or null.
     * @param nPoints the number of points that make the polygon.
     */
    void fillPolygon(double xPoints[], double yPoints[], int nPoints);

    /**
     * Resets the path.
     * The current path is a <a href="#path-attr">path attribute</a>
     * used for any of the path methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>
     * and <b>is not affected</b> by the {@link #save()} and
     * {@link #restore()} operations.
     */
    void beginPath();

    /**
     * Issues a move command for the current path to the given x,y coordinate.
     * The coordinates are transformed by the current transform as they are
     * added to the path and unaffected by subsequent changes to the transform.
     * The current path is a <a href="#path-attr">path attribute</a>
     * used for any of the path methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>
     * and <b>is not affected</b> by the {@link #save()} and
     * {@link #restore()} operations.
     *
     * @param x0 the X position for the move to command.
     * @param y0 the Y position for the move to command.
     */
    void moveTo(double x0, double y0);

    /**
     * Adds segments to the current path to make a line to the given x,y
     * coordinate.
     * The coordinates are transformed by the current transform as they are
     * added to the path and unaffected by subsequent changes to the transform.
     * The current path is a <a href="#path-attr">path attribute</a>
     * used for any of the path methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>
     * and <b>is not affected</b> by the {@link #save()} and
     * {@link #restore()} operations.
     *
     * @param x1 the X coordinate of the ending point of the line.
     * @param y1 the Y coordinate of the ending point of the line.
     */
    void lineTo(double x1, double y1);

    /**
     * Adds segments to the current path to make a quadratic Bezier curve.
     * The coordinates are transformed by the current transform as they are
     * added to the path and unaffected by subsequent changes to the transform.
     * The current path is a <a href="#path-attr">path attribute</a>
     * used for any of the path methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>
     * and <b>is not affected</b> by the {@link #save()} and
     * {@link #restore()} operations.
     *
     * @param xc the X coordinate of the control point
     * @param yc the Y coordinate of the control point
     * @param x1 the X coordinate of the end point
     * @param y1 the Y coordinate of the end point
     */
    void quadraticCurveTo(double xc, double yc, double x1, double y1);

    /**
     * Adds segments to the current path to make a cubic Bezier curve.
     * The coordinates are transformed by the current transform as they are
     * added to the path and unaffected by subsequent changes to the transform.
     * The current path is a <a href="#path-attr">path attribute</a>
     * used for any of the path methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>
     * and <b>is not affected</b> by the {@link #save()} and
     * {@link #restore()} operations.
     *
     * @param xc1 the X coordinate of first Bezier control point.
     * @param yc1 the Y coordinate of the first Bezier control point.
     * @param xc2 the X coordinate of the second Bezier control point.
     * @param yc2 the Y coordinate of the second Bezier control point.
     * @param x1  the X coordinate of the end point.
     * @param y1  the Y coordinate of the end point.
     */
    void bezierCurveTo(double xc1, double yc1, double xc2, double yc2, double x1, double y1);

    /**
     * Adds path elements to the current path to make an arc.
     * The coordinates are transformed by the current transform as they are
     * added to the path and unaffected by subsequent changes to the transform.
     * The current path is a <a href="#path-attr">path attribute</a>
     * used for any of the path methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>
     * and <b>is not affected</b> by the {@link #save()} and
     * {@link #restore()} operations.
     * <p>
     * If {@code p0} is the current point in the path and {@code p1} is the
     * point specified by {@code (x1, y1)} and {@code p2} is the point
     * specified by {@code (x2, y2)}, then the arc segments appended will
     * be segments along the circumference of a circle of the specified
     * radius touching and inscribed into the convex (interior) side of
     * {@code p0->p1->p2}.  The path will contain a line segment (if
     * needed) to the tangent point between that circle and {@code p0->p1}
     * followed by circular arc segments to reach the tangent point between
     * the circle and {@code p1->p2} and will end with the current point at
     * that tangent point (not at {@code p2}).
     * Note that the radius and circularity of the arc segments will be
     * measured or considered relative to the current transform, but the
     * resulting segments that are computed from those untransformed
     * points will then be transformed when they are added to the path.
     * Since all computation is done in untransformed space, but the
     * pre-existing path segments are all transformed, the ability to
     * correctly perform the computation may implicitly depend on being
     * able to inverse transform the current end of the current path back
     * into untransformed coordinates.
     * </p>
     * <p>
     * If there is no way to compute and inscribe the indicated circle
     * for any reason then the entire operation will simply append segments
     * to force a line to point {@code p1}.  Possible reasons that the
     * computation may fail include:
     * <ul>
     * <li>The current path is empty.</li>
     * <li>The segments {@code p0->p1->p2} are colinear.</li>
     * <li>the current transform is non-invertible so that the current end
     * point of the current path cannot be untransformed for computation.</li>
     * </ul>
     * <p>
     * Implementations that do not support this operation will throw
     * {@link UnsupportedOperationException}.
     * </p>
     *
     * @param x1 the X coordinate of the first point of the arc.
     * @param y1 the Y coordinate of the first point of the arc.
     * @param x2 the X coordinate of the second point of the arc.
     * @param y2 the Y coordinate of the second point of the arc.
     * @param radius the radius of the arc in the range {0.0-positive infinity}.
     * @throws UnsupportedOperationException if the implementation does not
     *         support this operation
     */
    void arcTo(double x1, double y1, double x2, double y2, double radius);

    /**
     * Adds path elements to the current path to make an arc that uses Euclidean
     * degrees. This Euclidean orientation sweeps from East to North, then West,
     * then South, then back to East.
     * The coordinates are transformed by the current transform as they are
     * added to the path and unaffected by subsequent changes to the transform.
     * The current path is a <a href="#path-attr">path attribute</a>
     * used for any of the path methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>
     * and <b>is not affected</b> by the {@link #save()} and
     * {@link #restore()} operations.
     *
     * @param centerX the center x position of the arc.
     * @param centerY the center y position of the arc.
     * @param radiusX the x radius of the arc.
     * @param radiusY the y radius of the arc.
     * @param startAngle the starting angle of the arc in the range {@code 0-360.0}
     * @param length  the length of the baseline of the arc.
     */
    void arc(double centerX, double centerY,
             double radiusX, double radiusY,
             double startAngle, double length);

    /**
     * Adds path elements to the current path to make a rectangle.
     * The coordinates are transformed by the current transform as they are
     * added to the path and unaffected by subsequent changes to the transform.
     * The current path is a <a href="#path-attr">path attribute</a>
     * used for any of the path methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>
     * and <b>is not affected</b> by the {@link #save()} and
     * {@link #restore()} operations.
     *
     * @param x x position of the upper left corner of the rectangle.
     * @param y y position of the upper left corner of the rectangle.
     * @param w width of the rectangle.
     * @param h height of the rectangle.
     */
    void rect(double x, double y, double w, double h);

    /**
     * Appends an SVG Path string to the current path. If there is no current
     * path the string must then start with either type of move command.
     * A {@code null} value or incorrect SVG path will be ignored.
     * The coordinates are transformed by the current transform as they are
     * added to the path and unaffected by subsequent changes to the transform.
     * The current path is a <a href="#path-attr">path attribute</a>
     * used for any of the path methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>
     * and <b>is not affected</b> by the {@link #save()} and
     * {@link #restore()} operations.
     * <p>
     * Implementations that do not support this operation will throw
     * {@link UnsupportedOperationException}.
     * </p>
     *
     * @param svgpath the SVG Path string.
     * @throws UnsupportedOperationException if the implementation does not
     *         support this operation
     */
    void appendSVGPath(String svgpath);

    /**
     * Closes the path.
     * The current path is a <a href="#path-attr">path attribute</a>
     * used for any of the path methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>
     * and <b>is not affected</b> by the {@link #save()} and
     * {@link #restore()} operations.
     */
    void closePath();

    /**
     * Fills the path with the current fill paint.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>,
     * <a href="#fill-attr">fill</a>,
     * or <a href="#path-attr">path</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * Note that the path segments were transformed as they were originally
     * added to the current path so the current transform will not affect
     * those path segments again, but it may affect other attributes in
     * affect at the time of the {@code fill()} operation.
     * </p>
     */
    void fill();

    /**
     * Strokes the path with the current stroke paint.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>,
     * <a href="#strk-attr">stroke</a>,
     * or <a href="#path-attr">path</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * Note that the path segments were transformed as they were originally
     * added to the current path so the current transform will not affect
     * those path segments again, but it may affect other attributes in
     * affect at the time of the {@code stroke()} operation.
     * </p>
     */
    void stroke();

    /**
     * Intersects the current clip with the current path and applies it to
     * subsequent rendering operations as an anti-aliased mask.
     * The current clip is a <a href="#comm-attr">common attribute</a>
     * used for nearly all rendering operations as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * <p>
     * This method will itself be affected only by the
     * <a href="#path-attr">path</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * Note that the path segments were transformed as they were originally
     * added to the current path so the current transform will not affect
     * those path segments again, but it may affect other attributes in
     * affect at the time of the {@code clip()} operation.
     * </p>
     * <p>
     * Implementations that do not support path based clipping will throw
     * {@link UnsupportedOperationException}.
     * </p>
     *
     * @throws UnsupportedOperationException if the implementation does not
     *         support path based clipping
     */
    void clip();

    /**
     * Returns true if the the given x,y point is inside the path.
     *
     * @param x the X coordinate to use for the check.
     * @param y the Y coordinate to use for the check.
     * @return true if the point given is inside the path, false
     * otherwise.
     */
    boolean isPointInPath(double x, double y);

    /**
     * Gets the current font.
     *
     * @defaultValue Font.getDefault()
     * @return the current font
     */
    Font getFont();

    /**
     * Sets the current font.
     * A {@code null} value will be ignored and the current value will remain unchanged.
     * The font is a text attribute used for any of the text methods.
     *
     * @param f the font to set or null.
     */
    void setFont(Font f);

    /**
     * Gets the current {@code TextAlignment}.
     *
     * @defaultValue TextAlignment.LEFT
     * @return the current {@code TextAlignment}
     */
    TextAlignment getTextAlign();

    /**
     * Sets the current {@code TextAlignment}.
     * A {@code null} value will be ignored and the current value will remain unchanged.
     * The text align is a text attribute used for any of the text methods.
     *
     * @param align the {@code TextAlignment} to set or null.
     */
    void setTextAlign(TextAlignment align);

    /**
     * Gets the current {@code VPos}.
     *
     * @defaultValue VPos.BASELINE
     * @return the current {@code VPos}
     */
    VPos getTextBaseline();

    /**
     * Sets the current {@code VPos}.
     * A {@code null} value will be ignored and the current value will remain unchanged.
     * The text baseline is a text attribute used for any of the text methods.
     *
     * @param baseline the {@code VPos} to set or null.
     */
    void setTextBaseline(VPos baseline);

    /**
     * Sets the current Font Smoothing Type.
     * The default value is {@link FontSmoothingType#GRAY GRAY}.
     * The font smoothing type is a text attribute used for any of the text methods.
     * A {@code null} value will be ignored and the current value will remain unchanged.
     * <p>
     * <b>Note</b> that the {@code FontSmoothingType} value of
     * {@link FontSmoothingType#LCD LCD} is only supported over an opaque
     * background. {@code LCD} text will generally appear as {@code GRAY}
     * text over transparent or partially transparent pixels, and in some
     * implementations it may not be supported at all on a surface that
     * contains an alpha channel.
     *
     * @param fontsmoothing the {@link FontSmoothingType} or null.
     */
    void setFontSmoothingType(FontSmoothingType fontsmoothing);

    /**
     * Gets the current Font Smoothing Type.
     * The default value is {@link FontSmoothingType#GRAY GRAY}.
     * The font smoothing type is a text attribute used for any of the text methods.
     *
     * @return the {@link FontSmoothingType}
     */
    FontSmoothingType getFontSmoothingType();

    /**
     * Fills the given string of text at position x, y
     * with the current fill paint attribute.
     * A {@code null} text value will be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#fill-attr">fill</a>
     * attributes, and by the current font, text alignment, and text baseline.
     *
     * @param text the string of text or null.
     * @param x position on the x axis.
     * @param y position on the y axis.
     */
    void fillText(String text, double x, double y);

    /**
     * Draws the given string of text at position x, y
     * with the current stroke paint attribute.
     * A {@code null} text value will be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#strk-attr">stroke</a>
     * attributes, and by the current font, text alignment, and text baseline.
     *
     * @param text the string of text or null.
     * @param x position on the x axis.
     * @param y position on the y axis.
     */
    void strokeText(String text, double x, double y);

    /**
     * Fills text and includes a maximum width of the string.
     * If the width of the text extends past max width, then it will be sized
     * to fit.
     * A {@code null} text value will be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#fill-attr">fill</a>
     * attributes, and by the current font, text alignment, and text baseline.
     *
     * @param text the string of text or null.
     * @param x position on the x axis.
     * @param y position on the y axis.
     * @param maxWidth the maximum width of the string.
     */
    void fillText(String text, double x, double y, double maxWidth);

    /**
     * Draws text and includes a maximum width of the string.
     * If the width of the text extends past max width, then it will be sized
     * to fit.
     * A {@code null} text value will be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#strk-attr">stroke</a>
     * attributes, and by the current font, text alignment, and text baseline.
     *
     * @param text the string of text or null.
     * @param x position on the x axis.
     * @param y position on the y axis.
     * @param maxWidth the maximum width of the string.
     */
    void strokeText(String text, double x, double y, double maxWidth);

    /**
     * Draws an image at the given x, y position using the width
     * and height of the given image.
     * A {@code null} image value or an image still in progress will be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#image-attr">image</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param img the image to be drawn or null.
     * @param x the X coordinate on the destination for the upper left of the image.
     * @param y the Y coordinate on the destination for the upper left of the image.
     */
    default void drawImage(Image img, double x, double y) {
        if (img == null || img.getProgress() < 1.0) {
            return;
        }

        drawImage(img, x, y, img.getWidth(), img.getHeight());
    }

    /**
     * Draws an image into the given destination rectangle of the drawing surface. The
     * Image is scaled to fit into the destination rectangle.
     * A {@code null} image value or an image still in progress will be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#image-attr">image</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param img the image to be drawn or null.
     * @param x the X coordinate on the destination for the upper left of the image.
     * @param y the Y coordinate on the destination for the upper left of the image.
     * @param w the width of the destination rectangle.
     * @param h the height of the destination rectangle.
     */
    default void drawImage(Image img, double x, double y, double w, double h) {
        if (img == null || img.getProgress() < 1.0) {
            return;
        }

        drawImage(img, 0, 0, img.getWidth(), img.getHeight(), x, y, w, h);
    }

    /**
     * Draws the specified source rectangle of the given image to the given
     * destination rectangle of the drawing surface.
     * A {@code null} image value or an image still in progress will be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#image-attr">image</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param img the image to be drawn or null.
     * @param sx the source rectangle's X coordinate position.
     * @param sy the source rectangle's Y coordinate position.
     * @param sw the source rectangle's width.
     * @param sh the source rectangle's height.
     * @param dx the destination rectangle's X coordinate position.
     * @param dy the destination rectangle's Y coordinate position.
     * @param dw the destination rectangle's width.
     * @param dh the destination rectangle's height.
     */
    void drawImage(
        Image img,
        double sx, double sy, double sw, double sh,
        double dx, double dy, double dw, double dh
    );
}
