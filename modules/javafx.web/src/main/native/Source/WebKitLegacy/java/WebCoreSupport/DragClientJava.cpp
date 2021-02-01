/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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


#include "DragClientJava.h"
#include "WebPage.h"

#include <WebCore/DataTransfer.h>
#include <WebCore/Frame.h>
#include <WebCore/NotImplemented.h>
#include <WebCore/Page.h>
#include <wtf/Vector.h>
#include <wtf/text/WTFString.h>

namespace WebCore {

DragClientJava::DragClientJava(const JLObject &webPage)
    : m_webPage(webPage)
{
}

DragClientJava::~DragClientJava()
{
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

//We work in window rather than view coordinates here
OptionSet<DragSourceAction> DragClientJava::dragSourceActionMaskForPoint(const IntPoint&)
{
    //TODO: check input element and produce correct respond
    notImplemented();
    return WebCore::anyDragSourceAction();
}

void DragClientJava::startDrag(DragItem item, DataTransfer& dataTransfer, Frame&)
{
    auto& dragImage = item.image;
    auto dragImageOrigin = item.dragLocationInContentCoordinates;
    auto eventPos = item.eventPositionInContentCoordinates;
    auto dragSourceAction = item.sourceAction;

    JNIEnv* env = WTF::GetJavaEnv();
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

    // we are temporary changing dataTransfer security context
    // for transfer-to-Java purposes.
    auto actualStoreMode = dataTransfer.storeMode();
    dataTransfer.setStoreMode(DataTransfer::StoreMode::Readonly);

    Vector<String> mimeTypes(dataTransfer.types());
    JLObjectArray jmimeTypes(env->NewObjectArray(mimeTypes.size(), clsString, NULL));
    JLObjectArray jvalues(env->NewObjectArray(mimeTypes.size(), clsObject, NULL));
    WTF::CheckAndClearException(env); // OOME

    auto document = WebPage::pageFromJObject(m_webPage)->mainFrame().document();
    if (document) {
        int index = 0;
        for(const auto& mime : mimeTypes) {
            String value = dataTransfer.getData(*document, mime);

            env->SetObjectArrayElement(
                jmimeTypes,
                index,
                (jstring)mime.toJavaString(env));

            env->SetObjectArrayElement(
                jvalues,
                index,
                (jstring)value.toJavaString(env));
            index++;
        }
    }
    // restore the original store mode
    dataTransfer.setStoreMode(actualStoreMode);

    // Attention! [jimage] can be the instance of WCImage or WCImageFrame class.
    // The nature of raster is too different to make a conversion inside the native code.
    jobject jimage = dragImage.get() && dragImage.get()->javaImage()
                  ? jobject(*(dragImage.get()->javaImage())) : nullptr;

    bool isImageSource = dragSourceAction && (*dragSourceAction == DragSourceAction::Image);

    env->CallVoidMethod(m_webPage, mid, jimage,
        eventPos.x() - dragImageOrigin.x(),
        eventPos.y() - dragImageOrigin.y(),
        eventPos.x(),
        eventPos.y(),
        jobjectArray(jmimeTypes),
        jobjectArray(jvalues),
        bool_to_jbool(isImageSource));
    WTF::CheckAndClearException(env);
}

} // namespace WebCore
