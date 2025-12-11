/*
 * Copyright (c) 2012, 2025, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Interface providing basic drawing operations. An instance of this interface
 * is provided by {@link WritableImage} and {@link Canvas} via {@link WritableImage#getDrawingContext()}
 * and {@link Canvas#getGraphicsContext2D()}.
 * <p>
 * The provider of this interface may be associated with a {@link Node} which may
 * be attached to a {@link Scene}. If the associated node is not attached to any scene,
 * then the operations provided here can be used from any thread, as long as it is only
 * used from one thread at a time. Once the node is attached to a scene, the operations must
 * be called from the JavaFX Application Thread.
 * <p>
 * TODO A {@code DrawingContext} also manages a stack of state objects that can
 * be saved or restored at anytime.
 * <p>
 * The {@code DrawingContext} maintains the following rendering attributes
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
 * <tr><th colspan="3" scope="row"><a id="path-attr">Path Attributes</a></th></tr>
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
 * The various rendering methods on the {@code DrawingContext} use the
 * following sets of rendering attributes:
 * </a>
 * <table class="overviewSummary" style="width:80%; margin-left:auto; margin-right:auto">
 * <caption>Rendering Attributes Table</caption>
 * <tr>
 * <th scope="col" class="colLast" style="width:22%">Method</th>
 * <th scope="col" class="colLast" style="width:13%; text-align:center"><a href="#comm-attr">Common Rendering Attributes</a></th>
 * <th scope="col" class="colLast" style="width:13%; text-align:center"><a href="#fill-attr">Fill Attributes</a></th>
 * <th scope="col" class="colLast" style="width:13%; text-align:center"><a href="#strk-attr">Stroke Attributes</a></th>
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
 * </tr>
 * <tr class="rowColor">
 * <th scope="row" class="colLast" style="width:22%">
 * {@link #clearRect(double, double, double, double) clearRect()}
 * </th>
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
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
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes <a href="#base-fn-2">[1]</a></td>
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
 * </tr>
 * <tr><th scope="row" colspan="6">
 * <a id="base-fn-2">[1]</a> Only the Fill Rule applies to fillPolygon()
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
 * <td class="colLast" style="width:13%; text-align:center; color:#0c0">Yes</td>
 * </tr>
 * </table>
 *
 * <p>Example:</p>
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
 * @see Canvas
 * @see WritableImage
 * @since 26
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
  void strokeLine(double x1, double y1, double x2, double y2);

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
  void strokeRect(double x, double y, double w, double h);

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
  void clearRect(double x, double y, double w, double h);

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
  void fillRect(double x, double y, double w, double h);

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
  void strokeRoundRect(double x, double y, double w, double h, double arcWidth, double arcHeight);

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
  void fillRoundRect(double x, double y, double w, double h, double arcWidth, double arcHeight);

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
  void strokeOval(double x, double y, double w, double h);

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
  void fillOval(double x, double y, double w, double h);

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
  void strokeArc(double x, double y, double w, double h, double startAngle, double arcExtent, ArcType closure);

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
   * </p>
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
   * </p>
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
   * </p>
   *
   * @param xPoints array containing the x coordinates of the polygon's points or null.
   * @param yPoints array containing the y coordinates of the polygon's points or null.
   * @param nPoints the number of points that make the polygon.
   */
  void fillPolygon(double xPoints[], double yPoints[], int nPoints);

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
   * </p>
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
   * Draws an image into the given destination rectangle of the canvas. The
   * Image is scaled to fit into the destination rectangle.
   * A {@code null} image value or an image still in progress will be ignored.
   * <p>
   * This method will be affected by any of the
   * <a href="#comm-attr">global common</a>
   * or <a href="#image-attr">image</a>
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
  default void drawImage(Image img, double x, double y, double w, double h) {
    if (img == null || img.getProgress() < 1.0) {
      return;
    }

    drawImage(img, 0, 0, img.getWidth(), img.getHeight(), x, y, w, h);
  }

  /**
   * Draws the specified source rectangle of the given image to the given
   * destination rectangle of the Canvas.
   * A {@code null} image value or an image still in progress will be ignored.
   * <p>
   * This method will be affected by any of the
   * <a href="#comm-attr">global common</a>
   * or <a href="#image-attr">image</a>
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
  void drawImage(
    Image img,
    double sx, double sy, double sw, double sh,
    double dx, double dy, double dw, double dh
  );

}
