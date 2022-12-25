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

package test.javafx.scene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.effect.Blend;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Shadow;
import javafx.scene.shape.Rectangle;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class Node_bind_Test {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

     @Test public void testClip() {
         Rectangle rectA = new Rectangle(300, 300);
         Rectangle clip1 = new Rectangle(10, 10);
         Rectangle clip2 = new Rectangle(100, 100);
         ObjectProperty<Node> v = new SimpleObjectProperty<>(clip1);
         rectA.clipProperty().bind(v);
         assertEquals(rectA.getClip(), clip1);
         v.set(clip2);
         assertEquals(rectA.getClip(), clip2);
     }

     int countIllegalArgumentException = 0;
     @Test public void testIllegalClip() {
         Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
             if (throwable instanceof IllegalArgumentException) {
                 countIllegalArgumentException++;
             } else {
                 Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
             }
         });
         Rectangle rectA = new Rectangle(300, 300);
         Rectangle clip1 = new Rectangle(10, 10);
         Rectangle clip2 = new Rectangle(100, 100);
         clip2.setClip(rectA);
         ObjectProperty<Node> v = new SimpleObjectProperty<>(clip1);
         rectA.clipProperty().bind(v);
         assertEquals(rectA.getClip(), clip1);
         v.set(clip2);
         assertNotSame(rectA.getClip(), clip2);

         assertEquals("Cycle in effect chain detected, exception should occur once.", 1, countIllegalArgumentException);
         Thread.currentThread().setUncaughtExceptionHandler(null);
     }

     @Test public void testBackToLegalClip() {
         countIllegalArgumentException = 0;
         Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
             if (throwable instanceof IllegalArgumentException) {
                 countIllegalArgumentException++;
             } else {
                 Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
             }
         });
         Rectangle rectA = new Rectangle(300, 300);
         Rectangle clip1 = new Rectangle(10, 10);
         Rectangle clip2 = new Rectangle(100, 100);
         clip2.setClip(rectA);
         ObjectProperty<Node> v = new SimpleObjectProperty<>(clip1);
         rectA.clipProperty().bind(v);
         assertEquals(rectA.getClip(), clip1);
         v.set(clip2);
         assertEquals(rectA.getClip(), clip1);

         assertEquals("Cycle in effect chain detected, exception should occur once.", 1, countIllegalArgumentException);
         Thread.currentThread().setUncaughtExceptionHandler(null);
     }

     @Test public void testEffect() {
         Shadow effect1 = new Shadow();
         Blend effect2 = new Blend();
         Rectangle rectA = new Rectangle(100, 100);
         ObjectProperty<Effect> v = new SimpleObjectProperty<>(effect1);
         rectA.effectProperty().bind(v);
         assertEquals(rectA.getEffect(), effect1);
         v.set(effect2);
         assertEquals(rectA.getEffect(), effect2);
     }
}
