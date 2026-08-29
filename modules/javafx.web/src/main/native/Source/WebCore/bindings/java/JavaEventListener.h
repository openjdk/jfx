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

#pragma once

#include "Event.h"
#include "EventListener.h"
#include "EventListenerManager.h"
#include "Node.h"

#include <wtf/Vector.h>

#include <webkit_java_api.h>

namespace WebCore {

class JavaEventListener final : public EventListener {
public:
    /*
     * `listener` is the registry id of the com.sun.webkit.dom.EventListenerImpl this listener
     * forwards to. It is borrowed: EventListenerManager retains it and owns the retained id
     * for as long as the entry lives, which is what the NewGlobalRef in ListenerJObjectWrapper
     * did. The constructor is explicit because a wkj_ref is an integer and an implicit one
     * would swallow any integer expression.
     */
    explicit JavaEventListener(wkj_ref listener)
        : EventListener(NativeEventListenerType)
    {
        relaxAdoptionRequirement();
        EventListenerManager::get_instance().registerListener(this, listener);
    }

    virtual ~JavaEventListener() override;

    bool operator == (const EventListener&) const override;
    void handleEvent(ScriptExecutionContext& context, Event& event) override;
    static ScriptExecutionContext* scriptExecutionContext();
    bool isJavaEventListener() const override { return true; }
private:
    static Vector<ScriptExecutionContext*> sm_vScriptExecutionContexts;
};

}; // namespace WebCore
