/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include "InspectorClient.h"
#include "InspectorFrontendChannel.h"
#include <wtf/java/JavaEnv.h>

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
    void sendMessageToFrontend(const String& message) override;

private:
    JGObject m_webPage;
};

} // namespace WebCore
