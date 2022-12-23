/*
 * Copyright (c) 2009, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio.common;

import com.sun.javafx.iio.ImageFormatDescription;
import com.sun.javafx.iio.ImageLoadListener;
import com.sun.javafx.iio.ImageLoader;
import com.sun.javafx.iio.ImageMetadata;
import java.util.HashSet;
import java.util.Iterator;

public abstract class ImageLoaderImpl implements ImageLoader {

    protected ImageFormatDescription formatDescription;
    protected HashSet<ImageLoadListener> listeners;
    protected int lastPercentDone = -1;

    protected ImageLoaderImpl(ImageFormatDescription formatDescription) {
        if (formatDescription == null) {
            throw new IllegalArgumentException("formatDescription == null!");
        }

        this.formatDescription = formatDescription;
    }

    @Override
    public final ImageFormatDescription getFormatDescription() {
        return formatDescription;
    }

    @Override
    public final void addListener(ImageLoadListener listener) {
        if (listeners == null) {
            listeners = new HashSet<>();
        }
        listeners.add(listener);
    }

    @Override
    public final void removeListener(ImageLoadListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    protected void emitWarning(String warning) {
        if(listeners != null && !listeners.isEmpty()) {
            Iterator<ImageLoadListener> iter = listeners.iterator();
            while(iter.hasNext()) {
                ImageLoadListener l = iter.next();
                l.imageLoadWarning(this, warning);
            }
        }
    }

    protected void updateImageProgress(float percentageDone) {
        if (listeners != null && !listeners.isEmpty()) {
            int percentDone = (int) percentageDone;
            int delta = ImageTools.PROGRESS_INTERVAL;
            if ((delta * percentDone / delta) % delta == 0 && percentDone != lastPercentDone) {
                lastPercentDone = percentDone;
                Iterator<ImageLoadListener> iter = listeners.iterator();
                while (iter.hasNext()) {
                    ImageLoadListener listener = iter.next();
                    listener.imageLoadProgress(this, percentDone);
                }
            }
        }
    }

    protected void updateImageMetadata(ImageMetadata metadata) {
        if(listeners != null && !listeners.isEmpty()) {
            Iterator<ImageLoadListener> iter = listeners.iterator();
            while(iter.hasNext()) {
                ImageLoadListener l = iter.next();
                l.imageLoadMetaData(this, metadata);
            }
        }
    }
}
