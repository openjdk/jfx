/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
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
package ensemble;

import ensemble.playground.PlaygroundProperty;
import ensemble.samplepage.SamplePage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.ConditionalFeature;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * Descriptor for a ensemble sample. Everything the ui needs is determined at
 * compile time from the sample sources and stored in these SampleInfo objects so
 * we don't have to calculate anything at runtime.
 */
public class SampleInfo {

    // =============== BASICS ==================================================

    public final String name;
    public final String description;
    public final String ensemblePath;

    // =============== SOURCES & RESOURCES =====================================

    /** The base URI for all the source files and resources for this sample. */
    public final String baseUri;
    /** All the files needed by this sample. Relative to the sample base URI. */
    public final String[] resourceUrls;
    /** The URL for the source of the sample main file. Relative to the sample base URI */
    public final String mainFileUrl;
    /** Full classpath for sample's application class */
    public final String appClass;
    /** ClassPath Url for preview image of size 206x152 */
    public final String previewUrl;
    /** List of properties in the sample that can be played with */
    public final PlaygroundProperty[] playgroundProperties;
    /** List of features that require specific platform support */
    public final ConditionalFeature[] conditionalFeatures;
    /** If true, then the sample runs on embedded platform  */
    public final boolean runsOnEmbedded;

    // =============== RELATED =================================================

    /** Array of classpaths to related docs. */
    public final String[] apiClasspaths;
    /** Array of urls to related (non-api) docs given as pairs (url, name). */
    private final String[] docsUrls;
    /** Array of ensmeble paths to related samples. */
    public final String[] relatesSamplePaths;

    public SampleInfo(String name, String description, String ensemblePath, String baseUri, String appClass,
                  String previewUrl, String[] resourceUrls, String[] apiClasspaths,
                  String[] docsUrls, String[] relatesSamplePaths, String mainFileUrl,
                  PlaygroundProperty[] playgroundProperties, ConditionalFeature[] conditionalFeatures,
                  boolean runsOnEmbedded) {
        this.name = name;
        this.description = description;
        this.ensemblePath = ensemblePath;
        this.baseUri = baseUri;
        this.appClass = appClass;
        this.resourceUrls = resourceUrls;
        this.mainFileUrl = mainFileUrl;
        this.apiClasspaths = apiClasspaths;
        this.docsUrls = docsUrls;
        this.relatesSamplePaths = relatesSamplePaths;
        this.playgroundProperties = playgroundProperties;
        this.conditionalFeatures = conditionalFeatures;
        this.runsOnEmbedded = runsOnEmbedded;

        if (EnsembleApp.PRELOAD_PREVIEW_IMAGES) {
            // Note: there may be missing classes/resources due to some filtering
            if (PlatformFeatures.USE_EMBEDDED_FILTER && !runsOnEmbedded) {
                // we should skip loading this image which will not ever be shown
            } else {
                java.net.URL url = getClass().getResource(previewUrl);
                if (url != null) {
                    getImage(url.toExternalForm());
                } else {
                    // mark this previewURL as missing
                    System.out.println("Note: Sample preview "+ensemblePath+" not found");
                    previewUrl = null;
                }
            }
        }

        this.previewUrl = previewUrl;
    }

    @Override public String toString() {
        return name;
    }

    // 460 x 345 - 5x5px border = 450x335
    public Node getLargePreview() {
//        if (previewUrl != null) {
//            String url = getClass().getResource(previewUrl).toExternalForm();
//            label.setBackground(new Background(
//                new BackgroundImage(
//                    new Image(url),
//                    BackgroundRepeat.NO_REPEAT,
//                    BackgroundRepeat.NO_REPEAT,
//                    new BackgroundPosition(Side.LEFT,5,false, Side.TOP,5,false),
//                    new BackgroundSize(450, 335, false, false, false, false)
//                )));
//        }

        return new LargePreviewRegion();
    }

    // 216x162 - 5+5px border = 206x152
    public Node getMediumPreview() {
        Region label = new Region();
        if (previewUrl != null) {
            String url = getClass().getResource(previewUrl).toExternalForm();
            label.setBackground(
                    new Background(
                            new BackgroundFill[]{
                                new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)
                            },
                            new BackgroundImage[]{
                                new BackgroundImage(
                                    getImage(url),
                                    BackgroundRepeat.NO_REPEAT,
                                    BackgroundRepeat.NO_REPEAT,
                                    new BackgroundPosition(Side.LEFT,5,false, Side.TOP,5,false),
                                    new BackgroundSize(206, 152, false, false, false, false)
                                )
                            }
                    ));
        }
        label.getStyleClass().add("sample-medium-preview");
        label.setMinSize(216, 162);
        label.setPrefSize(216, 162);
        label.setMaxSize(216, 162);
        return label;
    }

    public SampleRuntimeInfo buildSampleNode() {
        try {
            Method play = null;
            Method stop = null;
            Class clz = Class.forName(appClass);
            final Object app = clz.newInstance();
            Parent root = (Parent) clz.getMethod("createContent").invoke(app);

            for (Method m : clz.getMethods()) {
                switch(m.getName()) {
                    case "play":
                        play = m;
                        break;
                    case "stop":
                        stop = m;
                        break;
                }
            }
            final Method fPlay = play;
            final Method fStop = stop;

            root.sceneProperty().addListener((ObservableValue<? extends Scene> ov, Scene oldScene, Scene newScene) -> {
                try {
                    if (oldScene != null && fStop != null) {
                        fStop.invoke(app);
                    }
                    if (newScene != null && fPlay != null) {
                        fPlay.invoke(app);
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    Logger.getLogger(SamplePage.class.getName()).log(Level.SEVERE, null, ex);
                }
            });

            return new SampleRuntimeInfo(root, app, clz);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(SamplePage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new SampleRuntimeInfo(new Pane(), new Object(), Object.class);
    }
    private static final Image SAMPLE_BACKGROUND = getImage(
            SampleInfo.class.getResource("images/sample-background.png").toExternalForm());
    private class LargePreviewRegion extends Region {
        private final Node sampleNode = buildSampleNode().getSampleNode();
        private final Label label = new Label();
        private final ImageView background = new ImageView(SAMPLE_BACKGROUND);

        public LargePreviewRegion() {
            getStyleClass().add("sample-large-preview");
            label.setText(name);
            label.getStyleClass().add("sample-large-preview-label");
            label.setAlignment(Pos.BOTTOM_CENTER);
            label.setWrapText(true);
            getChildren().addAll(background,sampleNode,label);
        }

        @Override protected double computeMinWidth(double height) { return 460; }
        @Override protected double computeMinHeight(double width) { return 345; }
        @Override protected double computePrefWidth(double height) { return 460; }
        @Override protected double computePrefHeight(double width) { return 345; }

        @Override protected void layoutChildren() {
            double labelHeight = label.prefHeight(440);
            background.setLayoutX(5);
            background.setLayoutY(5);
            background.setFitWidth(450);
            background.setFitHeight(335);
            sampleNode.setLayoutX(10);
            sampleNode.setLayoutY(10);
            sampleNode.resize(440, 315-labelHeight);
            label.setLayoutX(10);
            label.setLayoutY(345 - 15 - labelHeight);
            label.resize(440, labelHeight);
        }
    }

    private List<URL> relatedSampleURLs = new AbstractList<URL>() {

        @Override
        public URL get(final int index) {
            return new URL() {

                @Override
                public String getURL() {
                    return relatesSamplePaths[index];
                }

                @Override
                public String getName() {
                    String url = getURL();
                    return url.substring(url.lastIndexOf('/') + 1);
                }
            };
        }

        @Override
        public int size() {
            return relatesSamplePaths.length;
        }
    };

    public List<URL> getRelatedSampleURLs() {
        return relatedSampleURLs;
    }

    private List<URL> docURLs = new AbstractList<URL>() {

        @Override
        public URL get(final int index) {
            return new URL() {

                @Override
                public String getURL() {
                    return docsUrls[index * 2];
                }

                @Override
                public String getName() {
                    return docsUrls[index * 2 + 1];
                }
            };
        }

        @Override
        public int size() {
            return docsUrls.length / 2;
        }
    };

    public List<URL> getDocURLs() {
        return docURLs;
    }

    private List<URL> sources = new AbstractList<URL>() {

        @Override
        public URL get(final int index) {
            return new URL() {

                @Override
                public String getURL() {
                    return resourceUrls[index];
                }

                @Override
                public String getName() {
                    String url = getURL();
                    return url.substring(url.lastIndexOf('/') + 1);
                }
            };
        }

        @Override
        public int size() {
            return resourceUrls.length;
        }
    };

    public List<URL> getSources() {
        return sources;
    }

    public boolean needsPlayground() {
        return playgroundProperties.length > 0;
    }

    public static interface URL {
        String getURL();
        String getName();
    }

    private static Map<String, Image> imageCache;

    private static Image getImage(String url) {
        if (imageCache == null) {
            imageCache = new WeakHashMap<>();
        }
        Image image = imageCache.get(url);
        if (image == null) {
            image = new Image(url);
            imageCache.put(url, image);
        }
        return image;
    }

    public static class SampleRuntimeInfo {
        private final Parent sampleNode;
        private final Object app;
        private final Class clz;

        public SampleRuntimeInfo(Parent sampleNode, Object app, Class clz) {
            this.sampleNode = sampleNode;
            this.app = app;
            this.clz = clz;
        }

        public Object getApp() {
            return app;
        }

        public Class getClz() {
            return clz;
        }

        public Parent getSampleNode() {
            return sampleNode;
        }
    }
}
