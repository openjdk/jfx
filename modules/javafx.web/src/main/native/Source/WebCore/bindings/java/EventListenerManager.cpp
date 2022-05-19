/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
#include "DOMWindow.h"

namespace WebCore {

EventListenerManager& EventListenerManager::get_instance()
{
    static NeverDestroyed<EventListenerManager> sharedManager;
    return sharedManager;
}

EventListenerManager::EventListenerManager() { }

void EventListenerManager::registerListener(JavaEventListener *ptr, const JLObject &listener)
{
    JavaObjectWrapperHandler *temp_ref = new JavaObjectWrapperHandler(listener);
    std::pair<JavaEventListener*, JavaObjectWrapperHandler*> entry{ ptr, temp_ref };
    listener_lists.insert(entry);
}

void EventListenerManager::unregisterListener(JavaEventListener *ptr)
{
     std::map<JavaEventListener*, JavaObjectWrapperHandler*>::iterator it;
     it = listener_lists.find(ptr);
     JNIEnv *env = nullptr;
     env = JavaScriptCore_GetJavaEnv();

     if (it != listener_lists.end()) {
         if (it->second && it->second->use_count() == 1) {
              delete it->second;
              it->second = nullptr;
         }

         if (it->second && it->second->use_count() > 1)
             it->second->dref();
     }
}

jobject EventListenerManager::get_listener(JavaEventListener *ptr)
{
     std::map<JavaEventListener*, JavaObjectWrapperHandler*>::iterator it;
     it = listener_lists.find(ptr);
     if (it != listener_lists.end())
         return it->second->get_listener();

     return nullptr;
}

void EventListenerManager::registerDOMWindow(DOMWindow* window, JavaEventListener *ptr)
{
    std::map<JavaEventListener*, JavaObjectWrapperHandler*>::iterator it;
    it = listener_lists.find(ptr);
    if (it != listener_lists.end())
        it->second->ref();

    std::pair<JavaEventListener*, DOMWindow*> entry{ ptr, window};
    windowHasEvent.insert(entry);
}

void EventListenerManager::unregisterDOMWindow(DOMWindow* window)
{
     std::multimap<JavaEventListener*, DOMWindow*>::iterator win_it;
     for (win_it = windowHasEvent.begin(); win_it != windowHasEvent.end(); win_it++) {
         // de register associated event listeners with window
         if( window == win_it->second) {
             unregisterListener(win_it->first);
         }
     }
}

} // namespace WebCore

