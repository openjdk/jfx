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

import com.pi4j.io.i2c.I2CBus;
import java.util.Arrays;
import com.sun.javafx.geom.Vec3d;

public class Mpu9150 {
    public static final int MAG_SENSOR_RANGE = 4096;
    public static final int ACCEL_SENSOR_RANGE = 32000;

    // Somewhat arbitrary limits here. The values are samples per second.
    // The MIN comes from the way we are timing our loop in imu and imucal.
    // That's easily worked around, but no one probably cares.
    // The MAX comes from the compass. This could be avoided with separate
    // sample rates for the compass and the accel/gyros which can handle
    // faster sampling rates. This is a TODO item to see if it's useful.
    // There are some practical limits on the speed that come from a 'userland'
    // implementation like this as opposed to a kernel or 'bare-metal' driver.
    public static final int MIN_SAMPLE_RATE = 2;
    public static final int MAX_SAMPLE_RATE = 100;

    private boolean debugOn;

    private int yawMixingFactor = 0;

    private boolean useMag = true;

    private boolean useAccelCal = false;
    private CalData accelCalData = null;

    private boolean useMagCal = false;
    private CalData magCalData = null;

    private final InvMpu invMPU = new InvMpu();
    private final InvMpuDmpMotionDriver invMpuDmp = new InvMpuDmpMotionDriver(invMPU);

    public void setDebug(boolean on) {
        debugOn = on;
    }

    public boolean init(int i2cBus, short i2cAddress, int sampleRate, int mixFactor, boolean useMag) {
        byte[] gyro_orientation = { 1, 0, 0,
                0, 1, 0,
                0, 0, 1 };

        if (i2cBus != I2CBus.BUS_0 && i2cBus != I2CBus.BUS_1) {
            System.out.printf("Invalid I2C bus %d\n", i2cBus);
            return false;
        }

        if (i2cAddress != 0x68 && i2cAddress != 0x69) {
            System.out.printf("Invalid I2C address %d\n", i2cAddress);
            return false;
        }

        if (sampleRate < MIN_SAMPLE_RATE || sampleRate > MAX_SAMPLE_RATE) {
            System.out.printf("Invalid sample rate %d\n", sampleRate);
            return false;
        }

        if (mixFactor < 0 || mixFactor > 100) {
            System.out.printf("Invalid mag mixing factor %d\n", mixFactor);
            return false;
        }

        yawMixingFactor = mixFactor;

        this.useMag = useMag;

        if (!invMPU.set_i2c(i2cBus, i2cAddress)) {
            System.out.printf("Failed to set I2C bus and address%d\n", i2cBus);
            return false;
        }

        System.out.printf("\nInitializing IMU .");
        System.out.flush();

        if (!invMPU.mpu_init(useMag)) {
            System.out.printf("\nmpu_init() failed\n");
            return false;
        }

        System.out.printf(".");
        System.out.flush();

        short sensors;
        if (useMag) {
            sensors = (short)(invMPU.INV_XYZ_GYRO | invMPU.INV_XYZ_ACCEL | invMPU.INV_XYZ_COMPASS);
        } else {
            sensors = (short)(invMPU.INV_XYZ_GYRO | invMPU.INV_XYZ_ACCEL);
        }
        if (!invMPU.mpu_set_sensors(sensors)) {
            System.out.printf("\nmpu_set_sensors() failed\n");
            return false;
        }

        System.out.printf(".");
        System.out.flush();

        if (!invMPU.mpu_configure_fifo((short)(invMPU.INV_XYZ_GYRO | invMPU.INV_XYZ_ACCEL))) {
            System.out.printf("\nmpu_configure_fifo() failed\n");
            return false;
        }

        System.out.printf(".");
        System.out.flush();

        if (!invMPU.mpu_set_sample_rate(sampleRate)) {
            System.out.printf("\nmpu_set_sample_rate() failed\n");
            return false;
        }

        System.out.printf(".");
        System.out.flush();

        if (useMag) {
            if (!invMPU.mpu_set_compass_sample_rate(sampleRate)) {
                System.out.printf("\nmpu_set_compass_sample_rate() failed\n");
                return false;
            }
        }

        System.out.printf(".");
        System.out.flush();

        if (!invMpuDmp.dmp_load_motion_driver_firmware()) {
            System.out.printf("\ndmp_load_motion_driver_firmware() failed\n");
            return false;
        }

        System.out.printf(".");
        System.out.flush();

        if (!invMpuDmp.dmp_set_orientation(invOrientationMatrixToScalar(gyro_orientation))) {
            System.out.printf("\ndmp_set_orientation() failed\n");
            return false;
        }

        System.out.printf(".");
        System.out.flush();

        if (!invMpuDmp.dmp_enable_feature( invMpuDmp.DMP_FEATURE_6X_LP_QUAT | invMpuDmp.DMP_FEATURE_SEND_RAW_ACCEL
                | invMpuDmp.DMP_FEATURE_SEND_CAL_GYRO | invMpuDmp.DMP_FEATURE_GYRO_CAL
                | invMpuDmp.DMP_FEATURE_ANDROID_ORIENT )) {
            System.out.printf("\ndmp_enable_feature() failed\n");
            return false;
        }

        System.out.printf(".");
        System.out.flush();

        if (!invMpuDmp.dmp_set_fifo_rate(sampleRate)) {
            System.out.printf("\ndmp_set_fifo_rate() failed\n");
            return false;
        }

        System.out.printf(".");
        System.out.flush();

        if (!invMPU.mpu_set_dmp_state(true)) {
            System.out.printf("\nmpu_set_dmp_state(1) failed\n");
            return false;
        }

        System.out.printf(" done\n\n");

        return true;
    }

    public void exit() {
        // turn off the DMP on exit
        if (!invMPU.mpu_set_dmp_state(false))
            System.out.println("mpu_set_dmp_state(0) failed");

        // TODO: Should turn off the sensors too
    }

    public void setAccelCal(CalData cal) {
        if (cal == null) {
            useAccelCal = false;
            return;
        }

        accelCalData = cal;

        long[] bias = new long[3];
        for (int i = 0; i < 3; i++) {
            if (accelCalData.range[i] < 1)
                accelCalData.range[i] = 1;
            else if (accelCalData.range[i] > ACCEL_SENSOR_RANGE)
                accelCalData.range[i] = ACCEL_SENSOR_RANGE;

            bias[i] = -accelCalData.offset[i];
        }

        invMPU.mpu_set_accel_bias(bias);

        useAccelCal = true;
    }

    public void setMagCal(CalData cal) {
        if (cal == null) {
            useMagCal = false;
            return;
        }

        magCalData = cal;

        for (int i = 0; i < 3; i++) {
            if (magCalData.range[i] < 1)
                magCalData.range[i] = 1;
            else if (magCalData.range[i] > MAG_SENSOR_RANGE)
                magCalData.range[i] = MAG_SENSOR_RANGE;

            if (magCalData.offset[i] < -MAG_SENSOR_RANGE)
                magCalData.offset[i] = -MAG_SENSOR_RANGE;
            else if (magCalData.offset[i] > MAG_SENSOR_RANGE)
                magCalData.offset[i] = MAG_SENSOR_RANGE;
        }

        useMagCal = true;
    }

    public boolean readDMP(MpuData mpu) {
        if (!dataReady()) {
            return false;
        }

        short[] sensors = new short[1];
        short[] more = new short[1];

        if (!invMpuDmp.dmp_read_fifo(mpu.rawGyro, mpu.rawAccel, mpu.rawQuat, mpu.dmpTimestamp, sensors, more)) {
            System.out.println("dmp_read_fifo() failed");
            return false;
        }

        while (more[0] != 0) {
            // Fell behind, reading again
            if (!invMpuDmp.dmp_read_fifo(mpu.rawGyro, mpu.rawAccel, mpu.rawQuat, mpu.dmpTimestamp, sensors, more)) {
                System.out.println("dmp_read_fifo() failed");
                return false;
            }
        }

        return true;
    }

    public boolean readMag(MpuData mpu) {
        if (!invMPU.mpu_get_compass_reg(mpu.rawMag, mpu.magTimestamp)) {
            System.out.println("mpu_get_compass_reg() failed");
            return false;
        }
        return true;
    }

    public boolean read(MpuData mpu) {
        if (!readDMP(mpu))
            return false;

        if (useMag) {
            if (!readMag(mpu))
                return false;
        }

        calibrateData(mpu);

        return dataFusion(mpu);
    }

    boolean dataReady() {
        short[] status = new short[1];

        if (!invMPU.mpu_get_int_status(status)) {
            System.out.println("java mpu_get_int_status() failed");
            return false;
        }

        return (status[0] == (InvMpu.MPU_INT_STATUS_DATA_READY | InvMpu.MPU_INT_STATUS_DMP | InvMpu.MPU_INT_STATUS_DMP_0));
    }

    void calibrateData(MpuData mpu) {
        if (useMagCal) {
            if (magCalData.range[0] != 0) {
                mpu.calibratedMag[1] = (short) -(((int)(mpu.rawMag[0] - magCalData.offset[0])
                        * (int)MAG_SENSOR_RANGE) / (int)magCalData.range[0]);
            }
            if (magCalData.range[1] != 0) {
                mpu.calibratedMag[0] = (short)(((int)(mpu.rawMag[1] - magCalData.offset[1])
                        * (int)MAG_SENSOR_RANGE) / (int)magCalData.range[1]);
            }
            if (magCalData.range[2] != 0) {
                mpu.calibratedMag[2] = (short)(((int)(mpu.rawMag[2] - magCalData.offset[2])
                        * (int)MAG_SENSOR_RANGE) / (int)magCalData.range[2]);
            }
        } else {
            mpu.calibratedMag[1] = (short) -mpu.rawMag[0];
            mpu.calibratedMag[0] = mpu.rawMag[1];
            mpu.calibratedMag[2] = mpu.rawMag[2];
        }

        if (useAccelCal) {
            if (accelCalData.range[0] != 0) {
                mpu.calibratedAccel[0] = (short) -(((int)mpu.rawAccel[0] * (int)ACCEL_SENSOR_RANGE)
                        / (int)accelCalData.range[0]);
            }
            if (accelCalData.range[1] != 0) {
                mpu.calibratedAccel[1] = (short)(((int)mpu.rawAccel[1] * (int)ACCEL_SENSOR_RANGE)
                        / (int)accelCalData.range[1]);
            }
            if (accelCalData.range[2] != 0) {
                mpu.calibratedAccel[2] = (short)(((int)mpu.rawAccel[2] * (int)ACCEL_SENSOR_RANGE)
                        / (int)accelCalData.range[2]);
            }
        } else {
            mpu.calibratedAccel[0] = (short) -mpu.rawAccel[0];
            mpu.calibratedAccel[1] = mpu.rawAccel[1];
            mpu.calibratedAccel[2] = mpu.rawAccel[2];
        }
    }

    void tiltCompensate(Quaternion magQ, Quaternion unfusedQ) {
        Quaternion unfusedConjugateQ = unfusedQ.conjugate();
        Quaternion tempQ = magQ.multiply(unfusedConjugateQ);
        magQ.set(unfusedQ.multiply(tempQ));
    }

    boolean dataFusion(MpuData mpu) {
        Quaternion dmpQuat;
        Vec3d dmpEuler;
        //Quaternion magQuat;
        //Quaternion unfusedQuat;
        float deltaDMPYaw;
        float deltaMagYaw;
        float newMagYaw;
        float newYaw;

        dmpQuat = new Quaternion(mpu.rawQuat[0], mpu.rawQuat[1], mpu.rawQuat[2], mpu.rawQuat[3]);

        dmpQuat = dmpQuat.normalize();
        dmpEuler = Quaternion.toEuler(dmpQuat);

        mpu.fusedEuler.x = dmpEuler.x;
        mpu.fusedEuler.y = -dmpEuler.y;
        mpu.fusedEuler.z = 0.0;

        mpu.unfusedQuat.set(Quaternion.fromEuler(mpu.fusedEuler));

        if (!useMag) {
            mpu.fusedQuat.set(mpu.unfusedQuat);
            return true;
        }

        deltaDMPYaw = -(float)dmpEuler.z + mpu.lastDMPYaw;
        mpu.lastDMPYaw = (float)dmpEuler.z;

        mpu.magQuat.set(new Quaternion(0.0, mpu.calibratedMag[0], mpu.calibratedMag[1], mpu.calibratedMag[2]));

        tiltCompensate(mpu.magQuat, mpu.unfusedQuat);

        newMagYaw = -(float)Math.atan2(mpu.magQuat.getY(), mpu.magQuat.getX());

        if (newMagYaw != newMagYaw) {
            System.out.println("newMagYaw NAN");
            return false;
        }

        if (newMagYaw < 0.0f) {
            newMagYaw = (float)(2.0*Math.PI + newMagYaw);
        }

        newYaw = mpu.lastYaw + deltaDMPYaw;

        if (newYaw > 2.0*Math.PI) {
            newYaw -= 2.0*Math.PI;
        } else if (newYaw < 0.0f) {
            newYaw += 2.0*Math.PI;
        }

        deltaMagYaw = newMagYaw - newYaw;

        if (deltaMagYaw >= Math.PI) {
            deltaMagYaw -= 2.0*Math.PI;
        } else if (deltaMagYaw < -Math.PI) {
            deltaMagYaw += 2.0*Math.PI;
        }

        if (yawMixingFactor > 0) {
            newYaw += deltaMagYaw / yawMixingFactor;
        }

        if (newYaw > 2.0*Math.PI) {
            newYaw -= 2.0*Math.PI;
        } else if (newYaw < 0.0f) {
            newYaw += 2.0*Math.PI;
        }

        mpu.lastYaw = newYaw;

        if (newYaw > Math.PI) {
            newYaw -= 2.0*Math.PI;
        }

        mpu.fusedEuler.z = newYaw;

        mpu.fusedQuat.set(Quaternion.fromEuler(mpu.fusedEuler));

        return true;
    }

    /**
     * These next two functions convert the orientation matrix (see
     * gyro_orientation) to a scalar representation for use by the DMP.
     * NOTE: These functions are borrowed from InvenSense's MPL
     * inv_row_2_scale and inv_orientation_matrix_to_scalar.
     */
    int invRowToScale(byte[] row)
    {
        int b;

        if (row[0] > 0)
            b = 0;
        else if (row[0] < 0)
            b = 4;
        else if (row[1] > 0)
            b = 1;
        else if (row[1] < 0)
            b = 5;
        else if (row[2] > 0)
            b = 2;
        else if (row[2] < 0)
            b = 6;
        else
            b = 7;      // error
        return b;
    }

    int invOrientationMatrixToScalar(byte[] mtx)
    {
        int scalar;
        /*
         * XYZ  010_001_000 Identity Matrix
         * XZY  001_010_000
         * YXZ  010_000_001
         * YZX  000_010_001
         * ZXY  001_000_010
         * ZYX  000_001_010
         */
        scalar = invRowToScale(Arrays.copyOfRange(mtx, 0, 3));
        scalar |= invRowToScale(Arrays.copyOfRange(mtx, 3, 6)) << 3;
        scalar |= invRowToScale(Arrays.copyOfRange(mtx, 6, 9)) << 6;
        return scalar;
    }
}
