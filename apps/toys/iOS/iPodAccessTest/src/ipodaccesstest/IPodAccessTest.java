/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package ipodaccesstest;

import javafx.application.Application;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.media.MediaException;

import java.util.Iterator;
import java.util.List;
import java.util.Calendar;

import java.text.SimpleDateFormat;
import javafx.scene.control.*;

import com.sun.javafx.ext.device.ios.ipod.MediaQuery;
import com.sun.javafx.ext.device.ios.ipod.MediaItem;
import com.sun.javafx.ext.device.ios.ipod.MediaFilter;


public class IPodAccessTest extends Application {
    private Scene scene;
    private TextArea ta;

    @Override
    public void start(final Stage stage) {
        stage.setTitle("iPodAccessTest");

        scene = new Scene(new Group());
        stage.setHeight(1004);
        stage.setWidth(768);
        scene.setFill(Color.SKYBLUE);

        ta = new TextArea("");
        ScrollPane sp = new ScrollPane();
        sp.setContent(ta);
        ta.setMinSize(768, 1004);
        ta.setEditable(false);

        ((Group)scene.getRoot()).getChildren().addAll(sp);

        stage.setScene(scene);
        stage.show();

        try {
            testIPodAccess();
            testIPodAccessWithGrouping();
        }
        catch (MediaException e) {
            addText("Media exception caught:" + e);
            e.printStackTrace();
        }
    }

    void addText(final String msg) {
        ta.textProperty().setValue(ta.textProperty().getValue() + msg + "\n");
    }

    private void printMediaItemProperties(final MediaItem item) {
        addText("Item: " + item);
        addText("      media type :        " + item.getMediaType());
        addText("      title:              " + item.getTitle());
        addText("      album title:        " + item.getAlbumTitle());
        addText("      artist:             " + item.getArtist());
        addText("      album artist:       " + item.getAlbumArtist());
        addText("      genre:              " + item.getGenre());
        addText("      composer:           " + item.getComposer());
        addText("      playback duration:  " + item.getPlaybackDuration());
        addText("      album track number: " + item.getAlbumTrackNumber());
        addText("      album track count:  " + item.getAlbumTrackCount());
        addText("      disc number:        " + item.getDiscNumber());
        addText("      disc count:         " + item.getDiscCount());
        // missing artwork
        addText("      lyrics:             " + item.getLyrics());
        addText("      is compilation ?    " + item.isCompilation());

        final Calendar date = item.getReleaseDate();
        if (date != null) {
            final SimpleDateFormat df = new SimpleDateFormat();
            df.applyPattern("dd/MM/yyyy");
            addText("      release date:       " + df.format(date.getTime()));
        } else {
            addText("      release date:       N/A");
        }
        addText("      beats per minute:   " + item.getBeatsPerMinute());
        addText("      comments:           " + item.getComments());
        addText("      url:                " + item.getURL());
    }

    private void testIPodAccess() {
        addText("Testing iPod library access extension API");
        final MediaQuery mediaQuery = new MediaQuery();

        addText("Retrieving media items");
        final List<MediaItem> items = mediaQuery.getItems();

        addText("Enumerating " + items.size() + " media items...");
        for (final MediaItem item : items) {
            printMediaItemProperties(item);
        }
    }

    private void testIPodAccessWithGrouping() {
        addText("Testing iPod library access extension");
        final MediaQuery mediaQuery = new MediaQuery();

        addText("Adding a filter (Artist must be \"RingtoneFeeder.com\")");
        final MediaFilter filter = new MediaFilter(MediaFilter.MediaFilterType.Artist, "RingtoneFeeder.com");
        mediaQuery.addFilter(filter);

        addText("Setting grouping type");
        mediaQuery.setGroupingType(MediaQuery.MediaGroupingType.AlbumArtist);

        addText("Retrieving media items collections");
        final List<List<MediaItem>> collections = mediaQuery.getCollections();

        addText("There are " + collections.size() + " collections");

        int i=0;
        for (final List<MediaItem> collection : collections) {
            addText("Collection #" + i++);
            final Iterator<MediaItem> iterator = collection.iterator();
            while (iterator.hasNext()) {
                final MediaItem item = iterator.next();
                printMediaItemProperties(item);
            }
        }
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
