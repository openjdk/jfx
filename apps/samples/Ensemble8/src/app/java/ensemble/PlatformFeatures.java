/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ensemble;


import javafx.application.ConditionalFeature;
import javafx.application.Platform;


public class PlatformFeatures {
    private static final String os = System.getProperty("os.name");
    private static final String arch = System.getProperty("os.arch");

    private static final boolean WINDOWS = os.startsWith("Windows");
    private static final boolean MAC = os.startsWith("Mac");
    private static final boolean LINUX = os.startsWith("Linux");
    private static final boolean ANDROID = "android".equals(System.getProperty("javafx.platform")) || "Dalvik".equals(System.getProperty("java.vm.name"));
    private static final boolean IOS = os.startsWith("iOS");
    private static final boolean EMBEDDED = "arm".equals(arch) && !IOS && !ANDROID;

    public static final boolean SUPPORTS_BENDING_PAGES = !EMBEDDED;
    public static final boolean HAS_HELVETICA = MAC || IOS;
    public static final boolean USE_IOS_THEME = IOS;
    public static final boolean START_FULL_SCREEN = EMBEDDED || IOS || ANDROID;
    public static final boolean LINK_TO_SOURCE = !(EMBEDDED || IOS || ANDROID);
    public static final boolean DISPLAY_PLAYGROUND = !(EMBEDDED || IOS || ANDROID);
    public static final boolean USE_EMBEDDED_FILTER = EMBEDDED || IOS || ANDROID;
    public static final boolean WEB_SUPPORTED = Platform.isSupported(ConditionalFeature.WEB);

    private PlatformFeatures(){}
}
