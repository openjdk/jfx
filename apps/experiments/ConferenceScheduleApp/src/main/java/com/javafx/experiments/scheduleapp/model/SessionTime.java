/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates.
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
package com.javafx.experiments.scheduleapp.model;

import java.util.Date;

/**
 * A model object for single scheduled presentation of a session
 */
public final class SessionTime {
    private final int id;
    private final Date start;
    /** Length of session in minutes */
    private final int length;
    private final Room room;
    private final int capacity;
    private final int registered;
    /** This is null if not in users schedule otherwise its the Event that 
     * is in their schedule. */
    private Event event;
    private Session session;

    public SessionTime(int id, Date start, int length, Room room, int capacity, int registered) {
        this.id = id;
        this.start = start;
        this.length = length;
        this.room = room;
        this.capacity = capacity;
        this.registered = registered;
    }
    
    public int getId() {
        return id;
    }
    
    public Date getStart() {
        return start;
    }

    public int getLength() {
        return length;
    }

    public Room getRoom() {
        return room;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getRegistered() {
        return registered;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Session getSession() {
        return session;
    }

    void setSession(Session session) {
        this.session = session;
    }

    public String debugString() {
        return start+ "(" + length + "min) " + room + " (" + registered + "/" + capacity + ')';
    }
    
    @Override public String toString() {
        return session.getTitle();
    }
}
