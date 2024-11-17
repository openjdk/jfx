/*
 * Copyright (c) 2017, 2024, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include "DragImage.h"
#include "CachedImage.h"
#include "NotImplemented.h"

namespace WebCore {

// ---- DragImage.h ---- //
IntSize dragImageSize(DragImageRef pr)
{
    return pr ? roundedIntSize(pr->size()) : IntSize();
}

DragImageRef scaleDragImage(DragImageRef pr, FloatSize)
{
    //TODO: pass to java
    notImplemented();
    return pr;
}

DragImageRef dissolveDragImageToFraction(DragImageRef pr, float)
{
    //TODO: pass to java
    notImplemented();
    return pr;
}

DragImageRef createDragImageFromImage(Image* img, ImageOrientation)
{
    return img;
}

DragImageRef createDragImageIconForCachedImage(CachedImage *cimg)
{
    if (cimg->hasImage()) return nullptr;
    return createDragImageFromImage(cimg->image(), ImageOrientation::Orientation::None); // todo tav valid orientation?
}

DragImageRef createDragImageForLink(Element&, URL&, const String&, TextIndicatorData&, float)
{
    return nullptr;
}

void deleteDragImage(DragImageRef)
{
    // Since DragImageRef is a RefPtr, there's nothing additional we need to do to
    // delete it. It will be released when it falls out of scope.
}

DragImageRef createDragImageIconForCachedImageFilename(const String&)
{
    return nullptr;
}

DragImageRef createDragImageForColor(const Color&, const FloatRect&, float, Path&)
{
    return nullptr;
}
}
