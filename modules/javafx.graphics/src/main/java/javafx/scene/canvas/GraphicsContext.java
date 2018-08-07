/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.canvas;

import com.sun.javafx.geom.Arc2D;
import com.sun.javafx.geom.IllegalPathStateException;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.javafx.image.*;
import com.sun.javafx.image.impl.ByteBgraPre;
import com.sun.javafx.sg.prism.GrowableDataBuffer;
import com.sun.javafx.sg.prism.NGCanvas;
import com.sun.javafx.scene.text.FontHelper;
import com.sun.javafx.tk.Toolkit;
import com.sun.scenario.effect.EffectHelper;
import javafx.geometry.NodeOrientation;
import javafx.geometry.VPos;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import javafx.scene.text.FontSmoothingType;

/**
 * This class is used to issue draw calls to a {@link Canvas} using a buffer.
 * <p>
 * Each call pushes the necessary parameters onto the buffer
 * where they will be later rendered onto the image of the {@code Canvas} node
 * by the rendering thread at the end of a pulse.
 * <p>
 * A {@code Canvas} only contains one {@code GraphicsContext}, and only one buffer.
 * If it is not attached to any scene, then it can be modified by any thread,
 * as long as it is only used from one thread at a time. Once a {@code Canvas}
 * node is attached to a scene, it must be modified on the JavaFX Application
 * Thread.
 * <p>
 * Calling any method on the {@code GraphicsContext} is considered modifying
 * its corresponding {@code Canvas} and is subject to the same threading
 * rules.
 * <p>
 * A {@code GraphicsContext} also manages a stack of state objects that can
 * be saved or restored at anytime.
 * <p>
 * The {@code GraphicsContext} maintains the following rendering attributes
 * which affect various subsets of the rendering methods:
 * <table class="overviewSummary" style="width:80%; margin-left:auto; margin-right:auto">
 * <caption>List of Rendering Attributes</caption>
 * <tr>
 * <th class="colLast" style="width:15%" scope="col">Attribute</th>
 * <th class="colLast" style="width:10%; text-align:center" scope="col">Save/Restore?</th>
 * <th class="colLast" style="width:10%; text-align:center" scope="col">Default value</th>
 * <th class="colLast" scope="col">Description</th>
 * </tr>
 * <tr><th colspan="3" scope="row"><a id="comm-attr">Common Rendering Attributes</a></th></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #clip() Clip}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">No clipping</td>
 * <td class="colLast">
 * An anti-aliased intersection of various clip paths to which rendering
 * is restricted.
 * </td></tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setGlobalAlpha(double) Global Alpha}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@code 1.0}</td>
 * <td class="colLast">
 * An opacity value that controls the visibility or fading of each rendering
 * operation.
 * </td></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setGlobalBlendMode(javafx.scene.effect.BlendMode) Global Blend Mode}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@link BlendMode#SRC_OVER SRC_OVER}</td>
 * <td class="colLast">
 * A {@link BlendMode} enum value that controls how pixels from each rendering
 * operation are composited into the existing image.
 * </td></tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setTransform(javafx.scene.transform.Affine) Transform}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@code Identity}</td>
 * <td class="colLast">
 * A 3x2 2D affine transformation matrix that controls how coordinates are
 * mapped onto the logical pixels of the canvas image.
 * </td></tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setEffect(javafx.scene.effect.Effect) Effect}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@code null}</td>
 * <td class="colLast">
 * An {@link Effect} applied individually to each rendering operation.
 * </td></tr>
 * <tr><th colspan="3" scope="row"><a id="fill-attr">Fill Attributes</a></th></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setFill(javafx.scene.paint.Paint) Fill Paint}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@link Color#BLACK BLACK}</td>
 * <td class="colLast">
 * The {@link Paint} to be applied to the interior of shapes in a
 * fill operation.
 * </td></tr>
 * <tr><th colspan="3" scope="row"><a id="strk-attr">Stroke Attributes</a></th></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setStroke(javafx.scene.paint.Paint) Stroke Paint}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@link Color#BLACK BLACK}</td>
 * <td class="colLast">
 * The {@link Paint} to be applied to the boundary of shapes in a
 * stroke operation.
 * </td></tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setLineWidth(double) Line Width}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@code 1.0}</td>
 * <td class="colLast">
 * The width of the stroke applied to the boundary of shapes in a
 * stroke operation.
 * </td></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setLineCap(javafx.scene.shape.StrokeLineCap) Line Cap}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@link StrokeLineCap#SQUARE SQUARE}</td>
 * <td class="colLast">
 * The style of the end caps applied to the beginnings and ends of each
 * dash and/or subpath in a stroke operation.
 * </td></tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setLineJoin(javafx.scene.shape.StrokeLineJoin) Line Join}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@link StrokeLineJoin#MITER MITER}</td>
 * <td class="colLast">
 * The style of the joins applied between individual segments in the boundary
 * paths of shapes in a stroke operation.
 * </td></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setMiterLimit(double) Miter Limit}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@code 10.0}</td>
 * <td class="colLast">
 * The ratio limit of how far a {@link StrokeLineJoin#MITER MITER} line join
 * may extend in the direction of a sharp corner between segments in the
 * boundary path of a shape, relative to the line width, before it is truncated
 * to a {@link StrokeLineJoin#BEVEL BEVEL} join in a stroke operation.
 * </td></tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setLineDashes(double...) Dashes}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@code null}</td>
 * <td class="colLast">
 * The array of dash lengths to be applied to the segments in the boundary
 * of shapes in a stroke operation.
 * </td></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setLineDashOffset(double) Dash Offset}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@code 0.0}</td>
 * <td class="colLast">
 * The distance offset into the array of dash lengths at which to start the
 * dashing of the segments in the boundary of shapes in a stroke operation.
 * </td></tr>
 * <tr><th colspan="3" scope="row"><a id="text-attr">Text Attributes</a></th></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setFont(javafx.scene.text.Font) Font}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@link Font#getDefault() Default Font}</td>
 * <td class="colLast">
 * The font used for all fill and stroke text operations.
 * </td></tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setTextAlign(javafx.scene.text.TextAlignment) Text Align}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@link TextAlignment#LEFT LEFT}</td>
 * <td class="colLast">
 * The horizontal alignment of text with respect to the {@code X} coordinate
 * specified in the text operation.
 * </td></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setTextBaseline(javafx.geometry.VPos) Text Baseline}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@link VPos#BASELINE BASELINE}</td>
 * <td class="colLast">
 * The vertical position of the text relative to the {@code Y} coordinate
 * specified in the text operation.
 * </td></tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setFontSmoothingType(javafx.scene.text.FontSmoothingType) Font Smoothing}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@link FontSmoothingType#GRAY GRAY}</td>
 * <td class="colLast">
 * The type of smoothing (antialiasing) applied to the glyphs in the font
 * for all fill text operations.
 * </td></tr>
 * <tr><th colspan="3" scope="row"><a id="path-attr">Path Attributes</a></th></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #beginPath() Current Path}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:10%; text-align:center">Empty path</td>
 * <td class="colLast">
 * The path constructed using various path construction methods to be used
 * in various path filling, stroking, or clipping operations.
 * </td></tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setFillRule(javafx.scene.shape.FillRule) Fill Rule}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@link FillRule#NON_ZERO NON_ZERO}</td>
 * <td class="colLast">
 * The method used to determine the interior of paths for a path fill or
 * clip operation.
 * </td></tr>
 * </table>
 * <p>
 * <a id="attr-ops-table">
 * The various rendering methods on the {@code GraphicsContext} use the
 * following sets of rendering attributes:
 * </a>
 * <table class="overviewSummary" style="width:80%; margin-left:auto; margin-right:auto">
 * <caption>Rendering Attributes Table</caption>
 * <tr>
 * <th scope="col" class="colLast" style="width:25%">Method</th>
 * <th scope="col" class="colLast" style="width:15%; text-align:center"><a href="#comm-attr">Common Rendering Attributes</a></th>
 * <th scope="col" class="colLast" style="width:15%; text-align:center"><a href="#fill-attr">Fill Attributes</a></th>
 * <th scope="col" class="colLast" style="width:15%; text-align:center"><a href="#strk-attr">Stroke Attributes</a></th>
 * <th scope="col" class="colLast" style="width:15%; text-align:center"><a href="#text-attr">Text Attributes</a></th>
 * <th scope="col" class="colLast" style="width:15%; text-align:center"><a href="#path-attr">Path Attributes</a></th>
 * </tr>
 * <tr><th colspan="1" scope="row">Basic Shape Rendering</th></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:25%">
 * {@link #fillRect(double, double, double, double) fillRect()},
 * {@link #fillRoundRect(double, double, double, double, double, double) fillRoundRect()},
 * {@link #fillOval(double, double, double, double) fillOval()},
 * {@link #fillArc(double, double, double, double, double, double, javafx.scene.shape.ArcType) fillArc()}
 * </th>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:25%">
 * {@link #strokeLine(double, double, double, double) strokeLine()},
 * {@link #strokeRect(double, double, double, double) strokeRect()},
 * {@link #strokeRoundRect(double, double, double, double, double, double) strokeRoundRect()},
 * {@link #strokeOval(double, double, double, double) strokeOval()},
 * {@link #strokeArc(double, double, double, double, double, double, javafx.scene.shape.ArcType) strokeArc()}
 * </th>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:25%">
 * {@link #clearRect(double, double, double, double) clearRect()}
 * </th>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes <a href="#base-fn-1">[1]</a></td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:25%">
 * {@link #fillPolygon(double[], double[], int) fillPolygon()}
 * </th>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes <a href="#base-fn-2">[2]</a></td>
 * </tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:25%">
 * {@link #strokePolygon(double[], double[], int) strokePolygon()},
 * {@link #strokePolyline(double[], double[], int) strokePolyline()}
 * </th>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr><th scope="row" colspan="6">
 * <a id="base-fn-1">[1]</a> Only the Transform, Clip, and Effect apply to clearRect()<br>
 * <a id="base-fn-2">[2]</a> Only the Fill Rule applies to fillPolygon(), the current path is left unchanged
 * </th></tr>
 * <tr><th colspan="1" scope="row">Text Rendering</th></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:25%">
 * {@link #fillText(java.lang.String, double, double) fillText()},
 * {@link #fillText(java.lang.String, double, double, double) fillText(with maxWidth)}
 * </th>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes <a href="#text-fn-3">[3]</a></td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:25%">
 * {@link #strokeText(java.lang.String, double, double) strokeText()},
 * {@link #strokeText(java.lang.String, double, double, double) strokeText(with maxWidth)}
 * </th>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes <a href="#text-fn-3">[3]</a></td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr><th scope="row" colspan="6">
 * <a id="text-fn-3">[3]</a> The Font Smoothing attribute only applies to filled text
 * </th></tr>
 * <tr><th colspan="1" scope="row">Path Rendering</th></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:25%">
 * {@link #beginPath() beginPath()},
 * {@link #moveTo(double, double) moveTo()},
 * {@link #lineTo(double, double) lineTo()},
 * {@link #quadraticCurveTo(double, double, double, double) quadraticCurveTo()},
 * {@link #bezierCurveTo(double, double, double, double, double, double) bezierCurveTo()},
 * {@link #arc(double, double, double, double, double, double) arc()},
 * {@link #arcTo(double, double, double, double, double) arcTo()},
 * {@link #appendSVGPath(java.lang.String) appendSVGPath()},
 * {@link #closePath() closePath()},
 * {@link #rect(double, double, double, double) rect()}
 * </th>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes <a href="#path-fn-4">[4]</a></td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:25%">
 * {@link #fill() fill()}
 * </th>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes <a href="#path-fn-4">[4]</a></td>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * </tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:25%">
 * {@link #stroke() stroke()}
 * </th>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes <a href="#path-fn-4">[4]</a></td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes <a href="#path-fn-5">[5]</a></td>
 * </tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:25%">
 * {@link #clip() clip()}
 * </th>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * </tr>
 * <tr><th scope="row" colspan="6">
 * <a id="path-fn-4">[4]</a> Transform applied only during path construction<br>
 * <a id="path-fn-5">[5]</a> Fill Rule only used for fill() and clip()
 * </th></tr>
 * <tr><th scope="row" colspan="1">Image Rendering</th></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:25%">
 * {@link #drawImage(javafx.scene.image.Image, double, double) drawImage(all forms)}
 * </th>
 * <td class="colLast" style="width:15%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr><th scope="row" colspan="1">Miscellaneous</th></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:25%">
 * {@link #applyEffect(javafx.scene.effect.Effect) applyEffect()},
 * {@link #getPixelWriter() PixelWriter methods}
 * </th>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:15%; text-align:center; color:#c00">No</td>
 * </tr>
 * </table>
 *
 * <p>Example:</p>
 *
 * <pre>
 * import javafx.scene.*;
 * import javafx.scene.paint.*;
 * import javafx.scene.canvas.*;
 *
 * Group root = new Group();
 * Scene s = new Scene(root, 300, 300, Color.BLACK);
 *
 * final Canvas canvas = new Canvas(250,250);
 * GraphicsContext gc = canvas.getGraphicsContext2D();
 *
 * gc.setFill(Color.BLUE);
 * gc.fillRect(75,75,100,100);
 *
 * root.getChildren().add(canvas);
 * </pre>
 *
 * @see Canvas
 * @since JavaFX 2.2
 */
public final class GraphicsContext {
    Canvas theCanvas;
    Path2D path;
    boolean pathDirty;

    State curState;
    LinkedList<State> stateStack;
    LinkedList<Path2D> clipStack;

    GraphicsContext(Canvas theCanvas) {
        this.theCanvas = theCanvas;
        this.path = new Path2D();
        pathDirty = true;

        this.curState = new State();
        this.stateStack = new LinkedList<State>();
        this.clipStack = new LinkedList<Path2D>();
    }

    static class State {
        double globalAlpha;
        BlendMode blendop;
        Affine2D transform;
        Paint fill;
        Paint stroke;
        double linewidth;
        StrokeLineCap linecap;
        StrokeLineJoin linejoin;
        double miterlimit;
        double dashes[];
        double dashOffset;
        int numClipPaths;
        Font font;
        FontSmoothingType fontsmoothing;
        TextAlignment textalign;
        VPos textbaseline;
        Effect effect;
        FillRule fillRule;

        State() {
            init();
        }

        final void init() {
            set(1.0, BlendMode.SRC_OVER,
                new Affine2D(),
                Color.BLACK, Color.BLACK,
                1.0, StrokeLineCap.SQUARE, StrokeLineJoin.MITER, 10.0,
                null, 0.0,
                0,
                Font.getDefault(), FontSmoothingType.GRAY,
                TextAlignment.LEFT, VPos.BASELINE,
                null, FillRule.NON_ZERO);
        }

        State(State copy) {
            set(copy.globalAlpha, copy.blendop,
                new Affine2D(copy.transform),
                copy.fill, copy.stroke,
                copy.linewidth, copy.linecap, copy.linejoin, copy.miterlimit,
                copy.dashes, copy.dashOffset,
                copy.numClipPaths,
                copy.font, copy.fontsmoothing, copy.textalign, copy.textbaseline,
                copy.effect, copy.fillRule);
        }

        final void set(double globalAlpha, BlendMode blendop,
                       Affine2D transform, Paint fill, Paint stroke,
                       double linewidth, StrokeLineCap linecap,
                       StrokeLineJoin linejoin, double miterlimit,
                       double dashes[], double dashOffset,
                       int numClipPaths,
                       Font font, FontSmoothingType smoothing,
                       TextAlignment align, VPos baseline,
                       Effect effect, FillRule fillRule)
        {
            this.globalAlpha = globalAlpha;
            this.blendop = blendop;
            this.transform = transform;
            this.fill = fill;
            this.stroke = stroke;
            this.linewidth = linewidth;
            this.linecap = linecap;
            this.linejoin = linejoin;
            this.miterlimit = miterlimit;
            this.dashes = dashes;
            this.dashOffset = dashOffset;
            this.numClipPaths = numClipPaths;
            this.font = font;
            this.fontsmoothing = smoothing;
            this.textalign = align;
            this.textbaseline = baseline;
            this.effect = effect;
            this.fillRule = fillRule;
        }

        State copy() {
            return new State(this);
        }

        void restore(GraphicsContext ctx) {
            ctx.setGlobalAlpha(globalAlpha);
            ctx.setGlobalBlendMode(blendop);
            ctx.setTransform(transform.getMxx(), transform.getMyx(),
                             transform.getMxy(), transform.getMyy(),
                             transform.getMxt(), transform.getMyt());
            ctx.setFill(fill);
            ctx.setStroke(stroke);
            ctx.setLineWidth(linewidth);
            ctx.setLineCap(linecap);
            ctx.setLineJoin(linejoin);
            ctx.setMiterLimit(miterlimit);
            ctx.setLineDashes(dashes);
            ctx.setLineDashOffset(dashOffset);
            GrowableDataBuffer buf = ctx.getBuffer();
            while (ctx.curState.numClipPaths > numClipPaths) {
                ctx.curState.numClipPaths--;
                ctx.clipStack.removeLast();
                buf.putByte(NGCanvas.POP_CLIP);
            }
            ctx.setFillRule(fillRule);
            ctx.setFont(font);
            ctx.setFontSmoothingType(fontsmoothing);
            ctx.setTextAlign(textalign);
            ctx.setTextBaseline(textbaseline);
            ctx.setEffect(effect);
        }
    }

    private GrowableDataBuffer getBuffer() {
        return theCanvas.getBuffer();
    }

    private float coords[] = new float[6];
    private static final byte pgtype[] = {
        NGCanvas.MOVETO,
        NGCanvas.LINETO,
        NGCanvas.QUADTO,
        NGCanvas.CUBICTO,
        NGCanvas.CLOSEPATH,
    };
    private static final int numsegs[] = { 2, 2, 4, 6, 0, };

    private void markPathDirty() {
        pathDirty = true;
    }

    private void writePath(byte command) {
        updateTransform();
        GrowableDataBuffer buf = getBuffer();
        if (pathDirty) {
            buf.putByte(NGCanvas.PATHSTART);
            PathIterator pi = path.getPathIterator(null);
            while (!pi.isDone()) {
                int pitype = pi.currentSegment(coords);
                buf.putByte(pgtype[pitype]);
                for (int i = 0; i < numsegs[pitype]; i++) {
                    buf.putFloat(coords[i]);
                }
                pi.next();
            }
            buf.putByte(NGCanvas.PATHEND);
            pathDirty = false;
        }
        buf.putByte(command);
    }

    private void writePaint(Paint p, byte command) {
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(command);
        buf.putObject(Toolkit.getPaintAccessor().getPlatformPaint(p));
    }

    private void writeArcType(ArcType closure) {
        byte type;
        switch (closure) {
            case OPEN:  type = NGCanvas.ARC_OPEN;  break;
            case CHORD: type = NGCanvas.ARC_CHORD; break;
            case ROUND: type = NGCanvas.ARC_PIE;   break;
            default: return;  // ignored for consistency with other attributes
        }
        writeParam(type, NGCanvas.ARC_TYPE);
    }

    private void writeRectParams(GrowableDataBuffer buf,
                                 double x, double y, double w, double h,
                                 byte command)
    {
        buf.putByte(command);
        buf.putFloat((float) x);
        buf.putFloat((float) y);
        buf.putFloat((float) w);
        buf.putFloat((float) h);
    }

    private void writeOp4(double x, double y, double w, double h, byte command) {
        updateTransform();
        writeRectParams(getBuffer(), x, y, w, h, command);
    }

    private void writeOp6(double x, double y, double w, double h,
                          double v1, double v2, byte command)
    {
        updateTransform();
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(command);
        buf.putFloat((float) x);
        buf.putFloat((float) y);
        buf.putFloat((float) w);
        buf.putFloat((float) h);
        buf.putFloat((float) v1);
        buf.putFloat((float) v2);
    }

    private float polybuf[] = new float[512];
    private void flushPolyBuf(GrowableDataBuffer buf,
                              float polybuf[], int n, byte command)
    {
        curState.transform.transform(polybuf, 0, polybuf, 0, n/2);
        for (int i = 0; i < n; i += 2) {
            buf.putByte(command);
            buf.putFloat(polybuf[i]);
            buf.putFloat(polybuf[i+1]);
            command = NGCanvas.LINETO;
        }
    }
    private void writePoly(double xPoints[], double yPoints[], int nPoints,
                           boolean close, byte command)
    {
        if (xPoints == null || yPoints == null) return;
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(NGCanvas.PATHSTART);
        int pos = 0;
        byte polycmd = NGCanvas.MOVETO;
        for (int i = 0; i < nPoints; i++) {
            if (pos >= polybuf.length) {
                flushPolyBuf(buf, polybuf, pos, polycmd);
                pos = 0;
                polycmd = NGCanvas.LINETO;
            }
            polybuf[pos++] = (float) xPoints[i];
            polybuf[pos++] = (float) yPoints[i];
        }
        flushPolyBuf(buf, polybuf, pos, polycmd);
        if (close) {
            buf.putByte(NGCanvas.CLOSEPATH);
        }
        buf.putByte(NGCanvas.PATHEND);
        // Transform needs to be updated for rendering attributes even though
        // we have already transformed the points as we sent them.
        updateTransform();
        buf.putByte(command);
        // Now that we have changed the PG layer path, we need to mark our path dirty.
        markPathDirty();
    }

    private void writeImage(Image img,
                            double dx, double dy, double dw, double dh)
    {
        if (img == null || img.getProgress() < 1.0) return;
        Object platformImg = Toolkit.getImageAccessor().getPlatformImage(img);
        if (platformImg == null) return;
        updateTransform();
        GrowableDataBuffer buf = getBuffer();
        writeRectParams(buf, dx, dy, dw, dh, NGCanvas.DRAW_IMAGE);
        buf.putObject(platformImg);
    }

    private void writeImage(Image img,
                            double dx, double dy, double dw, double dh,
                            double sx, double sy, double sw, double sh)
    {
        if (img == null || img.getProgress() < 1.0) return;
        Object platformImg = Toolkit.getImageAccessor().getPlatformImage(img);
        if (platformImg == null) return;
        updateTransform();
        GrowableDataBuffer buf = getBuffer();
        writeRectParams(buf, dx, dy, dw, dh, NGCanvas.DRAW_SUBIMAGE);
        buf.putFloat((float) sx);
        buf.putFloat((float) sy);
        buf.putFloat((float) sw);
        buf.putFloat((float) sh);
        buf.putObject(platformImg);
    }

    private void writeText(String text, double x, double y, double maxWidth,
                           byte command)
    {
        if (text == null) return;
        updateTransform();
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(command);
        buf.putFloat((float) x);
        buf.putFloat((float) y);
        buf.putFloat((float) maxWidth);
        buf.putBoolean(theCanvas.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT);
        buf.putObject(text);
    }

    void writeParam(double v, byte command) {
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(command);
        buf.putFloat((float) v);
    }

    private void writeParam(byte v, byte command) {
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(command);
        buf.putByte(v);
    }

    private boolean txdirty;
    private void updateTransform() {
        if (txdirty) {
            txdirty = false;
            GrowableDataBuffer buf = getBuffer();
            buf.putByte(NGCanvas.TRANSFORM);
            buf.putDouble(curState.transform.getMxx());
            buf.putDouble(curState.transform.getMxy());
            buf.putDouble(curState.transform.getMxt());
            buf.putDouble(curState.transform.getMyx());
            buf.putDouble(curState.transform.getMyy());
            buf.putDouble(curState.transform.getMyt());
        }
    }

    void updateDimensions() {
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(NGCanvas.SET_DIMS);
        buf.putFloat((float) theCanvas.getWidth());
        buf.putFloat((float) theCanvas.getHeight());
    }

    private void reset() {
        GrowableDataBuffer buf = getBuffer();
        // Only reset if we have a significant amount of data to omit,
        // this prevents a common occurrence of "setFill(bg); fillRect();"
        // at the start of a session from invoking a reset.
        // But, do a reset anyway if the rendering layer has been falling
        // behind because that lets the synchronization step throw out the
        // older buffers that have been backing up.
        if (buf.writeValuePosition() > Canvas.DEFAULT_VAL_BUF_SIZE ||
            theCanvas.isRendererFallingBehind())
        {
            buf.reset();
            buf.putByte(NGCanvas.RESET);
            updateDimensions();
            txdirty = true;
            pathDirty = true;
            State s = this.curState;
            int numClipPaths = this.curState.numClipPaths;
            this.curState = new State();
            for (int i = 0; i < numClipPaths; i++) {
                Path2D clip = clipStack.get(i);
                buf.putByte(NGCanvas.PUSH_CLIP);
                buf.putObject(clip);
            }
            this.curState.numClipPaths = numClipPaths;
            s.restore(this);
        }
    }

    private void resetIfCovers(Paint p, double x, double y, double w, double h) {
        Affine2D tx = this.curState.transform;
        if (tx.isTranslateOrIdentity()) {
            x += tx.getMxt();
            y += tx.getMyt();
            if (x > 0 || y > 0 ||
                (x+w) < theCanvas.getWidth() ||
                (y+h) < theCanvas.getHeight())
            {
                return;
            }
        } else {
//          quad test for coverage...?
            return;
        }
        if (p != null) {
            if (this.curState.blendop != BlendMode.SRC_OVER) return;
            if (!p.isOpaque() || this.curState.globalAlpha < 1.0) return;
        }
        if (this.curState.numClipPaths > 0) return;
        if (this.curState.effect != null) return;
        reset();
    }

    /**
    * Gets the {@code Canvas} that the {@code GraphicsContext} is issuing draw
    * commands to. There is only ever one {@code Canvas} for a
    * {@code GraphicsContext}.
    *
    * @return Canvas the canvas that this {@code GraphicsContext} is issuing draw
    * commands to.
    */
    public Canvas getCanvas() {
        return theCanvas;
    }

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
     *     <li>Clip</li>
     *     <li>Font</li>
     *     <li>Text Align</li>
     *     <li>Text Baseline</li>
     *     <li>Effect</li>
     *     <li>Fill Rule</li>
     * </ul>
     * This method does NOT alter the current state in any way. Also, note that
     * the current path is not saved.
     */
    public void save() {
        stateStack.push(curState.copy());
    }

    /**
     * Pops the state off of the stack, setting the following attributes to their
     * value at the time when that state was pushed onto the stack. If the stack
     * is empty then nothing is changed.
     *
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
     *     <li>Clip</li>
     *     <li>Font</li>
     *     <li>Text Align</li>
     *     <li>Text Baseline</li>
     *     <li>Effect</li>
     *     <li>Fill Rule</li>
     * </ul>
     * Note that the current path is not restored.
     */
    public void restore() {
        if (!stateStack.isEmpty()) {
            State savedState = stateStack.pop();
            savedState.restore(this);
            txdirty = true;
        }
    }

    /**
     * Translates the current transform by x, y.
     * @param x value to translate along the x axis.
     * @param y value to translate along the y axis.
     */
    public void translate(double x, double y) {
        curState.transform.translate(x, y);
        txdirty = true;
    }

    /**
     * Scales the current transform by x, y.
     * @param x value to scale in the x axis.
     * @param y value to scale in the y axis.
     */
    public void scale(double x, double y) {
        curState.transform.scale(x, y);
        txdirty = true;
    }

    /**
     * Rotates the current transform in degrees.
     * @param degrees value in degrees to rotate the current transform.
     */
    public void rotate(double degrees) {
        curState.transform.rotate(Math.toRadians(degrees));
        txdirty = true;
    }

    /**
     * Concatenates the input with the current transform.
     *
     * @param mxx - the X coordinate scaling element of the 3x4 matrix
     * @param myx - the Y coordinate shearing element of the 3x4 matrix
     * @param mxy - the X coordinate shearing element of the 3x4 matrix
     * @param myy - the Y coordinate scaling element of the 3x4 matrix
     * @param mxt - the X coordinate translation element of the 3x4 matrix
     * @param myt - the Y coordinate translation element of the 3x4 matrix
     */
    public void transform(double mxx, double myx,
                          double mxy, double myy,
                          double mxt, double myt)
    {
        curState.transform.concatenate(mxx, mxy, mxt,
                                       myx, myy, myt);
        txdirty = true;
    }

    /**
     * Concatenates the input with the current transform. Only 2D transforms are
     * supported. The only values used are the X and Y scaling, translation, and
     * shearing components of a transform. A {@code null} value is treated as identity.
     *
     * @param xform The affine to be concatenated with the current transform or null.
     */
    public void transform(Affine xform) {
        if (xform == null) return;
        curState.transform.concatenate(xform.getMxx(), xform.getMxy(), xform.getTx(),
                                       xform.getMyx(), xform.getMyy(), xform.getTy());
        txdirty = true;
    }

    /**
     * Sets the current transform.
     * @param mxx - the X coordinate scaling element of the 3x4 matrix
     * @param myx - the Y coordinate shearing element of the 3x4 matrix
     * @param mxy - the X coordinate shearing element of the 3x4 matrix
     * @param myy - the Y coordinate scaling element of the 3x4 matrix
     * @param mxt - the X coordinate translation element of the 3x4 matrix
     * @param myt - the Y coordinate translation element of the 3x4 matrix
     */
    public void setTransform(double mxx, double myx,
                             double mxy, double myy,
                             double mxt, double myt)
    {
        curState.transform.setTransform(mxx, myx,
                                        mxy, myy,
                                        mxt, myt);
        txdirty = true;
    }

    /**
     * Sets the current transform. Only 2D transforms are supported. The only
     * values used are the X and Y scaling, translation, and shearing components
     * of a transform.
     *
     * @param xform The affine to be copied and used as the current transform.
     */
    public void setTransform(Affine xform) {
        curState.transform.setTransform(xform.getMxx(), xform.getMyx(),
                                        xform.getMxy(), xform.getMyy(),
                                        xform.getTx(), xform.getTy());
        txdirty = true;
    }

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
    public Affine getTransform(Affine xform) {
        if (xform == null) {
            xform = new Affine();
        }

        xform.setMxx(curState.transform.getMxx());
        xform.setMxy(curState.transform.getMxy());
        xform.setMxz(0);
        xform.setTx(curState.transform.getMxt());
        xform.setMyx(curState.transform.getMyx());
        xform.setMyy(curState.transform.getMyy());
        xform.setMyz(0);
        xform.setTy(curState.transform.getMyt());
        xform.setMzx(0);
        xform.setMzy(0);
        xform.setMzz(1);
        xform.setTz(0);

        return xform;
    }

    /**
     * Returns a copy of the current transform.
     *
     * @return a copy of the transform of the current state.
     */
    public Affine getTransform() {
        return getTransform(null);
    }

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
    public void setGlobalAlpha(double alpha) {
        if (curState.globalAlpha != alpha) {
            curState.globalAlpha = alpha;
            alpha = (alpha > 1.0) ? 1.0 : (alpha < 0.0) ? 0.0 : alpha;
            writeParam(alpha, NGCanvas.GLOBAL_ALPHA);
        }
    }

    /**
     * Gets the current global alpha.
     * The default value is {@code 1.0}.
     * The global alpha is a <a href="#comm-attr">common attribute</a>
     * used for nearly all rendering methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return the current global alpha.
     */
    public double getGlobalAlpha() {
        return curState.globalAlpha;
    }

    /**
     * Sets the global blend mode.
     * The default value is {@link BlendMode#SRC_OVER SRC_OVER}.
     * A {@code null} value will be ignored and the current value will remain unchanged.
     * The blend mode is a <a href="#comm-attr">common attribute</a>
     * used for nearly all rendering methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param op the {@code BlendMode} that will be set or null.
     */
    public void setGlobalBlendMode(BlendMode op) {
        if (op != null && op != curState.blendop) {
            GrowableDataBuffer buf = getBuffer();
            curState.blendop = op;
            buf.putByte(NGCanvas.COMP_MODE);
            buf.putObject(EffectHelper.getToolkitBlendMode(op));
        }
    }

    /**
     * Gets the global blend mode.
     * The default value is {@link BlendMode#SRC_OVER SRC_OVER}.
     * The blend mode is a <a href="#comm-attr">common attribute</a>
     * used for nearly all rendering methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return the global {@code BlendMode} of the current state.
     */
    public BlendMode getGlobalBlendMode() {
        return curState.blendop;
    }

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
    public void setFill(Paint p) {
        if (p != null && curState.fill != p) {
            curState.fill = p;
            writePaint(p, NGCanvas.FILL_PAINT);
        }
    }

    /**
     * Gets the current fill paint attribute.
     * The default value is {@link Color#BLACK BLACK}.
     * The fill paint is a <a href="#fill-attr">fill attribute</a>
     * used for any of the fill methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return p The {@code Paint} to be used as the fill {@code Paint}.
     */
    public Paint getFill() {
        return curState.fill;
    }

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
    public void setStroke(Paint p) {
        if (p != null && curState.stroke != p) {
            curState.stroke = p;
            writePaint(p, NGCanvas.STROKE_PAINT);
        }
    }

    /**
     * Gets the current stroke.
     * The default value is {@link Color#BLACK BLACK}.
     * The stroke paint is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return the {@code Paint} to be used as the stroke {@code Paint}.
     */
    public Paint getStroke() {
        return curState.stroke;
    }

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
    public void setLineWidth(double lw) {
        // Per W3C spec: On setting, zero, negative, infinite, and NaN
        // values must be ignored, leaving the value unchanged
        if (lw > 0 && lw < Double.POSITIVE_INFINITY) {
            if (curState.linewidth != lw) {
                curState.linewidth = lw;
                writeParam(lw, NGCanvas.LINE_WIDTH);
            }
        }
    }

    /**
     * Gets the current line width.
     * The default value is {@code 1.0}.
     * The line width is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return value between 0 and infinity.
     */
    public double getLineWidth() {
        return curState.linewidth;
    }

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
    public void setLineCap(StrokeLineCap cap) {
        if (cap != null && curState.linecap != cap) {
            byte v;
            switch (cap) {
                case BUTT: v = NGCanvas.CAP_BUTT; break;
                case ROUND: v = NGCanvas.CAP_ROUND; break;
                case SQUARE: v = NGCanvas.CAP_SQUARE; break;
                default: return;
            }
            curState.linecap = cap;
            writeParam(v, NGCanvas.LINE_CAP);
        }
    }

    /**
     * Gets the current stroke line cap.
     * The default value is {@link StrokeLineCap#SQUARE SQUARE}.
     * The line cap is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return {@code StrokeLineCap} with a value of Butt, Round, or Square.
     */
    public StrokeLineCap getLineCap() {
        return curState.linecap;
    }

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
    public void setLineJoin(StrokeLineJoin join) {
        if (join != null && curState.linejoin != join) {
            byte v;
            switch (join) {
                case MITER: v = NGCanvas.JOIN_MITER; break;
                case BEVEL: v = NGCanvas.JOIN_BEVEL; break;
                case ROUND: v = NGCanvas.JOIN_ROUND; break;
                default: return;
            }
            curState.linejoin = join;
            writeParam(v, NGCanvas.LINE_JOIN);
        }
    }

    /**
     * Gets the current stroke line join.
     * The default value is {@link StrokeLineJoin#MITER}.
     * The line join is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return {@code StrokeLineJoin} with a value of Miter, Bevel, or Round.
     */
    public StrokeLineJoin getLineJoin() {
        return curState.linejoin;
    }

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
    public void setMiterLimit(double ml) {
        // Per W3C spec: On setting, zero, negative, infinite, and NaN
        // values must be ignored, leaving the value unchanged
        if (ml > 0.0 && ml < Double.POSITIVE_INFINITY) {
            if (curState.miterlimit != ml) {
                curState.miterlimit = ml;
                writeParam(ml, NGCanvas.MITER_LIMIT);
            }
        }
    }

    /**
     * Gets the current miter limit.
     * The default value is {@code 10.0}.
     * The miter limit is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return the miter limit value in the range {@code 0.0-positive infinity}
     */
    public double getMiterLimit() {
        return curState.miterlimit;
    }

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
     * @since JavaFX 8u40
     */
    public void setLineDashes(double... dashes) {
        if (dashes == null || dashes.length == 0) {
            if (curState.dashes == null) {
                return;
            }
            curState.dashes = null;
        } else {
            boolean allZeros = true;
            for (int i = 0; i < dashes.length; i++) {
                double d = dashes[i];
                if (d >= 0.0 && d < Double.POSITIVE_INFINITY) {
                    // Non-NaN, finite, non-negative
                    // Test cannot be inverted or it will not implicitly test for NaN
                    if (d > 0) {
                        allZeros = false;
                    }
                } else {
                    return;
                }
            }
            if (allZeros) {
                if (curState.dashes == null) {
                    return;
                }
                curState.dashes = null;
            } else {
                int dashlen = dashes.length;
                if ((dashlen & 1) == 0) {
                    curState.dashes = Arrays.copyOf(dashes, dashlen);
                } else {
                    curState.dashes = Arrays.copyOf(dashes, dashlen * 2);
                    System.arraycopy(dashes, 0, curState.dashes, dashlen, dashlen);
                }
            }
        }
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(NGCanvas.DASH_ARRAY);
        buf.putObject(curState.dashes);
    }

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
     * @since JavaFX 8u40
     */
    public double[] getLineDashes() {
        if (curState.dashes == null) {
            return null;
        }
        return Arrays.copyOf(curState.dashes, curState.dashes.length);
    }

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
     * @since JavaFX 8u40
     */
    public void setLineDashOffset(double dashOffset) {
        // Per W3C spec: On setting, infinite, and NaN
        // values must be ignored, leaving the value unchanged
        if (dashOffset > Double.NEGATIVE_INFINITY && dashOffset < Double.POSITIVE_INFINITY) {
            curState.dashOffset = dashOffset;
            writeParam(dashOffset, NGCanvas.DASH_OFFSET);
        }
    }

    /**
     * Gets the current line dash offset.
     * The default value is {@code 0.0}.
     * The line dash offset is a <a href="#strk-attr">stroke attribute</a>
     * used for any of the stroke methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return the line dash offset in the range {@code (-inf, +inf)}
     * @since JavaFX 8u40
     */
    public double getLineDashOffset() {
        return curState.dashOffset;
    }

    /**
     * Sets the current Font.
     * The default value is specified by {@link Font#getDefault()}.
     * The font is a <a href="#text-attr">text attribute</a>
     * used for any of the text methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * A {@code null} value will be ignored and the current value will remain unchanged.
     *
     * @param f the Font or null.
     */
    public void setFont(Font f) {
        if (f != null && curState.font != f) {
            curState.font = f;
            GrowableDataBuffer buf = getBuffer();
            buf.putByte(NGCanvas.FONT);
            buf.putObject(FontHelper.getNativeFont(f));
        }
    }

    /**
     * Gets the current Font.
     * The default value is specified by {@link Font#getDefault()}.
     * The font is a <a href="#text-attr">text attribute</a>
     * used for any of the text methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return the Font
     */
    public Font getFont() {
        return curState.font;
    }

    /**
     * Sets the current Font Smoothing Type.
     * The default value is {@link FontSmoothingType#GRAY GRAY}.
     * The font smoothing type is a <a href="#text-attr">text attribute</a>
     * used for any of the text methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * A {@code null} value will be ignored and the current value will remain unchanged.
     * <p>
     * <b>Note</b> that the {@code FontSmoothingType} value of
     * {@link FontSmoothingType#LCD LCD} is only supported over an opaque
     * background.  {@code LCD} text will generally appear as {@code GRAY}
     * text over transparent or partially transparent pixels, and in some
     * implementations it may not be supported at all on a {@link Canvas}
     * because the required support does not exist for surfaces which contain
     * an alpha channel as all {@code Canvas} objects do.
     *
     * @param fontsmoothing the {@link FontSmoothingType} or null
     * @since JavaFX 8u40
     */
    public void setFontSmoothingType(FontSmoothingType fontsmoothing) {
        if (fontsmoothing != null && fontsmoothing != curState.fontsmoothing) {
            curState.fontsmoothing = fontsmoothing;
            writeParam((byte) fontsmoothing.ordinal(), NGCanvas.FONT_SMOOTH);
        }
    }

    /**
     * Gets the current Font Smoothing Type.
     * The default value is {@link FontSmoothingType#GRAY GRAY}.
     * The font smoothing type is a <a href="#text-attr">text attribute</a>
     * used for any of the text methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return the {@link FontSmoothingType}
     * @since JavaFX 8u40
     */
    public FontSmoothingType getFontSmoothingType() {
        return curState.fontsmoothing;
    }

    /**
     * Defines horizontal text alignment, relative to the text {@code x} origin.
     * The default value is {@link TextAlignment#LEFT LEFT}.
     * The text alignment is a <a href="#text-attr">text attribute</a>
     * used for any of the text methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * <p>
     * Let horizontal bounds represent the logical width of a single line of
     * text. Where each line of text has a separate horizontal bounds.
     * <p>
     * Then TextAlignment is specified as:
     * <ul>
     * <li>Left: the left edge of the horizontal bounds will be at {@code x}.
     * <li>Center: the center, halfway between left and right edge, of the
     * horizontal bounds will be at {@code x}.
     * <li>Right: the right edge of the horizontal bounds will be at {@code x}.
     * </ul>
     * <p>
     *
     * Note: Canvas does not support line wrapping, therefore the text
     * alignment Justify is identical to left aligned text.
     * <p>
     * A {@code null} value will be ignored and the current value will remain unchanged.
     *
     * @param align {@code TextAlignment} with values of Left, Center, Right or null.
     */
    public void setTextAlign(TextAlignment align) {
        if (align != null && curState.textalign != align) {
            byte a;
            switch (align) {
                case LEFT: a = NGCanvas.ALIGN_LEFT; break;
                case CENTER: a = NGCanvas.ALIGN_CENTER; break;
                case RIGHT: a = NGCanvas.ALIGN_RIGHT; break;
                case JUSTIFY: a = NGCanvas.ALIGN_JUSTIFY; break;
                default: return;
            }
            curState.textalign = align;
            writeParam(a, NGCanvas.TEXT_ALIGN);
        }
    }

    /**
     * Gets the current {@code TextAlignment}.
     * The default value is {@link TextAlignment#LEFT LEFT}.
     * The text alignment is a <a href="#text-attr">text attribute</a>
     * used for any of the text methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return {@code TextAlignment} with values of Left, Center, Right, or
     * Justify.
     */
    public TextAlignment getTextAlign() {
        return curState.textalign;
    }

    /**
     * Sets the current Text Baseline.
     * The default value is {@link VPos#BASELINE BASELINE}.
     * The text baseline is a <a href="#text-attr">text attribute</a>
     * used for any of the text methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * A {@code null} value will be ignored and the current value will remain unchanged.
     *
     * @param baseline {@code VPos} with values of Top, Center, Baseline, or Bottom or null.
     */
    public void setTextBaseline(VPos baseline) {
        if (baseline != null && curState.textbaseline != baseline) {
            byte b;
            switch (baseline) {
                case TOP: b = NGCanvas.BASE_TOP; break;
                case CENTER: b = NGCanvas.BASE_MIDDLE; break;
                case BASELINE: b = NGCanvas.BASE_ALPHABETIC; break;
                case BOTTOM: b = NGCanvas.BASE_BOTTOM; break;
                default: return;
            }
            curState.textbaseline = baseline;
            writeParam(b, NGCanvas.TEXT_BASELINE);
        }
    }

    /**
     * Gets the current Text Baseline.
     * The default value is {@link VPos#BASELINE BASELINE}.
     * The text baseline is a <a href="#text-attr">text attribute</a>
     * used for any of the text methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @return {@code VPos} with values of Top, Center, Baseline, or Bottom
     */
    public VPos getTextBaseline() {
        return curState.textbaseline;
    }

    /**
     * Fills the given string of text at position x, y
     * with the current fill paint attribute.
     * A {@code null} text value will be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>,
     * <a href="#fill-attr">fill</a>,
     * or <a href="#text-attr">text</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
     *
     * @param text the string of text or null.
     * @param x position on the x axis.
     * @param y position on the y axis.
     */
    public void fillText(String text, double x, double y) {
        writeText(text, x, y, 0, NGCanvas.FILL_TEXT);
    }

    /**
     * Draws the given string of text at position x, y
     * with the current stroke paint attribute.
     * A {@code null} text value will be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>,
     * <a href="#strk-attr">stroke</a>,
     * or <a href="#text-attr">text</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
     *
     * @param text the string of text or null.
     * @param x position on the x axis.
     * @param y position on the y axis.
     */
    public void strokeText(String text, double x, double y) {
        writeText(text, x, y, 0, NGCanvas.STROKE_TEXT);
    }

    /**
     * Fills text and includes a maximum width of the string.
     * If the width of the text extends past max width, then it will be sized
     * to fit.
     * A {@code null} text value will be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>,
     * <a href="#fill-attr">fill</a>,
     * or <a href="#text-attr">text</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
     *
     * @param text the string of text or null.
     * @param x position on the x axis.
     * @param y position on the y axis.
     * @param maxWidth  maximum width the text string can have.
     */
    public void fillText(String text, double x, double y, double maxWidth) {
        if (maxWidth <= 0) return;
        writeText(text, x, y, maxWidth, NGCanvas.FILL_TEXT);
    }

    /**
     * Draws text with stroke paint and includes a maximum width of the string.
     * If the width of the text extends past max width, then it will be sized
     * to fit.
     * A {@code null} text value will be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>,
     * <a href="#strk-attr">stroke</a>,
     * or <a href="#text-attr">text</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
     *
     * @param text the string of text or null.
     * @param x position on the x axis.
     * @param y position on the y axis.
     * @param maxWidth  maximum width the text string can have.
     */
    public void strokeText(String text, double x, double y, double maxWidth) {
        if (maxWidth <= 0) return;
        writeText(text, x, y, maxWidth, NGCanvas.STROKE_TEXT);
    }


    /**
     * Set the filling rule attribute for determining the interior of paths
     * in fill or clip operations.
     * The default value is {@code FillRule.NON_ZERO}.
     * A {@code null} value will be ignored and the current value will remain unchanged.
     * The fill rule is a <a href="#path-attr">path attribute</a>
     * used for any of the fill or clip path methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param fillRule {@code FillRule} with a value of  Even_odd or Non_zero or null.
     */
     public void setFillRule(FillRule fillRule) {
         if (fillRule != null && curState.fillRule != fillRule) {
            byte b;
            if (fillRule == FillRule.EVEN_ODD) {
                b = NGCanvas.FILL_RULE_EVEN_ODD;
            } else {
                b = NGCanvas.FILL_RULE_NON_ZERO;
            }
            curState.fillRule = fillRule;
            writeParam(b, NGCanvas.FILL_RULE);
        }
     }

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
     public FillRule getFillRule() {
         return curState.fillRule;
     }

    /**
     * Resets the current path to empty.
     * The default path is empty.
     * The current path is a <a href="#path-attr">path attribute</a>
     * used for any of the path methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>
     * and <b>is not affected</b> by the {@link #save()} and
     * {@link #restore()} operations.
     */
    public void beginPath() {
        path.reset();
        markPathDirty();
    }

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
    public void moveTo(double x0, double y0) {
        coords[0] = (float) x0;
        coords[1] = (float) y0;
        curState.transform.transform(coords, 0, coords, 0, 1);
        path.moveTo(coords[0], coords[1]);
        markPathDirty();
    }

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
    public void lineTo(double x1, double y1) {
        coords[0] = (float) x1;
        coords[1] = (float) y1;
        curState.transform.transform(coords, 0, coords, 0, 1);
        if (path.getNumCommands() == 0) {
            path.moveTo(coords[0], coords[1]);
        }
        path.lineTo(coords[0], coords[1]);
        markPathDirty();
    }

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
    public void quadraticCurveTo(double xc, double yc, double x1, double y1) {
        coords[0] = (float) xc;
        coords[1] = (float) yc;
        coords[2] = (float) x1;
        coords[3] = (float) y1;
        curState.transform.transform(coords, 0, coords, 0, 2);
        if (path.getNumCommands() == 0) {
            path.moveTo(coords[0], coords[1]);
        }
        path.quadTo(coords[0], coords[1], coords[2], coords[3]);
        markPathDirty();
    }

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
    public void bezierCurveTo(double xc1, double yc1, double xc2, double yc2, double x1, double y1) {
        coords[0] = (float) xc1;
        coords[1] = (float) yc1;
        coords[2] = (float) xc2;
        coords[3] = (float) yc2;
        coords[4] = (float) x1;
        coords[5] = (float) y1;
        curState.transform.transform(coords, 0, coords, 0, 3);
        if (path.getNumCommands() == 0) {
            path.moveTo(coords[0], coords[1]);
        }
        path.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
        markPathDirty();
    }

    /**
     * Adds segments to the current path to make an arc.
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
     *
     * @param x1 the X coordinate of the first point of the arc.
     * @param y1 the Y coordinate of the first point of the arc.
     * @param x2 the X coordinate of the second point of the arc.
     * @param y2 the Y coordinate of the second point of the arc.
     * @param radius the radius of the arc in the range {0.0-positive infinity}.
     */
    public void arcTo(double x1, double y1, double x2, double y2, double radius) {
        if (path.getNumCommands() == 0) {
            moveTo(x1, y1);
            lineTo(x1, y1);
        } else if (!tryArcTo((float) x1, (float) y1, (float) x2, (float) y2,
                             (float) radius))
        {
            lineTo(x1, y1);
        }
    }

    private static double lenSq(double x0, double y0, double x1, double y1) {
        x1 -= x0;
        y1 -= y0;
        return x1 * x1 + y1 * y1;
    }

    private boolean tryArcTo(float x1, float y1, float x2, float y2, float radius) {
        float x0, y0;
        if (curState.transform.isTranslateOrIdentity()) {
            x0 = (float) (path.getCurrentX() - curState.transform.getMxt());
            y0 = (float) (path.getCurrentY() - curState.transform.getMyt());
        } else {
            coords[0] = path.getCurrentX();
            coords[1] = path.getCurrentY();
            try {
                curState.transform.inverseTransform(coords, 0, coords, 0, 1);
            } catch (NoninvertibleTransformException e) {
                return false;
            }
            x0 = coords[0];
            y0 = coords[1];
        }
        // call x1,y1 the corner point
        // If 2*theta is the angle described by p0->p1->p2
        // then theta is the angle described by p0->p1->centerpt and
        // centerpt->p1->p2
        // We know that the distance from the arc center to the tangent points
        // is r, and if A is the distance from the corner to the tangent point
        // then we know:
        // tan(theta) = r/A
        // A = r / sin(theta)
        // B = A * cos(theta) = r * (sin/cos) = r * tan
        // We use the cosine rule on the triangle to get the 2*theta angle:
        // cosB = (a^2 + c^2 - b^2) / (2ac)
        // where a and c are the adjacent sides and b is the opposite side
        // i.e. a = p0->p1, c=p1->p2, b=p0->p2
        // Then we can use the tan^2 identity to compute B:
        // tan^2 = (1 - cos(2theta)) / (1 + cos(2theta))
        double lsq01 = lenSq(x0, y0, x1, y1);
        double lsq12 = lenSq(x1, y1, x2, y2);
        double lsq02 = lenSq(x0, y0, x2, y2);
        double len01 = Math.sqrt(lsq01);
        double len12 = Math.sqrt(lsq12);
        double cosnum = lsq01 + lsq12 - lsq02;
        double cosden = 2.0 * len01 * len12;
        if (cosden == 0.0 || radius <= 0f) {
            return false;
        }
        double cos_2theta = cosnum / cosden;
        double tansq_den = (1.0 + cos_2theta);
        if (tansq_den == 0.0) {
            return false;
        }
        double tansq_theta = (1.0 - cos_2theta) / tansq_den;
        double A = radius / Math.sqrt(tansq_theta);
        double tx0 = x1 + (A / len01) * (x0 - x1);
        double ty0 = y1 + (A / len01) * (y0 - y1);
        double tx1 = x1 + (A / len12) * (x2 - x1);
        double ty1 = y1 + (A / len12) * (y2 - y1);
        // The midpoint between the two tangent points
        double mx = (tx0 + tx1) / 2.0;
        double my = (ty0 + ty1) / 2.0;
        // similar triangles tell us that:
        // len(m,center)/len(m,tangent) = len(m,tangent)/len(corner,m)
        // len(m,center) = lensq(m,tangent)/len(corner,m)
        // center = m + (m - p1) * len(m,center) / len(corner,m)
        //   = m + (m - p1) * (lensq(m,tangent) / lensq(corner,m))
        double lenratioden = lenSq(mx, my, x1, y1);
        if (lenratioden == 0.0) {
            return false;
        }
        double lenratio = lenSq(mx, my, tx0, ty0) / lenratioden;
        double cx = mx + (mx - x1) * lenratio;
        double cy = my + (my - y1) * lenratio;
        if (!(cx == cx && cy == cy)) {
            return false;
        }
        // Looks like we are good to draw, first we have to get to the
        // initial tangent point with a line segment.
        if (tx0 != x0 || ty0 != y0) {
            lineTo(tx0, ty0);
        }
        // We need sin(arc/2), cos(arc/2)
        // and possibly sin(arc/4), cos(arc/4) if we need 2 cubic beziers
        // We have tan(theta) = tan(tri/2)
        // arc = 180-tri
        // arc/2 = (180-tri)/2 = 90-(tri/2)
        // sin(arc/2) = sin(90-(tri/2)) = cos(tri/2)
        // cos(arc/2) = cos(90-(tri/2)) = sin(tri/2)
        // 2theta = tri, therefore theta = tri/2
        // cos(tri/2)^2 = (1+cos(tri)) / 2.0 = (1+cos_2theta)/2.0
        // sin(tri/2)^2 = (1-cos(tri)) / 2.0 = (1-cos_2theta)/2.0
        // sin(arc/2) = cos(tri/2) = sqrt((1+cos_2theta)/2.0)
        // cos(arc/2) = sin(tri/2) = sqrt((1-cos_2theta)/2.0)
        // We compute cos(arc/2) here as we need it in either case below
        double coshalfarc = Math.sqrt((1.0 - cos_2theta) / 2.0);
        boolean ccw = (ty0 - cy) * (tx1 - cx) > (ty1 - cy) * (tx0 - cx);
        // If the arc covers more than 90 degrees then we must use 2
        // cubic beziers to get a decent approximation.
        // arc = 180-tri
        // arc = 180-2*theta
        // arc > 90 implies 2*theta < 90
        // 2*theta < 90 implies cos_2theta > 0
        // So, we need 2 cubics if cos_2theta > 0
        if (cos_2theta <= 0.0) {
            // 1 cubic bezier
            double sinhalfarc = Math.sqrt((1.0 + cos_2theta) / 2.0);
            double cv = 4.0 / 3.0 * sinhalfarc / (1.0 + coshalfarc);
            if (ccw) cv = -cv;
            double cpx0 = tx0 - cv * (ty0 - cy);
            double cpy0 = ty0 + cv * (tx0 - cx);
            double cpx1 = tx1 + cv * (ty1 - cy);
            double cpy1 = ty1 - cv * (tx1 - cx);
            bezierCurveTo(cpx0, cpy0, cpx1, cpy1, tx1, ty1);
        } else {
            // 2 cubic beziers
            // We need sin(arc/4) and cos(arc/4)
            // We computed cos(arc/2), so we can compute them as follows:
            // sin(arc/4) = sqrt((1 - cos(arc/2)) / 2)
            // cos(arc/4) = sart((1 + cos(arc/2)) / 2)
            double sinqtrarc = Math.sqrt((1.0 - coshalfarc) / 2.0);
            double cosqtrarc = Math.sqrt((1.0 + coshalfarc) / 2.0);
            double cv = 4.0 / 3.0 * sinqtrarc / (1.0 + cosqtrarc);
            if (ccw) cv = -cv;
            double midratio = radius / Math.sqrt(lenratioden);
            double midarcx = cx + (x1 - mx) * midratio;
            double midarcy = cy + (y1 - my) * midratio;
            double cpx0 = tx0 - cv * (ty0 - cy);
            double cpy0 = ty0 + cv * (tx0 - cx);
            double cpx1 = midarcx + cv * (midarcy - cy);
            double cpy1 = midarcy - cv * (midarcx - cx);
            bezierCurveTo(cpx0, cpy0, cpx1, cpy1, midarcx, midarcy);
            cpx0 = midarcx - cv * (midarcy - cy);
            cpy0 = midarcy + cv * (midarcx - cx);
            cpx1 = tx1 + cv * (ty1 - cy);
            cpy1 = ty1 - cv * (tx1 - cx);
            bezierCurveTo(cpx0, cpy0, cpx1, cpy1, tx1, ty1);
        }
        return true;
    }

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
    public void arc(double centerX, double centerY,
                    double radiusX, double radiusY,
                    double startAngle, double length)
    {
        Arc2D arc = new Arc2D((float) (centerX - radiusX), // x
                              (float) (centerY - radiusY), // y
                              (float) (radiusX * 2.0), // w
                              (float) (radiusY * 2.0), // h
                              (float) startAngle,
                              (float) length,
                              Arc2D.OPEN);
        path.append(arc.getPathIterator(curState.transform), true);
        markPathDirty();
    }

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
    public void rect(double x, double y, double w, double h) {
        coords[0] = (float) x;
        coords[1] = (float) y;
        coords[2] = (float) w;
        coords[3] = (float) 0;
        coords[4] = (float) 0;
        coords[5] = (float) h;
        curState.transform.deltaTransform(coords, 0, coords, 0, 3);
        float x0 = coords[0] + (float) curState.transform.getMxt();
        float y0 = coords[1] + (float) curState.transform.getMyt();
        float dx1 = coords[2];
        float dy1 = coords[3];
        float dx2 = coords[4];
        float dy2 = coords[5];
        path.moveTo(x0, y0);
        path.lineTo(x0+dx1, y0+dy1);
        path.lineTo(x0+dx1+dx2, y0+dy1+dy2);
        path.lineTo(x0+dx2, y0+dy2);
        path.closePath();
        markPathDirty();
//        path.moveTo(x0, y0); // not needed, closepath leaves pen at moveto
    }

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
     *
     * @param svgpath the SVG Path string.
     */
    public void appendSVGPath(String svgpath) {
        if (svgpath == null) return;
        boolean prependMoveto = true;
        boolean skipMoveto = true;
        for (int i = 0; i < svgpath.length(); i++) {
            switch (svgpath.charAt(i)) {
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                    continue;
                case 'M':
                    prependMoveto = skipMoveto = false;
                    break;
                case 'm':
                    if (path.getNumCommands() == 0) {
                        // An initial relative moveTo becomes absolute
                        prependMoveto = false;
                    }
                    // Even if we prepend an initial moveTo in the temp
                    // path, we do not want to delete the resulting initial
                    // moveTo because the relative moveto will be folded
                    // into it by an optimization in the Path2D object.
                    skipMoveto = false;
                    break;
            }
            break;
        }
        Path2D p2d = new Path2D();
        if (prependMoveto && path.getNumCommands() > 0) {
            float x0, y0;
            if (curState.transform.isTranslateOrIdentity()) {
                x0 = (float) (path.getCurrentX() - curState.transform.getMxt());
                y0 = (float) (path.getCurrentY() - curState.transform.getMyt());
            } else {
                coords[0] = path.getCurrentX();
                coords[1] = path.getCurrentY();
                try {
                    curState.transform.inverseTransform(coords, 0, coords, 0, 1);
                } catch (NoninvertibleTransformException e) {
                }
                x0 = coords[0];
                y0 = coords[1];
            }
            p2d.moveTo(x0, y0);
        } else {
            skipMoveto = false;
        }
        try {
            p2d.appendSVGPath(svgpath);
            PathIterator pi = p2d.getPathIterator(curState.transform);
            if (skipMoveto) {
                // We need to delete the initial moveto and let the path
                // extend from the actual existing geometry.
                pi.next();
            }
            path.append(pi, false);
        } catch (IllegalArgumentException | IllegalPathStateException ex) {
            //Ignore incorrect path
        }
    }

    /**
     * Closes the path.
     * The current path is a <a href="#path-attr">path attribute</a>
     * used for any of the path methods as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>
     * and <b>is not affected</b> by the {@link #save()} and
     * {@link #restore()} operations.
     */
    public void closePath() {
        if (path.getNumCommands() > 0) {
            path.closePath();
            markPathDirty();
        }
    }

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
    public void fill() {
        writePath(NGCanvas.FILL_PATH);
    }

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
    public void stroke() {
        writePath(NGCanvas.STROKE_PATH);
    }

    /**
     * Intersects the current clip with the current path and applies it to
     * subsequent rendering operation as an anti-aliased mask.
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
     * affect at the time of the {@code stroke()} operation.
     * </p>
     */
    public void clip() {
        Path2D clip = new Path2D(path);
        clipStack.addLast(clip);
        curState.numClipPaths++;
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(NGCanvas.PUSH_CLIP);
        buf.putObject(clip);
    }

    /**
     * Returns true if the the given x,y point is inside the path.
     *
     * @param x the X coordinate to use for the check.
     * @param y the Y coordinate to use for the check.
     * @return true if the point given is inside the path, false
     * otherwise.
     */
    public boolean isPointInPath(double x, double y) {
        // TODO: HTML5 considers points on the path to be inside, but we
        // implement a halfin-halfout approach...
        return path.contains((float) x, (float) y);
    }

    /**
     * Clears a portion of the canvas with a transparent color value.
     * <p>
     * This method will be affected only by the current transform, clip,
     * and effect.
     * </p>
     *
     * @param x X position of the upper left corner of the rectangle.
     * @param y Y position of the upper left corner of the rectangle.
     * @param w width of the rectangle.
     * @param h height of the rectangle.
     */
    public void clearRect(double x, double y, double w, double h) {
        if (w != 0 && h != 0) {
            resetIfCovers(null, x, y, w, h);
            writeOp4(x, y, w, h, NGCanvas.CLEAR_RECT);
        }
    }

    /**
     * Fills a rectangle using the current fill paint.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#fill-attr">fill</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
     *
     * @param x the X position of the upper left corner of the rectangle.
     * @param y the Y position of the upper left corner of the rectangle.
     * @param w the width of the rectangle.
     * @param h the height of the rectangle.
     */
    public void fillRect(double x, double y, double w, double h) {
        if (w != 0 && h != 0) {
            resetIfCovers(this.curState.fill, x, y, w, h);
            writeOp4(x, y, w, h, NGCanvas.FILL_RECT);
        }
    }

    /**
     * Strokes a rectangle using the current stroke paint.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#strk-attr">stroke</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
     *
     * @param x the X position of the upper left corner of the rectangle.
     * @param y the Y position of the upper left corner of the rectangle.
     * @param w the width of the rectangle.
     * @param h the height of the rectangle.
     */
    public void strokeRect(double x, double y, double w, double h) {
        if (w != 0 || h != 0) {
            writeOp4(x, y, w, h, NGCanvas.STROKE_RECT);
        }
    }

    /**
     * Fills an oval using the current fill paint.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#fill-attr">fill</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
     *
     * @param x the X coordinate of the upper left bound of the oval.
     * @param y the Y coordinate of the upper left bound of the oval.
     * @param w the width at the center of the oval.
     * @param h the height at the center of the oval.
     */
    public void fillOval(double x, double y, double w, double h) {
        if (w != 0 && h != 0) {
            writeOp4(x, y, w, h, NGCanvas.FILL_OVAL);
        }
    }

    /**
     * Strokes an oval using the current stroke paint.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#strk-attr">stroke</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
     *
     * @param x the X coordinate of the upper left bound of the oval.
     * @param y the Y coordinate of the upper left bound of the oval.
     * @param w the width at the center of the oval.
     * @param h the height at the center of the oval.
     */
    public void strokeOval(double x, double y, double w, double h) {
        if (w != 0 || h != 0) {
            writeOp4(x, y, w, h, NGCanvas.STROKE_OVAL);
        }
    }

    /**
     * Fills an arc using the current fill paint. A {@code null} ArcType or
     * non positive width or height will cause the render command to be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#fill-attr">fill</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
     *
     * @param x the X coordinate of the arc.
     * @param y the Y coordinate of the arc.
     * @param w the width of the arc.
     * @param h the height of the arc.
     * @param startAngle the starting angle of the arc in degrees.
     * @param arcExtent the angular extent of the arc in degrees.
     * @param closure closure type (Round, Chord, Open) or null.
     */
    public void fillArc(double x, double y, double w, double h,
                        double startAngle, double arcExtent, ArcType closure)
    {
        if (w != 0 && h != 0 && closure != null) {
            writeArcType(closure);
            writeOp6(x, y, w, h, startAngle, arcExtent, NGCanvas.FILL_ARC);
        }
    }

    /**
     * Strokes an Arc using the current stroke paint. A {@code null} ArcType or
     * non positive width or height will cause the render command to be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#strk-attr">stroke</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
     *
     * @param x the X coordinate of the arc.
     * @param y the Y coordinate of the arc.
     * @param w the width of the arc.
     * @param h the height of the arc.
     * @param startAngle the starting angle of the arc in degrees.
     * @param arcExtent arcExtent the angular extent of the arc in degrees.
     * @param closure closure type (Round, Chord, Open) or null
     */
    public void strokeArc(double x, double y, double w, double h,
                        double startAngle, double arcExtent, ArcType closure)
    {
        if (w != 0 && h != 0 && closure != null) {
            writeArcType(closure);
            writeOp6(x, y, w, h, startAngle, arcExtent, NGCanvas.STROKE_ARC);
        }
    }

    /**
     * Fills a rounded rectangle using the current fill paint.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#fill-attr">fill</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
     *
     * @param x the X coordinate of the upper left bound of the oval.
     * @param y the Y coordinate of the upper left bound of the oval.
     * @param w the width at the center of the oval.
     * @param h the height at the center of the oval.
     * @param arcWidth the arc width of the rectangle corners.
     * @param arcHeight the arc height of the rectangle corners.
     */
    public void fillRoundRect(double x, double y, double w, double h,
                              double arcWidth, double arcHeight)
    {
        if (w != 0 && h != 0) {
            writeOp6(x, y, w, h, arcWidth, arcHeight, NGCanvas.FILL_ROUND_RECT);
        }
    }

    /**
     * Strokes a rounded rectangle using the current stroke paint.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#strk-attr">stroke</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
     *
     * @param x the X coordinate of the upper left bound of the oval.
     * @param y the Y coordinate of the upper left bound of the oval.
     * @param w the width at the center of the oval.
     * @param h the height at the center of the oval.
     * @param arcWidth the arc width of the rectangle corners.
     * @param arcHeight the arc height of the rectangle corners.
     */
    public void strokeRoundRect(double x, double y, double w, double h,
                              double arcWidth, double arcHeight)
    {
        if (w != 0 && h != 0) {
            writeOp6(x, y, w, h, arcWidth, arcHeight, NGCanvas.STROKE_ROUND_RECT);
        }
    }

    /**
     * Strokes a line using the current stroke paint.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#strk-attr">stroke</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
     *
     * @param x1 the X coordinate of the starting point of the line.
     * @param y1 the Y coordinate of the starting point of the line.
     * @param x2 the X coordinate of the ending point of the line.
     * @param y2 the Y coordinate of the ending point of the line.
     */
    public void strokeLine(double x1, double y1, double x2, double y2) {
        writeOp4(x1, y1, x2, y2, NGCanvas.STROKE_LINE);
    }

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
     * </p>
     *
     * @param xPoints array containing the x coordinates of the polygon's points or null.
     * @param yPoints array containing the y coordinates of the polygon's points or null.
     * @param nPoints the number of points that make the polygon.
     */
    public void fillPolygon(double xPoints[], double yPoints[], int nPoints) {
        if (nPoints >= 3) {
            writePoly(xPoints, yPoints, nPoints, true, NGCanvas.FILL_PATH);
        }
    }

    /**
     * Strokes a polygon with the given points using the currently set stroke paint.
     * A {@code null} value for any of the arrays will be ignored and nothing will be drawn.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * or <a href="#strk-attr">stroke</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
     *
     * @param xPoints array containing the x coordinates of the polygon's points or null.
     * @param yPoints array containing the y coordinates of the polygon's points or null.
     * @param nPoints the number of points that make the polygon.
     */
    public void strokePolygon(double xPoints[], double yPoints[], int nPoints) {
        if (nPoints >= 2) {
            writePoly(xPoints, yPoints, nPoints, true, NGCanvas.STROKE_PATH);
        }
    }

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
     * </p>
     *
     * @param xPoints array containing the x coordinates of the polyline's points or null.
     * @param yPoints array containing the y coordinates of the polyline's points or null.
     * @param nPoints the number of points that make the polyline.
     */
    public void strokePolyline(double xPoints[], double yPoints[], int nPoints) {
        if (nPoints >= 2) {
            writePoly(xPoints, yPoints, nPoints, false, NGCanvas.STROKE_PATH);
        }
    }

    /**
     * Draws an image at the given x, y position using the width
     * and height of the given image.
     * A {@code null} image value or an image still in progress will be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
     *
     * @param img the image to be drawn or null.
     * @param x the X coordinate on the destination for the upper left of the image.
     * @param y the Y coordinate on the destination for the upper left of the image.
     */
    public void drawImage(Image img, double x, double y) {
        if (img == null) return;
        double sw = img.getWidth();
        double sh = img.getHeight();
        writeImage(img, x, y, sw, sh);
    }

    /**
     * Draws an image into the given destination rectangle of the canvas. The
     * Image is scaled to fit into the destination rectangle.
     * A {@code null} image value or an image still in progress will be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
     *
     * @param img the image to be drawn or null.
     * @param x the X coordinate on the destination for the upper left of the image.
     * @param y the Y coordinate on the destination for the upper left of the image.
     * @param w the width of the destination rectangle.
     * @param h the height of the destination rectangle.
     */
    public void drawImage(Image img, double x, double y, double w, double h) {
        writeImage(img, x, y, w, h);
    }

    /**
     * Draws the specified source rectangle of the given image to the given
     * destination rectangle of the Canvas.
     * A {@code null} image value or an image still in progress will be ignored.
     * <p>
     * This method will be affected by any of the
     * <a href="#comm-attr">global common</a>
     * attributes as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     * </p>
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
    public void drawImage(Image img,
                          double sx, double sy, double sw, double sh,
                          double dx, double dy, double dw, double dh)
    {
        writeImage(img, dx, dy, dw, dh, sx, sy, sw, sh);
    }

    private PixelWriter writer;
    /**
     * Returns a {@link PixelWriter} object that can be used to modify
     * the pixels of the {@link Canvas} associated with this
     * {@code GraphicsContext}.
     * All coordinates in the {@code PixelWriter} methods on the returned
     * object will be in device space since they refer directly to pixels
     * and no other rendering attributes will be applied when modifying
     * pixels using this object.
     *
     * @return the {@code PixelWriter} for modifying the pixels of this
     *         {@code Canvas}
     */
    public PixelWriter getPixelWriter() {
        if (writer == null) {
            writer = new PixelWriter() {
                @Override
                public PixelFormat<ByteBuffer> getPixelFormat() {
                    return PixelFormat.getByteBgraPreInstance();
                }

                private BytePixelSetter getSetter() {
                    return ByteBgraPre.setter;
                }

                @Override
                public void setArgb(int x, int y, int argb) {
                    GrowableDataBuffer buf = getBuffer();
                    buf.putByte(NGCanvas.PUT_ARGB);
                    buf.putInt(x);
                    buf.putInt(y);
                    buf.putInt(argb);
                }

                @Override
                public void setColor(int x, int y, Color c) {
                    if (c == null) throw new NullPointerException("Color cannot be null");
                    int a = (int) Math.round(c.getOpacity() * 255.0);
                    int r = (int) Math.round(c.getRed() * 255.0);
                    int g = (int) Math.round(c.getGreen() * 255.0);
                    int b = (int) Math.round(c.getBlue() * 255.0);
                    setArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                }

                private void writePixelBuffer(int x, int y, int w, int h,
                                              byte[] pixels)
                {
                    GrowableDataBuffer buf = getBuffer();
                    buf.putByte(NGCanvas.PUT_ARGBPRE_BUF);
                    buf.putInt(x);
                    buf.putInt(y);
                    buf.putInt(w);
                    buf.putInt(h);
                    buf.putObject(pixels);
                }

                private int[] checkBounds(int x, int y, int w, int h,
                                          PixelFormat<? extends Buffer> pf,
                                          int scan)
                {
                    // assert (w >= 0 && h >= 0) - checked by caller
                    int cw = (int) Math.ceil(theCanvas.getWidth());
                    int ch = (int) Math.ceil(theCanvas.getHeight());
                    if (x >= 0 && y >= 0 && x+w <= cw && y+h <= ch) {
                        return null;
                    }
                    int offset = 0;
                    if (x < 0) {
                        w += x;
                        if (w < 0) return null;
                        if (pf != null) {
                            switch (pf.getType()) {
                                case BYTE_BGRA:
                                case BYTE_BGRA_PRE:
                                    offset -= x * 4;
                                    break;
                                case BYTE_RGB:
                                    offset -= x * 3;
                                    break;
                                case BYTE_INDEXED:
                                case INT_ARGB:
                                case INT_ARGB_PRE:
                                    offset -= x;
                                    break;
                                default:
                                    throw new InternalError("unknown Pixel Format");
                            }
                        }
                        x = 0;
                    }
                    if (y < 0) {
                        h += y;
                        if (h < 0) return null;
                        offset -= y * scan;
                        y = 0;
                    }
                    if (x + w > cw) {
                        w = cw - x;
                        if (w < 0) return null;
                    }
                    if (y + h > ch) {
                        h = ch - y;
                        if (h < 0) return null;
                    }
                    return new int[] {
                        x, y, w, h, offset
                    };
                }

                @Override
                public <T extends Buffer> void
                    setPixels(int x, int y, int w, int h,
                              PixelFormat<T> pixelformat,
                              T buffer, int scan)
                {
                    if (pixelformat == null) throw new NullPointerException("PixelFormat cannot be null");
                    if (buffer == null) throw new NullPointerException("Buffer cannot be null");
                    if (w <= 0 || h <= 0) return;
                    int offset = buffer.position();
                    int adjustments[] = checkBounds(x, y, w, h,
                                                    pixelformat, scan);
                    if (adjustments != null) {
                        x = adjustments[0];
                        y = adjustments[1];
                        w = adjustments[2];
                        h = adjustments[3];
                        offset += adjustments[4];
                    }

                    byte pixels[] = new byte[w * h * 4];
                    ByteBuffer dst = ByteBuffer.wrap(pixels);

                    PixelGetter<T> getter = PixelUtils.getGetter(pixelformat);
                    PixelConverter<T, ByteBuffer> converter =
                        PixelUtils.getConverter(getter, getSetter());
                    converter.convert(buffer, offset, scan,
                                      dst, 0, w * 4,
                                      w, h);
                    writePixelBuffer(x, y, w, h, pixels);
                }

                @Override
                public void setPixels(int x, int y, int w, int h,
                                      PixelFormat<ByteBuffer> pixelformat,
                                      byte[] buffer, int offset, int scanlineStride)
                {
                    if (pixelformat == null) throw new NullPointerException("PixelFormat cannot be null");
                    if (buffer == null) throw new NullPointerException("Buffer cannot be null");
                    if (w <= 0 || h <= 0) return;
                    int adjustments[] = checkBounds(x, y, w, h,
                                                    pixelformat, scanlineStride);
                    if (adjustments != null) {
                        x = adjustments[0];
                        y = adjustments[1];
                        w = adjustments[2];
                        h = adjustments[3];
                        offset += adjustments[4];
                    }

                    byte pixels[] = new byte[w * h * 4];

                    BytePixelGetter getter = PixelUtils.getByteGetter(pixelformat);
                    ByteToBytePixelConverter converter =
                        PixelUtils.getB2BConverter(getter, getSetter());
                    converter.convert(buffer, offset, scanlineStride,
                                      pixels, 0, w * 4,
                                      w, h);
                    writePixelBuffer(x, y, w, h, pixels);
                }

                @Override
                public void setPixels(int x, int y, int w, int h,
                                      PixelFormat<IntBuffer> pixelformat,
                                      int[] buffer, int offset, int scanlineStride)
                {
                    if (pixelformat == null) throw new NullPointerException("PixelFormat cannot be null");
                    if (buffer == null) throw new NullPointerException("Buffer cannot be null");
                    if (w <= 0 || h <= 0) return;
                    int adjustments[] = checkBounds(x, y, w, h,
                                                    pixelformat, scanlineStride);
                    if (adjustments != null) {
                        x = adjustments[0];
                        y = adjustments[1];
                        w = adjustments[2];
                        h = adjustments[3];
                        offset += adjustments[4];
                    }

                    byte pixels[] = new byte[w * h * 4];

                    IntPixelGetter getter = PixelUtils.getIntGetter(pixelformat);
                    IntToBytePixelConverter converter =
                        PixelUtils.getI2BConverter(getter, getSetter());
                    converter.convert(buffer, offset, scanlineStride,
                                      pixels, 0, w * 4,
                                      w, h);
                    writePixelBuffer(x, y, w, h, pixels);
                }

                @Override
                public void setPixels(int dstx, int dsty, int w, int h,
                                      PixelReader reader, int srcx, int srcy)
                {
                    if (reader == null) throw new NullPointerException("Reader cannot be null");
                    if (w <= 0 || h <= 0) return;
                    int adjustments[] = checkBounds(dstx, dsty, w, h, null, 0);
                    if (adjustments != null) {
                        int newx = adjustments[0];
                        int newy = adjustments[1];
                        srcx += newx - dstx;
                        srcy += newy - dsty;
                        dstx = newx;
                        dsty = newy;
                        w = adjustments[2];
                        h = adjustments[3];
                    }

                    byte pixels[] = new byte[w * h * 4];
                    reader.getPixels(srcx, srcy, w, h,
                                     PixelFormat.getByteBgraPreInstance(),
                                     pixels, 0, w * 4);
                    writePixelBuffer(dstx, dsty, w, h, pixels);
                }
            };
        }
        return writer;
    }

    /**
     * Sets the effect to be applied after the next draw call, or null to
     * disable effects.
     * The current effect is a <a href="#comm-attr">common attribute</a>
     * used for nearly all rendering operations as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param e the effect to use, or null to disable effects
     */
    public void setEffect(Effect e) {
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(NGCanvas.EFFECT);
        if (e == null) {
            curState.effect = null;
            buf.putObject(null);
        } else {
            curState.effect = EffectHelper.copy(e);
            EffectHelper.sync(curState.effect);
            buf.putObject(EffectHelper.getPeer(curState.effect));
        }
    }

    /**
     * Gets a copy of the effect to be applied after the next draw call.
     * A null return value means that no effect will be applied after subsequent
     * rendering calls.
     * The current effect is a <a href="#comm-attr">common attribute</a>
     * used for nearly all rendering operations as specified in the
     * <a href="#attr-ops-table">Rendering Attributes Table</a>.
     *
     * @param e an {@code Effect} object that may be used to store the
     *        copy of the current effect, if it is of a compatible type
     * @return the current effect used for all rendering calls,
     *         or null if there is no current effect
     */
    public Effect getEffect(Effect e) {
        return curState.effect == null ? null : EffectHelper.copy(curState.effect);
    }

    /**
     * Applies the given effect to the entire bounds of the canvas and stores
     * the result back into the same canvas.
     * A {@code null} value will be ignored.
     * The effect will be applied without any other rendering attributes and
     * under an Identity coordinate transform.
     * Since the effect is applied to the entire bounds of the canvas, some
     * effects may have a confusing result, such as a Reflection effect
     * that will apply its reflection off of the bottom of the canvas even if
     * only a portion of the canvas has been rendered to and will not be
     * visible unless a negative offset is used to bring the reflection back
     * into view.
     *
     * @param e the effect to apply onto the entire destination or null.
     */
    public void applyEffect(Effect e) {
        if (e == null) return;
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(NGCanvas.FX_APPLY_EFFECT);
        Effect effect = EffectHelper.copy(e);
        EffectHelper.sync(effect);
        buf.putObject(EffectHelper.getPeer(effect));
    }
}
