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

import javafx.geometry.Point3D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;

import java.util.Random;

public class IMUConfigurator {
    public static void calibrate(Mpu9150 mpu9150) throws NumberFormatException {
        CalData accelCal = new CalData();

        final double K = 3.4587251439;
        int ignore = 0;
        for (int i = 0; i < 3; i++) {
            String biasProperty = System.getProperty("accelCal." + i + ".bias");
            String offsetProperty = System.getProperty("accelCal." + i + ".offset");
            if (biasProperty == null) {
                if (offsetProperty == null) {
                    ignore++;
                    accelCal.offset[i] = 0;
                } else {
                    double offset = Double.parseDouble(offsetProperty);
                    System.out.println("offset[" + i + "] = " + offset);
                    accelCal.offset[i] = (long) (K * offset + 0.5);
                }
            } else {
                accelCal.offset[i] = Integer.parseInt(biasProperty);
            }
            System.out.println("accelCal.offset[" + i + "] = " + accelCal.offset[i]);
        }
        if (ignore < 3) {
            mpu9150.setAccelCal(accelCal);
        }
    }

    public static void quaternionToRotate(MpuData mpu, Rotate r, Rotate north) {
        double w = mpu.unfusedQuat.getW(), x = mpu.unfusedQuat.getY(), y = mpu.unfusedQuat.getZ(), z = mpu.unfusedQuat.getX();
        //double w = unfusedQuat.getW(), x = unfusedQuat.getX(), y = unfusedQuat.getY(), z = unfusedQuat.getZ();
        double angle = 2.0 * Math.atan2(Math.sqrt(x*x + y*y + z*z), w);
        Point3D axis;
        if (Math.abs(angle) > 0.0001) {
            double denom = Math.sin(angle/2.0);
            axis = new Point3D(x / denom, y / denom, z / denom);
        } else {
            axis = new Point3D(0.0, 0.0, 0.0);
        }
        r.setAngle(Math.toDegrees(angle));
        r.setAxis(axis);

        //since in our case, see assignment above, and using next vector
        //[q.w,q.x,q.y,q.z], we have x = q.y, y = q.z, z = q.x
        //azimuth = atan( (q.y*q.z + q.w*q.x) / (q.w*q.w + q.z*q.z - 0.5) )
        //double azimuth = Math.atan((mpu.fusedQuat.getX()*mpu.fusedQuat.getZ()+mpu.fusedQuat.getW()*mpu.fusedQuat.getY())/
        //        (mpu.fusedQuat.getW()*mpu.fusedQuat.getW()+mpu.fusedQuat.getX()*mpu.fusedQuat.getX()-0.5));
        double azimuth = Math.atan((mpu.fusedQuat.getY()*mpu.fusedQuat.getZ()+mpu.fusedQuat.getW()*mpu.fusedQuat.getX())/
                (mpu.fusedQuat.getW()*mpu.fusedQuat.getW()+mpu.fusedQuat.getX()*mpu.fusedQuat.getX()-0.5));
        System.out.println("x,y,z = " + mpu.magQuat.getX() + " " + mpu.magQuat.getY() + " " + mpu.magQuat.getZ());

        /*double magneticHeading = 0;
        if(mpu.magQuat.getY() > 0) {
            magneticHeading = 90 - Math.atan(mpu.magQuat.getX()/mpu.magQuat.getY()) * 180/Math.PI;
        } else if(mpu.magQuat.getY() < 0) {
            magneticHeading = 270 - Math.atan(mpu.magQuat.getX()/mpu.magQuat.getY()) * 180/Math.PI;
        } else if(mpu.magQuat.getY() == 0 && mpu.magQuat.getX() < 0) {
            magneticHeading = 180;
        }else if(mpu.magQuat.getY() == 0 && mpu.magQuat.getX() > 0) {
            magneticHeading = 0;
        } */


        north.setAngle(Math.toDegrees(azimuth));
        System.out.println("north.getAngle() = " + north.getAngle());
        /*for(int i = 0; i < mpu.rawMag.length; i ++)
            System.out.println("rawMag" + i + " = " + mpu.rawMag[i]);

        System.out.println("x,y,z = " + mpu.magQuat.getX() + " " + mpu.magQuat.getY() + " " + mpu.magQuat.getZ());*/

    }

    static double CalculateHeadingTiltCompensated(Quaternion mag, Quaternion acc)
    {
        // We are swapping the accelerometers axis as they are opposite to the compass the way we have them mounted.
        // We are swapping the signs axis as they are opposite.
        // Configure this for your setup.
        double accX = -acc.getY();
        double accY = -acc.getX();

        double rollRadians = Math.asin(accY);
        double pitchRadians = Math.asin(accX);

        // We cannot correct for tilt over 40 degrees with this algorthem, if the board is tilted as such, return 0.
        if(rollRadians > 0.78 || rollRadians < -0.78 || pitchRadians > 0.78 || pitchRadians < -0.78)
        {
            return 0;
        }

        // Some of these are used twice, so rather than computing them twice in the algorithem we precompute them before hand.
        double cosRoll = Math.cos(rollRadians);
        double sinRoll = Math.sin(rollRadians);
        double cosPitch = Math.cos(pitchRadians);
        double sinPitch = Math.sin(pitchRadians);

        // The tilt compensation algorithem.
        double Xh = mag.getX() * cosPitch + mag.getZ() * sinPitch;
        double Yh = mag.getX() * sinRoll * sinPitch + mag.getY() * cosRoll - mag.getZ() * sinRoll * cosPitch;

        double heading = Math.atan2(Yh, Xh);

        return heading;
    }

    static double CalculateHeadingNotTiltCompensated(Quaternion mag)
    {
        // Calculate heading when the magnetometer is level, then correct for signs of axis.
        double heading = Math.atan2(mag.getY(), mag.getX());
        return heading;
    }

    double RadiansToDegrees(float rads)
    {
        // Correct for when signs are reversed.
        if(rads < 0)
            rads += 2*Math.PI;

        // Check for wrap due to addition of declination.
        if(rads > 2*Math.PI)
            rads -= 2*Math.PI;

        // Convert radians to degrees for readability.
        double heading = rads * 180/Math.PI;

        return heading;
    }

    public static void quaternionToAffine(Quaternion q, Affine a) {
        double w = q.getW(), x = q.getX(), y = q.getY(), z = q.getZ();
        a.setToIdentity();
        a.setMxx(w*w + x*x - y*y - z*z);
        a.setMxy(2.0*(x*y - w*z));
        a.setMxz(2.0*(w*y + x*z));
        a.setMyx(2.0*(x*y + w*z));
        a.setMyy(w*w - x*x + y*y - z*z);
        a.setMyz(2.0*(y*z - w*x));
        a.setMzx(2.0*(x*z - w*y));
        a.setMzy(2.0*(w*x + y*z));
        a.setMzz(w*w - x*x - y*y + z*z);
    }
}
