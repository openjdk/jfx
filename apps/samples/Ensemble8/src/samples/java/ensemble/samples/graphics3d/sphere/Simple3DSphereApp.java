/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates.
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
package ensemble.samples.graphics3d.sphere;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * A sample that demonstrates features of PhongMaterial applied to a 3D Sphere.
 * Provided is a playground to exercise the following properties of material:
 * diffuse map and color, specular map, color and power, bump map,
 * self illumination map.
 * @sampleName 3D Sphere
 * @preview preview.png
 * @see javafx.scene.paint.PhongMaterial
 * @see javafx.scene.shape.Sphere
 * @see javafx.animation.Animation
 * @see javafx.animation.Interpolator
 * @see javafx.animation.RotateTransition
 * @see javafx.beans.binding.Bindings
 * @see javafx.beans.property.BooleanProperty
 * @see javafx.beans.property.DoubleProperty
 * @see javafx.beans.property.SimpleBooleanProperty
 * @see javafx.beans.property.SimpleDoubleProperty
 * @see javafx.scene.AmbientLight
 * @see javafx.scene.Group
 * @see javafx.scene.PerspectiveCamera
 * @see javafx.scene.PointLight
 * @see javafx.scene.SceneAntialiasing
 * @see javafx.scene.SubScene
 * @see javafx.scene.image.Image
 * @see javafx.scene.paint.Color
 * @see javafx.scene.transform.Rotate
 * @see javafx.scene.transform.Translate
 * @see javafx.util.Duration
 * @playground - (name="Material")
 * @playground material.diffuseColor
 * @playground diffuseMap
 * @playground material.specularColor
 * @playground specularMap
 * @playground material.specularPower (min=0, max=64)
 * @playground bumpMap
 * @playground selfIlluminationMap
 * @playground - (name="Light")
 * @playground sun.color
 * @playground sunLight
 * @playground sunDistance (min=5, max=150)
 * @playground - (name="Sphere")
 * @playground earth.drawMode
 * @playground earth.cullFace
 * @conditionalFeatures SCENE3D
 */
public class Simple3DSphereApp extends Application {

    private Sphere earth;
    private PhongMaterial material;
    private PointLight sun;
    private final DoubleProperty sunDistance = new SimpleDoubleProperty(100);
    private final BooleanProperty sunLight = new SimpleBooleanProperty(true);
    private final BooleanProperty diffuseMap = new SimpleBooleanProperty(true);
    private final BooleanProperty specularMap = new SimpleBooleanProperty(true);
    private final BooleanProperty bumpMap = new SimpleBooleanProperty(true);
    private final BooleanProperty selfIlluminationMap = new SimpleBooleanProperty(true);

    public Parent createContent() throws Exception {

        Image dImage = new Image(Simple3DSphereApp.class.getResource("earth-d.jpg").toExternalForm());
        Image nImage = new Image(Simple3DSphereApp.class.getResource("earth-n.jpg").toExternalForm());
        Image sImage = new Image(Simple3DSphereApp.class.getResource("earth-s.jpg").toExternalForm());
        Image siImage = new Image(Simple3DSphereApp.class.getResource("earth-l.jpg").toExternalForm());

        material = new PhongMaterial();
        material.setDiffuseColor(Color.WHITE);
        material.diffuseMapProperty().bind(
                Bindings.when(diffuseMap).then(dImage).otherwise((Image) null));
        material.setSpecularColor(Color.TRANSPARENT);
        material.specularMapProperty().bind(
                Bindings.when(specularMap).then(sImage).otherwise((Image) null));
        material.bumpMapProperty().bind(
                Bindings.when(bumpMap).then(nImage).otherwise((Image) null));
        material.selfIlluminationMapProperty().bind(
                Bindings.when(selfIlluminationMap).then(siImage).otherwise((Image) null));

        earth = new Sphere(5);
        earth.setMaterial(material);
        earth.setRotationAxis(Rotate.Y_AXIS);


        // Create and position camera
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.getTransforms().addAll(
                new Rotate(-20, Rotate.Y_AXIS),
                new Rotate(-20, Rotate.X_AXIS),
                new Translate(0, 0, -20));

        sun = new PointLight(Color.rgb(255, 243, 234));
        sun.translateXProperty().bind(sunDistance.multiply(-0.82));
        sun.translateYProperty().bind(sunDistance.multiply(-0.41));
        sun.translateZProperty().bind(sunDistance.multiply(-0.41));
        sun.lightOnProperty().bind(sunLight);

        AmbientLight ambient = new AmbientLight(Color.rgb(1, 1, 1));

        // Build the Scene Graph
        Group root = new Group();
        root.getChildren().add(camera);
        root.getChildren().add(earth);
        root.getChildren().add(sun);
        root.getChildren().add(ambient);

        RotateTransition rt = new RotateTransition(Duration.seconds(24), earth);
        rt.setByAngle(360);
        rt.setInterpolator(Interpolator.LINEAR);
        rt.setCycleCount(Animation.INDEFINITE);
        rt.play();

        // Use a SubScene
        SubScene subScene = new SubScene(root, 400, 300, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(camera);

        return new Group(subScene);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setResizable(false);
        Scene scene = new Scene(createContent());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
