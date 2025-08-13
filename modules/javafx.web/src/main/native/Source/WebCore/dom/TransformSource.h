/*
 * Copyright (C) 2009 Jakub Wieczorek <faw217@gmail.com>
 * Copyright (C) 2024 Apple Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this library; see the file COPYING.LIB.  If not, write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

#pragma once

#if ENABLE(XSLT)

// FIXME (286277): Stop ignoring -Wundef and -Wdeprecated-declarations in code that imports libxml and libxslt headers
IGNORE_WARNINGS_BEGIN("deprecated-declarations")
IGNORE_WARNINGS_BEGIN("undef")
#include <libxml/tree.h>
IGNORE_WARNINGS_END
IGNORE_WARNINGS_END
#include <wtf/Forward.h>
#include <wtf/Noncopyable.h>
#include <wtf/text/WTFString.h>

namespace WebCore {

typedef xmlDocPtr PlatformTransformSource;

class TransformSource {
    WTF_MAKE_TZONE_ALLOCATED(TransformSource);
    WTF_MAKE_NONCOPYABLE(TransformSource);
public:
    explicit TransformSource(const PlatformTransformSource&);
    ~TransformSource();

    PlatformTransformSource platformSource() const { return m_source; }

private:
    PlatformTransformSource m_source;
};

} // namespace WebCore

#endif // ENABLE(XSLT)
