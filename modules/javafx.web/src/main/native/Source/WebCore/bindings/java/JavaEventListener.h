/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include <WebCore/Event.h>
#include <WebCore/EventListener.h>
#include <WebCore/Node.h>

#include <wtf/RefCounted.h>
#include <wtf/PassRefPtr.h>
#include <wtf/Vector.h>
#include <wtf/java/JavaRef.h>

namespace WebCore {

class JavaEventListener final : public EventListener {
public:
    JavaEventListener(const JLObject &listener)
        : EventListener(NativeEventListenerType)
        , m_joListener(listener)
    {
        relaxAdoptionRequirement();
    }

    ~JavaEventListener() override;

    bool operator == (const EventListener&) const override;
    void handleEvent(ScriptExecutionContext* context, Event* event) override;

    JGObject m_joListener;
    static ScriptExecutionContext* scriptExecutionContext();
    bool isJavaEventListener() const override { return true; }
private:
    static Vector<ScriptExecutionContext*> sm_vScriptExecutionContexts;
};

}; // namespace WebCore
