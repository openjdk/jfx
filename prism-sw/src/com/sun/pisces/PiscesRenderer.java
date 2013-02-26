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

package com.sun.pisces;

/**
 * PiscesRenderer class is basic public API accessing Pisces library capabilities.
 * 
 * Pisces renderer is intended to draw directly into underlying data buffer of AbstractSurface. 
 * Basic implementation of AbstractSurface is e.g. GraphicsSurface.
 *
 * All coordinates are in 15.16 representation. ie. 13 will be passed as 13<<16.
 * Simple use-case for PiscesRenderer together with GraphicsSurface would be e.g. -
 *<br/>
 *
 *  <code>
 *  <br/>
 *      GraphicsSurface surface = new GraphicsSurface();<br/>
 *  <br/>
 *      PiscesRenderer pr = new PiscesRenderer(surface);<br/>
 *      </code>
 *  <br/>
 *  <br/>
 *  Now, when we have instances ready, we can render something from our paint(Graphics g) method 
 *  <br/><br/>
 *  <code><br/>
 *      void paint(Graphics g) {<br/>
 *      <dd>   surface.bindTarget(g);<br/>
 *              //we set stroke color<br/>
 *              pr.setColor(0xFF, 0x00, 0xAF);<br/>
 *              // we set required Porter-Duff Compositing Rule<br/>
 *              pr.setComposite(RendererBase.COMPOSITE_SRC_OVER);<br/>
 *      <br/>
 *              //switch antialising on/off as required<br/>
 *              pr.setAntialiasing(true); // on<br/>
 *              <br/>
 *              pr.setTransform(ourTransform6Matrix);<br/>              
 *      <br/>
 *              //and now let's draw something finally<br/>
 *              pr.beginRendering(RendererBase.WIND_EVEN_ODD);<br/>
 *                      pr.moveTo(50 << 16, 100 << 16); //              <br/>
 *                      pr.lineTo(30<<16, 1<<16);
 *              pr.endRendering();<br/>
 *          <br/>
 *      surface.releaseTarget();<br/>
 *      </dd>
 *      }<br/>
 *  </code>
 */
public final class PiscesRenderer {

    public static final int ARC_OPEN = 0;
    public static final int ARC_CHORD = 1;
    public static final int ARC_PIE = 2;
    
    private long nativePtr = 0L;
    private AbstractSurface surface;

    /**
     * Creates a renderer that will write into a given surface.
     *
     * @param surface destination surface
     */
    public PiscesRenderer(AbstractSurface surface) {
        this.surface = surface;
        initialize();
    }

    private native void initialize();

    /**
     * Sets the current paint color.
     *
     * @param red a value between 0 and 255.
     * @param green a value between 0 and 255.
     * @param blue a value between 0 and 255.
     * @param alpha a value between 0 and 255.
     */
    public native void setColor(int red, int green, int blue, int alpha);

    /**
     * Sets the current paint color.  An alpha value of 255 is used. Calling <code>setColor</code> also switches 
     * painting mode - i.e. if we have specified gradient, or texture previously, this will be overcome with <code>setColor</code>
     * call. Also note, that 3-param <code>setColor</code> sets fully opaque RGB color. To draw with semi-transparent color
     * use 4-param convenience method. 
     *
     * @param red a value between 0 and 255.
     * @param green a value between 0 and 255.
     * @param blue a value between 0 and 255.
     */
    public void setColor(int red, int green, int blue) {
        setColor(red, green, blue, 255);
    }
    
    /**
     * Sets current Compositing Rule (Porter-Duff) to be used in following rendering operation. Note that <code>compositeAlpha</code>
     * is not changed. 
     * @param compositeRule one of <code>RendererBase.COMPOSITE_*</code> constants.
     */
    public native void setCompositeRule(int compositeRule);

    GradientColorMap gradientColorMap = null;
    
    private boolean arraysDiffer(int[] a, int[] b) {
        if (a == null) {
            return true;
        }
        int len = b.length;
        if (a.length != len) {
            return true;
        }
        for (int i = 0; i < len; i++) {
            if (a[i] != b[i]) {
                return true;
            }
        }
        
        return false;
    }
    
    private void setGradientColorMap(int[] fractions, int[] rgba,
                                     int cycleMethod) {
        if (fractions.length != rgba.length) {
            throw new IllegalArgumentException("fractions.length != rgba.length!");
        }
        
        if (gradientColorMap == null ||
            gradientColorMap.cycleMethod != cycleMethod ||
            arraysDiffer(gradientColorMap.fractions, fractions) ||
            arraysDiffer(gradientColorMap.rgba, rgba)) {
            this.gradientColorMap =
                new GradientColorMap(fractions, rgba, cycleMethod);
        }
    }
    
    private native void setLinearGradientImpl(int x0, int y0, int x1, int y1,
                                              int[] colors,
                                              int cycleMethod,
                                              Transform6 gradientTransform);

    /**
     * This method calculates a GradientColorMap, which can be used in calls to @see setLinearGradient and @see setRadialGradient.
     * Imagine, we want to draw simple gradient from blue to red color. Each pixel on line perpendicular to line L = [[x0,y0], [x1, y1]] will have same constant color.
     * Pixels on perpendicular-line which passes [x0, y0] will be blue. Those on line passing [x1, y1] will be red. Colors on lines in between will be interpolated by <code>fractions</code>.
     * @param fractions this array defines normalized distances in which color (rgba[i]) starts to fade into next color (rgba[i+1]). This distance from the point [x0,y0] is given as fraction[i]*l, where l is length of line [[x0,y0], [x1,y1]]. fraction[i+1] says, in what distance fraction[i+1]*l from [x0,y0] should color already have firm value of rgba[i+1]. Values passed in fractions should be from interval <0.0, 1.0>, in 15.16 format.
     * @param rgba colors which the linear gradient passes through. Generally should be fulfilled this formula <code>rgba.length == fractions.length</code>
     * @param cycleMethod some value from <code>GradientColorMap.CYCLE_*</code>. @see GradienColorMap
     * @return The calculated GradientColorMap.
     * @see GradienColorMap
     */
    public GradientColorMap calculateGradientColorMap(int[] fractions,
                                                      int[] rgba,
                                                      int cycleMethod) {
        return new GradientColorMap(fractions, rgba, cycleMethod);
    }

    /**
     * This method sets linear color-gradient data to be used as paint data in following rendering operation.
     * Imagine, we want to draw simple gradient from blue to red color. Each pixel on line perpendicular to line L = [[x0,y0], [x1, y1]] will have same constant color.
     * Pixels on perpendicular-line which passes [x0, y0] will be blue. Those on line passing [x1, y1] will be red. Colors on lines in between will be interpolated by <code>fractions</code>.
     * @param x0 x-coordinate of the starting point of the linear gradient
     * @param y0 y-coordinate of the starting point of the linear gradient
     * @param x1 x-coordinate of the end point of the linear gradient
     * @param y0 y-coordinate of the end point of the linear gradient
     * @param fractions this array defines normalized distances in which color (rgba[i]) starts to fade into next color (rgba[i+1]). This distance from the point [x0,y0] is given as fraction[i]*l, where l is length of line [[x0,y0], [x1,y1]]. fraction[i+1] says, in what distance fraction[i+1]*l from [x0,y0] should color already have firm value of rgba[i+1]. Values passed in fractions should be from interval <0.0, 1.0>, in 15.16 format.  
     * @param rgba colors which the linear gradient passes through. Generally should be fulfilled this formula <code>rgba.length == fractions.length</code>
     * @param cycleMethod some value from <code>GradientColorMap.CYCLE_*</code>. @see GradienColorMap
     * @param gradientTransform transformation applied to gradient paint data. This way we can either transform gradient fill together with filled object or leave it as if transformed gradient-filled object was a window through which we observe gradient area.
     * @see GradienColorMap
     */
    public void setLinearGradient(int x0, int y0, int x1, int y1,
                                  int[] fractions, int[] rgba,
                                  int cycleMethod,
                                  Transform6 gradientTransform) {
        setGradientColorMap(fractions, rgba, cycleMethod);
        setLinearGradientImpl(x0, y0, x1, y1,
                              gradientColorMap.colors, cycleMethod,
                              gradientTransform == null ? new Transform6(1 << 16, 0, 0, 1 << 16, 0, 0) : gradientTransform);
    }

    /**
     * This method sets linear color-gradient data to be used as paint data in following rendering operation.
     * Imagine, we want to draw simple gradient from blue to red color. Each pixel on line perpendicular to line L = [[x0,y0], [x1, y1]] will have same constant color.
     * Pixels on perpendicular-line which passes [x0, y0] will be blue. Those on line passing [x1, y1] will be red. Colors on lines in between will be interpolated by <code>fractions</code>.
     * @param x0 x-coordinate of the starting point of the linear gradient
     * @param y0 y-coordinate of the starting point of the linear gradient
     * @param x1 x-coordinate of the end point of the linear gradient
     * @param y0 y-coordinate of the end point of the linear gradient
     * @param gradientColorMap The GradientColorMap calculated with @see calculateLinearGradient.
     * @param gradientTransform transformation applied to gradient paint data. This way we can either transform gradient fill together with filled object or leave it as if transformed gradient-filled object was a window through which we observe gradient area.
     * @see GradienColorMap
     */
    public void setLinearGradient(int x0, int y0, int x1, int y1,
                                  GradientColorMap gradientColorMap,
                                  Transform6 gradientTransform) {
        this.gradientColorMap = gradientColorMap;
        setLinearGradientImpl(x0, y0, x1, y1,
                              gradientColorMap.colors,
                              gradientColorMap.cycleMethod,
                              gradientTransform == null ? new Transform6(1 << 16, 0, 0, 1 << 16, 0, 0) : gradientTransform);
    }

    /**
     * Java2D-style linear gradient creation. The color changes proportionally
     * between point P0 (color0) nad P1 (color1). Cycle method constants are
     * defined in GradientColorMap (CYCLE_*). This is convenience method only. Same as if setLinearGradient method with 8 parameters was called with
     * fractions = {0x0000, 0x10000}, rgba = {color0, color1} and identity transformation matrix.           
     *
     * @param x0 x coordinate of point P0
     * @param y0 y coordinate of point P0     
     * @param color0 color of P0
     * @param x1 x coordinate of point P1
     * @param y1 y coordinate of point P1     
     * @param color1 color of P1
     * @param cycleMethod type of cycling of the gradient (NONE, REFLECT, REPEAT)
     * 
     * As Pisces Gradient support was added to support features introduced in SVG, see e.g. http://www.w3.org/TR/SVG11/pservers.html for more information and examples.         
     */
    public void setLinearGradient(int x0, int y0, int color0, 
                                  int x1, int y1, int color1,
                                  int cycleMethod) {
      int[] fractions = {0x0000, 0x10000};
      int[] rgba = {color0, color1};
      Transform6 ident = new Transform6(1 << 16, 0, 0, 1 << 16, 0, 0);
      setLinearGradient(x0, y0, x1, y1, fractions, rgba, cycleMethod, ident);
    }

    private native void setRadialGradientImpl(int cx, int cy, int fx, int fy,
                                              int radius,
                                              int[] colors,
                                              int cycleMethod,
                                              Transform6 gradientTransform);

    /**
     * This method sets radial gradient paint data to be used in subsequent rendering. Radial gradient data generated will be used to fill the touched pixels of the path we draw.
     * 
     * @param cx cx, cy and radius triplet defines the largest circle for the gradient. 100% gradient stop is mapped to perimeter of this circle. 
     * @param cy 
     * @param fx fx,fy defines focal point of the gradient. ie. 0% gradient stop is mapped to fx,fy point. If cx == fx and cy == fy, then gradient consists of homocentric circles. If these relations are not met, gradient field is deformed and eccentric ovals can be observed. 
     * @param fy
     * @param radius @see cx
     * @param fractions @see setLinearGradient
     * @param rgba @see setLinearGradient
     * @param cycleMethod @see setLinearGradient
     * @param gradientTransform @see setLinearGradient
     * 
     * As Pisces Gradient support was added to support features introduced in SVG, see e.g. http://www.w3.org/TR/SVG11/pservers.html for more information and examples. 
     */
    
    public void setRadialGradient(int cx, int cy, int fx, int fy,
                                  int radius,
                                  int[] fractions, int[] rgba,
                                  int cycleMethod,
                                  Transform6 gradientTransform) {
        setGradientColorMap(fractions, rgba, cycleMethod);
        setRadialGradientImpl(cx, cy, fx, fy, radius,
                              gradientColorMap.colors, cycleMethod,
                              gradientTransform == null ? new Transform6(1 << 16, 0, 0, 1 << 16, 0, 0) : gradientTransform);
    }

    /**
     * This method sets radial gradient paint data to be used in subsequent rendering. Radial gradient data generated will be used to fill the touched pixels of the path we draw.
     *
     * @param cx cx, cy and radius triplet defines the largest circle for the gradient. 100% gradient stop is mapped to perimeter of this circle.
     * @param cy
     * @param fx fx,fy defines focal point of the gradient. ie. 0% gradient stop is mapped to fx,fy point. If cx == fx and cy == fy, then gradient consists of homocentric circles. If these relations are not met, gradient field is deformed and eccentric ovals can be observed.
     * @param fy
     * @param radius @see cx
     * @param gradientColorMap @see setLinearGradient
     * @param gradientTransform @see setLinearGradient
     *
     * As Pisces Gradient support was added to support features introduced in SVG, see e.g. http://www.w3.org/TR/SVG11/pservers.html for more information and examples.
     */

    public void setRadialGradient(int cx, int cy, int fx, int fy,
                                  int radius,
                                  GradientColorMap gradientColorMap,
                                  Transform6 gradientTransform) {
        this.gradientColorMap = gradientColorMap;
        setRadialGradientImpl(cx, cy, fx, fy, radius,
                              gradientColorMap.colors,
                              gradientColorMap.cycleMethod,
                              gradientTransform == null ? new Transform6(1 << 16, 0, 0, 1 << 16, 0, 0) : gradientTransform);
    }

    public native void setTexture(int imageType, int data[], int width, int height,
        Transform6 textureTransform, boolean repeat, boolean hasAlpha);

    /**
     * Sets a clip rectangle for all primitives.  Each primitive will be
     * clipped to the intersection of this rectangle and the destination
     * image bounds.
     */
    public native void setClip(int minX, int minY, int width, int height);

    /**
     * Resets the clip rectangle.  Each primitive will be clipped only
     * to the destination image bounds.
     */
    public native void resetClip();

    /**
     * Clears rectangle (x, y, x + w, y + h). Clear sets all pixels to transparent black (0x00000000 ARGB).
     */
    public native void clearRect(int x, int y, int w, int h);

    public native void fillRect(int x, int y, int w, int h);

    public native void emitAndClearAlphaRow(byte[] alphaMap, int[] alphaDeltas, int pix_y, int pix_x_from, int pix_x_to,
        int rowNum);

    public native void fillAlphaMask(byte[] mask, int x, int y, int width, int height, int offset, int stride);

    public native void fillLCDAlphaMask(byte[] mask, int x, int y, int width, int height, int offset, int stride);

    public native void drawImage(int imageType, int data[],  int width, int height, int offset, int stride,
        Transform6 textureTransform, boolean repeat,
        int bboxX, int bboxY, int bboxW, int bboxH,
        int interpolateMinX, int interpolateMinY, int interpolateMaxX, int interpolateMaxY,
        int topOpacity, int bottomOpacity,
        boolean hasAlpha);


    protected void finalize() {
        this.nativeFinalize();
    }

    /**
     * Native finalizer. Releases native memory used by PiscesRenderer at lifetime.
     */
    public native void nativeFinalize();
}
