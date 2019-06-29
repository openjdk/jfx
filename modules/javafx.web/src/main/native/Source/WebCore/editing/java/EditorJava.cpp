/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
void Editor::pasteWithPasteboard(Pasteboard* pasteboard, OptionSet<PasteOption> options)
{
    RefPtr<Range> range = selectedRange();
    if (!range)
        return;

    bool chosePlainText;
    RefPtr<DocumentFragment> fragment = pasteboard->documentFragment(m_frame, *range, options.contains(PasteOption::AllowPlainText), chosePlainText);
    if (fragment && shouldInsertFragment(*fragment, range.get(), EditorInsertAction::Pasted))
        pasteAsFragment(fragment.releaseNonNull(), canSmartReplaceWithPasteboard(*pasteboard), chosePlainText, options.contains(PasteOption::IgnoreMailBlockquote) ? MailBlockquoteHandling::IgnoreBlockquote : MailBlockquoteHandling::RespectBlockquote);
}

RefPtr<DocumentFragment> Editor::webContentFromPasteboard(Pasteboard&, Range&, bool /*allowPlainText*/, bool& /*chosePlainText*/)
{
    notImplemented();
    return RefPtr<DocumentFragment>();
}

void Editor::writeImageToPasteboard(Pasteboard& pasteboard, Element& element, const URL& url, const String& title)
{
    pasteboard.writeImage(element, url, title);
}

void Editor::writeSelectionToPasteboard(Pasteboard& pasteboard)
{
    pasteboard.writeSelection(*selectedRange(), canSmartCopyOrDelete(), m_frame, DefaultSelectedTextType);
}

} // namespace WebCore
