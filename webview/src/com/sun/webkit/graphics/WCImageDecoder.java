/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.graphics;


public abstract class WCImageDecoder {

    /**
     * Receives a portion of image data.
     *
     * @param data  a portion of image data,
     *              or {@code null} if all data received
     */
    protected abstract void addImageData(byte[] data);

    /**
     * Returns image size.
     * @param size a buffer of size 2.
     */
    protected abstract void getImageSize(int[] size);

    /**
     * Returns a number of frames of the decoded image.
     *
     * @return  a number of image frames
     */
    protected abstract int getFrameCount();

    /*
     * Returns image frame at the specified index. The [data] parameter is
     * either null or an array of size 5. If non-null, it should be filled
     * as follows:
     *  1 if the frame is complete, 0 otherwise;
     *  frame width
     *  frame height
     *  frame duration in milliseconds
     *  1 if the frame has translucent pixels, 0 otherwise
     */
    protected abstract WCImageFrame getFrame(int idx, int[] data);

    protected abstract void loadFromResource(String name);

    protected abstract void destroy();

    protected abstract String getFilenameExtension();
}
