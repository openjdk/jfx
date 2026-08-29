/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include "config.h"
#include "ResourceRequest.h"

#include "WKJPlatformJava.h"

namespace WebCore {
#if 0
unsigned initializeMaximumHTTPConnectionCountPerHost()
{
    // This is used by the loader to control the number of parallel load
    // requests. Our java framework employs HttpURLConnection for all
    // HTTP exchanges, so we delegate this call to java to return
    // the value of the "http.maxConnections" system property.
    const WKJHostNetwork* cb = wkjNetwork();
    if (!cb || !cb->get_max_http_connection_count_per_host)
        return 0;

    int32_t result = cb->get_max_http_connection_count_per_host();
    wkjCheckAndClearException();

    ASSERT(result >= 0);
    return static_cast<unsigned>(result);
}
#endif
} // namespace WebCore
