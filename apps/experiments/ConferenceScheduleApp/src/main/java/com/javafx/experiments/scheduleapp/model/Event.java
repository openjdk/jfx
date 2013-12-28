/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates.
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Model object for a Calendar Event, this may have come from the users Calendar 
 * or be one we created for a session they want to attend.
 */
public class Event {
    private static final DateFormat DATE_FORMATTER = SimpleDateFormat.getDateTimeInstance();
    private final String title;
    private final String location;
    private final String organizer;
    private final String[] attendees;
    private final Date start;
    private final Date end;
    private final Session session;
    private final SessionTime sessionTime;

    public Event(Session session, Date start, Date end, SessionTime sessionTime) {
        this.title = null;
        this.location = null;
        this.organizer = null;
        this.attendees = null;
        this.start = start;
        this.end = end;
        this.session = session;
        this.sessionTime = sessionTime;
    }

    public Event(String title, String location, String organizer, String[] attendees, Date start, Date end) {
        this.title = title;
        this.location = location;
        this.organizer = organizer;
        this.attendees = attendees;
        this.start = start;
        this.end = end;
        this.session = null;
        this.sessionTime = null;
    }

    public String getTitle() {
        return title;
    }

    public String getLocation() {
        return location;
    }
    
    public String getOrganizer() {
        return organizer;
    }

    public String[] getAttendees() {
        return attendees;
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public Session getSession() {
        return session;
    }
    
    public SessionTime getSessionTime() {
//        if (session == null) return null;
//        final SessionTime[] sessionTimes = session.getSessionTimes();
//        for (int i=0; i<sessionTimes.length; i++) {
//            if (sessionTimes[i].getStart().getTime() == start.getTime()) return sessionTimes[i];
//        }
//        return null;
        return sessionTime;
    }

    @Override public String toString() {
        return "Event{" + "title=" + title + 
                ", location=" + location + 
                ", organizer=" + organizer + 
                ", attendees=" + Arrays.toString(attendees) + 
                ", start=" + DATE_FORMATTER.format(start) + 
                ", end=" + DATE_FORMATTER.format(end) + 
                ", session=" + session + '}';
    }
}
