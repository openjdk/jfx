/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include "NotImplemented.h"

#include "Image.h"
#include "CachedImage.h"
#include "DragActions.h"
#include "DragClient.h"
#include <wtf/Vector.h>
#include <wtf/text/WTFString.h>

#include <wtf/java/JavaEnv.h>
#include "DragClientJava.h"

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

DragImageRef createDragImageFromImage(Image* img, ImageOrientationDescription)
{
    return img;
}

DragImageRef createDragImageIconForCachedImage(CachedImage *cimg)
{
    if (cimg->hasImage()) return nullptr;
    return createDragImageFromImage(cimg->image(), ImageOrientationDescription(RespectImageOrientation)); // todo tav valid orientation?
}

void deleteDragImage(DragImageRef)
{
    // Since DragImageRef is a RefPtr, there's nothing additional we need to do to
    // delete it. It will be released when it falls out of scope.
}

DragImageRef createDragImageIconForCachedImageFilename(const String&)
{
    return 0;
}


DragClientJava::DragClientJava(const JLObject &webPage)
    : m_webPage(webPage)
{
}

DragClientJava::~DragClientJava()
{
}

void DragClientJava::dragControllerDestroyed()
{
    delete this;
}

void DragClientJava::willPerformDragDestinationAction(
    DragDestinationAction,
    const DragData&)
{
    notImplemented();
}

void DragClientJava::willPerformDragSourceAction(
    DragSourceAction,
    const IntPoint&,
    DataTransfer&)
{
    notImplemented();
}

DragDestinationAction DragClientJava::actionMaskForDrag(const DragData&)
{
    //TODO: check input element and produce correct respond
    notImplemented();
    return DragDestinationActionAny;
}

//We work in window rather than view coordinates here
DragSourceAction DragClientJava::dragSourceActionMaskForPoint(const IntPoint&)
{
    //TODO: check input element and produce correct respond
    notImplemented();
    return DragSourceActionAny;
}

void DragClientJava::startDrag(
    DragImage dragImage,
    const IntPoint& dragImageOrigin,
    const IntPoint& eventPos,
    const FloatPoint&,
    DataTransfer& DataTransfer,
    Frame&,
    DragSourceAction dragSourceAction)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID mid = env->GetMethodID(
        PG_GetWebPageClass(env),
        "fwkStartDrag", "("
        "Ljava/lang/Object;"
        "II"
        "II"
        "[Ljava/lang/String;"
        "[Ljava/lang/Object;"
        "Z"
        ")V");
    ASSERT(mid);

    static JGClass clsString(env->FindClass("java/lang/String"));
    static JGClass clsObject(env->FindClass("java/lang/Object"));

    Vector<String> mimeTypes(DataTransfer.typesPrivate());
    JLObjectArray jmimeTypes(env->NewObjectArray(mimeTypes.size(), clsString, NULL));
    JLObjectArray jvalues(env->NewObjectArray(mimeTypes.size(), clsObject, NULL));
    CheckAndClearException(env); // OOME

    {
        //we are temporary changing DataTransfer security context
        //for transfer-to-Java purposes.

        DataTransferAccessPolicy actualJSPolicy = DataTransfer.policy();
        DataTransfer.setAccessPolicy(DataTransferAccessPolicy::Readable); //XXX DataTransferReadable);

        int index = 0;
        Vector<String>::const_iterator end = mimeTypes.end();
        for(Vector<String>::const_iterator i = mimeTypes.begin();
            end!=i;
            ++i, ++index)
        {
            String value( DataTransfer.getData(*i) );

            env->SetObjectArrayElement(
                jmimeTypes,
                index,
                (jstring)i->toJavaString(env));

            env->SetObjectArrayElement(
                jvalues,
                index,
                (jstring)value.toJavaString(env));
        }

        DataTransfer.setAccessPolicy(actualJSPolicy);
    }

    // Attention! [jimage] can be the instance of WCImage or WCImageFrame class.
    // The nature of raster is too different to make a conversion inside the native code.
    jobject jimage = dragImage.get() && dragImage.get()->javaImage()
                  ? jobject(*(dragImage.get()->javaImage())) : nullptr;

    bool isImageSource = dragSourceAction & DragSourceActionImage;

    env->CallVoidMethod(m_webPage, mid, jimage,
        eventPos.x() - dragImageOrigin.x(),
        eventPos.y() - dragImageOrigin.y(),
        eventPos.x(),
        eventPos.y(),
        jobjectArray(jmimeTypes),
        jobjectArray(jvalues),
        bool_to_jbool(isImageSource));
    CheckAndClearException(env);
}

} // namespace WebCore
