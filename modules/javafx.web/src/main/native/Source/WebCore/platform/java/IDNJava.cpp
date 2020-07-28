/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "IDNJava.h"
#include <wtf/java/JavaEnv.h>
#include "com_sun_webkit_network_URLLoaderBase.h"

namespace IDNJavaInternal {

static JGClass idnClass;
static jmethodID toASCIIMID;

static void initRefs(JNIEnv* env)
{
    if (!idnClass) {
        idnClass = JLClass(env->FindClass("java/net/IDN"));
        ASSERT(idnClass);

        toASCIIMID = env->GetStaticMethodID(
                idnClass,
                "toASCII",
                "(Ljava/lang/String;I)Ljava/lang/String;");
        ASSERT(toASCIIMID);
    }
}
}

namespace WebCore {

namespace IDNJava {

String toASCII(const String& hostname)
{
    using namespace IDNJavaInternal;
    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    JLString result = static_cast<jstring>(env->CallStaticObjectMethod(
            idnClass,
            toASCIIMID,
            (jstring)hostname.toJavaString(env),
            com_sun_webkit_network_URLLoaderBase_ALLOW_UNASSIGNED));
    WTF::CheckAndClearException(env);
    return String(env, result);
}

} // namespace IDNJava

} // namespace WebCore
