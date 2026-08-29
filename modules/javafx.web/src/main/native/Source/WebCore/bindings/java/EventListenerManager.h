/*
 * Copyright (c) 2022, 2026, Oracle and/or its affiliates. All rights reserved.
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

#ifndef EVENT_LISTENER_MANAGER_H
#define EVENT_LISTENER_MANAGER_H


#include "config.h"

#include <map>
#include <wtf/NeverDestroyed.h>
#include <iterator>
#include <wtf/java/WKJHandle.h>

namespace WebCore {

class LocalDOMWindow;
class JavaEventListener;


/*
 * Owns the Java listener object for as long as the C++ listener is registered. This is the one
 * structure in the DOM surface that pins a Java object: it held a JGObject, i.e. a JNI global
 * reference, and it now holds a WKJHandle, i.e. a retained registry id. Both keep the referent
 * strongly reachable, which is load bearing - the Java tables in EventListenerImpl reference
 * the listener only weakly, so this handle is what keeps it, and the user EventListener behind
 * it, alive.
 *
 * ref_count is a second, hand-rolled count layered on top of that handle, and it is not the
 * handle's own count: it exists so that one listener registered against several DOM windows is
 * released only when the last of them goes away. Its arithmetic is preserved exactly as it was
 * (see unregisterListener), including the case where it never reaches 1.
 */
class ListenerJObjectWrapper {
    WKJHandle listenerObj;
    unsigned int ref_count = 0;
public:
    /*
     * `listenerObj` is borrowed; a reference is taken here and released by the destructor.
     * This is the retain that the JGObject-from-JLObject assignment used to perform.
     */
    explicit ListenerJObjectWrapper(wkj_ref listenerObj) {
        this->listenerObj = WKJHandle::retained(listenerObj);
    }

    ~ListenerJObjectWrapper() {
        listenerObj.clear();
    }
    /* Borrowed: ownership stays with this wrapper, so the caller must not release it. */
    wkj_ref getListenerJObject() { return listenerObj.get(); }
    void ref() { ++ref_count; }
    void dref() { --ref_count; }
    unsigned int use_count() { return ref_count;}
};

class EventListenerManager {
    EventListenerManager() = default;
    WTF_MAKE_NONCOPYABLE(EventListenerManager);

    std::map<JavaEventListener*, ListenerJObjectWrapper*> listenerJObjectMap;
    std::multimap<JavaEventListener*, LocalDOMWindow*> listenerDOMWindowMultiMap;

    friend class NeverDestroyed<EventListenerManager>;

public:
    static EventListenerManager& get_instance();

    void registerListener(JavaEventListener *listener, wkj_ref listenerJObj);
    void unregisterListener(JavaEventListener *listener) ;
    /*
     * The id of the Java listener, or 0 when there is no entry. The id is borrowed for the
     * duration of the call and must not be released by the caller: the map entry holds the
     * only reference. The JNI version returned a JGObject by value, which minted a second
     * global reference and destroyed it at the end of the enclosing full expression; that
     * pair had no observable effect, because the map entry already kept the object alive for
     * exactly the same window, so it is not reproduced.
     */
    wkj_ref getListenerJObject(JavaEventListener *listener);

    void registerDOMWindow(LocalDOMWindow*, JavaEventListener *listener);
    void unregisterDOMWindow(LocalDOMWindow*);
};

} // namespace WebCore

#endif // EVENT_LISTENER_MANAGER_H
