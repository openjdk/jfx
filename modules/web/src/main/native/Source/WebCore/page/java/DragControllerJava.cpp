/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "NotImplemented.h"
#include "Clipboard.h"
#include "Pasteboard.h"
#include "DragController.h"
#include "DragData.h"

namespace WebCore {

    // FIXME: constants are from gtk port
    const int DragController::LinkDragBorderInset = 2;
    const int DragController::MaxOriginalImageArea = 1500 * 1500;
    const int DragController::DragIconRightInset = 7;
    const int DragController::DragIconBottomInset = 3;
    const float DragController::DragImageAlpha = 0.75f;

    static bool copyKeyIsDown = false;
    void setCopyKeyState(bool _copyKeyIsDown)
    {
        copyKeyIsDown = _copyKeyIsDown;
    }

    DragOperation DragController::dragOperation(DragData& dragData)
    {
        //Protects the page from opening URL by fake anchor drag.
        return dragData.containsURL(0) && !m_didInitiateDrag ? DragOperationCopy : DragOperationNone;
    }

    //uta: need to be fixed with usage of DragData pointer
    bool DragController::isCopyKeyDown(DragData& dragData)
    {
        //State has not direct connection with keyboard state.
        //Now it is imported from Java (user drag action).
        return copyKeyIsDown;
    }

    void DragController::declareAndWriteDragImage(Clipboard& clipboard, Element& element, const URL& url, const String& label)
    {
        clipboard.pasteboard().writeImage(element, url, label);
    }

    const IntSize &DragController::maxDragImageSize()
    {
        static const IntSize s(400, 400);
        return s;
    }

    void DragController::cleanupAfterSystemDrag()
    {
    }

} // namespace WebCore
