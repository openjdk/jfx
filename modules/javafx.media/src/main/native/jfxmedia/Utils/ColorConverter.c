/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <Common/ProductFlags.h>
#include "ColorConverter.h"
#include <stdio.h>

#if (! TARGET_OS_LINUX || defined(__SSE2__))
#if defined(TARGET_OS_MAC_ARM64)
#define ENABLE_SIMD_SSE2 0
#else
#define ENABLE_SIMD_SSE2 1
#endif
#else
#define ENABLE_SIMD_SSE2 0
#endif

// --- Begin macros
#define TCLAMP_U8(val, dst) dst = pClip[val]

#define SCLAMP_U8(s, dst)                                       \
{                                                       \
int32_t v = s, mask = (v - 0x1fe) >> 31;           \
\
dst = (((uint32_t)v >> 1) | ~mask) & ~(v >> 31);    \
}
// --- End macros

// --- Begin tables
/* U8 Saturation table with zero in 288*2 */
const uint8_t color_tClip[288 * 2 + 544 * 2] = {
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7,
    8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15,
    16, 16, 17, 17, 18, 18, 19, 19, 20, 20, 21, 21, 22, 22, 23, 23,
    24, 24, 25, 25, 26, 26, 27, 27, 28, 28, 29, 29, 30, 30, 31, 31,
    32, 32, 33, 33, 34, 34, 35, 35, 36, 36, 37, 37, 38, 38, 39, 39,
    40, 40, 41, 41, 42, 42, 43, 43, 44, 44, 45, 45, 46, 46, 47, 47,
    48, 48, 49, 49, 50, 50, 51, 51, 52, 52, 53, 53, 54, 54, 55, 55,
    56, 56, 57, 57, 58, 58, 59, 59, 60, 60, 61, 61, 62, 62, 63, 63,
    64, 64, 65, 65, 66, 66, 67, 67, 68, 68, 69, 69, 70, 70, 71, 71,
    72, 72, 73, 73, 74, 74, 75, 75, 76, 76, 77, 77, 78, 78, 79, 79,
    80, 80, 81, 81, 82, 82, 83, 83, 84, 84, 85, 85, 86, 86, 87, 87,
    88, 88, 89, 89, 90, 90, 91, 91, 92, 92, 93, 93, 94, 94, 95, 95,
    96, 96, 97, 97, 98, 98, 99, 99, 100, 100, 101, 101, 102, 102, 103, 103,
    104, 104, 105, 105, 106, 106, 107, 107, 108, 108, 109, 109, 110, 110,
    111, 111,
    112, 112, 113, 113, 114, 114, 115, 115, 116, 116, 117, 117, 118, 118,
    119, 119,
    120, 120, 121, 121, 122, 122, 123, 123, 124, 124, 125, 125, 126, 126,
    127, 127,
    128, 128, 129, 129, 130, 130, 131, 131, 132, 132, 133, 133, 134, 134,
    135, 135,
    136, 136, 137, 137, 138, 138, 139, 139, 140, 140, 141, 141, 142, 142,
    143, 143,
    144, 144, 145, 145, 146, 146, 147, 147, 148, 148, 149, 149, 150, 150,
    151, 151,
    152, 152, 153, 153, 154, 154, 155, 155, 156, 156, 157, 157, 158, 158,
    159, 159,
    160, 160, 161, 161, 162, 162, 163, 163, 164, 164, 165, 165, 166, 166,
    167, 167,
    168, 168, 169, 169, 170, 170, 171, 171, 172, 172, 173, 173, 174, 174,
    175, 175,
    176, 176, 177, 177, 178, 178, 179, 179, 180, 180, 181, 181, 182, 182,
    183, 183,
    184, 184, 185, 185, 186, 186, 187, 187, 188, 188, 189, 189, 190, 190,
    191, 191,
    192, 192, 193, 193, 194, 194, 195, 195, 196, 196, 197, 197, 198, 198,
    199, 199,
    200, 200, 201, 201, 202, 202, 203, 203, 204, 204, 205, 205, 206, 206,
    207, 207,
    208, 208, 209, 209, 210, 210, 211, 211, 212, 212, 213, 213, 214, 214,
    215, 215,
    216, 216, 217, 217, 218, 218, 219, 219, 220, 220, 221, 221, 222, 222,
    223, 223,
    224, 224, 225, 225, 226, 226, 227, 227, 228, 228, 229, 229, 230, 230,
    231, 231,
    232, 232, 233, 233, 234, 234, 235, 235, 236, 236, 237, 237, 238, 238,
    239, 239,
    240, 240, 241, 241, 242, 242, 243, 243, 244, 244, 245, 245, 246, 246,
    247, 247,
    248, 248, 249, 249, 250, 250, 251, 251, 252, 252, 253, 253, 254, 254,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255,
};

/* color_tYY[i] = (int)(i*1.1644*2 + 0.49999999f) */
const uint16_t color_tYY[256] = {
    0, 2, 5, 7, 9, 12, 14, 16, 19, 21, 23, 26, 28, 30, 33, 35,
    37, 40, 42, 44, 47, 49, 51, 54, 56, 58, 61, 63, 65, 68, 70, 72,
    75, 77, 79, 82, 84, 86, 88, 91, 93, 95, 98, 100, 102, 105, 107, 109,
    112, 114, 116, 119, 121, 123, 126, 128, 130, 133, 135, 137, 140, 142,
    144, 147,
    149, 151, 154, 156, 158, 161, 163, 165, 168, 170, 172, 175, 177, 179,
    182, 184,
    186, 189, 191, 193, 196, 198, 200, 203, 205, 207, 210, 212, 214, 217,
    219, 221,
    224, 226, 228, 231, 233, 235, 238, 240, 242, 245, 247, 249, 252, 254,
    256, 258,
    261, 263, 265, 268, 270, 272, 275, 277, 279, 282, 284, 286, 289, 291,
    293, 296,
    298, 300, 303, 305, 307, 310, 312, 314, 317, 319, 321, 324, 326, 328,
    331, 333,
    335, 338, 340, 342, 345, 347, 349, 352, 354, 356, 359, 361, 363, 366,
    368, 370,
    373, 375, 377, 380, 382, 384, 387, 389, 391, 394, 396, 398, 401, 403,
    405, 408,
    410, 412, 415, 417, 419, 422, 424, 426, 428, 431, 433, 435, 438, 440,
    442, 445,
    447, 449, 452, 454, 456, 459, 461, 463, 466, 468, 470, 473, 475, 477,
    480, 482,
    484, 487, 489, 491, 494, 496, 498, 501, 503, 505, 508, 510, 512, 515,
    517, 519,
    522, 524, 526, 529, 531, 533, 536, 538, 540, 543, 545, 547, 550, 552,
    554, 557,
    559, 561, 564, 566, 568, 571, 573, 575, 578, 580, 582, 585, 587, 589,
    592, 594,
};

/* color_tRV[i] = (int)(i*1.5966*2 + 0.49999999f) */
const uint16_t color_tRV[256] = {
    0, 3, 6, 10, 13, 16, 19, 22, 26, 29, 32, 35, 38, 42, 45, 48,
    51, 54, 57, 61, 64, 67, 70, 73, 77, 80, 83, 86, 89, 93, 96, 99,
    102, 105, 109, 112, 115, 118, 121, 125, 128, 131, 134, 137, 141, 144,
    147, 150,
    153, 156, 160, 163, 166, 169, 172, 176, 179, 182, 185, 188, 192, 195,
    198, 201,
    204, 208, 211, 214, 217, 220, 224, 227, 230, 233, 236, 239, 243, 246,
    249, 252,
    255, 259, 262, 265, 268, 271, 275, 278, 281, 284, 287, 291, 294, 297,
    300, 303,
    307, 310, 313, 316, 319, 323, 326, 329, 332, 335, 338, 342, 345, 348,
    351, 354,
    358, 361, 364, 367, 370, 374, 377, 380, 383, 386, 390, 393, 396, 399,
    402, 406,
    409, 412, 415, 418, 422, 425, 428, 431, 434, 437, 441, 444, 447, 450,
    453, 457,
    460, 463, 466, 469, 473, 476, 479, 482, 485, 489, 492, 495, 498, 501,
    505, 508,
    511, 514, 517, 520, 524, 527, 530, 533, 536, 540, 543, 546, 549, 552,
    556, 559,
    562, 565, 568, 572, 575, 578, 581, 584, 588, 591, 594, 597, 600, 604,
    607, 610,
    613, 616, 619, 623, 626, 629, 632, 635, 639, 642, 645, 648, 651, 655,
    658, 661,
    664, 667, 671, 674, 677, 680, 683, 687, 690, 693, 696, 699, 703, 706,
    709, 712,
    715, 718, 722, 725, 728, 731, 734, 738, 741, 744, 747, 750, 754, 757,
    760, 763,
    766, 770, 773, 776, 779, 782, 786, 789, 792, 795, 798, 801, 805, 808,
    811, 814,
};

/* color_tGU[i] = (int)(135.6352*2 - i*0.3920*2 + 0.49999999f) */
const uint16_t color_tGU[256] = {
    271, 270, 270, 269, 268, 267, 267, 266, 265, 264, 263, 263, 262, 261,
    260, 260,
    259, 258, 257, 256, 256, 255, 254, 253, 252, 252, 251, 250, 249, 249,
    248, 247,
    246, 245, 245, 244, 243, 242, 241, 241, 240, 239, 238, 238, 237, 236,
    235, 234,
    234, 233, 232, 231, 231, 230, 229, 228, 227, 227, 226, 225, 224, 223,
    223, 222,
    221, 220, 220, 219, 218, 217, 216, 216, 215, 214, 213, 212, 212, 211,
    210, 209,
    209, 208, 207, 206, 205, 205, 204, 203, 202, 201, 201, 200, 199, 198,
    198, 197,
    196, 195, 194, 194, 193, 192, 191, 191, 190, 189, 188, 187, 187, 186,
    185, 184,
    183, 183, 182, 181, 180, 180, 179, 178, 177, 176, 176, 175, 174, 173,
    172, 172,
    171, 170, 169, 169, 168, 167, 166, 165, 165, 164, 163, 162, 162, 161,
    160, 159,
    158, 158, 157, 156, 155, 154, 154, 153, 152, 151, 151, 150, 149, 148,
    147, 147,
    146, 145, 144, 143, 143, 142, 141, 140, 140, 139, 138, 137, 136, 136,
    135, 134,
    133, 133, 132, 131, 130, 129, 129, 128, 127, 126, 125, 125, 124, 123,
    122, 122,
    121, 120, 119, 118, 118, 117, 116, 115, 114, 114, 113, 112, 111, 111,
    110, 109,
    108, 107, 107, 106, 105, 104, 103, 103, 102, 101, 100, 100, 99, 98, 97,
    96,
    96, 95, 94, 93, 93, 92, 91, 90, 89, 89, 88, 87, 86, 85, 85, 84,
    83, 82, 82, 81, 80, 79, 78, 78, 77, 76, 75, 74, 74, 73, 72, 71,
};

/* color_tGV[i] = (int)(i*0.8132*2 + 0.49999999f) */
const uint16_t color_tGV[256] = {
    0, 2, 3, 5, 7, 8, 10, 11, 13, 15, 16, 18, 20, 21, 23, 24,
    26, 28, 29, 31, 33, 34, 36, 37, 39, 41, 42, 44, 46, 47, 49, 50,
    52, 54, 55, 57, 59, 60, 62, 63, 65, 67, 68, 70, 72, 73, 75, 76,
    78, 80, 81, 83, 85, 86, 88, 89, 91, 93, 94, 96, 98, 99, 101, 102,
    104, 106, 107, 109, 111, 112, 114, 115, 117, 119, 120, 122, 124, 125,
    127, 128,
    130, 132, 133, 135, 137, 138, 140, 141, 143, 145, 146, 148, 150, 151,
    153, 155,
    156, 158, 159, 161, 163, 164, 166, 168, 169, 171, 172, 174, 176, 177,
    179, 181,
    182, 184, 185, 187, 189, 190, 192, 194, 195, 197, 198, 200, 202, 203,
    205, 207,
    208, 210, 211, 213, 215, 216, 218, 220, 221, 223, 224, 226, 228, 229,
    231, 233,
    234, 236, 237, 239, 241, 242, 244, 246, 247, 249, 250, 252, 254, 255,
    257, 259,
    260, 262, 263, 265, 267, 268, 270, 272, 273, 275, 276, 278, 280, 281,
    283, 285,
    286, 288, 289, 291, 293, 294, 296, 298, 299, 301, 303, 304, 306, 307,
    309, 311,
    312, 314, 316, 317, 319, 320, 322, 324, 325, 327, 329, 330, 332, 333,
    335, 337,
    338, 340, 342, 343, 345, 346, 348, 350, 351, 353, 355, 356, 358, 359,
    361, 363,
    364, 366, 368, 369, 371, 372, 374, 376, 377, 379, 381, 382, 384, 385,
    387, 389,
    390, 392, 394, 395, 397, 398, 400, 402, 403, 405, 407, 408, 410, 411,
    413, 415,
};

/* color_tBU[i] = (int)(i*2.0184*2 + 0.49999999f) */
const uint16_t color_tBU[256] = {
    0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52, 57, 61,
    65, 69, 73, 77, 81, 85, 89, 93, 97, 101, 105, 109, 113, 117, 121, 125,
    129, 133, 137, 141, 145, 149, 153, 157, 161, 166, 170, 174, 178, 182,
    186, 190,
    194, 198, 202, 206, 210, 214, 218, 222, 226, 230, 234, 238, 242, 246,
    250, 254,
    258, 262, 266, 270, 275, 279, 283, 287, 291, 295, 299, 303, 307, 311,
    315, 319,
    323, 327, 331, 335, 339, 343, 347, 351, 355, 359, 363, 367, 371, 375,
    379, 383,
    388, 392, 396, 400, 404, 408, 412, 416, 420, 424, 428, 432, 436, 440,
    444, 448,
    452, 456, 460, 464, 468, 472, 476, 480, 484, 488, 492, 497, 501, 505,
    509, 513,
    517, 521, 525, 529, 533, 537, 541, 545, 549, 553, 557, 561, 565, 569,
    573, 577,
    581, 585, 589, 593, 597, 601, 606, 610, 614, 618, 622, 626, 630, 634,
    638, 642,
    646, 650, 654, 658, 662, 666, 670, 674, 678, 682, 686, 690, 694, 698,
    702, 706,
    710, 715, 719, 723, 727, 731, 735, 739, 743, 747, 751, 755, 759, 763,
    767, 771,
    775, 779, 783, 787, 791, 795, 799, 803, 807, 811, 815, 819, 824, 828,
    832, 836,
    840, 844, 848, 852, 856, 860, 864, 868, 872, 876, 880, 884, 888, 892,
    896, 900,
    904, 908, 912, 916, 920, 924, 928, 933, 937, 941, 945, 949, 953, 957,
    961, 965,
    969, 973, 977, 981, 985, 989, 993, 997, 1001, 1005, 1009, 1013, 1017,
    1021, 1025, 1029,
};
// --- End tables

// --- Begin YCbCr420p conversion functions
#if ENABLE_SIMD_SSE2
// --- Begin SSE2 YCbCr420p conversion functions
#include <emmintrin.h>

__m128i inline_load_si128(const __m128i *p)
{
    return _mm_load_si128(p);
}

__m128i inline_loadu_si128(const __m128i *p)
{
    return _mm_loadu_si128(p);
}

#define    _mm_storeh_epi64(p, x_a)            \
{                            \
    __m128i x_ra = _mm_srli_si128(x_a, 8);        \
    _mm_storel_epi64(p, x_ra);            \
}

#define    SAVE_ARGB1(argb, pd)                \
{                            \
    _mm_store_si128((__m128i*)pd, argb);        \
    pd += 16;                    \
}

#define    SAVE_ARGB2(argb, pd)                \
{                            \
    _mm_storeu_si128((__m128i*)pd, argb);        \
    pd += 16;                    \
}

#define SAVE_BGRA1(bgr, pd)             \
{                           \
    _mm_store_si128((__m128i*)pd, bgr);     \
    pd += 16;                   \
}

#define SAVE_BGRA2(bgr, pd)             \
{                           \
    _mm_storeu_si128((__m128i*)pd, bgr);        \
    pd += 16;                   \
}

/*
 * cc = 16 color values
 * aa = 16 corresponding alpha values to premultiply with
 * tt = scratch register
 */
#define PREMULTIPLY_ALPHA(cc,aa,tt) {\
    x_temp = _mm_unpacklo_epi8(cc, x_zero); \
    x_temp1 = _mm_unpacklo_epi8(aa, x_zero); \
    x_temp1 = _mm_add_epi16(x_temp1, x_one); \
    tt = _mm_mullo_epi16(x_temp, x_temp1); \
    tt = _mm_srli_epi16(tt, 8); \
    x_temp = _mm_unpackhi_epi8(cc, x_zero); \
    x_temp1 = _mm_unpackhi_epi8(aa, x_zero); \
    x_temp1 = _mm_add_epi16(x_temp1, x_one); \
    x_temp1 = _mm_mullo_epi16(x_temp, x_temp1); \
    x_temp1 = _mm_srli_epi16(x_temp1, 8); \
    cc = _mm_packus_epi16(tt, x_temp1); \
}

int ColorConvert_YCbCr420p_to_ARGB32(
                               uint8_t *argb,
                               int32_t argb_stride,
                               int32_t width,
                               int32_t height,
                               const uint8_t *y,
                               const uint8_t *v,
                               const uint8_t *u,
                               const uint8_t *a,
                               int32_t y_stride,
                               int32_t v_stride,
                               int32_t u_stride,
                               int32_t a_stride)
{
    /* 1.1644  * 8192 */
    const __m128i x_c0 = _mm_set1_epi16(0x2543);

    /* 2.0184  * 8192 */
    const __m128i x_c1 = _mm_set1_epi16(0x4097);
    const int32_t ic1 = 0x4097;

    /* abs( -0.3920 * 8192 ) */
    const __m128i x_c4 = _mm_set1_epi16(0xc8b);
    const int32_t ic4 = 0xc8b;

    /* abs( -0.8132 * 8192 ) */
    const __m128i x_c5 = _mm_set1_epi16(0x1a06);
    const int32_t ic5 = 0x1a06;

    /* 1.5966  * 8192 */
    const __m128i x_c8 = _mm_set1_epi16(0x3317);
    const int32_t ic8 = 0x3317;

    /* -276.9856 * 32 */
    const __m128i x_coff0 = _mm_set1_epi16(0xdd60);
    const int32_t icoff0 = (int32_t)0xffffdd60;

    /* 135.6352  * 32 */
    const __m128i x_coff1 = _mm_set1_epi16(0x10f4);
    const int32_t icoff1 = 0x10f4;

    /* -222.9952 * 32 */
    const __m128i x_coff2 = _mm_set1_epi16(0xe420);
    const int32_t icoff2 = (int32_t)0xffffe420;

    const __m128i x_zero = _mm_setzero_si128();
    const __m128i x_one = _mm_set1_epi8(0xff);
//    const __m128i x_aa = _mm_set1_epi8(0xff);

    int32_t jH, iW;
    int32_t iu, iv, ig, ir, ib, iTemp;
    int32_t iu0, iu1, iv1, iv2;
    __m128i x_u0, x_u1, x_v1, x_v2, x_temp, x_out, x_bak, x_temp1;
    __m128i x_u, x_v, x_y1, x_y2, x_y3, x_y4, x_a1, x_a2;
    __m128i x_r1, x_r2, x_r3, x_r4, x_g1, x_g2, x_g3, x_g4;
    __m128i x_b1, x_b2, x_b3, x_b4, x_r, x_g, x_b;
    __m128i x_arl, x_arh, x_gbl, x_gbh, x_argbh, x_argbl;
    __m128i *px_y1, *px_y2, *px_a1, *px_a2;
    __m64 *pm_u, *pm_v;
    uint8_t *pY1, *pY2, *pU, *pV, *pA1, *pA2, *pD1, *pD2, *pd1, *pd2;

    __m128i (*load_si128) (const __m128i*);
    if (((intptr_t)y % 16) != 0 || ((intptr_t)u % 16) != 0 || ((intptr_t)v % 16) != 0 || ((intptr_t)a % 16) != 0 || (y_stride % 16) != 0 || (u_stride % 16) != 0 || (v_stride % 16) != 0 || (a_stride % 16) != 0)
        load_si128 = &inline_loadu_si128;
    else
        load_si128 = &inline_load_si128;

    pY1 = (uint8_t*)y;
    pY2 = (uint8_t*)y + y_stride;
    pU = (uint8_t*)u;
    pV = (uint8_t*)v;
    pA1 = (uint8_t*)a;
    pA2 = (uint8_t*)a + a_stride;
    pD1 = (uint8_t*)argb;
    pD2 = (uint8_t*)argb + argb_stride;

    for (jH = 0; jH < (height >> 1); jH++) {
        px_y1 = (__m128i*)pY1;
        px_y2 = (__m128i*)pY2;
        pm_u = (__m64*)pU;
        pm_v = (__m64*)pV;
        px_a1 = (__m128i*)pA1;
        px_a2 = (__m128i*)pA2;
        pd1 = pD1;
        pd2 = pD2;
        iW = 0;

        /* 32 pixels */
        for (; iW <= width - 16; iW += 16) {
            x_temp = _mm_loadl_epi64((__m128i*)pm_u);
            x_u = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_u++;
            x_temp = _mm_loadl_epi64((__m128i*)pm_v);
            x_v = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_v++;
            x_temp = load_si128(px_y1);
            px_y1++;
            x_temp1 = load_si128(px_y2);
            px_y2++;
            x_out = _mm_unpacklo_epi16(x_temp, x_temp1);
            x_y1 = _mm_unpacklo_epi8(x_zero, x_out);
            x_y2 = _mm_unpackhi_epi8(x_zero, x_out);
            x_out = _mm_unpackhi_epi16(x_temp, x_temp1);
            x_y3 = _mm_unpacklo_epi8(x_zero, x_out);
            x_y4 = _mm_unpackhi_epi8(x_zero, x_out);
            x_temp = load_si128(px_a1);             // top row (16 pix)
            px_a1++;
            x_temp1 = load_si128(px_a2);            // bottom row (16 pix)
            px_a2++;
            x_a1 = _mm_unpacklo_epi16(x_temp, x_temp1); // interleave top+bottom
            x_a2 = _mm_unpackhi_epi16(x_temp, x_temp1);

            /* pre calc x_[r/g/b][12] */
            x_u0 = _mm_mulhi_epu16(x_u, x_c1);
            x_b = _mm_add_epi16(x_u0, x_coff0); // 8 16 bit B

            x_u1 = _mm_mulhi_epu16(x_u, x_c4);
            x_v1 = _mm_mulhi_epu16(x_v, x_c5);
            x_temp = _mm_add_epi16(x_u1, x_v1);
            x_g = _mm_sub_epi16(x_coff1, x_temp);

            x_v2 = _mm_mulhi_epu16(x_v, x_c8);
            x_r = _mm_add_epi16(x_v2, x_coff2);

            x_y1 = _mm_mulhi_epu16(x_y1, x_c0);
            x_y2 = _mm_mulhi_epu16(x_y2, x_c0);
            x_y3 = _mm_mulhi_epu16(x_y3, x_c0);
            x_y4 = _mm_mulhi_epu16(x_y4, x_c0);

            /* x_b[1/2/3/4] */
            x_bak = _mm_unpacklo_epi16(x_b, x_b);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_b1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_b2 = _mm_add_epi16(x_y2, x_temp);

            x_bak = _mm_unpackhi_epi16(x_b, x_b);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_b3 = _mm_add_epi16(x_y3, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_b4 = _mm_add_epi16(x_y4, x_temp);

            /* x_g[1/2/3/4] */
            x_bak = _mm_unpacklo_epi16(x_g, x_g);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_g1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_g2 = _mm_add_epi16(x_y2, x_temp);

            x_bak = _mm_unpackhi_epi16(x_g, x_g);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_g3 = _mm_add_epi16(x_y3, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_g4 = _mm_add_epi16(x_y4, x_temp);

            /* x_r[1/2/3/4] */
            x_bak = _mm_unpacklo_epi16(x_r, x_r);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_r1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_r2 = _mm_add_epi16(x_y2, x_temp);

            x_bak = _mm_unpackhi_epi16(x_r, x_r);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_r3 = _mm_add_epi16(x_y3, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_r4 = _mm_add_epi16(x_y4, x_temp);

            x_b1 = _mm_srai_epi16(x_b1, 5);
            x_b2 = _mm_srai_epi16(x_b2, 5);
            x_b3 = _mm_srai_epi16(x_b3, 5);
            x_b4 = _mm_srai_epi16(x_b4, 5);
            x_g1 = _mm_srai_epi16(x_g1, 5);
            x_g2 = _mm_srai_epi16(x_g2, 5);
            x_g3 = _mm_srai_epi16(x_g3, 5);
            x_g4 = _mm_srai_epi16(x_g4, 5);
            x_r1 = _mm_srai_epi16(x_r1, 5);
            x_r2 = _mm_srai_epi16(x_r2, 5);
            x_r3 = _mm_srai_epi16(x_r3, 5);
            x_r4 = _mm_srai_epi16(x_r4, 5);

            /* pack: 16=>8 */
            x_b = _mm_packus_epi16(x_b1, x_b2);
            x_b1 = _mm_packus_epi16(x_b3, x_b4);
            x_r = _mm_packus_epi16(x_r1, x_r2);
            x_r1 = _mm_packus_epi16(x_r3, x_r4);
            x_g = _mm_packus_epi16(x_g1, x_g2);
            x_g1 = _mm_packus_epi16(x_g3, x_g4);

            /* create argb sequences */
            x_arl = _mm_unpacklo_epi8(x_a1, x_r); // XXXX xxxx xxxx xxxx - each X is top and bottom interleaved
            x_arh = _mm_unpackhi_epi8(x_a1, x_r); // xxxx XXXX xxxx xxxx
            x_gbl = _mm_unpacklo_epi8(x_g, x_b);
            x_gbh = _mm_unpackhi_epi8(x_g, x_b);

            x_argbl = _mm_unpacklo_epi16(x_arl, x_gbl);
            x_argbh = _mm_unpackhi_epi16(x_arl, x_gbl);
            x_temp = _mm_unpacklo_epi64(x_argbl, x_argbh);
            x_temp1 = _mm_unpackhi_epi64(x_argbl, x_argbh);
            SAVE_ARGB1(x_temp, pd1);
            SAVE_ARGB1(x_temp1, pd2);

            x_argbl = _mm_unpacklo_epi16(x_arh, x_gbh);
            x_argbh = _mm_unpackhi_epi16(x_arh, x_gbh);
            x_temp = _mm_unpacklo_epi64(x_argbl, x_argbh);
            x_temp1 = _mm_unpackhi_epi64(x_argbl, x_argbh);
            SAVE_ARGB1(x_temp, pd1);
            SAVE_ARGB1(x_temp1, pd2);

            /* create argb sequences */
            x_arl = _mm_unpacklo_epi8(x_a2, x_r1); // xxxx xxxx xxxx XXXX
            x_arh = _mm_unpackhi_epi8(x_a2, x_r1); // xxxx xxxx XXXX xxxx
            x_gbl = _mm_unpacklo_epi8(x_g1, x_b1);
            x_gbh = _mm_unpackhi_epi8(x_g1, x_b1);

            x_argbl = _mm_unpacklo_epi16(x_arl, x_gbl);
            x_argbh = _mm_unpackhi_epi16(x_arl, x_gbl);
            x_temp = _mm_unpacklo_epi64(x_argbl, x_argbh);
            x_temp1 = _mm_unpackhi_epi64(x_argbl, x_argbh);
            SAVE_ARGB1(x_temp, pd1);
            SAVE_ARGB1(x_temp1, pd2);

            x_argbl = _mm_unpacklo_epi16(x_arh, x_gbh);
            x_argbh = _mm_unpackhi_epi16(x_arh, x_gbh);
            x_temp = _mm_unpacklo_epi64(x_argbl, x_argbh);
            x_temp1 = _mm_unpackhi_epi64(x_argbl, x_argbh);
            SAVE_ARGB1(x_temp, pd1);
            SAVE_ARGB1(x_temp1, pd2);
        }
        /* 16 pixels - 8 byte chroma alignment */
        if (iW <= width - 8) {
            iTemp = *((int32_t *)pm_u);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_u = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_u = (__m64 *) (((int32_t *)pm_u) + 1);

            iTemp = *((int32_t *)pm_v);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_v = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_v = (__m64 *) (((int32_t *)pm_v) + 1);

            x_temp = _mm_loadl_epi64(px_y1);
            px_y1 = (__m128i *) (((__m64 *)px_y1) + 1);
            x_temp1 = _mm_loadl_epi64(px_y2);
            px_y2 = (__m128i *) (((__m64 *)px_y2) + 1);
            x_out = _mm_unpacklo_epi16(x_temp, x_temp1);
            x_y1 = _mm_unpacklo_epi8(x_zero, x_out);
            x_y2 = _mm_unpackhi_epi8(x_zero, x_out);

            x_temp = _mm_loadl_epi64(px_a1);
            px_a1 = (__m128i *) (((__m64 *)px_a1) + 1);
            x_temp1 = _mm_loadl_epi64(px_a2);
            px_a2 = (__m128i *) (((__m64 *)px_a2) + 1);
            x_a1 = _mm_unpacklo_epi16(x_temp, x_temp1);

            /* pre calc x_[r/g/b][12] */
            x_u0 = _mm_mulhi_epu16(x_u, x_c1);
            x_b = _mm_add_epi16(x_u0, x_coff0);

            x_u1 = _mm_mulhi_epu16(x_u, x_c4);
            x_v1 = _mm_mulhi_epu16(x_v, x_c5);
            x_temp = _mm_add_epi16(x_u1, x_v1);
            x_g = _mm_sub_epi16(x_coff1, x_temp);

            x_v2 = _mm_mulhi_epu16(x_v, x_c8);
            x_r = _mm_add_epi16(x_v2, x_coff2);

            x_y1 = _mm_mulhi_epu16(x_y1, x_c0);
            x_y2 = _mm_mulhi_epu16(x_y2, x_c0);

            /* x_b[1/2] */
            x_bak = _mm_unpacklo_epi16(x_b, x_b);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_b1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_b2 = _mm_add_epi16(x_y2, x_temp);

            /* x_g[1/2] */
            x_bak = _mm_unpacklo_epi16(x_g, x_g);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_g1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_g2 = _mm_add_epi16(x_y2, x_temp);

            /* x_r[1/2] */
            x_bak = _mm_unpacklo_epi16(x_r, x_r);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_r1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_r2 = _mm_add_epi16(x_y2, x_temp);

            x_b1 = _mm_srai_epi16(x_b1, 5);
            x_b2 = _mm_srai_epi16(x_b2, 5);
            x_g1 = _mm_srai_epi16(x_g1, 5);
            x_g2 = _mm_srai_epi16(x_g2, 5);
            x_r1 = _mm_srai_epi16(x_r1, 5);
            x_r2 = _mm_srai_epi16(x_r2, 5);

            /* pack: 16=>8 */
            x_b = _mm_packus_epi16(x_b1, x_b2);
            x_r = _mm_packus_epi16(x_r1, x_r2);
            x_g = _mm_packus_epi16(x_g1, x_g2);

            /* create argb sequences */
            x_arl = _mm_unpacklo_epi8(x_a1, x_r);
            x_arh = _mm_unpackhi_epi8(x_a1, x_r);
            x_gbl = _mm_unpacklo_epi8(x_g, x_b);
            x_gbh = _mm_unpackhi_epi8(x_g, x_b);

            x_argbl = _mm_unpacklo_epi16(x_arl, x_gbl);
            x_argbh = _mm_unpackhi_epi16(x_arl, x_gbl);
            x_temp = _mm_unpacklo_epi64(x_argbl, x_argbh);
            x_temp1 = _mm_unpackhi_epi64(x_argbl, x_argbh);
            SAVE_ARGB1(x_temp, pd1);
            SAVE_ARGB1(x_temp1, pd2);

            x_argbl = _mm_unpacklo_epi16(x_arh, x_gbh);
            x_argbh = _mm_unpackhi_epi16(x_arh, x_gbh);
            x_temp = _mm_unpacklo_epi64(x_argbl, x_argbh);
            x_temp1 = _mm_unpackhi_epi64(x_argbl, x_argbh);
            SAVE_ARGB1(x_temp, pd1);
            SAVE_ARGB1(x_temp1, pd2);

            iW += 8;
        }

        /* 8 pixels */
        if (iW <= width - 4) {
            iTemp = *((uint16_t *)pm_u);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_u = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_u = (__m64 *) (((uint16_t *)pm_u) + 1);
            iTemp = *((uint16_t *)pm_v);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_v = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_v = (__m64 *) (((uint16_t *)pm_v) + 1);

            iTemp = *((int32_t *)px_y1);
            px_y1 = (__m128i *) (((int32_t *)px_y1) + 1);
            x_temp = _mm_cvtsi32_si128(iTemp);
            iTemp = *((int32_t *)px_y2);
            px_y2 = (__m128i *) (((int32_t *)px_y2) + 1);
            x_temp1 = _mm_cvtsi32_si128(iTemp);
            x_out = _mm_unpacklo_epi16(x_temp, x_temp1);
            x_y1 = _mm_unpacklo_epi8(x_zero, x_out);

            iTemp = *((int32_t *)px_a1);
            px_a1 = (__m128i *) (((int32_t *)px_a1) + 1);
            x_temp = _mm_cvtsi32_si128(iTemp);
            iTemp = *((int32_t *)px_a2);
            px_a2 = (__m128i *) (((int32_t *)px_a2) + 1);
            x_temp1 = _mm_cvtsi32_si128(iTemp);
            x_a1 = _mm_unpacklo_epi16(x_temp, x_temp1);

            /* pre calc x_[r/g/b][1] */
            x_u0 = _mm_mulhi_epu16(x_u, x_c1);
            x_b = _mm_add_epi16(x_u0, x_coff0);

            x_u1 = _mm_mulhi_epu16(x_u, x_c4);
            x_v1 = _mm_mulhi_epu16(x_v, x_c5);
            x_temp = _mm_add_epi16(x_u1, x_v1);
            x_g = _mm_sub_epi16(x_coff1, x_temp);

            x_v2 = _mm_mulhi_epu16(x_v, x_c8);
            x_r = _mm_add_epi16(x_v2, x_coff2);

            x_y1 = _mm_mulhi_epu16(x_y1, x_c0);
            x_y2 = _mm_mulhi_epu16(x_y2, x_c0);

            /* x_b[1] */
            x_bak = _mm_unpacklo_epi16(x_b, x_b);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_b1 = _mm_add_epi16(x_y1, x_temp);

            /* x_g[1] */
            x_bak = _mm_unpacklo_epi16(x_g, x_g);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_g1 = _mm_add_epi16(x_y1, x_temp);

            /* x_r[1] */
            x_bak = _mm_unpacklo_epi16(x_r, x_r);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_r1 = _mm_add_epi16(x_y1, x_temp);

            x_b1 = _mm_srai_epi16(x_b1, 5);
            x_g1 = _mm_srai_epi16(x_g1, 5);
            x_r1 = _mm_srai_epi16(x_r1, 5);

            /* pack: 16=>8 */
            x_b = _mm_packus_epi16(x_b1, x_zero);
            x_r = _mm_packus_epi16(x_r1, x_zero);
            x_g = _mm_packus_epi16(x_g1, x_zero);

            /* create argb sequences */
            x_arl = _mm_unpacklo_epi8(x_a1, x_r);
            x_gbl = _mm_unpacklo_epi8(x_g, x_b);

            x_argbl = _mm_unpacklo_epi16(x_arl, x_gbl);
            x_argbh = _mm_unpackhi_epi16(x_arl, x_gbl);
            x_temp = _mm_unpacklo_epi64(x_argbl, x_argbh);
            x_temp1 = _mm_unpackhi_epi64(x_argbl, x_argbh);
            SAVE_ARGB1(x_temp, pd1);
            SAVE_ARGB1(x_temp1, pd2);

            iW += 4;
        }

        /* 4 pixels */
        if (iW <= width - 2) {
            /* load y u v, and expand */
            iu = *((uint8_t *)pm_u);
            pm_u = (__m64 *) (((uint8_t *)pm_u) + 1);
            iv = *((uint8_t *)pm_v);
            pm_v = (__m64 *) (((uint8_t *)pm_v) + 1);

            iTemp = (*((uint16_t *)px_y1)  & 0xffff) | (*((uint16_t *)px_y2)<<16);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_y1 = _mm_unpacklo_epi8(x_zero, x_temp);
            px_y1 = (__m128i *) (((uint16_t *)px_y1) + 1);
            px_y2 = (__m128i *) (((uint16_t *)px_y2) + 1);

            iTemp = (*((uint16_t *)px_a1)  & 0xffff) | (*((uint16_t *)px_a2)<<16);
            x_a1 = _mm_cvtsi32_si128(iTemp);
            px_a1 = (__m128i *) (((uint16_t *)px_a1) + 1);
            px_a2 = (__m128i *) (((uint16_t *)px_a2) + 1);

            /* pre-calc d[r/g/b][1] */
            iu0 = (iu * ic1) >> 8;
            ib = icoff0 + iu0;

            iu1 = (iu * ic4) >> 8;
            iv1 = (iv * ic5) >> 8;
            iTemp = iu1 + iv1;
            ig = icoff1 - iTemp;

            iv2 = (iv * ic8) >> 8;
            ir = iv2 + icoff2;

            x_y1 = _mm_mulhi_epu16(x_y1, x_c0);

            /* db1 */
            x_temp = _mm_set1_epi16(ib);
            x_b1 = _mm_add_epi16(x_y1, x_temp);

            /* dg1 */
            x_temp = _mm_set1_epi16(ig);
            x_g1 = _mm_add_epi16(x_y1, x_temp);

            /* dr1 */
            x_temp = _mm_set1_epi16(ir);
            x_r1 = _mm_add_epi16(x_y1, x_temp);

            x_b1 = _mm_srai_epi16(x_b1, 5);
            x_g1 = _mm_srai_epi16(x_g1, 5);
            x_r1 = _mm_srai_epi16(x_r1, 5);

            /* pack: 16=>8 */
            x_b = _mm_packus_epi16(x_b1, x_zero);
            x_r = _mm_packus_epi16(x_r1, x_zero);
            x_g = _mm_packus_epi16(x_g1, x_zero);

            /* create argb sequences */
            x_arl = _mm_unpacklo_epi8(x_a1, x_r);
            x_gbl = _mm_unpacklo_epi8(x_g, x_b);

            /* lower half of darl & dgbl */
            x_argbl = _mm_unpacklo_epi16(x_arl, x_gbl);
            _mm_storel_epi64((__m128i*)pd1, x_argbl);
            _mm_storeh_epi64((__m128i*)pd2, x_argbl);

            pd1 += 8;
            pd2 += 8;
        }

        pY1 += (2 * y_stride);
        pY2 += (2 * y_stride);
        pU += u_stride;
        pV += v_stride;
        pA1 += (2 * a_stride);
        pA2 += (2 * a_stride);
        pD1 += (2 * argb_stride);
        pD2 += (2 * argb_stride);
    }

    return 0;
}

int ColorConvert_YCbCr420p_to_ARGB32_no_alpha(
                                     uint8_t *argb,
                                     int32_t argb_stride,
                                     int32_t width,
                                     int32_t height,
                                     const uint8_t *y,
                                     const uint8_t *v,
                                     const uint8_t *u,
                                     int32_t y_stride,
                                     int32_t v_stride,
                                     int32_t u_stride)
{
    /* 1.1644  * 8192 */
    const __m128i x_c0 = _mm_set1_epi16(0x2543);

    /* 2.0184  * 8192 */
    const __m128i x_c1 = _mm_set1_epi16(0x4097);
    const int32_t ic1 = 0x4097;

    /* abs( -0.3920 * 8192 ) */
    const __m128i x_c4 = _mm_set1_epi16(0xc8b);
    const int32_t ic4 = 0xc8b;

    /* abs( -0.8132 * 8192 ) */
    const __m128i x_c5 = _mm_set1_epi16(0x1a06);
    const int32_t ic5 = 0x1a06;

    /* 1.5966  * 8192 */
    const __m128i x_c8 = _mm_set1_epi16(0x3317);
    const int32_t ic8 = 0x3317;

    /* -276.9856 * 32 */
    const __m128i x_coff0 = _mm_set1_epi16(0xdd60);
    const int32_t icoff0 = (int32_t)0xffffdd60;

    /* 135.6352  * 32 */
    const __m128i x_coff1 = _mm_set1_epi16(0x10f4);
    const int32_t icoff1 = 0x10f4;

    /* -222.9952 * 32 */
    const __m128i x_coff2 = _mm_set1_epi16(0xe420);
    const int32_t icoff2 = (int32_t)0xffffe420;

    const __m128i x_zero = _mm_setzero_si128();
    const __m128i x_aa = _mm_set1_epi8(0xff);

    int32_t jH, iW;
    int32_t iu, iv, ig, ir, ib, iTemp;
    int32_t iu0, iu1, iv1, iv2;
    __m128i x_u0, x_u1, x_v1, x_v2, x_temp, x_out, x_bak, x_temp1;
    __m128i x_u, x_v, x_y1, x_y2, x_y3, x_y4;
    __m128i x_r1, x_r2, x_r3, x_r4, x_g1, x_g2, x_g3, x_g4;
    __m128i x_b1, x_b2, x_b3, x_b4, x_r, x_g, x_b;
    __m128i x_arl, x_arh, x_gbl, x_gbh, x_argbh, x_argbl;
    __m128i *px_y1, *px_y2;
    __m64 *pm_u, *pm_v;
    uint8_t *pY1, *pY2, *pU, *pV, *pD1, *pD2, *pd1, *pd2;

    __m128i (*load_si128) (const __m128i*);
    if (((intptr_t)y % 16) != 0 || ((intptr_t)u % 16) != 0 || ((intptr_t)v % 16) != 0 || (y_stride % 16) != 0 || (u_stride % 16) != 0 || (v_stride % 16) != 0)
        load_si128 = &inline_loadu_si128;
    else
        load_si128 = &inline_load_si128;

    pY1 = (uint8_t*)y;
    pY2 = (uint8_t*)y + y_stride;
    pU = (uint8_t*)u;
    pV = (uint8_t*)v;
    pD1 = (uint8_t*)argb;
    pD2 = (uint8_t*)argb + argb_stride;

    for (jH = 0; jH < (height >> 1); jH++) {
        px_y1 = (__m128i*)pY1;
        px_y2 = (__m128i*)pY2;
        pm_u = (__m64*)pU;
        pm_v = (__m64*)pV;
        pd1 = pD1;
        pd2 = pD2;
        iW = 0;

        /* 32 pixels */
        for (; iW <= width - 16; iW += 16) {
            x_temp = _mm_loadl_epi64((__m128i*)pm_u);
            x_u = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_u++;
            x_temp = _mm_loadl_epi64((__m128i*)pm_v);
            x_v = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_v++;
            x_temp = load_si128(px_y1);
            px_y1++;
            x_temp1 = load_si128(px_y2);
            px_y2++;
            x_out = _mm_unpacklo_epi16(x_temp, x_temp1);
            x_y1 = _mm_unpacklo_epi8(x_zero, x_out);
            x_y2 = _mm_unpackhi_epi8(x_zero, x_out);
            x_out = _mm_unpackhi_epi16(x_temp, x_temp1);
            x_y3 = _mm_unpacklo_epi8(x_zero, x_out);
            x_y4 = _mm_unpackhi_epi8(x_zero, x_out);

            /* pre calc x_[r/g/b][12] */
            x_u0 = _mm_mulhi_epu16(x_u, x_c1);
            x_b = _mm_add_epi16(x_u0, x_coff0);

            x_u1 = _mm_mulhi_epu16(x_u, x_c4);
            x_v1 = _mm_mulhi_epu16(x_v, x_c5);
            x_temp = _mm_add_epi16(x_u1, x_v1);
            x_g = _mm_sub_epi16(x_coff1, x_temp);

            x_v2 = _mm_mulhi_epu16(x_v, x_c8);
            x_r = _mm_add_epi16(x_v2, x_coff2);

            x_y1 = _mm_mulhi_epu16(x_y1, x_c0);
            x_y2 = _mm_mulhi_epu16(x_y2, x_c0);
            x_y3 = _mm_mulhi_epu16(x_y3, x_c0);
            x_y4 = _mm_mulhi_epu16(x_y4, x_c0);

            /* x_b[1/2/3/4] */
            x_bak = _mm_unpacklo_epi16(x_b, x_b);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_b1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_b2 = _mm_add_epi16(x_y2, x_temp);

            x_bak = _mm_unpackhi_epi16(x_b, x_b);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_b3 = _mm_add_epi16(x_y3, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_b4 = _mm_add_epi16(x_y4, x_temp);

            /* x_g[1/2/3/4] */
            x_bak = _mm_unpacklo_epi16(x_g, x_g);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_g1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_g2 = _mm_add_epi16(x_y2, x_temp);

            x_bak = _mm_unpackhi_epi16(x_g, x_g);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_g3 = _mm_add_epi16(x_y3, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_g4 = _mm_add_epi16(x_y4, x_temp);

            /* x_r[1/2/3/4] */
            x_bak = _mm_unpacklo_epi16(x_r, x_r);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_r1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_r2 = _mm_add_epi16(x_y2, x_temp);

            x_bak = _mm_unpackhi_epi16(x_r, x_r);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_r3 = _mm_add_epi16(x_y3, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_r4 = _mm_add_epi16(x_y4, x_temp);

            x_b1 = _mm_srai_epi16(x_b1, 5);
            x_b2 = _mm_srai_epi16(x_b2, 5);
            x_b3 = _mm_srai_epi16(x_b3, 5);
            x_b4 = _mm_srai_epi16(x_b4, 5);
            x_g1 = _mm_srai_epi16(x_g1, 5);
            x_g2 = _mm_srai_epi16(x_g2, 5);
            x_g3 = _mm_srai_epi16(x_g3, 5);
            x_g4 = _mm_srai_epi16(x_g4, 5);
            x_r1 = _mm_srai_epi16(x_r1, 5);
            x_r2 = _mm_srai_epi16(x_r2, 5);
            x_r3 = _mm_srai_epi16(x_r3, 5);
            x_r4 = _mm_srai_epi16(x_r4, 5);

            /* pack: 16=>8 */
            x_b = _mm_packus_epi16(x_b1, x_b2);
            x_b1 = _mm_packus_epi16(x_b3, x_b4);
            x_r = _mm_packus_epi16(x_r1, x_r2);
            x_r1 = _mm_packus_epi16(x_r3, x_r4);
            x_g = _mm_packus_epi16(x_g1, x_g2);
            x_g1 = _mm_packus_epi16(x_g3, x_g4);

            /* create argb sequences */
            x_arl = _mm_unpacklo_epi8(x_aa, x_r); // XXXX xxxx xxxx xxxx - each X is top and bottom interleaved
            x_arh = _mm_unpackhi_epi8(x_aa, x_r); // xxxx XXXX xxxx xxxx
            x_gbl = _mm_unpacklo_epi8(x_g, x_b);
            x_gbh = _mm_unpackhi_epi8(x_g, x_b);

            x_argbl = _mm_unpacklo_epi16(x_arl, x_gbl);
            x_argbh = _mm_unpackhi_epi16(x_arl, x_gbl);
            x_temp = _mm_unpacklo_epi64(x_argbl, x_argbh);
            x_temp1 = _mm_unpackhi_epi64(x_argbl, x_argbh);
            SAVE_ARGB1(x_temp, pd1);
            SAVE_ARGB1(x_temp1, pd2);

            x_argbl = _mm_unpacklo_epi16(x_arh, x_gbh);
            x_argbh = _mm_unpackhi_epi16(x_arh, x_gbh);
            x_temp = _mm_unpacklo_epi64(x_argbl, x_argbh);
            x_temp1 = _mm_unpackhi_epi64(x_argbl, x_argbh);
            SAVE_ARGB1(x_temp, pd1);
            SAVE_ARGB1(x_temp1, pd2);

            /* create argb sequences */
            x_arl = _mm_unpacklo_epi8(x_aa, x_r1); // xxxx xxxx xxxx XXXX
            x_arh = _mm_unpackhi_epi8(x_aa, x_r1); // xxxx xxxx XXXX xxxx
            x_gbl = _mm_unpacklo_epi8(x_g1, x_b1);
            x_gbh = _mm_unpackhi_epi8(x_g1, x_b1);

            x_argbl = _mm_unpacklo_epi16(x_arl, x_gbl);
            x_argbh = _mm_unpackhi_epi16(x_arl, x_gbl);
            x_temp = _mm_unpacklo_epi64(x_argbl, x_argbh);
            x_temp1 = _mm_unpackhi_epi64(x_argbl, x_argbh);
            SAVE_ARGB1(x_temp, pd1);
            SAVE_ARGB1(x_temp1, pd2);

            x_argbl = _mm_unpacklo_epi16(x_arh, x_gbh);
            x_argbh = _mm_unpackhi_epi16(x_arh, x_gbh);
            x_temp = _mm_unpacklo_epi64(x_argbl, x_argbh);
            x_temp1 = _mm_unpackhi_epi64(x_argbl, x_argbh);
            SAVE_ARGB1(x_temp, pd1);
            SAVE_ARGB1(x_temp1, pd2);
        }
        /* 16 pixels - 8 byte chroma alignment */
        if (iW <= width - 8) {
            iTemp = *((int32_t *)pm_u);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_u = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_u = (__m64 *) (((int32_t *)pm_u) + 1);

            iTemp = *((int32_t *)pm_v);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_v = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_v = (__m64 *) (((int32_t *)pm_v) + 1);

            x_temp = _mm_loadl_epi64(px_y1);
            px_y1 = (__m128i *) (((__m64 *)px_y1) + 1);
            x_temp1 = _mm_loadl_epi64(px_y2);
            px_y2 = (__m128i *) (((__m64 *)px_y2) + 1);
            x_out = _mm_unpacklo_epi16(x_temp, x_temp1);
            x_y1 = _mm_unpacklo_epi8(x_zero, x_out);
            x_y2 = _mm_unpackhi_epi8(x_zero, x_out);

            /* pre calc x_[r/g/b][12] */
            x_u0 = _mm_mulhi_epu16(x_u, x_c1);
            x_b = _mm_add_epi16(x_u0, x_coff0);

            x_u1 = _mm_mulhi_epu16(x_u, x_c4);
            x_v1 = _mm_mulhi_epu16(x_v, x_c5);
            x_temp = _mm_add_epi16(x_u1, x_v1);
            x_g = _mm_sub_epi16(x_coff1, x_temp);

            x_v2 = _mm_mulhi_epu16(x_v, x_c8);
            x_r = _mm_add_epi16(x_v2, x_coff2);

            x_y1 = _mm_mulhi_epu16(x_y1, x_c0);
            x_y2 = _mm_mulhi_epu16(x_y2, x_c0);

            /* x_b[1/2] */
            x_bak = _mm_unpacklo_epi16(x_b, x_b);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_b1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_b2 = _mm_add_epi16(x_y2, x_temp);

            /* x_g[1/2] */
            x_bak = _mm_unpacklo_epi16(x_g, x_g);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_g1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_g2 = _mm_add_epi16(x_y2, x_temp);

            /* x_r[1/2] */
            x_bak = _mm_unpacklo_epi16(x_r, x_r);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_r1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_r2 = _mm_add_epi16(x_y2, x_temp);

            x_b1 = _mm_srai_epi16(x_b1, 5);
            x_b2 = _mm_srai_epi16(x_b2, 5);
            x_g1 = _mm_srai_epi16(x_g1, 5);
            x_g2 = _mm_srai_epi16(x_g2, 5);
            x_r1 = _mm_srai_epi16(x_r1, 5);
            x_r2 = _mm_srai_epi16(x_r2, 5);

            /* pack: 16=>8 */
            x_b = _mm_packus_epi16(x_b1, x_b2);
            x_r = _mm_packus_epi16(x_r1, x_r2);
            x_g = _mm_packus_epi16(x_g1, x_g2);

            /* create argb sequences */
            x_arl = _mm_unpacklo_epi8(x_aa, x_r);
            x_arh = _mm_unpackhi_epi8(x_aa, x_r);
            x_gbl = _mm_unpacklo_epi8(x_g, x_b);
            x_gbh = _mm_unpackhi_epi8(x_g, x_b);

            x_argbl = _mm_unpacklo_epi16(x_arl, x_gbl);
            x_argbh = _mm_unpackhi_epi16(x_arl, x_gbl);
            x_temp = _mm_unpacklo_epi64(x_argbl, x_argbh);
            x_temp1 = _mm_unpackhi_epi64(x_argbl, x_argbh);
            SAVE_ARGB1(x_temp, pd1);
            SAVE_ARGB1(x_temp1, pd2);

            x_argbl = _mm_unpacklo_epi16(x_arh, x_gbh);
            x_argbh = _mm_unpackhi_epi16(x_arh, x_gbh);
            x_temp = _mm_unpacklo_epi64(x_argbl, x_argbh);
            x_temp1 = _mm_unpackhi_epi64(x_argbl, x_argbh);
            SAVE_ARGB1(x_temp, pd1);
            SAVE_ARGB1(x_temp1, pd2);

            iW += 8;
        }

        /* 8 pixels */
        if (iW <= width - 4) {
            iTemp = *((uint16_t *)pm_u);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_u = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_u = (__m64 *) (((uint16_t *)pm_u) + 1);
            iTemp = *((uint16_t *)pm_v);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_v = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_v = (__m64 *) (((uint16_t *)pm_v) + 1);

            iTemp = *((int32_t *)px_y1);
            px_y1 = (__m128i *) (((int32_t *)px_y1) + 1);
            x_temp = _mm_cvtsi32_si128(iTemp);
            iTemp = *((int32_t *)px_y2);
            px_y2 = (__m128i *) (((int32_t *)px_y2) + 1);
            x_temp1 = _mm_cvtsi32_si128(iTemp);
            x_out = _mm_unpacklo_epi16(x_temp, x_temp1);
            x_y1 = _mm_unpacklo_epi8(x_zero, x_out);

            /* pre calc x_[r/g/b][1] */
            x_u0 = _mm_mulhi_epu16(x_u, x_c1);
            x_b = _mm_add_epi16(x_u0, x_coff0);

            x_u1 = _mm_mulhi_epu16(x_u, x_c4);
            x_v1 = _mm_mulhi_epu16(x_v, x_c5);
            x_temp = _mm_add_epi16(x_u1, x_v1);
            x_g = _mm_sub_epi16(x_coff1, x_temp);

            x_v2 = _mm_mulhi_epu16(x_v, x_c8);
            x_r = _mm_add_epi16(x_v2, x_coff2);

            x_y1 = _mm_mulhi_epu16(x_y1, x_c0);
            x_y2 = _mm_mulhi_epu16(x_y2, x_c0);

            /* x_b[1] */
            x_bak = _mm_unpacklo_epi16(x_b, x_b);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_b1 = _mm_add_epi16(x_y1, x_temp);

            /* x_g[1] */
            x_bak = _mm_unpacklo_epi16(x_g, x_g);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_g1 = _mm_add_epi16(x_y1, x_temp);

            /* x_r[1] */
            x_bak = _mm_unpacklo_epi16(x_r, x_r);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_r1 = _mm_add_epi16(x_y1, x_temp);

            x_b1 = _mm_srai_epi16(x_b1, 5);
            x_g1 = _mm_srai_epi16(x_g1, 5);
            x_r1 = _mm_srai_epi16(x_r1, 5);

            /* pack: 16=>8 */
            x_b = _mm_packus_epi16(x_b1, x_zero);
            x_r = _mm_packus_epi16(x_r1, x_zero);
            x_g = _mm_packus_epi16(x_g1, x_zero);

            /* create argb sequences */
            x_arl = _mm_unpacklo_epi8(x_aa, x_r);
            x_gbl = _mm_unpacklo_epi8(x_g, x_b);

            x_argbl = _mm_unpacklo_epi16(x_arl, x_gbl);
            x_argbh = _mm_unpackhi_epi16(x_arl, x_gbl);
            x_temp = _mm_unpacklo_epi64(x_argbl, x_argbh);
            x_temp1 = _mm_unpackhi_epi64(x_argbl, x_argbh);
            SAVE_ARGB1(x_temp, pd1);
            SAVE_ARGB1(x_temp1, pd2);

            iW += 4;
        }

        /* 4 pixels */
        if (iW <= width - 2) {
            /* load y u v, and expand */
            iu = *((uint8_t *)pm_u);
            pm_u = (__m64 *) (((uint8_t *)pm_u) + 1);
            iv = *((uint8_t *)pm_v);
            pm_v = (__m64 *) (((uint8_t *)pm_v) + 1);

            iTemp = (*((uint16_t *)px_y1)  & 0xffff) | (*((uint16_t *)px_y2)<<16);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_y1 = _mm_unpacklo_epi8(x_zero, x_temp);
            px_y1 = (__m128i *) (((uint16_t *)px_y1) + 1);
            px_y2 = (__m128i *) (((uint16_t *)px_y2) + 1);

            /* pre-calc d[r/g/b][1] */
            iu0 = (iu * ic1) >> 8;
            ib = icoff0 + iu0;

            iu1 = (iu * ic4) >> 8;
            iv1 = (iv * ic5) >> 8;
            iTemp = iu1 + iv1;
            ig = icoff1 - iTemp;

            iv2 = (iv * ic8) >> 8;
            ir = iv2 + icoff2;

            x_y1 = _mm_mulhi_epu16(x_y1, x_c0);

            /* db1 */
            x_temp = _mm_set1_epi16(ib);
            x_b1 = _mm_add_epi16(x_y1, x_temp);

            /* dg1 */
            x_temp = _mm_set1_epi16(ig);
            x_g1 = _mm_add_epi16(x_y1, x_temp);

            /* dr1 */
            x_temp = _mm_set1_epi16(ir);
            x_r1 = _mm_add_epi16(x_y1, x_temp);

            x_b1 = _mm_srai_epi16(x_b1, 5);
            x_g1 = _mm_srai_epi16(x_g1, 5);
            x_r1 = _mm_srai_epi16(x_r1, 5);

            /* pack: 16=>8 */
            x_b = _mm_packus_epi16(x_b1, x_zero);
            x_r = _mm_packus_epi16(x_r1, x_zero);
            x_g = _mm_packus_epi16(x_g1, x_zero);

            /* create argb sequences */
            x_arl = _mm_unpacklo_epi8(x_aa, x_r);
            x_gbl = _mm_unpacklo_epi8(x_g, x_b);

            /* lower half of darl & dgbl */
            x_argbl = _mm_unpacklo_epi16(x_arl, x_gbl);
            _mm_storel_epi64((__m128i*)pd1, x_argbl);
            _mm_storeh_epi64((__m128i*)pd2, x_argbl);

            pd1 += 8;
            pd2 += 8;
        }

        pY1 += (2 * y_stride);
        pY2 += (2 * y_stride);
        pU += u_stride;
        pV += v_stride;
        pD1 += (2 * argb_stride);
        pD2 += (2 * argb_stride);
    }

    return 0;
}

int ColorConvert_YCbCr420p_to_BGRA32(
                                     uint8_t *bgra,
                                     int32_t bgra_stride,
                                     int32_t width,
                                     int32_t height,
                                     const uint8_t *y,
                                     const uint8_t *v,
                                     const uint8_t *u,
                                     const uint8_t *a,
                                     int32_t y_stride,
                                     int32_t v_stride,
                                     int32_t u_stride,
                                     int32_t a_stride)
{
    /* 1.1644  * 8192 */
    const __m128i x_c0 = _mm_set1_epi16(0x2543);

    /* 2.0184  * 8192 */
    const __m128i x_c1 = _mm_set1_epi16(0x4097);
    const int32_t ic1 = 0x4097;

    /* abs( -0.3920 * 8192 ) */
    const __m128i x_c4 = _mm_set1_epi16(0xc8b);
    const int32_t ic4 = 0xc8b;

    /* abs( -0.8132 * 8192 ) */
    const __m128i x_c5 = _mm_set1_epi16(0x1a06);
    const int32_t ic5 = 0x1a06;

    /* 1.5966  * 8192 */
    const __m128i x_c8 = _mm_set1_epi16(0x3317);
    const int32_t ic8 = 0x3317;

    /* -276.9856 * 32 */
    const __m128i x_coff0 = _mm_set1_epi16(0xdd60);
    const int32_t icoff0 = (int32_t)0xffffdd60;

    /* 135.6352  * 32 */
    const __m128i x_coff1 = _mm_set1_epi16(0x10f4);
    const int32_t icoff1 = 0x10f4;

    /* -222.9952 * 32 */
    const __m128i x_coff2 = _mm_set1_epi16(0xe420);
    const int32_t icoff2 = (int32_t)0xffffe420;

    const __m128i x_zero = _mm_setzero_si128();
    const __m128i x_one = _mm_set1_epi16(0x0001);
    //    const __m128i x_aa = _mm_set1_epi8(0xff);

    int32_t jH, iW;
    int32_t iu, iv, ig, ir, ib, iTemp;
    int32_t iu0, iu1, iv1, iv2;
    __m128i x_u0, x_u1, x_v1, x_v2, x_temp, x_out, x_bak, x_temp1;
    __m128i x_u, x_v, x_y1, x_y2, x_y3, x_y4, x_a1, x_a2;
    __m128i x_r1, x_r2, x_r3, x_r4, x_g1, x_g2, x_g3, x_g4;
    __m128i x_b1, x_b2, x_b3, x_b4, x_r, x_g, x_b;
    __m128i x_ral, x_rah, x_bgl, x_bgh, x_bgrah, x_bgral;
    __m128i *px_y1, *px_y2, *px_a1, *px_a2;
    __m64 *pm_u, *pm_v;
    uint8_t *pY1, *pY2, *pU, *pV, *pA1, *pA2, *pD1, *pD2, *pd1, *pd2;

    __m128i (*load_si128) (const __m128i*);
    if (((intptr_t)y % 16) != 0 || ((intptr_t)u % 16) != 0 || ((intptr_t)v % 16) != 0 || ((intptr_t)a % 16) != 0 || (y_stride % 16) != 0 || (u_stride % 16) != 0 || (v_stride % 16) != 0 || (a_stride % 16) != 0)
        load_si128 = &inline_loadu_si128;
    else
        load_si128 = &inline_load_si128;

#if 0
    if ((intptr_t)bgra & 0xf)
        fprintf(stderr, "bgra is unaligned! %p\n", bgra);
    if ((intptr_t)y & 0xf)
        fprintf(stderr, "luma is unaligned! %p\n", y);
    if ((intptr_t)v & 0xf)
        fprintf(stderr, "Cb is unaligned! %p\n", v);
    if ((intptr_t)u & 0xf)
        fprintf(stderr, "Cr is unaligned! %p\n", u);
    if ((intptr_t)a & 0xf)
        fprintf(stderr, "alpha is unaligned! %p\n", a);
#endif
    pY1 = (uint8_t*)y;
    pY2 = (uint8_t*)y + y_stride;
    pU = (uint8_t*)u;
    pV = (uint8_t*)v;
    pA1 = (uint8_t*)a;
    pA2 = (uint8_t*)a + a_stride;
    pD1 = (uint8_t*)bgra;
    pD2 = (uint8_t*)bgra + bgra_stride;

    for (jH = 0; jH < (height >> 1); jH++) {
        px_y1 = (__m128i*)pY1;
        px_y2 = (__m128i*)pY2;
        pm_u = (__m64*)pU;
        pm_v = (__m64*)pV;
        px_a1 = (__m128i*)pA1;
        px_a2 = (__m128i*)pA2;
        pd1 = pD1;
        pd2 = pD2;
        iW = 0;

        /* 32 pixels */
        for (; iW <= width - 16; iW += 16) {
            x_temp = _mm_loadl_epi64((__m128i*)pm_u);
            x_u = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_u++;
            x_temp = _mm_loadl_epi64((__m128i*)pm_v);
            x_v = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_v++;
            x_temp = load_si128(px_y1);
            px_y1++;
            x_temp1 = load_si128(px_y2);
            px_y2++;
            x_out = _mm_unpacklo_epi16(x_temp, x_temp1);
            x_y1 = _mm_unpacklo_epi8(x_zero, x_out);
            x_y2 = _mm_unpackhi_epi8(x_zero, x_out);
            x_out = _mm_unpackhi_epi16(x_temp, x_temp1);
            x_y3 = _mm_unpacklo_epi8(x_zero, x_out);
            x_y4 = _mm_unpackhi_epi8(x_zero, x_out);
            x_temp = load_si128(px_a1);             // top row (16 pix)
            px_a1++;
            x_temp1 = load_si128(px_a2);            // bottom row (16 pix)
            px_a2++;
            x_a1 = _mm_unpacklo_epi16(x_temp, x_temp1); // interleave top+bottom
            x_a2 = _mm_unpackhi_epi16(x_temp, x_temp1);

            /* pre calc x_[r/g/b][12] */
            x_u0 = _mm_mulhi_epu16(x_u, x_c1);
            x_b = _mm_add_epi16(x_u0, x_coff0);

            x_u1 = _mm_mulhi_epu16(x_u, x_c4);
            x_v1 = _mm_mulhi_epu16(x_v, x_c5);
            x_temp = _mm_add_epi16(x_u1, x_v1);
            x_g = _mm_sub_epi16(x_coff1, x_temp);

            x_v2 = _mm_mulhi_epu16(x_v, x_c8);
            x_r = _mm_add_epi16(x_v2, x_coff2);

            x_y1 = _mm_mulhi_epu16(x_y1, x_c0);
            x_y2 = _mm_mulhi_epu16(x_y2, x_c0);
            x_y3 = _mm_mulhi_epu16(x_y3, x_c0);
            x_y4 = _mm_mulhi_epu16(x_y4, x_c0);

            /* x_b[1/2/3/4] */
            x_bak = _mm_unpacklo_epi16(x_b, x_b);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_b1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_b2 = _mm_add_epi16(x_y2, x_temp);

            x_bak = _mm_unpackhi_epi16(x_b, x_b);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_b3 = _mm_add_epi16(x_y3, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_b4 = _mm_add_epi16(x_y4, x_temp);

            /* x_g[1/2/3/4] */
            x_bak = _mm_unpacklo_epi16(x_g, x_g);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_g1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_g2 = _mm_add_epi16(x_y2, x_temp);

            x_bak = _mm_unpackhi_epi16(x_g, x_g);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_g3 = _mm_add_epi16(x_y3, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_g4 = _mm_add_epi16(x_y4, x_temp);

            /* x_r[1/2/3/4] */
            x_bak = _mm_unpacklo_epi16(x_r, x_r);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_r1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_r2 = _mm_add_epi16(x_y2, x_temp);

            x_bak = _mm_unpackhi_epi16(x_r, x_r);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_r3 = _mm_add_epi16(x_y3, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_r4 = _mm_add_epi16(x_y4, x_temp);

            x_b1 = _mm_srai_epi16(x_b1, 5);
            x_b2 = _mm_srai_epi16(x_b2, 5);
            x_b3 = _mm_srai_epi16(x_b3, 5);
            x_b4 = _mm_srai_epi16(x_b4, 5);
            x_g1 = _mm_srai_epi16(x_g1, 5);
            x_g2 = _mm_srai_epi16(x_g2, 5);
            x_g3 = _mm_srai_epi16(x_g3, 5);
            x_g4 = _mm_srai_epi16(x_g4, 5);
            x_r1 = _mm_srai_epi16(x_r1, 5);
            x_r2 = _mm_srai_epi16(x_r2, 5);
            x_r3 = _mm_srai_epi16(x_r3, 5);
            x_r4 = _mm_srai_epi16(x_r4, 5);

            /* pack: 16=>8 */
            x_b = _mm_packus_epi16(x_b1, x_b2);
            PREMULTIPLY_ALPHA(x_b, x_a1, x_b2);
            x_b1 = _mm_packus_epi16(x_b3, x_b4);
            PREMULTIPLY_ALPHA(x_b1, x_a2, x_b2);
            x_r = _mm_packus_epi16(x_r1, x_r2);
            PREMULTIPLY_ALPHA(x_r, x_a1, x_r2);
            x_r1 = _mm_packus_epi16(x_r3, x_r4);
            PREMULTIPLY_ALPHA(x_r1, x_a2, x_r2);
            x_g = _mm_packus_epi16(x_g1, x_g2);
            PREMULTIPLY_ALPHA(x_g, x_a1, x_g2);
            x_g1 = _mm_packus_epi16(x_g3, x_g4);
            PREMULTIPLY_ALPHA(x_g1, x_a2, x_g2);

            /* create bgra sequences */
            x_bgl = _mm_unpacklo_epi8(x_b, x_g);
            x_bgh = _mm_unpackhi_epi8(x_b, x_g);
            x_ral = _mm_unpacklo_epi8(x_r, x_a1);
            x_rah = _mm_unpackhi_epi8(x_r, x_a1);

            x_bgral = _mm_unpacklo_epi16(x_bgl, x_ral);
            x_bgrah = _mm_unpackhi_epi16(x_bgl, x_ral);
            x_temp = _mm_unpacklo_epi64(x_bgral, x_bgrah);
            x_temp1 = _mm_unpackhi_epi64(x_bgral, x_bgrah);
            SAVE_BGRA1(x_temp, pd1);
            SAVE_BGRA1(x_temp1, pd2);

            x_bgral = _mm_unpacklo_epi16(x_bgh, x_rah);
            x_bgrah = _mm_unpackhi_epi16(x_bgh, x_rah);
            x_temp = _mm_unpacklo_epi64(x_bgral, x_bgrah);
            x_temp1 = _mm_unpackhi_epi64(x_bgral, x_bgrah);
            SAVE_BGRA1(x_temp, pd1);
            SAVE_BGRA1(x_temp1, pd2);

            /* create bgra sequences */
            x_bgl = _mm_unpacklo_epi8(x_b1, x_g1);
            x_bgh = _mm_unpackhi_epi8(x_b1, x_g1);
            x_ral = _mm_unpacklo_epi8(x_r1, x_a2);
            x_rah = _mm_unpackhi_epi8(x_r1, x_a2);

            x_bgral = _mm_unpacklo_epi16(x_bgl, x_ral);
            x_bgrah = _mm_unpackhi_epi16(x_bgl, x_ral);
            x_temp = _mm_unpacklo_epi64(x_bgral, x_bgrah);
            x_temp1 = _mm_unpackhi_epi64(x_bgral, x_bgrah);
            SAVE_BGRA1(x_temp, pd1);
            SAVE_BGRA1(x_temp1, pd2);

            x_bgral = _mm_unpacklo_epi16(x_bgh, x_rah);
            x_bgrah = _mm_unpackhi_epi16(x_bgh, x_rah);
            x_temp = _mm_unpacklo_epi64(x_bgral, x_bgrah);
            x_temp1 = _mm_unpackhi_epi64(x_bgral, x_bgrah);
            SAVE_BGRA1(x_temp, pd1);
            SAVE_BGRA1(x_temp1, pd2);
        }
        /* 16 pixels - 8 byte chroma alignment */
        if (iW <= width - 8) {
            iTemp = *((int32_t *)pm_u);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_u = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_u = (__m64 *) (((int32_t *)pm_u) + 1);

            iTemp = *((int32_t *)pm_v);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_v = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_v = (__m64 *) (((int32_t *)pm_v) + 1);

            x_temp = _mm_loadl_epi64(px_y1);
            px_y1 = (__m128i *) (((__m64 *)px_y1) + 1);
            x_temp1 = _mm_loadl_epi64(px_y2);
            px_y2 = (__m128i *) (((__m64 *)px_y2) + 1);
            x_out = _mm_unpacklo_epi16(x_temp, x_temp1);
            x_y1 = _mm_unpacklo_epi8(x_zero, x_out);
            x_y2 = _mm_unpackhi_epi8(x_zero, x_out);

            x_temp = _mm_loadl_epi64(px_a1);
            px_a1 = (__m128i *) (((__m64 *)px_a1) + 1);
            x_temp1 = _mm_loadl_epi64(px_a2);
            px_a2 = (__m128i *) (((__m64 *)px_a2) + 1);
            x_a1 = _mm_unpacklo_epi16(x_temp, x_temp1);

            /* pre calc x_[r/g/b][12] */
            x_u0 = _mm_mulhi_epu16(x_u, x_c1);
            x_b = _mm_add_epi16(x_u0, x_coff0);

            x_u1 = _mm_mulhi_epu16(x_u, x_c4);
            x_v1 = _mm_mulhi_epu16(x_v, x_c5);
            x_temp = _mm_add_epi16(x_u1, x_v1);
            x_g = _mm_sub_epi16(x_coff1, x_temp);

            x_v2 = _mm_mulhi_epu16(x_v, x_c8);
            x_r = _mm_add_epi16(x_v2, x_coff2);

            x_y1 = _mm_mulhi_epu16(x_y1, x_c0);
            x_y2 = _mm_mulhi_epu16(x_y2, x_c0);

            /* x_b[1/2] */
            x_bak = _mm_unpacklo_epi16(x_b, x_b);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_b1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_b2 = _mm_add_epi16(x_y2, x_temp);

            /* x_g[1/2] */
            x_bak = _mm_unpacklo_epi16(x_g, x_g);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_g1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_g2 = _mm_add_epi16(x_y2, x_temp);

            /* x_r[1/2] */
            x_bak = _mm_unpacklo_epi16(x_r, x_r);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_r1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_r2 = _mm_add_epi16(x_y2, x_temp);

            x_b1 = _mm_srai_epi16(x_b1, 5);
            x_b2 = _mm_srai_epi16(x_b2, 5);
            x_g1 = _mm_srai_epi16(x_g1, 5);
            x_g2 = _mm_srai_epi16(x_g2, 5);
            x_r1 = _mm_srai_epi16(x_r1, 5);
            x_r2 = _mm_srai_epi16(x_r2, 5);

            /* pack: 16=>8 */
            x_b = _mm_packus_epi16(x_b1, x_b2);
            PREMULTIPLY_ALPHA(x_b, x_a1, x_b2);
            x_r = _mm_packus_epi16(x_r1, x_r2);
            PREMULTIPLY_ALPHA(x_r, x_a1, x_r2);
            x_g = _mm_packus_epi16(x_g1, x_g2);
            PREMULTIPLY_ALPHA(x_g, x_a1, x_g2);

            /* create bgra sequences */
            x_bgl = _mm_unpacklo_epi8(x_b, x_g);
            x_bgh = _mm_unpackhi_epi8(x_b, x_g);
            x_ral = _mm_unpacklo_epi8(x_r, x_a1);
            x_rah = _mm_unpackhi_epi8(x_r, x_a1);

            x_bgral = _mm_unpacklo_epi16(x_bgl, x_ral);
            x_bgrah = _mm_unpackhi_epi16(x_bgl, x_ral);
            x_temp = _mm_unpacklo_epi64(x_bgral, x_bgrah);
            x_temp1 = _mm_unpackhi_epi64(x_bgral, x_bgrah);
            SAVE_BGRA1(x_temp, pd1);
            SAVE_BGRA1(x_temp1, pd2);

            x_bgral = _mm_unpacklo_epi16(x_bgh, x_rah);
            x_bgrah = _mm_unpackhi_epi16(x_bgh, x_rah);
            x_temp = _mm_unpacklo_epi64(x_bgral, x_bgrah);
            x_temp1 = _mm_unpackhi_epi64(x_bgral, x_bgrah);
            SAVE_BGRA1(x_temp, pd1);
            SAVE_BGRA1(x_temp1, pd2);

            iW += 8;
        }

        /* 8 pixels */
        if (iW <= width - 4) {
            iTemp = *((uint16_t *)pm_u);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_u = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_u = (__m64 *) (((uint16_t *)pm_u) + 1);
            iTemp = *((uint16_t *)pm_v);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_v = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_v = (__m64 *) (((uint16_t *)pm_v) + 1);

            iTemp = *((int32_t *)px_y1);
            px_y1 = (__m128i *) (((int32_t *)px_y1) + 1);
            x_temp = _mm_cvtsi32_si128(iTemp);
            iTemp = *((int32_t *)px_y2);
            px_y2 = (__m128i *) (((int32_t *)px_y2) + 1);
            x_temp1 = _mm_cvtsi32_si128(iTemp);
            x_out = _mm_unpacklo_epi16(x_temp, x_temp1);
            x_y1 = _mm_unpacklo_epi8(x_zero, x_out);

            iTemp = *((int32_t *)px_a1);
            px_a1 = (__m128i *) (((int32_t *)px_a1) + 1);
            x_temp = _mm_cvtsi32_si128(iTemp);
            iTemp = *((int32_t *)px_a2);
            px_a2 = (__m128i *) (((int32_t *)px_a2) + 1);
            x_temp1 = _mm_cvtsi32_si128(iTemp);
            x_a1 = _mm_unpacklo_epi16(x_temp, x_temp1);

            /* pre calc x_[r/g/b][1] */
            x_u0 = _mm_mulhi_epu16(x_u, x_c1);
            x_b = _mm_add_epi16(x_u0, x_coff0);

            x_u1 = _mm_mulhi_epu16(x_u, x_c4);
            x_v1 = _mm_mulhi_epu16(x_v, x_c5);
            x_temp = _mm_add_epi16(x_u1, x_v1);
            x_g = _mm_sub_epi16(x_coff1, x_temp);

            x_v2 = _mm_mulhi_epu16(x_v, x_c8);
            x_r = _mm_add_epi16(x_v2, x_coff2);

            x_y1 = _mm_mulhi_epu16(x_y1, x_c0);
            x_y2 = _mm_mulhi_epu16(x_y2, x_c0);

            /* x_b[1] */
            x_bak = _mm_unpacklo_epi16(x_b, x_b);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_b1 = _mm_add_epi16(x_y1, x_temp);

            /* x_g[1] */
            x_bak = _mm_unpacklo_epi16(x_g, x_g);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_g1 = _mm_add_epi16(x_y1, x_temp);

            /* x_r[1] */
            x_bak = _mm_unpacklo_epi16(x_r, x_r);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_r1 = _mm_add_epi16(x_y1, x_temp);

            x_b1 = _mm_srai_epi16(x_b1, 5);
            x_g1 = _mm_srai_epi16(x_g1, 5);
            x_r1 = _mm_srai_epi16(x_r1, 5);

            /* pack: 16=>8 */
            x_b = _mm_packus_epi16(x_b1, x_zero);
            PREMULTIPLY_ALPHA(x_b, x_a1, x_b2);
            x_r = _mm_packus_epi16(x_r1, x_zero);
            PREMULTIPLY_ALPHA(x_r, x_a1, x_r2);
            x_g = _mm_packus_epi16(x_g1, x_zero);
            PREMULTIPLY_ALPHA(x_g, x_a1, x_g2);

            /* create bgra sequences */
            x_bgl = _mm_unpacklo_epi8(x_b, x_g);
            x_ral = _mm_unpacklo_epi8(x_r, x_a1);

            x_bgral = _mm_unpacklo_epi16(x_bgl, x_ral);
            x_bgrah = _mm_unpackhi_epi16(x_bgl, x_ral);
            x_temp = _mm_unpacklo_epi64(x_bgral, x_bgrah);
            x_temp1 = _mm_unpackhi_epi64(x_bgral, x_bgrah);
            SAVE_BGRA1(x_temp, pd1);
            SAVE_BGRA1(x_temp1, pd2);

            iW += 4;
        }

        /* 4 pixels */
        if (iW <= width - 2) {
            /* load y u v, and expand */
            iu = *((uint8_t *)pm_u);
            pm_u = (__m64 *) (((uint8_t *)pm_u) + 1);
            iv = *((uint8_t *)pm_v);
            pm_v = (__m64 *) (((uint8_t *)pm_v) + 1);

            iTemp = (*((uint16_t *)px_y1)  & 0xffff) | (*((uint16_t *)px_y2)<<16);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_y1 = _mm_unpacklo_epi8(x_zero, x_temp);
            px_y1 = (__m128i *) (((uint16_t *)px_y1) + 1);
            px_y2 = (__m128i *) (((uint16_t *)px_y2) + 1);

            iTemp = (*((uint16_t *)px_a1)  & 0xffff) | (*((uint16_t *)px_a2)<<16);
            x_a1 = _mm_cvtsi32_si128(iTemp);
            px_a1 = (__m128i *) (((uint16_t *)px_a1) + 1);
            px_a2 = (__m128i *) (((uint16_t *)px_a2) + 1);

            /* pre-calc d[r/g/b][1] */
            iu0 = (iu * ic1) >> 8;
            ib = icoff0 + iu0;

            iu1 = (iu * ic4) >> 8;
            iv1 = (iv * ic5) >> 8;
            iTemp = iu1 + iv1;
            ig = icoff1 - iTemp;

            iv2 = (iv * ic8) >> 8;
            ir = iv2 + icoff2;

            x_y1 = _mm_mulhi_epu16(x_y1, x_c0);

            /* db1 */
            x_temp = _mm_set1_epi16(ib);
            x_b1 = _mm_add_epi16(x_y1, x_temp);

            /* dg1 */
            x_temp = _mm_set1_epi16(ig);
            x_g1 = _mm_add_epi16(x_y1, x_temp);

            /* dr1 */
            x_temp = _mm_set1_epi16(ir);
            x_r1 = _mm_add_epi16(x_y1, x_temp);

            x_b1 = _mm_srai_epi16(x_b1, 5);
            x_g1 = _mm_srai_epi16(x_g1, 5);
            x_r1 = _mm_srai_epi16(x_r1, 5);

            /* pack: 16=>8 */
            x_b = _mm_packus_epi16(x_b1, x_zero);
            PREMULTIPLY_ALPHA(x_b, x_a1, x_b2);
            x_r = _mm_packus_epi16(x_r1, x_zero);
            PREMULTIPLY_ALPHA(x_r, x_a1, x_r2);
            x_g = _mm_packus_epi16(x_g1, x_zero);
            PREMULTIPLY_ALPHA(x_g, x_a1, x_g2);

            /* create bgra sequences */
            x_bgl = _mm_unpacklo_epi8(x_b, x_g);
            x_ral = _mm_unpacklo_epi8(x_r, x_a1);

            /* lower half of darl & dgbl */
            x_bgral = _mm_unpacklo_epi16(x_bgl, x_ral);
            _mm_storel_epi64((__m128i*)pd1, x_bgral);
            _mm_storeh_epi64((__m128i*)pd2, x_bgral);

            pd1 += 8;
            pd2 += 8;
        }

        pY1 += (2 * y_stride);
        pY2 += (2 * y_stride);
        pU += u_stride;
        pV += v_stride;
        pA1 += (2 * a_stride);
        pA2 += (2 * a_stride);
        pD1 += (2 * bgra_stride);
        pD2 += (2 * bgra_stride);
    }

    return 0;
}

int ColorConvert_YCbCr420p_to_BGRA32_no_alpha(
                                              uint8_t *bgra,
                                              int32_t bgra_stride,
                                              int32_t width,
                                              int32_t height,
                                              const uint8_t *y,
                                              const uint8_t *v,
                                              const uint8_t *u,
                                              int32_t y_stride,
                                              int32_t v_stride,
                                              int32_t u_stride)
{
    /* 1.1644  * 8192 */
    const __m128i x_c0 = _mm_set1_epi16(0x2543);

    /* 2.0184  * 8192 */
    const __m128i x_c1 = _mm_set1_epi16(0x4097);
    const int32_t ic1 = 0x4097;

    /* abs( -0.3920 * 8192 ) */
    const __m128i x_c4 = _mm_set1_epi16(0xc8b);
    const int32_t ic4 = 0xc8b;

    /* abs( -0.8132 * 8192 ) */
    const __m128i x_c5 = _mm_set1_epi16(0x1a06);
    const int32_t ic5 = 0x1a06;

    /* 1.5966  * 8192 */
    const __m128i x_c8 = _mm_set1_epi16(0x3317);
    const int32_t ic8 = 0x3317;

    /* -276.9856 * 32 */
    const __m128i x_coff0 = _mm_set1_epi16(0xdd60);
    const int32_t icoff0 = (int32_t)0xffffdd60;

    /* 135.6352  * 32 */
    const __m128i x_coff1 = _mm_set1_epi16(0x10f4);
    const int32_t icoff1 = 0x10f4;

    /* -222.9952 * 32 */
    const __m128i x_coff2 = _mm_set1_epi16(0xe420);
    const int32_t icoff2 = (int32_t)0xffffe420;

    const __m128i x_zero = _mm_setzero_si128();
    const __m128i x_aa = _mm_set1_epi8(0xff);

    int32_t jH, iW;
    int32_t iu, iv, ig, ir, ib, iTemp;
    int32_t iu0, iu1, iv1, iv2;
    __m128i x_u0, x_u1, x_v1, x_v2, x_temp, x_out, x_bak, x_temp1;
    __m128i x_u, x_v, x_y1, x_y2, x_y3, x_y4;
    __m128i x_r1, x_r2, x_r3, x_r4, x_g1, x_g2, x_g3, x_g4;
    __m128i x_b1, x_b2, x_b3, x_b4, x_r, x_g, x_b;
    __m128i x_ral, x_rah, x_bgl, x_bgh, x_bgrah, x_bgral;
    __m128i *px_y1, *px_y2;
    __m64 *pm_u, *pm_v;
    uint8_t *pY1, *pY2, *pU, *pV, *pD1, *pD2, *pd1, *pd2;

    __m128i (*load_si128) (const __m128i*);
    if (((intptr_t)y % 16) != 0 || ((intptr_t)u % 16) != 0 || ((intptr_t)v % 16) != 0 || (y_stride % 16) != 0 || (u_stride % 16) != 0 || (v_stride % 16) != 0)
        load_si128 = &inline_loadu_si128;
    else
        load_si128 = &inline_load_si128;

    pY1 = (uint8_t*)y;
    pY2 = (uint8_t*)y + y_stride;
    pU = (uint8_t*)u;
    pV = (uint8_t*)v;
    pD1 = (uint8_t*)bgra;
    pD2 = (uint8_t*)bgra + bgra_stride;

    for (jH = 0; jH < (height >> 1); jH++) {
        px_y1 = (__m128i*)pY1;
        px_y2 = (__m128i*)pY2;
        pm_u = (__m64*)pU;
        pm_v = (__m64*)pV;
        pd1 = pD1;
        pd2 = pD2;
        iW = 0;

        /* 32 pixels */
        for (; iW <= width - 16; iW += 16) {
            x_temp = _mm_loadl_epi64((__m128i*)pm_u);
            x_u = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_u++;
            x_temp = _mm_loadl_epi64((__m128i*)pm_v);
            x_v = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_v++;
            x_temp = load_si128(px_y1);
            px_y1++;
            x_temp1 = load_si128(px_y2);
            px_y2++;
            x_out = _mm_unpacklo_epi16(x_temp, x_temp1);
            x_y1 = _mm_unpacklo_epi8(x_zero, x_out);
            x_y2 = _mm_unpackhi_epi8(x_zero, x_out);
            x_out = _mm_unpackhi_epi16(x_temp, x_temp1);
            x_y3 = _mm_unpacklo_epi8(x_zero, x_out);
            x_y4 = _mm_unpackhi_epi8(x_zero, x_out);

            /* pre calc x_[r/g/b][12] */
            x_u0 = _mm_mulhi_epu16(x_u, x_c1);
            x_b = _mm_add_epi16(x_u0, x_coff0);

            x_u1 = _mm_mulhi_epu16(x_u, x_c4);
            x_v1 = _mm_mulhi_epu16(x_v, x_c5);
            x_temp = _mm_add_epi16(x_u1, x_v1);
            x_g = _mm_sub_epi16(x_coff1, x_temp);

            x_v2 = _mm_mulhi_epu16(x_v, x_c8);
            x_r = _mm_add_epi16(x_v2, x_coff2);

            x_y1 = _mm_mulhi_epu16(x_y1, x_c0);
            x_y2 = _mm_mulhi_epu16(x_y2, x_c0);
            x_y3 = _mm_mulhi_epu16(x_y3, x_c0);
            x_y4 = _mm_mulhi_epu16(x_y4, x_c0);

            /* x_b[1/2/3/4] */
            x_bak = _mm_unpacklo_epi16(x_b, x_b);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_b1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_b2 = _mm_add_epi16(x_y2, x_temp);

            x_bak = _mm_unpackhi_epi16(x_b, x_b);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_b3 = _mm_add_epi16(x_y3, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_b4 = _mm_add_epi16(x_y4, x_temp);

            /* x_g[1/2/3/4] */
            x_bak = _mm_unpacklo_epi16(x_g, x_g);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_g1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_g2 = _mm_add_epi16(x_y2, x_temp);

            x_bak = _mm_unpackhi_epi16(x_g, x_g);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_g3 = _mm_add_epi16(x_y3, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_g4 = _mm_add_epi16(x_y4, x_temp);

            /* x_r[1/2/3/4] */
            x_bak = _mm_unpacklo_epi16(x_r, x_r);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_r1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_r2 = _mm_add_epi16(x_y2, x_temp);

            x_bak = _mm_unpackhi_epi16(x_r, x_r);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_r3 = _mm_add_epi16(x_y3, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_r4 = _mm_add_epi16(x_y4, x_temp);

            x_b1 = _mm_srai_epi16(x_b1, 5);
            x_b2 = _mm_srai_epi16(x_b2, 5);
            x_b3 = _mm_srai_epi16(x_b3, 5);
            x_b4 = _mm_srai_epi16(x_b4, 5);
            x_g1 = _mm_srai_epi16(x_g1, 5);
            x_g2 = _mm_srai_epi16(x_g2, 5);
            x_g3 = _mm_srai_epi16(x_g3, 5);
            x_g4 = _mm_srai_epi16(x_g4, 5);
            x_r1 = _mm_srai_epi16(x_r1, 5);
            x_r2 = _mm_srai_epi16(x_r2, 5);
            x_r3 = _mm_srai_epi16(x_r3, 5);
            x_r4 = _mm_srai_epi16(x_r4, 5);

            /* pack: 16=>8 */
            x_b = _mm_packus_epi16(x_b1, x_b2);
            x_b1 = _mm_packus_epi16(x_b3, x_b4);
            x_r = _mm_packus_epi16(x_r1, x_r2);
            x_r1 = _mm_packus_epi16(x_r3, x_r4);
            x_g = _mm_packus_epi16(x_g1, x_g2);
            x_g1 = _mm_packus_epi16(x_g3, x_g4);

            /* create bgra sequences */
            x_bgl = _mm_unpacklo_epi8(x_b, x_g);
            x_bgh = _mm_unpackhi_epi8(x_b, x_g);
            x_ral = _mm_unpacklo_epi8(x_r, x_aa);
            x_rah = _mm_unpackhi_epi8(x_r, x_aa);

            x_bgral = _mm_unpacklo_epi16(x_bgl, x_ral);
            x_bgrah = _mm_unpackhi_epi16(x_bgl, x_ral);
            x_temp = _mm_unpacklo_epi64(x_bgral, x_bgrah);
            x_temp1 = _mm_unpackhi_epi64(x_bgral, x_bgrah);
            SAVE_BGRA1(x_temp, pd1);
            SAVE_BGRA1(x_temp1, pd2);

            x_bgral = _mm_unpacklo_epi16(x_bgh, x_rah);
            x_bgrah = _mm_unpackhi_epi16(x_bgh, x_rah);
            x_temp = _mm_unpacklo_epi64(x_bgral, x_bgrah);
            x_temp1 = _mm_unpackhi_epi64(x_bgral, x_bgrah);
            SAVE_BGRA1(x_temp, pd1);
            SAVE_BGRA1(x_temp1, pd2);

            /* create bgra sequences */
            x_bgl = _mm_unpacklo_epi8(x_b1, x_g1);
            x_bgh = _mm_unpackhi_epi8(x_b1, x_g1);
            x_ral = _mm_unpacklo_epi8(x_r1, x_aa);
            x_rah = _mm_unpackhi_epi8(x_r1, x_aa);

            x_bgral = _mm_unpacklo_epi16(x_bgl, x_ral);
            x_bgrah = _mm_unpackhi_epi16(x_bgl, x_ral);
            x_temp = _mm_unpacklo_epi64(x_bgral, x_bgrah);
            x_temp1 = _mm_unpackhi_epi64(x_bgral, x_bgrah);
            SAVE_BGRA1(x_temp, pd1);
            SAVE_BGRA1(x_temp1, pd2);

            x_bgral = _mm_unpacklo_epi16(x_bgh, x_rah);
            x_bgrah = _mm_unpackhi_epi16(x_bgh, x_rah);
            x_temp = _mm_unpacklo_epi64(x_bgral, x_bgrah);
            x_temp1 = _mm_unpackhi_epi64(x_bgral, x_bgrah);
            SAVE_BGRA1(x_temp, pd1);
            SAVE_BGRA1(x_temp1, pd2);
        }
        /* 16 pixels - 8 byte chroma alignment */
        if (iW <= width - 8) {
            iTemp = *((int32_t *)pm_u);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_u = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_u = (__m64 *) (((int32_t *)pm_u) + 1);

            iTemp = *((int32_t *)pm_v);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_v = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_v = (__m64 *) (((int32_t *)pm_v) + 1);

            x_temp = _mm_loadl_epi64(px_y1);
            px_y1 = (__m128i *) (((__m64 *)px_y1) + 1);
            x_temp1 = _mm_loadl_epi64(px_y2);
            px_y2 = (__m128i *) (((__m64 *)px_y2) + 1);
            x_out = _mm_unpacklo_epi16(x_temp, x_temp1);
            x_y1 = _mm_unpacklo_epi8(x_zero, x_out);
            x_y2 = _mm_unpackhi_epi8(x_zero, x_out);

            /* pre calc x_[r/g/b][12] */
            x_u0 = _mm_mulhi_epu16(x_u, x_c1);
            x_b = _mm_add_epi16(x_u0, x_coff0);

            x_u1 = _mm_mulhi_epu16(x_u, x_c4);
            x_v1 = _mm_mulhi_epu16(x_v, x_c5);
            x_temp = _mm_add_epi16(x_u1, x_v1);
            x_g = _mm_sub_epi16(x_coff1, x_temp);

            x_v2 = _mm_mulhi_epu16(x_v, x_c8);
            x_r = _mm_add_epi16(x_v2, x_coff2);

            x_y1 = _mm_mulhi_epu16(x_y1, x_c0);
            x_y2 = _mm_mulhi_epu16(x_y2, x_c0);

            /* x_b[1/2] */
            x_bak = _mm_unpacklo_epi16(x_b, x_b);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_b1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_b2 = _mm_add_epi16(x_y2, x_temp);

            /* x_g[1/2] */
            x_bak = _mm_unpacklo_epi16(x_g, x_g);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_g1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_g2 = _mm_add_epi16(x_y2, x_temp);

            /* x_r[1/2] */
            x_bak = _mm_unpacklo_epi16(x_r, x_r);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_r1 = _mm_add_epi16(x_y1, x_temp);
            x_temp = _mm_unpackhi_epi32(x_bak, x_bak);
            x_r2 = _mm_add_epi16(x_y2, x_temp);

            x_b1 = _mm_srai_epi16(x_b1, 5);
            x_b2 = _mm_srai_epi16(x_b2, 5);
            x_g1 = _mm_srai_epi16(x_g1, 5);
            x_g2 = _mm_srai_epi16(x_g2, 5);
            x_r1 = _mm_srai_epi16(x_r1, 5);
            x_r2 = _mm_srai_epi16(x_r2, 5);

            /* pack: 16=>8 */
            x_b = _mm_packus_epi16(x_b1, x_b2);
            x_r = _mm_packus_epi16(x_r1, x_r2);
            x_g = _mm_packus_epi16(x_g1, x_g2);

            /* create bgra sequences */
            x_bgl = _mm_unpacklo_epi8(x_b, x_g);
            x_bgh = _mm_unpackhi_epi8(x_b, x_g);
            x_ral = _mm_unpacklo_epi8(x_r, x_aa);
            x_rah = _mm_unpackhi_epi8(x_r, x_aa);

            x_bgral = _mm_unpacklo_epi16(x_bgl, x_ral);
            x_bgrah = _mm_unpackhi_epi16(x_bgl, x_ral);
            x_temp = _mm_unpacklo_epi64(x_bgral, x_bgrah);
            x_temp1 = _mm_unpackhi_epi64(x_bgral, x_bgrah);
            SAVE_BGRA1(x_temp, pd1);
            SAVE_BGRA1(x_temp1, pd2);

            x_bgral = _mm_unpacklo_epi16(x_bgh, x_rah);
            x_bgrah = _mm_unpackhi_epi16(x_bgh, x_rah);
            x_temp = _mm_unpacklo_epi64(x_bgral, x_bgrah);
            x_temp1 = _mm_unpackhi_epi64(x_bgral, x_bgrah);
            SAVE_BGRA1(x_temp, pd1);
            SAVE_BGRA1(x_temp1, pd2);

            iW += 8;
        }

        /* 8 pixels */
        if (iW <= width - 4) {
            iTemp = *((uint16_t *)pm_u);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_u = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_u = (__m64 *) (((uint16_t *)pm_u) + 1);
            iTemp = *((uint16_t *)pm_v);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_v = _mm_unpacklo_epi8(x_zero, x_temp);
            pm_v = (__m64 *) (((uint16_t *)pm_v) + 1);

            iTemp = *((int32_t *)px_y1);
            px_y1 = (__m128i *) (((int32_t *)px_y1) + 1);
            x_temp = _mm_cvtsi32_si128(iTemp);
            iTemp = *((int32_t *)px_y2);
            px_y2 = (__m128i *) (((int32_t *)px_y2) + 1);
            x_temp1 = _mm_cvtsi32_si128(iTemp);
            x_out = _mm_unpacklo_epi16(x_temp, x_temp1);
            x_y1 = _mm_unpacklo_epi8(x_zero, x_out);

            /* pre calc x_[r/g/b][1] */
            x_u0 = _mm_mulhi_epu16(x_u, x_c1);
            x_b = _mm_add_epi16(x_u0, x_coff0);

            x_u1 = _mm_mulhi_epu16(x_u, x_c4);
            x_v1 = _mm_mulhi_epu16(x_v, x_c5);
            x_temp = _mm_add_epi16(x_u1, x_v1);
            x_g = _mm_sub_epi16(x_coff1, x_temp);

            x_v2 = _mm_mulhi_epu16(x_v, x_c8);
            x_r = _mm_add_epi16(x_v2, x_coff2);

            x_y1 = _mm_mulhi_epu16(x_y1, x_c0);
            x_y2 = _mm_mulhi_epu16(x_y2, x_c0);

            /* x_b[1] */
            x_bak = _mm_unpacklo_epi16(x_b, x_b);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_b1 = _mm_add_epi16(x_y1, x_temp);

            /* x_g[1] */
            x_bak = _mm_unpacklo_epi16(x_g, x_g);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_g1 = _mm_add_epi16(x_y1, x_temp);

            /* x_r[1] */
            x_bak = _mm_unpacklo_epi16(x_r, x_r);
            x_temp = _mm_unpacklo_epi32(x_bak, x_bak);
            x_r1 = _mm_add_epi16(x_y1, x_temp);

            x_b1 = _mm_srai_epi16(x_b1, 5);
            x_g1 = _mm_srai_epi16(x_g1, 5);
            x_r1 = _mm_srai_epi16(x_r1, 5);

            /* pack: 16=>8 */
            x_b = _mm_packus_epi16(x_b1, x_zero);
            x_r = _mm_packus_epi16(x_r1, x_zero);
            x_g = _mm_packus_epi16(x_g1, x_zero);

            /* create bgra sequences */
            x_bgl = _mm_unpacklo_epi8(x_b, x_g);
            x_ral = _mm_unpacklo_epi8(x_r, x_aa);

            x_bgral = _mm_unpacklo_epi16(x_bgl, x_ral);
            x_bgrah = _mm_unpackhi_epi16(x_bgl, x_ral);
            x_temp = _mm_unpacklo_epi64(x_bgral, x_bgrah);
            x_temp1 = _mm_unpackhi_epi64(x_bgral, x_bgrah);
            SAVE_BGRA1(x_temp, pd1);
            SAVE_BGRA1(x_temp1, pd2);

            iW += 4;
        }

        /* 4 pixels */
        if (iW <= width - 2) {
            /* load y u v, and expand */
            iu = *((uint8_t *)pm_u);
            pm_u = (__m64 *) (((uint8_t *)pm_u) + 1);
            iv = *((uint8_t *)pm_v);
            pm_v = (__m64 *) (((uint8_t *)pm_v) + 1);

            iTemp = (*((uint16_t *)px_y1)  & 0xffff) | (*((uint16_t *)px_y2)<<16);
            x_temp = _mm_cvtsi32_si128(iTemp);
            x_y1 = _mm_unpacklo_epi8(x_zero, x_temp);
            px_y1 = (__m128i *) (((uint16_t *)px_y1) + 1);
            px_y2 = (__m128i *) (((uint16_t *)px_y2) + 1);

            /* pre-calc d[r/g/b][1] */
            iu0 = (iu * ic1) >> 8;
            ib = icoff0 + iu0;

            iu1 = (iu * ic4) >> 8;
            iv1 = (iv * ic5) >> 8;
            iTemp = iu1 + iv1;
            ig = icoff1 - iTemp;

            iv2 = (iv * ic8) >> 8;
            ir = iv2 + icoff2;

            x_y1 = _mm_mulhi_epu16(x_y1, x_c0);

            /* db1 */
            x_temp = _mm_set1_epi16(ib);
            x_b1 = _mm_add_epi16(x_y1, x_temp);

            /* dg1 */
            x_temp = _mm_set1_epi16(ig);
            x_g1 = _mm_add_epi16(x_y1, x_temp);

            /* dr1 */
            x_temp = _mm_set1_epi16(ir);
            x_r1 = _mm_add_epi16(x_y1, x_temp);

            x_b1 = _mm_srai_epi16(x_b1, 5);
            x_g1 = _mm_srai_epi16(x_g1, 5);
            x_r1 = _mm_srai_epi16(x_r1, 5);

            /* pack: 16=>8 */
            x_b = _mm_packus_epi16(x_b1, x_zero);
            x_r = _mm_packus_epi16(x_r1, x_zero);
            x_g = _mm_packus_epi16(x_g1, x_zero);

            /* create bgra sequences */
            x_bgl = _mm_unpacklo_epi8(x_b, x_g);
            x_ral = _mm_unpacklo_epi8(x_r, x_aa);

            /* lower half of darl & dgbl */
            x_bgral = _mm_unpacklo_epi16(x_bgl, x_ral);
            _mm_storel_epi64((__m128i*)pd1, x_bgral);
            _mm_storeh_epi64((__m128i*)pd2, x_bgral);

            pd1 += 8;
            pd2 += 8;
        }

        pY1 += (2 * y_stride);
        pY2 += (2 * y_stride);
        pU += u_stride;
        pV += v_stride;
        pD1 += (2 * bgra_stride);
        pD2 += (2 * bgra_stride);
    }

    return 0;
}
// --- End SSE2 YCbCr420p conversion functions

#else // Generic C implementation

// --- Begin C YCbCr420p conversion functions
int ColorConvert_YCbCr420p_to_ARGB32(
                               uint8_t *argb,
                               int32_t argb_stride,
                               int32_t width,
                               int32_t height,
                               const uint8_t *y,
                               const uint8_t *v,
                               const uint8_t *u,
                               const uint8_t *a,
                               int32_t y_stride,
                               int32_t v_stride,
                               int32_t u_stride,
                               int32_t a_stride)
{
    return 1; // NOTE: Not implemented
}

int ColorConvert_YCbCr420p_to_ARGB32_no_alpha(
                                     uint8_t *argb,
                                     int32_t argb_stride,
                                     int32_t width,
                                     int32_t height,
                                     const uint8_t *y,
                                     const uint8_t *v,
                                     const uint8_t *u,
                                     int32_t y_stride,
                                     int32_t v_stride,
                                     int32_t u_stride)
{
    return 1; // NOTE: Not implemented
}

int ColorConvert_YCbCr420p_to_BGRA32(uint8_t *bgra,
                                     int32_t bgra_stride,
                                     int32_t width,
                                     int32_t height,
                                     const uint8_t *y,
                                     const uint8_t *v,
                                     const uint8_t *u,
                                     const uint8_t *a,
                                     int32_t y_stride,
                                     int32_t v_stride,
                                     int32_t u_stride,
                                     int32_t a_stride)
{
    int32_t i, j;
    const uint8_t *say1, *say2, *sau, *sav, *sly1, *sly2, *slu, *slv;
    uint8_t *da1, *dl1, *da2, *dl2;

    int32_t BBi = 554;
    int32_t RRi = 446;

    uint8_t *const pClip = (uint8_t *const)color_tClip + 288 * 2;

    if (bgra == NULL || y == NULL || u == NULL || v == NULL)
        return 1;

    if (width <= 0 || height <= 0)
        return 1;

    if ((width | height) & 1)
        return 1;

    sly1 = say1 = y;
    sly2 = say2 = y + y_stride;
    slu = sau = u;
    slv = sav = v;
    dl1 = da1 = bgra;
    dl2 = da2 = bgra + bgra_stride;

    // non aligned case

    uint8_t *a_row1 = (uint8_t*)a;
    uint8_t *a_row2 = (uint8_t*)(a + a_stride);

    for (j = 0; j < (height >> 1); j++) {
        uint8_t *a_col1 = a_row1;
        uint8_t *a_col2 = a_row2;
        for (i = 0; i < (width >> 1); i++) {
            int32_t sf01, sf02, sf03, sf04, sf1, sf2, sfr,
            sfg, sfb;

            sf1 = sau[0];
            sf2 = sav[0];

            sf01 = say1[0];
            sf03 = say1[1];
            sf02 = say2[0];
            sf04 = say2[1];

            sfr = color_tRV[sf2] - RRi;
            sfg = color_tGU[sf1] - color_tGV[sf2];
            sfb = color_tBU[sf1] - BBi;

            sf01 = color_tYY[sf01];
            sf03 = color_tYY[sf03];
            sf02 = color_tYY[sf02];
            sf04 = color_tYY[sf04];

            TCLAMP_U8(sf01 + sfr, da1[2]);
            TCLAMP_U8(sf01 + sfg, da1[1]);
            SCLAMP_U8(sf01 + sfb, da1[0]);
            TCLAMP_U8(sf03 + sfr, da1[6]);
            TCLAMP_U8(sf03 + sfg, da1[5]);
            SCLAMP_U8(sf03 + sfb, da1[4]);
            TCLAMP_U8(sf02 + sfr, da2[2]);
            TCLAMP_U8(sf02 + sfg, da2[1]);
            SCLAMP_U8(sf02 + sfb, da2[0]);
            TCLAMP_U8(sf04 + sfr, da2[6]);
            SCLAMP_U8(sf04 + sfg, da2[5]);
            TCLAMP_U8(sf04 + sfb, da2[4]);

            da1[3] = a_col1[0];
            da1[7] = a_col1[1];
            da2[3] = a_col2[0];
            da2[7] = a_col2[1];

            say1 += 2;
            say2 += 2;
            sau++;
            sav++;
            da1 += 8;
            da2 += 8;

            a_col1 += 2;
            a_col2 += 2;
        }

        sly1 = say1 = ((uint8_t *)sly1 + 2 * y_stride);
        sly2 = say2 = ((uint8_t *)sly2 + 2 * y_stride);
        slu = sau = ((uint8_t *)slu + u_stride);
        slv = sav = ((uint8_t *)slv + v_stride);
        dl1 = da1 = ((uint8_t *)dl1 + 2 * bgra_stride);
        dl2 = da2 = ((uint8_t *)dl2 + 2 * bgra_stride);

        a_row1 += (a_stride << 1);
        a_row2 += (a_stride << 1);
    }

    return 0;
}

int ColorConvert_YCbCr420p_to_BGRA32_no_alpha(
                                              uint8_t *bgra,
                                              int32_t bgra_stride,
                                              int32_t width,
                                              int32_t height,
                                              const uint8_t *y,
                                              const uint8_t *v,
                                              const uint8_t *u,
                                              int32_t y_stride,
                                              int32_t v_stride,
                                              int32_t u_stride)
{
    int32_t i, j;
    const uint8_t *say1, *say2, *sau, *sav, *sly1, *sly2, *slu, *slv;
    uint8_t *da1, *dl1, *da2, *dl2;

    int32_t BBi = 554;
    int32_t RRi = 446;

    uint8_t *const pClip = (uint8_t *const)color_tClip + 288 * 2;

    if (bgra == NULL || y == NULL || u == NULL || v == NULL)
        return 1;

    if (width <= 0 || height <= 0)
        return 1;

    if ((width | height) & 1)
        return 1;

    sly1 = say1 = y;
    sly2 = say2 = y + y_stride;
    slu = sau = u;
    slv = sav = v;
    dl1 = da1 = bgra;
    dl2 = da2 = bgra + bgra_stride;

    // non aligned case

    for (j = 0; j < (height >> 1); j++) {
        for (i = 0; i < (width >> 1); i++) {
            int32_t sf01, sf02, sf03, sf04, sf1, sf2, sfr,
            sfg, sfb;

            sf1 = sau[0];
            sf2 = sav[0];

            sf01 = say1[0];
            sf03 = say1[1];
            sf02 = say2[0];
            sf04 = say2[1];

            sfr = color_tRV[sf2] - RRi;
            sfg = color_tGU[sf1] - color_tGV[sf2];
            sfb = color_tBU[sf1] - BBi;

            sf01 = color_tYY[sf01];
            sf03 = color_tYY[sf03];
            sf02 = color_tYY[sf02];
            sf04 = color_tYY[sf04];

            TCLAMP_U8(sf01 + sfr, da1[2]);
            TCLAMP_U8(sf01 + sfg, da1[1]);
            SCLAMP_U8(sf01 + sfb, da1[0]);
            TCLAMP_U8(sf03 + sfr, da1[6]);
            TCLAMP_U8(sf03 + sfg, da1[5]);
            SCLAMP_U8(sf03 + sfb, da1[4]);
            TCLAMP_U8(sf02 + sfr, da2[2]);
            TCLAMP_U8(sf02 + sfg, da2[1]);
            SCLAMP_U8(sf02 + sfb, da2[0]);
            TCLAMP_U8(sf04 + sfr, da2[6]);
            SCLAMP_U8(sf04 + sfg, da2[5]);
            TCLAMP_U8(sf04 + sfb, da2[4]);

            da1[3] = da1[7] = da2[3] = da2[7] = 0xff;

            say1 += 2;
            say2 += 2;
            sau++;
            sav++;
            da1 += 8;
            da2 += 8;
        }

        sly1 = say1 = ((uint8_t *)sly1 + 2 * y_stride);
        sly2 = say2 = ((uint8_t *)sly2 + 2 * y_stride);
        slu = sau = ((uint8_t *)slu + u_stride);
        slv = sav = ((uint8_t *)slv + v_stride);
        dl1 = da1 = ((uint8_t *)dl1 + 2 * bgra_stride);
        dl2 = da2 = ((uint8_t *)dl2 + 2 * bgra_stride);
    }

    return 0;
}
// --- End C YCbCr420p conversion functions
#endif // ENABLE_SIMD_SSE2
// --- End YCbCr420p conversion functions

// --- Begin YCbCr422p conversion functions

int ColorConvert_YCbCr422p_to_ARGB32_no_alpha(uint8_t *argb,
                                              int32_t argb_stride,
                                              int32_t width,
                                              int32_t height,
                                              const uint8_t *y,
                                              const uint8_t *v,
                                              const uint8_t *u,
                                              int32_t y_stride,
                                              int32_t uv_stride)
{
    return 1; // NOTE: Not implemented
}

int ColorConvert_YCbCr422p_to_BGRA32_no_alpha(uint8_t *bgra,
                                              int32_t bgra_stride,
                                              int32_t width,
                                              int32_t height,
                                              const uint8_t *y,
                                              const uint8_t *v,
                                              const uint8_t *u,
                                              int32_t y_stride,
                                              int32_t uv_stride)
{
    int32_t i, j;
    const uint8_t *say1, *sau, *sav, *sly1, *slu, *slv;
    uint8_t *da1, *dl1;

    int32_t BBi = 554;
    int32_t RRi = 446;

    uint8_t *const pClip = (uint8_t *const)color_tClip + 288 * 2;

    if (bgra == NULL || y == NULL || u == NULL || v == NULL)
        return 1;

    if (width <= 0 || height <= 0)
        return 1;

    if (width & 1)
        return 1;

    sly1 = say1 = y;
    slu = sau = u;
    slv = sav = v;
    dl1 = da1 = bgra;

    for (j = 0; j < height; j++) {
        for (i = 0; i < (width >> 1); i++) {
            int32_t sf01, sf03, sf1, sf2, sfr, sfg, sfb;

            sf1 = sau[0];
            sf2 = sav[0];

            sf01 = say1[0];
            sf03 = say1[2];

            sfr = color_tRV[sf2] - RRi;
            sfg = color_tGU[sf1] - color_tGV[sf2];
            sfb = color_tBU[sf1] - BBi;

            sf01 = color_tYY[sf01];
            sf03 = color_tYY[sf03];

            TCLAMP_U8(sf01 + sfr, da1[2]);
            TCLAMP_U8(sf01 + sfg, da1[1]);
            SCLAMP_U8(sf01 + sfb, da1[0]);
            TCLAMP_U8(sf03 + sfr, da1[6]);
            TCLAMP_U8(sf03 + sfg, da1[5]);
            SCLAMP_U8(sf03 + sfb, da1[4]);

            da1[3] = da1[7] = 0xff;

            say1 += 4;
            sau += 4;
            sav += 4;
            da1 += 8;
        }

        sly1 = say1 = ((uint8_t *)sly1 + y_stride);
        slu = sau = ((uint8_t *)slu + uv_stride);
        slv = sav = ((uint8_t *)slv + uv_stride);
        dl1 = da1 = ((uint8_t *)dl1 + bgra_stride);
    }

    return 0;
}
// --- End YCbCr422p conversion functions
