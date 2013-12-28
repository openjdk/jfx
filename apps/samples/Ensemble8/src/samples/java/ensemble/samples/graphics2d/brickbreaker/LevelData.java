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
package ensemble.samples.graphics2d.brickbreaker;

import java.util.Arrays;

import javafx.collections.ObservableList;

public class LevelData {

    private static final String NEXT_LEVEL = "---";

    private static final String[] LEVELS_DATA = new String[] {
        "",
        "",
        "",
        "",
        "WWWWWWWWWWWWWWW",
        "WYYWYYWYWYYWYYW",
        "WWWWWWWWWWWWWWW",
        "WWWWWWWWWWWWWWW",
        "RWRWRWRWRWRWRWR",
        "WRWRWRWRWRWRWRW",
        "WWWWWWWWWWWWWWW",
        "LLLLLLLLLLLLLLL",

        NEXT_LEVEL,

        "",
        "",
        "",
        "",
        "",
        "W              ",
        "WO             ",
        "WOG            ",
        "WOGR           ",
        "WOGRB          ",
        "WOGRBC         ",
        "WOGRBCL        ",
        "WOGRBCLV       ",
        "WOGRBCLVY      ",
        "WOGRBCLVYM     ",
        "WOGRBCLVYMW    ",
        "WOGRBCLVYMWO   ",
        "WOGRBCLVYMWOG  ",
        "WOGRBCLVYMWOGR ",
        "22222222222222B",

        NEXT_LEVEL,

        "",
        "",
        "",
        "00    000000000",
        "",
        "    222 222 222",
        "    2G2 2G2 2G2",
        "    222 222 222",
        "",
        "  222 222 222  ",
        "  2R2 2R2 2R2  ",
        "  222 222 222  ",
        "",
        "222 222 222    ",
        "2L2 2L2 2L2    ",
        "222 222 222    ",

        NEXT_LEVEL,

        "RRRRRRRRRRRRRRR",
        "RWWWWWWWWWWWWWR",
        "RWRRRRRRRRRRRWR",
        "RWRWWWWWWWWWRWR",
        "RWRWRRRRRRRWRWR",
        "RWRWR     RWRWR",
        "RWRWR     RWRWR",
        "RWRWR     RWRWR",
        "RWRWR     RWRWR",
        "RWRWR     RWRWR",
        "RWRW2222222WRWR",
        "",
        "",
        "222222222222222",

        NEXT_LEVEL,

        "",
        "    Y     Y    ",
        "    Y     Y    ",
        "     Y   Y     ",
        "     Y   Y     ",
        "    2222222    ",
        "   222222222   ",
        "   22R222R22   ",
        "  222R222R222  ",
        " 2222222222222 ",
        " 2222222222222 ",
        " 2222222222222 ",
        " 2 222222222 2 ",
        " 2 2       2 2 ",
        " 2 2       2 2 ",
        "    222 222    ",
        "    222 222    ",

        NEXT_LEVEL,

        "OOOOOOOOOOOOOOO",
        "OOOOOOOOOOOOOOO",
        "OOOOOOOOOOOOOOO",
        "",
        "",
        "GGGGGGGGGGGGGGG",
        "GGGGGGGGGGGGGGG",
        "GGGGGGGGGGGGGGG",
        "",
        "",
        "YYYYYYWWWYYYYYY",
        "222222WWW222222",
        "YYYYYYWWWYYYYYY",
        "YYY0       0YYY",
        "YY           YY",
        "Y             Y",

        NEXT_LEVEL,

        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "R O Y W G B C M",
        "2 2 2 2 2 2 2 2",

        NEXT_LEVEL,

        "",
        "",
        "RRR YYY G G RRR",
        "  R Y Y G G R R",
        "  R Y Y G G R R",
        "  R YYY G G RRR",
        "  R Y Y G G R R",
        "  R Y Y G G R R",
        "RR  Y Y  G  R R",
        "               ",
        "    222 2 2    ",
        "    2   2 2    ",
        "    2   2 2    ",
        "    222  2     ",
        "    2   2 2    ",
        "    2   2 2    ",
        "    2   2 2    ",
    };

    private static ObservableList<Integer> levelsOffsets;

    public static int getLevelsCount() {
        initLevelsOffsets();
        return levelsOffsets.size() - 1;
    }

    public static String[] getLevelData(int level) {
        initLevelsOffsets();
        if (level < 1 || level > getLevelsCount()) {
            return null;
        } else {
            return Arrays.copyOfRange(LEVELS_DATA, levelsOffsets.get(level - 1) + 1, levelsOffsets.get(level));
        }
    }

    private static void initLevelsOffsets() {
        if (levelsOffsets == null) {
            levelsOffsets = javafx.collections.FXCollections.<Integer>observableArrayList();
            levelsOffsets.add(-1);
            for (int i = 0; i < LEVELS_DATA.length; i++) {
                if (LEVELS_DATA[i].equals(NEXT_LEVEL)) {
                    levelsOffsets.add(i);
                }
            }
            levelsOffsets.add(LEVELS_DATA.length + 1);
        }
    }

}

