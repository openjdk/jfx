/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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
    auto range = selectedRange();
    if (!range)
        return;

    bool chosePlainText;
    auto fragment = pasteboard->documentFragment(*m_document->frame(), *range, options.contains(PasteOption::AllowPlainText), chosePlainText);

    if (fragment && options.contains(PasteOption::AsQuotation))
        quoteFragmentForPasting(*fragment);

    if (fragment && shouldInsertFragment(*fragment, *range, EditorInsertAction::Pasted))
        pasteAsFragment(fragment.releaseNonNull(), canSmartReplaceWithPasteboard(*pasteboard), chosePlainText, options.contains(PasteOption::IgnoreMailBlockquote) ? MailBlockquoteHandling::IgnoreBlockquote : MailBlockquoteHandling::RespectBlockquote);
}

RefPtr<DocumentFragment> Editor::webContentFromPasteboard(Pasteboard&, const SimpleRange&, bool /*allowPlainText*/, bool& /*chosePlainText*/)
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
    pasteboard.writeSelection(*selectedRange(), canSmartCopyOrDelete(), *m_document->frame(), DefaultSelectedTextType);
}

void Editor::platformCopyFont()
{
}

void Editor::platformPasteFont()
{
}

} // namespace WebCore
