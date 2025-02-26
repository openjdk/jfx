/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

#include <JavaScriptCore/InspectorFrontendChannel.h>
#include <WebCore/InspectorClient.h>
#include <WebCore/PlatformJavaClasses.h>

namespace WebCore {

class InspectorClientJava final
    : public InspectorClient,
      public Inspector::FrontendChannel
{
    WTF_MAKE_FAST_ALLOCATED;
public:
    InspectorClientJava(const JLObject &webPage);

    void inspectedPageDestroyed() override;

    Inspector::FrontendChannel* openLocalFrontend(InspectorController*) override;
    void bringFrontendToFront() override;

    void highlight() override;
    void hideHighlight() override;

    ConnectionType connectionType() const override { return Inspector::FrontendChannel::ConnectionType::Local; }
    void sendMessageToFrontend(const String& message) override;

private:
    JGObject m_webPage;
};

} // namespace WebCore
