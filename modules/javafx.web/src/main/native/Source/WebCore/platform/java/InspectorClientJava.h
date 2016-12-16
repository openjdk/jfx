/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef InspectorClientJava_h
#define InspectorClientJava_h

#include "InspectorClient.h"
#include "InspectorFrontendChannel.h"
#include "JavaEnv.h"

namespace WebCore {

class InspectorClientJava final
    : public InspectorClient,
      public Inspector::FrontendChannel
{
public:
    InspectorClientJava(const JLObject &webPage);

    void inspectedPageDestroyed() override;

    Inspector::FrontendChannel* openLocalFrontend(InspectorController*) override;
    void bringFrontendToFront() override;

    void highlight() override;
    void hideHighlight() override;

    ConnectionType connectionType() const override { return Inspector::FrontendChannel::ConnectionType::Local; }
    bool sendMessageToFrontend(const String& message) override;

private:
    JGObject m_webPage;
};

} // namespace WebCore

#endif // InspectorClientJava_h
