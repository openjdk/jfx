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

package com.javafx.experiments.dukepad.cubeGame;

import javafx.geometry.Point3D;
import javafx.scene.transform.Rotate;

import java.util.Iterator;
import java.util.LinkedList;

public class MagicCubePlayer {

    private MagicCube magicCube;
    
    private LinkedList<Entry> record = new LinkedList<Entry>();
    private Iterator<Entry> iterator;
    private Runnable onPlayEnd;
    private boolean autoReverse = true;
    private boolean doReverseTrick = true;

    public void setOnPlayEnd(Runnable onPlayEnd) {
        this.onPlayEnd = onPlayEnd;
    }
    
    public MagicCubePlayer(MagicCube magicCube) {
        this.magicCube = magicCube;
    }

    public void setDoReverseTrick(boolean doReverseTrick) {
        this.doReverseTrick = doReverseTrick;
    }
    
    public void setAutoReverse(boolean autoReverse) {
        this.autoReverse = autoReverse;
    }
    
    private Runnable runnable = new Runnable() {

        @Override
        public void run() {
            if (iterator.hasNext()) {
                rotate(iterator.next());
            } else {
                if (autoReverse) {
                    playBack();
                } else {
                    magicCube.setOnRotateEnd(null);
                    fireOnPlayEnd();
                }
            }
        }
    };
    
    private Runnable runnableBack = new Runnable() {

        @Override
        public void run() {
            if (iterator.hasNext()) {
                rotate(iterator.next().reverse());
            } else {
                magicCube.setOnRotateEnd(null);
                fireOnPlayEnd();
            }
        }
    };
    
    public void play() {
        if (autoReverse && doReverseTrick) {
            Entry last1 = record.removeLast();
            Entry last2 = record.getLast();
            record.add(last1);
            if (last2.axis != last1.axis) {
                final int layer = (int) ((Math.random() * 2 + 1) + last1.layer) % 3;
                if (layer == last1.layer) {
                    throw new RuntimeException("Incorrect algorithm work!");
                }
                record.add(new Entry(
                        last1.axis, layer, 
                        Math.random() < 0.5 ? -1 : 1));
            }
        }
        iterator = record.iterator();
        magicCube.setOnRotateEnd(runnable);
        runnable.run();
    }
    
    public void playBack() {
        if (doReverseTrick) {
            Entry last1 = record.removeLast();
            Entry last2 = record.removeLast();
            record.add(last1);
            record.add(last2);
        }
        iterator = record.descendingIterator();
        magicCube.setOnRotateEnd(runnableBack);
        runnableBack.run();
    }
    
    public void generateRandom() {
        record.clear();
        Entry entry = null;
        int size = (int) (Math.random() * 30 + 20);
        for (int i = 0; i < size; i++) {
            while (true) {
                double r1 = Math.random();
                double r2 = Math.random();
                double r3 = Math.random();
                Point3D axis = r1 < 0.333 ? Rotate.X_AXIS : 
                               r1 < 0.666 ? Rotate.Y_AXIS : Rotate.Z_AXIS;
                int layer = (int) (r2 * 3);
                int rotate = r3 < 0.5 ? -1 : 1;
                if (entry != null && axis == entry.axis && layer == entry.layer) {
                    continue;
                }
                entry = new Entry(axis, layer, rotate);
                record.add(entry);
                break;
            }
        }
    }
    
    private void fireOnPlayEnd() {
        if (onPlayEnd != null) {
            onPlayEnd.run();
        }
    }
    
    public static class Entry {
        public Point3D axis;
        public int rotate;
        public int layer;

        public Entry(Point3D axis, int layer, int rotate) {
            this.axis = axis;
            this.rotate = rotate;
            this.layer = layer;
        }

        private Entry reverse() {
            return new Entry(axis, layer, -rotate);
        }
        
    }
    
    public boolean rotate(Entry e) {
        return magicCube.rotate(e.axis, e.layer, e.rotate);
    }
}
