/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import java.lang.reflect.Field;
import java.util.Formatter;

/** Constants from /usr/include/linux/input.h */
class LinuxInput {

    static final short INPUT_PROP_POINTER = 0x00;
    static final short INPUT_PROP_DIRECT = 0x01;
    static final short INPUT_PROP_BUTTONPAD = 0x02;
    static final short INPUT_PROP_SEMI_MT = 0x03;
    static final short INPUT_PROP_MAX = 0x1f;
    static final short INPUT_PROP_CNT = INPUT_PROP_MAX + 1;

    static final short EV_SYN = 0x00;
    static final short EV_KEY = 0x01;
    static final short EV_REL = 0x02;
    static final short EV_ABS = 0x03;
    static final short EV_MSC = 0x04;
    static final short EV_SW = 0x05;
    static final short EV_LED = 0x11;
    static final short EV_SND = 0x12;
    static final short EV_REP = 0x14;
    static final short EV_FF = 0x15;
    static final short EV_PWR = 0x16;
    static final short EV_FF_STATUS = 0x17;
    static final short EV_MAX = 0x1f;
    static final short EV_CNT = EV_MAX + 1;

    static final short SYN_REPORT = 0;
    static final short SYN_CONFIG = 1;
    static final short SYN_MT_REPORT = 2;
    static final short SYN_DROPPED = 3;

    static final short KEY_RESERVED = 0;
    static final short KEY_ESC = 1;
    static final short KEY_1 = 2;
    static final short KEY_2 = 3;
    static final short KEY_3 = 4;
    static final short KEY_4 = 5;
    static final short KEY_5 = 6;
    static final short KEY_6 = 7;
    static final short KEY_7 = 8;
    static final short KEY_8 = 9;
    static final short KEY_9 = 10;
    static final short KEY_0 = 11;
    static final short KEY_MINUS = 12;
    static final short KEY_EQUAL = 13;
    static final short KEY_BACKSPACE = 14;
    static final short KEY_TAB = 15;
    static final short KEY_Q = 16;
    static final short KEY_W = 17;
    static final short KEY_E = 18;
    static final short KEY_R = 19;
    static final short KEY_T = 20;
    static final short KEY_Y = 21;
    static final short KEY_U = 22;
    static final short KEY_I = 23;
    static final short KEY_O = 24;
    static final short KEY_P = 25;
    static final short KEY_LEFTBRACE = 26;
    static final short KEY_RIGHTBRACE = 27;
    static final short KEY_ENTER = 28;
    static final short KEY_LEFTCTRL = 29;
    static final short KEY_A = 30;
    static final short KEY_S = 31;
    static final short KEY_D = 32;
    static final short KEY_F = 33;
    static final short KEY_G = 34;
    static final short KEY_H = 35;
    static final short KEY_J = 36;
    static final short KEY_K = 37;
    static final short KEY_L = 38;
    static final short KEY_SEMICOLON = 39;
    static final short KEY_APOSTROPHE = 40;
    static final short KEY_GRAVE = 41;
    static final short KEY_LEFTSHIFT = 42;
    static final short KEY_BACKSLASH = 43;
    static final short KEY_Z = 44;
    static final short KEY_X = 45;
    static final short KEY_C = 46;
    static final short KEY_V = 47;
    static final short KEY_B = 48;
    static final short KEY_N = 49;
    static final short KEY_M = 50;
    static final short KEY_COMMA = 51;
    static final short KEY_DOT = 52;
    static final short KEY_SLASH = 53;
    static final short KEY_RIGHTSHIFT = 54;
    static final short KEY_KPASTERISK = 55;
    static final short KEY_LEFTALT = 56;
    static final short KEY_SPACE = 57;
    static final short KEY_CAPSLOCK = 58;
    static final short KEY_F1 = 59;
    static final short KEY_F2 = 60;
    static final short KEY_F3 = 61;
    static final short KEY_F4 = 62;
    static final short KEY_F5 = 63;
    static final short KEY_F6 = 64;
    static final short KEY_F7 = 65;
    static final short KEY_F8 = 66;
    static final short KEY_F9 = 67;
    static final short KEY_F10 = 68;
    static final short KEY_NUMLOCK = 69;
    static final short KEY_SCROLLLOCK = 70;
    static final short KEY_KP7 = 71;
    static final short KEY_KP8 = 72;
    static final short KEY_KP9 = 73;
    static final short KEY_KPMINUS = 74;
    static final short KEY_KP4 = 75;
    static final short KEY_KP5 = 76;
    static final short KEY_KP6 = 77;
    static final short KEY_KPPLUS = 78;
    static final short KEY_KP1 = 79;
    static final short KEY_KP2 = 80;
    static final short KEY_KP3 = 81;
    static final short KEY_KP0 = 82;
    static final short KEY_KPDOT = 83;
    static final short KEY_ZENKAKUHANKAKU = 85;
    static final short KEY_102ND = 86;
    static final short KEY_F11 = 87;
    static final short KEY_F12 = 88;
    static final short KEY_RO = 89;
    static final short KEY_KATAKANA = 90;
    static final short KEY_HIRAGANA = 91;
    static final short KEY_HENKAN = 92;
    static final short KEY_KATAKANAHIRAGANA = 93;
    static final short KEY_MUHENKAN = 94;
    static final short KEY_KPJPCOMMA = 95;
    static final short KEY_KPENTER = 96;
    static final short KEY_RIGHTCTRL = 97;
    static final short KEY_KPSLASH = 98;
    static final short KEY_SYSRQ = 99;
    static final short KEY_RIGHTALT = 100;
    static final short KEY_LINEFEED = 101;
    static final short KEY_HOME = 102;
    static final short KEY_UP = 103;
    static final short KEY_PAGEUP = 104;
    static final short KEY_LEFT = 105;
    static final short KEY_RIGHT = 106;
    static final short KEY_END = 107;
    static final short KEY_DOWN = 108;
    static final short KEY_PAGEDOWN = 109;
    static final short KEY_INSERT = 110;
    static final short KEY_DELETE = 111;
    static final short KEY_MACRO = 112;
    static final short KEY_MUTE = 113;
    static final short KEY_VOLUMEDOWN = 114;
    static final short KEY_VOLUMEUP = 115;
    static final short KEY_POWER = 116;
    static final short KEY_KPEQUAL = 117;
    static final short KEY_KPPLUSMINUS = 118;
    static final short KEY_PAUSE = 119;
    static final short KEY_SCALE = 120;
    static final short KEY_KPCOMMA = 121;
    static final short KEY_HANGEUL = 122;
    static final short KEY_HANGUEL = KEY_HANGEUL;
    static final short KEY_HANJA = 123;
    static final short KEY_YEN = 124;
    static final short KEY_LEFTMETA = 125;
    static final short KEY_RIGHTMETA = 126;
    static final short KEY_COMPOSE = 127;
    static final short KEY_STOP = 128;
    static final short KEY_AGAIN = 129;
    static final short KEY_PROPS = 130;
    static final short KEY_UNDO = 131;
    static final short KEY_FRONT = 132;
    static final short KEY_COPY = 133;
    static final short KEY_OPEN = 134;
    static final short KEY_PASTE = 135;
    static final short KEY_FIND = 136;
    static final short KEY_CUT = 137;
    static final short KEY_HELP = 138;
    static final short KEY_MENU = 139;
    static final short KEY_CALC = 140;
    static final short KEY_SETUP = 141;
    static final short KEY_SLEEP = 142;
    static final short KEY_WAKEUP = 143;
    static final short KEY_FILE = 144;
    static final short KEY_SENDFILE = 145;
    static final short KEY_DELETEFILE = 146;
    static final short KEY_XFER = 147;
    static final short KEY_PROG1 = 148;
    static final short KEY_PROG2 = 149;
    static final short KEY_WWW = 150;
    static final short KEY_MSDOS = 151;
    static final short KEY_COFFEE = 152;
    static final short KEY_SCREENLOCK = KEY_COFFEE;
    static final short KEY_DIRECTION = 153;
    static final short KEY_CYCLEWINDOWS = 154;
    static final short KEY_MAIL = 155;
    static final short KEY_BOOKMARKS = 156;
    static final short KEY_COMPUTER = 157;
    static final short KEY_BACK = 158;
    static final short KEY_FORWARD = 159;
    static final short KEY_CLOSECD = 160;
    static final short KEY_EJECTCD = 161;
    static final short KEY_EJECTCLOSECD = 162;
    static final short KEY_NEXTSONG = 163;
    static final short KEY_PLAYPAUSE = 164;
    static final short KEY_PREVIOUSSONG = 165;
    static final short KEY_STOPCD = 166;
    static final short KEY_RECORD = 167;
    static final short KEY_REWIND = 168;
    static final short KEY_PHONE = 169;
    static final short KEY_ISO = 170;
    static final short KEY_CONFIG = 171;
    static final short KEY_HOMEPAGE = 172;
    static final short KEY_REFRESH = 173;
    static final short KEY_EXIT = 174;
    static final short KEY_MOVE = 175;
    static final short KEY_EDIT = 176;
    static final short KEY_SCROLLUP = 177;
    static final short KEY_SCROLLDOWN = 178;
    static final short KEY_KPLEFTPAREN = 179;
    static final short KEY_KPRIGHTPAREN = 180;
    static final short KEY_NEW = 181;
    static final short KEY_REDO = 182;
    static final short KEY_F13 = 183;
    static final short KEY_F14 = 184;
    static final short KEY_F15 = 185;
    static final short KEY_F16 = 186;
    static final short KEY_F17 = 187;
    static final short KEY_F18 = 188;
    static final short KEY_F19 = 189;
    static final short KEY_F20 = 190;
    static final short KEY_F21 = 191;
    static final short KEY_F22 = 192;
    static final short KEY_F23 = 193;
    static final short KEY_F24 = 194;
    static final short KEY_PLAYCD = 200;
    static final short KEY_PAUSECD = 201;
    static final short KEY_PROG3 = 202;
    static final short KEY_PROG4 = 203;
    static final short KEY_DASHBOARD = 204;
    static final short KEY_SUSPEND = 205;
    static final short KEY_CLOSE = 206;
    static final short KEY_PLAY = 207;
    static final short KEY_FASTFORWARD = 208;
    static final short KEY_BASSBOOST = 209;
    static final short KEY_PRINT = 210;
    static final short KEY_HP = 211;
    static final short KEY_CAMERA = 212;
    static final short KEY_SOUND = 213;
    static final short KEY_QUESTION = 214;
    static final short KEY_EMAIL = 215;
    static final short KEY_CHAT = 216;
    static final short KEY_SEARCH = 217;
    static final short KEY_CONNECT = 218;
    static final short KEY_FINANCE = 219;
    static final short KEY_SPORT = 220;
    static final short KEY_SHOP = 221;
    static final short KEY_ALTERASE = 222;
    static final short KEY_CANCEL = 223;
    static final short KEY_BRIGHTNESSDOWN = 224;
    static final short KEY_BRIGHTNESSUP = 225;
    static final short KEY_MEDIA = 226;
    static final short KEY_SWITCHVIDEOMODE = 227;
    static final short KEY_KBDILLUMTOGGLE = 228;
    static final short KEY_KBDILLUMDOWN = 229;
    static final short KEY_KBDILLUMUP = 230;
    static final short KEY_SEND = 231;
    static final short KEY_REPLY = 232;
    static final short KEY_FORWARDMAIL = 233;
    static final short KEY_SAVE = 234;
    static final short KEY_DOCUMENTS = 235;
    static final short KEY_BATTERY = 236;
    static final short KEY_BLUETOOTH = 237;
    static final short KEY_WLAN = 238;
    static final short KEY_UWB = 239;
    static final short KEY_UNKNOWN = 240;
    static final short KEY_VIDEO_NEXT = 241;
    static final short KEY_VIDEO_PREV = 242;
    static final short KEY_BRIGHTNESS_CYCLE = 243;
    static final short KEY_BRIGHTNESS_ZERO = 244;
    static final short KEY_DISPLAY_OFF = 245;
    static final short KEY_WIMAX = 246;
    static final short KEY_RFKILL = 247;
    static final short KEY_MICMUTE = 248;

    static final short BTN_MISC = 0x100;
    static final short BTN_0 = 0x100;
    static final short BTN_1 = 0x101;
    static final short BTN_2 = 0x102;
    static final short BTN_3 = 0x103;
    static final short BTN_4 = 0x104;
    static final short BTN_5 = 0x105;
    static final short BTN_6 = 0x106;
    static final short BTN_7 = 0x107;
    static final short BTN_8 = 0x108;
    static final short BTN_9 = 0x109;
    static final short BTN_MOUSE = 0x110;
    static final short BTN_LEFT = 0x110;
    static final short BTN_RIGHT = 0x111;
    static final short BTN_MIDDLE = 0x112;
    static final short BTN_SIDE = 0x113;
    static final short BTN_EXTRA = 0x114;
    static final short BTN_FORWARD = 0x115;
    static final short BTN_BACK = 0x116;
    static final short BTN_TASK = 0x117;
    static final short BTN_JOYSTICK = 0x120;
    static final short BTN_TRIGGER = 0x120;
    static final short BTN_THUMB = 0x121;
    static final short BTN_THUMB2 = 0x122;
    static final short BTN_TOP = 0x123;
    static final short BTN_TOP2 = 0x124;
    static final short BTN_PINKIE = 0x125;
    static final short BTN_BASE = 0x126;
    static final short BTN_BASE2 = 0x127;
    static final short BTN_BASE3 = 0x128;
    static final short BTN_BASE4 = 0x129;
    static final short BTN_BASE5 = 0x12a;
    static final short BTN_BASE6 = 0x12b;
    static final short BTN_DEAD = 0x12f;
    static final short BTN_GAMEPAD = 0x130;
    static final short BTN_A = 0x130;
    static final short BTN_B = 0x131;
    static final short BTN_C = 0x132;
    static final short BTN_X = 0x133;
    static final short BTN_Y = 0x134;
    static final short BTN_Z = 0x135;
    static final short BTN_TL = 0x136;
    static final short BTN_TR = 0x137;
    static final short BTN_TL2 = 0x138;
    static final short BTN_TR2 = 0x139;
    static final short BTN_SELECT = 0x13a;
    static final short BTN_START = 0x13b;
    static final short BTN_MODE = 0x13c;
    static final short BTN_THUMBL = 0x13d;
    static final short BTN_THUMBR = 0x13e;
    static final short BTN_DIGI = 0x140;
    static final short BTN_TOOL_PEN = 0x140;
    static final short BTN_TOOL_RUBBER = 0x141;
    static final short BTN_TOOL_BRUSH = 0x142;
    static final short BTN_TOOL_PENCIL = 0x143;
    static final short BTN_TOOL_AIRBRUSH = 0x144;
    static final short BTN_TOOL_FINGER = 0x145;
    static final short BTN_TOOL_MOUSE = 0x146;
    static final short BTN_TOOL_LENS = 0x147;
    static final short BTN_TOOL_QUINTTAP = 0x148;
    static final short BTN_TOUCH = 0x14a;
    static final short BTN_STYLUS = 0x14b;
    static final short BTN_STYLUS2 = 0x14c;
    static final short BTN_TOOL_DOUBLETAP = 0x14d;
    static final short BTN_TOOL_TRIPLETAP = 0x14e;
    static final short BTN_TOOL_QUADTAP = 0x14f;
    static final short BTN_WHEEL = 0x150;
    static final short BTN_GEAR_DOWN = 0x150;
    static final short BTN_GEAR_UP = 0x151;
    static final short KEY_OK = 0x160;
    static final short KEY_SELECT = 0x161;
    static final short KEY_GOTO = 0x162;
    static final short KEY_CLEAR = 0x163;
    static final short KEY_POWER2 = 0x164;
    static final short KEY_OPTION = 0x165;
    static final short KEY_INFO = 0x166;
    static final short KEY_TIME = 0x167;
    static final short KEY_VENDOR = 0x168;
    static final short KEY_ARCHIVE = 0x169;
    static final short KEY_PROGRAM = 0x16a;
    static final short KEY_CHANNEL = 0x16b;
    static final short KEY_FAVORITES = 0x16c;
    static final short KEY_EPG = 0x16d;
    static final short KEY_PVR = 0x16e;
    static final short KEY_MHP = 0x16f;
    static final short KEY_LANGUAGE = 0x170;
    static final short KEY_TITLE = 0x171;
    static final short KEY_SUBTITLE = 0x172;
    static final short KEY_ANGLE = 0x173;
    static final short KEY_ZOOM = 0x174;
    static final short KEY_MODE = 0x175;
    static final short KEY_KEYBOARD = 0x176;
    static final short KEY_SCREEN = 0x177;
    static final short KEY_PC = 0x178;
    static final short KEY_TV = 0x179;
    static final short KEY_TV2 = 0x17a;
    static final short KEY_VCR = 0x17b;
    static final short KEY_VCR2 = 0x17c;
    static final short KEY_SAT = 0x17d;
    static final short KEY_SAT2 = 0x17e;
    static final short KEY_CD = 0x17f;
    static final short KEY_TAPE = 0x180;
    static final short KEY_RADIO = 0x181;
    static final short KEY_TUNER = 0x182;
    static final short KEY_PLAYER = 0x183;
    static final short KEY_TEXT = 0x184;
    static final short KEY_DVD = 0x185;
    static final short KEY_AUX = 0x186;
    static final short KEY_MP3 = 0x187;
    static final short KEY_AUDIO = 0x188;
    static final short KEY_VIDEO = 0x189;
    static final short KEY_DIRECTORY = 0x18a;
    static final short KEY_LIST = 0x18b;
    static final short KEY_MEMO = 0x18c;
    static final short KEY_CALENDAR = 0x18d;
    static final short KEY_RED = 0x18e;
    static final short KEY_GREEN = 0x18f;
    static final short KEY_YELLOW = 0x190;
    static final short KEY_BLUE = 0x191;
    static final short KEY_CHANNELUP = 0x192;
    static final short KEY_CHANNELDOWN = 0x193;
    static final short KEY_FIRST = 0x194;
    static final short KEY_LAST = 0x195;
    static final short KEY_AB = 0x196;
    static final short KEY_NEXT = 0x197;
    static final short KEY_RESTART = 0x198;
    static final short KEY_SLOW = 0x199;
    static final short KEY_SHUFFLE = 0x19a;
    static final short KEY_BREAK = 0x19b;
    static final short KEY_PREVIOUS = 0x19c;
    static final short KEY_DIGITS = 0x19d;
    static final short KEY_TEEN = 0x19e;
    static final short KEY_TWEN = 0x19f;
    static final short KEY_VIDEOPHONE = 0x1a0;
    static final short KEY_GAMES = 0x1a1;
    static final short KEY_ZOOMIN = 0x1a2;
    static final short KEY_ZOOMOUT = 0x1a3;
    static final short KEY_ZOOMRESET = 0x1a4;
    static final short KEY_WORDPROCESSOR = 0x1a5;
    static final short KEY_EDITOR = 0x1a6;
    static final short KEY_SPREADSHEET = 0x1a7;
    static final short KEY_GRAPHICSEDITOR = 0x1a8;
    static final short KEY_PRESENTATION = 0x1a9;
    static final short KEY_DATABASE = 0x1aa;
    static final short KEY_NEWS = 0x1ab;
    static final short KEY_VOICEMAIL = 0x1ac;
    static final short KEY_ADDRESSBOOK = 0x1ad;
    static final short KEY_MESSENGER = 0x1ae;
    static final short KEY_DISPLAYTOGGLE = 0x1af;
    static final short KEY_SPELLCHECK = 0x1b0;
    static final short KEY_LOGOFF = 0x1b1;
    static final short KEY_DOLLAR = 0x1b2;
    static final short KEY_EURO = 0x1b3;
    static final short KEY_FRAMEBACK = 0x1b4;
    static final short KEY_FRAMEFORWARD = 0x1b5;
    static final short KEY_CONTEXT_MENU = 0x1b6;
    static final short KEY_MEDIA_REPEAT = 0x1b7;
    static final short KEY_10CHANNELSUP = 0x1b8;
    static final short KEY_10CHANNELSDOWN = 0x1b9;
    static final short KEY_IMAGES = 0x1ba;
    static final short KEY_DEL_EOL = 0x1c0;
    static final short KEY_DEL_EOS = 0x1c1;
    static final short KEY_INS_LINE = 0x1c2;
    static final short KEY_DEL_LINE = 0x1c3;
    static final short KEY_FN = 0x1d0;
    static final short KEY_FN_ESC = 0x1d1;
    static final short KEY_FN_F1 = 0x1d2;
    static final short KEY_FN_F2 = 0x1d3;
    static final short KEY_FN_F3 = 0x1d4;
    static final short KEY_FN_F4 = 0x1d5;
    static final short KEY_FN_F5 = 0x1d6;
    static final short KEY_FN_F6 = 0x1d7;
    static final short KEY_FN_F7 = 0x1d8;
    static final short KEY_FN_F8 = 0x1d9;
    static final short KEY_FN_F9 = 0x1da;
    static final short KEY_FN_F10 = 0x1db;
    static final short KEY_FN_F11 = 0x1dc;
    static final short KEY_FN_F12 = 0x1dd;
    static final short KEY_FN_1 = 0x1de;
    static final short KEY_FN_2 = 0x1df;
    static final short KEY_FN_D = 0x1e0;
    static final short KEY_FN_E = 0x1e1;
    static final short KEY_FN_F = 0x1e2;
    static final short KEY_FN_S = 0x1e3;
    static final short KEY_FN_B = 0x1e4;
    static final short KEY_BRL_DOT1 = 0x1f1;
    static final short KEY_BRL_DOT2 = 0x1f2;
    static final short KEY_BRL_DOT3 = 0x1f3;
    static final short KEY_BRL_DOT4 = 0x1f4;
    static final short KEY_BRL_DOT5 = 0x1f5;
    static final short KEY_BRL_DOT6 = 0x1f6;
    static final short KEY_BRL_DOT7 = 0x1f7;
    static final short KEY_BRL_DOT8 = 0x1f8;
    static final short KEY_BRL_DOT9 = 0x1f9;
    static final short KEY_BRL_DOT10 = 0x1fa;
    static final short KEY_NUMERIC_0 = 0x200;
    static final short KEY_NUMERIC_1 = 0x201;
    static final short KEY_NUMERIC_2 = 0x202;
    static final short KEY_NUMERIC_3 = 0x203;
    static final short KEY_NUMERIC_4 = 0x204;
    static final short KEY_NUMERIC_5 = 0x205;
    static final short KEY_NUMERIC_6 = 0x206;
    static final short KEY_NUMERIC_7 = 0x207;
    static final short KEY_NUMERIC_8 = 0x208;
    static final short KEY_NUMERIC_9 = 0x209;
    static final short KEY_NUMERIC_STAR = 0x20a;
    static final short KEY_NUMERIC_POUND = 0x20b;
    static final short KEY_CAMERA_FOCUS = 0x210;
    static final short KEY_WPS_BUTTON = 0x211;
    static final short KEY_TOUCHPAD_TOGGLE = 0x212;
    static final short KEY_TOUCHPAD_ON = 0x213;
    static final short KEY_TOUCHPAD_OFF = 0x214;
    static final short KEY_CAMERA_ZOOMIN = 0x215;
    static final short KEY_CAMERA_ZOOMOUT = 0x216;
    static final short KEY_CAMERA_UP = 0x217;
    static final short KEY_CAMERA_DOWN = 0x218;
    static final short KEY_CAMERA_LEFT = 0x219;
    static final short KEY_CAMERA_RIGHT = 0x21a;
    static final short BTN_TRIGGER_HAPPY = 0x2c0;
    static final short BTN_TRIGGER_HAPPY1 = 0x2c0;
    static final short BTN_TRIGGER_HAPPY2 = 0x2c1;
    static final short BTN_TRIGGER_HAPPY3 = 0x2c2;
    static final short BTN_TRIGGER_HAPPY4 = 0x2c3;
    static final short BTN_TRIGGER_HAPPY5 = 0x2c4;
    static final short BTN_TRIGGER_HAPPY6 = 0x2c5;
    static final short BTN_TRIGGER_HAPPY7 = 0x2c6;
    static final short BTN_TRIGGER_HAPPY8 = 0x2c7;
    static final short BTN_TRIGGER_HAPPY9 = 0x2c8;
    static final short BTN_TRIGGER_HAPPY10 = 0x2c9;
    static final short BTN_TRIGGER_HAPPY11 = 0x2ca;
    static final short BTN_TRIGGER_HAPPY12 = 0x2cb;
    static final short BTN_TRIGGER_HAPPY13 = 0x2cc;
    static final short BTN_TRIGGER_HAPPY14 = 0x2cd;
    static final short BTN_TRIGGER_HAPPY15 = 0x2ce;
    static final short BTN_TRIGGER_HAPPY16 = 0x2cf;
    static final short BTN_TRIGGER_HAPPY17 = 0x2d0;
    static final short BTN_TRIGGER_HAPPY18 = 0x2d1;
    static final short BTN_TRIGGER_HAPPY19 = 0x2d2;
    static final short BTN_TRIGGER_HAPPY20 = 0x2d3;
    static final short BTN_TRIGGER_HAPPY21 = 0x2d4;
    static final short BTN_TRIGGER_HAPPY22 = 0x2d5;
    static final short BTN_TRIGGER_HAPPY23 = 0x2d6;
    static final short BTN_TRIGGER_HAPPY24 = 0x2d7;
    static final short BTN_TRIGGER_HAPPY25 = 0x2d8;
    static final short BTN_TRIGGER_HAPPY26 = 0x2d9;
    static final short BTN_TRIGGER_HAPPY27 = 0x2da;
    static final short BTN_TRIGGER_HAPPY28 = 0x2db;
    static final short BTN_TRIGGER_HAPPY29 = 0x2dc;
    static final short BTN_TRIGGER_HAPPY30 = 0x2dd;
    static final short BTN_TRIGGER_HAPPY31 = 0x2de;
    static final short BTN_TRIGGER_HAPPY32 = 0x2df;
    static final short BTN_TRIGGER_HAPPY33 = 0x2e0;
    static final short BTN_TRIGGER_HAPPY34 = 0x2e1;
    static final short BTN_TRIGGER_HAPPY35 = 0x2e2;
    static final short BTN_TRIGGER_HAPPY36 = 0x2e3;
    static final short BTN_TRIGGER_HAPPY37 = 0x2e4;
    static final short BTN_TRIGGER_HAPPY38 = 0x2e5;
    static final short BTN_TRIGGER_HAPPY39 = 0x2e6;
    static final short BTN_TRIGGER_HAPPY40 = 0x2e7;

    static final short KEY_MIN_INTERESTING = KEY_MUTE;
    static final short KEY_MAX = 0x2ff;
    static final short KEY_CNT = KEY_MAX + 1;

    static final short REL_X = 0x00;
    static final short REL_Y = 0x01;
    static final short REL_Z = 0x02;
    static final short REL_RX = 0x03;
    static final short REL_RY = 0x04;
    static final short REL_RZ = 0x05;
    static final short REL_HWHEEL = 0x06;
    static final short REL_DIAL = 0x07;
    static final short REL_WHEEL = 0x08;
    static final short REL_MISC = 0x09;
    static final short REL_MAX = 0x0f;
    static final short REL_CNT = REL_MAX + 1;

    static final short ABS_X = 0x00;
    static final short ABS_Y = 0x01;
    static final short ABS_Z = 0x02;
    static final short ABS_RX = 0x03;
    static final short ABS_RY = 0x04;
    static final short ABS_RZ = 0x05;
    static final short ABS_THROTTLE = 0x06;
    static final short ABS_RUDDER = 0x07;
    static final short ABS_WHEEL = 0x08;
    static final short ABS_GAS = 0x09;
    static final short ABS_BRAKE = 0x0a;
    static final short ABS_HAT0X = 0x10;
    static final short ABS_HAT0Y = 0x11;
    static final short ABS_HAT1X = 0x12;
    static final short ABS_HAT1Y = 0x13;
    static final short ABS_HAT2X = 0x14;
    static final short ABS_HAT2Y = 0x15;
    static final short ABS_HAT3X = 0x16;
    static final short ABS_HAT3Y = 0x17;
    static final short ABS_PRESSURE = 0x18;
    static final short ABS_DISTANCE = 0x19;
    static final short ABS_TILT_X = 0x1a;
    static final short ABS_TILT_Y = 0x1b;
    static final short ABS_TOOL_WIDTH = 0x1c;
    static final short ABS_VOLUME = 0x20;
    static final short ABS_MISC = 0x28;
    static final short ABS_MT_SLOT = 0x2f;
    static final short ABS_MT_TOUCH_MAJOR = 0x30;
    static final short ABS_MT_TOUCH_MINOR = 0x31;
    static final short ABS_MT_WIDTH_MAJOR = 0x32;
    static final short ABS_MT_WIDTH_MINOR = 0x33;
    static final short ABS_MT_ORIENTATION = 0x34;
    static final short ABS_MT_POSITION_X = 0x35;
    static final short ABS_MT_POSITION_Y = 0x36;
    static final short ABS_MT_TOOL_TYPE = 0x37;
    static final short ABS_MT_BLOB_ID = 0x38;
    static final short ABS_MT_TRACKING_ID = 0x39;
    static final short ABS_MT_PRESSURE = 0x3a;
    static final short ABS_MT_DISTANCE = 0x3b;
    static final short ABS_MAX = 0x3f;
    static final short ABS_CNT = ABS_MAX + 1;

    static final short SW_LID = 0x00;
    static final short SW_TABLET_MODE = 0x01;
    static final short SW_HEADPHONE_INSERT = 0x02;
    static final short SW_RFKILL_ALL = 0x03;
    static final short SW_RADIO = SW_RFKILL_ALL;
    static final short SW_MICROPHONE_INSERT = 0x04;
    static final short SW_DOCK = 0x05;
    static final short SW_LINEOUT_INSERT = 0x06;
    static final short SW_JACK_PHYSICAL_INSERT = 0x07;
    static final short SW_VIDEOOUT_INSERT = 0x08;
    static final short SW_CAMERA_LENS_COVER = 0x09;
    static final short SW_KEYPAD_SLIDE = 0x0a;
    static final short SW_FRONT_PROXIMITY = 0x0b;
    static final short SW_ROTATE_LOCK = 0x0c;
    static final short SW_LINEIN_INSERT = 0x0d;
    static final short SW_MAX = 0x0f;
    static final short SW_CNT = SW_MAX + 1;

    static final short MSC_SERIAL = 0x00;
    static final short MSC_PULSELED = 0x01;
    static final short MSC_GESTURE = 0x02;
    static final short MSC_RAW = 0x03;
    static final short MSC_SCAN = 0x04;
    static final short MSC_MAX = 0x07;
    static final short MSC_CNT = MSC_MAX + 1;

    static final short LED_NUML = 0x00;
    static final short LED_CAPSL = 0x01;
    static final short LED_SCROLLL = 0x02;
    static final short LED_COMPOSE = 0x03;
    static final short LED_KANA = 0x04;
    static final short LED_SLEEP = 0x05;
    static final short LED_SUSPEND = 0x06;
    static final short LED_MUTE = 0x07;
    static final short LED_MISC = 0x08;
    static final short LED_MAIL = 0x09;
    static final short LED_CHARGING = 0x0a;
    static final short LED_MAX = 0x0f;
    static final short LED_CNT = LED_MAX + 1;

    static final short REP_DELAY = 0x00;
    static final short REP_PERIOD = 0x01;
    static final short REP_MAX = 0x01;
    static final short REP_CNT = REP_MAX + 1;

    static final short SND_CLICK = 0x00;
    static final short SND_BELL = 0x01;
    static final short SND_TONE = 0x02;
    static final short SND_MAX = 0x07;
    static final short SND_CNT = SND_MAX + 1;

    static final short ID_BUS = 0;
    static final short ID_VENDOR = 1;
    static final short ID_PRODUCT = 2;
    static final short ID_VERSION = 3;
    static final short BUS_PCI = 0x01;
    static final short BUS_ISAPNP = 0x02;
    static final short BUS_USB = 0x03;
    static final short BUS_HIL = 0x04;
    static final short BUS_BLUETOOTH = 0x05;
    static final short BUS_VIRTUAL = 0x06;
    static final short BUS_ISA = 0x10;
    static final short BUS_I8042 = 0x11;
    static final short BUS_XTKBD = 0x12;
    static final short BUS_RS232 = 0x13;
    static final short BUS_GAMEPORT = 0x14;
    static final short BUS_PARPORT = 0x15;
    static final short BUS_AMIGA = 0x16;
    static final short BUS_ADB = 0x17;
    static final short BUS_I2C = 0x18;
    static final short BUS_HOST = 0x19;
    static final short BUS_GSC = 0x1A;
    static final short BUS_ATARI = 0x1B;
    static final short BUS_SPI = 0x1C;

    static final short MT_TOOL_FINGER = 0;
    static final short MT_TOOL_PEN = 1;
    static final short MT_TOOL_MAX = 1;

    static final short FF_STATUS_STOPPED = 0x00;
    static final short FF_STATUS_PLAYING = 0x01;
    static final short FF_STATUS_MAX = 0x01;

    static final short FF_RUMBLE = 0x50;
    static final short FF_PERIODIC = 0x51;
    static final short FF_CONSTANT = 0x52;
    static final short FF_SPRING = 0x53;
    static final short FF_FRICTION = 0x54;
    static final short FF_DAMPER = 0x55;
    static final short FF_INERTIA = 0x56;
    static final short FF_RAMP = 0x57;
    static final short FF_EFFECT_MIN = FF_RUMBLE;
    static final short FF_EFFECT_MAX = FF_RAMP;

    static final short FF_SQUARE = 0x58;
    static final short FF_TRIANGLE = 0x59;
    static final short FF_SINE = 0x5a;
    static final short FF_SAW_UP = 0x5b;
    static final short FF_SAW_DOWN = 0x5c;
    static final short FF_CUSTOM = 0x5d;
    static final short FF_WAVEFORM_MIN = FF_SQUARE;
    static final short FF_WAVEFORM_MAX = FF_CUSTOM;

    static final short FF_GAIN = 0x60;
    static final short FF_AUTOCENTER = 0x61;
    static final short FF_MAX = 0x7f;
    static final short FF_CNT = FF_MAX + 1;

    /**
     * Convert an event type to its equivalent string. This method is
     * inefficient and is for debugging use only.
     */
    static String typeToString(short type) {
        for (Field field : LinuxInput.class.getDeclaredFields()) {
            try {
                if (field.getName().startsWith("EV_")
                        && field.getType() == Short.TYPE
                        && field.getShort(null) == type) {
                    return field.getName();
                }
            } catch (IllegalAccessException e) {
            }
        }
        return new Formatter().format("0x%04x", type & 0xffff).out().toString();
    }

    /**
     * Convert an event code to its equivalent string, given its type string.
     * The type string is needed because event codes are context sensitive. For
     * example, the code 1 is "SYN_CONFIG" for an EV_SYN event but "KEY_ESC" for
     * an EV_KEY event.  This method is inefficient and is for debugging use
     * only.
     */
    static String codeToString(String type, short code) {
        int i = type.indexOf("_");
        if (i >= 0) {
            String prefix = type.substring(i + 1);
            String altPrefix = prefix.equals("KEY") ? "BTN" : prefix;
            for (Field field : LinuxInput.class.getDeclaredFields()) {
                String name = field.getName();
                try {
                    if ((name.startsWith(prefix) || name.startsWith(altPrefix))
                            && field.getType() == Short.TYPE
                            && field.getShort(null) == code) {
                        return field.getName();
                    }
                } catch (IllegalAccessException e) {
                }
            }
        }
        return new Formatter().format("0x%04x", code & 0xffff).out().toString();
    }

}
