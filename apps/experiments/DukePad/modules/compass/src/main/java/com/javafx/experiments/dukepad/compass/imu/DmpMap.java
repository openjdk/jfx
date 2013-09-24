/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.compass.imu;

public class DmpMap {
    public static int DMP_PTAT = 0;
    public static int DMP_XGYR = 2;
    public static int DMP_YGYR = 4;
    public static int DMP_ZGYR = 6;
    public static int DMP_XACC = 8;
    public static int DMP_YACC = 10;
    public static int DMP_ZACC = 12;
    public static int DMP_ADC1 = 14;
    public static int DMP_ADC2 = 16;
    public static int DMP_ADC3 = 18;
    public static int DMP_BIASUNC = 20;
    public static int DMP_FIFORT = 22;
    public static int DMP_INVGSFH = 24;
    public static int DMP_INVGSFL = 26;
    public static int DMP_1H = 28;
    public static int DMP_1L = 30;
    public static int DMP_BLPFSTCH = 32;
    public static int DMP_BLPFSTCL = 34;
    public static int DMP_BLPFSXH = 36;
    public static int DMP_BLPFSXL = 38;
    public static int DMP_BLPFSYH = 40;
    public static int DMP_BLPFSYL = 42;
    public static int DMP_BLPFSZH = 44;
    public static int DMP_BLPFSZL = 46;
    public static int DMP_BLPFMTC = 48;
    public static int DMP_SMC = 50;
    public static int DMP_BLPFMXH = 52;
    public static int DMP_BLPFMXL = 54;
    public static int DMP_BLPFMYH = 56;
    public static int DMP_BLPFMYL = 58;
    public static int DMP_BLPFMZH = 60;
    public static int DMP_BLPFMZL = 62;
    public static int DMP_BLPFC = 64;
    public static int DMP_SMCTH = 66;
    public static int DMP_0H2 = 68;
    public static int DMP_0L2 = 70;
    public static int DMP_BERR2H = 72;
    public static int DMP_BERR2L = 74;
    public static int DMP_BERR2NH = 76;
    public static int DMP_SMCINC = 78;
    public static int DMP_ANGVBXH = 80;
    public static int DMP_ANGVBXL = 82;
    public static int DMP_ANGVBYH = 84;
    public static int DMP_ANGVBYL = 86;
    public static int DMP_ANGVBZH = 88;
    public static int DMP_ANGVBZL = 90;
    public static int DMP_BERR1H = 92;
    public static int DMP_BERR1L = 94;
    public static int DMP_ATCH = 96;
    public static int DMP_BIASUNCSF = 98;
    public static int DMP_ACT2H = 100;
    public static int DMP_ACT2L = 102;
    public static int DMP_GSFH = 104;
    public static int DMP_GSFL = 106;
    public static int DMP_GH = 108;
    public static int DMP_GL = 110;
    public static int DMP_0_5H = 112;
    public static int DMP_0_5L = 114;
    public static int DMP_0_0H = 116;
    public static int DMP_0_0L = 118;
    public static int DMP_1_0H = 120;
    public static int DMP_1_0L = 122;
    public static int DMP_1_5H = 124;
    public static int DMP_1_5L = 126;
    public static int DMP_TMP1AH = 128;
    public static int DMP_TMP1AL = 130;
    public static int DMP_TMP2AH = 132;
    public static int DMP_TMP2AL = 134;
    public static int DMP_TMP3AH = 136;
    public static int DMP_TMP3AL = 138;
    public static int DMP_TMP4AH = 140;
    public static int DMP_TMP4AL = 142;
    public static int DMP_XACCW = 144;
    public static int DMP_TMP5 = 146;
    public static int DMP_XACCB = 148;
    public static int DMP_TMP8 = 150;
    public static int DMP_YACCB = 152;
    public static int DMP_TMP9 = 154;
    public static int DMP_ZACCB = 156;
    public static int DMP_TMP10 = 158;
    public static int DMP_DZH = 160;
    public static int DMP_DZL = 162;
    public static int DMP_XGCH = 164;
    public static int DMP_XGCL = 166;
    public static int DMP_YGCH = 168;
    public static int DMP_YGCL = 170;
    public static int DMP_ZGCH = 172;
    public static int DMP_ZGCL = 174;
    public static int DMP_YACCW = 176;
    public static int DMP_TMP7 = 178;
    public static int DMP_AFB1H = 180;
    public static int DMP_AFB1L = 182;
    public static int DMP_AFB2H = 184;
    public static int DMP_AFB2L = 186;
    public static int DMP_MAGFBH = 188;
    public static int DMP_MAGFBL = 190;
    public static int DMP_QT1H = 192;
    public static int DMP_QT1L = 194;
    public static int DMP_QT2H = 196;
    public static int DMP_QT2L = 198;
    public static int DMP_QT3H = 200;
    public static int DMP_QT3L = 202;
    public static int DMP_QT4H = 204;
    public static int DMP_QT4L = 206;
    public static int DMP_CTRL1H = 208;
    public static int DMP_CTRL1L = 210;
    public static int DMP_CTRL2H = 212;
    public static int DMP_CTRL2L = 214;
    public static int DMP_CTRL3H = 216;
    public static int DMP_CTRL3L = 218;
    public static int DMP_CTRL4H = 220;
    public static int DMP_CTRL4L = 222;
    public static int DMP_CTRLS1 = 224;
    public static int DMP_CTRLSF1 = 226;
    public static int DMP_CTRLS2 = 228;
    public static int DMP_CTRLSF2 = 230;
    public static int DMP_CTRLS3 = 232;
    public static int DMP_CTRLSFNLL = 234;
    public static int DMP_CTRLS4 = 236;
    public static int DMP_CTRLSFNL2 = 238;
    public static int DMP_CTRLSFNL = 240;
    public static int DMP_TMP30 = 242;
    public static int DMP_CTRLSFJT = 244;
    public static int DMP_TMP31 = 246;
    public static int DMP_TMP11 = 248;
    public static int DMP_CTRLSF2_2 = 250;
    public static int DMP_TMP12 = 252;
    public static int DMP_CTRLSF1_2 = 254;
    public static int DMP_PREVPTAT = 256;
    public static int DMP_ACCZB = 258;
    public static int DMP_ACCXB = 264;
    public static int DMP_ACCYB = 266;
    public static int DMP_1HB = 272;
    public static int DMP_1LB = 274;
    public static int DMP_0H = 276;
    public static int DMP_0L = 278;
    public static int DMP_ASR22H = 280;
    public static int DMP_ASR22L = 282;
    public static int DMP_ASR6H = 284;
    public static int DMP_ASR6L = 286;
    public static int DMP_TMP13 = 288;
    public static int DMP_TMP14 = 290;
    public static int DMP_FINTXH = 292;
    public static int DMP_FINTXL = 294;
    public static int DMP_FINTYH = 296;
    public static int DMP_FINTYL = 298;
    public static int DMP_FINTZH = 300;
    public static int DMP_FINTZL = 302;
    public static int DMP_TMP1BH = 304;
    public static int DMP_TMP1BL = 306;
    public static int DMP_TMP2BH = 308;
    public static int DMP_TMP2BL = 310;
    public static int DMP_TMP3BH = 312;
    public static int DMP_TMP3BL = 314;
    public static int DMP_TMP4BH = 316;
    public static int DMP_TMP4BL = 318;
    public static int DMP_STXG = 320;
    public static int DMP_ZCTXG = 322;
    public static int DMP_STYG = 324;
    public static int DMP_ZCTYG = 326;
    public static int DMP_STZG = 328;
    public static int DMP_ZCTZG = 330;
    public static int DMP_CTRLSFJT2 = 332;
    public static int DMP_CTRLSFJTCNT = 334;
    public static int DMP_PVXG = 336;
    public static int DMP_TMP15 = 338;
    public static int DMP_PVYG = 340;
    public static int DMP_TMP16 = 342;
    public static int DMP_PVZG = 344;
    public static int DMP_TMP17 = 346;
    public static int DMP_MNMFLAGH = 352;
    public static int DMP_MNMFLAGL = 354;
    public static int DMP_MNMTMH = 356;
    public static int DMP_MNMTML = 358;
    public static int DMP_MNMTMTHRH = 360;
    public static int DMP_MNMTMTHRL = 362;
    public static int DMP_MNMTHRH = 364;
    public static int DMP_MNMTHRL = 366;
    public static int DMP_ACCQD4H = 368;
    public static int DMP_ACCQD4L = 370;
    public static int DMP_ACCQD5H = 372;
    public static int DMP_ACCQD5L = 374;
    public static int DMP_ACCQD6H = 376;
    public static int DMP_ACCQD6L = 378;
    public static int DMP_ACCQD7H = 380;
    public static int DMP_ACCQD7L = 382;
    public static int DMP_ACCQD0H = 384;
    public static int DMP_ACCQD0L = 386;
    public static int DMP_ACCQD1H = 388;
    public static int DMP_ACCQD1L = 390;
    public static int DMP_ACCQD2H = 392;
    public static int DMP_ACCQD2L = 394;
    public static int DMP_ACCQD3H = 396;
    public static int DMP_ACCQD3L = 398;
    public static int DMP_XN2H = 400;
    public static int DMP_XN2L = 402;
    public static int DMP_XN1H = 404;
    public static int DMP_XN1L = 406;
    public static int DMP_YN2H = 408;
    public static int DMP_YN2L = 410;
    public static int DMP_YN1H = 412;
    public static int DMP_YN1L = 414;
    public static int DMP_YH = 416;
    public static int DMP_YL = 418;
    public static int DMP_B0H = 420;
    public static int DMP_B0L = 422;
    public static int DMP_A1H = 424;
    public static int DMP_A1L = 426;
    public static int DMP_A2H = 428;
    public static int DMP_A2L = 430;
    public static int DMP_SEM1 = 432;
    public static int DMP_FIFOCNT = 434;
    public static int DMP_SH_TH_X = 436;
    public static int DMP_PACKET = 438;
    public static int DMP_SH_TH_Y = 440;
    public static int DMP_FOOTER = 442;
    public static int DMP_SH_TH_Z = 444;
    public static int DMP_TEMP29 = 448;
    public static int DMP_TEMP30 = 450;
    public static int DMP_XACCB_PRE = 452;
    public static int DMP_XACCB_PREL = 454;
    public static int DMP_YACCB_PRE = 456;
    public static int DMP_YACCB_PREL = 458;
    public static int DMP_ZACCB_PRE = 460;
    public static int DMP_ZACCB_PREL = 462;
    public static int DMP_TMP22 = 464;
    public static int DMP_TAP_TIMER = 466;
    public static int DMP_TAP_THX = 468;
    public static int DMP_TAP_THY = 472;
    public static int DMP_TAP_THZ = 476;
    public static int DMP_TAPW_MIN = 478;
    public static int DMP_TMP25 = 480;
    public static int DMP_TMP26 = 482;
    public static int DMP_TMP27 = 484;
    public static int DMP_TMP28 = 486;
    public static int DMP_ORIENT = 488;
    public static int DMP_THRSH = 490;
    public static int DMP_ENDIANH = 492;
    public static int DMP_ENDIANL = 494;
    public static int DMP_BLPFNMTCH = 496;
    public static int DMP_BLPFNMTCL = 498;
    public static int DMP_BLPFNMXH = 500;
    public static int DMP_BLPFNMXL = 502;
    public static int DMP_BLPFNMYH = 504;
    public static int DMP_BLPFNMYL = 506;
    public static int DMP_BLPFNMZH = 508;
    public static int DMP_BLPFNMZL = 510;
}
