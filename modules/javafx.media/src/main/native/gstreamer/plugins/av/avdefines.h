/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AVDEFINES_H
#define AVDEFINES_H

// According to ffmpeg Git they introduced
// _decode_audio4 in version 53.25.0 and removed in version 59
#define DECODE_AUDIO4          (LIBAVCODEC_VERSION_INT >= AV_VERSION_INT(53,25,0) && LIBAVCODEC_VERSION_INT < AV_VERSION_INT(59,0,0))

// New AVCodecID was introduced in 54.25.0
#define NEW_CODEC_ID           (LIBAVCODEC_VERSION_INT >= AV_VERSION_INT(54,25,0))

// New Frame alloc functions were introduced in 55.28.0
#define NEW_ALLOC_FRAME        (LIBAVCODEC_VERSION_INT >= AV_VERSION_INT(55,28,0))

// HEVC/H.265 support should be available in 56 and up
#define HEVC_SUPPORT           (LIBAVCODEC_VERSION_INT >= AV_VERSION_INT(56,0,0))

// "codec" field was removed from AVStream in 59 and "codecpar" should be used
// instead.
#define CODEC_PAR              (LIBAVCODEC_VERSION_INT >= AV_VERSION_INT(59,0,0))

// Use "av_packet_unref()"
#define PACKET_UNREF           (LIBAVCODEC_VERSION_INT >= AV_VERSION_INT(59,0,0))

// Use "avcodec_send_packet()" and "avcodec_receive_frame()"
#define USE_SEND_RECEIVE       (LIBAVCODEC_VERSION_INT >= AV_VERSION_INT(59,0,0))

// Do not call avcodec_register_all() and av_register_all()
// Not required since 58 and removed in 59
#define NO_REGISTER_ALL        (LIBAVCODEC_VERSION_INT >= AV_VERSION_INT(59,0,0))

#endif  /* AVDEFINES_H */

