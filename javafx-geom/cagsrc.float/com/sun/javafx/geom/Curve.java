/*
 * Copyright (c) 1998, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.geom;

import java.util.Vector;

public abstract class Curve {
    public static final int INCREASING = 1;
    public static final int DECREASING = -1;

    protected int direction;

    public static void insertMove(Vector curves, float x, float y) {
        curves.add(new Order0(x, y));
    }

    public static void insertLine(Vector curves,
                                  float x0, float y0,
                                  float x1, float y1)
    {
        if (y0 < y1) {
            curves.add(new Order1(x0, y0,
                                  x1, y1,
                                  INCREASING));
        } else if (y0 > y1) {
            curves.add(new Order1(x1, y1,
                                  x0, y0,
                                  DECREASING));
        } else {
            // Do not add horizontal lines
        }
    }

    public static void insertQuad(Vector curves,
                                  float x0, float y0,
                                  float coords[])
    {
        float y1 = coords[3];
        if (y0 > y1) {
            Order2.insert(curves, coords,
                          coords[2], y1,
                          coords[0], coords[1],
                          x0, y0,
                          DECREASING);
        } else if (y0 == y1 && y0 == coords[1]) {
            // Do not add horizontal lines
            return;
        } else {
            Order2.insert(curves, coords,
                          x0, y0,
                          coords[0], coords[1],
                          coords[2], y1,
                          INCREASING);
        }
    }

    public static void insertCubic(Vector curves,
                                   float x0, float y0,
                                   float coords[])
    {
        float y1 = coords[5];
        if (y0 > y1) {
            Order3.insert(curves, coords,
                          coords[4], y1,
                          coords[2], coords[3],
                          coords[0], coords[1],
                          x0, y0,
                          DECREASING);
        } else if (y0 == y1 && y0 == coords[1] && y0 == coords[3]) {
            // Do not add horizontal lines
            return;
        } else {
            Order3.insert(curves, coords,
                          x0, y0,
                          coords[0], coords[1],
                          coords[2], coords[3],
                          coords[4], y1,
                          INCREASING);
        }
    }

    public Curve(int direction) {
        this.direction = direction;
    }

    public final int getDirection() {
        return direction;
    }

    public final Curve getWithDirection(int direction) {
        return (this.direction == direction ? this : getReversedCurve());
    }

    public static float round(float v) {
        //return Math.rint(v*10)/10;
        return v;
    }

    public static int orderof(float x1, float x2) {
        if (x1 < x2) {
            return -1;
        }
        if (x1 > x2) {
            return 1;
        }
        return 0;
    }

    public static long signeddiffbits(float y1, float y2) {
        return (Float.floatToIntBits(y1) - Float.floatToIntBits(y2));
    }
    public static long diffbits(float y1, float y2) {
        return Math.abs(Float.floatToIntBits(y1) -
                        Float.floatToIntBits(y2));
    }
    public static float prev(float v) {
        return Float.intBitsToFloat(Float.floatToIntBits(v)-1);
    }
    public static float next(float v) {
        return Float.intBitsToFloat(Float.floatToIntBits(v)+1);
    }

    public String toString() {
        return ("Curve["+
                getOrder()+", "+
                ("("+round(getX0())+", "+round(getY0())+"), ")+
                controlPointString()+
                ("("+round(getX1())+", "+round(getY1())+"), ")+
                (direction == INCREASING ? "D" : "U")+
                "]");
    }

    public String controlPointString() {
        return "";
    }

    public abstract int getOrder();

    public abstract float getXTop();
    public abstract float getYTop();
    public abstract float getXBot();
    public abstract float getYBot();

    public abstract float getXMin();
    public abstract float getXMax();

    public abstract float getX0();
    public abstract float getY0();
    public abstract float getX1();
    public abstract float getY1();

    public abstract float XforY(float y);
    public abstract float TforY(float y);
    public abstract float XforT(float t);
    public abstract float YforT(float t);
    public abstract float dXforT(float t, int deriv);
    public abstract float dYforT(float t, int deriv);

    public abstract float nextVertical(float t0, float t1);

    public int crossingsFor(float x, float y) {
        if (y >= getYTop() && y < getYBot()) {
            if (x < getXMax() && (x < getXMin() || x < XforY(y))) {
                return 1;
            }
        }
        return 0;
    }

    public boolean accumulateCrossings(Crossings c) {
        float xhi = c.getXHi();
        if (getXMin() >= xhi) {
            return false;
        }
        float xlo = c.getXLo();
        float ylo = c.getYLo();
        float yhi = c.getYHi();
        float y0 = getYTop();
        float y1 = getYBot();
        float tstart, ystart, tend, yend;
        if (y0 < ylo) {
            if (y1 <= ylo) {
                return false;
            }
            ystart = ylo;
            tstart = TforY(ylo);
        } else {
            if (y0 >= yhi) {
                return false;
            }
            ystart = y0;
            tstart = 0;
        }
        if (y1 > yhi) {
            yend = yhi;
            tend = TforY(yhi);
        } else {
            yend = y1;
            tend = 1;
        }
        boolean hitLo = false;
        boolean hitHi = false;
        while (true) {
            float x = XforT(tstart);
            if (x < xhi) {
                if (hitHi || x > xlo) {
                    return true;
                }
                hitLo = true;
            } else {
                if (hitLo) {
                    return true;
                }
                hitHi = true;
            }
            if (tstart >= tend) {
                break;
            }
            tstart = nextVertical(tstart, tend);
        }
        if (hitLo) {
            c.record(ystart, yend, direction);
        }
        return false;
    }

    public abstract void enlarge(RectBounds r);

    public Curve getSubCurve(float ystart, float yend) {
        return getSubCurve(ystart, yend, direction);
    }

    public abstract Curve getReversedCurve();
    public abstract Curve getSubCurve(float ystart, float yend, int dir);

    public int compareTo(Curve that, float yrange[]) {
        /*
        System.out.println(this+".compareTo("+that+")");
        System.out.println("target range = "+yrange[0]+"=>"+yrange[1]);
        */
        float y0 = yrange[0];
        float y1 = yrange[1];
        y1 = (float) Math.min(Math.min(y1, this.getYBot()), that.getYBot());
        if (y1 <= yrange[0]) {
            System.err.println("this == "+this);
            System.err.println("that == "+that);
            System.out.println("target range = "+yrange[0]+"=>"+yrange[1]);
            throw new InternalError("backstepping from "+yrange[0]+" to "+y1);
        }
        yrange[1] = y1;
        if (this.getXMax() <= that.getXMin()) {
            if (this.getXMin() == that.getXMax()) {
                return 0;
            }
            return -1;
        }
        if (this.getXMin() >= that.getXMax()) {
            return 1;
        }
        // Parameter s for thi(s) curve and t for tha(t) curve
        // [st]0 = parameters for top of current section of interest
        // [st]1 = parameters for bottom of valid range
        // [st]h = parameters for hypothesis point
        // [d][xy]s = valuations of thi(s) curve at sh
        // [d][xy]t = valuations of tha(t) curve at th
        float s0 = this.TforY(y0);
        float ys0 = this.YforT(s0);
        if (ys0 < y0) {
            s0 = refineTforY(s0, ys0, y0);
            ys0 = this.YforT(s0);
        }
        float s1 = this.TforY(y1);
        if (this.YforT(s1) < y0) {
            s1 = refineTforY(s1, this.YforT(s1), y0);
            //System.out.println("s1 problem!");
        }
        float t0 = that.TforY(y0);
        float yt0 = that.YforT(t0);
        if (yt0 < y0) {
            t0 = that.refineTforY(t0, yt0, y0);
            yt0 = that.YforT(t0);
        }
        float t1 = that.TforY(y1);
        if (that.YforT(t1) < y0) {
            t1 = that.refineTforY(t1, that.YforT(t1), y0);
            //System.out.println("t1 problem!");
        }
        float xs0 = this.XforT(s0);
        float xt0 = that.XforT(t0);
        float scale = (float) Math.max(Math.abs(y0), Math.abs(y1));
        float ymin = (float) Math.max(scale * 1E-14, 1E-300);
        if (fairlyClose(xs0, xt0)) {
            float bump = ymin;
            float maxbump = (float) Math.min(ymin * 1E13, (y1 - y0) * .1);
            float y = y0 + bump;
            while (y <= y1) {
                if (fairlyClose(this.XforY(y), that.XforY(y))) {
                    if ((bump *= 2) > maxbump) {
                        bump = maxbump;
                    }
                } else {
                    y -= bump;
                    while (true) {
                        bump /= 2;
                        float newy = y + bump;
                        if (newy <= y) {
                            break;
                        }
                        if (fairlyClose(this.XforY(newy), that.XforY(newy))) {
                            y = newy;
                        }
                    }
                    break;
                }
                y += bump;
            }
            if (y > y0) {
                if (y < y1) {
                    yrange[1] = y;
                }
                return 0;
            }
        }
        //float ymin = y1 * 1E-14;
        if (ymin <= 0) {
            System.out.println("ymin = "+ymin);
        }
        /*
        System.out.println("s range = "+s0+" to "+s1);
        System.out.println("t range = "+t0+" to "+t1);
        */
        while (s0 < s1 && t0 < t1) {
            float sh = this.nextVertical(s0, s1);
            float xsh = this.XforT(sh);
            float ysh = this.YforT(sh);
            float th = that.nextVertical(t0, t1);
            float xth = that.XforT(th);
            float yth = that.YforT(th);
            /*
            System.out.println("sh = "+sh);
            System.out.println("th = "+th);
            */
        try {
            if (findIntersect(that, yrange, ymin, 0, 0,
                              s0, xs0, ys0, sh, xsh, ysh,
                              t0, xt0, yt0, th, xth, yth)) {
                break;
            }
        } catch (Throwable t) {
            System.err.println("Error: "+t);
            System.err.println("y range was "+yrange[0]+"=>"+yrange[1]);
            System.err.println("s y range is "+ys0+"=>"+ysh);
            System.err.println("t y range is "+yt0+"=>"+yth);
            System.err.println("ymin is "+ymin);
            return 0;
        }
            if (ysh < yth) {
                if (ysh > yrange[0]) {
                    if (ysh < yrange[1]) {
                        yrange[1] = ysh;
                    }
                    break;
                }
                s0 = sh;
                xs0 = xsh;
                ys0 = ysh;
            } else {
                if (yth > yrange[0]) {
                    if (yth < yrange[1]) {
                        yrange[1] = yth;
                    }
                    break;
                }
                t0 = th;
                xt0 = xth;
                yt0 = yth;
            }
        }
        float ymid = (yrange[0] + yrange[1]) / 2;
        /*
        System.out.println("final this["+s0+", "+sh+", "+s1+"]");
        System.out.println("final    y["+ys0+", "+ysh+"]");
        System.out.println("final that["+t0+", "+th+", "+t1+"]");
        System.out.println("final    y["+yt0+", "+yth+"]");
        System.out.println("final order = "+orderof(this.XforY(ymid),
                                                    that.XforY(ymid)));
        System.out.println("final range = "+yrange[0]+"=>"+yrange[1]);
        */
        /*
        System.out.println("final sx = "+this.XforY(ymid));
        System.out.println("final tx = "+that.XforY(ymid));
        System.out.println("final order = "+orderof(this.XforY(ymid),
                                                    that.XforY(ymid)));
        */
        return orderof(this.XforY(ymid), that.XforY(ymid));
    }

    public static final float TMIN = (float) 1E-3;

    public boolean findIntersect(Curve that, float yrange[], float ymin,
                                 int slevel, int tlevel,
                                 float s0, float xs0, float ys0,
                                 float s1, float xs1, float ys1,
                                 float t0, float xt0, float yt0,
                                 float t1, float xt1, float yt1)
    {
        /*
        String pad = "        ";
        pad = pad+pad+pad+pad+pad;
        pad = pad+pad;
        System.out.println("----------------------------------------------");
        System.out.println(pad.substring(0, slevel)+ys0);
        System.out.println(pad.substring(0, slevel)+ys1);
        System.out.println(pad.substring(0, slevel)+(s1-s0));
        System.out.println("-------");
        System.out.println(pad.substring(0, tlevel)+yt0);
        System.out.println(pad.substring(0, tlevel)+yt1);
        System.out.println(pad.substring(0, tlevel)+(t1-t0));
        */
        if (ys0 > yt1 || yt0 > ys1) {
            return false;
        }
        if (Math.min(xs0, xs1) > Math.max(xt0, xt1) ||
            Math.max(xs0, xs1) < Math.min(xt0, xt1))
        {
            return false;
        }
        // Bounding boxes intersect - back off the larger of
        // the two subcurves by half until they stop intersecting
        // (or until they get small enough to switch to a more
        //  intensive algorithm).
        if (s1 - s0 > TMIN) {
            float s = (s0 + s1) / 2;
            float xs = this.XforT(s);
            float ys = this.YforT(s);
            if (s == s0 || s == s1) {
                System.out.println("s0 = "+s0);
                System.out.println("s1 = "+s1);
                throw new InternalError("no s progress!");
            }
            if (t1 - t0 > TMIN) {
                float t = (t0 + t1) / 2;
                float xt = that.XforT(t);
                float yt = that.YforT(t);
                if (t == t0 || t == t1) {
                    System.out.println("t0 = "+t0);
                    System.out.println("t1 = "+t1);
                    throw new InternalError("no t progress!");
                }
                if (ys >= yt0 && yt >= ys0) {
                    if (findIntersect(that, yrange, ymin, slevel+1, tlevel+1,
                                      s0, xs0, ys0, s, xs, ys,
                                      t0, xt0, yt0, t, xt, yt)) {
                        return true;
                    }
                }
                if (ys >= yt) {
                    if (findIntersect(that, yrange, ymin, slevel+1, tlevel+1,
                                      s0, xs0, ys0, s, xs, ys,
                                      t, xt, yt, t1, xt1, yt1)) {
                        return true;
                    }
                }
                if (yt >= ys) {
                    if (findIntersect(that, yrange, ymin, slevel+1, tlevel+1,
                                      s, xs, ys, s1, xs1, ys1,
                                      t0, xt0, yt0, t, xt, yt)) {
                        return true;
                    }
                }
                if (ys1 >= yt && yt1 >= ys) {
                    if (findIntersect(that, yrange, ymin, slevel+1, tlevel+1,
                                      s, xs, ys, s1, xs1, ys1,
                                      t, xt, yt, t1, xt1, yt1)) {
                        return true;
                    }
                }
            } else {
                if (ys >= yt0) {
                    if (findIntersect(that, yrange, ymin, slevel+1, tlevel,
                                      s0, xs0, ys0, s, xs, ys,
                                      t0, xt0, yt0, t1, xt1, yt1)) {
                        return true;
                    }
                }
                if (yt1 >= ys) {
                    if (findIntersect(that, yrange, ymin, slevel+1, tlevel,
                                      s, xs, ys, s1, xs1, ys1,
                                      t0, xt0, yt0, t1, xt1, yt1)) {
                        return true;
                    }
                }
            }
        } else if (t1 - t0 > TMIN) {
            float t = (t0 + t1) / 2;
            float xt = that.XforT(t);
            float yt = that.YforT(t);
            if (t == t0 || t == t1) {
                System.out.println("t0 = "+t0);
                System.out.println("t1 = "+t1);
                throw new InternalError("no t progress!");
            }
            if (yt >= ys0) {
                if (findIntersect(that, yrange, ymin, slevel, tlevel+1,
                                  s0, xs0, ys0, s1, xs1, ys1,
                                  t0, xt0, yt0, t, xt, yt)) {
                    return true;
                }
            }
            if (ys1 >= yt) {
                if (findIntersect(that, yrange, ymin, slevel, tlevel+1,
                                  s0, xs0, ys0, s1, xs1, ys1,
                                  t, xt, yt, t1, xt1, yt1)) {
                    return true;
                }
            }
        } else {
            // No more subdivisions
            float xlk = xs1 - xs0;
            float ylk = ys1 - ys0;
            float xnm = xt1 - xt0;
            float ynm = yt1 - yt0;
            float xmk = xt0 - xs0;
            float ymk = yt0 - ys0;
            float det = xnm * ylk - ynm * xlk;
            if (det != 0) {
                float detinv = 1 / det;
                float s = (xnm * ymk - ynm * xmk) * detinv;
                float t = (xlk * ymk - ylk * xmk) * detinv;
                if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
                    s = s0 + s * (s1 - s0);
                    t = t0 + t * (t1 - t0);
                    if (s < 0 || s > 1 || t < 0 || t > 1) {
                        System.out.println("Uh oh!");
                    }
                    float y = (this.YforT(s) + that.YforT(t)) / 2;
                    if (y <= yrange[1] && y > yrange[0]) {
                        yrange[1] = y;
                        return true;
                    }
                }
            }
            //System.out.println("Testing lines!");
        }
        return false;
    }

    public float refineTforY(float t0, float yt0, float y0) {
        float t1 = 1;
        while (true) {
            float th = (t0 + t1) / 2;
            if (th == t0 || th == t1) {
                return t1;
            }
            float y = YforT(th);
            if (y < y0) {
                t0 = th;
                yt0 = y;
            } else if (y > y0) {
                t1 = th;
            } else {
                return t1;
            }
        }
    }

    public boolean fairlyClose(float v1, float v2) {
        return (Math.abs(v1 - v2) <
                4 * Math.ulp(Math.max(Math.abs(v1), Math.abs(v2))));
    }

    public abstract int getSegment(float coords[]);
}
