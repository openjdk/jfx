/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef InspectorClientJava_h
#define InspectorClientJava_h

#include "InspectorClient.h"
#include "InspectorFrontendChannel.h"
#include "JavaEnv.h"

namespace WebCore {

class InspectorClientJava
    : public InspectorClient,
      public InspectorFrontendChannel
{
public:
    InspectorClientJava(const JLObject &webPage);

    virtual void inspectorDestroyed();

    virtual InspectorFrontendChannel* openInspectorFrontend(InspectorController*);
    virtual void closeInspectorFrontend();
    virtual void bringFrontendToFront();

    virtual void highlight();
    virtual void hideHighlight();

    virtual bool sendMessageToFrontend(const String& message);

private:
    JGObject m_webPage;
};

} // namespace WebCore

#endif // InspectorClientJava_h
