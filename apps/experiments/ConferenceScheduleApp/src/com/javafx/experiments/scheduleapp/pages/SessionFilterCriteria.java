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
package com.javafx.experiments.scheduleapp.pages;

import com.javafx.experiments.scheduleapp.model.SessionType;
import com.javafx.experiments.scheduleapp.model.Track;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Filters a List of Session instances
 * Contains the criteria by which the CatalogPage will filter the sessions.
 */
public final class SessionFilterCriteria {
    public static final SessionFilterCriteria EMPTY = new SessionFilterCriteria(
            "", Collections.<Track>emptySet(), Collections.<SessionType>emptySet());

    private final Set<Track> tracks;
    private final Set<SessionType> sessionTypes;
    private final String text;

    public SessionFilterCriteria(String text, Collection<Track> tracks, Collection<SessionType> sessionTypes) {
        this.text = text;
        this.tracks = Collections.unmodifiableSet(new HashSet<Track>(tracks));
        this.sessionTypes = Collections.unmodifiableSet(new HashSet<SessionType>(sessionTypes));
    }

    private SessionFilterCriteria(String text, Set<Track> tracks, Set<SessionType> sessionTypes) {
        this.text = text;
        this.tracks = tracks;
        this.sessionTypes = sessionTypes;
    }

    public final String getText() {
        return text;
    }

    public final Set<Track> getTracks() {
        return tracks;
    }

    public final Set<SessionType> getSessionTypes() {
        return sessionTypes;
    }

    public static SessionFilterCriteria withText(String text, SessionFilterCriteria other) {
        return new SessionFilterCriteria(text, other.tracks, other.sessionTypes);
    }

    public static SessionFilterCriteria withTracks(Collection<Track> tracks, SessionFilterCriteria other) {
        return new SessionFilterCriteria(other.text,
                Collections.unmodifiableSet(new HashSet<Track>(tracks)), other.sessionTypes);
    }

    public static SessionFilterCriteria withSessionTypes(Collection<SessionType> sessionTypes, SessionFilterCriteria other) {
        return new SessionFilterCriteria(other.text, other.tracks,
                Collections.unmodifiableSet(new HashSet<SessionType>(sessionTypes)));
    }
}
