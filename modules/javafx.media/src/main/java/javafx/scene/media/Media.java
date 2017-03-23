/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.media;

import com.sun.media.jfxmedia.MetadataParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.image.Image;
import javafx.util.Duration;

import com.sun.media.jfxmedia.locator.Locator;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import com.sun.media.jfxmedia.events.MetadataListener;
import com.sun.media.jfxmedia.track.VideoResolution;

/**
 * The <code>Media</code> class represents a media resource. It is instantiated
 * from the string form of a source URI. Information about the media such as
 * duration, metadata, tracks, and video resolution may be obtained from a
 * <code>Media</code> instance. The media information is obtained asynchronously
 * and so not necessarily available immediately after instantiation of the class.
 * All information should however be available if the instance has been
 * associated with a {@link MediaPlayer} and that player has transitioned to
 * {@link MediaPlayer.Status#READY} status. To be notified when metadata or
 * {@link Track}s are added, observers may be registered with the collections
 * returned by {@link #getMetadata()}and {@link #getTracks()}, respectively.
 *
 * <p>The same <code>Media</code> object may be shared among multiple
 * <code>MediaPlayer</code> objects. Such a shared instance might manage a single
 * copy of the source media data to be used by all players, or it might require a
 * separate copy of the data for each player. The choice of implementation will
 * not however have any effect on player behavior at the interface level.</p>
 *
 * @see MediaPlayer
 * @see MediaException
 * @since JavaFX 2.0
 */
public final class Media {
    /**
     * A property set to a MediaException value when an error occurs.
     * If <code>error</code> is non-<code>null</code>, then the media could not
     * be loaded and is not usable. If {@link #onErrorProperty onError} is non-<code>null</code>,
     * it will be invoked when the <code>error</code> property is set.
     *
     * @see MediaException
     */
    private ReadOnlyObjectWrapper<MediaException> error;

    private void setError(MediaException value) {
        if (getError() == null) {
            errorPropertyImpl().set(value);
        }
    }

    /**
     * Return any error encountered in the media.
     * @return a {@link MediaException} or <code>null</code> if there is no error.
     */
    public final MediaException getError() {
        return error == null ? null : error.get();
    }

    public ReadOnlyObjectProperty<MediaException> errorProperty() {
        return errorPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<MediaException> errorPropertyImpl() {
        if (error == null) {
            error = new ReadOnlyObjectWrapper<MediaException>() {

                @Override
                protected void invalidated() {
                    if (getOnError() != null) {
                        Platform.runLater(getOnError());
                    }
                }

                @Override
                public Object getBean() {
                    return Media.this;
                }

                @Override
                public String getName() {
                    return "error";
                }
            };
        }
        return error;
    }
    /**
     * Event handler called when an error occurs. This will happen
     * if a malformed or invalid URL is passed to the constructor or there is
     * a problem accessing the URL.
     */
    private ObjectProperty<Runnable> onError;

    /**
     * Set the event handler to be called when an error occurs.
     * @param value the error event handler.
     */
    public final void setOnError(Runnable value) {
        onErrorProperty().set(value);
    }

    /**
     * Retrieve the error handler to be called if an error occurs.
     * @return the error handler or <code>null</code> if none is defined.
     */
    public final Runnable getOnError() {
        return onError == null ? null : onError.get();
    }

    public ObjectProperty<Runnable> onErrorProperty() {
        if (onError == null) {
            onError = new ObjectPropertyBase<Runnable>() {

                @Override
                protected void invalidated() {
                    /*
                     * if we have an existing error condition schedule the handler to be
                     * called immediately. This way the client app does not have to perform
                     * an explicit error check.
                     */
                    if (get() != null && getError() != null) {
                        Platform.runLater(get());
                    }
                }

                @Override
                public Object getBean() {
                    return Media.this;
                }

                @Override
                public String getName() {
                    return "onError";
                }
            };
        }
        return onError;
    }

    private MetadataListener metadataListener = new _MetadataListener();

    /**
     * An {@link ObservableMap} of metadata which can contain information about
     * the media. Metadata entries use {@link String}s for keys and contain
     * {@link Object} values. This map is unmodifiable: its contents or stored
     * values cannot be changed.
     */
    // FIXME: define standard metadata keys and the corresponding objects types
    // FIXME: figure out how to make the entries read-only to observers, we'll
    //        need to enhance javafx.collections a bit to accomodate this
    private ObservableMap<String, Object> metadata;

    /**
     * Retrieve the metadata contained in this media source. If there are
     * no metadata, the returned {@link ObservableMap} will be empty.
     * @return the metadata contained in this media source.
     */
    public final ObservableMap<String, Object> getMetadata() {
        return metadata;
    }

    private final ObservableMap<String,Object> metadataBacking = FXCollections.observableMap(new HashMap<String,Object>());
    /**
     * The width in pixels of the source media.
     * This may be zero if the media has no width, e.g., when playing audio,
     * or if the width is currently unknown which may occur with streaming
     * media.
     * @see height
     */
    private ReadOnlyIntegerWrapper width;


    final void setWidth(int value) {
        widthPropertyImpl().set(value);
    }

    /**
     * Retrieve the width in pixels of the media.
     * @return the media width or zero if the width is undefined or unknown.
     */
    public final int getWidth() {
        return width == null ? 0 : width.get();
    }

    public ReadOnlyIntegerProperty widthProperty() {
        return widthPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyIntegerWrapper widthPropertyImpl() {
        if (width == null) {
            width = new ReadOnlyIntegerWrapper(this, "width");
        }
        return width;
    }
    /**
     * The height in pixels of the source media.
     * This may be zero if the media has no height, e.g., when playing audio,
     * or if the height is currently unknown which may occur with streaming
     * media.
     * @see width
     */
    private ReadOnlyIntegerWrapper height;


    final void setHeight(int value) {
        heightPropertyImpl().set(value);
    }

    /**
     * Retrieve the height in pixels of the media.
     * @return the media height or zero if the height is undefined or unknown.
     */
    public final int getHeight() {
        return height == null ? 0 : height.get();
    }

    public ReadOnlyIntegerProperty heightProperty() {
        return heightPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyIntegerWrapper heightPropertyImpl() {
        if (height == null) {
            height = new ReadOnlyIntegerWrapper(this, "height");
        }
        return height;
    }
    /**
     * The duration in seconds of the source media. If the media duration is
     * unknown then this property value will be {@link Duration#UNKNOWN}.
     */
    private ReadOnlyObjectWrapper<Duration> duration;

    final void setDuration(Duration value) {
        durationPropertyImpl().set(value);
    }

    /**
     * Retrieve the duration in seconds of the media.
     * @return the duration of the media, {@link Duration#UNKNOWN} if unknown or {@link Duration#INDEFINITE} for live streams
     */
    public final Duration getDuration() {
        return duration == null || duration.get() == null ? Duration.UNKNOWN : duration.get();
    }

    public ReadOnlyObjectProperty<Duration> durationProperty() {
        return durationPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<Duration> durationPropertyImpl() {
        if (duration == null) {
            duration = new ReadOnlyObjectWrapper<Duration>(this, "duration");
        }
        return duration;
    }
    /**
     * An <code>ObservableList</code> of tracks contained in this media object.
     * A <code>Media</code> object can contain multiple tracks, such as a video track
     * with several audio track. This list is unmodifiable: the contents cannot
     * be changed.
     * @see Track
     */
    private ObservableList<Track> tracks;

    /**
     * Retrieve the tracks contained in this media source. If there are
     * no tracks, the returned {@link ObservableList} will be empty.
     * @return the tracks contained in this media source.
     */
    public final ObservableList<Track> getTracks() {
        return tracks;
    }
    private final ObservableList<Track> tracksBacking = FXCollections.observableArrayList();

    /**
     * The markers defined on this media source. A marker is defined to be a
     * mapping from a name to a point in time between the beginning and end of
     * the media.
     */
    private ObservableMap<String, Duration> markers = FXCollections.observableMap(new HashMap<String,Duration>());

    /**
     * Retrieve the markers defined on this <code>Media</code> instance. If
     * there are no markers the returned {@link ObservableMap} will be empty.
     * Programmatic markers may be added by inserting entries in the returned
     * <code>Map</code>.
     *
     * @return the markers defined on this media source.
     */
    public final ObservableMap<String, Duration> getMarkers() {
        return markers;
    }

    /**
     * Constructs a <code>Media</code> instance.  This is the only way to
     * specify the media source. The source must represent a valid <code>URI</code>
     * and is immutable. Only HTTP, HTTPS, FILE, and JAR <code>URL</code>s are supported. If the
     * provided URL is invalid then an exception will be thrown.  If an
     * asynchronous error occurs, the {@link #errorProperty error} property will be set. Listen
     * to this property to be notified of any such errors.
     *
     * <p>If the source uses a non-blocking protocol such as FILE, then any
     * problems which can be detected immediately will cause a <code>MediaException</code>
     * to be thrown. Such problems include the media being inaccessible or in an
     * unsupported format. If however a potentially blocking protocol such as
     * HTTP is used, then the connection will be initialized asynchronously so
     * that these sorts of errors will be signaled by setting the {@link #errorProperty error}
     * property.</p>
     *
     * <p>Constraints:
     * <ul>
     * <li>The supplied URI must conform to RFC-2396 as required by
     * <A href="https://docs.oracle.com/javase/8/docs/api/java/net/URI.html">java.net.URI</A>.</li>
     * <li>Only HTTP, HTTPS, FILE, and JAR URIs are supported.</li>
     * </ul>
     *
     * <p>See <A href="https://docs.oracle.com/javase/8/docs/api/java/net/URI.html">java.net.URI</A>
     * for more information about URI formatting in general.
     * JAR URL syntax is specified in <a href="https://docs.oracle.com/javase/8/docs/api/java/net/JarURLConnection.html">java.net.JarURLConnection</A>.
     *
     * @param source The URI of the source media.
     * @throws NullPointerException if the URI string is <code>null</code>.
     * @throws IllegalArgumentException if the URI string does not conform to RFC-2396
     * or, if appropriate, the Jar URL specification, or is in a non-compliant
     * form which cannot be modified to a compliant form.
     * @throws IllegalArgumentException if the URI string has a <code>null</code>
     * scheme.
     * @throws UnsupportedOperationException if the protocol specified for the
     * source is not supported.
     * @throws MediaException if the media source cannot be connected
     * (type {@link MediaException.Type#MEDIA_INACCESSIBLE}) or is not supported
     * (type {@link MediaException.Type#MEDIA_UNSUPPORTED}).
     */
    public Media(@NamedArg("source") String source) {
        this.source = source;

        URI uri = null;
        try {
            // URI will throw NPE if source == null: do not catch it!
            uri = new URI(source);
        } catch(URISyntaxException use) {
            throw new IllegalArgumentException(use);
        }

        metadata = FXCollections.unmodifiableObservableMap(metadataBacking);
        tracks = FXCollections.unmodifiableObservableList(tracksBacking);

        Locator locator = null;
        try {
            locator = new com.sun.media.jfxmedia.locator.Locator(uri);
            jfxLocator = locator;
            if (locator.canBlock()) {
                InitLocator locatorInit = new InitLocator();
                Thread t = new Thread(locatorInit);
                t.setDaemon(true);
                t.start();
            } else {
                locator.init();
                runMetadataParser();
            }
        } catch(URISyntaxException use) {
            throw new IllegalArgumentException(use);
        } catch(FileNotFoundException fnfe) {
            throw new MediaException(MediaException.Type.MEDIA_UNAVAILABLE, fnfe.getMessage());
        } catch(IOException ioe) {
            throw new MediaException(MediaException.Type.MEDIA_INACCESSIBLE, ioe.getMessage());
        } catch(com.sun.media.jfxmedia.MediaException me) {
            throw new MediaException(MediaException.Type.MEDIA_UNSUPPORTED, me.getMessage());
        }
    }

    private void runMetadataParser() {
        try {
            jfxParser = com.sun.media.jfxmedia.MediaManager.getMetadataParser(jfxLocator);
            jfxParser.addListener(metadataListener);
            jfxParser.startParser();
        } catch (Exception e) {
            jfxParser = null;
        }
    }

    /**
     * The source URI of the media;
     */
    private final String source;

    /**
     * Retrieve the source URI of the media.
     * @return the media source URI as a {@link String}.
     */
    public String getSource() {
        return source;
    }

    /**
     * Locator used by the jfxmedia player, MediaPlayer needs access to this
     */
    private final Locator jfxLocator;
    Locator retrieveJfxLocator() {
        return jfxLocator;
    }

    private MetadataParser jfxParser;

    private Track getTrackWithID(long trackID) {
        for (Track track : tracksBacking) {
            if (track.getTrackID() == trackID) {
                return track;
            }
        }
        return null;
    }

    // http://javafx-jira.kenai.com/browse/RT-24594
    // TODO: Remove this entire method (and associated stuff) when we switch to track parsing in MetadataParser
    void _updateMedia(com.sun.media.jfxmedia.Media _media) {
        try {
            List<com.sun.media.jfxmedia.track.Track> trackList = _media.getTracks();

            if (trackList != null) {
                for (com.sun.media.jfxmedia.track.Track trackElement : trackList) {
                    long trackID = trackElement.getTrackID();
                    if (getTrackWithID(trackID) == null) {
                        Track newTrack = null;
                        Map<String,Object> trackMetadata = new HashMap<String,Object>();
                        if (null != trackElement.getName()) {
                            // FIXME: need constants for metadata keys (globally)
                            trackMetadata.put("name", trackElement.getName());
                        }
                        if (null != trackElement.getLocale()) {
                            trackMetadata.put("locale", trackElement.getLocale());
                        }
                        trackMetadata.put("encoding", trackElement.getEncodingType().toString());
                        trackMetadata.put("enabled", Boolean.valueOf(trackElement.isEnabled()));

                        if (trackElement instanceof com.sun.media.jfxmedia.track.VideoTrack) {
                            com.sun.media.jfxmedia.track.VideoTrack vt =
                                    (com.sun.media.jfxmedia.track.VideoTrack) trackElement;

                            int videoWidth = vt.getFrameSize().getWidth();
                            int videoHeight = vt.getFrameSize().getHeight();

                            // FIXME: this isn't valid when there are multiple video tracks...
                            setWidth(videoWidth);
                            setHeight(videoHeight);

                            trackMetadata.put("video width", Integer.valueOf(videoWidth));
                            trackMetadata.put("video height", Integer.valueOf(videoHeight));

                            newTrack = new VideoTrack(trackElement.getTrackID(), trackMetadata);
                        } else if (trackElement instanceof com.sun.media.jfxmedia.track.AudioTrack) {
                            newTrack = new AudioTrack(trackElement.getTrackID(), trackMetadata);
                        } else if (trackElement instanceof com.sun.media.jfxmedia.track.SubtitleTrack) {
                            newTrack = new SubtitleTrack(trackID, trackMetadata);
                        }

                        if (null != newTrack) {
                            tracksBacking.add(newTrack);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Save any async exceptions as an error.
            setError(new MediaException(MediaException.Type.UNKNOWN, e));
        }
    }

    void _setError(MediaException.Type type, String message) {
        setError(new MediaException(type, message));
    }

    private synchronized void updateMetadata(Map<String, Object> metadata) {
        if (metadata != null) {
            for (Map.Entry<String,Object> entry : metadata.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (key.equals(MetadataParser.IMAGE_TAG_NAME) && value instanceof byte[]) {
                    byte[] imageData = (byte[]) value;
                    Image image = new Image(new ByteArrayInputStream(imageData));
                    if (!image.isError()) {
                        metadataBacking.put(MetadataParser.IMAGE_TAG_NAME, image);
                    }
                } else if (key.equals(MetadataParser.DURATION_TAG_NAME) && value instanceof java.lang.Long) {
                    Duration d = new Duration((Long) value);
                    if (d != null) {
                        metadataBacking.put(MetadataParser.DURATION_TAG_NAME, d);
                    }
                } else {
                    metadataBacking.put(key, value);
                }
            }
        }
    }

    private class _MetadataListener implements MetadataListener {
        @Override
        public void onMetadata(final Map<String, Object> metadata) {
            // Clean up metadata
            Platform.runLater(() -> {
                updateMetadata(metadata);
                jfxParser.removeListener(metadataListener);
                jfxParser.stopParser();
                jfxParser = null;
            });
        }
    }

    private class InitLocator implements Runnable {

        @Override
        public void run() {
            try {
                jfxLocator.init();
                runMetadataParser();
            } catch (URISyntaxException use) {
                _setError(MediaException.Type.OPERATION_UNSUPPORTED, use.getMessage());
            } catch (FileNotFoundException fnfe) {
                _setError(MediaException.Type.MEDIA_UNAVAILABLE, fnfe.getMessage());
            } catch (IOException ioe) {
                _setError(MediaException.Type.MEDIA_INACCESSIBLE, ioe.getMessage());
            } catch (com.sun.media.jfxmedia.MediaException me) {
                _setError(MediaException.Type.MEDIA_UNSUPPORTED, me.getMessage());
            } catch (Exception e) {
                _setError(MediaException.Type.UNKNOWN, e.getMessage());
            }
        }
    }
}
