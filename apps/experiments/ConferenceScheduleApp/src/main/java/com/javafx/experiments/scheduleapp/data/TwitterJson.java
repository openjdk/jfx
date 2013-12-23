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
package com.javafx.experiments.scheduleapp.data;

import com.javafx.experiments.scheduleapp.model.Tweet;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Callback;

/**
 * Helper class for twitter
 */
public class TwitterJson implements JSONParserJP.Callback, Runnable {
    //Thu, 26 Jul 2012 16:29:52 +0000
    private static final DateFormat TWITTER_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZZ");
    
    private final String url;
    private final Callback<Tweet[],Void> gotTweetCallBack;
    private final ArrayList<Tweet> tweets = new ArrayList<Tweet>();
    
    private Date createdAt;
    private String fromUser;
    private String fromUserName;
    private String profileImageUrl;
    private String text;

    public TwitterJson(String url, Callback<Tweet[], Void> gotTweetCallBack) {
        this.url = url;
        this.gotTweetCallBack = gotTweetCallBack;
    }
    
    public static void getTweetsForQuery(String url, final Callback<Tweet[],Void> gotTweetCallBack) {
        new Thread(new TwitterJson(url, gotTweetCallBack)).start();
    }

    @Override public void run() {
        try {
            JSONParserJP.parse(url, this);
        } catch (IOException ex) {
            Logger.getLogger(TwitterJson.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override public void keyValue(String key, String value, int depth) {
        if (depth == 3) {
            if ("from_user".equals(key)) {
                fromUser = value;
            } else if ("from_user_name".equals(key)) {
                fromUserName = value;
            } else if ("profile_image_url".equals(key)) {
                profileImageUrl = value;
            } else if ("text".equals(key)) {
                text = value;
            } else if ("created_at".equals(key)) {
                try {
                    createdAt = TWITTER_DATE_FORMAT.parse(value);
                } catch (ParseException ex) {
                    Logger.getLogger(TwitterJson.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    @Override public void endObject(String objectName, int depth) {
        if (depth == 0) {
            gotTweetCallBack.call(tweets.toArray(new Tweet[tweets.size()]));
        } if (depth == 2) {
            tweets.add(new Tweet(createdAt, fromUser, fromUserName, profileImageUrl, text));
        }
    }

    @Override public boolean isCanceled() {
        return false;
    }

    @Override public void startObject(String objectName, int depth) {}
    @Override public void startArray(String arrayName, int depth) {}
    @Override public void endArray(String arrayName, int depth) {}
    @Override public void stringValue(String value, int depth) {}
    @Override public void numberValue(double value, int depth) {}
    @Override public void booleanValue(boolean value, int depth) {}
}
