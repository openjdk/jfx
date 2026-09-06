/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Effect;
import javafx.scene.image.DrawingContext;
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
 * <tr class="rowColor">
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
 * <tr><th colspan="3" scope="row"><a id="image-attr">Image Attributes</a></th></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:15%">{@link #setImageSmoothing(boolean) Image Smoothing}</th>
 * <td class="colLast" style="width:10%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:10%; text-align:center">{@code true}</td>
 * <td class="colLast">
 * A boolean state which enables or disables image smoothing for
 * {@link #drawImage(javafx.scene.image.Image, double, double) drawImage(all forms)}.
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
 * <th scope="col" class="colLast" style="width:22%">Method</th>
 * <th scope="col" class="colLast" style="width:13%; text-align:center"><a href="#comm-attr">Common Rendering Attributes</a></th>
 * <th scope="col" class="colLast" style="width:13%; text-align:center"><a href="#fill-attr">Fill Attributes</a></th>
 * <th scope="col" class="colLast" style="width:13%; text-align:center"><a href="#strk-attr">Stroke Attributes</a></th>
 * <th scope="col" class="colLast" style="width:13%; text-align:center"><a href="#text-attr">Text Attributes</a></th>
 * <th scope="col" class="colLast" style="width:13%; text-align:center"><a href="#path-attr">Path Attributes</a></th>
 * <th scope="col" class="colLast" style="width:13%; text-align:center"><a href="#image-attr">Image Attributes</a></th>
 * </tr>
 * <tr><th colspan="1" scope="row">Basic Shape Rendering</th></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:22%">
 * {@link #fillRect(double, double, double, double) fillRect()},
 * {@link #fillRoundRect(double, double, double, double, double, double) fillRoundRect()},
 * {@link #fillOval(double, double, double, double) fillOval()},
 * {@link #fillArc(double, double, double, double, double, double, javafx.scene.shape.ArcType) fillArc()}
 * </th>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:22%">
 * {@link #strokeLine(double, double, double, double) strokeLine()},
 * {@link #strokeRect(double, double, double, double) strokeRect()},
 * {@link #strokeRoundRect(double, double, double, double, double, double) strokeRoundRect()},
 * {@link #strokeOval(double, double, double, double) strokeOval()},
 * {@link #strokeArc(double, double, double, double, double, double, javafx.scene.shape.ArcType) strokeArc()}
 * </th>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:22%">
 * {@link #clearRect(double, double, double, double) clearRect()}
 * </th>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#base-fn-1">[1]</a></td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:22%">
 * {@link #fillPolygon(double[], double[], int) fillPolygon()}
 * </th>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#base-fn-2">[2]</a></td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:22%">
 * {@link #strokePolygon(double[], double[], int) strokePolygon()},
 * {@link #strokePolyline(double[], double[], int) strokePolyline()}
 * </th>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr><th scope="row" colspan="6">
 * <a id="base-fn-1">[1]</a> Only the Transform, Clip, and Effect apply to clearRect()<br>
 * <a id="base-fn-2">[2]</a> Only the Fill Rule applies to fillPolygon(), the current path is left unchanged
 * </th></tr>
 * <tr><th colspan="1" scope="row">Text Rendering</th></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:22%">
 * {@link #fillText(java.lang.String, double, double) fillText()},
 * {@link #fillText(java.lang.String, double, double, double) fillText(with maxWidth)}
 * </th>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#text-fn-3">[3]</a></td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:22%">
 * {@link #strokeText(java.lang.String, double, double) strokeText()},
 * {@link #strokeText(java.lang.String, double, double, double) strokeText(with maxWidth)}
 * </th>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#text-fn-3">[3]</a></td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr><th scope="row" colspan="6">
 * <a id="text-fn-3">[3]</a> The Font Smoothing attribute only applies to filled text
 * </th></tr>
 * <tr><th colspan="1" scope="row">Path Rendering</th></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:22%">
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
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#path-fn-4">[4]</a></td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:22%">
 * {@link #fill() fill()}
 * </th>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#path-fn-4">[4]</a></td>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:22%">
 * {@link #stroke() stroke()}
 * </th>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#path-fn-4">[4]</a></td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#path-fn-5">[5]</a></td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr class="altColor">
 * <th scope="row" class="colLast" style="width:22%">
 * {@link #clip() clip()}
 * </th>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * </tr>
 * <tr><th scope="row" colspan="6">
 * <a id="path-fn-4">[4]</a> Transform applied only during path construction<br>
 * <a id="path-fn-5">[5]</a> Fill Rule only used for fill() and clip()
 * </th></tr>
 * <tr><th scope="row" colspan="1">Image Rendering</th></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:22%">
 * {@link #drawImage(javafx.scene.image.Image, double, double) drawImage(all forms)}
 * </th>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * </tr>
 * <tr><th scope="row" colspan="1">Miscellaneous</th></tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:22%">
 * {@link #applyEffect(javafx.scene.effect.Effect) applyEffect()},
 * {@link #getPixelWriter() PixelWriter methods}
 * </th>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
 * <td class="colLast" style="width:13%; text-align:center; color:#c00">No</td>
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
 * <p>This class implements {@link javafx.scene.image.DrawingContext}, the
 * interface shared with the drawing context obtained from a
 * {@link javafx.scene.image.WritableImage}.</p>
 *
 * @see Canvas
 * @see DrawingContext
 * @since JavaFX 2.2
 */
public final class GraphicsContext implements DrawingContext {
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
        this.stateStack = new LinkedList<>();
        this.clipStack = new LinkedList<>();
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
        boolean imageSmoothing = true;

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
                null, FillRule.NON_ZERO, true);
        }

        State(State copy) {
            set(copy.globalAlpha, copy.blendop,
                new Affine2D(copy.transform),
                copy.fill, copy.stroke,
                copy.linewidth, copy.linecap, copy.linejoin, copy.miterlimit,
                copy.dashes, copy.dashOffset,
                copy.numClipPaths,
                copy.font, copy.fontsmoothing, copy.textalign, copy.textbaseline,
                copy.effect, copy.fillRule, copy.imageSmoothing);
        }

        final void set(double globalAlpha, BlendMode blendop,
                       Affine2D transform, Paint fill, Paint stroke,
                       double linewidth, StrokeLineCap linecap,
                       StrokeLineJoin linejoin, double miterlimit,
                       double dashes[], double dashOffset,
                       int numClipPaths,
                       Font font, FontSmoothingType smoothing,
                       TextAlignment align, VPos baseline,
                       Effect effect, FillRule fillRule, boolean imageSmoothing)
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
            this.imageSmoothing = imageSmoothing;
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
            ctx.setImageSmoothing(imageSmoothing);
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

    private static void writeRectParams(GrowableDataBuffer buf,
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

    @Override
    public void save() {
        stateStack.push(curState.copy());
    }

    @Override
    public void restore() {
        if (!stateStack.isEmpty()) {
            State savedState = stateStack.pop();
            savedState.restore(this);
            txdirty = true;
        }
    }

    @Override
    public void translate(double x, double y) {
        curState.transform.translate(x, y);
        txdirty = true;
    }

    @Override
    public void scale(double x, double y) {
        curState.transform.scale(x, y);
        txdirty = true;
    }

    @Override
    public void rotate(double degrees) {
        curState.transform.rotate(Math.toRadians(degrees));
        txdirty = true;
    }

    @Override
    public void transform(double mxx, double myx,
                          double mxy, double myy,
                          double mxt, double myt)
    {
        curState.transform.concatenate(mxx, mxy, mxt,
                                       myx, myy, myt);
        txdirty = true;
    }

    @Override
    public void transform(Affine xform) {
        if (xform == null) return;
        curState.transform.concatenate(xform.getMxx(), xform.getMxy(), xform.getTx(),
                                       xform.getMyx(), xform.getMyy(), xform.getTy());
        txdirty = true;
    }

    @Override
    public void setTransform(double mxx, double myx,
                             double mxy, double myy,
                             double mxt, double myt)
    {
        curState.transform.setTransform(mxx, myx,
                                        mxy, myy,
                                        mxt, myt);
        txdirty = true;
    }

    @Override
    public void setTransform(Affine xform) {
        if (xform == null) {
            return;
        }

        curState.transform.setTransform(xform.getMxx(), xform.getMyx(),
                                        xform.getMxy(), xform.getMyy(),
                                        xform.getTx(), xform.getTy());
        txdirty = true;
    }

    @Override
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

    @Override
    public Affine getTransform() {
        return getTransform(null);
    }

    @Override
    public void setGlobalAlpha(double alpha) {
        if (curState.globalAlpha != alpha) {
            curState.globalAlpha = alpha;
            alpha = (alpha > 1.0) ? 1.0 : (alpha < 0.0) ? 0.0 : alpha;
            writeParam(alpha, NGCanvas.GLOBAL_ALPHA);
        }
    }

    @Override
    public double getGlobalAlpha() {
        return curState.globalAlpha;
    }

    @Override
    public void setGlobalBlendMode(BlendMode op) {
        if (op != null && op != curState.blendop) {
            GrowableDataBuffer buf = getBuffer();
            curState.blendop = op;
            buf.putByte(NGCanvas.COMP_MODE);
            buf.putObject(EffectHelper.getToolkitBlendMode(op));
        }
    }

    @Override
    public BlendMode getGlobalBlendMode() {
        return curState.blendop;
    }

    @Override
    public void setFill(Paint p) {
        if (p != null && curState.fill != p) {
            curState.fill = p;
            writePaint(p, NGCanvas.FILL_PAINT);
        }
    }

    @Override
    public Paint getFill() {
        return curState.fill;
    }

    @Override
    public void setStroke(Paint p) {
        if (p != null && curState.stroke != p) {
            curState.stroke = p;
            writePaint(p, NGCanvas.STROKE_PAINT);
        }
    }

    @Override
    public Paint getStroke() {
        return curState.stroke;
    }

    @Override
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

    @Override
    public double getLineWidth() {
        return curState.linewidth;
    }

    @Override
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

    @Override
    public StrokeLineCap getLineCap() {
        return curState.linecap;
    }

    @Override
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

    @Override
    public StrokeLineJoin getLineJoin() {
        return curState.linejoin;
    }

    @Override
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

    @Override
    public double getMiterLimit() {
        return curState.miterlimit;
    }

    /**
     * {@inheritDoc}
     *
     * @since JavaFX 8u40
     */
    @Override
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
     * {@inheritDoc}
     *
     * @since JavaFX 8u40
     */
    @Override
    public double[] getLineDashes() {
        if (curState.dashes == null) {
            return null;
        }
        return Arrays.copyOf(curState.dashes, curState.dashes.length);
    }

    /**
     * {@inheritDoc}
     *
     * @since JavaFX 8u40
     */
    @Override
    public void setLineDashOffset(double dashOffset) {
        // Per W3C spec: On setting, infinite, and NaN
        // values must be ignored, leaving the value unchanged
        if (dashOffset > Double.NEGATIVE_INFINITY && dashOffset < Double.POSITIVE_INFINITY) {
            curState.dashOffset = dashOffset;
            writeParam(dashOffset, NGCanvas.DASH_OFFSET);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since JavaFX 8u40
     */
    @Override
    public double getLineDashOffset() {
        return curState.dashOffset;
    }

    @Override
    public void setFont(Font f) {
        if (f != null && curState.font != f) {
            curState.font = f;
            GrowableDataBuffer buf = getBuffer();
            buf.putByte(NGCanvas.FONT);
            buf.putObject(FontHelper.getNativeFont(f));
        }
    }

    @Override
    public Font getFont() {
        return curState.font;
    }

    /**
     * {@inheritDoc}
     *
     * @since JavaFX 8u40
     */
    @Override
    public void setFontSmoothingType(FontSmoothingType fontsmoothing) {
        if (fontsmoothing != null && fontsmoothing != curState.fontsmoothing) {
            curState.fontsmoothing = fontsmoothing;
            writeParam((byte) fontsmoothing.ordinal(), NGCanvas.FONT_SMOOTH);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since JavaFX 8u40
     */
    @Override
    public FontSmoothingType getFontSmoothingType() {
        return curState.fontsmoothing;
    }

    @Override
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

    @Override
    public TextAlignment getTextAlign() {
        return curState.textalign;
    }

    @Override
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

    @Override
    public VPos getTextBaseline() {
        return curState.textbaseline;
    }

    @Override
    public void fillText(String text, double x, double y) {
        writeText(text, x, y, 0, NGCanvas.FILL_TEXT);
    }

    @Override
    public void strokeText(String text, double x, double y) {
        writeText(text, x, y, 0, NGCanvas.STROKE_TEXT);
    }

    @Override
    public void fillText(String text, double x, double y, double maxWidth) {
        if (maxWidth <= 0) return;
        writeText(text, x, y, maxWidth, NGCanvas.FILL_TEXT);
    }

    @Override
    public void strokeText(String text, double x, double y, double maxWidth) {
        if (maxWidth <= 0) return;
        writeText(text, x, y, maxWidth, NGCanvas.STROKE_TEXT);
    }


    @Override
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

    @Override
    public FillRule getFillRule() {
        return curState.fillRule;
    }

    /**
     * {@inheritDoc}
     *
     * @since 12
     */
    @Override
    public void setImageSmoothing(boolean imageSmoothing) {
        if (curState.imageSmoothing != imageSmoothing) {
            curState.imageSmoothing = imageSmoothing;
            GrowableDataBuffer buf = getBuffer();
            buf.putByte(NGCanvas.IMAGE_SMOOTH);
            buf.putBoolean(curState.imageSmoothing);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 12
     */
    @Override
    public boolean isImageSmoothing() {
        return curState.imageSmoothing;
    }

    @Override
    public void beginPath() {
        path.reset();
        markPathDirty();
    }

    @Override
    public void moveTo(double x0, double y0) {
        coords[0] = (float) x0;
        coords[1] = (float) y0;
        curState.transform.transform(coords, 0, coords, 0, 1);
        path.moveTo(coords[0], coords[1]);
        markPathDirty();
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public void arcTo(double x1, double y1, double x2, double y2, double radius) {
        if (path.getNumCommands() == 0) {
            moveTo(x1, y1);
            lineTo(x1, y1);
        }
        else {
            try {
                path.arcTo(curState.transform, (float) x1, (float) y1, (float) x2, (float) y2, (float) radius);
                markPathDirty();
            }
            catch (IllegalPathStateException | NoninvertibleTransformException e) {
                lineTo(x1, y1);
            }
        }
    }

    @Override
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

    @Override
    public void rect(double x, double y, double w, double h) {
        addRect(path, x, y, w, h);
        markPathDirty();
//        path.moveTo(x0, y0); // not needed, closepath leaves pen at moveto
    }

    @Override
    public void appendSVGPath(String svgpath) {
        if (svgpath == null) return;

        try {
            path.appendSVGPath(curState.transform, svgpath);
            markPathDirty();
        }
        catch (IllegalArgumentException | IllegalPathStateException | NoninvertibleTransformException e) {
            //Ignore incorrect path
        }
    }

    @Override
    public void closePath() {
        if (path.getNumCommands() > 0) {
            path.closePath();
            markPathDirty();
        }
    }

    @Override
    public void fill() {
        writePath(NGCanvas.FILL_PATH);
    }

    @Override
    public void stroke() {
        writePath(NGCanvas.STROKE_PATH);
    }

    @Override
    public void clip() {
        Path2D clip = new Path2D(path);

        pushClip(clip);
    }

    /**
     * {@inheritDoc}
     *
     * @since 28
     */
    @Override
    public void clipRect(double x, double y, double w, double h) {
        Path2D clip = new Path2D();

        addRect(clip, x, y, w, h);
        pushClip(clip);
    }

    private void addRect(Path2D target, double x, double y, double w, double h) {
        coords[0] = (float) x;
        coords[1] = (float) y;
        coords[2] = (float) w;
        coords[3] = 0;
        coords[4] = 0;
        coords[5] = (float) h;

        curState.transform.deltaTransform(coords, 0, coords, 0, 3);

        float x0 = coords[0] + (float) curState.transform.getMxt();
        float y0 = coords[1] + (float) curState.transform.getMyt();
        float dx1 = coords[2];
        float dy1 = coords[3];
        float dx2 = coords[4];
        float dy2 = coords[5];

        target.moveTo(x0, y0);
        target.lineTo(x0+dx1, y0+dy1);
        target.lineTo(x0+dx1+dx2, y0+dy1+dy2);
        target.lineTo(x0+dx2, y0+dy2);
        target.closePath();
    }

    private void pushClip(Path2D clip) {
        clipStack.addLast(clip);
        curState.numClipPaths++;
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(NGCanvas.PUSH_CLIP);
        buf.putObject(clip);
    }

    @Override
    public boolean isPointInPath(double x, double y) {

        /*
         * Tests the point against the region the current path would fill. The
         * path is treated as an infinitely thin boundary, so the stroke
         * attributes do not matter. A point lying exactly on the boundary is
         * reported per the fill's half-open rasterization convention (the low
         * edges and corner count as inside, the high ones as outside), rather
         * than treated as inside the way HTML5 does.
         */

        return path.contains((float) x, (float) y);
    }

    @Override
    public void clearRect(double x, double y, double w, double h) {
        if (w != 0 && h != 0) {
            resetIfCovers(null, x, y, w, h);
            writeOp4(x, y, w, h, NGCanvas.CLEAR_RECT);
        }
    }

    @Override
    public void fillRect(double x, double y, double w, double h) {
        if (w != 0 && h != 0) {
            resetIfCovers(this.curState.fill, x, y, w, h);
            writeOp4(x, y, w, h, NGCanvas.FILL_RECT);
        }
    }

    @Override
    public void strokeRect(double x, double y, double w, double h) {
        if (w != 0 || h != 0) {
            writeOp4(x, y, w, h, NGCanvas.STROKE_RECT);
        }
    }

    @Override
    public void fillOval(double x, double y, double w, double h) {
        if (w != 0 && h != 0) {
            writeOp4(x, y, w, h, NGCanvas.FILL_OVAL);
        }
    }

    @Override
    public void strokeOval(double x, double y, double w, double h) {
        if (w != 0 || h != 0) {
            writeOp4(x, y, w, h, NGCanvas.STROKE_OVAL);
        }
    }

    @Override
    public void fillArc(double x, double y, double w, double h,
                        double startAngle, double arcExtent, ArcType closure)
    {
        if (w != 0 && h != 0 && closure != null) {
            writeArcType(closure);
            writeOp6(x, y, w, h, startAngle, arcExtent, NGCanvas.FILL_ARC);
        }
    }

    @Override
    public void strokeArc(double x, double y, double w, double h,
                        double startAngle, double arcExtent, ArcType closure)
    {
        if (w != 0 && h != 0 && closure != null) {
            writeArcType(closure);
            writeOp6(x, y, w, h, startAngle, arcExtent, NGCanvas.STROKE_ARC);
        }
    }

    @Override
    public void fillRoundRect(double x, double y, double w, double h,
                              double arcWidth, double arcHeight)
    {
        if (w != 0 && h != 0) {
            writeOp6(x, y, w, h, arcWidth, arcHeight, NGCanvas.FILL_ROUND_RECT);
        }
    }

    @Override
    public void strokeRoundRect(double x, double y, double w, double h,
                              double arcWidth, double arcHeight)
    {
        if (w != 0 && h != 0) {
            writeOp6(x, y, w, h, arcWidth, arcHeight, NGCanvas.STROKE_ROUND_RECT);
        }
    }

    @Override
    public void strokeLine(double x1, double y1, double x2, double y2) {
        writeOp4(x1, y1, x2, y2, NGCanvas.STROKE_LINE);
    }

    @Override
    public void fillPolygon(double xPoints[], double yPoints[], int nPoints) {
        if (nPoints >= 3) {
            writePoly(xPoints, yPoints, nPoints, true, NGCanvas.FILL_PATH);
        }
    }

    @Override
    public void strokePolygon(double xPoints[], double yPoints[], int nPoints) {
        if (nPoints >= 2) {
            writePoly(xPoints, yPoints, nPoints, true, NGCanvas.STROKE_PATH);
        }
    }

    @Override
    public void strokePolyline(double xPoints[], double yPoints[], int nPoints) {
        if (nPoints >= 2) {
            writePoly(xPoints, yPoints, nPoints, false, NGCanvas.STROKE_PATH);
        }
    }

    @Override
    public void drawImage(Image img, double x, double y) {
        if (img == null) return;
        double sw = img.getWidth();
        double sh = img.getHeight();
        writeImage(img, x, y, sw, sh);
    }

    @Override
    public void drawImage(Image img, double x, double y, double w, double h) {
        writeImage(img, x, y, w, h);
    }

    @Override
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
