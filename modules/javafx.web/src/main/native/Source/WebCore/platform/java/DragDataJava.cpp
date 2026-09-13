/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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
#include "Frame.h"
#include "DragData.h"
#include "Range.h"

#include "DataObjectJava.h"
#include "DataTransfer.h"
#include "DocumentFragment.h"
#include "markup.h"
#include "NotImplemented.h"

#include <wtf/text/WTFString.h>
#include <wtf/URL.h>

namespace WebCore {

bool DragData::containsURL(FilenameConversionPolicy /*= ConvertFilenames*/) const
{
    /* utaTODO: extent the functionality
    */
    return m_platformDragData->containsURL();
}

String DragData::asURL(FilenameConversionPolicy, String* title) const
{
    /* utaTODO: extent the functionality
    String url;
    if (m_platformDragData->hasValidURL())
        url = m_platformDragData->getURL().string();
    else if (filenamePolicy == ConvertFilenames && !m_platformDragData->filenames.isEmpty()) {
        String fileName = m_platformDragData->filenames[0];
        fileName = ChromiumBridge::getAbsolutePath(fileName);
        url = ChromiumBridge::filePathToURL(fileName).string();
    }
    */
    return m_platformDragData->asURL(title);
}

bool DragData::containsFiles() const
{
    return m_platformDragData->containsFiles();
}

Vector<String> DragData::asFilenames() const
{
    return m_platformDragData->asFilenames();
}

bool DragData::containsPlainText() const
{
    return m_platformDragData->containsPlainText();
}

String DragData::asPlainText() const
{
    return m_platformDragData->asPlainText();
}

bool DragData::canSmartReplace() const
{
    // Mimic the situations in which mac allows drag&drop to do a smart replace.
    // This is allowed whenever the drag data contains a 'range' (ie.,
    // ClipboardWin::writeRange is called).  For example, dragging a link
    // should not result in a space being added.
    /*
    return containsPlainText()
        && containsURL();
    */
    return false;
}

bool DragData::containsCompatibleContent(DraggingPurpose) const
{
    return containsPlainText()
        || containsURL()
        || m_platformDragData->containsHTML()
        || containsColor();
}

//XXX: WebCore::DragController::createFragmentFromDragData): Move DragData::asFragment() implementation here.
//XXX: WebCore::DragController::createFragmentFromDragData) moved to editor
// PassRefPtr<DocumentFragment> DragData::asFragment(Frame* frame, Range&, bool, bool&) const
// {
//     /*
//      * Order is richest format first. On OSX this is:
//      * * Web Archive
//      * * Filenames
//      * * HTML
//      * * RTF
//      * * TIFF
//      * * PICT
//      */

//     if (containsFiles()) {
//         // FIXME: Implement this.  Should be pretty simple to make some HTML
//         // and call createFragmentFromMarkup.
//         //if (RefPtr<DocumentFragment> fragment = createFragmentFromMarkup(doc,
//         //    ?, KURL()))
//         //    return fragment;
//     }

//     if (m_platformDragData->containsHTML()) {
//         bool ignoredSuccess;
//         String sBase;
//         String sHtml = m_platformDragData->asHTML(&sBase);
//         RefPtr<DocumentFragment> fragment = createFragmentFromMarkup(
//             *frame->document(),
//             sHtml,
//             sBase,
//             DisallowScriptingContent);
//         return fragment.release();
//     }

//     return 0;
// }
bool DragData::containsColor() const
{
    notImplemented();
    return false;
}

Color DragData::asColor() const
{
    notImplemented();
    return Color();
}

unsigned DragData::numberOfFiles() const
{
    return m_platformDragData->filenames().size();
}

bool DragData::shouldMatchStyleOnDrop() const
{
    return false;
}

} // namespace WebCore
