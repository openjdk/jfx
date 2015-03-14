/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef FrameLoaderClientJava_H
#define FrameLoaderClientJava_H

#include "Document.h"
#include "DocumentLoader.h"
#include "Frame.h"
#include "FrameLoader.h"
#include "FrameLoaderClient.h"
#include "FrameView.h"
#include "FormState.h"
#include "HTMLFrameOwnerElement.h"

#include "ResourceRequest.h"
#include "ResourceResponse.h"
#include "Widget.h"
#include "PluginWidgetJava.h"
#include "ProgressTrackerClient.h"

#include "JavaEnv.h"

namespace WebCore {

    class FrameLoaderClientJava : public FrameLoaderClient, public ProgressTrackerClient {
    public:
        FrameLoaderClientJava(const JLObject &webPage);
        virtual void frameLoaderDestroyed();

        virtual bool hasWebView() const;

        virtual bool privateBrowsingEnabled() const;

        virtual void makeDocumentView();
        virtual void makeRepresentation(DocumentLoader*);
        virtual void setDocumentViewFromCachedPage(CachedPage*);
        virtual void forceLayout();
        virtual void forceLayoutForNonHTML();

        virtual void setCopiesOnScroll();

        virtual void detachedFromParent1();
        virtual void detachedFromParent2();
        virtual void detachedFromParent3();
        virtual void detachedFromParent4();

        virtual void loadedFromCachedPage();

        virtual void assignIdentifierToInitialRequest(unsigned long identifier, DocumentLoader*, const ResourceRequest&);

        virtual void dispatchWillSendRequest(DocumentLoader*, unsigned long  identifier, ResourceRequest&, const ResourceResponse& redirectResponse);
        virtual void dispatchDidReceiveAuthenticationChallenge(DocumentLoader*, unsigned long identifier, const AuthenticationChallenge&);
        virtual void dispatchDidCancelAuthenticationChallenge(DocumentLoader*, unsigned long  identifier, const AuthenticationChallenge&);
        virtual void dispatchDidReceiveResponse(DocumentLoader*, unsigned long  identifier, const ResourceResponse&);
        virtual void dispatchDidReceiveContentLength(DocumentLoader*, unsigned long identifier, int lengthReceived);
        virtual void dispatchDidFinishLoading(DocumentLoader*, unsigned long  identifier);
        virtual void dispatchDidFailLoading(DocumentLoader*, unsigned long  identifier, const ResourceError&);
        virtual bool dispatchDidLoadResourceFromMemoryCache(DocumentLoader*, const ResourceRequest&, const ResourceResponse&, int length);
//        virtual void dispatchDidLoadResourceByXMLHttpRequest(unsigned long identifier, const String&);

        virtual void dispatchDidHandleOnloadEvents();
        virtual void dispatchDidPushStateWithinPage();
        virtual void dispatchDidReplaceStateWithinPage();
        virtual void dispatchDidPopStateWithinPage();
        virtual void dispatchDidChangeBackForwardIndex() const;
        virtual void dispatchDidReceiveServerRedirectForProvisionalLoad();
        virtual void dispatchDidCancelClientRedirect();
        virtual void dispatchWillPerformClientRedirect(const URL&, double, double);
        virtual void dispatchDidChangeLocationWithinPage();
        virtual void dispatchWillClose();
        virtual void dispatchDidReceiveIcon();
        virtual void dispatchDidStartProvisionalLoad();
        virtual void dispatchDidReceiveTitle(const StringWithDirection&);
        virtual void dispatchDidChangeIcons(IconType);
        virtual void dispatchDidCommitLoad();
        virtual void dispatchDidFailProvisionalLoad(const ResourceError&);
        virtual void dispatchDidFailLoad(const ResourceError&);
        virtual void dispatchDidFinishDocumentLoad();
        virtual void dispatchDidFinishLoad();
        virtual void dispatchDidFirstLayout();
        virtual void dispatchDidFirstVisuallyNonEmptyLayout();
        virtual void dispatchDidClearWindowObjectInWorld(WebCore::DOMWrapperWorld&);

        virtual Frame* dispatchCreatePage(const NavigationAction&);
        virtual void dispatchShow();

        virtual void dispatchDecidePolicyForResponse(const ResourceResponse&, const ResourceRequest&, FramePolicyFunction);
        virtual void dispatchDecidePolicyForNewWindowAction(const NavigationAction&, const ResourceRequest&, PassRefPtr<FormState>, const String&, FramePolicyFunction);
        virtual void dispatchDecidePolicyForNavigationAction(const NavigationAction&, const ResourceRequest&, PassRefPtr<FormState>, FramePolicyFunction);
        virtual void cancelPolicyCheck();

        virtual void dispatchUnableToImplementPolicy(const ResourceError&);

        virtual void dispatchWillSendSubmitEvent(PassRefPtr<FormState>) {};
        virtual void dispatchWillSubmitForm(PassRefPtr<FormState>, FramePolicyFunction);

        virtual void dispatchDidLoadMainResource(DocumentLoader*);

        virtual void dispatchDidChangeIcons() {}

        virtual void revertToProvisionalState(DocumentLoader*);
        virtual void setMainDocumentError(DocumentLoader*, const ResourceError&);
        virtual void clearUnarchivingState(DocumentLoader*);

        // ProgressTrackerClient methods
        virtual void progressStarted(Frame& originatingProgressFrame);
        virtual void progressEstimateChanged(Frame& originatingProgressFrame);
        virtual void progressFinished(Frame& originatingProgressFrame);
        virtual void progressTrackerDestroyed();

        virtual PassRefPtr<Frame> createFrame(const URL& url, const String& name, HTMLFrameOwnerElement* ownerElement,
                                   const String& referrer, bool allowsScrolling, int marginWidth, int marginHeight);
        virtual PassRefPtr<Widget> createPlugin(const IntSize&, HTMLPlugInElement*, const URL&, const Vector<String>&, const Vector<String>&, const String&, bool loadManually);
        virtual void recreatePlugin(Widget*) { }
        virtual void redirectDataToPlugin(Widget* pluginWidget);
        virtual PassRefPtr<Widget> createJavaAppletWidget(const IntSize&, HTMLAppletElement*, const URL& baseURL, const Vector<String>& paramNames, const Vector<String>& paramValues);
        virtual String overrideMediaType() const;

        virtual ObjectContentType objectContentType(const URL&, const String& mimeType, bool shouldPreferPlugInsForImages);

        virtual void setMainFrameDocumentReady(bool);

        virtual void download(ResourceHandle*, const ResourceRequest&, const ResourceResponse&);
        virtual void startDownload(const ResourceRequest&, const String& suggestedName = String());

        virtual void willChangeTitle(DocumentLoader*);
        virtual void didChangeTitle(DocumentLoader*);

        virtual void committedLoad(DocumentLoader*, const char*, int);
        virtual void finishedLoading(DocumentLoader*);
        virtual void finalSetupForReplace(DocumentLoader*);

        virtual void updateGlobalHistory();
        virtual void updateGlobalHistoryRedirectLinks();

        virtual bool shouldGoToHistoryItem(HistoryItem*) const;
        virtual bool shouldStopLoadingForHistoryItem(HistoryItem*) const;
        virtual void dispatchDidAddBackForwardItem(HistoryItem*) const;
        virtual void dispatchDidRemoveBackForwardItem(HistoryItem*) const;


        // This frame has displayed inactive content (such as an image) from an
        // insecure source.  Inactive content cannot spread to other frames.
        virtual void didDisplayInsecureContent();

        // The indicated security origin has run active content (such as a
        // script) from an insecure source.  Note that the insecure content can
        // spread to other frames in the same origin.
        virtual void didRunInsecureContent(SecurityOrigin*, const URL&);
        virtual void didDetectXSS(const URL&, bool);

        virtual ResourceError cancelledError(const ResourceRequest&);
        virtual ResourceError blockedError(const ResourceRequest&);
        virtual ResourceError cannotShowURLError(const ResourceRequest&);
        virtual ResourceError interruptedForPolicyChangeError(const ResourceRequest&);

        virtual ResourceError cannotShowMIMETypeError(const ResourceResponse&);
        virtual ResourceError fileDoesNotExistError(const ResourceResponse&);
        virtual ResourceError pluginWillHandleLoadError(const ResourceResponse&);

        virtual bool shouldFallBack(const ResourceError&);

        virtual bool shouldUseCredentialStorage(DocumentLoader*, unsigned long identifier);

        virtual bool isArchiveLoadPending(ResourceLoader*) const;
        virtual void cancelPendingArchiveLoad(ResourceLoader*);
        virtual void clearArchivedResources();

        virtual bool canHandleRequest(const ResourceRequest&) const;
        virtual bool canShowMIMEType(const String&) const;
        virtual bool canShowMIMETypeAsHTML(const String& MIMEType) const;
        virtual bool representationExistsForURLScheme(const String&) const;
        virtual String generatedMIMETypeForURLScheme(const String&) const;

        virtual void frameLoadCompleted();
        virtual void saveViewStateToItem(HistoryItem*);
        virtual void restoreViewState();
        virtual void provisionalLoadStarted();
        virtual void didFinishLoad();
        virtual void prepareForDataSourceReplacement();

        virtual PassRefPtr<DocumentLoader> createDocumentLoader(const ResourceRequest&, const SubstituteData&);
        virtual void setTitle(const StringWithDirection& title, const URL&);

        virtual String userAgent(const URL&);

        virtual void savePlatformDataToCachedFrame(CachedFrame*);
        virtual void transitionToCommittedFromCachedFrame(CachedFrame*);
        virtual void transitionToCommittedForNewPage();

        virtual bool canCachePage() const;
        virtual void convertMainResourceLoadToDownload(DocumentLoader*, const ResourceRequest&, const ResourceResponse&);

        virtual void didSaveToPageCache();
        virtual void didRestoreFromPageCache();

        virtual void dispatchDidBecomeFrameset(bool); // Can change due to navigation or DOM modification.

        virtual void didTransferChildFrameToNewDocument(Page* oldPage);
        virtual void transferLoadingResourceFromPage(ResourceLoader*, const ResourceRequest&, Page*);
        virtual PassRefPtr<FrameNetworkingContext> createNetworkingContext();

        virtual void documentElementAvailable();
        // "Navigation" here means a transition from one page to another that ends up in the back/forward list.
        virtual void didPerformFirstNavigation() const;

        virtual void registerForIconNotification(bool listen = true);

        void setFrame(Frame* frame);
    private:
        Page* m_page;
        Frame* m_frame;
        ResourceResponse m_response;
        int mainResourceRequestID;
        bool m_isPageRedirected;
        bool m_hasRepresentation;
        bool m_FrameLoaderClientDestroyed;
        bool m_ProgressTrackerClientDestroyed;

        JGObject m_webPage;

        Page* page();
        Frame* frame();

        void setRequestURL(Frame* f, int identifier, String url);
        void removeRequestURL(Frame* f, int identifier);

        void postLoadEvent(Frame* f, int state, String url, String contentType, double progress, int errorCode = 0);
        void postResourceLoadEvent(Frame* f, int state, int id, String contentType, double progress, int errorCode = 0);

        void destroyIfNeeded();

        // Plugin widget for handling data redirection
//        PluginWidgetJava* m_pluginWidget;
    };

}

#endif
