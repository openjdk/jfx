/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "GraphicsContext.h"
#include "GraphicsContextJava.h"
#include "Icon.h"
#include "IntRect.h"
#include "JavaEnv.h"
#include "JavaRef.h"
#include "NotImplemented.h"

#include <wtf/RefPtr.h>
#include <wtf/PassRefPtr.h>
#include <wtf/text/WTFString.h>

#include "PlatformContextJava.h"
#include "com_sun_webkit_graphics_GraphicsDecoder.h"


using namespace WebCore;

namespace WebCore {

Icon::Icon(const JLObject &jicon)
    : m_jicon(RQRef::create(jicon))
{   
}

Icon::~Icon()
{
}
  
PassRefPtr<Icon> Icon::createIconForFiles(const Vector<String>& filenames)
{
    notImplemented();
    return 0;
}

void Icon::paint(GraphicsContext* gc, const IntRect& rect)
{
    gc->platformContext()->rq().freeSpace(16)    
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWICON  
    << *m_jicon << (jint)rect.x() <<  (jint)rect.y();
}

} // namespace WebCore
