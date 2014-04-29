/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
#include "Events.h"
#include <stdio.h>
#include "logging.h"

static char *SIGNAL_STARTUP  = "Signal Event: startup";
static char *SIGNAL_SHUTDOWN = "Signal Event: shutdown";
static char *SIGNAL_SHOW_IME = "Signal Event: show IME";
static char *SIGNAL_HIDE_IME = "Signal Event: hide IME";
static char *SIGNAL_UNKNOWN  = "Signal Event: unknown";
static char *EVENT_UNKNOWN   = "Event unknown";

char *SignalEvent_toString(Event e) {
    SignalEvent sevent = (SignalEvent)e;
    if (sevent->type == JFX_SIGNAL_STARTUP) {
        return SIGNAL_STARTUP;
    } else if (sevent->type == JFX_SIGNAL_SHUTDOWN) {
        return SIGNAL_SHUTDOWN;
    } else if (sevent->type == JFX_SIGNAL_SHOW_IME) {
        return SIGNAL_SHOW_IME;
    } else if (sevent->type == JFX_SIGNAL_HIDE_IME) {
        return SIGNAL_HIDE_IME;
    }
    return SIGNAL_UNKNOWN;
}

SignalEvent createSignalEvent(SignalEventType type) {
    SignalEvent signal_event = (SignalEvent)malloc(sizeof(_SignalEvent));
    signal_event->event = JFX_SIGNAL_EVENT;
    signal_event->prev = 0;
    signal_event->next = 0;
    signal_event->type = type;
    signal_event->toString = &SignalEvent_toString;
    return signal_event;
}

char *event_toString(Event e) {
    char *out = (char*)malloc(100);
    sprintf(out, "[%s]", e->toString(e));
    return out;    
}

