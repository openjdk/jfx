/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef ImageBufferDataJava_h
#define ImageBufferDataJava_h

#include "RQRef.h"

namespace WebCore {
    class IntSize;
    class ImageBuffer;

    class ImageBufferData {
    public:
        ImageBufferData(const IntSize &size, ImageBuffer &rq_holder);
        JLObject getWCImage() const;
        unsigned char *data() const;
        void update();

        //we need to have RQRef here due to [deref]
        //callback in destructor. Texture need to be released.
        RefPtr<RQRef> m_image; //WCImage wrapper
        ImageBuffer &m_rq_holder; //accessor to the RenderQueue
        //RenderQueue need to be processed before pixel buffer extraction.
    };
}  // namespace WebCore

#endif  //ImageBufferDataJava_h
