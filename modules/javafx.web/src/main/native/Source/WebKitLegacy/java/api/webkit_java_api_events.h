/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

/*
 * webkit_java_api_events.h - the Source/WebCore/bindings/java slice of the jfxwebkit C ABI.
 *
 * Scope, exactly (FFM-ABI-CONTRACT.md section 12.1, plus section 6.5 of the WTF/WebCore audit):
 *
 *   bindings/java/JavaEventListener.cpp     a Java org.w3c.dom.events.EventListener attached
 *                                           to a WebCore EventTarget
 *   bindings/java/EventListenerManager.*    the map that owns the Java listener object for as
 *                                           long as the C++ listener is registered
 *   bindings/java/WKJFrameDOM.cpp           Frame -> Document / owner Element
 *
 * It carries three things:
 *
 *   1. WKJEventListenerCallbacks: the two upcalls that were cached jmethodIDs on
 *      com.sun.webkit.dom.EventListenerImpl (fwkHandleEvent and dispose).
 *   2. The three wkj_event_listener_ entry points that replace the last three JNIEXPORTs of
 *      the generated DOM tree - including twkCreatePeer, which was the module's only INSTANCE
 *      native and the only place in the DOM surface that pinned a Java object.
 *   3. The two wkj_frame_ accessors that replace Java_com_sun_webkit_WebPage_twkGetDocument
 *      and Java_com_sun_webkit_WebPage_twkGetOwnerElement. Those lived in JavaDOMUtils.cpp,
 *      which is why they are declared here rather than in webkit_java_api_page.h with the rest
 *      of the wkj_frame_ family. Moving the two declarations there later is a pure header move:
 *      the symbol names, the signatures and the implementation file are unaffected by which
 *      header declares them.
 *
 * ------------------------------------------------------------------------------------------
 * INTEGRATION - the include direction, and why the callbacks are not a WKJHost member
 * ------------------------------------------------------------------------------------------
 * webkit_java_api.h includes this header, the way it includes webkit_java_api_platform.h and
 * webkit_java_api_theme.h, at the point where wkj_ref and WKJ_EXPORT already exist. Naming
 * this header directly is a mistake worth catching, hence the #error below.
 *
 * It lives beside the master, in Source/WebKitLegacy/java/api, and not in the directory it
 * serves, because the master is included from Source/WTF (wtf/java/WKJHandle.h) and from
 * Tools/DumpRenderTree, whose include paths carry only WEBKITLEGACY_DIR/java/api
 * (Source/WTF/wtf/PlatformJava.cmake and Tools/DumpRenderTree/java/CMakeLists.txt). A master
 * that included a header out of WEBCORE_DIR/bindings/java would stop those two targets
 * compiling, and would make WTF depend on a WebCore include directory.
 *
 * WKJEventListenerCallbacks is deliberately NOT a member of WKJHost. WKJHost has a fixed shape
 * that the Java side asserts against the library's own exported sizeof, and a new member would
 * be an ABI break for every other slice. This table therefore follows the pattern the page
 * slice established for tables that arrive after WKJHost froze - wkj_bfl_set_callbacks,
 * wkj_install_color_chooser_callbacks, wkj_install_popup_callbacks,
 * wkj_install_network_callbacks - and gets its own install entry point. Java fills it in the
 * same process-lifetime Arena.ofShared() that holds the host table and installs it once,
 * before the first page is created.
 *
 * ------------------------------------------------------------------------------------------
 * CONVENTIONS - all inherited from webkit_java_api.h; only the additions are restated
 * ------------------------------------------------------------------------------------------
 * Java objects    wkj_ref (contract 3). A wkj_ref PARAMETER is borrowed for the duration of
 *                 the call: wkj_event_listener_create retains what it keeps, exactly as the
 *                 JNI code turned the local jobject into a global reference, and the caller
 *                 still owns and must release the id it passed in.
 * Native objects  int64_t peers, converted with wkj_to_ptr / wkj_from_ptr. A peer returned by
 *                 wkj_frame_get_document or wkj_frame_get_owner_element carries ONE reference
 *                 for Java, which the NodeImpl disposer drops - see the note on those two.
 * NULL slots      every callback pointer may be NULL, and the whole table may be NULL until it
 *                 is installed. The library tests both before every call and behaves as the
 *                 default documented on the slot.
 * Upcall failure  a Java exception never propagates, and - this one is load bearing - it is
 *                 also never reported. JavaEventListener::handleEvent ended in
 *                 WTF::CheckAndClearException(env) and ignored the result, so a listener that
 *                 threw was swallowed and dispatch continued to the next listener. The Java
 *                 upcall target must swallow in the same way; propagating would change the
 *                 behaviour of every page with a throwing listener.
 * Threading       handle_event runs wherever WebCore dispatches the event, which is the WebKit
 *                 main thread for user input and for script-driven dispatchEvent. dispose runs
 *                 from ~JavaEventListener, i.e. wherever the last reference to the listener
 *                 goes away, including WebCore teardown. Both stubs therefore belong in an
 *                 Arena.ofShared(); the JNI code they replace was equally unguarded about the
 *                 thread, and its destructor half deliberately did nothing when the JVM was no
 *                 longer reachable.
 */

#ifndef WEBKIT_JAVA_API_EVENTS_H
#define WEBKIT_JAVA_API_EVENTS_H

/*
 * Included by webkit_java_api.h, not the other way round - see INTEGRATION above. Naming this
 * header directly would process the declarations below before wkj_ref and WKJ_EXPORT exist.
 */
#ifndef WEBKIT_JAVA_API_H
#error "include webkit_java_api.h; it includes this header at the right point"
#endif

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ======================================================================================== */
/* Upcalls: com.sun.webkit.dom.EventListenerImpl                                            */
/* ======================================================================================== */

/*
 * The two methods JavaEventListener called through cached jmethodIDs. Both were looked up with
 * env->FindClass("com/sun/webkit/dom/EventListenerImpl") inside a function-local static
 * (JavaEventListener.cpp:60-63 and :77-82), which is the class-loader trap JNI_OnLoad exists
 * to avoid: the class was resolved with whatever loader happened to be on the stack the first
 * time a listener fired, and then cached for the life of the process. With a table installed
 * from Java there is no lookup and no class loader involved at all.
 */
typedef struct WKJEventListenerCallbacks {
    /*
     * EventListenerImpl.fwkHandleEvent(long eventPeer), which was an INSTANCE method invoked
     * on the listener object; `listener` is the id of that object, the one that was handed to
     * wkj_event_listener_create.
     *
     * `event_peer` is a WebCore::Event* that the library has already ref()ed for Java, on the
     * line the JNI code did it (JavaEventListener.cpp:69). Java turns it into an EventImpl
     * with EventImpl.getImpl(peer), whose disposer drops that reference - and on a cache hit
     * getCachedImpl drops it immediately. That +1/-1 pairing is what the leak tests measure,
     * so neither side may add or remove a reference here.
     *
     * A Throwable from the listener is swallowed, not reported - see CONVENTIONS above.
     * Default when NULL: nothing happens, and the library does not ref the event either, so
     * the pairing still holds.
     */
    void (*handle_event)(wkj_ref listener, int64_t event_peer);

    /*
     * EventListenerImpl.dispose(long peer), a private STATIC method: the C++ listener at
     * `listener_peer` has been destroyed, so Java drops it from its peer tables.
     *
     * `listener_peer` is the value wkj_event_listener_create returned. It is not a wkj_ref,
     * and nothing may dereference it: by the time this runs the C++ object is already being
     * destroyed. It is a key and nothing else.
     *
     * The JNI version of this call sat behind WC_GETJAVAENV_CHKRET, so it did nothing at all
     * when the JVM was no longer reachable - during shutdown, for instance. A NULL slot, or a
     * table that was never installed, reproduces that exactly. Default when NULL: no-op.
     */
    void (*dispose)(int64_t listener_peer);
} WKJEventListenerCallbacks;

/*
 * Installs the table above, or detaches it when `callbacks` is NULL. The library keeps the
 * pointer and does not copy the struct, so the memory must outlive the library - the same rule
 * wkj_init states for WKJHost. Call it once, where the host table is installed, before any
 * page exists.
 */
WKJ_EXPORT void wkj_install_event_listener_callbacks(const WKJEventListenerCallbacks* callbacks);

/* ======================================================================================== */
/* Downcalls: com.sun.webkit.dom.EventListenerImpl                                          */
/* ======================================================================================== */

/*
 * EventListenerImpl.twkCreatePeer() - the module's only instance native, whose implicit
 * jobject self becomes an explicit registry id.
 *
 * Creates the WebCore::EventListener that forwards to the Java listener `self`, and returns
 * its peer. Java keeps that peer, passes it to the other two entry points, and receives it
 * back through the dispose callback.
 *
 * OWNERSHIP: `self` is borrowed, and the library retains it for as long as the listener is
 * registered. This is the one structure in the DOM surface that pins a Java object, and it
 * pins it strongly, exactly as the NewGlobalRef in EventListenerManager did. The retained id
 * is released when EventListenerManager::unregisterListener drops the entry. The caller still
 * owns the id it passed in and must release that one itself.
 *
 * The Java listener must be reachable from the registry alone, and today it is: the tables in
 * EventListenerImpl hold it only weakly (a WeakHashMap and a WeakReference), so the native
 * reference is what keeps both the EventListenerImpl and the user's EventListener alive.
 * Registering `self` weakly would collect it at the next GC.
 */
WKJ_EXPORT int64_t wkj_event_listener_create(wkj_ref self);

/*
 * EventListenerImpl.twkDispatchEvent(long, long) - dispatches `event_peer` to the JS-side
 * listener at `listener_peer`, which is how a Java listener wrapping a JS one forwards.
 *
 * Does nothing when either peer is 0, or when no script execution context is on the stack:
 * the same three-way guard the JNI function carried.
 */
WKJ_EXPORT void wkj_event_listener_dispatch_event(int64_t listener_peer, int64_t event_peer);

/*
 * EventListenerImpl.twkDisposeJSPeer(long) - drops the reference Java holds on the listener at
 * `peer`. The deref may destroy the listener, which runs the dispose callback above before
 * this function returns. A peer of 0 does nothing.
 */
WKJ_EXPORT void wkj_event_listener_dispose_js_peer(int64_t peer);

/* ======================================================================================== */
/* Downcalls: com.sun.webkit.WebPage, the two that lived in JavaDOMUtils.cpp                */
/* ======================================================================================== */

/*
 * WebPage.twkGetDocument(long) and WebPage.twkGetOwnerElement(long).
 *
 * Both used to build the org.w3c.dom.Node inside C, with FindClass("com/sun/webkit/dom/NodeImpl")
 * and a CallStaticObjectMethod of NodeImpl.getImpl(long) (JavaDOMUtils.cpp:88-103). They now
 * return the peer and Java calls NodeImpl.getImpl itself, which removes the last upcall and the
 * last FindClass from this slice; contract 2 forbids returning a Java object from C in any case.
 *
 * The returned peer carries ONE reference for Java - the peer->ref() that makeObjectFromNode
 * made on the line before the upcall - and NodeImpl's disposer drops it, or getCachedImpl drops
 * it at once on a cache hit. The Java side must pass the peer to NodeImpl.getImpl exactly once
 * and must not deref it itself; anything else moves the NodeImpl hash count that LeakTest pins.
 *
 * 0 means the Java-visible result is null: a null frame, a frame that is not a local one, or no
 * document / no owner element. NodeImpl.getImpl(0) already answers null, so the facade can pass
 * 0 straight through.
 */
WKJ_EXPORT int64_t wkj_frame_get_document(int64_t frame);
WKJ_EXPORT int64_t wkj_frame_get_owner_element(int64_t frame);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* WEBKIT_JAVA_API_EVENTS_H */
