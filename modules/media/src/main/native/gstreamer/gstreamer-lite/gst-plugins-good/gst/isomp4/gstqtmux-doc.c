/* Quicktime muxer documentation
 * Copyright (C) 2008-2010 Thiago Santos <thiagoss@embedded.ufcg.edu.br>
 * Copyright (C) 2008 Mark Nauwelaerts <mnauw@users.sf.net>
 * Copyright (C) 2010 Nokia Corporation. All rights reserved.
 * Contact: Stefan Kost <stefan.kost@nokia.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
/*
 * Unless otherwise indicated, Source Code is licensed under MIT license.
 * See further explanation attached in License Statement (distributed in the file
 * LICENSE).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/* ============================= mp4mux ==================================== */

/**
 * SECTION:element-mp4mux
 * @short_description: Muxer for ISO MPEG-4 (.mp4) files
 *
 * This element merges streams (audio and video) into ISO MPEG-4 (.mp4) files.
 *
 * The following background intends to explain why various similar muxers
 * are present in this plugin.
 *
 * The <ulink url="http://www.apple.com/quicktime/resources/qtfileformat.pdf">
 * QuickTime file format specification</ulink> served as basis for the MP4 file
 * format specification (mp4mux), and as such the QuickTime file structure is
 * nearly identical to the so-called ISO Base Media file format defined in
 * ISO 14496-12 (except for some media specific parts).
 * In turn, the latter ISO Base Media format was further specialized as a
 * Motion JPEG-2000 file format in ISO 15444-3 (mj2mux)
 * and in various 3GPP(2) specs (gppmux).
 * The fragmented file features defined (only) in ISO Base Media are used by
 * ISMV files making up (a.o.) Smooth Streaming (ismlmux).
 *
 * A few properties (<link linkend="GstMP4Mux--movie-timescale">movie-timescale</link>,
 * <link linkend="GstMP4Mux--trak-timescale">trak-timescale</link>) allow adjusting
 * some technical parameters, which might be useful in (rare) cases to resolve
 * compatibility issues in some situations.
 *
 * Some other properties influence the result more fundamentally.
 * A typical mov/mp4 file's metadata (aka moov) is located at the end of the file,
 * somewhat contrary to this usually being called "the header".
 * However, a <link linkend="GstMP4Mux--faststart">faststart</link> file will
 * (with some effort) arrange this to be located near start of the file,
 * which then allows it e.g. to be played while downloading.
 * Alternatively, rather than having one chunk of metadata at start (or end),
 * there can be some metadata at start and most of the other data can be spread
 * out into fragments of <link linkend="GstMP4Mux--fragment-duration">fragment-duration</link>.
 * If such fragmented layout is intended for streaming purposes, then
 * <link linkend="GstMP4Mux--streamable">streamable</link> allows foregoing to add
 * index metadata (at the end of file).
 *
 * <link linkend="GstMP4Mux--dts-method">dts-method</link> allows selecting a
 * method for managing input timestamps (stay tuned for 0.11 to have this
 * automagically settled).  The default delta/duration method should handle nice
 * (aka perfect streams) just fine, but may experience problems otherwise
 * (e.g. input stream with re-ordered B-frames and/or with frame dropping).
 * The re-ordering approach re-assigns incoming timestamps in ascending order
 * to incoming buffers and offers an alternative in such cases.  In cases where
 * that might fail, the remaining method can be tried, which is exact and
 * according to specs, but might experience playback on not so spec-wise players.
 * Note that this latter approach also requires one to enable
 * <link linkend="GstMP4Mux--presentation-timestamp">presentation-timestamp</link>.
 *
 * <refsect2>
 * <title>Example pipelines</title>
 * |[
 * gst-launch gst-launch v4l2src num-buffers=50 ! queue ! x264enc ! mp4mux ! filesink location=video.mp4
 * ]|
 * Records a video stream captured from a v4l2 device, encodes it into H.264
 * and muxes it into an mp4 file.
 * </refsect2>
 *
 * Documentation last reviewed on 2011-04-21
 */

/* ============================= 3gppmux ==================================== */

/**
 * SECTION:element-3gppmux
 * @short_description: Muxer for 3GPP (.3gp) files
 *
 * This element merges streams (audio and video) into 3GPP (.3gp) files.
 *
 * The following background intends to explain why various similar muxers
 * are present in this plugin.
 *
 * The <ulink url="http://www.apple.com/quicktime/resources/qtfileformat.pdf">
 * QuickTime file format specification</ulink> served as basis for the MP4 file
 * format specification (mp4mux), and as such the QuickTime file structure is
 * nearly identical to the so-called ISO Base Media file format defined in
 * ISO 14496-12 (except for some media specific parts).
 * In turn, the latter ISO Base Media format was further specialized as a
 * Motion JPEG-2000 file format in ISO 15444-3 (mj2mux)
 * and in various 3GPP(2) specs (gppmux).
 * The fragmented file features defined (only) in ISO Base Media are used by
 * ISMV files making up (a.o.) Smooth Streaming (ismlmux).
 *
 * A few properties (<link linkend="Gst3GPPMux--movie-timescale">movie-timescale</link>,
 * <link linkend="Gst3GPPMux--trak-timescale">trak-timescale</link>) allow adjusting
 * some technical parameters, which might be useful in (rare) cases to resolve
 * compatibility issues in some situations.
 *
 * Some other properties influence the result more fundamentally.
 * A typical mov/mp4 file's metadata (aka moov) is located at the end of the file,
 * somewhat contrary to this usually being called "the header".
 * However, a <link linkend="Gst3GPPMux--faststart">faststart</link> file will
 * (with some effort) arrange this to be located near start of the file,
 * which then allows it e.g. to be played while downloading.
 * Alternatively, rather than having one chunk of metadata at start (or end),
 * there can be some metadata at start and most of the other data can be spread
 * out into fragments of <link linkend="Gst3GPPMux--fragment-duration">fragment-duration</link>.
 * If such fragmented layout is intended for streaming purposes, then
 * <link linkend="Gst3GPPMux--streamable">streamable</link> allows foregoing to add
 * index metadata (at the end of file).
 *
 * <link linkend="Gst3GPPMux--dts-method">dts-method</link> allows selecting a
 * method for managing input timestamps (stay tuned for 0.11 to have this
 * automagically settled).  The default delta/duration method should handle nice
 * (aka perfect streams) just fine, but may experience problems otherwise
 * (e.g. input stream with re-ordered B-frames and/or with frame dropping).
 * The re-ordering approach re-assigns incoming timestamps in ascending order
 * to incoming buffers and offers an alternative in such cases.  In cases where
 * that might fail, the remaining method can be tried, which is exact and
 * according to specs, but might experience playback on not so spec-wise players.
 * Note that this latter approach also requires one to enable
 * <link linkend="Gst3GPPMux--presentation-timestamp">presentation-timestamp</link>.
 *
 * <refsect2>
 * <title>Example pipelines</title>
 * |[
 * gst-launch v4l2src num-buffers=50 ! queue ! ffenc_h263 ! gppmux ! filesink location=video.3gp
 * ]|
 * Records a video stream captured from a v4l2 device, encodes it into H.263
 * and muxes it into an 3gp file.
 * </refsect2>
 *
 * Documentation last reviewed on 2011-04-21
 */

/* ============================= mj2pmux ==================================== */

/**
 * SECTION:element-mj2mux
 * @short_description: Muxer for Motion JPEG-2000 (.mj2) files
 *
 * This element merges streams (audio and video) into MJ2 (.mj2) files.
 *
 * The following background intends to explain why various similar muxers
 * are present in this plugin.
 *
 * The <ulink url="http://www.apple.com/quicktime/resources/qtfileformat.pdf">
 * QuickTime file format specification</ulink> served as basis for the MP4 file
 * format specification (mp4mux), and as such the QuickTime file structure is
 * nearly identical to the so-called ISO Base Media file format defined in
 * ISO 14496-12 (except for some media specific parts).
 * In turn, the latter ISO Base Media format was further specialized as a
 * Motion JPEG-2000 file format in ISO 15444-3 (mj2mux)
 * and in various 3GPP(2) specs (gppmux).
 * The fragmented file features defined (only) in ISO Base Media are used by
 * ISMV files making up (a.o.) Smooth Streaming (ismlmux).
 *
 * A few properties (<link linkend="GstMJ2Mux--movie-timescale">movie-timescale</link>,
 * <link linkend="GstMJ2Mux--trak-timescale">trak-timescale</link>) allow adjusting
 * some technical parameters, which might be useful in (rare) cases to resolve
 * compatibility issues in some situations.
 *
 * Some other properties influence the result more fundamentally.
 * A typical mov/mp4 file's metadata (aka moov) is located at the end of the file,
 * somewhat contrary to this usually being called "the header".
 * However, a <link linkend="GstMJ2Mux--faststart">faststart</link> file will
 * (with some effort) arrange this to be located near start of the file,
 * which then allows it e.g. to be played while downloading.
 * Alternatively, rather than having one chunk of metadata at start (or end),
 * there can be some metadata at start and most of the other data can be spread
 * out into fragments of <link linkend="GstMJ2Mux--fragment-duration">fragment-duration</link>.
 * If such fragmented layout is intended for streaming purposes, then
 * <link linkend="GstMJ2Mux--streamable">streamable</link> allows foregoing to add
 * index metadata (at the end of file).
 *
 * <link linkend="GstMJ2Mux--dts-method">dts-method</link> allows selecting a
 * method for managing input timestamps (stay tuned for 0.11 to have this
 * automagically settled).  The default delta/duration method should handle nice
 * (aka perfect streams) just fine, but may experience problems otherwise
 * (e.g. input stream with re-ordered B-frames and/or with frame dropping).
 * The re-ordering approach re-assigns incoming timestamps in ascending order
 * to incoming buffers and offers an alternative in such cases.  In cases where
 * that might fail, the remaining method can be tried, which is exact and
 * according to specs, but might experience playback on not so spec-wise players.
 * Note that this latter approach also requires one to enable
 * <link linkend="GstMJ2Mux--presentation-timestamp">presentation-timestamp</link>.
 *
 * <refsect2>
 * <title>Example pipelines</title>
 * |[
 * gst-launch v4l2src num-buffers=50 ! queue ! jp2kenc ! mj2mux ! filesink location=video.mj2
 * ]|
 * Records a video stream captured from a v4l2 device, encodes it into JPEG-2000
 * and muxes it into an mj2 file.
 * </refsect2>
 *
 * Documentation last reviewed on 2011-04-21
 */

/* ============================= ismlmux ==================================== */

/**
 * SECTION:element-ismlmux
 * @short_description: Muxer for ISML smooth streaming (.isml) files
 *
 * This element merges streams (audio and video) into MJ2 (.mj2) files.
 *
 * The following background intends to explain why various similar muxers
 * are present in this plugin.
 *
 * The <ulink url="http://www.apple.com/quicktime/resources/qtfileformat.pdf">
 * QuickTime file format specification</ulink> served as basis for the MP4 file
 * format specification (mp4mux), and as such the QuickTime file structure is
 * nearly identical to the so-called ISO Base Media file format defined in
 * ISO 14496-12 (except for some media specific parts).
 * In turn, the latter ISO Base Media format was further specialized as a
 * Motion JPEG-2000 file format in ISO 15444-3 (mj2mux)
 * and in various 3GPP(2) specs (gppmux).
 * The fragmented file features defined (only) in ISO Base Media are used by
 * ISMV files making up (a.o.) Smooth Streaming (ismlmux).
 *
 * A few properties (<link linkend="GstISMLMux--movie-timescale">movie-timescale</link>,
 * <link linkend="GstISMLMux--trak-timescale">trak-timescale</link>) allow adjusting
 * some technical parameters, which might be useful in (rare) cases to resolve
 * compatibility issues in some situations.
 *
 * Some other properties influence the result more fundamentally.
 * A typical mov/mp4 file's metadata (aka moov) is located at the end of the file,
 * somewhat contrary to this usually being called "the header".
 * However, a <link linkend="GstISMLMux--faststart">faststart</link> file will
 * (with some effort) arrange this to be located near start of the file,
 * which then allows it e.g. to be played while downloading.
 * Alternatively, rather than having one chunk of metadata at start (or end),
 * there can be some metadata at start and most of the other data can be spread
 * out into fragments of <link linkend="GstISMLMux--fragment-duration">fragment-duration</link>.
 * If such fragmented layout is intended for streaming purposes, then
 * <link linkend="GstISMLMux--streamable">streamable</link> allows foregoing to add
 * index metadata (at the end of file).
 *
 * <link linkend="GstISMLMux--dts-method">dts-method</link> allows selecting a
 * method for managing input timestamps (stay tuned for 0.11 to have this
 * automagically settled).  The default delta/duration method should handle nice
 * (aka perfect streams) just fine, but may experience problems otherwise
 * (e.g. input stream with re-ordered B-frames and/or with frame dropping).
 * The re-ordering approach re-assigns incoming timestamps in ascending order
 * to incoming buffers and offers an alternative in such cases.  In cases where
 * that might fail, the remaining method can be tried, which is exact and
 * according to specs, but might experience playback on not so spec-wise players.
 * Note that this latter approach also requires one to enable
 * <link linkend="GstISMLMux--presentation-timestamp">presentation-timestamp</link>.
 *
 * <refsect2>
 * <title>Example pipelines</title>
 * |[
 * gst-launch v4l2src num-buffers=50 ! queue ! jp2kenc ! mj2mux ! filesink location=video.mj2
 * ]|
 * Records a video stream captured from a v4l2 device, encodes it into JPEG-2000
 * and muxes it into an mj2 file.
 * </refsect2>
 *
 * Documentation last reviewed on 2011-04-21
 */
