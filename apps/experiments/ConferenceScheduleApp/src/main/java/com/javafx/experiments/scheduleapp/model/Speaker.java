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
import java.util.List;

/**
 * Model object for conference speaker
 */
public class Speaker {
    public final int id;
    public final String firstName;
    public final String lastName;
    private final String fullName;
    private final String jobTitle;
    private final String company;
    private final String bio;
    private final String imageUrl;
    private final String twitter;
    private final boolean rockStar;
    private final List<Session> sessions = new ArrayList<Session>();

    public Speaker(String fullName, String jobTitle, String company, String bio) {
        this(null,null,fullName, jobTitle, company, bio, null,null, false);
    }

    public Speaker(String firstName, String lastName, String fullName, String jobTitle, String company, String bio, String imageUrl, String twitter,boolean rockStar) {
        this(-1, firstName,lastName,fullName,jobTitle,company,bio,imageUrl,twitter,rockStar);
    }
    
    public Speaker(int id, String firstName, String lastName, String fullName, String jobTitle, String company, String bio, String imageUrl, String twitter,boolean rockStar) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.jobTitle = jobTitle;
        this.company = company;
        this.bio = bio;
        this.imageUrl = imageUrl;
        this.twitter = twitter;
        this.rockStar = rockStar;
    }

    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getCompany() {
        return company;
    }

    public String getBio() {
        return bio;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getTwitter() {
        return twitter;
    }

    public boolean isRockStar() {
        return rockStar;
    }

    public List<Session> getSessions() {
        return sessions;
    }
    
    @Override public String toString() {
        return fullName;
    }
}
