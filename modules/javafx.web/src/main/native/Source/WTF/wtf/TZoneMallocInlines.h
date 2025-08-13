/*
 * Copyright (C) 2023-2024 Apple Inc. All rights reserved.
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

#include <wtf/ForbidHeapAllocation.h>
#include <wtf/Platform.h>

WTF_ALLOW_UNSAFE_BUFFER_USAGE_BEGIN

#if USE(SYSTEM_MALLOC) || !USE(TZONE_MALLOC)

#include <wtf/FastMalloc.h>

#define WTF_MAKE_TZONE_ALLOCATED_INLINE(typeName) WTF_MAKE_FAST_ALLOCATED

#define WTF_MAKE_TZONE_ALLOCATED_IMPL(typeName) using __makeTZoneMallocedMacroSemicolonifier UNUSED_TYPE_ALIAS = int

#define WTF_MAKE_COMPACT_TZONE_ALLOCATED_IMPL(typeName) using __makeTZoneMallocedMacroSemicolonifier UNUSED_TYPE_ALIAS = int

#define WTF_MAKE_STRUCT_TZONE_ALLOCATED_IMPL(typeName) WTF_MAKE_TZONE_ALLOCATED_IMPL(typeName)

#if USE(SYSTEM_MALLOC) || !USE(ISO_MALLOC)

#define WTF_MAKE_TZONE_OR_ISO_ALLOCATED_IMPL(name) using __makeTZoneMallocedMacroSemicolonifier UNUSED_TYPE_ALIAS = int
#define WTF_MAKE_COMPACT_TZONE_OR_ISO_ALLOCATED_IMPL(name) using __makeTZoneMallocedMacroSemicolonifier UNUSED_TYPE_ALIAS = int

#else // !USE(SYSTEM_MALLOC) && USE(ISO_MALLOC) && !USE(TZONE_MALLOC)

#include <bmalloc/IsoHeapInlines.h>

#define WTF_MAKE_TZONE_OR_ISO_ALLOCATED_IMPL(name) MAKE_BISO_MALLOCED_IMPL(name, IsoHeap)
#define WTF_MAKE_COMPACT_TZONE_OR_ISO_ALLOCATED_IMPL(name) MAKE_BISO_MALLOCED_IMPL(name, CompactIsoHeap)

#endif

#else // !USE(SYSTEM_MALLOC) && USE(TZONE_MALLOC)

#include <bmalloc/TZoneHeapInlines.h>

#if !BUSE(TZONE)
#error "TZones enabled in WTF, but not enabled in bmalloc"
#endif

#define WTF_MAKE_TZONE_ALLOCATED_INLINE(type) MAKE_BTZONE_MALLOCED_INLINE(type, NonCompact)

#define WTF_MAKE_TZONE_ALLOCATED_IMPL(type) MAKE_BTZONE_MALLOCED_IMPL(type, NonCompact)

#define WTF_MAKE_COMPACT_TZONE_ALLOCATED_IMPL(type) MAKE_BTZONE_MALLOCED_IMPL(type, Compact)

#define WTF_MAKE_STRUCT_TZONE_ALLOCATED_IMPL(typeName) \
    MAKE_BTZONE_MALLOCED_IMPL(typeName, NonCompact)

#define WTF_MAKE_TZONE_OR_ISO_ALLOCATED_IMPL(name) MAKE_BTZONE_MALLOCED_IMPL(name, NonCompact)
#define WTF_MAKE_COMPACT_TZONE_OR_ISO_ALLOCATED_IMPL(name) MAKE_BTZONE_MALLOCED_IMPL(name, Compact)

#endif

WTF_ALLOW_UNSAFE_BUFFER_USAGE_END
