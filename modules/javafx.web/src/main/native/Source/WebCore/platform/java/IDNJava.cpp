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
#include <wkj_constants.h>
#include "IDNJava.h"
#include "PlatformJavaClasses.h"

namespace WebCore {

namespace IDNJava {

String toASCII(const String& hostname)
{
    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->idn_to_ascii)
        return emptyString();

    WKJStringArg host(hostname);
    String result = wkjFetchString([&](uint16_t* buffer, int32_t capacity, int32_t* length) {
        return cb->idn_to_ascii(host.data(), host.length(),
                                com_sun_webkit_network_URLLoaderBase_ALLOW_UNASSIGNED,
                                buffer, capacity, length);
    });
    wkjCheckAndClearException();

    /*
     * A null result collapses to the empty string, because the JNI code ended in
     * String(env, result), whose constructor mapped a null Java string to StringImpl::empty()
     * (contract 11.1). Returning the null String here instead would change what URL parsing
     * sees when IDN.toASCII fails.
     */
    return result.isNull() ? emptyString() : result;
}

} // namespace IDNJava

} // namespace WebCore
