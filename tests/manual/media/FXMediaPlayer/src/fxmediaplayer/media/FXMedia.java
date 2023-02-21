/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package fxmediaplayer.media;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;
import java.util.stream.Stream;
import java.nio.file.Files;
import java.util.function.Predicate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.NoSuchFileException;

/**
 * <p>Helper class to generate playlist from embeded media files located under
 * src/fxmediaplayer/media. This is useful to test "file", "jar" and "jrt" protocol.
 * Copy supported media files under src/fxmediaplayer/media,
 * compile "FXMediaPlayer", run it and go to "Play List" tab and you should see
 * your embeded media files.</p><p>
 *
 * FILE protocol:
 * <br>
 * cd rt/tests/manual/media/FXMediaPlayer
 * <br>
 * and run
 * </p><p>
 *
 * JAR protocol:
 * <br>
 * cd rt/tests/manual/media/FXMediaPlayer
 * <br>
 * ant
 * <br>
 * java @../../../../build/run.args -jar dist/FXMediaPlayer.jar
 * </p><p>
 *
 * JRT protocol:
 * <br>
 * cd rt/tests/manual/media/FXMediaPlayer
 * <br>
 * ant
 * <br><br>
 * [macOS/Linux]<br>
 * jlink --output dist/FXMediaPlayer -p ../../../../build/jmods:dist --add-modules javafx.controls,javafx.media,FXMediaPlayer --launcher FXMediaPlayer=FXMediaPlayer/fxmediaplayer.FXMediaPlayer
 * <br>
 * ./dist/FXMediaPlayer/bin/FXMediaPlayer
 * <br><br>
 * [Windows]<br>
 * jlink --output dist/FXMediaPlayer -p ../../../../build/jmods;dist --add-modules javafx.controls,javafx.media,FXMediaPlayer --launcher FXMediaPlayer=FXMediaPlayer/fxmediaplayer.FXMediaPlayer
 * <br>
 * dist\FXMediaPlayer\bin\FXMediaPlayer.bat
 * </p>
 */
public class FXMedia {

    private static List<String> SUPPORTED_EXT = new ArrayList<>();
    private static boolean isJRT = true;

    static {
        SUPPORTED_EXT.add("mp3");
        SUPPORTED_EXT.add("wav");
        SUPPORTED_EXT.add("aif");
        SUPPORTED_EXT.add("mp4");
        SUPPORTED_EXT.add("m4a");
        SUPPORTED_EXT.add("m4v");
    }

    public static List<String> getEmbededMediaFiles() {
        List<String> sources = new ArrayList<>();

        try {
            Path path = null;
            URI uri = null;
            Stream<Path> walk = null;
            try {
                FileSystem fileSystem = FileSystems.getFileSystem(URI.create("jrt:/"));
                path = fileSystem.getPath("modules", "FXMediaPlayer",
                        "fxmediaplayer", "media");
                walk = Files.walk(path, 1);
            } catch (NoSuchFileException ex) {
                isJRT = false;
                uri = FXMedia.class.getResource("/fxmediaplayer/media").toURI();

                if (uri.getScheme().equals("jar")) {
                    FileSystem fileSystem = FileSystems.newFileSystem(uri,
                            Collections.<String, Object>emptyMap());
                    path = fileSystem.getPath("fxmediaplayer", "media");
                } else {
                    path = Path.of(uri);
                }
                walk = Files.walk(path, 1);
            }

            Predicate<Path> predicate = new Predicate<Path>() {
                @Override
                public boolean test(Path p) {
                    final AtomicBoolean isSupported = new AtomicBoolean(false);
                    SUPPORTED_EXT.stream().forEach(ext -> {
                        if (p.getFileName().toString().endsWith(ext)) {
                            isSupported.set(true);
                        }
                    });

                    return isSupported.get();
                }
            };

            final URI uri2 = uri;
            walk.filter(predicate).forEach(p -> {
                if (isJRT()) {
                    sources.add("jrt:///FXMediaPlayer/fxmediaplayer/media" +
                        "/" + p.getFileName());
                } else {
                    sources.add(uri2.toString() + "/" + p.getFileName());
                }
            });
        } catch (URISyntaxException | IOException ex) {
            System.err.println("Exception: " + ex);
        }

        return sources;
    }

    private static boolean isJRT() {
        return isJRT;
    }

}
