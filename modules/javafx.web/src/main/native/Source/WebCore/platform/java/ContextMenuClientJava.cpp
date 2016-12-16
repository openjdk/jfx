/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "NotImplemented.h"

#include "ContextMenu.h"
#include "ContextMenuClientJava.h"
#include "PlatformMenuDescription.h"

namespace WebCore {

ContextMenuClientJava::ContextMenuClientJava(const JLObject &webPage)
    : m_webPage(webPage)
{
}

void ContextMenuClientJava::contextMenuDestroyed()
{
    delete this;
}

void ContextMenuClientJava::downloadURL(const URL& url)
{
    notImplemented();
}

void ContextMenuClientJava::searchWithGoogle(const Frame*)
{
    notImplemented();
}

void ContextMenuClientJava::lookUpInDictionary(Frame*)
{
    notImplemented();
}

bool ContextMenuClientJava::isSpeaking()
{
    notImplemented();
    return false;
}

void ContextMenuClientJava::speak(const String&)
{
    notImplemented();
}

void ContextMenuClientJava::stopSpeaking()
{
    notImplemented();
}

}

