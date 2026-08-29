/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "WKJDOMUtils.h"
#include "JavaEventListener.h"

#include <webkit_java_api.h>

namespace WebCore {

/*
 * The table installed by wkj_install_event_listener_callbacks, NULL until Java installs it.
 * The pointer is kept, not the struct: Java owns the memory and it outlives the library, the
 * same rule wkj_init states for WKJHost.
 *
 * The name carries the wkj prefix on purpose - WebCore builds these sources unified, so
 * several .cpp files share one translation unit and therefore one set of file-scope names.
 */
static const WKJEventListenerCallbacks* wkjEventListenerCallbacks = nullptr;

// DOM Document implements ScriptExecutionContext!
// FIXME: it need to be per-thread object then [WORKERS] would be introduced!
Vector<ScriptExecutionContext*> JavaEventListener::sm_vScriptExecutionContexts;

ScriptExecutionContext* JavaEventListener::scriptExecutionContext()
{
    return sm_vScriptExecutionContexts.size() == 0
        ? nullptr
        : sm_vScriptExecutionContexts.last();
}

bool JavaEventListener::operator==(const EventListener& other) const
{
    const JavaEventListener* jother = other.isJavaEventListener()
                                        ? static_cast<const JavaEventListener*>(&other)
                                        : nullptr;
    return this == jother;
}

void JavaEventListener::handleEvent(ScriptExecutionContext& context, Event& event)
{
    //we need to store context for cascade JS EL execution.
    sm_vScriptExecutionContexts.append(&context);

    /*
     * The JNI version resolved com.sun.webkit.dom.EventListenerImpl with FindClass in a
     * function-local static and cached the method id beside it, which bound the class to
     * whatever class loader happened to be on the stack the first time any listener fired.
     * The installed table has no lookup and no loader.
     *
     * event.ref() stays exactly where it was: the reference it takes is the one that
     * EventImpl.getImpl hands to the Java disposer, or that getCachedImpl drops at once on a
     * cache hit. It is taken only when there is a Java listener to hand it to, so that the
     * pairing balances on every path - with no table installed, and with no map entry.
     *
     * A missing map entry cannot happen: the constructor registers, and the entry outlives
     * the listener. The JNI code did not test for it either, but its failure mode was worse -
     * CallVoidMethod on a null jobject, i.e. a crash rather than a skipped dispatch.
     */
    const WKJEventListenerCallbacks* callbacks = wkjEventListenerCallbacks;
    wkj_ref listener = EventListenerManager::get_instance().getListenerJObject(this);
    if (callbacks && callbacks->handle_event && listener) {
        event.ref();
        callbacks->handle_event(listener, wkj_from_ptr(&event));
    }

    sm_vScriptExecutionContexts.removeLast();

    /*
     * This is WTF::CheckAndClearException(env), which the JNI code called here and whose
     * result it ignored: a Throwable out of a listener was discarded and dispatch carried on
     * with the next listener. That swallowing is the behaviour of every page with a throwing
     * listener, so it is preserved rather than fixed. Java has already caught the Throwable -
     * an escaping exception would take down the JVM - and this call only clears the flag it
     * left behind, so that a later caller that does branch on it is not misled.
     */
    if (wkj_host && wkj_host->core.check_and_clear_exception)
        wkj_host->core.check_and_clear_exception();
}

JavaEventListener::~JavaEventListener()
{
    /*
     * WC_GETJAVAENV_CHKRET(env) used to return early here when the JVM was no longer
     * reachable, which is what an uninstalled or detached table now reproduces.
     */
    const WKJEventListenerCallbacks* callbacks = wkjEventListenerCallbacks;
    if (callbacks && callbacks->dispose)
        callbacks->dispose(wkj_from_ptr(this));
}

} // namespace WebCore

using namespace WebCore;

extern "C" {

WKJ_EXPORT void wkj_install_event_listener_callbacks(const WKJEventListenerCallbacks* callbacks)
{
    WKJCallScope wkjScope;
    WebCore::wkjEventListenerCallbacks = callbacks;
}

WKJ_EXPORT int64_t wkj_event_listener_create(wkj_ref self)
{
    WKJCallScope wkjScope;
    return wkj_from_ptr(new WebCore::JavaEventListener(self));
}

WKJ_EXPORT void wkj_event_listener_dispose_js_peer(int64_t peer)
{
    WKJCallScope wkjScope;
    EventListener* pEventListener = static_cast<EventListener *>(wkj_to_ptr(peer));
    if (pEventListener)
        pEventListener->deref();
}

WKJ_EXPORT void wkj_event_listener_dispatch_event(int64_t listenerPeer, int64_t eventPeer)
{
    WKJCallScope wkjScope;
    if (!listenerPeer || !eventPeer || !JavaEventListener::scriptExecutionContext())
        return;

    static_cast<EventListener *>(wkj_to_ptr(listenerPeer))->handleEvent(
        *JavaEventListener::scriptExecutionContext(),
        *static_cast<Event*>(wkj_to_ptr(eventPeer)));
}

}
