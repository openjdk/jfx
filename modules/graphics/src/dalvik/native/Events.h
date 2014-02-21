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
#ifndef EVENTS_H
#define EVENTS_H

#include <sys/types.h>
#include <stdlib.h>

#define JFX_SIGNAL_EVENT          1    
    
#define JFX_SIGNAL_STARTUP        2
#define JFX_SIGNAL_SHUTDOWN       3    
#define JFX_SIGNAL_SHOW_IME       4
#define JFX_SIGNAL_HIDE_IME       5    


#ifdef __cplusplus
extern "C" {
#endif

typedef uint16_t EventType;
typedef uint16_t SignalEventType;

typedef struct Event {
    EventType     event;
    struct Event  *prev;
    struct Event  *next;
    char *(*toString)(struct Event *);
} _Event;
typedef _Event *Event;

typedef struct {
    EventType       event;
    struct Event    *prev;
    struct Event    *next;
    char *(*toString)(struct Event *);
    SignalEventType type;
}_SignalEvent;
typedef _SignalEvent *SignalEvent;

char *event_toString(Event e);
SignalEvent createSignalEvent(SignalEventType type);

#ifdef __cplusplus
}
#endif

#endif /* EVENTS_H */    
