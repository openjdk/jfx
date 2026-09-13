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

#include "EventListenerManager.h"
#include "JavaEventListener.h"
#include "LocalDOMWindow.h"

namespace WebCore {

EventListenerManager& EventListenerManager::get_instance()
{
    static NeverDestroyed<EventListenerManager> sharedManager;
    return sharedManager;
}

void EventListenerManager::registerListener(JavaEventListener *listener, const JLObject &listenerObj)
{
    ListenerJObjectWrapper *temp_ref = new ListenerJObjectWrapper(listenerObj);
    std::pair<JavaEventListener*, ListenerJObjectWrapper*> entry{ listener, temp_ref };
    listenerJObjectMap.insert(entry);
}

void EventListenerManager::unregisterListener(JavaEventListener *listener)
{
     std::map<JavaEventListener*, ListenerJObjectWrapper*>::iterator it;
     it = listenerJObjectMap.find(listener);

     if (it != listenerJObjectMap.end()) {
         if (it->second && it->second->use_count() == 1) {
             delete it->second;
             it->second = nullptr;
             listenerJObjectMap.erase(it); // remove from list
         }
         else if (it->second && it->second->use_count() > 1)
             it->second->dref();
     }
}

JGObject EventListenerManager::getListenerJObject(JavaEventListener *listener)
{
    std::map<JavaEventListener*, ListenerJObjectWrapper*>::iterator it;
    it = listenerJObjectMap.find(listener);
    if (it != listenerJObjectMap.end())
        return it->second->getListenerJObject();

    return nullptr;
}

void EventListenerManager::registerDOMWindow(LocalDOMWindow* window, JavaEventListener *listener)
{
    std::map<JavaEventListener*, ListenerJObjectWrapper*>::iterator it;
    it = listenerJObjectMap.find(listener);
    if (it != listenerJObjectMap.end())
        it->second->ref();

    std::pair<JavaEventListener*, LocalDOMWindow*> entry{ listener, window};
    listenerDOMWindowMultiMap.insert(entry);
}

void EventListenerManager::unregisterDOMWindow(LocalDOMWindow* window)
{
    std::multimap<JavaEventListener*, LocalDOMWindow*>::iterator win_it;
    for (win_it = listenerDOMWindowMultiMap.begin(); win_it != listenerDOMWindowMultiMap.end();) {
        // de register associated event listeners with window
        // and remove the entry from the map
        if (window == win_it->second) {
            unregisterListener(win_it->first);

            std::multimap<JavaEventListener*, LocalDOMWindow*>::iterator tmp_it;
            tmp_it = win_it;
            ++win_it;
            listenerDOMWindowMultiMap.erase(tmp_it);
        } else {
            ++win_it;
        }
    }
}

} // namespace WebCore
