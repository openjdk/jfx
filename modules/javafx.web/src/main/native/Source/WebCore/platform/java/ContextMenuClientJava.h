/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include "ContextMenuClient.h"
#include <wtf/java/JavaEnv.h>

namespace WebCore {

class ContextMenu;

class ContextMenuClientJava : public ContextMenuClient {
public:
    ContextMenuClientJava(const JLObject &webPage);

    void contextMenuDestroyed() override;

    void downloadURL(const URL& url) override;
    void searchWithGoogle(const Frame*) override;
    void lookUpInDictionary(Frame*) override;
    bool isSpeaking() override;
    void speak(const String&) override;
    void stopSpeaking() override;

private:
    JGObject m_webPage;
};
}
