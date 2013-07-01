/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.util.tess;

import com.sun.prism.util.tess.impl.tess.TessellatorImpl;


// TODO: RT-17486 - Need a replacement for GLU Tessellator

 /**
  * Provides access to the OpenGL Utility Library (Tess).
  * 
  * <P>
  *
  * Notes from the Reference Implementation for this class:
  * Thanks to the contributions of many individuals, this class is a
  * pure Java port of SGI's original C sources. All of the projection,
  * mipmap, scaling, and tessellation routines that are exposed are
  * compatible with the GLU 1.3 specification. The GLU NURBS routines
  * are not currently exposed.
  */
public class Tess
{ 
    // TODO: Temporary constants needed to isolate jogl
    public static final int GL_POINTS = 0; // GL.GL_POINTS;
    public static final int GL_LINES = 1; //GL.GL_LINES;
    public static final int GL_LINE_LOOP = 2; //GL.GL_LINE_LOOP;
    public static final int GL_LINE_STRIP = 3; //GL.GL_LINE_STRIP;
    public static final int GL_TRIANGLES = 4; //GL.GL_TRIANGLES;
    public static final int GL_TRIANGLE_STRIP = 5; //GL.GL_TRIANGLE_STRIP;
    public static final int GL_TRIANGLE_FAN = 6; //GL.GL_TRIANGLE_FAN;

  
  //----------------------------------------------------------------------
  // Tessellation routines
  //
  
  /*****************************************************************************
   * <b>newTess</b> creates and returns a new tessellation object.  This
   * object must be referred to when calling tesselation methods.  A return
   * value of null means that there was not enough memeory to allocate the
   * object.
   *
   * Optional, throws GLException if not available in profile
   *
   * @return A new tessellation object.
   *
   * @see #tessBeginPolygon tessBeginPolygon
   * @see #deleteTess       deleteTess
   * @see #tessCallback     tessCallback
   ****************************************************************************/
  public static final Tessellator newTess() {
      return TessellatorImpl.gluNewTess();
  }

  /*****************************************************************************
   * <b>deleteTess</b> destroys the indicated tessellation object (which was
   * created with {@link #newTess newTess}).
   *
   * Optional, throws GLException if not available in profile
   *
   * @param tessellator
   *        Specifies the tessellation object to destroy.
   *
   * @see #beginPolygon beginPolygon
   * @see #newTess      newTess
   * @see #tessCallback tessCallback
   ****************************************************************************/
  public static final void deleteTess(Tessellator tessellator) {
      TessellatorImpl tess = (TessellatorImpl) tessellator;
      tess.gluDeleteTess();
  }

  /*****************************************************************************
   * <b>tessProperty</b> is used to control properites stored in a
   * tessellation object.  These properties affect the way that the polygons are
   * interpreted and rendered.  The legal value for <i>which</i> are as
   * follows:<P>
   *
   * Optional, throws GLException if not available in profile
   *
   * <b>TESS_WINDING_RULE</b>
   * <UL>
   *   Determines which parts of the polygon are on the "interior".
   *   <em>value</em> may be set to one of
   *   <BR><b>TESS_WINDING_ODD</b>,
   *   <BR><b>TESS_WINDING_NONZERO</b>,
   *   <BR><b>TESS_WINDING_POSITIVE</b>, or
   *   <BR><b>TESS_WINDING_NEGATIVE</b>, or
   *   <BR><b>TESS_WINDING_ABS_GEQ_TWO</b>.<P>
   *
   *   To understand how the winding rule works, consider that the input
   *   contours partition the plane into regions.  The winding rule determines
   *   which of these regions are inside the polygon.<P>
   *
   *   For a single contour C, the winding number of a point x is simply the
   *   signed number of revolutions we make around x as we travel once around C
   *   (where CCW is positive).  When there are several contours, the individual
   *   winding numbers are summed.  This procedure associates a signed integer
   *   value with each point x in the plane.  Note that the winding number is
   *   the same for all points in a single region.<P>
   *
   *   The winding rule classifies a region as "inside" if its winding number
   *   belongs to the chosen category (odd, nonzero, positive, negative, or
   *   absolute value of at least two).  The previous Tess tessellator (prior to
   *   Tess 1.2) used the "odd" rule.  The "nonzero" rule is another common way
   *   to define the interior.  The other three rules are useful for polygon CSG
   *   operations.
   * </UL>
   * <BR><b>TESS_BOUNDARY_ONLY</b>
   * <UL>
   *   Is a boolean value ("value" should be set to GL_TRUE or GL_FALSE). When
   *   set to GL_TRUE, a set of closed contours separating the polygon interior
   *   and exterior are returned instead of a tessellation.  Exterior contours
   *   are oriented CCW with respect to the normal; interior contours are
   *   oriented CW. The <b>TESS_BEGIN</b> and <b>TESS_BEGIN_DATA</b>
   *   callbacks use the type GL_LINE_LOOP for each contour.
   * </UL>
   * <BR><b>TESS_TOLERANCE</b>
   * <UL>
   *   Specifies a tolerance for merging features to reduce the size of the
   *   output. For example, two vertices that are very close to each other
   *   might be replaced by a single vertex.  The tolerance is multiplied by the
   *   largest coordinate magnitude of any input vertex; this specifies the
   *   maximum distance that any feature can move as the result of a single
   *   merge operation.  If a single feature takes part in several merge
   *   operations, the toal distance moved could be larger.<P>
   *
   *   Feature merging is completely optional; the tolerance is only a hint.
   *   The implementation is free to merge in some cases and not in others, or
   *   to never merge features at all.  The initial tolerance is 0.<P>
   *
   *   The current implementation merges vertices only if they are exactly
   *   coincident, regardless of the current tolerance.  A vertex is spliced
   *   into an edge only if the implementation is unable to distinguish which
   *   side of the edge the vertex lies on.  Two edges are merged only when both
   *   endpoints are identical.
   * </UL>
   *
   * @param tessellator
   *        Specifies the tessellation object created with
   *        {@link #newTess newTess}
   * @param which
   *        Specifies the property to be set.  Valid values are
   *        <b>TESS_WINDING_RULE</b>, <b>GLU_TESS_BOUNDARDY_ONLY</b>,
   *        <b>TESS_TOLERANCE</b>.
   * @param value
   *        Specifices the value of the indicated property.
   *
   * @see #gluGetTessProperty gluGetTessProperty
   * @see #newTess         newTess
   ****************************************************************************/
  public static final void tessProperty(Tessellator tessellator, int which, double value) {
      TessellatorImpl tess = (TessellatorImpl) tessellator;
      tess.gluTessProperty(which, value);
  }

  /*****************************************************************************
   * <b>gluGetTessProperty</b> retrieves properties stored in a tessellation
   * object.  These properties affect the way that tessellation objects are
   * interpreted and rendered.  See the
   * {@link #tessProperty tessProperty} reference
   * page for information about the properties and what they do.
   *
   * Optional, throws GLException if not available in profile
   *
   * @param tessellator
   *        Specifies the tessellation object (created with
   *        {@link #newTess newTess}).
   * @param which
   *        Specifies the property whose value is to be fetched. Valid values
   *        are <b>TESS_WINDING_RULE</b>, <b>TESS_BOUNDARY_ONLY</b>,
   *        and <b>GLU_TESS_TOLERANCES</b>.
   * @param value
   *        Specifices an array into which the value of the named property is
   *        written.
   *
   * @see #newTess      newTess
   * @see #tessProperty tessProperty
   ****************************************************************************/
  public static final void gluGetTessProperty(Tessellator tessellator, int which, double[] value, int value_offset) {
      TessellatorImpl tess = (TessellatorImpl) tessellator;
      tess.gluGetTessProperty(which, value, value_offset);
  }

  /*****************************************************************************
   * <b>tessNormal</b> describes a normal for a polygon that the program is
   * defining. All input data will be projected onto a plane perpendicular to
   * the one of the three coordinate axes before tessellation and all output
   * triangles will be oriented CCW with repsect to the normal (CW orientation
   * can be obtained by reversing the sign of the supplied normal).  For
   * example, if you know that all polygons lie in the x-y plane, call
   * <b>tessNormal</b>(tess, 0.0, 0.0, 0.0) before rendering any polygons.<P>
   *
   * If the supplied normal is (0.0, 0.0, 0.0)(the initial value), the normal
   * is determined as follows.  The direction of the normal, up to its sign, is
   * found by fitting a plane to the vertices, without regard to how the
   * vertices are connected.  It is expected that the input data lies
   * approximately in the plane; otherwise, projection perpendicular to one of
   * the three coordinate axes may substantially change the geometry.  The sign
   * of the normal is chosen so that the sum of the signed areas of all input
   * contours is nonnegative (where a CCW contour has positive area).<P>
   *
   * The supplied normal persists until it is changed by another call to
   * <b>tessNormal</b>.
   *
   * Optional, throws GLException if not available in profile
   *
   * @param tessellator
   *        Specifies the tessellation object (created by
   *        {@link #newTess newTess}).
   * @param x
   *        Specifies the first component of the normal.
   * @param y
   *        Specifies the second component of the normal.
   * @param z
   *        Specifies the third component of the normal.
   *
   * @see #tessBeginPolygon tessBeginPolygon
   * @see #tessEndPolygon   tessEndPolygon
   ****************************************************************************/
  public static final void tessNormal(Tessellator tessellator, double x, double y, double z) {
      TessellatorImpl tess = (TessellatorImpl) tessellator;
      tess.gluTessNormal(x, y, z);
  }

  /*****************************************************************************
   * <b>tessCallback</b> is used to indicate a callback to be used by a
   * tessellation object. If the specified callback is already defined, then it
   * is replaced. If <i>aCallback</i> is null, then the existing callback
   * becomes undefined.<P>
   *
   * Optional, throws GLException if not available in profile
   *
   * These callbacks are used by the tessellation object to describe how a
   * polygon specified by the user is broken into triangles. Note that there are
   * two versions of each callback: one with user-specified polygon data and one
   * without. If both versions of a particular callback are specified, then the
   * callback with user-specified polygon data will be used. Note that the
   * polygonData parameter used by some of the methods is a copy of the
   * reference that was specified when
   * {@link #tessBeginPolygon tessBeginPolygon}
   * was called. The legal callbacks are as follows:<P>
   *
   * <b>TESS_BEGIN</b>
   * <UL>
   *   The begin callback is invoked like {@link com.sun.prism.opengl.GL#glBegin
   *   glBegin} to indicate the start of a (triangle) primitive. The method
   *   takes a single argument of type int. If the
   *   <b>TESS_BOUNDARY_ONLY</b> property is set to <b>GL_FALSE</b>, then
   *   the argument is set to either <b>GL_TRIANGLE_FAN</b>,
   *   <b>GL_TRIANGLE_STRIP</b>, or <b>GL_TRIANGLES</b>. If the
   *   <b>TESS_BOUNDARY_ONLY</b> property is set to <b>GL_TRUE</b>, then the
   *   argument will be set to <b>GL_LINE_LOOP</b>. The method prototype for
   *   this callback is:
   * </UL>
   *
   * <PRE>
   *         void begin(int type);</PRE><P>
   *
   * <b>TESS_BEGIN_DATA</b>
   * <UL>
   *   The same as the <b>TESS_BEGIN</b> callback except
   *   that it takes an additional reference argument. This reference is
   *   identical to the opaque reference provided when
   *   {@link #tessBeginPolygon tessBeginPolygon}
   *   was called. The method prototype for this callback is:
   * </UL>
   *
   * <PRE>
   *         void beginData(int type, Object polygonData);</PRE>
   *
   * <b>TESS_EDGE_FLAG</b>
   * <UL>
   *   The edge flag callback is similar to
   *   {@link com.sun.prism.opengl.GL#glEdgeFlag glEdgeFlag}. The method takes
   *   a single boolean boundaryEdge that indicates which edges lie on the
   *   polygon boundary. If the boundaryEdge is <b>GL_TRUE</b>, then each vertex
   *   that follows begins an edge that lies on the polygon boundary, that is,
   *   an edge that separates an interior region from an exterior one. If the
   *   boundaryEdge is <b>GL_FALSE</b>, then each vertex that follows begins an
   *   edge that lies in the polygon interior. The edge flag callback (if
   *   defined) is invoked before the first vertex callback.<P>
   *
   *   Since triangle fans and triangle strips do not support edge flags, the
   *   begin callback is not called with <b>GL_TRIANGLE_FAN</b> or
   *   <b>GL_TRIANGLE_STRIP</b> if a non-null edge flag callback is provided.
   *   (If the callback is initialized to null, there is no impact on
   *   performance). Instead, the fans and strips are converted to independent
   *   triangles. The method prototype for this callback is:
   * </UL>
   *
   * <PRE>
   *         void edgeFlag(boolean boundaryEdge);</PRE>
   *
   * <b>TESS_EDGE_FLAG_DATA</b>
   * <UL>
   *   The same as the <b>TESS_EDGE_FLAG</b> callback except that it takes
   *   an additional reference argument. This reference is identical to the
   *   opaque reference provided when
   *   {@link #tessBeginPolygon tessBeginPolygon}
   *   was called. The method prototype for this callback is:
   * </UL>
   *
   * <PRE>
   *         void edgeFlagData(boolean boundaryEdge, Object polygonData);</PRE>
   *
   * <b>TESS_VERTEX</b>
   * <UL>
   *   The vertex callback is invoked between the begin and end callbacks. It is
   *   similar to {@link com.sun.prism.opengl.GL#glVertex3f glVertex3f}, and it
   *   defines the vertices of the triangles created by the tessellation
   *   process. The method takes a reference as its only argument. This
   *   reference is identical to the opaque reference provided by the user when
   *   the vertex was described (see
   *   {@link #tessVertex tessVertex}). The method
   *   prototype for this callback is:
   * </UL>
   *
   * <PRE>
   *         void vertex(Object vertexData);</PRE>
   *
   * <b>TESS_VERTEX_DATA</b>
   * <UL>
   *   The same as the <b>TESS_VERTEX</b> callback except that it takes an
   *   additional reference argument. This reference is identical to the opaque
   *   reference provided when
   *   {@link #tessBeginPolygon tessBeginPolygon}
   *   was called. The method prototype for this callback is:
   * </UL>
   *
   * <PRE>
   *         void vertexData(Object vertexData, Object polygonData);</PRE>
   *
   * <b>TESS_END</b>
   * <UL>
   *   The end callback serves the same purpose as
   *   {@link com.sun.prism.opengl.GL#glEnd glEnd}. It indicates the end of a
   *   primitive and it takes no arguments. The method prototype for this
   *   callback is:
   * </UL>
   *
   * <PRE>
   *         void end();</PRE>
   *
   * <b>TESS_END_DATA</b>
   * <UL>
   *   The same as the <b>TESS_END</b> callback except that it takes an
   *   additional reference argument. This reference is identical to the opaque
   *   reference provided when
   *   {@link #tessBeginPolygon tessBeginPolygon}
   *   was called. The method prototype for this callback is:
   * </UL>
   *
   * <PRE>
   *         void endData(Object polygonData);</PRE>
   *
   * <b>TESS_COMBINE</b>
   * <UL>
   *   The combine callback is called to create a new vertex when the
   *   tessellation detects an intersection, or wishes to merge features. The
   *   method takes four arguments: an array of three elements each of type
   *   double, an array of four references, an array of four elements each of
   *   type float, and a reference to a reference. The prototype is:
   * </UL>
   *
   * <PRE>
   *         void combine(double[] coords, Object[] data,
   *                      float[] weight, Object[] outData);</PRE>
   *
   * <UL>
   *   The vertex is defined as a linear combination of up to four existing
   *   vertices, stored in <i>data</i>. The coefficients of the linear
   *   combination are given by <i>weight</i>; these weights always add up to 1.
   *   All vertex pointers are valid even when some of the weights are 0.
   *   <i>coords</i> gives the location of the new vertex.<P>
   *
   *   The user must allocate another vertex, interpolate parameters using
   *   <i>data</i> and <i>weight</i>, and return the new vertex pointer
   *   in <i>outData</i>. This handle is supplied during rendering callbacks.
   *   The user is responsible for freeing the memory some time after
   *   {@link #tessEndPolygon tessEndPolygon} is
   *   called.<P>
   *
   *   For example, if the polygon lies in an arbitrary plane in 3-space, and a
   *   color is associated with each vertex, the <b>TESS_COMBINE</b>
   *   callback might look like this:
   * </UL>
   * <PRE>
   *         void myCombine(double[] coords, Object[] data,
   *                        float[] weight, Object[] outData)
   *         {
   *            MyVertex newVertex = new MyVertex();
   *
   *            newVertex.x = coords[0];
   *            newVertex.y = coords[1];
   *            newVertex.z = coords[2];
   *            newVertex.r = weight[0]*data[0].r +
   *                          weight[1]*data[1].r +
   *                          weight[2]*data[2].r +
   *                          weight[3]*data[3].r;
   *            newVertex.g = weight[0]*data[0].g +
   *                          weight[1]*data[1].g +
   *                          weight[2]*data[2].g +
   *                          weight[3]*data[3].g;
   *            newVertex.b = weight[0]*data[0].b +
   *                          weight[1]*data[1].b +
   *                          weight[2]*data[2].b +
   *                          weight[3]*data[3].b;
   *            newVertex.a = weight[0]*data[0].a +
   *                          weight[1]*data[1].a +
   *                          weight[2]*data[2].a +
   *                          weight[3]*data[3].a;
   *            outData = newVertex;
   *         }</PRE>
   *
   * <UL>
   *   If the tessellation detects an intersection, then the
   *   <b>TESS_COMBINE</b> or <b>TESS_COMBINE_DATA</b> callback (see
   *   below) must be defined, and it must write a non-null reference into
   *   <i>outData</i>. Otherwise the <b>TESS_NEED_COMBINE_CALLBACK</b> error
   *   occurs, and no output is generated.
   * </UL>
   *
   * <b>TESS_COMBINE_DATA</b>
   * <UL>
   *   The same as the <b>TESS_COMBINE</b> callback except that it takes an
   *   additional reference argument. This reference is identical to the opaque
   *   reference provided when
   *   {@link #tessBeginPolygon tessBeginPolygon}
   *   was called. The method prototype for this callback is:
   * </UL>
   *
   * <PRE>
   *         void combineData(double[] coords, Object[] data,
                              float[] weight, Object[] outData,
                              Object polygonData);</PRE>
   *
   * <b>TESS_ERROR</b>
   * <UL>
   *   The error callback is called when an error is encountered. The one
   *   argument is of type int; it indicates the specific error that occurred
   *   and will be set to one of <b>TESS_MISSING_BEGIN_POLYGON</b>,
   *   <b>TESS_MISSING_END_POLYGON</b>,
   *   <b>TESS_MISSING_BEGIN_CONTOUR</b>,
   *   <b>TESS_MISSING_END_CONTOUR</b>, <b>TESS_COORD_TOO_LARGE</b>,
   *   <b>TESS_NEED_COMBINE_CALLBACK</b> or <b>OUT_OF_MEMORY</b>.
   *   Character strings describing these errors can be retrieved with the
   *   {@link #gluErrorString gluErrorString} call. The
   *   method prototype for this callback is:
   * </UL>
   *
   * <PRE>
   *         void error(int errnum);</PRE>
   *
   * <UL>
   *   The Tess library will recover from the first four errors by inserting the
   *   missing call(s). <b>TESS_COORD_TOO_LARGE</b> indicates that some
   *   vertex coordinate exceeded the predefined constant
   *   <b>TESS_MAX_COORD</b> in absolute value, and that the value has been
   *   clamped. (Coordinate values must be small enough so that two can be
   *   multiplied together without overflow.)
   *   <b>TESS_NEED_COMBINE_CALLBACK</b> indicates that the tessellation
   *   detected an intersection between two edges in the input data, and the
   *   <b>TESS_COMBINE</b> or <b>TESS_COMBINE_DATA</b> callback was not
   *   provided. No output is generated. <b>OUT_OF_MEMORY</b> indicates that
   *   there is not enough memory so no output is generated.
   * </UL>
   *
   * <b>TESS_ERROR_DATA</b>
   * <UL>
   *   The same as the TESS_ERROR callback except that it takes an
   *   additional reference argument. This reference is identical to the opaque
   *   reference provided when
   *   {@link #tessBeginPolygon tessBeginPolygon}
   *   was called. The method prototype for this callback is:
   * </UL>
   *
   * <PRE>
   *         void errorData(int errnum, Object polygonData);</PRE>
   *
   * @param tessellator
   *        Specifies the tessellation object (created with
   *        {@link #newTess newTess}).
   * @param which
   *        Specifies the callback being defined. The following values are
   *        valid: <b>TESS_BEGIN</b>, <b>TESS_BEGIN_DATA</b>,
   *        <b>TESS_EDGE_FLAG</b>, <b>TESS_EDGE_FLAG_DATA</b>,
   *        <b>TESS_VERTEX</b>, <b>TESS_VERTEX_DATA</b>,
   *        <b>TESS_END</b>, <b>TESS_END_DATA</b>,
   *        <b>TESS_COMBINE</b>,  <b>TESS_COMBINE_DATA</b>,
   *        <b>TESS_ERROR</b>, and <b>TESS_ERROR_DATA</b>.
   * @param aCallback
   *        Specifies the callback object to be called.
   *
   * @see com.sun.prism.opengl.GL#glBegin              glBegin
   * @see com.sun.prism.opengl.GL#glEdgeFlag           glEdgeFlag
   * @see com.sun.prism.opengl.GL#glVertex3f           glVertex3f
   * @see #newTess          newTess
   * @see #gluErrorString      gluErrorString
   * @see #tessVertex       tessVertex
   * @see #tessBeginPolygon tessBeginPolygon
   * @see #tessBeginContour tessBeginContour
   * @see #tessProperty     tessProperty
   * @see #tessNormal       tessNormal
   ****************************************************************************/
  public static final void tessCallback(Tessellator tessellator, int which, TessellatorCallback aCallback) {
      TessellatorImpl tess = (TessellatorImpl) tessellator;
      tess.gluTessCallback(which, aCallback);
  }

  /*****************************************************************************
   * <b>tessVertex</b> describes a vertex on a polygon that the program
   * defines. Successive <b>tessVertex</b> calls describe a closed contour.
   * For example, to describe a quadrilateral <b>tessVertex</b> should be
   * called four times. <b>tessVertex</b> can only be called between
   * {@link #tessBeginContour tessBeginContour} and
   * {@link #tessBeginContour tessEndContour}.<P>
   *
   * Optional, throws GLException if not available in profile
   *
   * <b>data</b> normally references to a structure containing the vertex
   * location, as well as other per-vertex attributes such as color and normal.
   * This reference is passed back to the user through the
   * <b>TESS_VERTEX</b> or <b>TESS_VERTEX_DATA</b> callback after
   * tessellation (see the {@link #tessCallback
   * tessCallback} reference page).
   *
   * @param tessellator
   *        Specifies the tessellation object (created with
   *        {@link #newTess newTess}).
   * @param coords
   *        Specifies the coordinates of the vertex.
   * @param data
   *        Specifies an opaque reference passed back to the program with the
   *        vertex callback (as specified by
   *        {@link #tessCallback tessCallback}).
   *
   * @see #tessBeginPolygon tessBeginPolygon
   * @see #newTess          newTess
   * @see #tessBeginContour tessBeginContour
   * @see #tessCallback     tessCallback
   * @see #tessProperty     tessProperty
   * @see #tessNormal       tessNormal
   * @see #tessEndPolygon   tessEndPolygon
   ****************************************************************************/
  public static final void tessVertex(Tessellator tessellator, double[] coords, int coords_offset, Object data) {
      TessellatorImpl tess = (TessellatorImpl) tessellator;
      tess.gluTessVertex(coords, coords_offset, data);
  }

  /*****************************************************************************
   * <b>tessBeginPolygon</b> and
   * {@link #tessEndPolygon tessEndPolygon} delimit
   * the definition of a convex, concave or self-intersecting polygon. Within
   * each <b>tessBeginPolygon</b>/
   * {@link #tessEndPolygon tessEndPolygon} pair,
   * there must be one or more calls to
   * {@link #tessBeginContour tessBeginContour}/
   * {@link #tessEndContour tessEndContour}. Within
   * each contour, there are zero or more calls to
   * {@link #tessVertex tessVertex}. The vertices
   * specify a closed contour (the last vertex of each contour is automatically
   * linked to the first). See the {@link #tessVertex
   * tessVertex}, {@link #tessBeginContour
   * tessBeginContour}, and {@link #tessEndContour
   * tessEndContour} reference pages for more details.<P>
   *
   * Optional, throws GLException if not available in profile
   *
   * <b>data</b> is a reference to a user-defined data structure. If the
   * appropriate callback(s) are specified (see
   * {@link #tessCallback tessCallback}), then this
   * reference is returned to the callback method(s). Thus, it is a convenient
   * way to store per-polygon information.<P>
   *
   * Once {@link #tessEndPolygon tessEndPolygon} is
   * called, the polygon is tessellated, and the resulting triangles are
   * described through callbacks. See
   * {@link #tessCallback tessCallback} for
   * descriptions of the callback methods.
   *
   * @param tessellator
   *        Specifies the tessellation object (created with
   *        {@link #newTess newTess}).
   * @param data
   *        Specifies a reference to user polygon data.
   *
   * @see #newTess          newTess
   * @see #tessBeginContour tessBeginContour
   * @see #tessVertex       tessVertex
   * @see #tessCallback     tessCallback
   * @see #tessProperty     tessProperty
   * @see #tessNormal       tessNormal
   * @see #tessEndPolygon   tessEndPolygon
   ****************************************************************************/
  public static final void tessBeginPolygon(Tessellator tessellator, Object data) {
      TessellatorImpl tess = (TessellatorImpl) tessellator;
      tess.gluTessBeginPolygon(data);
  }

  /*****************************************************************************
   * <b>tessBeginContour</b> and
   * {@link #tessEndContour tessEndContour} delimit
   * the definition of a polygon contour. Within each
   * <b>tessBeginContour</b>/
   * {@link #tessEndContour tessEndContour} pair,
   * there can be zero or more calls to
   * {@link #tessVertex tessVertex}. The vertices
   * specify a closed contour (the last vertex of each contour is automatically
   * linked to the first). See the {@link #tessVertex
   * tessVertex} reference page for more details. <b>tessBeginContour</b>
   * can only be called between
   * {@link #tessBeginPolygon tessBeginPolygon} and
   * {@link #tessEndPolygon tessEndPolygon}.
   *
   * Optional, throws GLException if not available in profile
   *
   * @param tessellator
   *        Specifies the tessellation object (created with
   *        {@link #newTess newTess}).
   *
   * @see #newTess          newTess
   * @see #tessBeginPolygon tessBeginPolygon
   * @see #tessVertex       tessVertex
   * @see #tessCallback     tessCallback
   * @see #tessProperty     tessProperty
   * @see #tessNormal       tessNormal
   * @see #tessEndPolygon   tessEndPolygon
   ****************************************************************************/
  public static final void tessBeginContour(Tessellator tessellator) {
      TessellatorImpl tess = (TessellatorImpl) tessellator;
      tess.gluTessBeginContour();
  }

  /*****************************************************************************
   *  <b>tessEndContour</b> and
   * {@link #tessBeginContour tessBeginContour}
   * delimit the definition of a polygon contour. Within each
   * {@link #tessBeginContour tessBeginContour}/
   * <b>tessEndContour</b> pair, there can be zero or more calls to
   * {@link #tessVertex tessVertex}. The vertices
   * specify a closed contour (the last vertex of each contour is automatically
   * linked to the first). See the {@link #tessVertex
   * tessVertex} reference page for more details.
   * {@link #tessBeginContour tessBeginContour} can
   * only be called between {@link #tessBeginPolygon
   * tessBeginPolygon} and
   * {@link #tessEndPolygon tessEndPolygon}.
   *
   * Optional, throws GLException if not available in profile
   *
   * @param tessellator
   *        Specifies the tessellation object (created with
   *        {@link #newTess newTess}).
   *
   * @see #newTess          newTess
   * @see #tessBeginPolygon tessBeginPolygon
   * @see #tessVertex       tessVertex
   * @see #tessCallback     tessCallback
   * @see #tessProperty     tessProperty
   * @see #tessNormal       tessNormal
   * @see #tessEndPolygon   tessEndPolygon
   ****************************************************************************/
  public static final void tessEndContour(Tessellator tessellator) {
      TessellatorImpl tess = (TessellatorImpl) tessellator;
      tess.gluTessEndContour();
  }

  /*****************************************************************************
   * <b>tessEndPolygon</b> and
   * {@link #tessBeginPolygon tessBeginPolygon}
   * delimit the definition of a convex, concave or self-intersecting polygon.
   * Within each {@link #tessBeginPolygon
   * tessBeginPolygon}/<b>tessEndPolygon</b> pair, there must be one or
   * more calls to {@link #tessBeginContour
   * tessBeginContour}/{@link #tessEndContour
   * tessEndContour}. Within each contour, there are zero or more calls to
   * {@link #tessVertex tessVertex}. The vertices
   * specify a closed contour (the last vertex of each contour is automatically
   * linked to the first). See the {@link #tessVertex
   * tessVertex}, {@link #tessBeginContour
   * tessBeginContour} and {@link #tessEndContour
   * tessEndContour} reference pages for more details.<P>
   *
   * Optional, throws GLException if not available in profile
   *
   * Once <b>tessEndPolygon</b> is called, the polygon is tessellated, and
   * the resulting triangles are described through callbacks. See
   * {@link #tessCallback tessCallback} for
   * descriptions of the callback functions.
   *
   * @param tessellator
   *        Specifies the tessellation object (created with
   *        {@link #newTess newTess}).
   *
   * @see #newTess          newTess
   * @see #tessBeginContour tessBeginContour
   * @see #tessVertex       tessVertex
   * @see #tessCallback     tessCallback
   * @see #tessProperty     tessProperty
   * @see #tessNormal       tessNormal
   * @see #tessBeginPolygon tessBeginPolygon
   ****************************************************************************/
  public static final void tessEndPolygon(Tessellator tessellator) {
      TessellatorImpl tess = (TessellatorImpl) tessellator;
      tess.gluTessEndPolygon();
  }

  /*****************************************************************************

   * <b>beginPolygon</b> and {@link #endPolygon endPolygon}
   * delimit the definition of a nonconvex polygon. To define such a
   * polygon, first call <b>beginPolygon</b>. Then define the
   * contours of the polygon by calling {@link #tessVertex
   * tessVertex} for each vertex and {@link #nextContour
   * nextContour} to start each new contour. Finally, call {@link
   * #endPolygon endPolygon} to signal the end of the
   * definition. See the {@link #tessVertex tessVertex} and {@link
   * #nextContour nextContour} reference pages for more
   * details.<P>
   *
   * Optional, throws GLException if not available in profile
   *
   * Once {@link #endPolygon endPolygon} is called,
   * the polygon is tessellated, and the resulting triangles are described
   * through callbacks. See {@link #tessCallback
   * tessCallback} for descriptions of the callback methods.
   *
   * @param tessellator
   *        Specifies the tessellation object (created with
   *        {@link #newTess newTess}).
   *
   * @see #newTess          newTess
   * @see #nextContour      nextContour
   * @see #tessCallback     tessCallback
   * @see #tessVertex       tessVertex
   * @see #tessBeginPolygon tessBeginPolygon
   * @see #tessBeginContour tessBeginContour
   ****************************************************************************/
  public static final void beginPolygon(Tessellator tessellator) {
      TessellatorImpl tess = (TessellatorImpl) tessellator;
      tess.gluBeginPolygon();
  }

  /*****************************************************************************
   * <b>nextContour</b> is used to describe polygons with multiple
   * contours. After you describe the first contour through a series of
   * {@link #tessVertex tessVertex} calls, a
   * <b>nextContour</b> call indicates that the previous contour is complete
   * and that the next contour is about to begin. Perform another series of
   * {@link #tessVertex tessVertex} calls to
   * describe the new contour. Repeat this process until all contours have been
   * described.<P>
   *
   * Optional, throws GLException if not available in profile
   *
   * The type parameter defines what type of contour follows. The following
   * values are valid. <P>
   *
   * <b>GLU_EXTERIOR</b>
   * <UL>
   *   An exterior contour defines an exterior boundary of the polygon.
   * </UL>
   * <b>GLU_INTERIOR</b>
   * <UL>
   *   An interior contour defines an interior boundary of the polygon (such as
   *   a hole).
   * </UL>
   * <b>GLU_UNKNOWN</b>
   * <UL>
   *   An unknown contour is analyzed by the library to determine whether it is
   *   interior or exterior.
   * </UL>
   * <b>GLU_CCW, GLU_CW</b>
   * <UL>
   *   The first <b>GLU_CCW</b> or <b>GLU_CW</b> contour defined is considered
   *   to be exterior. All other contours are considered to be exterior if they
   *   are oriented in the same direction (clockwise or counterclockwise) as the
   *   first contour, and interior if they are not. If one contour is of type
   *   <b>GLU_CCW</b> or <b>GLU_CW</b>, then all contours must be of the same
   *   type (if they are not, then all <b>GLU_CCW</b> and <b>GLU_CW</b> contours
   *   will be changed to <b>GLU_UNKNOWN</b>). Note that there is no
   *   real difference between the <b>GLU_CCW</b> and <b>GLU_CW</b> contour
   *   types.
   * </UL><P>
   *
   * To define the type of the first contour, you can call <b>nextContour</b>
   * before describing the first contour. If you do not call
   * <b>nextContour</b> before the first contour, the first contour is marked
   * <b>GLU_EXTERIOR</b>.<P>
   *
   * <UL>
   *   <b>Note:</b>  The <b>nextContour</b> function is obsolete and is
   *   provided for backward compatibility only. The <b>nextContour</b>
   *   function is mapped to {@link #tessEndContour
   *   tessEndContour} followed by
   *   {@link #tessBeginContour tessBeginContour}.
   * </UL>
   *
   * @param tessellator
   *        Specifies the tessellation object (created with
   *        {@link #newTess newTess}).
   * @param type
   *        The type of the contour being defined.
   *
   * @see #newTess          newTess
   * @see #tessBeginContour tessBeginContour
   * @see #tessBeginPolygon tessBeginPolygon
   * @see #tessCallback     tessCallback
   * @see #tessEndContour   tessEndContour
   * @see #tessVertex       tessVertex
   ****************************************************************************/
  public static final void nextContour(Tessellator tessellator, int type) {
      TessellatorImpl tess = (TessellatorImpl) tessellator;
      tess.gluNextContour(type);
  }

  /*****************************************************************************
   * <b>endPolygon</b> and {@link #beginPolygon
   * beginPolygon} delimit the definition of a nonconvex polygon. To define
   * such a polygon, first call {@link #beginPolygon
   * beginPolygon}. Then define the contours of the polygon by calling
   * {@link #tessVertex tessVertex} for each vertex
   * and {@link #nextContour nextContour} to start
   * each new contour. Finally, call <b>endPolygon</b> to signal the end of
   * the definition. See the {@link #tessVertex
   * tessVertex} and {@link #nextContour
   * nextContour} reference pages for more details.<P>
   *
   * Optional, throws GLException if not available in profile
   *
   * Once <b>endPolygon</b> is called, the polygon is tessellated, and the
   * resulting triangles are described through callbacks. See
   * {@link #tessCallback tessCallback} for
   * descriptions of the callback methods.
   *
   * @param tessellator
   *        Specifies the tessellation object (created with
   *        {@link #newTess newTess}).
   *
   * @see #newTess          newTess
   * @see #nextContour      nextContour
   * @see #tessCallback     tessCallback
   * @see #tessVertex       tessVertex
   * @see #tessBeginPolygon tessBeginPolygon
   * @see #tessBeginContour tessBeginContour
   ****************************************************************************/
  public static final void endPolygon(Tessellator tessellator) {
      TessellatorImpl tess = (TessellatorImpl) tessellator;
      tess.gluEndPolygon();
  }

  // ErrorCode
  public static final int INVALID_ENUM = 100900;
  public static final int INVALID_VALUE = 100901;
  public static final int OUT_OF_MEMORY = 100902;
  public static final int INVALID_OPERATION = 100904;
  
  // TessCallback
  public static final int TESS_BEGIN = 100100;
  public static final int BEGIN = 100100;
  public static final int TESS_VERTEX = 100101;
  public static final int VERTEX = 100101;
  public static final int TESS_END = 100102;
  public static final int END = 100102;
  public static final int TESS_ERROR = 100103;
  public static final int TESS_EDGE_FLAG = 100104;
  public static final int EDGE_FLAG = 100104;
  public static final int TESS_COMBINE = 100105;
  public static final int TESS_BEGIN_DATA = 100106;
  public static final int TESS_VERTEX_DATA = 100107;
  public static final int TESS_END_DATA = 100108;
  public static final int TESS_ERROR_DATA = 100109;
  public static final int TESS_EDGE_FLAG_DATA = 100110;
  public static final int TESS_COMBINE_DATA = 100111;
  
  // TessProperty
  public static final int TESS_WINDING_RULE = 100140;
  public static final int TESS_BOUNDARY_ONLY = 100141;
  public static final int TESS_TOLERANCE = 100142;
  // JOGL-specific boolean property, false by default, that may improve the tessellation
  public static final int TESS_AVOID_DEGENERATE_TRIANGLES = 100149;
  
  // TessError
  public static final int TESS_ERROR1 = 100151;
  public static final int TESS_ERROR2 = 100152;
  public static final int TESS_ERROR3 = 100153;
  public static final int TESS_ERROR4 = 100154;
  public static final int TESS_ERROR5 = 100155;
  public static final int TESS_ERROR6 = 100156;
  public static final int TESS_MISSING_BEGIN_POLYGON = 100151;
  public static final int TESS_MISSING_BEGIN_CONTOUR = 100152;
  public static final int TESS_MISSING_END_POLYGON = 100153;
  public static final int TESS_MISSING_END_CONTOUR = 100154;
  public static final int TESS_COORD_TOO_LARGE = 100155;
  public static final int TESS_NEED_COMBINE_CALLBACK = 100156;
  
  // TessWinding
  public static final int TESS_WINDING_ODD = 100130;
  public static final int TESS_WINDING_NONZERO = 100131;
  public static final int TESS_WINDING_POSITIVE = 100132;
  public static final int TESS_WINDING_NEGATIVE = 100133;
  public static final int TESS_WINDING_ABS_GEQ_TWO = 100134;
  public static final double TESS_MAX_COORD = 1.0e150;
 
  

} // end of class Tess
