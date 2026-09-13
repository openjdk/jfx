/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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
#include <wtf/java/JavaRef.h>
#include <jni.h>

namespace WebCore {

class LocalDOMWindow;
class JavaEventListener;


class ListenerJObjectWrapper {
    JGObject listenerObj;
    unsigned int ref_count = 0;
public:
    ListenerJObjectWrapper(const JLObject& listenerObj) {
        this->listenerObj = listenerObj;
    }

    ~ListenerJObjectWrapper() {
        listenerObj.clear();
    }
    JGObject getListenerJObject() { return listenerObj; }
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

    void registerListener(JavaEventListener *listener, const JLObject &listenerJObj);
    void unregisterListener(JavaEventListener *listener) ;
    JGObject getListenerJObject(JavaEventListener *listener);

    void registerDOMWindow(LocalDOMWindow*, JavaEventListener *listener);
    void unregisterDOMWindow(LocalDOMWindow*);
};

} // namespace WebCore

#endif // EVENT_LISTENER_MANAGER_H
