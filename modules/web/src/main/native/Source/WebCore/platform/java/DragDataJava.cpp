/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "Frame.h"
#include "DragData.h"
#include "Range.h"

#include "DataObjectJava.h"
#include "Clipboard.h"
#include "ClipboardJava.h"
#include "DocumentFragment.h"
#include "URL.h"
#include "markup.h"
#include "NotImplemented.h"

#include <wtf/text/WTFString.h>

namespace WebCore {

/*
PassRefPtr<Clipboard> DragData::createClipboard(ClipboardAccessPolicy policy) const
{
    return ClipboardJava::create(policy, true, m_platformDragData);
}
*/

bool DragData::containsURL(Frame*, FilenameConversionPolicy filenamePolicy /*= ConvertFilenames*/) const
{
    /* utaTODO: extent the functionality
    */
    return m_platformDragData->containsURL();
}

String DragData::asURL(Frame* frame, FilenameConversionPolicy filenamePolicy, String* title) const
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

void DragData::asFilenames(Vector<String>& result) const
{
    return m_platformDragData->asFilenames(result);
}

bool DragData::containsPlainText() const
{
    return m_platformDragData->containsPlainText();
}

String DragData::asPlainText(Frame*) const
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

bool DragData::containsCompatibleContent() const
{
    return containsPlainText()
        || containsURL(0)
        || m_platformDragData->containsHTML()
        || containsColor();
}

PassRefPtr<DocumentFragment> DragData::asFragment(Frame* frame, Range&, bool, bool&) const
{     
    /*
     * Order is richest format first. On OSX this is:
     * * Web Archive
     * * Filenames
     * * HTML
     * * RTF
     * * TIFF
     * * PICT
     */

    if (containsFiles()) {
        // FIXME: Implement this.  Should be pretty simple to make some HTML
        // and call createFragmentFromMarkup.
        //if (RefPtr<DocumentFragment> fragment = createFragmentFromMarkup(doc,
        //    ?, KURL()))
        //    return fragment;
    }

    if (m_platformDragData->containsHTML()) {
        bool ignoredSuccess;
        String sBase;
        String sHtml = m_platformDragData->asHTML(&sBase);
        RefPtr<DocumentFragment> fragment = createFragmentFromMarkup(
            *frame->document(),
            sHtml,
            sBase,
            DisallowScriptingContent);
        return fragment.release();
    }

    return 0;
}
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

} // namespace WebCore
