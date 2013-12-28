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
package com.javafx.experiments.scheduleapp.data.devoxx;

import com.javafx.experiments.scheduleapp.data.JSONParserJP;
import com.javafx.experiments.scheduleapp.model.Level;
import com.javafx.experiments.scheduleapp.model.Room;
import com.javafx.experiments.scheduleapp.model.Session;
import com.javafx.experiments.scheduleapp.model.SessionTime;
import com.javafx.experiments.scheduleapp.model.SessionType;
import com.javafx.experiments.scheduleapp.model.Speaker;
import com.javafx.experiments.scheduleapp.model.Track;
import com.javafx.experiments.scheduleapp.model.Venue;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.concurrent.Task;

class GetConferenceDataTask extends Task<Void> {
    private static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static String[] TRACK_COLORS = new String[] {
          "#00a8cc","#ea0069","#ffd900","#727272","#8ed700","#ff6600","#00a695","#8c7ccf","#666666"
    };
    private List<Speaker> sortedSpeakers = new ArrayList<>();
    private Map<Integer,Speaker> speakerMap = new HashMap<>();
    private Map<Integer,Session> sessionMap = new HashMap<>();
    private Map<String,Level> levelMap = new HashMap<>();
    private Map<String,Track> trackMap = new HashMap<>();
    private Map<String,SessionType> typeMap = new HashMap<>();
    private Map<String,Room> roomMap = new HashMap<>();
    private Map<Integer,List<SessionTime>> sessionTimesMap = new HashMap<>();
    private Map<Date,List<Session>> sessionPerTimeSlot = new HashMap<>();
    private final Venue venue;

    GetConferenceDataTask(Venue venue) {
        this.venue = venue;
        
        trackMap.put("New Languages on the JVM", 
            new Track("LANG","New Languages on the JVM",
                "Languages that are build on top of the JVM.", 
                TRACK_COLORS[0]));
        trackMap.put("Methodology", 
            new Track("METH","Methodology",
                "Methodology and anything related.", 
                TRACK_COLORS[1]));
        trackMap.put("Java SE", 
            new Track("JSE","Java SE",
                "Everything Java SE and JavaFX related.", 
                TRACK_COLORS[2]));
        trackMap.put("Mobile", 
            new Track("MOBI","Mobile",
                "This includes Android and mobile web.", 
                TRACK_COLORS[3]));
        trackMap.put("Architecture, Cloud and Security", 
            new Track("CLOU","Architecture, Cloud and Security",
                "Architecture, Cloud and Security", 
                TRACK_COLORS[4]));
        trackMap.put("Java EE", 
            new Track("JEE","Java EE",
                "All Java all Enterprise.", 
                TRACK_COLORS[5]));
        trackMap.put("Web", 
            new Track("WEB","Web",
                "This includes HTML5, CSS3 and RIA technologies", 
                TRACK_COLORS[6]));
        trackMap.put("Future<Devoxx>", 
            new Track("LANG","Future<Devoxx>",
                "Micro Controllers (Arduino, Dwengo, Raspberry Pi, ...), Robots, Automotive, Domotica, etc.", 
                TRACK_COLORS[7]));
    }

    @Override protected Void call() throws Exception {
        // GET ALL SPEAKERS FIRST
        JSONParserJP.parse(" https://cfp.devoxx.com/rest/v1/events/7/speakers",new SpeakerCallcack());
        System.out.println("----------------------------------------");
        System.out.println("SPEAKERS size = "+speakerMap.size());
        System.out.println("SPEAKERS = "+Arrays.toString(speakerMap.values().toArray()));
        sortedSpeakers.addAll(speakerMap.values());
        Collections.sort(sortedSpeakers, new Comparator<Speaker>() {
            @Override public int compare(Speaker o1, Speaker o2) {
                return o1.getFirstName().equals(o2.getFirstName()) ?
                        o1.getLastName().compareTo(o2.getLastName()):
                        o1.getFirstName().compareTo(o2.getFirstName());
            }
        });
        System.out.println("SORTED SPEAKERS = "+Arrays.toString(sortedSpeakers.toArray()));
        
        // THEN GET ALL SESSION TIMES
        System.out.println("----------------------------------------");
        try{
            JSONParserJP.parse("https://cfp.devoxx.com/rest/v1/events/7/schedule ",new SessionTimeCallcack());
        } catch (Exception e) { e.printStackTrace(); }
        System.out.println("----------------------------------------");
        System.out.println("SESSIONS we have times for SIZE = "+sessionTimesMap.size());
        
        // THEN GET ALL SESSIONS
        System.out.println("----------------------------------------");
        try{
            JSONParserJP.parse("https://cfp.devoxx.com/rest/v1/events/7/presentations",new SessionCallcack());
        } catch (Exception e) { e.printStackTrace(); }
        System.out.println("----------------------------------------");
        System.out.println("SESSIONS SIZE = "+sessionMap.size());
        return null;
    }

    public Map<String, Room> getRoomMap() {
        return roomMap;
    }

    public Map<Integer, List<SessionTime>> getSessionTimesMap() {
        return sessionTimesMap;
    }

    public Map<Date, List<Session>> getSessionPerTimeSlot() {
        return sessionPerTimeSlot;
    }

    public List<Speaker> getSortedSpeakers() {
        return sortedSpeakers;
    }

    public Map<Integer, Speaker> getSpeakerMap() {
        return speakerMap;
    }

    public Map<Integer, Session> getSessionMap() {
        return sessionMap;
    }

    public Map<String, Level> getLevelMap() {
        return levelMap;
    }

    public Map<String, Track> getTrackMap() {
        return trackMap;
    }

    public Map<String, SessionType> getTypeMap() {
        return typeMap;
    }
    
    private class SpeakerCallcack extends JSONParserJP.CallbackAdapter {
        private int id;
        private String firstName;
        private String lastName;
        private String company;
        private String bio;
        private String imageUrl;
        private String twitter;
        private boolean rockStar = false;
        
        @Override public void keyValue(String key, String value, int depth) {
            if(depth == 2) {
                if ("id".equals(key)) {
                    id = Integer.parseInt(value);
                } else if ("lastName".equals(key)) {
                    lastName = value;
                } else if ("firstName".equals(key)) {
                    firstName = value;
                } else if ("bio".equals(key)) {
                    bio = value;
                } else if ("company".equals(key)) {
                    company = value;
                } else if ("imageURI".equals(key)) {
                    imageUrl = value;
                    // there is a bug that we can not follow redirects, so switch to https up front. http://javafx-jira.kenai.com/browse/RT-25739
                    if (imageUrl.startsWith("http:")) imageUrl = "https" + imageUrl.substring(4);
                } else if ("tweethandle".equals(key)) {
                    twitter = value;
                }
            }
        }
        @Override public void endObject(String objectName, int depth) {
            if(depth == 1) {
                final Speaker speaker = new Speaker(id,firstName,lastName,firstName+" "+lastName,null,company,bio,imageUrl,twitter,rockStar);
//                System.out.println("Speaker ==> ["+id+"] "+firstName+" "+lastName+"  url=["+imageUrl+"]");
                speakerMap.put(id,speaker);
            }
        }
    }
    
    private class SessionCallcack extends JSONParserJP.CallbackAdapter {
        private List<Session> sessions = null;
        public int id;
        private String abbreviation;
        private String title;
        private String summary;
        private Level level;
        private List<Speaker> speakers = new ArrayList<Speaker>();
        private SessionType sessionType;
        private Track track;

        @Override public void keyValue(String key, String value, int depth) {
            if(depth == 2) {
                if ("id".equals(key)) {
                    id = Integer.parseInt(value);
                } else if ("summary".equals(key)) {
                    summary = value;
                } else if ("track".equals(key)) {
                    track = trackMap.get(value);
                    if (track == null) {
                        final String color = TRACK_COLORS[trackMap.size() % TRACK_COLORS.length];
                        track = new Track(value,value,"",color);
                        trackMap.put(value, track);
                    }
                } else if ("title".equals(key)) {
                    title = value;
                } else if ("experience".equals(key)) {
                    level = levelMap.get(value);
                    if (level == null) {
                        level = new Level(value);
                        levelMap.put(value, level);
                    }
                } else if ("type".equals(key)) {
                    sessionType = typeMap.get(value);
                    if (sessionType == null) {
                        sessionType = new SessionType(value);
                        typeMap.put(value, sessionType);
                    }
                }
            } else if (depth == 4 && "speakerId".equals(key)) {
                speakers.add(speakerMap.get(Integer.parseInt(value)));
            }
        }
        
        @Override public void endObject(String objectName, int depth) {
            if(depth == 1) {
                List<SessionTime> sessionTimes = sessionTimesMap.get(id);
                if (sessionTimes == null || sessionTimes.isEmpty()) {
                    System.err.println("Error: not found any session times for session ["+id+"] \""+title+"\"");
                    return;
                }
                final Session session = new Session(id,"DX"+id,title,summary,level,
                        speakers.toArray(new Speaker[speakers.size()]),
                        sessionTimes.toArray(new SessionTime[sessionTimes.size()]),
                        sessionType,track);
                sessionMap.put(id,session);
                // store session per start time
                for (SessionTime sessionTime: sessionTimes) {
                    List<Session> sessionsPerTime = sessionPerTimeSlot.get(sessionTime.getStart());
                    if (sessionsPerTime == null) {
                        sessionsPerTime = new ArrayList<>();
                        sessionPerTimeSlot.put(sessionTime.getStart(), sessionsPerTime);
                    }
                    sessionsPerTime.add(session);
                }
                // add session to speakers sessions
                for (Speaker speaker:speakers) {
                    speaker.getSessions().add(session);
                }
//                System.out.println("\n\nsession = " + session);
            }
        }

        @Override public void startObject(String objectName, int depth) {
            if(depth == 1) {
                speakers.clear();
            }
        }
    }
    
    private class SessionTimeCallcack extends JSONParserJP.CallbackAdapter {
        private List<Session> sessions = null;
        public int id;
        private Date start,end;
        private int length;
        private Room room;
        private int sessionId;

        @Override public void keyValue(String key, String value, int depth) {
            if(depth == 2) {
                if ("id".equals(key)) {
                    id = Integer.parseInt(value);
                } else if ("presentationUri".equals(key)) {
                    //summary = value;
                    int lastSlash = value.lastIndexOf('/');
                    sessionId = Integer.parseInt(value.substring(lastSlash+1, value.length()));
                } else if ("fromTime".equals(key)) {
                    try {
                        start = DATE_FORMAT.parse(value);
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                } else if ("toTime".equals(key)) {
                    try {
                        end = DATE_FORMAT.parse(value);
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                } else if ("room".equals(key)) {
                    room = roomMap.get(value);
                    if (room == null) {
                        room = new Room(venue,value);
                        roomMap.put(value, room);
                    }
                }
            }
        }
        
        @Override public void endObject(String objectName, int depth) {
            if(depth == 1) {
                length = (int)((end.getTime()/60000) - (start.getTime()/60000));
                final SessionTime sessionTime = new SessionTime(id, start, length, room, -1, -1);
                // store per session
                List<SessionTime> sessionTimes = sessionTimesMap.get(sessionId);
                if (sessionTimes == null) {
                    sessionTimes = new ArrayList<>();
                    sessionTimesMap.put(sessionId, sessionTimes);
                }
                sessionTimes.add(sessionTime);
            }
        }
    }
}
