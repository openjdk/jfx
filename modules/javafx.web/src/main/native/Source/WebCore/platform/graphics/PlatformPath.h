/*
 * Copyright (C) 2023 Apple Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#if USE(CG)
#include <CoreGraphics/CGPath.h>
#elif PLATFORM(JAVA)
#include <wtf/RefPtr.h>
#include "RQRef.h"
#else
#include "RefPtrCairo.h"
#endif

#if USE(CG)
typedef struct CGPath PlatformPath;
typedef PlatformPath* PlatformPathPtr;
#elif PLATFORM(JAVA)
typedef RefPtr<WebCore::RQRef> PlatformPath;
typedef RefPtr<WebCore::RQRef> PlatformPathPtr;
#else
typedef cairo_t* PlatformPathPtr;
#endif

namespace WebCore {

#if USE(CG)
class PathCG;
using PlatformPathImpl = PathCG;
#elif PLATFORM(JAVA)
class PathJava;
using PlatformPathImpl = PathJava;
#else
class PathCairo;
using PlatformPathImpl = PathCairo;
#endif

} // namespace WebCore
