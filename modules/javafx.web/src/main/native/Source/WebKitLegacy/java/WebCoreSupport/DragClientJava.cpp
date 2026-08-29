/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include <WebCore/PlatformJavaClasses.h>

#include <WebCore/DataTransfer.h>
#include <WebCore/Frame.h>
#include "LocalFrameInlines.h"
#include <WebCore/NotImplemented.h>
#include <WebCore/Page.h>
#include <wtf/Vector.h>
#include <wtf/text/WTFString.h>

namespace WebCore {

WTF_MAKE_TZONE_ALLOCATED_IMPL(DragClientJava);

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

void DragClientJava::startDrag(DragItem item, DataTransfer& dataTransfer, Frame& localFrame,const std::optional<NodeIdentifier>& nodeIdentifier)
{
    auto& dragImage = item.image;
    auto dragImageOrigin = item.dragLocationInContentCoordinates;
    auto eventPos = item.eventPositionInContentCoordinates;
    auto dragSourceAction = item.sourceAction;

    if (!m_callbacks || !m_callbacks->start_drag || !m_page)
        return;

    // we are temporary changing dataTransfer security context
    // for transfer-to-Java purposes.
    auto actualStoreMode = dataTransfer.storeMode();
    dataTransfer.setStoreMode(DataTransfer::StoreMode::Readonly);
    auto& localFrameRef = downcast<LocalFrame>(localFrame);
    Vector<String> mimeTypes(dataTransfer.types(*localFrameRef.document()));

    /*
     * The two Java arrays the JNI code allocated become two arrays of (pointer, length)
     * that live for the duration of the call. Each WKJStringArg owns its characters, so
     * they are held in vectors rather than built as temporaries.
     */
    Vector<std::unique_ptr<WKJStringArg>> mimeArgs;
    Vector<std::unique_ptr<WKJStringArg>> valueArgs;
    Vector<const uint16_t*> mimePtrs;
    Vector<int32_t> mimeLengths;
    Vector<const uint16_t*> valuePtrs;
    Vector<int32_t> valueLengths;

    auto document = (dynamicDowncast<LocalFrame>(m_page->mainFrame()))->document();
    if (document) {
        for(const auto& mime : mimeTypes) {
            String value = dataTransfer.getData(*document, mime);

            mimeArgs.append(makeUnique<WKJStringArg>(mime));
            valueArgs.append(makeUnique<WKJStringArg>(value));
            mimePtrs.append(mimeArgs.last()->data());
            mimeLengths.append(mimeArgs.last()->length());
            valuePtrs.append(valueArgs.last()->data());
            valueLengths.append(valueArgs.last()->length());
        }
    }
    // restore the original store mode
    dataTransfer.setStoreMode(actualStoreMode);

    // Attention! [image] can be the instance of WCImage or WCImageFrame class.
    // The nature of raster is too different to make a conversion inside the native code,
    // so Java still receives one object of one of two classes and decides which.
    wkj_ref image = 0;
    if (dragImage.get() && dragImage.get()->javaImage()) {
        RefPtr<RQRef> rqImage = dragImage.get()->javaImage()->platformImage()->getImage();
        if (rqImage)
            image = static_cast<wkj_ref>(*rqImage);
    }

    bool isImageSource = dragSourceAction && (*dragSourceAction == DragSourceAction::Image);

    m_callbacks->start_drag(m_pageRef, image,
        eventPos.x() - dragImageOrigin.x(),
        eventPos.y() - dragImageOrigin.y(),
        eventPos.x(),
        eventPos.y(),
        mimePtrs.span().data(), mimeLengths.span().data(),
        valuePtrs.span().data(), valueLengths.span().data(),
        static_cast<int32_t>(mimePtrs.size()),
        isImageSource ? 1 : 0);
}

} // namespace WebCore
