/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "PageCache.h"
#include <wtf/java/JavaEnv.h>

#include "com_sun_webkit_PageCache.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_com_sun_webkit_PageCache_twkGetCapacity
  (JNIEnv *, jclass)
{
    return WebCore::PageCache::singleton().maxSize();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_PageCache_twkSetCapacity
  (JNIEnv *, jclass, jint capacity)
{
    ASSERT(capacity >= 0);
    WebCore::PageCache::singleton().setMaxSize(capacity); //XXX rename twkSetCapacity func?
}

#ifdef __cplusplus
}
#endif
