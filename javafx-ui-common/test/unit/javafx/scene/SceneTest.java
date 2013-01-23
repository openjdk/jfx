/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests various aspects of Scene.
 *
 */
public class SceneTest {

    private Stage stage;
    private boolean handler1Called = false;
    private boolean handler2Called = false;


    @Before
    public void setUp() {
        stage = new Stage();
        stage.show();
        stage.requestFocus();
    }

    @After
    public void tearDown() {
        stage.hide();
    }

    /***************************************************************************
     *                                                                         *
     *                           Lookup related tests                          *
     *                                                                         *
     **************************************************************************/
    @Test
    public void testLookupCorrectId() {
        Node n;
        Group root = new Group();
        Scene scene = new Scene(root);
        Rectangle a = new Rectangle(); a.setId("a");
        Rectangle b = new Rectangle(); a.setId("b");
        Rectangle c = new Rectangle(); a.setId("c");
        Group g = new Group();
        g.setId("d");
        
        Rectangle r1 = new Rectangle(); a.setId("1");
        Rectangle r2 = new Rectangle(); a.setId("2");
        Rectangle r3 = new Rectangle(); a.setId("3");
        n = new Rectangle(); n.setId("4");
        Rectangle r5 = new Rectangle(); a.setId("5");
        Rectangle r6 = new Rectangle(); a.setId("6");
        
        Rectangle e = new Rectangle(); a.setId("e");
        Rectangle f = new Rectangle(); a.setId("f");
        
        g.getChildren().addAll(r1,r2,r3,n,r5,r6);

        root.getChildren().addAll(a,b,c,g,e,f);
        
        assertEquals(n, scene.lookup("#4"));
    }

    @Test
    public void testLookupBadId() {
        Node n;
        Group root = new Group();
        Scene scene = new Scene(root);
        Rectangle a = new Rectangle(); a.setId("a");
        Rectangle b = new Rectangle(); a.setId("b");
        Rectangle c = new Rectangle(); a.setId("c");
        Group g = new Group();
        g.setId("d");
        
        Rectangle r1 = new Rectangle(); a.setId("1");
        Rectangle r2 = new Rectangle(); a.setId("2");
        Rectangle r3 = new Rectangle(); a.setId("3");
        n = new Rectangle(); n.setId("4");
        Rectangle r5 = new Rectangle(); a.setId("5");
        Rectangle r6 = new Rectangle(); a.setId("6");
        
        Rectangle e = new Rectangle(); a.setId("e");
        Rectangle f = new Rectangle(); a.setId("f");
        
        g.getChildren().addAll(r1,r2,r3,n,r5,r6);
        
        root.getChildren().addAll(a,b,c,g,e,f);
        
        assertNull(scene.lookup("#4444"));
    }
    
    /***************************************************************************
     *                                                                         *
     *                          Scene Content Tests                            *
     *                                                                         *
     **************************************************************************/
    @Test(expected=NullPointerException.class)
    public void testNullRoot() {
        Scene scene = new Scene(null);
    }

    @Test(expected=NullPointerException.class)
    public void testSetNullRoot() {
        Scene scene = new Scene(new Group());
        scene.setRoot(null);
    }

    @Test
    public void testRootInitializedInConstructor() {
        Group g = new Group();
        Scene scene = new Scene(g);

        assertEquals(g, scene.getRoot());
        assertEquals(scene, g.getScene());
    }

    @Test
    public void testDepthBufferInitializedInConstructor() {
        Group g = new Group();
        Scene scene = new Scene(g, 100, 100, true);

        assertTrue(scene.isDepthBuffer());
    }

    @Test
    public void testRootUpdatedWhenAddedToScene() {
        Scene scene = new Scene(new Group());

        Group g = new Group();
        scene.setRoot(g);

        assertEquals(g, scene.getRoot());
        assertEquals(scene, g.getScene());
    }

    @Test
    public void testRootUpdatedWhenChangedInScene() {
        Group g = new Group();
        Scene scene = new Scene(g);

        Group g2 = new Group();
        scene.setRoot(g2);

        assertNull(g.getScene());
        assertEquals(g2, scene.getRoot());
        assertEquals(scene, g2.getScene());
    }

    @Test 
    public void testNodeUpdatedWhenAddedToScene() {
        Group root = new Group();
        Scene scene = new Scene(root);
        Rectangle rect = new Rectangle();
        
        assertNull(rect.getScene());
        
        root.getChildren().add(rect);
        
        assertEquals(scene, rect.getScene());
    }
    
    @Test
    public void testNodeUpdatedWhenRemovedFromScene() {
        Rectangle rect;
        Group root = new Group();
        Scene scene = new Scene(root);
        root.getChildren().add(rect = new Rectangle());
        
        assertEquals(scene, rect.getScene());
        
        root.getChildren().remove(rect);
        
        assertNull(rect.getScene());
    }
    
    @Test
    public void testNodeTreeUpdatedWhenAddedToScene() {
        Rectangle rect;
        Group root = new Group();
        Scene scene = new Scene(root);
        Group g = new Group();
        
        g.getChildren().add(rect = new Rectangle());
            
        assertNull(rect.getScene());
        assertNull(g.getScene());

        root.getChildren().add(g);
                
        assertEquals(scene, g.getScene());
        assertEquals(scene, rect.getScene());
    }
    
    @Test
    public void testNodeTreeUpdatedWhenRemovedFromScene() {
        Rectangle rect;
        Group g;
        Group root = new Group();
        Scene scene = new Scene(root);
        root.getChildren().add(g = new Group());
        
        g.getChildren().add(rect = new Rectangle());
        
        assertEquals(scene, g.getScene());
        assertEquals(scene, rect.getScene());
        
        root.getChildren().remove(g);
        
        assertNull(rect.getScene());
        assertNull(g.getScene());
    }

    @Test
    public void testNodeTreeUpdatedWhenAddedToChildOfScene() {
        Group parentGroup;
        Group root = new Group();
        Scene scene = new Scene(root);
        root.getChildren().add(parentGroup = new Group());
        
        Rectangle rect;
        Group childGroup = new Group();
        childGroup.getChildren().add(rect = new Rectangle());
        
        assertNull(rect.getScene());
        assertNull(childGroup.getScene());
        assertEquals(scene, parentGroup.getScene());
        
        parentGroup.getChildren().add(childGroup);
        
        assertEquals(scene, rect.getScene());
        assertEquals(scene, childGroup.getScene());
        assertEquals(scene, parentGroup.getScene());
    }

    @Test
    public void testNodeTreeUpdatedWhenRemovedFromChildOfScene() {
        Group parentGroup;
        Group root = new Group();
        Scene scene = new Scene(root);
        root.getChildren().add(parentGroup = new Group());
        
        Rectangle rect;
        Group childGroup = new Group();
        parentGroup.getChildren().add(childGroup);
        childGroup.getChildren().add(rect = new Rectangle());
        
        assertEquals(scene, rect.getScene());
        assertEquals(scene, childGroup.getScene());
        assertEquals(scene, parentGroup.getScene());
        
        parentGroup.getChildren().remove(childGroup);
                
        assertNull(rect.getScene());
        assertNull(childGroup.getScene());
        assertEquals(scene, parentGroup.getScene());
    }

    @Test
    public void testSceneSizeSetWhenNotInitialized() {
        Group g = new Group();
        
        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(200);
        r.setHeight(200);
        g.getChildren().add(r);
        
        Scene scene = new Scene(g);
        stage.setScene(scene);
        
        assertEquals(180, (int) scene.getWidth());
        assertEquals(180, (int) scene.getHeight());
    }

    @Test
    public void testSceneSizeSetWithEffectOnRoot() {
        Group g = new Group();
        
        g.setEffect(new javafx.scene.effect.DropShadow());
        
        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        g.getChildren().add(r);
        r.setWidth(200);
        r.setHeight(200);
        
        Scene scene = new Scene(g);
        stage.setScene(scene);
        
        assertEquals(189, (int) scene.getWidth());
        assertEquals(189, (int) scene.getHeight());
    }

    @Test
    public void testSceneSizeSetWithClipOnRoot() {
        Group g = new Group();
        
        Rectangle clip = new Rectangle();
        clip.setX(20); clip.setY(20); clip.setWidth(150); clip.setHeight(150);
        
        g.setClip(clip);
        
        Rectangle r = new Rectangle();
        
        r.setX(20);
        r.setY(20);
        g.getChildren().add(r);
        r.setWidth(200);
        r.setHeight(200);
        
        Scene scene = new Scene(g);
        stage.setScene(scene);
        
        assertEquals(170,(int) scene.getWidth());
        assertEquals(170, (int) scene.getHeight());

    }

    @Test
    public void testSceneSizeSetWithTransformOnRoot() {
        Group g = new Group();
        
        Scale s = new Scale(); s.setX(2.0f); s.setY(2.0f);
        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        g.getChildren().add(r);
        r.setWidth(200);
        r.setHeight(200);
        
        g.getTransforms().add(s);
        Scene scene = new Scene(g);
        stage.setScene(scene);
        
        assertEquals(360,(int) scene.getWidth());
        assertEquals(360, (int) scene.getHeight());
    }

    @Test
    public void testSceneSizeSetWithScaleOnRoot() {
        Group g = new Group();
       
        g.setScaleX(2);
        g.setScaleY(2);
        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(200);
        r.setHeight(200);
        g.getChildren().add(r);
       
        Scene scene = new Scene(g);
        stage.setScene(scene);
        
        assertEquals(280,(int) scene.getWidth());
        assertEquals(280, (int) scene.getHeight());
    }

    @Test
    public void testSceneSizeSetWithRotationOnRoot() {
        Group g = new Group();
        g.setRotate(45);
        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(200);
        r.setHeight(200);
        g.getChildren().add(r);
        
        Scene scene = new Scene(g);
        stage.setScene(scene);
        
        assertTrue(scene.getWidth() > 220.0f && scene.getWidth() < 222.0f);
        assertTrue(scene.getHeight() > 220.0f && scene.getHeight() < 222.0f);
    }

    @Test
    public void testSceneSizeSetWithTranslateOnRoot() {
        Group g = new Group();
       
        g.setTranslateX(10);
        g.setTranslateY(10);
        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(200);
        r.setHeight(200);
        g.getChildren().add(r);
        Scene scene = new Scene(g);
        stage.setScene(scene);
        
        assertEquals(190, (int)scene.getWidth());
        assertEquals(190, (int)scene.getHeight());
    }

    @Test
    public void testSceneSizeSetWithResizableAsRoot() {
        StackPane st = new StackPane();
        
        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(200);
        r.setHeight(200);
        st.getChildren().add(r);

        Scene scene = new Scene(st);
        stage.setScene(scene);
        
        assertEquals(200,(int) scene.getWidth());
        assertEquals(200, (int) scene.getHeight());
    }
    
    @Test
    public void testSceneSizeWhenWidthInitialized() {
        Group g = new Group();

        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(100);
        r.setHeight(100);
        g.getChildren().add(r);

        Scene scene = new Scene(g, 200, -1);
        stage.setScene(scene);
        
        assertEquals(200,(int) scene.getWidth());
        assertEquals(80, (int) scene.getHeight());
    }

    @Test
    public void testSceneSizeWhenHeightInitialized() {
        Group g = new Group();

        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(100);
        r.setHeight(100);
        g.getChildren().add(r);

        Scene scene = new Scene(g, -1, 300);
        stage.setScene(scene);

        assertEquals(80,(int) scene.getWidth());
        assertEquals(300,(int) scene.getHeight());
    }

    @Test
    public void testSceneSizeWhenWidthAndHeightInitialized() {
        Group g = new Group();

        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(100);
        r.setHeight(100);
        g.getChildren().add(r);

        Scene scene = new Scene(g, 400, 400);
        stage.setScene(scene);

        assertEquals(400,(int) scene.getWidth());
        assertEquals(400, (int) scene.getHeight());
    }

    @Test
    public void testSceneSizeOverridesResizableRootPrefSize() {
        StackPane s = new StackPane();

        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(100);
        r.setHeight(100);
        s.getChildren().add(r);

        Scene scene = new Scene(s, 600, 600);
        stage.setScene(scene);

        assertEquals(600, (int) scene.getWidth());
        assertEquals(600, (int) scene.getHeight());
    }

    @Test
    public void testSceneSizeWithContentBiasOnRoot() {
        Rectangle r1 = new Rectangle(20, 20);
        Rectangle r2 = new Rectangle(20, 20);
        Rectangle r3 = new Rectangle(100, 20);
        
        TilePane tilePane = new TilePane();
        tilePane.getChildren().addAll(r1, r2);

        final VBox root = new VBox();
        root.getChildren().addAll(tilePane, r3);
        Scene scene = new Scene(root);
        stage.setScene(scene);
                
        assertEquals(100, (int) scene.getWidth());
        assertEquals(40, (int) scene.getHeight());
    }
    
    @Test
    public void focusChangeShouldBeAtomic() {
        final Group root = new Group();

        final Rectangle r1 = new Rectangle();
        final Rectangle r2 = new Rectangle();

        root.getChildren().addAll(r1, r2);
        final Scene scene = new Scene(root, 600, 600);
        stage.setScene(scene);

        r1.requestFocus();

        assertTrue(r1.isFocused());
        assertFalse(r2.isFocused());

        handler1Called = false;
        handler2Called = true;

        r1.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean focused) {
                assertFalse(focused); // r1 is being defocused
                assertTrue(r2.isFocused()); // r2 is already focused
                handler1Called = true;

                root.getChildren().remove(r2); // be evil: remove r2
            }
        });

        r2.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean focused) {
                assertTrue(focused); // r2 is being focused
                assertFalse(r1.isFocused()); // r1 is already defocused
                assertTrue(handler1Called); // r1 listener was called first
                handler2Called = true;
                // remove the listener otherwise thi final defocus calls it again
                r2.focusedProperty().removeListener(this);
            }
        });

        r2.requestFocus();
        assertTrue(handler2Called); // both listeners were called
    }

}



