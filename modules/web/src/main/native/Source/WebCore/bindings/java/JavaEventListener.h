/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef _JavaEventListener_h
#define _JavaEventListener_h

#include "Event.h"
#include "EventListener.h"
#include "Node.h"

#include "wtf/RefCounted.h"
#include "wtf/PassRefPtr.h"
#include "wtf/Vector.h"
#include "wtf/java/JavaRef.h"

namespace WebCore {

class JavaEventListener : public EventListener {
public:
    JavaEventListener(const JLObject &listener)
        : EventListener(NativeEventListenerType)
        , m_joListener(listener)
    {
        relaxAdoptionRequirement();
    }

    virtual ~JavaEventListener();

    virtual bool operator==(const EventListener&);
    virtual void handleEvent(ScriptExecutionContext* context, Event* event);

    JGObject m_joListener;
    static ScriptExecutionContext* scriptExecutionContext();

private:
    static Vector<ScriptExecutionContext*> sm_vScriptExecutionContexts;
};

}; // namespace WebCore

#endif
