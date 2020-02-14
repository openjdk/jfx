/*
 * Copyright (C) 2009-2017 Apple Inc. All rights reserved.
 * Copyright (C) 2011 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1.  Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 3.  Neither the name of Apple Inc. ("Apple") nor the names of
 *     its contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE AND ITS CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "EventTarget.h"
#include "InspectorWebAgentBase.h"
#include "Timer.h"
#include <JavaScriptCore/InspectorBackendDispatchers.h>
#include <JavaScriptCore/InspectorFrontendDispatchers.h>
#include <wtf/HashMap.h>
#include <wtf/HashSet.h>
#include <wtf/JSONValues.h>
#include <wtf/RefPtr.h>
#include <wtf/Vector.h>
#include <wtf/text/AtomString.h>

namespace Inspector {
class InjectedScriptManager;
}

namespace JSC {
class ExecState;
class JSValue;
}

namespace WebCore {

class AccessibilityObject;
class CharacterData;
class DOMEditor;
class Document;
class Element;
class Event;
class Exception;
class FloatQuad;
class Frame;
class InspectorHistory;
class InspectorOverlay;
#if ENABLE(VIDEO)
class HTMLMediaElement;
#endif
class HitTestResult;
class Node;
class Page;
class PseudoElement;
class RevalidateStyleAttributeTask;
class ShadowRoot;

struct HighlightConfig;

typedef String ErrorString;

class InspectorDOMAgent final : public InspectorAgentBase, public Inspector::DOMBackendDispatcherHandler {
    WTF_MAKE_NONCOPYABLE(InspectorDOMAgent);
    WTF_MAKE_FAST_ALLOCATED;
public:
    InspectorDOMAgent(PageAgentContext&, InspectorOverlay*);
    virtual ~InspectorDOMAgent();

    static String toErrorString(ExceptionCode);
    static String toErrorString(Exception&&);

    static String documentURLString(Document*);

    // We represent embedded doms as a part of the same hierarchy. Hence we treat children of frame owners differently.
    // We also skip whitespace text nodes conditionally. Following methods encapsulate these specifics.
    static Node* innerFirstChild(Node*);
    static Node* innerNextSibling(Node*);
    static Node* innerPreviousSibling(Node*);
    static unsigned innerChildNodeCount(Node*);
    static Node* innerParentNode(Node*);

    static Node* scriptValueAsNode(JSC::JSValue);
    static JSC::JSValue nodeAsScriptValue(JSC::ExecState&, Node*);

    // InspectorAgentBase
    void didCreateFrontendAndBackend(Inspector::FrontendRouter*, Inspector::BackendDispatcher*);
    void willDestroyFrontendAndBackend(Inspector::DisconnectReason);

    // DOMBackendDispatcherHandler
    void querySelector(ErrorString&, int nodeId, const String& selectors, int* elementId);
    void querySelectorAll(ErrorString&, int nodeId, const String& selectors, RefPtr<JSON::ArrayOf<int>>& result);
    void getDocument(ErrorString&, RefPtr<Inspector::Protocol::DOM::Node>& root);
    void requestChildNodes(ErrorString&, int nodeId, const int* depth);
    void setAttributeValue(ErrorString&, int elementId, const String& name, const String& value);
    void setAttributesAsText(ErrorString&, int elementId, const String& text, const String* name);
    void removeAttribute(ErrorString&, int elementId, const String& name);
    void removeNode(ErrorString&, int nodeId);
    void setNodeName(ErrorString&, int nodeId, const String& name, int* newId);
    void getOuterHTML(ErrorString&, int nodeId, WTF::String* outerHTML);
    void setOuterHTML(ErrorString&, int nodeId, const String& outerHTML);
    void insertAdjacentHTML(ErrorString&, int nodeId, const String& position, const String& html);
    void setNodeValue(ErrorString&, int nodeId, const String& value);
    void getSupportedEventNames(ErrorString&, RefPtr<JSON::ArrayOf<String>>& eventNames);
    void getDataBindingsForNode(ErrorString&, int nodeId, RefPtr<JSON::ArrayOf<Inspector::Protocol::DOM::DataBinding>>& dataArray);
    void getAssociatedDataForNode(ErrorString&, int nodeId, Optional<String>& associatedData);
    void getEventListenersForNode(ErrorString&, int nodeId, RefPtr<JSON::ArrayOf<Inspector::Protocol::DOM::EventListener>>& listenersArray);
    void setEventListenerDisabled(ErrorString&, int eventListenerId, bool disabled);
    void setBreakpointForEventListener(ErrorString&, int eventListenerId);
    void removeBreakpointForEventListener(ErrorString&, int eventListenerId);
    void getAccessibilityPropertiesForNode(ErrorString&, int nodeId, RefPtr<Inspector::Protocol::DOM::AccessibilityProperties>& axProperties);
    void performSearch(ErrorString&, const String& query, const JSON::Array* nodeIds, const bool* caseSensitive, String* searchId, int* resultCount);
    void getSearchResults(ErrorString&, const String& searchId, int fromIndex, int toIndex, RefPtr<JSON::ArrayOf<int>>&);
    void discardSearchResults(ErrorString&, const String& searchId);
    void resolveNode(ErrorString&, int nodeId, const String* objectGroup, RefPtr<Inspector::Protocol::Runtime::RemoteObject>& result);
    void getAttributes(ErrorString&, int nodeId, RefPtr<JSON::ArrayOf<String>>& result);
    void setInspectModeEnabled(ErrorString&, bool enabled, const JSON::Object* highlightConfig, const bool* showRulers);
    void requestNode(ErrorString&, const String& objectId, int* nodeId);
    void pushNodeByPathToFrontend(ErrorString&, const String& path, int* nodeId);
    void hideHighlight(ErrorString&);
    void highlightRect(ErrorString&, int x, int y, int width, int height, const JSON::Object* color, const JSON::Object* outlineColor, const bool* usePageCoordinates);
    void highlightQuad(ErrorString&, const JSON::Array& quad, const JSON::Object* color, const JSON::Object* outlineColor, const bool* usePageCoordinates);
    void highlightSelector(ErrorString&, const JSON::Object& highlightConfig, const String& selectorString, const String* frameId);
    void highlightNode(ErrorString&, const JSON::Object& highlightConfig, const int* nodeId, const String* objectId);
    void highlightNodeList(ErrorString&, const JSON::Array& nodeIds, const JSON::Object& highlightConfig);
    void highlightFrame(ErrorString&, const String& frameId, const JSON::Object* color, const JSON::Object* outlineColor);
    void moveTo(ErrorString&, int nodeId, int targetNodeId, const int* anchorNodeId, int* newNodeId);
    void undo(ErrorString&);
    void redo(ErrorString&);
    void markUndoableState(ErrorString&);
    void focus(ErrorString&, int nodeId);
    void setInspectedNode(ErrorString&, int nodeId);

    // InspectorInstrumentation
    int identifierForNode(Node&);
    void addEventListenersToNode(Node&);
    void didInsertDOMNode(Node&);
    void didRemoveDOMNode(Node&);
    void willModifyDOMAttr(Element&, const AtomString& oldValue, const AtomString& newValue);
    void didModifyDOMAttr(Element&, const AtomString& name, const AtomString& value);
    void didRemoveDOMAttr(Element&, const AtomString& name);
    void characterDataModified(CharacterData&);
    void didInvalidateStyleAttr(Element&);
    void didPushShadowRoot(Element& host, ShadowRoot&);
    void willPopShadowRoot(Element& host, ShadowRoot&);
    void didChangeCustomElementState(Element&);
    bool handleTouchEvent(Node&);
    void didCommitLoad(Document*);
    void frameDocumentUpdated(Frame&);
    void pseudoElementCreated(PseudoElement&);
    void pseudoElementDestroyed(PseudoElement&);
    void didAddEventListener(EventTarget&);
    void willRemoveEventListener(EventTarget&, const AtomString& eventType, EventListener&, bool capture);
    bool isEventListenerDisabled(EventTarget&, const AtomString& eventType, EventListener&, bool capture);
    void eventDidResetAfterDispatch(const Event&);

    // Callbacks that don't directly correspond to an instrumentation entry point.
    void setDocument(Document*);
    void releaseDanglingNodes();

    void styleAttributeInvalidated(const Vector<Element*>& elements);

    int pushNodeToFrontend(ErrorString&, int documentNodeId, Node*);
    Node* nodeForId(int nodeId);
    int boundNodeId(const Node*);

    RefPtr<Inspector::Protocol::Runtime::RemoteObject> resolveNode(Node*, const String& objectGroup);
    bool handleMousePress();
    void mouseDidMoveOverElement(const HitTestResult&, unsigned modifierFlags);
    void inspect(Node*);
    void focusNode();

    InspectorHistory* history() { return m_history.get(); }
    Vector<Document*> documents();
    void reset();

    Node* assertNode(ErrorString&, int nodeId);
    Element* assertElement(ErrorString&, int nodeId);
    Document* assertDocument(ErrorString&, int nodeId);

    bool hasBreakpointForEventListener(EventTarget&, const AtomString& eventType, EventListener&, bool capture);
    int idForEventListener(EventTarget&, const AtomString& eventType, EventListener&, bool capture);

private:
#if ENABLE(VIDEO)
    void mediaMetricsTimerFired();
#endif

    void highlightMousedOverNode();
    void setSearchingForNode(ErrorString&, bool enabled, const JSON::Object* highlightConfig, bool showRulers);
    std::unique_ptr<HighlightConfig> highlightConfigFromInspectorObject(ErrorString&, const JSON::Object* highlightInspectorObject);

    // Node-related methods.
    typedef HashMap<RefPtr<Node>, int> NodeToIdMap;
    int bind(Node*, NodeToIdMap*);
    void unbind(Node*, NodeToIdMap*);

    Node* assertEditableNode(ErrorString&, int nodeId);
    Element* assertEditableElement(ErrorString&, int nodeId);

    int pushNodePathToFrontend(Node*);
    void pushChildNodesToFrontend(int nodeId, int depth = 1);

    Ref<Inspector::Protocol::DOM::Node> buildObjectForNode(Node*, int depth, NodeToIdMap*);
    Ref<JSON::ArrayOf<String>> buildArrayForElementAttributes(Element*);
    Ref<JSON::ArrayOf<Inspector::Protocol::DOM::Node>> buildArrayForContainerChildren(Node* container, int depth, NodeToIdMap* nodesMap);
    RefPtr<JSON::ArrayOf<Inspector::Protocol::DOM::Node>> buildArrayForPseudoElements(const Element&, NodeToIdMap* nodesMap);
    Ref<Inspector::Protocol::DOM::EventListener> buildObjectForEventListener(const RegisteredEventListener&, int identifier, EventTarget&, const AtomString& eventType, bool disabled, bool hasBreakpoint);
    RefPtr<Inspector::Protocol::DOM::AccessibilityProperties> buildObjectForAccessibilityProperties(Node*);
    void processAccessibilityChildren(AccessibilityObject&, JSON::ArrayOf<int>&);

    Node* nodeForPath(const String& path);
    Node* nodeForObjectId(const String& objectId);

    void discardBindings();

    void innerHighlightQuad(std::unique_ptr<FloatQuad>, const JSON::Object* color, const JSON::Object* outlineColor, const bool* usePageCoordinates);

    Inspector::InjectedScriptManager& m_injectedScriptManager;
    std::unique_ptr<Inspector::DOMFrontendDispatcher> m_frontendDispatcher;
    RefPtr<Inspector::DOMBackendDispatcher> m_backendDispatcher;
    Page& m_inspectedPage;
    InspectorOverlay* m_overlay { nullptr };
    NodeToIdMap m_documentNodeToIdMap;
    // Owns node mappings for dangling nodes.
    Vector<std::unique_ptr<NodeToIdMap>> m_danglingNodeToIdMaps;
    HashMap<int, Node*> m_idToNode;
    HashMap<int, NodeToIdMap*> m_idToNodesMap;
    HashSet<int> m_childrenRequested;
    int m_lastNodeId { 1 };
    RefPtr<Document> m_document;
    typedef HashMap<String, Vector<RefPtr<Node>>> SearchResults;
    SearchResults m_searchResults;
    std::unique_ptr<RevalidateStyleAttributeTask> m_revalidateStyleAttrTask;
    RefPtr<Node> m_nodeToFocus;
    RefPtr<Node> m_mousedOverNode;
    RefPtr<Node> m_inspectedNode;
    std::unique_ptr<HighlightConfig> m_inspectModeHighlightConfig;
    std::unique_ptr<InspectorHistory> m_history;
    std::unique_ptr<DOMEditor> m_domEditor;
    bool m_searchingForNode { false };
    bool m_suppressAttributeModifiedEvent { false };
    bool m_suppressEventListenerChangedEvent { false };
    bool m_documentRequested { false };

#if ENABLE(VIDEO)
    Timer m_mediaMetricsTimer;
    struct MediaMetrics {
        unsigned displayCompositedFrames { 0 };
        bool isPowerEfficient { false };

        MediaMetrics() { }

        MediaMetrics(unsigned displayCompositedFrames)
            : displayCompositedFrames(displayCompositedFrames)
        {
        }
    };

    // The pointer key for this map should not be used for anything other than matching.
    HashMap<HTMLMediaElement*, MediaMetrics> m_mediaMetrics;
#endif

    struct InspectorEventListener {
        int identifier { 1 };
        RefPtr<EventTarget> eventTarget;
        RefPtr<EventListener> eventListener;
        AtomString eventType;
        bool useCapture { false };
        bool disabled { false };
        bool hasBreakpoint { false };

        InspectorEventListener() { }

        InspectorEventListener(int identifier, EventTarget& target, const AtomString& type, EventListener& listener, bool capture)
            : identifier(identifier)
            , eventTarget(&target)
            , eventListener(&listener)
            , eventType(type)
            , useCapture(capture)
        {
        }

        bool matches(EventTarget& target, const AtomString& type, EventListener& listener, bool capture)
        {
            if (eventTarget.get() != &target)
                return false;
            if (eventListener.get() != &listener)
                return false;
            if (eventType != type)
                return false;
            if (useCapture != capture)
                return false;
            return true;
        }
    };

    friend class EventFiredCallback;

    HashSet<const Event*> m_dispatchedEvents;
    HashMap<int, InspectorEventListener> m_eventListenerEntries;
    int m_lastEventListenerId { 1 };
};

} // namespace WebCore
