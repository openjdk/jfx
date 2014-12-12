/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package launchertest;

public class Constants {

    // Error exit codes. Note that 0 and 1 are reserved for normal exit and
    // failure to launch java, respectively
    static final int ERROR_NONE = 2;

    static final int ERROR_TOOLKIT_NOT_RUNNING = 3;
    static final int ERROR_UNEXPECTED_EXCEPTION = 4;
    static final int ERROR_TOOLKIT_IS_RUNNING = 5;

    static final int ERROR_INIT_BEFORE_MAIN = 6;
    static final int ERROR_START_BEFORE_MAIN = 7;
    static final int ERROR_STOP_BEFORE_MAIN = 8;

    static final int ERROR_START_BEFORE_INIT = 9;
    static final int ERROR_STOP_BEFORE_INIT = 10;

    static final int ERROR_STOP_BEFORE_START = 11;

    static final int ERROR_CLASS_INIT_WRONG_THREAD = 12;
    static final int ERROR_MAIN_WRONG_THREAD = 13;
    static final int ERROR_CONSTRUCTOR_WRONG_THREAD = 14;
    static final int ERROR_INIT_WRONG_THREAD = 15;
    static final int ERROR_START_WRONG_THREAD = 16;
    static final int ERROR_STOP_WRONG_THREAD = 17;

    static final int ERROR_PRELOADER_CLASS_INIT_WRONG_THREAD = 18;
    static final int ERROR_PRELOADER_CONSTRUCTOR_WRONG_THREAD = 19;
    static final int ERROR_PRELOADER_INIT_WRONG_THREAD = 20;
    static final int ERROR_PRELOADER_START_WRONG_THREAD = 21;
    static final int ERROR_PRELOADER_STOP_WRONG_THREAD = 22;

    static final int ERROR_LAUNCH_SUCCEEDED = 23;

    static final int ERROR_CONSTRUCTOR_WRONG_CCL = 24;
    static final int ERROR_START_WRONG_CCL = 25;
}
