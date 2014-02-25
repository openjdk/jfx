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

package javafx.scene;

import com.sun.javafx.test.TransformHelper;
import javafx.scene.transform.Translate;
import javafx.scene.shape.Rectangle;
import javafx.beans.Observable;
import java.lang.reflect.Method;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.transform.Transform;
import org.junit.Test;
import static org.junit.Assert.*;

public class Node_LocalToSceneTransform_Test {
    private boolean notified;

    @Test
    public void notTransformedNodeShouldReturnIdentity() {
        Node n = new Rectangle(20, 20);
        TransformHelper.assertMatrix(n.getLocalToSceneTransform(),
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0);
    }

    @Test
    public void noListenersShouldBeAddedByDefault() throws Exception {
        Node r = new Rectangle(20, 20);
        Group n = new Group(r);
        Group p = new Group(n);

        p.getLocalToSceneTransform();
        n.getLocalToSceneTransform();
        r.getLocalToSceneTransform();

        p.setTranslateX(10);
        p.setRotate(80);

        // n didn't react on its parent's transformation
        TransformHelper.assertMatrix(n.getCurrentLocalToSceneTransformState(),
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0);
    }

    @Test
    public void shouldConsiderAllParents() {
        Node n = new Rectangle(20, 20);
        Group p1 = new Group(n);
        Group p2 = new Group(p1);

        n.setTranslateX(10);
        p1.setTranslateY(20);
        p2.getTransforms().add(new Translate(5, 6));

        TransformHelper.assertMatrix(n.getLocalToSceneTransform(),
                1, 0, 0, 15,
                0, 1, 0, 26,
                0, 0, 1, 0);
    }

    @Test
    public void shouldBeUpToDate() {
        Node n = new Rectangle(20, 20);
        Group p1 = new Group(n);
        Group p2 = new Group(p1);

        n.setTranslateX(10);
        p1.setTranslateY(20);
        p2.getTransforms().add(new Translate(5, 6));

        TransformHelper.assertMatrix(n.getLocalToSceneTransform(),
                1, 0, 0, 15,
                0, 1, 0, 26,
                0, 0, 1, 0);

        n.setTranslateX(0);
        p1.setTranslateY(0);
        p2.getTransforms().clear();
        p2.setScaleY(10);

        TransformHelper.assertMatrix(n.getLocalToSceneTransform(),
                1,  0, 0,   0,
                0, 10, 0, -90,
                0,  0, 1,   0);
    }

    @Test
    public void shouldBeNotifiedWhenThisTransforms() {
        final Node n = new Rectangle(20, 20);
        n.localToSceneTransformProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable o) {
                notified = true;
                TransformHelper.assertMatrix(n.getLocalToSceneTransform(),
                    1, 0, 0, 10,
                    0, 1, 0, 20,
                    0, 0, 1,  0);
            }
        });

        notified = false;
        n.getTransforms().add(new Translate(10, 20));
        assertTrue(notified);
    }
    
    @Test
    public void shouldBeNotifiedWhenParentTransforms() {
        final Node n = new Rectangle(20, 20);
        final Group g = new Group(n);
        n.localToSceneTransformProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable o) {
                notified = true;
                TransformHelper.assertMatrix(n.getLocalToSceneTransform(),
                    1, 0, 0, 10,
                    0, 1, 0, 20,
                    0, 0, 1,  0);
            }
        });

        notified = false;
        g.getTransforms().add(new Translate(10, 20));
        assertTrue(notified);
    }

    @Test
    public void shouldBeNotifiedOnReparent() {
        final Node n = new Rectangle(20, 20);
        final Group g = new Group(n);
        final Group g2 = new Group();
        g2.setTranslateX(100);

        n.localToSceneTransformProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable o) {
                if (n.getParent() != null) {
                    notified = true;
                    TransformHelper.assertMatrix(n.getLocalToSceneTransform(),
                        1, 0, 0, 100,
                        0, 1, 0,   0,
                        0, 0, 1,   0);
                } else {
                    TransformHelper.assertMatrix(n.getLocalToSceneTransform(),
                        1, 0, 0, 0,
                        0, 1, 0, 0,
                        0, 0, 1, 0);
                }
            }
        });

        notified = false;
        g2.getChildren().add(n);
        assertTrue(notified);
    }

    @Test
    public void shouldBeNotifiedWhenParentTransformsAfterReparent() {
        final Node n = new Rectangle(20, 20);
        final Group g = new Group(n);
        final Group g2 = new Group();
        g.setTranslateX(50);
        g2.setTranslateX(100);

        n.localToSceneTransformProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable o) {
                if (!notified) {
                    notified = true;
                    TransformHelper.assertMatrix(n.getLocalToSceneTransform(),
                        1, 0, 0, 60,
                        0, 1, 0,  0,
                        0, 0, 1,  0);
                } else {
                    // enable next invalidation
                    n.getLocalToSceneTransform();
                }
            }
        });

        // disable listener
        notified = true;
        g2.getChildren().add(n);
        // enable listener
        notified = false;
        g2.setTranslateX(60);
        assertTrue(notified);
    }

    @Test
    public void shouldUnregisterListenersWhenNotNeeded() {
        final Node n = new Rectangle(20, 20);
        final Group p1 = new Group(n);
        final Group p2 = new Group(p1);

        InvalidationListener lstnr = new InvalidationListener() {
            public void invalidated(Observable o) {
                n.getLocalToSceneTransform();
            }
        };

        n.localToSceneTransformProperty().addListener(lstnr);

        // with listener on leave, parents update
        p2.setTranslateX(30);
        TransformHelper.assertMatrix(p1.getCurrentLocalToSceneTransformState(),
                1, 0, 0, 30,
                0, 1, 0,  0,
                0, 0, 1,  0);

        // without listener on leave, parents don't update
        n.localToSceneTransformProperty().removeListener(lstnr);
        p2.setTranslateX(60);
        TransformHelper.assertMatrix(p1.getCurrentLocalToSceneTransformState(),
                1, 0, 0, 30,
                0, 1, 0,  0,
                0, 0, 1,  0);

        // with listener on leave again, parents update again
        n.localToSceneTransformProperty().addListener(lstnr);
        p2.setTranslateX(90);
        TransformHelper.assertMatrix(p1.getCurrentLocalToSceneTransformState(),
                1, 0, 0, 90,
                0, 1, 0,  0,
                0, 0, 1,  0);
    }

    @Test
    public void shouldUnregisterListenersWhenNotNeededButStillUpdate() {
        final Node n = new Rectangle(20, 20);
        final Group p1 = new Group(n);
        final Group p2 = new Group(p1);

        InvalidationListener lstnr = new InvalidationListener() {
            public void invalidated(Observable o) {
                n.getLocalToSceneTransform();
            }
        };

        n.localToSceneTransformProperty().addListener(lstnr);

        // with listener on leave, parents update
        p2.setTranslateX(30);
        TransformHelper.assertMatrix(p1.getCurrentLocalToSceneTransformState(),
                1, 0, 0, 30,
                0, 1, 0,  0,
                0, 0, 1,  0);

        // without listener on leave, parents don't update immediately
        n.localToSceneTransformProperty().removeListener(lstnr);
        p2.setTranslateX(60);
        TransformHelper.assertMatrix(p1.getCurrentLocalToSceneTransformState(),
                1, 0, 0, 30,
                0, 1, 0,  0,
                0, 0, 1,  0);

        // ... but must do it on request:
        TransformHelper.assertMatrix(p1.getLocalToSceneTransform(),
                1, 0, 0, 60,
                0, 1, 0,  0,
                0, 0, 1,  0);
    }

    @Test
    public void shouldUnregisterListenersWhenReparent() {
        final Node n = new Rectangle(20, 20);
        final Group p1 = new Group(n);
        final Group p2 = new Group(p1);
        final Group g = new Group();

        InvalidationListener lstnr = new InvalidationListener() {
            public void invalidated(Observable o) {
                n.getLocalToSceneTransform();
            }
        };

        n.localToSceneTransformProperty().addListener(lstnr);

        // with listener on leave, parents update
        p2.setTranslateX(30);
        TransformHelper.assertMatrix(p1.getCurrentLocalToSceneTransformState(),
                1, 0, 0, 30,
                0, 1, 0,  0,
                0, 0, 1,  0);

        // child with listener is moved away, parents stop updating
        g.getChildren().add(n);
        p2.setTranslateX(60);
        TransformHelper.assertMatrix(p1.getCurrentLocalToSceneTransformState(),
                1, 0, 0, 30,
                0, 1, 0,  0,
                0, 0, 1,  0);

        // leaf with listener moved back, parents update again
        p1.getChildren().add(n);
        p2.setTranslateX(90);
        TransformHelper.assertMatrix(p1.getCurrentLocalToSceneTransformState(),
                1, 0, 0, 90,
                0, 1, 0,  0,
                0, 0, 1,  0);
    }

    @Test
    public void shouldNotUnregisterListenerIfThereIsOtherReason() {
        final Node n = new Rectangle(20, 20);
        final Group p1 = new Group(n);
        final Group p2 = new Group(p1);
        final Group g = new Group();

        InvalidationListener nlstnr = new InvalidationListener() {
            public void invalidated(Observable o) {
                n.getLocalToSceneTransform();
            }
        };
        InvalidationListener plstnr = new InvalidationListener() {
            public void invalidated(Observable o) {
                p1.getLocalToSceneTransform();
            }
        };

        n.localToSceneTransformProperty().addListener(nlstnr);
        p1.localToSceneTransformProperty().addListener(plstnr);

        // with listeners, parents update
        p2.setTranslateX(30);
        TransformHelper.assertMatrix(p1.getCurrentLocalToSceneTransformState(),
                1, 0, 0, 30,
                0, 1, 0,  0,
                0, 0, 1,  0);

        // child moved away, but there is still listener on parent
        g.getChildren().add(n);
        p2.setTranslateX(60);
        TransformHelper.assertMatrix(p1.getCurrentLocalToSceneTransformState(),
                1, 0, 0, 60,
                0, 1, 0,  0,
                0, 0, 1,  0);

        // removed even the listener on parent, now we can stop updating
        p1.localToSceneTransformProperty().removeListener(plstnr);
        p2.setTranslateX(90);
        TransformHelper.assertMatrix(p1.getCurrentLocalToSceneTransformState(),
                1, 0, 0, 60,
                0, 1, 0,  0,
                0, 0, 1,  0);

        // return both listener and child
        p1.localToSceneTransformProperty().addListener(plstnr);
        p1.getChildren().add(n);

        // remove the listener, must still update because of the child
        p1.localToSceneTransformProperty().removeListener(plstnr);
        p2.setTranslateX(45);
        TransformHelper.assertMatrix(p1.getCurrentLocalToSceneTransformState(),
                1, 0, 0, 45,
                0, 1, 0,  0,
                0, 0, 1,  0);

        // remove the child as well, now we can stop updating
        g.getChildren().add(n);
        p2.setTranslateX(25);
        TransformHelper.assertMatrix(p1.getCurrentLocalToSceneTransformState(),
                1, 0, 0, 45,
                0, 1, 0,  0,
                0, 0, 1,  0);
    }

    @Test
    public void shouldNotBeReusedWhenReferenceGivenToUser() {

        final Node n = new Rectangle(20, 20);
        final Group g = new Group(n);

        g.setTranslateX(200);
        Transform t1 = n.getLocalToSceneTransform();
        TransformHelper.assertMatrix(t1,
                1, 0, 0, 200,
                0, 1, 0, 0,
                0, 0, 1, 0);

        g.setTranslateX(300);
        Transform t2 = n.getLocalToSceneTransform();
        TransformHelper.assertMatrix(t2,
                1, 0, 0, 300,
                0, 1, 0, 0,
                0, 0, 1, 0);

        assertFalse(t1 == t2);
        TransformHelper.assertMatrix(t1,
                1, 0, 0, 200,
                0, 1, 0, 0,
                0, 0, 1, 0);
    }

    @Test
    public void shouldGetProperValueWhenSiblingValidatesParent() {
        final Node n = new Rectangle(20, 20);
        final Node n2 = new Rectangle(20, 20);
        final Group g = new Group(n, n2);
        g.setTranslateX(50);

        // one of the nodes has listener
        n.localToSceneTransformProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable o) {
                notified = true;
                TransformHelper.assertMatrix(n.getLocalToSceneTransform(),
                    1, 0, 0, 60,
                    0, 1, 0,  0,
                    0, 0, 1,  0);
            }
        });

        // the other node reports the value correctly
        TransformHelper.assertMatrix(n2.getLocalToSceneTransform(),
            1, 0, 0, 50,
            0, 1, 0,  0,
            0, 0, 1,  0);

        // change the value on parent, child with listener is notified
        // and validates parent
        notified = false;
        g.setTranslateX(60);
        assertTrue(notified);

        // now the other node needs to report correctly updated value
        TransformHelper.assertMatrix(n2.getLocalToSceneTransform(),
            1, 0, 0, 60,
            0, 1, 0,  0,
            0, 0, 1,  0);
    }
}
