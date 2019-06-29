/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#if ENABLE(INPUT_TYPE_COLOR)
#include <WebCore/PlatformJavaClasses.h>
#include <WebCore/ColorChooser.h>

namespace WebCore {
class ColorChooserClient;

class ColorChooserJava final : public ColorChooser {
public:
    ColorChooserJava(JGObject&, ColorChooserClient*, const Color&);
    ColorChooserClient* getClient() { return m_colorChooserClient; }

    ~ColorChooserJava() override { }
    void reattachColorChooser(const Color&) override;
    void setSelectedColor(const Color&) override;
    void endChooser() override;

private:
    ColorChooserClient* m_colorChooserClient;
    JGObject m_colorChooserRef;
};

} // namespace WebCore

#endif  // #if ENABLE(INPUT_TYPE_COLOR)
