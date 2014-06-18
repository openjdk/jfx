/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.javafx.experiments.importers.maya.values;

import java.util.List;

public interface MPolyFace extends MData {
    public static class FaceData {
        private int[] faceEdges;
        private int[] holeEdges;
        private int[][] uvData;
        private int[] faceColors;

        public void setFaceEdges(int[] faceEdges) { this.faceEdges = faceEdges; }

        public void setHoleEdges(int[] holeEdges) { this.holeEdges = holeEdges; }

        public void setUVData(int index, int[] data) {
            if (uvData == null || index >= uvData.length) {
                int[][] newUVData = new int[index + 1][];
                if (uvData != null) {
                    System.arraycopy(uvData, 0, newUVData, 0, uvData.length);
                }
                uvData = newUVData;
            }
            uvData[index] = data;
        }

        public void setFaceColors(int[] faceColors) { this.faceColors = faceColors; }

        public int[] getFaceEdges() { return faceEdges; }

        public int[] getHoleEdges() { return holeEdges; }

        public int[][] getUVData() {
            return uvData;
        }

        public int[] getFaceColors() { return faceColors; }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("[FaceData faceEdges: ");
            appendIntArray(buf, faceEdges);
            buf.append(" holeEdges: ");
            appendIntArray(buf, holeEdges);
            buf.append(" uvData: ");
            append2DIntArray(buf, uvData);
            buf.append(" faceColors: ");
            appendIntArray(buf, faceColors);
            buf.append("]");
            return buf.toString();
        }

        private void appendIntArray(StringBuffer buf, int[] array) {
            if (array == null) {
                buf.append(array);
            } else {
                buf.append("[");
                for (int i = 0; i < array.length; i++) {
                    buf.append(" ");
                    buf.append(array[i]);
                }
            }
        }

        private void append2DIntArray(StringBuffer buf, int[][] array) {
            if (array == null) {
                buf.append(array);
            } else {
                buf.append("[");
                for (int i = 0; i < array.length; i++) {
                    appendIntArray(buf, array[i]);
                }
                buf.append("]");
            }
        }
    }

    public void addFace(FaceData face);

    public List<FaceData> getFaces();
}
