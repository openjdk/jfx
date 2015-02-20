/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef ContextMenuClientJava_h
#define ContextMenuClientJava_h

#include "ContextMenuClient.h"
#include "JavaEnv.h"

namespace WebCore {

    class ContextMenu;

    class ContextMenuClientJava : public ContextMenuClient
    {
    public:
        ContextMenuClientJava(const JLObject &webPage);

        virtual void contextMenuDestroyed();

        virtual PlatformMenuDescription getCustomMenuFromDefaultItems(ContextMenu*);
        virtual void contextMenuItemSelected(ContextMenuItem*, const ContextMenu*);

        virtual void downloadURL(const URL& url);
        virtual void searchWithGoogle(const Frame*);
        virtual void lookUpInDictionary(Frame*);
        virtual bool isSpeaking();
        virtual void speak(const String&);
        virtual void stopSpeaking();
    private:
        JGObject m_webPage;
    };
}

#endif
