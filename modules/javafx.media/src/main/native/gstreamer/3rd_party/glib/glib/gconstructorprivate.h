/*
 * Copyright © 2023 Luca Bacci
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

#include "gconstructor.h"

#ifdef _WIN32
#include <windows.h>
#endif

#ifdef _WIN32

#ifdef __cplusplus
/* const defaults to static (internal visibility) in C++,
 * but we want extern instead */
#define G_EXTERN_CONST extern const
#else
/* Using extern const in C is perfectly valid, but triggers
 * a warning in GCC and CLANG, therefore we avoid it */
#define G_EXTERN_CONST const
#endif

#ifdef _MSC_VER

#define G_HAS_TLS_CALLBACKS 1

#define G_DEFINE_TLS_CALLBACK(func) \
__pragma (section (".CRT$XLCE", long, read))                                \
                                                                            \
static void NTAPI func (PVOID, DWORD, PVOID);                               \
                                                                            \
G_BEGIN_DECLS                                                               \
__declspec (allocate (".CRT$XLCE"))                                         \
G_EXTERN_CONST PIMAGE_TLS_CALLBACK _ptr_##func = func;                      \
G_END_DECLS                                                                 \
                                                                            \
__pragma (comment (linker, "/INCLUDE:" G_MSVC_SYMBOL_PREFIX "_tls_used"))   \
__pragma (comment (linker, "/INCLUDE:" G_MSVC_SYMBOL_PREFIX "_ptr_" #func))

#else

#define G_HAS_TLS_CALLBACKS 1

#define G_DEFINE_TLS_CALLBACK(func) \
static void NTAPI func (PVOID, DWORD, PVOID);          \
                                                       \
G_BEGIN_DECLS                                          \
__attribute__ ((section (".CRT$XLCE")))                \
G_EXTERN_CONST PIMAGE_TLS_CALLBACK _ptr_##func = func; \
G_END_DECLS

#endif /* _MSC_VER */

#endif /* _WIN32 */
