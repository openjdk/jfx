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
package com.sun.javafx.ext.device.ios.sensors;


import java.util.ArrayList;
import java.util.List;

public class IOSLocationManager {

    public interface HeadingListener {
        void headingUpdated(
            final double magneticHeading,
            final double trueHeading,
            final double magneticFieldX,
            final double magneticFieldY,
            final double magneticFieldZ);
    }

    public interface LocationListener {
        void locationUpdated(
            final double latitude,
            final double longitude,
            final double altitude,
            final double course,
            final double speed);
    }

    static {
        _init();
    }

    private static native void _init();

    public static native boolean isHeadingAvailable();


    private static List<HeadingListener> headingListeners = new ArrayList<HeadingListener>();

    public static void addHeadingListener(final HeadingListener headingListener) {
        headingListeners.add(headingListener);
    }

    public static void removeHeadingListener(final HeadingListener headingListener) {
        headingListeners.remove(headingListener);
    }


    private static List<LocationListener> locationListeners = new ArrayList<LocationListener>();

    public static void addLocationListener(final LocationListener locationListener) {
        locationListeners.add(locationListener);
    }

    public static void removeLocationListener(final LocationListener locationListener) {
        locationListeners.remove(locationListener);
    }


    private static void didUpdateHeading(
        final double magneticHeading,
        final double trueHeading,
        final double magneticFieldX,
        final double magneticFieldY,
        final double magneticFieldZ) {
        for (HeadingListener headingListener : headingListeners) {
            headingListener.headingUpdated(
                magneticHeading,
                trueHeading,
                magneticFieldX,
                magneticFieldY,
                magneticFieldZ
            );
        }
    }

    private static void didUpdateLocation(
        final double latitude,
        final double longiude,
        final double altitude,
        final double course,
        final double speed) {
        for (LocationListener locationListener : locationListeners) {
            locationListener.locationUpdated(
                    latitude,
                    longiude,
                    altitude,
                    course,
                    speed
            );
        }
    }
}
