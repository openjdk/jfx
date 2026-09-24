/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _GLASS_STRING_BLOCK_H
#define _GLASS_STRING_BLOCK_H

#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <string>
#include <vector>

/*
 * The "string block" of glass_win_api.h's clipboard section: count NUL-terminated UTF-16 strings laid
 * end to end plus one extra NUL. Java builds one for the mimes it pushes; the library builds one for
 * the mimes it pops and the paths a file dialog returns. Blocks and strings the library hands out are
 * malloc'd (gwin_free is free) and owned by the caller.
 */

/* Builds a block from items; NULL when the heap is exhausted. An empty vector is a one-NUL block. */
inline uint16_t* GwinMakeStringBlock(const std::vector<std::wstring>& items)
{
    size_t total = 1;
    for (size_t i = 0; i < items.size(); ++i) {
        total += items[i].size() + 1;
    }
    uint16_t* block = reinterpret_cast<uint16_t*>(malloc(total * sizeof(uint16_t)));
    if (block == NULL) {
        return NULL;
    }
    uint16_t* d = block;
    for (size_t i = 0; i < items.size(); ++i) {
        const size_t n = items[i].size() + 1;   // with its NUL
        memcpy(d, items[i].c_str(), n * sizeof(wchar_t));
        d += n;
    }
    *d = 0;
    return block;
}

/* A malloc'd NUL-terminated copy of s (gwin_dialog_folder's out_path); NULL when the heap is exhausted. */
inline uint16_t* GwinMakeString(const wchar_t* s)
{
    const size_t n = wcslen(s) + 1;
    uint16_t* copy = reinterpret_cast<uint16_t*>(malloc(n * sizeof(uint16_t)));
    if (copy != NULL) {
        memcpy(copy, s, n * sizeof(wchar_t));
    }
    return copy;
}

/* Walks a block the caller supplied: the string after s. */
inline const wchar_t* GwinNextString(const wchar_t* s)
{
    return s + wcslen(s) + 1;
}

#endif // _GLASS_STRING_BLOCK_H
