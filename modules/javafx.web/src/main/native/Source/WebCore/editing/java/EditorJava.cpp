/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "Editor.h"
#include "CachedImage.h"
#include "DataObjectJava.h"
#include "DocumentFragment.h"
#include "Frame.h"
#include "HTMLEmbedElement.h"
#include "HTMLImageElement.h"
#include "HTMLInputElement.h"
#include "HTMLObjectElement.h"
#include "HTMLNames.h"
#include "HTMLParserIdioms.h"
#include "Pasteboard.h"
#include "RenderImage.h"
#include "SVGElement.h"
#include "SVGImageElement.h"
#include "XLinkNames.h"
#include "markup.h"

namespace WebCore {

// FIXME-java: Implemetation task is tracked at JDK-8146460.
void Editor::pasteWithPasteboard(Pasteboard* pasteboard, bool allowPlainText, MailBlockquoteHandling mailBlockquoteHandling)
{
    RefPtr<Range> range = selectedRange();
    if (!range)
        return;

    bool chosePlainText;
    RefPtr<DocumentFragment> fragment = pasteboard->documentFragment(m_frame, *range, allowPlainText, chosePlainText);
    if (fragment && shouldInsertFragment(*fragment, range.get(), EditorInsertAction::Pasted))
        pasteAsFragment(fragment.releaseNonNull(), canSmartReplaceWithPasteboard(*pasteboard), chosePlainText, mailBlockquoteHandling);
}

RefPtr<DocumentFragment> Editor::webContentFromPasteboard(Pasteboard&, Range&, bool /*allowPlainText*/, bool& /*chosePlainText*/)
{
    notImplemented();
    return RefPtr<DocumentFragment>();
}

void Editor::writeImageToPasteboard(Pasteboard&, Element&, const URL&, const String&)
{
    notImplemented();
#if 0
    PasteboardImage pasteboardImage;

    if (!getImageForElement(imageElement, pasteboardImage.image))
        return;
    ASSERT(pasteboardImage.image);

    pasteboardImage.url.url = imageElement.document().completeURL(stripLeadingAndTrailingHTMLSpaces(elementURL(imageElement)));
    pasteboardImage.url.title = title;
    pasteboardImage.url.markup = createMarkup(imageElement, IncludeNode, nullptr, ResolveAllURLs);
    pasteboard.write(pasteboardImage);
#endif
}

void Editor::writeSelectionToPasteboard(Pasteboard& pasteboard)
{
    pasteboard.writeSelection(*selectedRange(), canSmartCopyOrDelete(), m_frame, DefaultSelectedTextType);
}

} // namespace WebCore
