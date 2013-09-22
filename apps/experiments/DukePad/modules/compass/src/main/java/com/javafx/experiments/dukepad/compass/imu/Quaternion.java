/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.compass.imu;

import com.sun.javafx.geom.Vec3d;

public class Quaternion {
    private double w;
    private double x;
    private double y;
    private double z;

    public final double getW() {
        return w;
    }
    
    public final double getX() {
        return x;
    }
    
    public final double getY() {
        return y;
    }
    
    public final double getZ() {
        return z;
    }
    
    public void setW(double w) {
        this.w = w;
    }
    
    public void setX(double x) {
        this.x = x;
    }
    
    public void setY(double y) {
        this.y = y;
    }
    
    public void setZ(double z) {
        this.z = z;
    }
    
    public void set(double w, double x, double y, double z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public void set(Quaternion q) {
        set(q.getW(), q.getX(), q.getY(), q.getZ());
    }

    public Quaternion() {
        set(1.0, 0.0, 0.0, 0.0);
    }
    
    public Quaternion(double w, double x, double y, double z) {
        set(w, x, y, z);
    }
    
    public Quaternion(Quaternion q) {
        set(q);
    }

    public double magnitude() {
        final double w = getW();
        final double x = getX();
        final double y = getY();
        final double z = getZ();

        return Math.sqrt(w * w + x * x + y * y + z * z);
    }

    public Quaternion normalize() {
        final double mag = magnitude();

        if (mag == 0.0) {
            return new Quaternion(0.0, 0.0, 0.0, 0.0);
        }

        return new Quaternion(
            getW() / mag,
            getX() / mag,
            getY() / mag,
            getZ() / mag);
    }
    
    public Quaternion conjugate() {
        return new Quaternion(getW(), -getX(), -getY(), -getZ());
    }

    public Quaternion multiply(Quaternion q) {
        Vec3d va = new Vec3d(getX(), getY(), getZ());
        Vec3d vb = new Vec3d(q.getX(), q.getY(), q.getZ());
        double dotAB = va.dot(vb);
        Vec3d crossAB = new Vec3d();
        crossAB.cross(va, vb);
	
        return new Quaternion(
            getW() * q.getW() - dotAB,
            getW() * vb.x + q.getW() * va.x + crossAB.x,
            getW() * vb.y + q.getW() * va.y + crossAB.y,
            getW() * vb.z + q.getW() * va.z + crossAB.z);
    }
    
    static Vec3d toEuler(Quaternion q) {
        Vec3d v = new Vec3d();
        
	// fix roll near poles with this tolerance
	double pole = Math.PI / 2.0 - 0.05;

	v.y = Math.asin(2.0 * (q.getW() * q.getY() - q.getX() * q.getZ()));

	if ((v.y < pole) && (v.y > -pole)) {
            v.x = Math.atan2(2.0 * (q.getY() * q.getZ() + q.getW() * q.getX()),
                1.0 - 2.0 * (q.getX() * q.getX() + q.getY() * q.getY()));
	}

	v.z = Math.atan2(2.0 * (q.getX() * q.getY() + q.getW() * q.getZ()),
            1.0 - 2.0 * (q.getY() * q.getY() + q.getZ() * q.getZ()));
        
        return v;
    }

    static Quaternion fromEuler(Vec3d v) {
	double cosX2 = Math.cos(v.x / 2.0);
	double sinX2 = Math.sin(v.x / 2.0);
	double cosY2 = Math.cos(v.y / 2.0);
	double sinY2 = Math.sin(v.y / 2.0);
	double cosZ2 = Math.cos(v.z / 2.0);
	double sinZ2 = Math.sin(v.z / 2.0);
        
        Quaternion q = new Quaternion(
            cosX2 * cosY2 * cosZ2 + sinX2 * sinY2 * sinZ2,
            sinX2 * cosY2 * cosZ2 - cosX2 * sinY2 * sinZ2,
            cosX2 * sinY2 * cosZ2 + sinX2 * cosY2 * sinZ2,
            cosX2 * cosY2 * sinZ2 - sinX2 * sinY2 * cosZ2);

        return q.normalize();
    }
    
    @Override public String toString() {
        return "Quaternion [w = " + getW() + ", x = " + getX() + ", y = " + getY() + ", z = " + getZ() + "]";
    }
}
