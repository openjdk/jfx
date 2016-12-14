/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */

#ifndef VisitedLinkStoreJava_h
#define VisitedLinkStoreJava_h

#include <WebCore/LinkHash.h>
#include <WebCore/VisitedLinkStore.h>
#include <wtf/PassRef.h>

class VisitedLinkStoreJava final : public WebCore::VisitedLinkStore {
public:
    static Ref<VisitedLinkStoreJava> create();
    virtual ~VisitedLinkStoreJava();

    static void setShouldTrackVisitedLinks(bool);
    static void removeAllVisitedLinks();

    void addVisitedLink(const String& urlString);

private:
    VisitedLinkStoreJava();

    virtual bool isLinkVisited(WebCore::Page&, WebCore::LinkHash, const WebCore::URL& baseURL, const AtomicString& attributeURL) override;
    virtual void addVisitedLink(WebCore::Page&, WebCore::LinkHash) override;

    void populateVisitedLinksIfNeeded(WebCore::Page&);
    void addVisitedLinkHash(WebCore::LinkHash);
    void removeVisitedLinkHashes();

    HashSet<WebCore::LinkHash, WebCore::LinkHashHash> m_visitedLinkHashes;
    bool m_visitedLinksPopulated;
};

#endif // VisitedLinkStoreJava_h
