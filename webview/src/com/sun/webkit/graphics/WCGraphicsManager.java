/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.graphics;

import com.sun.webkit.SharedBuffer;
import com.sun.webkit.SimpleSharedBufferInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class WCGraphicsManager {

    private static final Logger logger =
            Logger.getLogger(WCGraphicsManager.class.getName());

    private final AtomicInteger idCount = new AtomicInteger(0);

    private final HashMap<Integer,Ref> refMap = new HashMap<Integer,Ref>();

    private static ResourceBundle imageProperties = null;
    private static WCGraphicsManager manager = null;

    public static void setGraphicsManager(WCGraphicsManager manager) {
        WCGraphicsManager.manager = manager;
    }

    public static WCGraphicsManager getGraphicsManager() {
        return manager;
    }

    protected abstract WCImageDecoder getImageDecoder();

    public abstract WCGraphicsContext createGraphicsContext(Object g);
    
    public abstract WCRenderQueue createRenderQueue(WCRectangle clip,
                                                    boolean opaque);

    protected abstract WCRenderQueue createBufferedContextRQ(WCImage image);

    public abstract WCPageBackBuffer createPageBackBuffer();

    protected abstract WCFont getWCFont(String name, boolean bold, boolean italic, float size);

    private WCFontCustomPlatformData fwkCreateFontCustomPlatformData(
            SharedBuffer sharedBuffer)
    {
        try {
            return createFontCustomPlatformData(
                    new SimpleSharedBufferInputStream(sharedBuffer));
        } catch (IOException ex) {
            logger.log(Level.FINEST,
                       "Error creating font custom platform data",
                       ex);
            return null;
        }
    }

    protected abstract WCFontCustomPlatformData createFontCustomPlatformData(
            InputStream inputStream) throws IOException;

    protected abstract WCPath createWCPath();

    protected abstract WCPath createWCPath(WCPath path);

    protected abstract WCImage createWCImage(int w, int h);

    protected abstract WCImage createRTImage(int w, int h);

    public abstract WCImage getIconImage(String iconURL);
    
    public abstract Object toPlatformImage(WCImage image);

    protected abstract WCImageFrame createFrame(int w, int h, ByteBuffer data);

    public static String getResourceName(String key) {
        if (imageProperties == null) {
            imageProperties = ResourceBundle.getBundle(
                    "com.sun.webkit.graphics.Images");
        }
        try {
            return imageProperties.getString(key);
        }
        catch (MissingResourceException exception) {
            return key;
        }
    }

    private void fwkLoadFromResource(String key, long bufPtr) {
        InputStream in = getClass().getResourceAsStream(getResourceName(key));
        if (in == null) {
            return;
        }

        byte[] buf = new byte[1024]; // big enough for most resources
        int count;
        try {
            while ((count = in.read(buf)) > -1) {
                append(bufPtr, buf, count);
            }
            in.close();
        } catch (IOException e) {
            // just use what we've read so far
        }
    }

    protected abstract WCTransform createTransform(double m00, double m10,
            double m01, double m11, double m02, double m12);

    protected String[] getSupportedMediaTypes() {
        // default implementation: nothing is supported
        return new String[0];
    }

    private WCMediaPlayer fwkCreateMediaPlayer(long nativePointer) {
        WCMediaPlayer mediaPlayer = createMediaPlayer();
        mediaPlayer.setNativePointer(nativePointer);
        return mediaPlayer;
    }

    protected abstract WCMediaPlayer createMediaPlayer();

    int createID() {
        return idCount.incrementAndGet();
    }

    synchronized void ref(Ref ref) {
        refMap.put(ref.getID(), ref);
    }

    synchronized Ref deref(Ref ref) {
        return refMap.remove(ref.getID());
    }

    synchronized Ref getRef(int id) {
        return refMap.get(id);
    }

    private static native void append(long bufPtr, byte[] data, int count);
}
