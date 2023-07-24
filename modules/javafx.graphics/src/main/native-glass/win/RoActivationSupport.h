/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

#pragma once

/*
 * This flag gives us function prototypes without the __declspec(dllimport) storage class specifier.
 * We're using GetProcAddress to load the functions at runtime.
 */
#define _ROAPI_

#include <roapi.h>
#include <wrl.h>
#include <hstring.h>

#define RO_CHECKED(NAME, FUNC) \
    { HRESULT res = FUNC; if (FAILED(res)) throw RoException(NAME ## " failed: ", res); }

void tryInitializeRoActivationSupport();
void uninitializeRoActivationSupport();
bool isRoActivationSupported();

/*
 * Facilitates interop between C-style strings and WinRT HSTRINGs.
 * A hstring can be constructed from a C-style string, and it can be implicitly converted to a HSTRING.
 * The lifetime of the HSTRING corresponds to the lifetime of the hstring instance.
 */
struct hstring
{
    hstring(const char* str);
    hstring(const hstring&) = delete;
    ~hstring();

    operator HSTRING();
    hstring& operator=(hstring) = delete;

private:
    HSTRING hstr_;
};

/*
 * The exception thrown by the RO_CHECKED macro, indicating that a Windows Runtime API call has failed.
 * The exception message contains the system message text for the failed HRESULT.
 */
class RoException
{
public:
    RoException(const char* message);
    RoException(const char* message, HRESULT);
    RoException(const RoException&);
    ~RoException();

    RoException& operator=(RoException);
    const char* message() const;

private:
    const char* message_;
};
