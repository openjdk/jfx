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

package com.oracle.javafx.jmx;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;

import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Window;
import javafx.embed.swing.SwingFXUtils;

import com.oracle.javafx.jmx.json.JSONDocument;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import com.sun.javafx.jmx.HighlightRegion;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.Toolkit;
import com.sun.media.jfxmedia.AudioClip;
import com.sun.media.jfxmedia.MediaManager;
import com.sun.media.jfxmedia.MediaPlayer;
import com.sun.media.jfxmedia.events.PlayerStateEvent.PlayerState;

/**
 * Default implementation of {@link SGMXBean} interface.
 */
public class SGMXBeanImpl implements SGMXBean, MXNodeAlgorithm {

    private static final String SGMX_NOT_PAUSED_TEXT = "Scene-graph is not PAUSED.";
    private static final String SGMX_CALL_GETSGTREE_FIRST = "You need to call getSGTree() first.";

    private boolean paused = false;

    private Map<Integer, Window> windowMap = null;
    private JSONDocument jwindows = null;
    private Map<Integer, Node> nodeMap = null;
    private JSONDocument[] jsceneGraphs = null;
    private Map<Scene, BufferedImage> scene2Image = null;

    private List<MediaPlayer> playersToResume = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void pause() {
        if (paused) {
            return;
        }
        paused = true;
        releaseAllStateObject();
        Toolkit tk = Toolkit.getToolkit();
        tk.pauseScenes();
        pauseMedia();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resume() {
        if (!paused) {
            return;
        }
        paused = false;
        releaseAllStateObject();
        Toolkit tk = Toolkit.getToolkit();
        tk.resumeScenes();
        resumeMedia();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void step() throws IllegalStateException {
        if (!paused) {
            throw new IllegalStateException(SGMX_NOT_PAUSED_TEXT);
        }

        releaseAllStateObject();

        Toolkit tk = Toolkit.getToolkit();
        final CountDownLatch onePulseLatch = new CountDownLatch(1);

        tk.setLastTkPulseListener(new TKPulseListener() {
            @Override public void pulse() {
                onePulseLatch.countDown();
            }
        });

        tk.resumeScenes();

        try {
            onePulseLatch.await();
        } catch (InterruptedException e) { }

        tk.pauseScenes();
        tk.setLastTkPulseListener(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWindows() throws IllegalStateException {
        if (!paused) {
            throw new IllegalStateException(SGMX_NOT_PAUSED_TEXT);
        }
        importWindowsIfNeeded();
        return jwindows.toJSON();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSGTree(int windowId) throws IllegalStateException {
        if (!paused) {
            throw new IllegalStateException(SGMX_NOT_PAUSED_TEXT);
        }
        importWindowsIfNeeded();
        if (nodeMap == null) {
             nodeMap = new LinkedHashMap<Integer, Node>();
        }
        if (jsceneGraphs[windowId] == null) {
            final Window window = windowMap.get(windowId);
            this.importSGTree(window.getScene().getRoot(), windowId);
        }
        return jsceneGraphs[windowId].toJSON();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHighlightedNode(int nodeId) throws IllegalStateException {
        if (!paused) {
            throw new IllegalStateException(SGMX_NOT_PAUSED_TEXT);
        }
        Toolkit.getToolkit().getHighlightedRegions().add(
                                createHighlightRegion(nodeId));
        getNode(nodeId).getScene().impl_getPeer().markDirty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeHighlightedNode(int nodeId) throws IllegalStateException {
        if (!paused) {
            throw new IllegalStateException(SGMX_NOT_PAUSED_TEXT);
        }
        Toolkit.getToolkit().getHighlightedRegions().remove(
                                createHighlightRegion(nodeId));
        getNode(nodeId).getScene().impl_getPeer().markDirty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHighlightedRegion(int windowId, double x, double y, double w, double h)
        throws IllegalStateException
    {
        if (!paused) {
            throw new IllegalStateException(SGMX_NOT_PAUSED_TEXT);
        }
        TKScene scenePeer = getScene(windowId).impl_getPeer();
        Toolkit.getToolkit().getHighlightedRegions().add(
                          new HighlightRegion(scenePeer, x, y, w, h));
        scenePeer.markDirty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeHighlightedRegion(int windowId, double x, double y, double w, double h)
        throws IllegalStateException
    {
        if (!paused) {
            throw new IllegalStateException(SGMX_NOT_PAUSED_TEXT);
        }
        TKScene scenePeer = getScene(windowId).impl_getPeer();
        Toolkit.getToolkit().getHighlightedRegions().remove(
                          new HighlightRegion(scenePeer, x, y, w, h));
        scenePeer.markDirty();
    }

    private Node getNode(int nodeId) {
        if (nodeMap == null) {
            throw new IllegalStateException(SGMX_CALL_GETSGTREE_FIRST);
        }
        Node node = nodeMap.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Wrong node id.");
        }
        return node;
    }

    private Scene getScene(int windowId) {
        if (windowMap == null) {
            throw new IllegalStateException(SGMX_CALL_GETSGTREE_FIRST);
        }
        Window window = windowMap.get(windowId);
        if (window == null) {
            throw new IllegalArgumentException("Wrong window id.");
        }
        return window.getScene();
    }

    private HighlightRegion createHighlightRegion(int nodeId) {
        Node node = getNode(nodeId);
        Bounds bounds = node.localToScene(node.getBoundsInLocal());
        return new HighlightRegion(node.getScene().impl_getPeer(),
                                   bounds.getMinX(),
                                   bounds.getMinY(),
                                   bounds.getWidth(),
                                   bounds.getHeight());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String makeScreenShot(int nodeId) throws IllegalStateException {
        if (!paused) {
            throw new IllegalStateException(SGMX_NOT_PAUSED_TEXT);
        }
        if (nodeMap == null) {
            throw new IllegalStateException(SGMX_CALL_GETSGTREE_FIRST);
        }

        Node node = nodeMap.get(nodeId);
        if (node == null) {
            return null;
        }

        Scene scene = node.getScene();
        Bounds sceneBounds = node.localToScene(node.getBoundsInLocal());
        return getScreenShotPath(scene, sceneBounds.getMinX(), sceneBounds.getMinY(),
                sceneBounds.getWidth(), sceneBounds.getHeight());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String makeScreenShot(int windowId, double x, double y, double w, double h)
        throws IllegalStateException
    {
        if (!paused) {
            throw new IllegalStateException(SGMX_NOT_PAUSED_TEXT);
        }
        if (nodeMap == null) {
            throw new IllegalStateException(SGMX_CALL_GETSGTREE_FIRST);
        }
        Scene scene = getScene(windowId);
        return getScreenShotPath(scene, x, y, w, h);
    }

    private String getScreenShotPath(Scene scene, double x, double y, double w, double h) {
        if (scene2Image == null) {
            scene2Image = new LinkedHashMap<Scene, BufferedImage>();
        }

        BufferedImage bufferedImage = scene2Image.get(scene);
        if (bufferedImage == null) {
            Image fxImage = scene.snapshot(null);
            bufferedImage  = SwingFXUtils.fromFXImage(fxImage, null);
            scene2Image.put(scene, bufferedImage);
        }

        BufferedImage nodeImage = bufferedImage.getSubimage((int)x, (int)y, (int)w, (int)h);

        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("jfx", ".png");
            ImageIO.write(nodeImage, "PNG", tmpFile);
            tmpFile.deleteOnExit();
            return tmpFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void releaseAllStateObject() {
        clearWindowMap();
        jwindows = null;
        clearNodeMap();
        jsceneGraphs = null;
        clearScene2Image();
    }

    private void clearWindowMap() {
        if (windowMap != null) {
            windowMap.clear();
            windowMap = null;
        }
    }

    private void clearNodeMap() {
        if (nodeMap != null) {
            nodeMap.clear();
            nodeMap = null;
        }
     }

    private void clearScene2Image() {
        if (scene2Image != null) {
            scene2Image.clear();
            scene2Image = null;
        }
    }

    private void importWindowsIfNeeded() {
        if (windowMap == null) {
            windowMap = new LinkedHashMap<Integer, Window>();
            this.importWindows();
        }
    }

    private void importWindows() {
        int windowCount = 0;
        final Iterator<Window> it = Window.impl_getWindows();

        jwindows = JSONDocument.createArray();
        while (it.hasNext()) {
            final Window window = it.next();

            windowMap.put(windowCount, window);

            final JSONDocument jwindow = JSONDocument.createObject();
            jwindow.setNumber("id", windowCount);
            jwindow.setString("type", window.impl_getMXWindowType());
            jwindows.array().add(jwindow);
            windowCount++;
        }

        jsceneGraphs = new JSONDocument[windowCount];
    }

    private void importSGTree(Node sgRoot, int windowId) {
        if (sgRoot == null) {
            return;
        }
        jsceneGraphs[windowId] = (JSONDocument)sgRoot.impl_processMXNode(this,
            new MXNodeAlgorithmContext(nodeMap.size()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCSSInfo(int nodeId) throws IllegalStateException {
        if (!paused) {
            throw new IllegalStateException(SGMX_NOT_PAUSED_TEXT);
        }
        if (nodeMap == null) {
            throw new IllegalStateException(SGMX_CALL_GETSGTREE_FIRST);
        }

        Node node = nodeMap.get(nodeId);
        if (node == null) {
            return null;
        }

        JSONDocument d = new JSONDocument(JSONDocument.Type.OBJECT);

        List<CssMetaData<? extends Styleable, ?>> styleables = node.getCssMetaData();

        for (CssMetaData sp: styleables) {
            processCssMetaData(sp, node, d);
        }

        return d.toJSON();
    }

    private static void processCssMetaData(CssMetaData sp, Node node, JSONDocument d) {

        List<CssMetaData> subProps = sp.getSubProperties();
        if (subProps != null && !subProps.isEmpty()) {
            for (CssMetaData subSp: subProps) {
                processCssMetaData(subSp, node, d);
            }
        }

        try {
            StyleableProperty writable = sp.getStyleableProperty(node);
            Object value = writable != null ? writable.getValue() : null;
            if (value != null) {
                d.setString(sp.getProperty(), value.toString());
            } else {
                d.setString(sp.getProperty(), "null");
            }
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private static String upcaseFirstLetter(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBounds(int nodeId) throws IllegalStateException {
        if (!paused) {
            throw new IllegalStateException(SGMX_NOT_PAUSED_TEXT);
        }
        if (nodeMap == null) {
            throw new IllegalStateException(SGMX_CALL_GETSGTREE_FIRST);
        }

        Node node = nodeMap.get(nodeId);
        if (node == null) {
            return null;
        }

        Bounds sceneBounds = node.localToScene(node.getBoundsInLocal());
        JSONDocument d = JSONDocument.createObject();
        d.setNumber("x", sceneBounds.getMinX());
        d.setNumber("y", sceneBounds.getMinY());
        d.setNumber("w", sceneBounds.getWidth());
        d.setNumber("h", sceneBounds.getHeight());

        return d.toJSON();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object processLeafNode(Node node, MXNodeAlgorithmContext ctx) {
        return createJSONDocument(node, ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object processContainerNode(Parent parent, MXNodeAlgorithmContext ctx) {
        JSONDocument d = createJSONDocument(parent, ctx);

        final ObservableList<Node> children = parent.getChildrenUnmodifiable();

        JSONDocument childrenDoc = JSONDocument.createArray(children.size());
        d.set("children", childrenDoc);

        for (int i = 0; i < children.size(); i++) {
            childrenDoc.set(i, (JSONDocument)children.get(i).impl_processMXNode(this, ctx));
        }
        return d;
    }

    private JSONDocument createJSONDocument(Node n, MXNodeAlgorithmContext ctx) {
        int id = ctx.getNextInt();

        nodeMap.put(id, n);

        JSONDocument d = JSONDocument.createObject();
        d.setNumber("id", id);
        d.setString("class", n.getClass().getSimpleName());
        return d;
    }

    private void pauseMedia() {
        AudioClip.stopAllClips();

        List<MediaPlayer> allPlayers = MediaManager.getAllMediaPlayers();
        if (allPlayers == null) {
            return;
        }
        if ((!allPlayers.isEmpty()) && (playersToResume == null)) {
            playersToResume = new ArrayList<MediaPlayer>();
        }
        for (MediaPlayer player: allPlayers) {
            if (player.getState() == PlayerState.PLAYING) {
                player.pause();
                playersToResume.add(player);
            }
        }
    }

    private void resumeMedia() {
        if (playersToResume == null) {
            return;
        }
        for (MediaPlayer player: playersToResume) {
            player.play();
        }
        playersToResume.clear();
    }
}
