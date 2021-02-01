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

#pragma once

#include <WebCore/Document.h>
#include <WebCore/DocumentLoader.h>
#include <WebCore/FormState.h>
#include <WebCore/Frame.h>
#include <WebCore/FrameLoader.h>
#include <WebCore/FrameLoaderClient.h>
#include <WebCore/FrameView.h>
#include <WebCore/HTMLFrameOwnerElement.h>
#include <WebCore/PlatformJavaClasses.h>
#include <WebCore/ProgressTrackerClient.h>
#include <WebCore/ResourceRequest.h>
#include <WebCore/ResourceResponse.h>
#include <WebCore/Widget.h>

namespace WebCore {

class FrameLoaderClientJava final : public FrameLoaderClient {
public:
    FrameLoaderClientJava(const JLObject &webPage);
    ~FrameLoaderClientJava();

    bool hasWebView() const override;

    void makeRepresentation(DocumentLoader*) override;
    void forceLayoutForNonHTML() override;

    Optional<PageIdentifier> pageID() const final;
    Optional<FrameIdentifier> frameID() const final;

    void setCopiesOnScroll() override;

    void detachedFromParent2() override;
    void detachedFromParent3() override;

    void assignIdentifierToInitialRequest(unsigned long identifier, DocumentLoader*, const ResourceRequest&) override;

    void dispatchWillSendRequest(DocumentLoader*, unsigned long  identifier, ResourceRequest&, const ResourceResponse& redirectResponse) override;
    void dispatchDidReceiveResponse(DocumentLoader*, unsigned long  identifier, const ResourceResponse&) override;
    void dispatchDidReceiveContentLength(DocumentLoader*, unsigned long identifier, int lengthReceived) override;
    void dispatchDidFinishLoading(DocumentLoader*, unsigned long  identifier) override;
    void dispatchDidFailLoading(DocumentLoader*, unsigned long  identifier, const ResourceError&) override;
    bool dispatchDidLoadResourceFromMemoryCache(DocumentLoader*, const ResourceRequest&, const ResourceResponse&, int length) override;

    void dispatchDidDispatchOnloadEvents() override;
    void dispatchDidPushStateWithinPage() override;
    void dispatchDidReplaceStateWithinPage() override;
    void dispatchDidPopStateWithinPage() override;
    void dispatchDidReceiveServerRedirectForProvisionalLoad() override;
    void dispatchDidCancelClientRedirect() override;
    void dispatchWillPerformClientRedirect(const URL&, double, WallTime, LockBackForwardList) override;
    void dispatchDidNavigateWithinPage() override;
    void dispatchDidChangeLocationWithinPage() override;
    void dispatchWillClose() override;
    void dispatchDidReceiveIcon() override;
    void dispatchDidStartProvisionalLoad() override;
    void dispatchDidReceiveTitle(const StringWithDirection&) override;
    void dispatchDidCommitLoad(Optional<HasInsecureContent>, Optional<WebCore::UsedLegacyTLS>) override;
    void dispatchDidFailProvisionalLoad(const ResourceError&, WillContinueLoading) override;
    void dispatchDidFailLoad(const ResourceError&) override;
    void dispatchDidFinishDocumentLoad() override;
    void dispatchDidFinishLoad() override;
    void dispatchDidClearWindowObjectInWorld(WebCore::DOMWrapperWorld&) override;

    Frame* dispatchCreatePage(const NavigationAction&) override;
    void dispatchShow() override;

    void dispatchDecidePolicyForResponse(const ResourceResponse&, const ResourceRequest&, PolicyCheckIdentifier, const String& downloadAttribute, FramePolicyFunction&&) override;
    void dispatchDecidePolicyForNewWindowAction(const NavigationAction&, const ResourceRequest&, FormState*, const String& frameName, PolicyCheckIdentifier, FramePolicyFunction&&) override;
    void dispatchDecidePolicyForNavigationAction(const NavigationAction&, const ResourceRequest&, const ResourceResponse& redirectResponse, FormState*, PolicyDecisionMode, PolicyCheckIdentifier, FramePolicyFunction&&) override;
    void cancelPolicyCheck() override;

    void dispatchUnableToImplementPolicy(const ResourceError&) override;

    void dispatchWillSendSubmitEvent(Ref<FormState>&&) override {}
    void dispatchWillSubmitForm(FormState&, CompletionHandler<void()>&&) override;

    void dispatchDidLoadMainResource(DocumentLoader*);

    void revertToProvisionalState(DocumentLoader*) override;
    void setMainDocumentError(DocumentLoader*, const ResourceError&) override;

    RefPtr<Frame> createFrame(const String& name, HTMLFrameOwnerElement& ownerElement) override;
    ObjectContentType objectContentType(const URL& url, const String& mimeTypeIn) override;
    RefPtr<Widget> createPlugin(const IntSize&, HTMLPlugInElement&, const URL&, const Vector<String>&, const Vector<String>&, const String&, bool loadManually) override;
    void redirectDataToPlugin(Widget&) override;
    RefPtr<Widget> createJavaAppletWidget(const IntSize&, HTMLAppletElement&, const URL& baseURL, const Vector<String>& paramNames, const Vector<String>& paramValues) override;
    String overrideMediaType() const override;

    void setMainFrameDocumentReady(bool) override;

    void startDownload(const ResourceRequest&, const String& suggestedName = String()) override;

    void willChangeTitle(DocumentLoader*) override;
    void didChangeTitle(DocumentLoader*) override;

    void committedLoad(DocumentLoader*, const char*, int) override;
    void finishedLoading(DocumentLoader*) override;

    void updateGlobalHistory() override;
    void updateGlobalHistoryRedirectLinks() override;

    bool shouldGoToHistoryItem(HistoryItem&) const override;

    // This frame has displayed inactive content (such as an image) from an
    // insecure source.  Inactive content cannot spread to other frames.
    void didDisplayInsecureContent() override;

    // The indicated security origin has run active content (such as a
    // script) from an insecure source.  Note that the insecure content can
    // spread to other frames in the same origin.
    void didRunInsecureContent(SecurityOrigin&, const URL&) override;
    void didDetectXSS(const URL&, bool) override;

    ResourceError cancelledError(const ResourceRequest&) const override;
    ResourceError blockedByContentBlockerError(const ResourceRequest& request) const override;
    ResourceError blockedError(const ResourceRequest&) const override;
    ResourceError cannotShowURLError(const ResourceRequest&) const override;
    ResourceError interruptedForPolicyChangeError(const ResourceRequest&) const override;

    ResourceError cannotShowMIMETypeError(const ResourceResponse&) const override;
    ResourceError fileDoesNotExistError(const ResourceResponse&) const override;
    ResourceError pluginWillHandleLoadError(const ResourceResponse&) const override;

    bool shouldFallBack(const ResourceError&) const override;

    bool shouldUseCredentialStorage(DocumentLoader*, unsigned long identifier) override;
    void dispatchDidReceiveAuthenticationChallenge(DocumentLoader*, unsigned long identifier, const AuthenticationChallenge&) override;

    bool canHandleRequest(const ResourceRequest&) const override;
    bool canShowMIMEType(const String&) const override;
    bool canShowMIMETypeAsHTML(const String& MIMEType) const override;
    bool representationExistsForURLScheme(const String&) const override;
    String generatedMIMETypeForURLScheme(const String&) const override;

    void frameLoadCompleted() override;
    void saveViewStateToItem(HistoryItem&) override;
    void restoreViewState() override;
    void provisionalLoadStarted() override;
    void didFinishLoad() override;
    void prepareForDataSourceReplacement() override;

    Ref<DocumentLoader> createDocumentLoader(const ResourceRequest&, const SubstituteData&) override;
    void setTitle(const StringWithDirection& title, const URL&) override;

    void willReplaceMultipartContent() override;
    void didReplaceMultipartContent() override;
    void updateCachedDocumentLoader(DocumentLoader&) override;

    String userAgent(const URL&) const override;

    void savePlatformDataToCachedFrame(CachedFrame*) override;
    void transitionToCommittedFromCachedFrame(CachedFrame*) override;
    void transitionToCommittedForNewPage() override;

    void didRestoreFromBackForwardCache() override;
    bool canCachePage() const override;
    void convertMainResourceLoadToDownload(DocumentLoader*, const ResourceRequest&, const ResourceResponse&) override;

    Ref<FrameNetworkingContext> createNetworkingContext() override;

    void registerForIconNotification() override;

    void setFrame(Frame* frame);

    bool isJavaFrameLoaderClient() override { return true; }
    void prefetchDNS(const String&) override;
    void sendH2Ping(const URL&, CompletionHandler<void(Expected<Seconds, ResourceError>&&)>&&) override;
private:
    Page* m_page;
    Frame* m_frame;
    ResourceResponse m_response;
    unsigned long m_mainResourceRequestID;
    bool m_isPageRedirected;
    bool m_hasRepresentation;

    JGObject m_webPage;

    Page* page();
    Frame* frame();

    void setRequestURL(Frame* f, int identifier, String url);
    void removeRequestURL(Frame* f, int identifier);

    void postLoadEvent(Frame* f, int state, String url, String contentType, double progress, int errorCode = 0);
    void postResourceLoadEvent(Frame* f, int state, int id, String contentType, double progress, int errorCode = 0);
    // Plugin widget for handling data redirection
//        PluginWidgetJava* m_pluginWidget;
};
} // namespace WebCore
