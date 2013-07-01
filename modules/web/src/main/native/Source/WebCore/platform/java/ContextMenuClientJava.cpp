/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

PlatformMenuDescription ContextMenuClientJava::getCustomMenuFromDefaultItems(ContextMenu* contextMenu)
{
    return contextMenu->platformDescription();
}

void ContextMenuClientJava::contextMenuItemSelected(ContextMenuItem*, const ContextMenu*)
{
    notImplemented();
}

void ContextMenuClientJava::downloadURL(const KURL& url)
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

