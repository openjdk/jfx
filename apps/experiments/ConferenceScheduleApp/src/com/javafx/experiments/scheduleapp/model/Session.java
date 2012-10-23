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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A model object for a conference session
 */
public class Session {
    private final int id;
    private final String abbreviation;
    private final String title;
    private final String summary;
    private final Level level; // TODO could move this to enum maybe
    private final Speaker[] speakers;
    private final SessionTime[] schedules;
    private final SessionType sessionType;
    private final Track track;

    public Session(int id, String abbreviation, String title, String summary, Level level, Speaker[] speakers, SessionTime[] schedules, SessionType sessionType, Track track) {
        this.id = id;
        this.abbreviation = abbreviation;
        this.title = title;
        this.summary = summary;
        this.level = level;
        this.speakers = speakers;
        this.schedules = schedules;
        this.sessionType = sessionType;
        this.track = track;
        if (schedules!= null) {
            for(int i=0; i<schedules.length; i++) {
                schedules[i].setSession(this);
            }
        }
    }
    
    public int getId() {
        return id;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public Level getLevel() {
        return level;
    }

    public Speaker[] getSpeakers() {
        return speakers;
    }

    public SessionTime[] getSessionTimes() {
        return schedules;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public Track getTrack() {
        return track;
    }
    
    public String getSpeakersDisplay() {
        if (speakers == null || speakers.length == 0) {
            return "";
        } else if (speakers.length == 1) {
            return  speakers[0].toString();
        } else {
            String speakersStr = speakers[0].toString();
            for (int i=1; i< speakers.length; i++) {
                speakersStr += " + " + speakers[i];
            }
            return speakersStr;
        }
    }

    @Override public String toString() {
        List<String> times = new ArrayList<>();
        for(SessionTime sessionTime: schedules) {
            times.add(sessionTime.debugString());
        }
        return abbreviation + " : " + title + "\n" + summary + "\nlevel: " + level + "\nspeakers: " + Arrays.toString(speakers) + 
                "\ntimes: " + Arrays.toString(times.toArray()) + "\nsessionType: " + sessionType + "\ntrack: " + track;
    }
}
