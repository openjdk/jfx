/*
 * Copyright (C) 2010 Google, Inc. All Rights Reserved.
 * Copyright (C) 2013 Apple, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include "HTMLParserScheduler.h"

#include "Document.h"
#include "FrameView.h"
#include "HTMLDocumentParser.h"
#include "Page.h"

// defaultParserTimeLimit is the seconds the parser will run in one write() call
// before yielding. Inline <script> execution can cause it to exceed the limit.
// FIXME: We would like this value to be 0.2.
static const double defaultParserTimeLimit = 0.500;

namespace WebCore {

static double parserTimeLimit(Page* page)
{
    // We're using the poorly named customHTMLTokenizerTimeDelay setting.
    if (page && page->hasCustomHTMLTokenizerTimeDelay())
        return page->customHTMLTokenizerTimeDelay();
    return defaultParserTimeLimit;
}

ActiveParserSession::ActiveParserSession(Document* document)
    : m_document(document)
{
    if (!m_document)
        return;
    m_document->incrementActiveParserCount();
}

ActiveParserSession::~ActiveParserSession()
{
    if (!m_document)
        return;
    m_document->decrementActiveParserCount();
}

PumpSession::PumpSession(unsigned& nestingLevel, Document* document)
    : NestingLevelIncrementer(nestingLevel)
    , ActiveParserSession(document)
    // Setting processedTokens to INT_MAX causes us to check for yields
    // after any token during any parse where yielding is allowed.
    // At that time we'll initialize startTime.
    , processedTokens(INT_MAX)
    , didSeeScript(false)
{
}

PumpSession::~PumpSession() = default;

HTMLParserScheduler::HTMLParserScheduler(HTMLDocumentParser& parser)
    : m_parser(parser)
    , m_parserTimeLimit(Seconds(parserTimeLimit(m_parser.document()->page())))
    , m_continueNextChunkTimer(*this, &HTMLParserScheduler::continueNextChunkTimerFired)
    , m_isSuspendedWithActiveTimer(false)
#if ASSERT_ENABLED
    , m_suspended(false)
#endif
{
}

HTMLParserScheduler::~HTMLParserScheduler()
{
    m_continueNextChunkTimer.stop();
}

void HTMLParserScheduler::continueNextChunkTimerFired()
{
    ASSERT(!m_suspended);

    // FIXME: The timer class should handle timer priorities instead of this code.
    // If a layout is scheduled, wait again to let the layout timer run first.
    if (m_parser.document()->isLayoutTimerActive()) {
        m_continueNextChunkTimer.startOneShot(0_s);
        return;
    }
    m_parser.resumeParsingAfterYield();
}

bool HTMLParserScheduler::shouldYieldBeforeExecutingScript(PumpSession& session)
{
    // If we've never painted before and a layout is pending, yield prior to running
    // scripts to give the page a chance to paint earlier.
    RefPtr<Document> document = m_parser.document();
    bool needsFirstPaint = document->view() && !document->view()->hasEverPainted();
    session.didSeeScript = true;

    if (UNLIKELY(m_documentHasActiveParserYieldTokens))
        return true;

    return needsFirstPaint && document->isLayoutTimerActive();
}

void HTMLParserScheduler::scheduleForResume()
{
    ASSERT(!m_suspended);
    m_continueNextChunkTimer.startOneShot(0_s);
}

void HTMLParserScheduler::suspend()
{
    ASSERT(!m_suspended);
    ASSERT(!m_isSuspendedWithActiveTimer);
#if ASSERT_ENABLED
    m_suspended = true;
#endif

    if (!m_continueNextChunkTimer.isActive())
        return;
    m_isSuspendedWithActiveTimer = true;
    m_continueNextChunkTimer.stop();
}

void HTMLParserScheduler::resume()
{
    ASSERT(m_suspended);
    ASSERT(!m_continueNextChunkTimer.isActive());
#if ASSERT_ENABLED
    m_suspended = false;
#endif

    if (!m_isSuspendedWithActiveTimer)
        return;
    m_isSuspendedWithActiveTimer = false;
    m_continueNextChunkTimer.startOneShot(0_s);
}

}
