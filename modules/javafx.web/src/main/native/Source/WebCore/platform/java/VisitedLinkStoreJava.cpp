/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */


#include "config.h"
#include "VisitedLinkStoreJava.h"

#include <WebCore/PageCache.h>
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
    addVisitedLinkHash(visitedLinkHash(urlString));
}

bool VisitedLinkStoreJava::isLinkVisited(Page& page, LinkHash linkHash, const URL& baseURL, const AtomicString& attributeURL)
{
    populateVisitedLinksIfNeeded(page);

    return m_visitedLinkHashes.contains(linkHash);
}

void VisitedLinkStoreJava::addVisitedLink(Page&, LinkHash linkHash)
{
    if (!s_shouldTrackVisitedLinks)
        return;

    addVisitedLinkHash(linkHash);
}

void VisitedLinkStoreJava::populateVisitedLinksIfNeeded(Page& sourcePage)
{
    if (m_visitedLinksPopulated)
        return;

    m_visitedLinksPopulated = true;
}

void VisitedLinkStoreJava::addVisitedLinkHash(LinkHash linkHash)
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

