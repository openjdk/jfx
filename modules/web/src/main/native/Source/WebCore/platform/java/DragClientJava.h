/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef DragClientJava_h
#define DragClientJava_h

#include "Clipboard.h"
#include "DragActions.h"
#include "DragClient.h"
#include "DragImage.h"
#include "Frame.h"
#include "IntPoint.h"
#include "JavaEnv.h"

namespace WebCore {
    
    class Clipboard;
    class DragData;
    class Frame;
    class Image;
    class HTMLImageElement;
    
    class DragClientJava : public DragClient {
    public:
        DragClientJava(const JLObject &webPage);
        virtual ~DragClientJava();

        virtual void willPerformDragDestinationAction(DragDestinationAction, DragData& data);
        virtual void willPerformDragSourceAction(DragSourceAction, const IntPoint&, Clipboard& clipboard);
        virtual DragDestinationAction actionMaskForDrag(DragData& data);
        //We work in window rather than view coordinates here
        virtual DragSourceAction dragSourceActionMaskForPoint(const IntPoint& windowPoint);
        
        virtual void startDrag(DragImageRef dragImage, const IntPoint& dragImageOrigin, const IntPoint& eventPos, Clipboard& clipboard, Frame& frame, bool linkDrag = false);
        virtual DragImageRef createDragImageForLink(URL& url, const String& label, Frame* frame);
        
        virtual void dragControllerDestroyed();
    private:
        JGObject m_webPage;
    };
    
} // namespace WebCore

#endif // !DragClientJava_h

