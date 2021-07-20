/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "DataTransfer.h"
#include "Pasteboard.h"
#include "DragController.h"
#include "DragData.h"

namespace WebCore {

// FIXME: constants are from gtk port
const int DragController::MaxOriginalImageArea = 1500 * 1500;
const int DragController::DragIconRightInset = 7;
const int DragController::DragIconBottomInset = 3;
const float DragController::DragImageAlpha = 0.75f;

static bool copyKeyIsDown = false;
void setCopyKeyState(bool _copyKeyIsDown)
{
    copyKeyIsDown = _copyKeyIsDown;
}

Optional<DragOperation> DragController::dragOperation(const DragData& dragData)
{
    //Protects the page from opening URL by fake anchor drag.
    if (dragData.containsURL() && !m_didInitiateDrag)
        return DragOperation::Copy;

    return WTF::nullopt;
}

//uta: need to be fixed with usage of DragData pointer
bool DragController::isCopyKeyDown(const DragData&)
{
    //State has not direct connection with keyboard state.
    //Now it is imported from Java (user drag action).
    return copyKeyIsDown;
}

void DragController::declareAndWriteDragImage(DataTransfer& clipboard, Element& element, const URL& url, const String& label)
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
