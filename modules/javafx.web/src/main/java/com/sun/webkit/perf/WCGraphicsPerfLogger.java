/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.perf;

import java.nio.ByteBuffer;

import com.sun.javafx.logging.PlatformLogger;
import com.sun.prism.paint.Color;
import com.sun.webkit.graphics.Ref;
import com.sun.webkit.graphics.RenderTheme;
import com.sun.webkit.graphics.ScrollBarTheme;
import com.sun.webkit.graphics.WCFont;
import com.sun.webkit.graphics.WCGradient;
import com.sun.webkit.graphics.WCGraphicsContext;
import com.sun.webkit.graphics.WCIcon;
import com.sun.webkit.graphics.WCImage;
import com.sun.webkit.graphics.WCPath;
import com.sun.webkit.graphics.WCPoint;
import com.sun.webkit.graphics.WCRectangle;
import com.sun.webkit.graphics.WCTransform;

public final class WCGraphicsPerfLogger extends WCGraphicsContext {
    private static final PlatformLogger log = PlatformLogger.getLogger(WCGraphicsPerfLogger.class.getName());

    private static final PerfLogger logger = PerfLogger.getLogger(log);

    private final WCGraphicsContext gc;

    public WCGraphicsPerfLogger(WCGraphicsContext gc) {
        this.gc = gc;
    }

    public synchronized static boolean isEnabled() {
        return logger.isEnabled();
    }

    public static void log() {
        logger.log();
    }

    public static void reset() {
        logger.reset();
    }

    @Override
    public Object getPlatformGraphics() {
        return gc.getPlatformGraphics();
    }

    @Override
    public boolean isValid() {
        return gc.isValid();
    }

    @Override
    public void drawString(WCFont f, int[] glyphs,
                           float[] advanceDXY,
                           float x, float y)
    {
        logger.resumeCount("DRAWSTRING_GV");
        gc.drawString(f, glyphs, advanceDXY, x, y);
        logger.suspendCount("DRAWSTRING_GV");
    }

    @Override
    public void strokeRect(float x, float y, float w, float h, float lengthWidth) {
        logger.resumeCount("STROKERECT_FFFFF");
        gc.strokeRect(x,y,w,h,lengthWidth);
        logger.suspendCount("STROKERECT_FFFFF");
    }

    @Override
    public void fillRect(float x, float y, float w, float h, Color color) {
        logger.resumeCount("FILLRECT_FFFFI");
        gc.fillRect(x, y, w, h, color);
        logger.suspendCount("FILLRECT_FFFFI");
    }

    @Override public void fillRoundedRect(float x, float y, float w, float h,
            float topLeftW, float topLeftH, float topRightW, float topRightH,
            float bottomLeftW, float bottomLeftH, float bottomRightW, float bottomRightH,
            Color color) {
        logger.resumeCount("FILL_ROUNDED_RECT");
        gc.fillRoundedRect(x, y, w, h, topLeftW, topLeftH, topRightW, topRightH,
                bottomLeftW, bottomLeftH, bottomRightW, bottomRightH, color);
        logger.suspendCount("FILL_ROUNDED_RECT");
    }

    @Override
    public void clearRect(float x, float y, float w, float h) {
        logger.resumeCount("CLEARRECT");
        gc.clearRect(x, y, w, h);
        logger.suspendCount("CLEARRECT");
    }

    @Override
    public void setFillColor(Color color) {
        logger.resumeCount("SETFILLCOLOR");
        gc.setFillColor(color);
        logger.suspendCount("SETFILLCOLOR");
    }

    @Override
    public void setFillGradient(WCGradient gradient) {
        logger.resumeCount("SET_FILL_GRADIENT");
        gc.setFillGradient(gradient);
        logger.suspendCount("SET_FILL_GRADIENT");
    }

    @Override
    public void setTextMode(boolean fill, boolean stroke, boolean clip) {
        logger.resumeCount("SET_TEXT_MODE");
        gc.setTextMode(fill, stroke, clip);
        logger.suspendCount("SET_TEXT_MODE");
    }

    @Override
    public void setFontSmoothingType(int fontSmoothingType) {
        logger.resumeCount("SET_FONT_SMOOTHING_TYPE");
        gc.setFontSmoothingType(fontSmoothingType);
        logger.suspendCount("SET_FONT_SMOOTHING_TYPE");
    }

    @Override
    public int getFontSmoothingType() {
        logger.resumeCount("GET_FONT_SMOOTHING_TYPE");
        int n = gc.getFontSmoothingType();
        logger.suspendCount("GET_FONT_SMOOTHING_TYPE");
        return n;
    }

    @Override
    public void setStrokeStyle(int style) {
        logger.resumeCount("SETSTROKESTYLE");
        gc.setStrokeStyle(style);
        logger.suspendCount("SETSTROKESTYLE");
    }

    @Override
    public void setStrokeColor(Color color) {
        logger.resumeCount("SETSTROKECOLOR");
        gc.setStrokeColor(color);
        logger.suspendCount("SETSTROKECOLOR");
    }

    @Override
    public void setStrokeWidth(float width) {
        logger.resumeCount("SETSTROKEWIDTH");
        gc.setStrokeWidth(width);
        logger.suspendCount("SETSTROKEWIDTH");
    }

    @Override
    public void setStrokeGradient(WCGradient gradient) {
        logger.resumeCount("SET_STROKE_GRADIENT");
        gc.setStrokeGradient(gradient);
        logger.suspendCount("SET_STROKE_GRADIENT");
    }

    @Override
    public void setLineDash(float offset, float... sizes) {
        logger.resumeCount("SET_LINE_DASH");
        gc.setLineDash(offset, sizes);
        logger.suspendCount("SET_LINE_DASH");
    }

    @Override
    public void setLineCap(int lineCap) {
        logger.resumeCount("SET_LINE_CAP");
        gc.setLineCap(lineCap);
        logger.suspendCount("SET_LINE_CAP");
    }

    @Override
    public void setLineJoin(int lineJoin) {
        logger.resumeCount("SET_LINE_JOIN");
        gc.setLineJoin(lineJoin);
        logger.suspendCount("SET_LINE_JOIN");
    }

    @Override
    public void setMiterLimit(float miterLimit) {
        logger.resumeCount("SET_MITER_LIMIT");
        gc.setMiterLimit(miterLimit);
        logger.suspendCount("SET_MITER_LIMIT");
    }

    @Override
    public void setShadow(float dx, float dy, float blur, Color color) {
        logger.resumeCount("SETSHADOW");
        gc.setShadow(dx, dy, blur, color);
        logger.suspendCount("SETSHADOW");
    }

    @Override
    public void drawPolygon(WCPath path, boolean shouldAntialias) {
        logger.resumeCount("DRAWPOLYGON");
        gc.drawPolygon(path, shouldAntialias);
        logger.suspendCount("DRAWPOLYGON");
    }

    @Override
    public void drawLine(int x0, int y0, int x1, int y1) {
        logger.resumeCount("DRAWLINE");
        gc.drawLine(x0, y0, x1, y1);
        logger.suspendCount("DRAWLINE");
    }

    @Override
    public void drawImage(WCImage img,
                          float dstx, float dsty, float dstw, float dsth,
                          float srcx, float srcy, float srcw, float srch) {
        logger.resumeCount("DRAWIMAGE");
        gc.drawImage(img, dstx, dsty, dstw, dsth, srcx, srcy, srcw, srch);
        logger.suspendCount("DRAWIMAGE");
    }

    @Override
    public void drawIcon(WCIcon icon, int x, int y) {
        logger.resumeCount("DRAWICON");
        gc.drawIcon(icon, x, y);
        logger.suspendCount("DRAWICON");
    }

    @Override
    public void drawPattern(WCImage texture, WCRectangle srcRect,
            WCTransform patternTransform, WCPoint phase,
            WCRectangle destRect) {
        logger.resumeCount("DRAWPATTERN");
        gc.drawPattern(texture, srcRect, patternTransform, phase, destRect);
        logger.suspendCount("DRAWPATTERN");
    }

    @Override
    public void translate(float x, float y) {
        logger.resumeCount("TRANSLATE");
        gc.translate(x, y);
        logger.suspendCount("TRANSLATE");
    }

    @Override
    public void scale(float scaleX, float scaleY) {
        logger.resumeCount("SCALE");
        gc.scale(scaleX, scaleY);
        logger.suspendCount("SCALE");
    }

    @Override
    public void rotate(float radians) {
        logger.resumeCount("ROTATE");
        gc.rotate(radians);
        logger.suspendCount("ROTATE");
    }

    @Override
    public void saveState() {
        logger.resumeCount("SAVESTATE");
        gc.saveState();
        logger.suspendCount("SAVESTATE");
    }

    @Override
    public void restoreState() {
        logger.resumeCount("RESTORESTATE");
        gc.restoreState();
        logger.suspendCount("RESTORESTATE");
    }

    @Override
    public void setClip(WCPath path, boolean isOut) {
        logger.resumeCount("CLIP_PATH");
        gc.setClip(path, isOut);
        logger.suspendCount("CLIP_PATH");
    }

    @Override
    public void setClip(WCRectangle clip) {
        logger.resumeCount("SETCLIP_R");
        gc.setClip(clip);
        logger.suspendCount("SETCLIP_R");
    }

    @Override
    public void setClip(int cx, int cy, int cw, int ch) {
        logger.resumeCount("SETCLIP_IIII");
        gc.setClip(cx, cy, cw, ch);
        logger.suspendCount("SETCLIP_IIII");
    }

    @Override
    public WCRectangle getClip() {
        logger.resumeCount("SETCLIP_IIII");
        WCRectangle r = gc.getClip();
        logger.suspendCount("SETCLIP_IIII");
        return r;
    }

    @Override
    public void drawRect(int x, int y, int w, int h) {
        logger.resumeCount("DRAWRECT");
        gc.drawRect(x, y, w, h);
        logger.suspendCount("DRAWRECT");
    }

    @Override
    public void setComposite(int composite) {
        logger.resumeCount("SETCOMPOSITE");
        gc.setComposite(composite);
        logger.suspendCount("SETCOMPOSITE");
    }

    @Override
    public void strokeArc(int x, int y, int w, int h, int startAngle,
                          int angleSpan) {
        logger.resumeCount("STROKEARC");
        gc.strokeArc(x, y, w, h, startAngle, angleSpan);
        logger.suspendCount("STROKEARC");
    }

    @Override
    public void drawEllipse(int x, int y, int w, int h) {
        logger.resumeCount("DRAWELLIPSE");
        gc.drawEllipse(x, y, w, h);
        logger.suspendCount("DRAWELLIPSE");
    }

    @Override
    public void drawFocusRing(int x, int y, int w, int h, Color color) {
        logger.resumeCount("DRAWFOCUSRING");
        gc.drawFocusRing(x, y, w, h, color);
        logger.suspendCount("DRAWFOCUSRING");
    }

    @Override
    public void setAlpha(float alpha) {
        logger.resumeCount("SETALPHA");
        gc.setAlpha(alpha);
        logger.suspendCount("SETALPHA");
    }

    @Override
    public float getAlpha() {
        logger.resumeCount("GETALPHA");
        float a = gc.getAlpha();
        logger.suspendCount("GETALPHA");
        return a;
    }

    @Override
    public void beginTransparencyLayer(float opacity) {
        logger.resumeCount("BEGINTRANSPARENCYLAYER");
        gc.beginTransparencyLayer(opacity);
        logger.suspendCount("BEGINTRANSPARENCYLAYER");
    }

    @Override
    public void endTransparencyLayer() {
        logger.resumeCount("ENDTRANSPARENCYLAYER");
        gc.endTransparencyLayer();
        logger.suspendCount("ENDTRANSPARENCYLAYER");
    }

    @Override
    public void drawString(WCFont f, String str, boolean rtl,
                           int from, int to,
                           float x, float y)
    {
        logger.resumeCount("DRAWSTRING");
        gc.drawString(f, str, rtl, from, to, x, y);
        logger.suspendCount("DRAWSTRING");
    }

    @Override
    public void strokePath(WCPath path) {
        logger.resumeCount("STROKE_PATH");
        gc.strokePath(path);
        logger.suspendCount("STROKE_PATH");
    }

    @Override
    public void fillPath(WCPath path) {
        logger.resumeCount("FILL_PATH");
        gc.fillPath(path);
        logger.suspendCount("FILL_PATH");
    }

    @Override
    public WCImage getImage() {
        logger.resumeCount("GETIMAGE");
        WCImage res = gc.getImage();
        logger.suspendCount("GETIMAGE");
        return res;
    }

    @Override
    public void drawWidget(RenderTheme theme, Ref widget, int x, int y) {
        logger.resumeCount("DRAWWIDGET");
        gc.drawWidget(theme, widget, x, y);
        logger.suspendCount("DRAWWIDGET");
    }

    @Override
    public void drawScrollbar(ScrollBarTheme theme, Ref widget,
                              int x, int y, int pressedPart, int hoveredPart)
    {
        logger.resumeCount("DRAWSCROLLBAR");
        gc.drawScrollbar(theme, widget, x, y, pressedPart, hoveredPart);
        logger.suspendCount("DRAWSCROLLBAR");
    }

    @Override
    public void dispose() {
        logger.resumeCount("DISPOSE");
        gc.dispose();
        logger.suspendCount("DISPOSE");
    }

    @Override
    public void flush() {
        logger.resumeCount("FLUSH");
        gc.flush();
        logger.suspendCount("FLUSH");
    }

    @Override
    public void setPerspectiveTransform(WCTransform t) {
        logger.resumeCount("SETPERSPECTIVETRANSFORM");
        gc.setPerspectiveTransform(t);
        logger.suspendCount("SETPERSPECTIVETRANSFORM");
    }

    @Override
    public void setTransform(WCTransform t) {
        logger.resumeCount("SETTRANSFORM");
        gc.setTransform(t);
        logger.suspendCount("SETTRANSFORM");
    }

    @Override
    public WCTransform getTransform() {
        logger.resumeCount("GETTRANSFORM");
        WCTransform t = gc.getTransform();
        logger.suspendCount("GETTRANSFORM");
        return t;
    }

    @Override
    public void concatTransform(WCTransform t) {
        logger.resumeCount("CONCATTRANSFORM");
        gc.concatTransform(t);
        logger.suspendCount("CONCATTRANSFORM");
    }

    @Override
    public void drawBitmapImage(ByteBuffer image, int x, int y, int w, int h) {
        logger.resumeCount("DRAWBITMAPIMAGE");
        gc.drawBitmapImage(image, x, y, w, h);
        logger.suspendCount("DRAWBITMAPIMAGE");
    }

    @Override
    public WCGradient createLinearGradient(WCPoint p1, WCPoint p2) {
        logger.resumeCount("CREATE_LINEAR_GRADIENT");
        WCGradient gradient = gc.createLinearGradient(p1, p2);
        logger.suspendCount("CREATE_LINEAR_GRADIENT");
        return gradient;
    }

    @Override
    public WCGradient createRadialGradient(WCPoint p1, float r1, WCPoint p2, float r2) {
        logger.resumeCount("CREATE_RADIAL_GRADIENT");
        WCGradient gradient = gc.createRadialGradient(p1, r1, p2, r2);
        logger.suspendCount("CREATE_RADIAL_GRADIENT");
        return gradient;
    }
}
