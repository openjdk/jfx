/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
package sensorstest;

import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class SensorsPanel extends Group {

    static Text text(final int x, final int y) {
        final Text text = new Text();
        text.setFont(Font.font("Courier", 16));
        text.setFill(Color.WHITE);
        text.setTranslateX(x);
        text.setTranslateY(y);
        return text;
    }

    static Text label(final int x, final int y, final String text) {
        final Text label = text(x, y);
        label.setText(text);
        return label;
    }

    public SensorsPanel() {
        final int DY = 0x0E;
        final int LX = 0x00;
        final int VX = 0xC0;
        int y = 0x18;

        final Text textLocation1       = label(LX, y += DY, "Location");
        final Text textLocation2       = label(LX, y += DY, "=================================");
        final Text textLatitude        = label(LX, y += DY, "Latitude");
        final Text textLatitudeValue   = text (VX, y);
        final Text textLongitude       = label(LX, y += DY, "Longitude");
        final Text textLongitudeValue  = text (VX, y);
        final Text textAltitude        = label(LX, y += DY, "Altitude");
        final Text textAltitudeValue   = text (VX, y);
        final Text textCourse          = label(LX, y += DY, "Course");
        final Text textCourseValue     = text (VX, y);
        final Text textSpeed           = label(LX, y += DY, "Speed");
        final Text textSpeedValue      = text (VX, y);
        y += DY;

        final Text textAccelerometer1  = label(LX, y += DY, "Accelerometer");
        final Text textAccelerometerA  = text (VX, y);
        final Text textAccelerometer2  = label(LX, y += DY, "=================================");
        final Text textAccX            = label(LX, y += DY, "Acceleration X");
        final Text textAccXValue       = text (VX, y);
        final Text textAccY            = label(LX, y += DY, "Acceleration Y");
        final Text textAccYValue       = text (VX, y);
        final Text textAccZ            = label(LX, y += DY, "Acceleration Z");
        final Text textAccZValue       = text (VX, y);
        y += DY;

        final Text textGyroscope1      = label(LX, y += DY, "Gyroscope");
        final Text textGyroscopeA      = text (VX, y);
        final Text textGyroscope2      = label(LX, y += DY, "=================================");
        final Text textRotX            = label(LX, y += DY, "Rotation X");
        final Text textRotXValue       = text (VX, y);
        final Text textRotY            = label(LX, y += DY, "Rotation Y");
        final Text textRotYValue       = text (VX, y);
        final Text textRotZ            = label(LX, y += DY, "Rotation Z");
        final Text textRotZValue       = text (VX, y);
        y += DY;

        final Text textMagnetometer1   = label(LX, y += DY, "Magnetometer");
        final Text textMagnetometerA   = text (VX, y);
        final Text textMagnetometer2   = label(LX, y += DY, "=================================");
        final Text textMHeading        = label(LX, y += DY, "Magnetic heading");
        final Text textMHeadingValue   = text (VX, y);
        final Text textTHeading        = label(LX, y += DY, "True heading");
        final Text textTHeadingValue   = text (VX, y);
        final Text textFieldX          = label(LX, y += DY, "Field X");
        final Text textFieldXValue     = text (VX, y);
        final Text textFieldY          = label(LX, y += DY, "Field Y");
        final Text textFieldYValue     = text (VX, y);
        final Text textFieldZ          = label(LX, y += DY, "Field Z");
        final Text textFieldZValue     = text (VX, y);
        y += DY;

        final Text textBattery1        = label(LX, y += DY, "Battery");
        final Text textBattery2        = label(LX, y += DY, "=================================");
        final Text textBatLevel        = label(LX, y += DY, "Level");
        final Text textBatLevelValue   = text (VX, y);
        final Text textBatState        = label(LX, y += DY, "State");
        final Text textBatStateValue   = text (VX, y);

        getChildren().addAll(
                textLocation1,
                textLocation2,
                textLatitude,  textLatitudeValue,
                textLongitude, textLongitudeValue,
                textAltitude,  textAltitudeValue,
                textCourse,    textCourseValue,
                textSpeed,     textSpeedValue,

                textAccelerometer1, textAccelerometerA,
                textAccelerometer2,
                textAccX, textAccXValue,
                textAccY, textAccYValue,
                textAccZ, textAccZValue,

                textGyroscope1, textGyroscopeA,
                textGyroscope2,
                textRotX, textRotXValue,
                textRotY, textRotYValue,
                textRotZ, textRotZValue,

                textMagnetometer1, textMagnetometerA,
                textMagnetometer2,
                textMHeading,   textMHeadingValue,
                textTHeading,   textTHeadingValue,
                textFieldX,     textFieldXValue,
                textFieldY,     textFieldYValue,
                textFieldZ,     textFieldZValue,

                textBattery1,
                textBattery2,
                textBatLevel,
                textBatLevelValue,
                textBatState,
                textBatStateValue
        );

        // SENSORS STUFF
        // --------------------------------------------------------------------------------
        com.sun.javafx.ext.device.ios.sensors.IOSLocationManager.addLocationListener(
                new com.sun.javafx.ext.device.ios.sensors.IOSLocationManager.LocationListener() {
                    @Override
                    public void locationUpdated(double latitude, 
                                                double longitude, 
                                                double altitude, 
                                                double course, 
                                                double speed) {
                        textLatitudeValue.setText(String.valueOf(latitude));
                        textLongitudeValue.setText(String.valueOf(longitude));
                        textAltitudeValue.setText(String.valueOf(altitude));
                        textCourseValue.setText(String.valueOf(course));
                        textSpeedValue.setText(String.valueOf(speed));
                    }
        });

        textAccelerometerA.setText(com.sun.javafx.ext.device.ios.sensors.IOSMotionManager.isAccelerometerAvailable() ? "AVAILABLE" : "NOT AVAILABLE");
        com.sun.javafx.ext.device.ios.sensors.IOSMotionManager.addAccelerationListener(new com.sun.javafx.ext.device.ios.sensors.IOSMotionManager.Listener() {
            public void handleMotion(float x, float y, float z) {
                textAccXValue.setText(String.valueOf(x));
                textAccYValue.setText(String.valueOf(y));
                textAccZValue.setText(String.valueOf(z));
            }
        });

        textGyroscopeA.setText(com.sun.javafx.ext.device.ios.sensors.IOSMotionManager.isGyroAvailable() ? "AVAILABLE" : "NOT AVAILABLE");
        com.sun.javafx.ext.device.ios.sensors.IOSMotionManager.addRotationListener(new com.sun.javafx.ext.device.ios.sensors.IOSMotionManager.Listener() {
            public void handleMotion(float x, float y, float z) {
                textRotXValue.setText(String.valueOf(x));
                textRotYValue.setText(String.valueOf(y));
                textRotZValue.setText(String.valueOf(z));
            }
        });

        textMagnetometerA.setText(com.sun.javafx.ext.device.ios.sensors.IOSLocationManager.isHeadingAvailable() ? "AVAILABLE" : "NOT AVAILABLE");
        com.sun.javafx.ext.device.ios.sensors.IOSLocationManager.addHeadingListener(new com.sun.javafx.ext.device.ios.sensors.IOSLocationManager.HeadingListener() {
            public void headingUpdated(double m, double t, double x, double y, double z) {
                textMHeadingValue.setText(String.valueOf(m));
                textTHeadingValue.setText(String.valueOf(t));
                textFieldXValue.setText(String.valueOf(x));
                textFieldYValue.setText(String.valueOf(y));
                textFieldZValue.setText(String.valueOf(z));
            }
        });

        com.sun.javafx.ext.device.ios.sensors.IOSDevice.getCurrentDevice().setProximityMonitoringEnabled(true);
        setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent me) {
                System.out.println("PROXIMITY: " + com.sun.javafx.ext.device.ios.sensors.IOSDevice.getCurrentDevice().getProximityState());
            }
        });

        com.sun.javafx.ext.device.ios.sensors.IOSDevice.getCurrentDevice().setBatteryMonitoringEnabled(true);
        textBatLevelValue.setText(String.valueOf(com.sun.javafx.ext.device.ios.sensors.IOSDevice.getCurrentDevice().getBatteryLevel()));
        textBatStateValue.setText(String.valueOf(com.sun.javafx.ext.device.ios.sensors.IOSDevice.getCurrentDevice().getBatteryState()));
    }
}
