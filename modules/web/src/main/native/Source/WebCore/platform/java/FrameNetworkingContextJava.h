/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef FrameNetworkingContextJava_h
#define FrameNetworkingContextJava_h

#include "FrameNetworkingContext.h"

namespace WebCore {

class FrameNetworkingContextJava : public FrameNetworkingContext {
public:
    static PassRefPtr<FrameNetworkingContextJava> create(Frame* frame)
    {
        return adoptRef(new FrameNetworkingContextJava(frame));
    }

    Page* page() const { return frame()->page(); }
private:
    FrameNetworkingContextJava(Frame* frame)
        : WebCore::FrameNetworkingContext(frame)
    {
    }
};

}

#endif // FrameNetworkingContextJava_h
