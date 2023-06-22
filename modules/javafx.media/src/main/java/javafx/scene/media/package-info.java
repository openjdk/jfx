/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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

/**
 * <p>Provides the set of classes for integrating audio and video into Java FX
 * Applications. The primary use for this package is media playback. There are
 * three principal classes in the media package:
 * {@link javafx.scene.media.Media Media},
 * {@link javafx.scene.media.MediaPlayer MediaPlayer}, and
 * {@link javafx.scene.media.MediaView MediaView}.
 * </p>
 * <h2>Contents</h2>
 * <ol>
 * <li><a href="#SupportedMediaTypes">Supported Media Types</a></li>
 * <li><a href="#SupportedProtocols">Supported Protocols</a></li>
 * <li><a href="#SupportedMetadataTags">Supported Metadata Tags</a></li>
 * <li><a href="#PlayingMediaInJavaFX">Playing Media in Java FX</a></li>
 * </ol>
 *
 * <a id="SupportedMediaTypes"></a>
 * <h3>Supported Media Types</h3>
 *
 * Java FX supports a number of different media types. A media type is considered to
 * be the combination of a container format and one or more encodings. In some
 * cases the container format might simply be an elementary stream containing the
 * encoded data.
 *
 * <h4>Supported Encoding Types</h4>
 *
 * <p>
 * An encoding type specifies how sampled audio or video data are stored. Usually
 * the encoding type implies a particular compression algorithm. The following
 * table indicates the encoding types supported by Java FX Media.
 * </p>
 *
 * <table border="1">
 * <caption>Media Encoding Table</caption>
 * <tr><th scope="col">Encoding</th><th scope="col">Type</th><th scope="col">Description</th></tr>
 * <tr><th scope="row">AAC</th><td>Audio</td><td>Advanced Audio Coding audio compression</td></tr>
 * <tr><th scope="row">MP3</th><td>Audio</td>
 * <td>Raw MPEG-1, 2, and 2.5 audio; layers I, II, and III; all supported
 * combinations of sampling frequencies and bit rates. Note: File must contain at least 3 MP3 frames.</td>
 * </tr>
 * <tr><th scope="row">PCM</th><td>Audio</td><td>Uncompressed, raw audio samples</td></tr>
 * <tr><th scope="row">H.264/AVC</th><td>Video</td><td>H.264/MPEG-4 Part 10 / AVC (Advanced Video Coding)
 * video compression</td></tr>
 * <tr><th scope="row">H.265/HEVC</th><td>Video</td><td>H.265/MPEG-H Part 2 / HEVC (High Efficiency Video Coding)
 * video compression</td></tr>
 * </table>
 *
 * <h4>Supported Container Types</h4>
 *
 * <p>
 * A container type specifies the file format used to store the encoded audio,
 * video, and other media data. Each container type is associated with one or more
 * MIME types, file extensions, and file signatures (the initial bytes in the file).
 * The following table indicates the combination of container and encoding types
 * supported by Java FX Media.
 * </p>
 *
 * <table border="1">
 * <caption>Media Container / Encoding Types Table</caption>
 * <tr><th scope="col">Container</th><th scope="col">Description</th><th scope="col">Video Encoding</th>
 * <th scope="col">Audio Encoding</th><th scope="col">MIME Type</th><th scope="col">File Extension</th></tr>
 * <tr><th scope="row">AIFF</th><td>Audio Interchange File Format</td><td>N/A</td>
 *     <td>PCM</td><td>audio/x-aiff</td><td>.aif, .aiff</td></tr>
 * <tr><th scope="row">HLS (*)</th><td>MP2T HTTP Live Streaming (audiovisual)</td><td>H.264/AVC</td>
 *     <td>AAC</td><td>application/vnd.apple.mpegurl, audio/mpegurl</td><td>.m3u8</td></tr>
 * <tr><th scope="row">HLS (*)</th><td>MP3 HTTP Live Streaming (audio-only)</td><td>N/A</td>
 *     <td>MP3</td><td>application/vnd.apple.mpegurl, audio/mpegurl</td><td>.m3u8</td></tr>
 * <tr><th scope="row">HLS (*)</th><td>fMP4 HTTP Live Streaming (audiovisual)</td><td>H.264/AVC</td>
 *     <td>AAC</td><td>application/vnd.apple.mpegurl, audio/mpegurl</td><td>.m3u8</td></tr>
 * <tr><th scope="row">HLS (*)</th><td>fMP4 HTTP Live Streaming (audiovisual)</td><td>H.265/HEVC</td>
 *     <td>AAC</td><td>application/vnd.apple.mpegurl, audio/mpegurl</td><td>.m3u8</td></tr>
 * <tr><th scope="row">HLS (*)</th><td>fMP4 HTTP Live Streaming (audio-only)</td><td>N/A</td>
 *     <td>AAC</td><td>application/vnd.apple.mpegurl, audio/mpegurl</td><td>.m3u8</td></tr>
 * <tr><th scope="row">HLS (*)</th><td>AAC HTTP Live Streaming (audio-only)</td><td>N/A</td>
 *     <td>AAC</td><td>application/vnd.apple.mpegurl, audio/mpegurl</td><td>.m3u8</td></tr>
 * <tr><th scope="row">MP3</th><td>MPEG-1, 2, 2.5 raw audio stream possibly with ID3 metadata v2.3 or v2.4</td>
 *     <td>N/A</td><td>MP3</td><td>audio/mpeg</td><td>.mp3</td></tr>
 * <tr><th scope="row">MP4</th><td>MPEG-4 Part 14</td><td>H.264/AVC</td>
 *     <td>AAC</td><td>video/mp4, audio/x-m4a, video/x-m4v</td><td>.mp4, .m4a, .m4v</td></tr>
 * <tr><th scope="row">MP4</th><td>MPEG-4 Part 14</td><td>H.265/HEVC</td>
 *     <td>AAC</td><td>video/mp4, audio/x-m4a, video/x-m4v</td><td>.mp4, .m4a, .m4v</td></tr>
 * <tr><th scope="row">WAV</th><td>Waveform Audio Format</td><td>N/A</td>
 *     <td>PCM</td><td>audio/x-wav</td><td>.wav</td></tr>
 * </table>
 *
 * <br>(*) HLS is a protocol rather than a container type but is included here to
 * aggregate similar attributes.
 *
 * <a id="SupportedProtocols"></a>
 * <h3>Supported Protocols</h3>
 *
 * <table border="1">
 * <caption>Supported Protocols Table</caption>
 * <tr><th scope="col">Protocol</th><th scope="col">Description</th><th scope="col">Reference</th></tr>
 * <tr>
 *     <th scope="row">FILE</th>
 *     <td>Protocol for URI representation of local files</td>
 *     <td><a href="https://docs.oracle.com/javase/8/docs/api/java/net/URI.html">java.net.URI</a></td>
 * </tr>
 * <tr>
 *     <th scope="row">HTTP</th>
 *     <td>Hypertext transfer protocol for representation of remote files</td>
 *     <td><a href="https://docs.oracle.com/javase/8/docs/api/java/net/URI.html">java.net.URI</a></td>
 * </tr>
 * <tr>
 *     <th scope="row">HTTPS</th>
 *     <td>Hypertext transfer protocol secure for representation of remote files</td>
 *     <td><a href="https://docs.oracle.com/javase/8/docs/api/java/net/URI.html">java.net.URI</a></td>
 * </tr>
 * <tr>
 *     <th scope="row">JAR</th>
 *     <td>Representation of media entries in files accessible via the FILE, HTTP or HTTPS protocols</td>
 *     <td><a href="https://docs.oracle.com/javase/8/docs/api/java/net/JarURLConnection.html">java.net.JarURLConnection</a></td>
 * </tr>
 * <tr>
 *     <th scope="row">HTTP Live Streaming (HLS)</th>
 *     <td>Playlist-based media streaming via HTTP or HTTPS</td>
 *     <td><a href="http://tools.ietf.org/html/draft-pantos-http-live-streaming">Internet-Draft: HTTP Live Streaming</a></td>
 * </tr>
 * </table>
 * <br>
 * <h4>MPEG-4 Playback via HTTP</h4>
 * <p>
 * It is recommended that MPEG-4 media to be played over HTTP or HTTPS be formatted such that the
 * headers required to decode the stream appear at the beginning of the file. Otherwise,
 * playback might stall until the entire file is downloaded.
 * </p>
 * <h4>HTTP Live Streaming (HLS)</h4>
 * <p>
 * HLS playback handles sources with these characteristics:
 * </p>
 * <ul>
 *     <li>On-demand and live playlists.</li>
 *     <li>Elementary MP3 audio streams (audio/mpegurl) and multiplexed MP2T streams
 *         (application/vnd.apple.mpegurl) with one AAC audio and one H.264/AVC video track.</li>
 *     <li>Playlists with integer or float duration.</li>
 * </ul>
 * <p>
 * Sources which do not conform to this basic profile are not guaranteed to be handled.
 * The playlist contains information about the streams comprising the source and is
 * downloaded at the start of playback. Switching between alternate streams, bit rates,
 * and video resolutions is handled automatically as a function of network conditions.
 * </p>
 *
 * <a id="SupportedMetadataTags"></a>
 * <h3>Supported Metadata Tags</h3>
 *
 * A media container may also include certain metadata which describe the media in
 * the file. The Java FX Media API makes the metadata available via the
 * {@link javafx.scene.media.Media#getMetadata()} method. The keys in this mapping
 * are referred to as <i>tags</i> with the tags supported by Java FX Media listed in
 * the following table. Note that which tags are available for a given media source
 * depend on the metadata actually stored in that source, i.e., not all tags are
 * guaranteed to be available.
 *
 * <table border="1">
 * <caption>"Metadata Keys and Tags Table</caption>
 * <tr><th scope="col"> Container </th><th scope="col"> Tag (type String) </th><th scope="col"> Type </th><th scope="col"> Description </th></tr>
 * <tr><td> MP3 </td><th scope="row"> raw&nbsp;metadata </th><td> Map&lt;String,ByteBuffer&gt; </td><td>The raw metadata according to the appropriate media specification. The key "ID3" maps to MP3 ID3v2 metadata.</td></tr>
 * <tr><td> MP3 </td><th scope="row"> album&nbsp;artist </th><td> java.lang.String </td><td>The artist for the overall album, possibly "Various Artists" for compilations.</td></tr>
 * <tr><td> MP3 </td><th scope="row"> album </th><td> java.lang.String </td><td>The name of the album.</td></tr>
 * <tr><td> MP3 </td><th scope="row"> artist </th><td> java.lang.String </td><td>The artist of the track.</td></tr>
 * <tr><td> MP3 </td><th scope="row"> comment-N </th><td> java.lang.String </td><td>A comment where N is a 0-relative index. Comment format: ContentDescription[lng]=Comment </td></tr>
 * <tr><td> MP3 </td><th scope="row"> composer </th><td> java.lang.String </td><td>The composer of the track.</td></tr>
 * <tr><td> MP3 </td><th scope="row"> year </th><td> java.lang.Integer </td><td>The year the track was recorded.</td></tr>
 * <tr><td> MP3 </td><th scope="row"> disc&nbsp;count </th><td> java.lang.Integer </td><td>The number of discs in the album.</td></tr>
 * <tr><td> MP3 </td><th scope="row"> disc&nbsp;number </th><td> java.lang.Integer </td><td>The 1-relative index of the disc on which this track appears.</td></tr>
 * <tr><td> MP3 </td><th scope="row"> duration </th><td> javafx.util.Duration </td><td>The duration of the track.</td></tr>
 * <tr><td> MP3 </td><th scope="row"> genre </th><td> java.lang.String </td><td>The genre of the track, for example, "Classical," "Darkwave," or "Jazz."</td></tr>
 * <tr><td> MP3 </td><th scope="row"> image </th><td> javafx.scene.image.Image </td><td>The album cover.</td></tr>
 * <tr><td> MP3 </td><th scope="row"> title </th><td> java.lang.String </td><td>The name of the track.</td></tr>
 * <tr><td> MP3 </td><th scope="row"> track&nbsp;count </th><td> java.lang.Integer </td><td>The number of tracks on the album.</td></tr>
 * <tr><td> MP3 </td><th scope="row"> track&nbsp;number </th><td> java.lang.Integer </td><td>The 1-relative index of this track on the disc.</td></tr>
 * </table>
 *
 * <a id="PlayingMediaInJavaFX"></a>
 * <h3>Playing Media in Java FX</h3>
 * <h4>Basic Playback</h4>
 * <p>
 * The basic steps required to play media in Java FX are:
 * </p>
 * <ol>
 *     <li>Create a {@link javafx.scene.media.Media} object for the desired media source.</li>
 *     <li>Create a {@link javafx.scene.media.MediaPlayer} object from the <code>Media</code> object.</li>
 *     <li>Create a {@link javafx.scene.media.MediaView} object.</li>
 *     <li>Add the <code>MediaPlayer</code> to the <code>MediaView</code>.</li>
 *     <li>Add the <code>MediaView</code> to the scene graph.</li>
 *     <li>Invoke {@link javafx.scene.media.MediaPlayer#play()}.</li>
 * </ol>
 * The foregoing steps are illustrated by the sample code in the <code>MediaView</code>
 * class documentation. Some things which should be noted are:
 * <ul>
 *     <li>One <code>Media</code> object may be shared among multiple <code>MediaPlayer</code>s.
 *     <li>One <code>MediaPlayer</code> may be shared amoung multiple <code>MediaView</code>s.
 *     <li>Media may be played directly by a <code>MediaPlayer</code>
 *         without creating a <code>MediaView</code> although a view is required for display.</li>
 *     <li>Instead of <code>MediaPlayer.play()</code>,
 *         {@link javafx.scene.media.MediaPlayer#setAutoPlay MediaPlayer.setAutoPlay(true)}
 *         may be used to request that playing start as soon as possible.</li>
 *     <li><code>MediaPlayer</code> has several operational states defined by
 *         {@link javafx.scene.media.MediaPlayer.Status}.
 *     <li>Audio-only media may instead be played using {@link javafx.scene.media.AudioClip}
 *         (recommended for low latency playback of short clips).</li>
 * </ul>
 * <h4>Error Handling</h4>
 * <p>
 * Errors using Java FX Media may be either synchronous or asynchronous. In general
 * synchronous errors will manifest themselves as a Java <code>Exception</code> and
 * asynchronous errors will cause a Java FX property to be set. In the latter case
 * either the <code>error</code> property may be observed directly, an
 * <code>onError</code> callback registered, or possibly both.</p>
 *
 * <p>The main sources of synchronous errors are
 * {@link javafx.scene.media.Media#Media Media()} and
 * {@link javafx.scene.media.MediaPlayer#MediaPlayer MediaPlayer()}.
 * The asynchronous error properties are
 * {@link javafx.scene.media.Media#errorProperty Media.error} and
 * {@link javafx.scene.media.MediaPlayer#errorProperty MediaPlayer.error}, and the
 * asynchronous error callbacks
 * {@link javafx.scene.media.Media#onErrorProperty Media.onError},
 * {@link javafx.scene.media.MediaPlayer#onErrorProperty MediaPlayer.onError}, and
 * {@link javafx.scene.media.MediaView#onErrorProperty MediaView.onError}.</p>
 *
 * <p>
 * Some errors might be duplicated. For example, a <code>MediaPlayer</code> will
 * propagate an error that it encounters to its associated <code>Media</code>, and
 * a <code>MediaPlayer</code> to all its associated <code>MediaView</code>s. As a
 * consequence, it is possible to receive multiple notifications of the occurrence
 * of a given error, depending on which properties are monitored.
 * </p>
 *
 * <p>The following code snippet illustrates error handling with media:</p>
 * <pre>{@code
 *     String source;
 *     Media media;
 *     MediaPlayer mediaPlayer;
 *     MediaView mediaView;
 *     try {
 *         media = new Media(source);
 *         if (media.getError() == null) {
 *             media.setOnError(new Runnable() {
 *                 public void run() {
 *                     // Handle asynchronous error in Media object.
 *                 }
 *             });
 *             try {
 *                 mediaPlayer = new MediaPlayer(media);
 *                 if (mediaPlayer.getError() == null) {
 *                     mediaPlayer.setOnError(new Runnable() {
 *                         public void run() {
 *                             // Handle asynchronous error in MediaPlayer object.
 *                         }
 *                     });
 *                     mediaView = new MediaView(mediaPlayer);
 *                     mediaView.setOnError(new EventHandler<MediaErrorEvent>() {
 *                         public void handle(MediaErrorEvent t) {
 *                             // Handle asynchronous error in MediaView.
 *                         }
 *                     });
 *                 } else {
 *                     // Handle synchronous error creating MediaPlayer.
 *                 }
 *             } catch (Exception mediaPlayerException) {
 *                 // Handle exception in MediaPlayer constructor.
 *             }
 *         } else {
 *             // Handle synchronous error creating Media.
 *         }
 *     } catch (Exception mediaException) {
 *         // Handle exception in Media constructor.
 *     }
 * }</pre>
 */
package javafx.scene.media;
