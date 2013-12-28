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
package com.javafx.experiments.scheduleapp;

import static com.javafx.experiments.scheduleapp.ConferenceScheduleApp.*;
import com.javafx.experiments.scheduleapp.data.TwitterJson;
import com.javafx.experiments.scheduleapp.model.Event;
import com.javafx.experiments.scheduleapp.model.Session;
import com.javafx.experiments.scheduleapp.model.Tweet;
import com.javafx.experiments.scheduleapp.pages.SocialPage;
import java.awt.Desktop;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Callback;

/**
 * class giving access to platform integration
 */
public class PlatformIntegration {
    
    /**
     * Check if platform supports opening URLs in the system browser
     * 
     * @return true if system can open URLs
     */
    public static boolean supportsSystemCalendar() {
        return false; 
    }
    
    /**
     * Read all events from the system calendar between the given start and 
     * end dates.
     * 
     * @param start The start of range to find events from
     * @param end   The end of range to find events to
     * @return List of all events found
     */
    public static List<Event> readEvents(List<Session> sessions, Date start, Date end) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Write the given list of events into the system calendar.
     * 
     * @param events List of events to save into calendar
     */
    public static void writeEvents(List<Event> events) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Delete the given list of events from the system calendar.
     * 
     * @param events List of events to delete
     */
    public static void deleteEvents(List<Event> events) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Check if platform supports opening URLs in the system browser
     * 
     * @return true if system can open URLs
     */
    public static boolean supportsOpeningUrls() {
        return IS_MAC || IS_WINDOWS; 
    }
    
    /**
     * Open the given URL in the system web browser.
     * 
     * @param url 
     */
    public static void openUrl(String url) {
        if (IS_MAC) {
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"open",url});
            } catch (Exception ex) {
                Logger.getLogger(SocialPage.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if(IS_WINDOWS) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                Logger.getLogger(SocialPage.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * Get all tweets returned by the twitter API call with the given URL.
     * 
     * @param url
     * @param gotTweetCallBack 
     */
    public static void getTweetsForQuery(String url, final Callback<Tweet[],Void> gotTweetCallBack) {
        TwitterJson.getTweetsForQuery(url, gotTweetCallBack);
    }
    
    /**
     * Check if platform supports opening URLs in the system browser
     * 
     * @return true if system can open URLs
     */
    public static boolean supportsSendingTweets() {
        return false; 
    }
    
    /**
     * Open a platform native send tweet dialog with the given initial text.
     * 
     * @param text The initial text for tweet
     */
    public static void tweet(String text) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Check if platform supports showing map routes
     * 
     * @return true if system can show maps
     */
    public static boolean supportsMapRouting() {
        return false; 
    }
    
    /**
     * Open system maps application and show route from current location to the
     * given address.
     * 
     * @param address The address to show route to
     */
    public static void openMapRoute(final String address) {
        throw new UnsupportedOperationException();
    }
}
