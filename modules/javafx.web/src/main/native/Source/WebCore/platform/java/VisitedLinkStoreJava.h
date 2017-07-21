/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */


#pragma once

#include "LinkHash.h"
#include "VisitedLinkStore.h"

class VisitedLinkStoreJava final : public WebCore::VisitedLinkStore {
public:
    static Ref<VisitedLinkStoreJava> create();
    ~VisitedLinkStoreJava() override;

    static void setShouldTrackVisitedLinks(bool);
    static void removeAllVisitedLinks();

    void addVisitedLink(const String& urlString);

private:
    VisitedLinkStoreJava();

    bool isLinkVisited(WebCore::Page&, WebCore::LinkHash, const WebCore::URL& baseURL, const AtomicString& attributeURL) override;
    void addVisitedLink(WebCore::Page&, WebCore::LinkHash) override;

    void populateVisitedLinksIfNeeded(WebCore::Page&);
    void addVisitedLinkHash(WebCore::LinkHash);
    void removeVisitedLinkHashes();

    HashSet<WebCore::LinkHash, WebCore::LinkHashHash> m_visitedLinkHashes;
    bool m_visitedLinksPopulated;
};

