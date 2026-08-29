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

#include "RQRef.h"

#include "WKJPlatformJava.h"

namespace WebCore {

RQRef::~RQRef()
{
    if (-1 != m_refID) {
        const WKJHostGraphics* cb = wkjGraphics();

        // Do it only if the host table is here. The JNI version made the same test against
        // the JNI environment, because this destructor can run after the VM has detached.
        if (cb && cb->ref_deref) {
            cb->ref_deref(m_ref.get());
            wkjCheckAndClearException();
        }
    }
}

RQRef::operator int32_t()
{
    if (-1 == m_refID) {
        const WKJHostGraphics* cb = wkjGraphics();
        if (!cb)
            return m_refID;

        if (cb->ref_get_id)
            m_refID = cb->ref_get_id(m_ref.get());

        // Pair the ref() with the deref() in the destructor, which only runs once an id has
        // been resolved. Without an id there is nothing to pair, so nothing is ref()ed.
        if (-1 != m_refID && cb->ref_ref)
            cb->ref_ref(m_ref.get());

        wkjCheckAndClearException();
    }
    return m_refID;
}

} // namespace WebCore
