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


#include "VisitedLinkStoreJava.h"

#include <WebCore/BackForwardCache.h>
#include <wtf/NeverDestroyed.h>


using namespace WebCore;

static bool s_shouldTrackVisitedLinks;

static HashSet<VisitedLinkStoreJava*>& visitedLinkStores()
{
    static NeverDestroyed<HashSet<VisitedLinkStoreJava*>> visitedLinkStores;

    return visitedLinkStores;
}

Ref<VisitedLinkStoreJava> VisitedLinkStoreJava::create()
{
    return adoptRef(*new VisitedLinkStoreJava);
}

VisitedLinkStoreJava::VisitedLinkStoreJava()
    : m_visitedLinksPopulated(false)
{
    visitedLinkStores().add(this);
}

VisitedLinkStoreJava::~VisitedLinkStoreJava()
{
    visitedLinkStores().remove(this);
}

void VisitedLinkStoreJava::setShouldTrackVisitedLinks(bool shouldTrackVisitedLinks)
{
    if (s_shouldTrackVisitedLinks == shouldTrackVisitedLinks)
        return;

    s_shouldTrackVisitedLinks = shouldTrackVisitedLinks;
    if (!s_shouldTrackVisitedLinks)
        removeAllVisitedLinks();
}

void VisitedLinkStoreJava::removeAllVisitedLinks()
{
    for (auto& visitedLinkStore : visitedLinkStores())
        visitedLinkStore->removeVisitedLinkHashes();
}

void VisitedLinkStoreJava::addVisitedLink(const String& urlString)
{
    addVisitedLinkHash(computeSharedStringHash(urlString));
}

bool VisitedLinkStoreJava::isLinkVisited(Page& page, SharedStringHash linkHash, const URL&, const AtomString&)
{
    populateVisitedLinksIfNeeded(page);

    return m_visitedLinkHashes.contains(linkHash);
}

void VisitedLinkStoreJava::addVisitedLink(Page&, SharedStringHash linkHash)
{
    if (!s_shouldTrackVisitedLinks)
        return;

    addVisitedLinkHash(linkHash);
}

void VisitedLinkStoreJava::populateVisitedLinksIfNeeded(Page&)
{
    if (m_visitedLinksPopulated)
        return;

    m_visitedLinksPopulated = true;
}

void VisitedLinkStoreJava::addVisitedLinkHash(SharedStringHash linkHash)
{
    ASSERT(s_shouldTrackVisitedLinks);
    m_visitedLinkHashes.add(linkHash);

    invalidateStylesForLink(linkHash);
}

void VisitedLinkStoreJava::removeVisitedLinkHashes()
{
    m_visitedLinksPopulated = false;
    if (m_visitedLinkHashes.isEmpty())
        return;
    m_visitedLinkHashes.clear();

    invalidateStylesForAllLinks();
}


