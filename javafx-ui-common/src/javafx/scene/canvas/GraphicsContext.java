/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.javafx.sg.GrowableDataBuffer;
import com.sun.javafx.sg.PGCanvas;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import javafx.geometry.VPos;
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

/**
 * This class is used to issue draw calls to a {@code Canvas} using a buffer. 
 * <p>
 * Each call pushes the necessary parameters onto the buffer
 * where it is executed on the image of the {@code Canvas} node.
 * <p>
 * A {@code Canvas} only contains one {@code GraphicsContext} and only one 
 * buffer and you can only issue commands to its GraphicsContext from one thread. 
 * Like any node, if the {@code Canvas} node is not attached to the scene, 
 * it can be modified by any thread. Once a {@code Canvas} node is attached to
 * the scene, it must be modified on the JavaFX Application Thread.
 * <p>
 * A {@code GraphicsContext} also manages a stack of state objects that can
 * be saved or restored at anytime. 
 *
 * <p>Example:</p>
 *
 * <p>
 * <pre>
import javafx.scene.*;
import javafx.scene.paint.*;
import javafx.scene.canvas.*;

Group root = new Group();
Scene s = new Scene(root, 300, 300, Color.BLACK);

final Canvas canvas = new Canvas(250,250);
GraphicsContext gc = canvas.getGraphicsContext2D();
 
gc.setFill(Color.BLUE);
gc.fillRect(75,75,100,100);
 
root.getChildren().add(canvas);
 * </pre>
 * </p>
 *
 * @since JavaFX 2.2
 */
public final class GraphicsContext {
    /**
     * @deprecated 
     */
    public enum CompositeOperation {       
        SRC_OVER,
        SRC_IN,
        SRC_OUT,
        SRC_ATOP,
        DST_OVER,
        DST_IN,
        DST_OUT,
        DST_ATOP,
        LIGHTER,
        COPY,
        XOR,
    };

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
        CompositeOperation compop;
        Affine2D transform;
        Paint fill;
        Paint stroke;
        double linewidth;
        StrokeLineCap linecap;
        StrokeLineJoin linejoin;
        double miterlimit;
        int numClipPaths;
        Font font;
        TextAlignment textalign;
        VPos textbaseline;
        Effect effect;
        FillRule fillRule;

        State() {
            this(1.0, CompositeOperation.SRC_OVER,
                 new Affine2D(),
                 Color.BLACK, Color.BLACK,
                 1.0, StrokeLineCap.BUTT, StrokeLineJoin.MITER, 10.0,
                 0, Font.getDefault(), TextAlignment.LEFT, VPos.BASELINE,
                 null, FillRule.NON_ZERO);
        }

        State(State copy) {
            this(copy.globalAlpha, copy.compop,
                 new Affine2D(copy.transform),
                 copy.fill, copy.stroke,
                 copy.linewidth, copy.linecap, copy.linejoin, copy.miterlimit,
                 copy.numClipPaths,
                 copy.font, copy.textalign, copy.textbaseline,
                 copy.effect, copy.fillRule);
        }

        State(double globalAlpha, CompositeOperation compop,
                     Affine2D transform, Paint fill, Paint stroke,
                     double linewidth, StrokeLineCap linecap,
                     StrokeLineJoin linejoin, double miterlimit,
                     int numClipPaths,
                     Font font, TextAlignment align, VPos baseline,
                     Effect effect, FillRule fillRule)
        {
            this.globalAlpha = globalAlpha;
            this.compop = compop;
            this.transform = transform;
            this.fill = fill;
            this.stroke = stroke;
            this.linewidth = linewidth;
            this.linecap = linecap;
            this.linejoin = linejoin;
            this.miterlimit = miterlimit;
            this.numClipPaths = numClipPaths;
            this.font = font;
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
            ctx.setGlobalCompositeOperation(compop);
            ctx.setTransform(transform.getMxx(), transform.getMyx(),
                             transform.getMxy(), transform.getMyy(),
                             transform.getMxt(), transform.getMyt());
            ctx.setFill(fill);
            ctx.setStroke(stroke);
            ctx.setLineWidth(linewidth);
            ctx.setLineCap(linecap);
            ctx.setLineJoin(linejoin);
            ctx.setMiterLimit(miterlimit);
            GrowableDataBuffer buf = ctx.getBuffer();
            while (ctx.curState.numClipPaths > numClipPaths) {
                ctx.curState.numClipPaths--;
                ctx.clipStack.removeLast();
                buf.putByte(PGCanvas.POP_CLIP);
            }
            ctx.setFillRule(fillRule);
            ctx.setFont(font);
            ctx.setTextAlign(textalign);
            ctx.setTextBaseline(textbaseline);
            ctx.setEffect(effect);
        }
    }

    private GrowableDataBuffer getBuffer() {
        theCanvas.markBufferDirty();
        return theCanvas.getBuffer();
    }

    private static float coords[] = new float[6];
    private static final byte pgtype[] = {
        PGCanvas.MOVETO,
        PGCanvas.LINETO,
        PGCanvas.QUADTO,
        PGCanvas.CUBICTO,
        PGCanvas.CLOSEPATH,
    };
    private static final int numsegs[] = { 2, 2, 4, 6, 0, };

    private void markPathDirty() {
        pathDirty = true;
    }

    private void writePath(byte command) {
        updateTransform();
        GrowableDataBuffer buf = getBuffer();
        if (pathDirty) {
            buf.putByte(PGCanvas.PATHSTART);
            PathIterator pi = path.getPathIterator(null);
            while (!pi.isDone()) {
                int pitype = pi.currentSegment(coords);
                buf.putByte(pgtype[pitype]);
                for (int i = 0; i < numsegs[pitype]; i++) {
                    buf.putFloat(coords[i]);
                }
                pi.next();
            }
            buf.putByte(PGCanvas.PATHEND);
            pathDirty = false;
        }
        buf.putByte(command);
    }

    private void writePaint(Paint p, byte command) {
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(command);
        buf.putObject(p.impl_getPlatformPaint());
    }

    private void writeArcType(ArcType closure) {
        byte type;
        switch (closure) {
            case OPEN:  type = PGCanvas.ARC_OPEN;  break;
            case CHORD: type = PGCanvas.ARC_CHORD; break;
            case ROUND: type = PGCanvas.ARC_PIE;   break;
            default: return;  // ignored for consistency with other attributes
        }
        writeParam(type, PGCanvas.ARC_TYPE);
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
        curState.transform.deltaTransform(polybuf, 0, polybuf, 0, n/2);
        for (int i = 0; i < n; i += 2) {
            buf.putByte(command);
            buf.putFloat(polybuf[i]);
            buf.putFloat(polybuf[i+1]);
            command = PGCanvas.LINETO;
        }
    }
    private void writePoly(double xPoints[], double yPoints[], int nPoints,
                           boolean close, byte command)
    {
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(PGCanvas.PATHSTART);
        int pos = 0;
        byte polycmd = PGCanvas.MOVETO;
        for (int i = 0; i < nPoints; i++) {
            if (pos >= polybuf.length) {
                flushPolyBuf(buf, polybuf, pos, polycmd);
                polycmd = PGCanvas.LINETO;
            }
            polybuf[pos++] = (float) xPoints[i];
            polybuf[pos++] = (float) yPoints[i];
        }
        flushPolyBuf(buf, polybuf, pos, polycmd);
        if (close) {
            buf.putByte(PGCanvas.CLOSEPATH);
        }
        buf.putByte(command);
        // Now that we have changed the PG layer path, we need to mark our path dirty.
        markPathDirty();
    }

    private void writeImage(Image img,
                            double dx, double dy, double dw, double dh)
    {
        if (img.getProgress() < 1.0) return;
        Object platformImg = img.impl_getPlatformImage();
        if (platformImg == null) return;
        updateTransform();
        GrowableDataBuffer buf = getBuffer();
        writeRectParams(buf, dx, dy, dw, dh, PGCanvas.DRAW_IMAGE);
        buf.putObject(platformImg);
    }

    private void writeImage(Image img,
                            double dx, double dy, double dw, double dh,
                            double sx, double sy, double sw, double sh)
    {
        if (img.getProgress() < 1.0) return;
        Object platformImg = img.impl_getPlatformImage();
        if (platformImg == null) return;
        updateTransform();
        GrowableDataBuffer buf = getBuffer();
        writeRectParams(buf, dx, dy, dw, dh, PGCanvas.DRAW_SUBIMAGE);
        buf.putFloat((float) sx);
        buf.putFloat((float) sy);
        buf.putFloat((float) sw);
        buf.putFloat((float) sh);
        buf.putObject(platformImg);
    }

    private void writeText(String text, double x, double y, double maxWidth,
                           byte command)
    {
        updateTransform();
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(command);
        buf.putFloat((float) x);
        buf.putFloat((float) y);
        buf.putFloat((float) maxWidth);
        buf.putObject(text);
    }

    private void writeParam(double v, byte command) {
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
            buf.putByte(PGCanvas.TRANSFORM);
            buf.putDouble(curState.transform.getMxx());
            buf.putDouble(curState.transform.getMxy());
            buf.putDouble(curState.transform.getMxt());
            buf.putDouble(curState.transform.getMyx());
            buf.putDouble(curState.transform.getMyy());
            buf.putDouble(curState.transform.getMyt());
        }
    }
    
   /**
    * Gets the {@code Canvas} that the {@code GraphicsContext} is issuing draw
    * calls to. There is only ever one {@code Canvas} for a {@code GraphicsContext}.
    *
    * @return Canvas the canvas that this {@code GraphicsContext} is issuing draw
    * calls to.
    */
    public Canvas getCanvas() {
        return theCanvas;
    } 

    /**
     * Saves the current State, pushing it onto a Stack.
     * This method does NOT alter the current state in any way.
     */
    public void save() {
        stateStack.push(curState.copy());
    }

    /**
     * Pops the state off of the stack, setting all state attributes to their 
     * value at the time the state was saved.
     */
    public void restore() {
        if (!stateStack.isEmpty()) {
            State savedState = stateStack.pop();
            savedState.restore(this);
            txdirty = true;
        }
    }

    /**
     * Translates the current transform by x, y
     * @param x value to translate along the x axis.
     * @param y value to translate along the y axis.
     */
    public void translate(double x, double y) {
        curState.transform.translate(x, y);
        txdirty = true;
    }

    /**
     * Scales the current transform by x, y
     * @param x value to scale in the x axis.
     * @param y value to scale in the y axis.
     */
    public void scale(double x, double y) {
        curState.transform.scale(x, y);
        txdirty = true;
    }

    /**
     * Rotates the current transform in degrees
     * @param degrees value in degrees to rotate the current transform.
     */
    public void rotate(double degrees) {
        curState.transform.rotate(Math.toRadians(degrees));
        txdirty = true;
    }

    private static Affine2D scratchTX = new Affine2D();
    /**
     * Concatenates the input with the current transform.
     * 
     * @param mxx
     * @param myx
     * @param mxy
     * @param myy
     * @param mxt
     * @param myt
     */
    public void transform(double mxx, double myx,
                          double mxy, double myy,
                          double mxt, double myt)
    {
        scratchTX.setTransform(mxx, myx,
                               mxy, myy,
                               mxt, myt);
        curState.transform.concatenate(scratchTX);
        txdirty = true;
    }
    
    /**
     * Concatenates the input with the current transform.
     * 
     * @param xform
     */
    public void transform(Affine xform) {
        scratchTX.setTransform(xform.getMxx(), xform.getMyx(),
                               xform.getMxy(), xform.getMyy(),
                               xform.getTx(), xform.getTy());
        curState.transform.concatenate(scratchTX);
        txdirty = true;
    }

    /**
     * Sets the current transform
     * @param mxx 
     * @param myx
     * @param mxy 
     * @param myy 
     * @param mxt
     * @param myt  
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
     * Sets the current transform ignoring any 3D transform by grabbing the 2x3
     * of the matrix. 3D transforms are not supported in a 2D GraphicsContext.
     * 
     * @param mxx 
     * @param myx
     * @param mxy 
     * @param myy 
     * @param mxt
     * @param myt  
     */
    public void setTransform(Affine xform) {
        curState.transform.setTransform(xform.getMxx(), xform.getMyx(),
                                        xform.getMxy(), xform.getMyy(),
                                        xform.getTx(), xform.getTy());
        txdirty = true;
    }
    
    /**
     * Returns a copy of the current transform. 
     * 
     * @param xform A transform object that will be used to hold the result.
     * If xform is non null, then this method will copy the current transform 
     * into that object. If xform is null a new transform object will be
     * constructed. In either case, the return value is a copy of the current 
     * transform. 
     *
     * @return A copy Current State's transform  
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
     * @return A copy Current State's transform  
     */
    public Affine getTransform() {
        return getTransform(null);
    }

    /**
     * Sets the Global Alpha of the current state.
     * @param alpha 
     */
    public void setGlobalAlpha(double alpha) {
        if (curState.globalAlpha != alpha) {
            curState.globalAlpha = alpha;
            writeParam(alpha, PGCanvas.GLOBAL_ALPHA);
        }
    }
    
    /**
     * Gets the Global Alpha of the current state.
     * @return double global alpha 
     */
    public double getGlobalAlpha() {
        return curState.globalAlpha;
    }
    
    
    
    /**
     * Sets the Global Composite Operation of the current state.
     * 
     * @param op
     */
    public void setGlobalCompositeOperation(CompositeOperation op) {
        if (op != curState.compop) {
            byte mode;
            switch (op) {
                case SRC_OVER: mode = PGCanvas.COMP_SRC_OVER; break;
                case SRC_IN:   mode = PGCanvas.COMP_SRC_IN;   break;
                case SRC_OUT:  mode = PGCanvas.COMP_SRC_OUT;  break;
                case SRC_ATOP: mode = PGCanvas.COMP_SRC_ATOP; break;
                case DST_OVER: mode = PGCanvas.COMP_DST_OVER; break;
                case DST_IN:   mode = PGCanvas.COMP_DST_IN;   break;
                case DST_OUT:  mode = PGCanvas.COMP_DST_OUT;  break;
                case DST_ATOP: mode = PGCanvas.COMP_DST_ATOP; break;
                case LIGHTER:  mode = PGCanvas.COMP_LIGHTER;  break;
                case COPY:     mode = PGCanvas.COMP_COPY;     break;
                case XOR:      mode = PGCanvas.COMP_XOR;      break;
                default: return;  // Unknown values are ignored per W3C spec
            }
            curState.compop = op;
            writeParam(mode, PGCanvas.COMP_MODE);
        }
    }
    
    /**
     * Gets the Global Composite Operation of the current state.
     * 
     * @return CompositeOperation 
     */
    public CompositeOperation getGlobalCompositeOperation() {
        return curState.compop;
    }

    /**
     * Sets the current fill attribute. This method affects the paint used for any
     * method with "fill" in it. For Example, fillRect(...), fillOval(...).
     * 
     * @param p The {@code Paint} to be used as the fill {@code Paint}.
     */
    public void setFill(Paint p) {
        if (curState.fill != p) {
            curState.fill = p;
            writePaint(p, PGCanvas.FILL_PAINT);
        }
    }
    
    /**
     * Gets the current fill attribute. This method affects the paint used for any
     * method with "fill" in it. For Example, fillRect(...), fillOval(...).
     * 
     * @return p The {@code Paint} to be used as the fill {@code Paint}.
     */
    public Paint getFill() {
        return curState.fill;
    }

    /**
     * Sets the current state's stroke.
     * 
     * @param p The Paint to be used as the stroke Paint.
     */
    public void setStroke(Paint p) {
        if (curState.stroke != p) {
            curState.stroke = p;
            writePaint(p, PGCanvas.STROKE_PAINT);
        }
    }
    
    /**
     * gets the current state's stroke.
     * 
     * @param p The Paint to be used as the stroke Paint.
     */
    public Paint getStroke() {
        return curState.stroke;
    }

    /**
     * Sets the current line width attribute.
     * 
     * @param lw value between 0 and infinity, with any other value being
     * ignored and leaving the value unchanged.
     * 
     */
    public void setLineWidth(double lw) {
        // Per W3C spec: On setting, zero, negative, infinite, and NaN
        // values must be ignored, leaving the value unchanged
        if (lw > 0 && lw < Double.POSITIVE_INFINITY) {
            if (curState.linewidth != lw) {
                curState.linewidth = lw;
                writeParam(lw, PGCanvas.LINE_WIDTH);
            }
        }
    }
    
    /**
     * Gets the current line width attribute.
     * 
     * @return value between 0 and infinity, with any other value being
     * ignored and leaving the value unchanged.
     * 
     */
    public double getLineWidth() {
        return curState.linewidth;
    }

    /**
     * Sets the current stroke line cap attribute. 
     * 
     * @param cap {@code StrokeLineCap} with a value of Butt, Round, or Square.
     */
    public void setLineCap(StrokeLineCap cap) {
        if (curState.linecap != cap) {
            byte v;
            switch (cap) {
                case BUTT: v = PGCanvas.CAP_BUTT; break;
                case ROUND: v = PGCanvas.CAP_ROUND; break;
                case SQUARE: v = PGCanvas.CAP_SQUARE; break;
                default: return;
            }
            curState.linecap = cap;
            writeParam(v, PGCanvas.LINE_CAP);
        }
    }
    
    /**
     * Gets the current stroke line cap attribute. 
     * 
     * @return {@code StrokeLineCap} with a value of Butt, Round, or Square.
     */
    public StrokeLineCap getLineCap() {
        return curState.linecap;
    }

    /**
     * Sets the current stroke line join attribute.
     * 
     * @param join {@code StrokeLineJoin} with a value of Miter, Bevel, or Round.
     */
    public void setLineJoin(StrokeLineJoin join) {
        if (curState.linejoin != join) {
            byte v;
            switch (join) {
                case MITER: v = PGCanvas.JOIN_MITER; break;
                case BEVEL: v = PGCanvas.JOIN_BEVEL; break;
                case ROUND: v = PGCanvas.JOIN_ROUND; break;
                default: return;
            }
            curState.linejoin = join;
            writeParam(v, PGCanvas.LINE_JOIN);
        }
    }
    
    /**
     * Gets the current stroke line join attribute.
     * 
     * @return {@code StrokeLineJoin} with a value of Miter, Bevel, or Round.
     */
    public StrokeLineJoin getLineJoin() {
        return curState.linejoin;
    }

    /**
     * Sets the current miter limit attribute.
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
                writeParam(ml, PGCanvas.MITER_LIMIT);
            }
        }
    }
    
    /**
     * Gets the current miter limit attribute.
     * 
     * @param miter limit value between 0 and positive infinity with 
     * any other value being ignored and leaving the value unchanged.
     */
    public double getMiterLimit() {
        return curState.miterlimit;
    }

    /**
     * Sets the current state's Font attribute.
     * 
     * @param f the Font 
     */
    public void setFont(Font f) {
        if (curState.font != f) {
            curState.font = f;
            GrowableDataBuffer buf = getBuffer();
            buf.putByte(PGCanvas.FONT);
            buf.putObject(f.impl_getNativeFont());
        }
    }
    
    /**
     * Gets the current state's Font attribute.
     * 
     * @return the Font 
     */
    public Font getFont() {
        return curState.font;
    }

    /**
     * Sets the current TextAlignment attribute
     * 
     * @param align {@code TextAlignment} with values of Left, Center, Right, or
     * Justify.
     */
    public void setTextAlign(TextAlignment align) {
        if (curState.textalign != align) {
            byte a;
            switch (align) {
                case LEFT: a = PGCanvas.ALIGN_LEFT; break;
                case CENTER: a = PGCanvas.ALIGN_CENTER; break;
                case RIGHT: a = PGCanvas.ALIGN_RIGHT; break;
                case JUSTIFY: a = PGCanvas.ALIGN_JUSTIFY; break;
                default: return;
            }
            curState.textalign = align;
            writeParam(a, PGCanvas.TEXT_ALIGN);
        }
    }
    
    /**
     * Gets the current TextAlignment attribute
     * 
     * @return {@code TextAlignment} with values of Left, Center, Right, or
     * Justify.
     */
    public TextAlignment getTextAlign() {
        return curState.textalign;
    }

    /**
     * Sets the current Text Baseline attribute.
     * 
     * @param baseline {@code VPos} with values of Top, Center, Baseline, or Bottom
     */
    public void setTextBaseline(VPos baseline) {
        if (curState.textbaseline != baseline) {
            byte b;
            switch (baseline) {
                case TOP: b = PGCanvas.BASE_TOP; break;
                case CENTER: b = PGCanvas.BASE_MIDDLE; break;
                case BASELINE: b = PGCanvas.BASE_ALPHABETIC; break;
                case BOTTOM: b = PGCanvas.BASE_BOTTOM; break;
                default: return;
            }
            curState.textbaseline = baseline;
            writeParam(b, PGCanvas.TEXT_BASELINE);
        }
    }
    
    /**
     * Gets the current Text Baseline attribute.
     * 
     * @return {@code VPos} with values of Top, Center, Baseline, or Bottom
     */
    public VPos getTextBaseline() {
        return curState.textbaseline;
    }

    /**
     * Fills the given string of text at position x, y (0,0 at top left)
     * with the current fill paint attribute.
     * 
     * @param text the string of text
     * @param x position on the x axis.
     * @param y position on the y axis.
     */
    public void fillText(String text, double x, double y) {
        writeText(text, x, y, 0, PGCanvas.FILL_TEXT);
    }

    /**
     * draws the given string of text at position x, y (0,0 at top left)
     * with the current stroke paint attribute.
     * 
     * @param text the string of text
     * @param x position on the x axis.
     * @param y position on the y axis.
     */
    public void strokeText(String text, double x, double y) {
        writeText(text, x, y, 0, PGCanvas.STROKE_TEXT);
    }

    /**
     * Fills text and includes a maximum width of the string. 
     * 
     * If the width of the text extends past max width, then it will be sized
     * to fit.
     * 
     * @param text
     * @param x position on the x axis.
     * @param y position on the y axis.
     * @param maxWidth  maximum width the text string can have.
     */
    public void fillText(String text, double x, double y, double maxWidth) {
        if (maxWidth <= 0) return;
        writeText(text, x, y, maxWidth, PGCanvas.FILL_TEXT);
    }

    /**
     * Draws text with stroke paint and includes a maximum width of the string. 
     * 
     * If the width of the text extends past max width, then it will be sized
     * to fit.
     * 
     * @param text
     * @param x position on the x axis.
     * @param y position on the y axis.
     * @param maxWidth  maximum width the text string can have.
     */
    public void strokeText(String text, double x, double y, double maxWidth) {
        if (maxWidth <= 0) return;
        writeText(text, x, y, maxWidth, PGCanvas.STROKE_TEXT);
    }


    /**
     * Set the filling rule constant for determining the interior of the path.
     * The value must be one of the following constants:
     * {@code FillRile.EVEN_ODD} or {@code FillRule.NON_ZERO}.
     * The default value is {@code FillRule.NON_ZERO}.
     *
     * @defaultValue FillRule.NON_ZERO
     */
     public void setFillRule(FillRule fillRule) {
         if (curState.fillRule != fillRule) {
            byte b;
            if (fillRule == FillRule.EVEN_ODD) {
                b = PGCanvas.FILL_RULE_EVEN_ODD;
            } else { 
                b = PGCanvas.FILL_RULE_NON_ZERO;
            }
            curState.fillRule = fillRule;
            writeParam(b, PGCanvas.FILL_RULE);
        }
     }
    
    /**
     * Get the filling rule constant for determining the interior of the path.
     * The default value is {@code FillRule.NON_ZERO}.
     *
     * @return current state's fill rule
     */
     public FillRule getFillRule() {
         return curState.fillRule;
     }
    
    /**
     * Starts a Path 
     */
    public void beginPath() {
        path.reset();
        markPathDirty();
    }

    /**
     * Issues a move command for the current path to the given x,y coordinate.
     * 
     * @param x0 
     * @param y0 
     */
    public void moveTo(double x0, double y0) {
        coords[0] = (float) x0;
        coords[1] = (float) y0;
        curState.transform.transform(coords, 0, coords, 0, 1);
        path.moveTo(coords[0], coords[1]);
        markPathDirty();
    }

    /**
     * Issues a lineTo command for the current path to the given x,y coordinate
     * 
     * @param x1 
     * @param y1 
     */
    public void lineTo(double x1, double y1) {
        coords[0] = (float) x1;
        coords[1] = (float) y1;
        curState.transform.transform(coords, 0, coords, 0, 1);
        path.lineTo(coords[0], coords[1]);
        markPathDirty();
    }

    /**
     * Issues a quadraticCurveTo command for the current path.
     * 
     * @param xc 
     * @param yc
     * @param x1 
     * @param y1  
     */
    public void quadraticCurveTo(double xc, double yc, double x1, double y1) {
        coords[0] = (float) xc;
        coords[1] = (float) yc;
        coords[2] = (float) x1;
        coords[3] = (float) y1;
        curState.transform.transform(coords, 0, coords, 0, 2);
        path.quadTo(coords[0], coords[1], coords[2], coords[3]);
        markPathDirty();
    }

    /**
     * Issues a bezierCurveTo command for the current path.
     * 
     * @param xc1 
     * @param yc1
     * @param y1
     * @param xc2 
     * @param yc2
     * @param x1  
     */
    public void bezierCurveTo(double xc1, double yc1, double xc2, double yc2, double x1, double y1) {
        coords[0] = (float) xc1;
        coords[1] = (float) yc1;
        coords[2] = (float) xc2;
        coords[3] = (float) yc2;
        coords[4] = (float) x1;
        coords[5] = (float) y1;
        curState.transform.transform(coords, 0, coords, 0, 3);
        path.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
        markPathDirty();
    }

    /**
     * Issues a arcTo command for the current path.
     * 
     * @param x1 
     * @param y1
     * @param x2 
     * @param y2
     * @param radius  
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

    private static final Arc2D TEMP_ARC = new Arc2D();
    // Takes ESWNE angle in radians with no limits
    // Returns ENWSE angle in degrees 0 <= a < 360
//    private static float getNormDegrees(double radians) {
//        float deg = (float) Math.toDegrees(-radians) % 360.0f;
//        if (deg < 0.0f) {
//            deg += 360.0f;
//        }
//        return deg;
//    }
    /**
     * Like HTML5 Canvas arc() function, but we use Euclidean degrees rather
     * than screen-oriented radians.  Euclidean orientation sweeps from East
     * to North, then West, then South, then back to East whereas the screen
     * oriented system that HTML5 Canvas uses sweeps E->S->W->N->E.
     * @param centerX 
     * @param centerY 
     * @param radiusX 
     * @param startAngle
     * @param radiusY 
     * @param length  
     */
    /*
    private void arcHTML5(double cx, double cy, double radius,
                    double startAngle, double endAngle,
                    boolean anticlockwise)
    {
        // HTML5 Canvas.arc() sweeps angles in radians from E->S->W->N->E
        // Arc2D sweeps angles in degrees from E->N->W->S->E
        float sa = getNormDegrees(startAngle);
        float extent;
        if (startAngle == endAngle) {
            extent = 0.0f;
        } else {
            float ea = getNormDegrees(endAngle);
            // sa,ea are now degrees in ENWSE system 0 <= a < 360
            extent = ea - sa;
            if (anticlockwise) {
                // extent must be 0 <= extent <= 360
                if (startAngle - endAngle > 2.0 * Math.PI) {
                    extent = 360.0f;
                } else {
                    if (extent <= 0f) extent += 360.0f;
                }
            } else {
                // extent must be 0 >= extent >= -360
                if (endAngle - startAngle > 2.0 * Math.PI) {
                    extent = -360.0f;
                } else {
                    if (extent >= 0f) extent -= 360.0f;
                }
            }
        }
        TEMP_ARC.setArc((float) (cx - radius), (float) (cy - radius),
                        (float) (radius * 2.0), (float) (radius * 2.0),
                        sa, extent, Arc2D.OPEN);
        path.append(TEMP_ARC.getPathIterator(curState.transform), true);
    }
    */

    public void arc(double centerX, double centerY,
                    double radiusX, double radiusY,
                    double startAngle, double length)
    {
        TEMP_ARC.setArc((float)(centerX - radiusX), // x
                        (float)(centerY - radiusY), // y
                        (float)(radiusX * 2.0), // w
                        (float)(radiusY * 2.0), // h
                        (float)startAngle,
                        (float)length,
                        Arc2D.OPEN);
        path.append(TEMP_ARC.getPathIterator(curState.transform), true);
        markPathDirty();
    }

    /**
     * Strokes a rectangle using a path.
     * 
     * @param x
     * @param y
     * @param w
     * @param h
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
     * 
     * @param svgpath the SVG Path string to be used.
     */
    public void appendSVGPath(String svgpath) {
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
        p2d.appendSVGPath(svgpath);
        PathIterator pi = p2d.getPathIterator(curState.transform);
        if (skipMoveto) {
            // We need to delete the initial moveto and let the path
            // extend from the actual existing geometry.
            pi.next();
        }
        path.append(pi, false);
    }

    /**
     * Closes the path.
     */
    public void closePath() {
        path.closePath();
        markPathDirty();
    }

    /**
     * Fills the path with the current fill paint attribute.
     */
    public void fill() {
        writePath(PGCanvas.FILL_PATH);
    }

    /**
     * Strokes the path with the current stroke paint attribute.
     */
    public void stroke() {
        writePath(PGCanvas.STROKE_PATH);
    }

    /**
     * Clips using the current path 
     */
    public void clip() {
        Path2D clip = new Path2D(path);
        clipStack.addLast(clip);
        curState.numClipPaths++;
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(PGCanvas.PUSH_CLIP);
        buf.putObject(clip);
    }

    /**
     * Returns true of the the given x,y point is touching the path.
     * 
     * @param x
     * @param y
     * @return
     */
    public boolean isPointInPath(double x, double y) {
        // TODO: HTML5 considers points on the path to be inside, but we
        // implement a halfin-halfout approach...
        return path.contains((float) x, (float) y);
    }

    /**
     * Clears the canvas with the current fill color attribute.
     * 
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public void clearRect(double x, double y, double w, double h) {
        if (w != 0 && h != 0) {
            writeOp4(x, y, w, h, PGCanvas.CLEAR_RECT);
        }
    }

    /**
     * Fills a rectangle using the current Fill paint attribute.
     * 
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public void fillRect(double x, double y, double w, double h) {
        if (w != 0 && h != 0) {
            writeOp4(x, y, w, h, PGCanvas.FILL_RECT);
        }
    }

    /**
     * Strokes a rectangle using the current stroke paint attribute.
     * 
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public void strokeRect(double x, double y, double w, double h) {
        if (w != 0 || h != 0) {
            writeOp4(x, y, w, h, PGCanvas.STROKE_RECT);
        }
    }

    /**
     * Fills an Oval using the current Fill paint attribute.
     * 
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public void fillOval(double x, double y, double w, double h) {
        if (w != 0 && h != 0) {
            writeOp4(x, y, w, h, PGCanvas.FILL_OVAL);
        }
    }

    /**
     * Strokes a rectangle using the current stroke paint attribute.
     * 
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public void strokeOval(double x, double y, double w, double h) {
        if (w != 0 || h != 0) {
            writeOp4(x, y, w, h, PGCanvas.STROKE_OVAL);
        }
    }

    /**
     * Fills an Arc using the current Fill paint attribute.
     * 
     * @param x
     * @param y
     * @param w
     * @param h
     * @param startAngle
     * @param arcExtent
     * @param closure
     */
    public void fillArc(double x, double y, double w, double h,
                        double startAngle, double arcExtent, ArcType closure)
    {
        if (w != 0 && h != 0) {
            writeArcType(closure);
            writeOp6(x, y, w, h, startAngle, arcExtent, PGCanvas.FILL_ARC);
        }
    }

    /**
     * Strokes an Arc using the current stroke paint attribute.
     * 
     * @param x
     * @param y
     * @param w
     * @param h
     * @param startAngle
     * @param arcExtent
     * @param closure
     */
    public void strokeArc(double x, double y, double w, double h,
                        double startAngle, double arcExtent, ArcType closure)
    {
        if (w != 0 && h != 0) {
            writeArcType(closure);
            writeOp6(x, y, w, h, startAngle, arcExtent, PGCanvas.STROKE_ARC);
        }
    }

    /**
     * Fills a rounded rectangle using the current fill paint attribute.
     * 
     * @param x
     * @param y
     * @param w
     * @param h
     * @param arcWidth
     * @param arcHeight
     */
    public void fillRoundRect(double x, double y, double w, double h,
                              double arcWidth, double arcHeight)
    {
        if (w != 0 && h != 0) {
            writeOp6(x, y, w, h, arcWidth, arcHeight, PGCanvas.FILL_ROUND_RECT);
        }
    }

    /**
     * Strokes a rounded rectangle using the current Stroke paint attribute.
     * 
     * @param x
     * @param y
     * @param w
     * @param h
     * @param arcWidth
     * @param arcHeight
     */
    public void strokeRoundRect(double x, double y, double w, double h,
                              double arcWidth, double arcHeight)
    {
        if (w != 0 && h != 0) {
            writeOp6(x, y, w, h, arcWidth, arcHeight, PGCanvas.STROKE_ROUND_RECT);
        }
    }

    /**
     * Strokes a line using the current Stroke paint attribute.
     * 
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     */
    public void strokeLine(double x1, double y1, double x2, double y2) {
        writeOp4(x1, y1, x2, y2, PGCanvas.STROKE_LINE);
    }

    /**
     * Fills a polygon with the given points using the currently set fill paint
     * attribute.
     * 
     * @param xPoints
     * @param yPoints
     * @param nPoints
     */
    public void fillPolygon(double xPoints[], double yPoints[], int nPoints) {
        if (nPoints >= 3) {
            writePoly(xPoints, yPoints, nPoints, true, PGCanvas.FILL_PATH);
        }
    }

    /**
     * Strokes a polygon with the given points using the currently set stroke paint
     * attribute.
     * 
     * @param xPoints
     * @param yPoints
     * @param nPoints
     */
    public void strokePolygon(double xPoints[], double yPoints[], int nPoints) {
        if (nPoints >= 2) {
            writePoly(xPoints, yPoints, nPoints, true, PGCanvas.STROKE_PATH);
        }
    }

    /**
     * Draws a polyline with the given points using the currently set stroke 
     * paint attribute.
     * 
     * @param xPoints
     * @param yPoints
     * @param nPoints
     */
    public void strokePolyline(double xPoints[], double yPoints[], int nPoints) {
        if (nPoints >= 2) {
            writePoly(xPoints, yPoints, nPoints, false, PGCanvas.STROKE_PATH);
        }
    }

    /**
     * Draws an image at the given x, y position using the width
     * and height of the given image.
     * 
     * @param img
     * @param x
     * @param y
     */
    public void drawImage(Image img, double x, double y) {
        double sw = img.getWidth();
        double sh = img.getHeight();
        writeImage(img, x, y, sw, sh);
    }

    /**
     * Draws an image into the given destination rectangle of the canvas.
     * 
     * @param img
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public void drawImage(Image img, double x, double y, double w, double h) {
        writeImage(img, x, y, w, h);
    }

    /**
     * Draws the current source rectangle of the given image to the given 
     * destination rectangle of the Canvas.
     * 
     * @param img
     * @param sx
     * @param sy
     * @param sw
     * @param sh
     * @param dx
     * @param dy
     * @param dw
     * @param dh
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
     * object will be in device space since they refer directly to pixels.
     * 
     * @return the {@code PixelWriter} for modifying the pixels of this
     *         {@code Canvas}
     */
    public PixelWriter getPixelWriter() {
        if (writer == null) {
            writer = new PixelWriter() {
                @Override
                public PixelFormat getPixelFormat() {
                    return PixelFormat.getByteBgraPreInstance();
                }

                @Override
                public void setArgb(int x, int y, int argb) {
                    GrowableDataBuffer buf = getBuffer();
                    buf.putByte(PGCanvas.PUT_ARGB);
                    buf.putInt(x);
                    buf.putInt(y);
                    buf.putInt(argb);
                }

                @Override
                public void setColor(int x, int y, Color c) {
                    int a = (int) Math.round(c.getOpacity() * 255.0);
                    int r = (int) Math.round(c.getRed() * 255.0);
                    int g = (int) Math.round(c.getGreen() * 255.0);
                    int b = (int) Math.round(c.getBlue() * 255.0);
                    setArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                }

                @Override
                public <T extends Buffer> void
                    setPixels(int x, int y, int w, int h,
                              PixelFormat<T> pixelformat,
                              T buffer, int scan)
                {
                    for (int j = 0; j < h; j++) {
                        for (int i = 0; i < w; i++) {
                            int argb = pixelformat.getArgb(buffer, i, j, scan);
                            setArgb(x + i, y + j, argb);
                        }
                    }
                }

                @Override
                public void setPixels(int x, int y, int w, int h,
                                      PixelFormat<ByteBuffer> pixelformat,
                                      byte[] buffer, int offset, int scanlineStride)
                {
                    ByteBuffer bytebuf = ByteBuffer.wrap(buffer);
                    bytebuf.position(offset);
                    setPixels(x, y, w, h, pixelformat, bytebuf, scanlineStride);
                }

                @Override
                public void setPixels(int x, int y, int w, int h,
                                      PixelFormat<IntBuffer> pixelformat,
                                      int[] buffer, int offset, int scanlineStride)
                {
                    IntBuffer bytebuf = IntBuffer.wrap(buffer);
                    bytebuf.position(offset);
                    setPixels(x, y, w, h, pixelformat, bytebuf, scanlineStride);
                }

                @Override
                public void setPixels(int dstx, int dsty, int w, int h,
                                      PixelReader reader, int srcx, int srcy)
                {
                    for (int j = 0; j < h; j++) {
                        for (int i = 0; i < w; i++) {
                            int argb = reader.getArgb(srcx + i, srcy + j);
                            setArgb(dstx + i, dsty + j, argb);
                        }
                    }
                }
            };
        }
        return writer;
    }

    /**
     * Sets the effect to be applied after the next draw call, or null to
     * disable effects.
     * @param e the effect to use, or null to disable effects
     */
    public void setEffect(Effect e) {
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(PGCanvas.EFFECT);
        if (e == null) {
            curState.effect = null;
            buf.putObject(null);
        } else {
            curState.effect = e.impl_copy();
            curState.effect.impl_sync();
            buf.putObject(curState.effect.impl_getImpl());
        }
    }
    
    /**
     * Gets a copy of the effect to be applied after the next draw call.
     * A null return value means that no effect will be applied after future
     * rendering calls.
     * @param e an {@code Effect} object that may be used to store the
     *        copy of the current effect, if it is of a compatible type
     * @return the current effect used for all rendering calls,
     *         or null if there is no current effect
     */
    public Effect getEffect(Effect e) {
        return curState.effect == null ? null : curState.effect.impl_copy();
    }

    /**
     * Applies the given effect before the next.
     * @param e
     */
    public void applyEffect(Effect e) {
        GrowableDataBuffer buf = getBuffer();
        buf.putByte(PGCanvas.FX_APPLY_EFFECT);
        Effect effect = e.impl_copy();
        effect.impl_sync();
        buf.putObject(effect.impl_getImpl());
    }
}
