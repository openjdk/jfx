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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Stanislav Smirnov <stanislav.smirnov@oracle.com>
 * Date: 05.09.13
 * Time: 12:53
 * To change this template use File | Settings | File Templates.
 */
public class CalData {
    public final long[] offset = new long[3];
    public final short[] range = new short[3];

    public CalData() {}

    public CalData(short[] val) {
        offset[0] = (short)((val[0] + val[1]) / 2);
        offset[1] = (short)((val[2] + val[3]) / 2);
        offset[2] = (short)((val[4] + val[5]) / 2);
        range[0] = (short)(val[1] - offset[0]);
        range[1] = (short)(val[3] - offset[1]);
        range[2] = (short)(val[5] - offset[2]);
    }

    public CalData(short[] minVal, short[] maxVal) {
        offset[0] = (short)((minVal[0] + maxVal[0]) / 2);
        offset[1] = (short)((minVal[1] + maxVal[1]) / 2);
        offset[2] = (short)((minVal[2] + maxVal[2]) / 2);
        range[0] = (short)(maxVal[0] - offset[0]);
        range[1] = (short)(maxVal[1] - offset[1]);
        range[2] = (short)(maxVal[2] - offset[2]);
    }

    public boolean readFromFile(String calFile) {
        try {
            BufferedReader input;
            try {
                input = new BufferedReader(new FileReader(calFile));
            } catch (FileNotFoundException ex) {
                System.out.println("Unable to open calibration file " + calFile);
                return false;
            }
            try {
                String line = null;
                int i = 0;
                while ((line = input.readLine()) != null) {
                    if (i < 3) {
                        offset[i] = Short.parseShort(line);
                    } else if (i < 6) {
                        range[i-3] = Short.parseShort(line);
                    } else {
                        break;
                    }
                    i++;
                }
            } finally {
                input.close();
            }
        } catch (IOException ex) {
            System.out.println("Failed to read from calibration file " + calFile);
            return false;
        }
        return true;
    }

    public boolean writeToFile(String calFile) {
        File file = new File(calFile);

        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (IOException ex) {
            System.out.println("Unable to create calibration file " + calFile);
            return false;
        }

        try {
            BufferedWriter output = new BufferedWriter(new FileWriter(file));
            try {
                for (int i = 0; i < 3; i++) {
                    output.write(String.format("%d\n", offset[i]));
                }
                for (int i = 0; i < 3; i++) {
                    output.write(String.format("%d\n", range[i]));
                }
            } finally {
                output.close();
            }
        } catch (IOException ex) {
            System.out.println("Failed to write to calibration file " + calFile);
            return false;
        }
        return true;
    }
}
