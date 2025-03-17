/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.jfx.incubator.scene.control.richtext.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javafx.scene.input.DataFormat;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Tests RTF Import in RichTextModel.
 */
public class RTFImportTest {
    private RichTextModel model;

    @BeforeEach
    public void beforeEach() {
        model = new RichTextModel();
    }

    @Test
    public void characterAttributes() {
        initModel(
            """
            {/rtf1/ansi/ansicpg1252/cocoartf2821
            /cocoatextscaling0/cocoaplatform0{/fonttbl/f0/fswiss/fcharset0 Helvetica-Bold;/f1/fswiss/fcharset0 Helvetica;/f2/fswiss/fcharset0 ArialMT;
            /f3/fswiss/fcharset0 Helvetica-Oblique;}
            {/colortbl;/red255/green255/blue255;/red0/green0/blue0;/red251/green0/blue7;}
            {/*/expandedcolortbl;;/cssrgb/c0/c0/c0;/cssrgb/c100000/c0/c0;}
            /margl1440/margr1440/vieww11520/viewh9000/viewkind0
            /deftab720
            /pard/pardeftab720/partightenfactor0

            /f0/b/fs24 /cf2 /expnd0/expndtw0/kerning0
            bold
            /f1/b0 /

            /f2/fs36 font
            /f1/fs24 /
            /pard/pardeftab720/partightenfactor0

            /f3/i /cf2 italic
            /f1/i0 /
            /pard/pardeftab720/partightenfactor0
            /cf2 /strike /strikec2 strikethrough/strike0/striked0 /
            /pard/pardeftab720/partightenfactor0
            /cf3 text color/cf2 /
            /pard/pardeftab720/sl398/sa213/partightenfactor0
            /cf2 /ul /ulc2 underline/ulnone /
            }
            """);

        assertEquals(7, model.size());
        // bold
        int ix = 0;
        checkCharAttr(ix, StyleAttributeMap.BOLD, Boolean.TRUE);
        checkCharAttr(ix, StyleAttributeMap.ITALIC, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.STRIKE_THROUGH, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.UNDERLINE, Boolean.FALSE);
        ix++;
        // font
        checkCharAttr(ix, StyleAttributeMap.BOLD, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.FONT_FAMILY, "ArialMT");
        checkCharAttr(ix, StyleAttributeMap.FONT_SIZE, 18.0);
        checkCharAttr(ix, StyleAttributeMap.ITALIC, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.STRIKE_THROUGH, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.UNDERLINE, Boolean.FALSE);
        ix++;
        // italic
        checkCharAttr(ix, StyleAttributeMap.BOLD, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.ITALIC, Boolean.TRUE);
        checkCharAttr(ix, StyleAttributeMap.STRIKE_THROUGH, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.UNDERLINE, Boolean.FALSE);
        ix++;
        // strikethrough
        checkCharAttr(ix, StyleAttributeMap.BOLD, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.ITALIC, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.STRIKE_THROUGH, Boolean.TRUE);
        checkCharAttr(ix, StyleAttributeMap.UNDERLINE, Boolean.FALSE);
        ix++;
        // text color
        checkCharAttr(ix, StyleAttributeMap.BOLD, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.ITALIC, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.TEXT_COLOR, Color.rgb(251, 0, 7));
        checkCharAttr(ix, StyleAttributeMap.STRIKE_THROUGH, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.UNDERLINE, Boolean.FALSE);
        ix++;
        // underline
        checkCharAttr(ix, StyleAttributeMap.BOLD, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.ITALIC, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.STRIKE_THROUGH, Boolean.FALSE);
        checkCharAttr(ix, StyleAttributeMap.UNDERLINE, Boolean.TRUE);
    }

    @Test
    public void paragraphAttributes() {
        // TODO
        // background color
        // bullet point
        // first line indent
        // line spacing
        // paragraph direction
        // space (above | below | left | right)
        // text alignment
    }


    @Test
    public void unicode() {
        initModel(
            """
            {/rtf1/adeflang1025/ansi/ansicpg1252/uc1/adeff31507/deff0/stshfdbch31505/stshfloch31506/stshfhich31506/stshfbi31507/deflang1033/deflangfe1041/themelang1033/themelangfe2052/themelangcs1025{/fonttbl{/f0/fbidi /froman/fcharset0/fprq2{/*/panose 02020603050405020304}Times New Roman;}{/f1/fbidi /fswiss/fcharset0/fprq2{/*/panose 020b0604020202020204}Arial;}
            {/f2/fbidi /fmodern/fcharset0/fprq1{/*/panose 02070309020205020404}Courier New;}{/f11/fbidi /fmodern/fcharset128/fprq1{/*/panose 02020609040205080304}MS Mincho{/*/falt /'82/'6c/'82/'72 /'96/'be/'92/'a9};}
            {/f34/fbidi /froman/fcharset0/fprq2{/*/panose 02040503050406030204}Cambria Math;}{/f36/fbidi /fnil/fcharset134/fprq2{/*/panose 02010600030101010101}DengXian{/*/falt /'b5/'c8/'cf/'df};}
            {/f45/fbidi /fswiss/fcharset0/fprq2{/*/panose 020b0004020202020204}Aptos;}{/f49/fbidi /fswiss/fcharset0/fprq2{/*/panose 020b0604030504040204}Tahoma;}{/f51/fbidi /fnil/fcharset0/fprq2{/*/panose 00000000000000000000}Apple Color Emoji;}
            {/f53/fbidi /fmodern/fcharset128/fprq1{/*/panose 02020609040205080304}@MS Mincho;}{/f54/fbidi /fnil/fcharset134/fprq2{/*/panose 02010600030101010101}@DengXian;}
            {/flomajor/f31500/fbidi /froman/fcharset0/fprq2{/*/panose 02020603050405020304}Times New Roman;}{/fdbmajor/f31501/fbidi /froman/fcharset134/fprq0{/*/panose 02010600030101010101}DengXian Light{/*/falt /'b5/'c8/'cf/'df Light};}
            {/fhimajor/f31502/fbidi /fswiss/fcharset0/fprq2{/*/panose 020b0004020202020204}Aptos Display;}{/fbimajor/f31503/fbidi /froman/fcharset0/fprq2{/*/panose 02020603050405020304}Times New Roman;}
            {/flominor/f31504/fbidi /froman/fcharset0/fprq2{/*/panose 02020603050405020304}Times New Roman;}{/fdbminor/f31505/fbidi /fnil/fcharset134/fprq2{/*/panose 02010600030101010101}DengXian{/*/falt /'b5/'c8/'cf/'df};}
            {/fhiminor/f31506/fbidi /fswiss/fcharset0/fprq2{/*/panose 020b0004020202020204}Aptos;}{/fbiminor/f31507/fbidi /fswiss/fcharset0/fprq2{/*/panose 020b0604020202020204}Arial;}{/f55/fbidi /froman/fcharset238/fprq2 Times New Roman CE;}
            {/f56/fbidi /froman/fcharset204/fprq2 Times New Roman Cyr;}{/f58/fbidi /froman/fcharset161/fprq2 Times New Roman Greek;}{/f59/fbidi /froman/fcharset162/fprq2 Times New Roman Tur;}{/f60/fbidi /froman/fcharset177/fprq2 Times New Roman (Hebrew);}
            {/f61/fbidi /froman/fcharset178/fprq2 Times New Roman (Arabic);}{/f62/fbidi /froman/fcharset186/fprq2 Times New Roman Baltic;}{/f63/fbidi /froman/fcharset163/fprq2 Times New Roman (Vietnamese);}{/f65/fbidi /fswiss/fcharset238/fprq2 Arial CE;}
            {/f66/fbidi /fswiss/fcharset204/fprq2 Arial Cyr;}{/f68/fbidi /fswiss/fcharset161/fprq2 Arial Greek;}{/f69/fbidi /fswiss/fcharset162/fprq2 Arial Tur;}{/f70/fbidi /fswiss/fcharset177/fprq2 Arial (Hebrew);}
            {/f71/fbidi /fswiss/fcharset178/fprq2 Arial (Arabic);}{/f72/fbidi /fswiss/fcharset186/fprq2 Arial Baltic;}{/f73/fbidi /fswiss/fcharset163/fprq2 Arial (Vietnamese);}{/f75/fbidi /fmodern/fcharset238/fprq1 Courier New CE;}
            {/f76/fbidi /fmodern/fcharset204/fprq1 Courier New Cyr;}{/f78/fbidi /fmodern/fcharset161/fprq1 Courier New Greek;}{/f79/fbidi /fmodern/fcharset162/fprq1 Courier New Tur;}{/f80/fbidi /fmodern/fcharset177/fprq1 Courier New (Hebrew);}
            {/f81/fbidi /fmodern/fcharset178/fprq1 Courier New (Arabic);}{/f82/fbidi /fmodern/fcharset186/fprq1 Courier New Baltic;}{/f83/fbidi /fmodern/fcharset163/fprq1 Courier New (Vietnamese);}
            {/f167/fbidi /fmodern/fcharset0/fprq1 MS Mincho Western{/*/falt /'82/'6c/'82/'72 /'96/'be/'92/'a9};}{/f165/fbidi /fmodern/fcharset238/fprq1 MS Mincho CE{/*/falt /'82/'6c/'82/'72 /'96/'be/'92/'a9};}
            {/f166/fbidi /fmodern/fcharset204/fprq1 MS Mincho Cyr{/*/falt /'82/'6c/'82/'72 /'96/'be/'92/'a9};}{/f168/fbidi /fmodern/fcharset161/fprq1 MS Mincho Greek{/*/falt /'82/'6c/'82/'72 /'96/'be/'92/'a9};}
            {/f169/fbidi /fmodern/fcharset162/fprq1 MS Mincho Tur{/*/falt /'82/'6c/'82/'72 /'96/'be/'92/'a9};}{/f172/fbidi /fmodern/fcharset186/fprq1 MS Mincho Baltic{/*/falt /'82/'6c/'82/'72 /'96/'be/'92/'a9};}{/f395/fbidi /froman/fcharset238/fprq2 Cambria Math CE;}
            {/f396/fbidi /froman/fcharset204/fprq2 Cambria Math Cyr;}{/f398/fbidi /froman/fcharset161/fprq2 Cambria Math Greek;}{/f399/fbidi /froman/fcharset162/fprq2 Cambria Math Tur;}{/f402/fbidi /froman/fcharset186/fprq2 Cambria Math Baltic;}
            {/f403/fbidi /froman/fcharset163/fprq2 Cambria Math (Vietnamese);}{/f417/fbidi /fnil/fcharset0/fprq2 DengXian Western{/*/falt /'b5/'c8/'cf/'df};}{/f415/fbidi /fnil/fcharset238/fprq2 DengXian CE{/*/falt /'b5/'c8/'cf/'df};}
            {/f416/fbidi /fnil/fcharset204/fprq2 DengXian Cyr{/*/falt /'b5/'c8/'cf/'df};}{/f418/fbidi /fnil/fcharset161/fprq2 DengXian Greek{/*/falt /'b5/'c8/'cf/'df};}{/f505/fbidi /fswiss/fcharset238/fprq2 Aptos CE;}{/f506/fbidi /fswiss/fcharset204/fprq2 Aptos Cyr;}
            {/f508/fbidi /fswiss/fcharset161/fprq2 Aptos Greek;}{/f509/fbidi /fswiss/fcharset162/fprq2 Aptos Tur;}{/f512/fbidi /fswiss/fcharset186/fprq2 Aptos Baltic;}{/f513/fbidi /fswiss/fcharset163/fprq2 Aptos (Vietnamese);}
            {/f545/fbidi /fswiss/fcharset238/fprq2 Tahoma CE;}{/f546/fbidi /fswiss/fcharset204/fprq2 Tahoma Cyr;}{/f548/fbidi /fswiss/fcharset161/fprq2 Tahoma Greek;}{/f549/fbidi /fswiss/fcharset162/fprq2 Tahoma Tur;}
            {/f550/fbidi /fswiss/fcharset177/fprq2 Tahoma (Hebrew);}{/f551/fbidi /fswiss/fcharset178/fprq2 Tahoma (Arabic);}{/f552/fbidi /fswiss/fcharset186/fprq2 Tahoma Baltic;}{/f553/fbidi /fswiss/fcharset163/fprq2 Tahoma (Vietnamese);}
            {/f554/fbidi /fswiss/fcharset222/fprq2 Tahoma (Thai);}{/f587/fbidi /fmodern/fcharset0/fprq1 @MS Mincho Western;}{/f585/fbidi /fmodern/fcharset238/fprq1 @MS Mincho CE;}{/f586/fbidi /fmodern/fcharset204/fprq1 @MS Mincho Cyr;}
            {/f588/fbidi /fmodern/fcharset161/fprq1 @MS Mincho Greek;}{/f589/fbidi /fmodern/fcharset162/fprq1 @MS Mincho Tur;}{/f592/fbidi /fmodern/fcharset186/fprq1 @MS Mincho Baltic;}{/f597/fbidi /fnil/fcharset0/fprq2 @DengXian Western;}
            {/f595/fbidi /fnil/fcharset238/fprq2 @DengXian CE;}{/f596/fbidi /fnil/fcharset204/fprq2 @DengXian Cyr;}{/f598/fbidi /fnil/fcharset161/fprq2 @DengXian Greek;}{/flomajor/f31508/fbidi /froman/fcharset238/fprq2 Times New Roman CE;}
            {/flomajor/f31509/fbidi /froman/fcharset204/fprq2 Times New Roman Cyr;}{/flomajor/f31511/fbidi /froman/fcharset161/fprq2 Times New Roman Greek;}{/flomajor/f31512/fbidi /froman/fcharset162/fprq2 Times New Roman Tur;}
            {/flomajor/f31513/fbidi /froman/fcharset177/fprq2 Times New Roman (Hebrew);}{/flomajor/f31514/fbidi /froman/fcharset178/fprq2 Times New Roman (Arabic);}{/flomajor/f31515/fbidi /froman/fcharset186/fprq2 Times New Roman Baltic;}
            {/flomajor/f31516/fbidi /froman/fcharset163/fprq2 Times New Roman (Vietnamese);}{/fhimajor/f31528/fbidi /fswiss/fcharset238/fprq2 Aptos Display CE;}{/fhimajor/f31529/fbidi /fswiss/fcharset204/fprq2 Aptos Display Cyr;}
            {/fhimajor/f31531/fbidi /fswiss/fcharset161/fprq2 Aptos Display Greek;}{/fhimajor/f31532/fbidi /fswiss/fcharset162/fprq2 Aptos Display Tur;}{/fhimajor/f31535/fbidi /fswiss/fcharset186/fprq2 Aptos Display Baltic;}
            {/fhimajor/f31536/fbidi /fswiss/fcharset163/fprq2 Aptos Display (Vietnamese);}{/fbimajor/f31538/fbidi /froman/fcharset238/fprq2 Times New Roman CE;}{/fbimajor/f31539/fbidi /froman/fcharset204/fprq2 Times New Roman Cyr;}
            {/fbimajor/f31541/fbidi /froman/fcharset161/fprq2 Times New Roman Greek;}{/fbimajor/f31542/fbidi /froman/fcharset162/fprq2 Times New Roman Tur;}{/fbimajor/f31543/fbidi /froman/fcharset177/fprq2 Times New Roman (Hebrew);}
            {/fbimajor/f31544/fbidi /froman/fcharset178/fprq2 Times New Roman (Arabic);}{/fbimajor/f31545/fbidi /froman/fcharset186/fprq2 Times New Roman Baltic;}{/fbimajor/f31546/fbidi /froman/fcharset163/fprq2 Times New Roman (Vietnamese);}
            {/flominor/f31548/fbidi /froman/fcharset238/fprq2 Times New Roman CE;}{/flominor/f31549/fbidi /froman/fcharset204/fprq2 Times New Roman Cyr;}{/flominor/f31551/fbidi /froman/fcharset161/fprq2 Times New Roman Greek;}
            {/flominor/f31552/fbidi /froman/fcharset162/fprq2 Times New Roman Tur;}{/flominor/f31553/fbidi /froman/fcharset177/fprq2 Times New Roman (Hebrew);}{/flominor/f31554/fbidi /froman/fcharset178/fprq2 Times New Roman (Arabic);}
            {/flominor/f31555/fbidi /froman/fcharset186/fprq2 Times New Roman Baltic;}{/flominor/f31556/fbidi /froman/fcharset163/fprq2 Times New Roman (Vietnamese);}{/fdbminor/f31560/fbidi /fnil/fcharset0/fprq2 DengXian Western{/*/falt /'b5/'c8/'cf/'df};}
            {/fdbminor/f31558/fbidi /fnil/fcharset238/fprq2 DengXian CE{/*/falt /'b5/'c8/'cf/'df};}{/fdbminor/f31559/fbidi /fnil/fcharset204/fprq2 DengXian Cyr{/*/falt /'b5/'c8/'cf/'df};}
            {/fdbminor/f31561/fbidi /fnil/fcharset161/fprq2 DengXian Greek{/*/falt /'b5/'c8/'cf/'df};}{/fhiminor/f31568/fbidi /fswiss/fcharset238/fprq2 Aptos CE;}{/fhiminor/f31569/fbidi /fswiss/fcharset204/fprq2 Aptos Cyr;}
            {/fhiminor/f31571/fbidi /fswiss/fcharset161/fprq2 Aptos Greek;}{/fhiminor/f31572/fbidi /fswiss/fcharset162/fprq2 Aptos Tur;}{/fhiminor/f31575/fbidi /fswiss/fcharset186/fprq2 Aptos Baltic;}
            {/fhiminor/f31576/fbidi /fswiss/fcharset163/fprq2 Aptos (Vietnamese);}{/fbiminor/f31578/fbidi /fswiss/fcharset238/fprq2 Arial CE;}{/fbiminor/f31579/fbidi /fswiss/fcharset204/fprq2 Arial Cyr;}{/fbiminor/f31581/fbidi /fswiss/fcharset161/fprq2 Arial Greek;}
            {/fbiminor/f31582/fbidi /fswiss/fcharset162/fprq2 Arial Tur;}{/fbiminor/f31583/fbidi /fswiss/fcharset177/fprq2 Arial (Hebrew);}{/fbiminor/f31584/fbidi /fswiss/fcharset178/fprq2 Arial (Arabic);}
            {/fbiminor/f31585/fbidi /fswiss/fcharset186/fprq2 Arial Baltic;}{/fbiminor/f31586/fbidi /fswiss/fcharset163/fprq2 Arial (Vietnamese);}}{/colortbl;/red0/green0/blue0;/red0/green0/blue255;/red0/green255/blue255;/red0/green255/blue0;/red255/green0/blue255;
            /red255/green0/blue0;/red255/green255/blue0;/red255/green255/blue255;/red0/green0/blue128;/red0/green128/blue128;/red0/green128/blue0;/red128/green0/blue128;/red128/green0/blue0;/red128/green128/blue0;/red128/green128/blue128;/red192/green192/blue192;
            /red0/green0/blue0;/red0/green0/blue0;/caccentone/ctint255/cshade191/red15/green71/blue97;/ctextone/ctint166/cshade255/red89/green89/blue89;/ctextone/ctint216/cshade255/red39/green39/blue39;/ctextone/ctint191/cshade255/red64/green64/blue64;}{/*/defchp
            /fs24/lang1033/langfe2052/kerning2/loch/af31506/hich/af31506/dbch/af31505/langfenp2052 }{/*/defpap /ql /li0/ri0/sa160/sl278/slmult1/widctlpar/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0 }/noqfpromote {/stylesheet{
            /ql /li0/ri0/sa160/sl278/slmult1/widctlpar/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /af31507/afs24/alang1025 /ltrch/fcs0
            /fs24/lang1033/langfe2052/kerning2/loch/f31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052 /snext0 /sqformat /spriority0 Normal;}{/s1/ql /li0/ri0/sb360/sa80/sl278/slmult1
            /keep/keepn/widctlpar/wrapdefault/aspalpha/aspnum/faauto/outlinelevel0/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /af31503/afs40/alang1025 /ltrch/fcs0
            /fs40/cf19/lang1033/langfe2052/kerning2/loch/f31502/hich/af31502/dbch/af31501/cgrid/langnp1033/langfenp2052 /sbasedon0 /snext0 /slink15 /sqformat /spriority9 /styrsid9247681 heading 1;}{/s2/ql /li0/ri0/sb160/sa80/sl278/slmult1
            /keep/keepn/widctlpar/wrapdefault/aspalpha/aspnum/faauto/outlinelevel1/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /af31503/afs32/alang1025 /ltrch/fcs0
            /fs32/cf19/lang1033/langfe2052/kerning2/loch/f31502/hich/af31502/dbch/af31501/cgrid/langnp1033/langfenp2052 /sbasedon0 /snext0 /slink16 /ssemihidden /sunhideused /sqformat /spriority9 /styrsid9247681 heading 2;}{/s3/ql /li0/ri0/sb160/sa80/sl278/slmult1
            /keep/keepn/widctlpar/wrapdefault/aspalpha/aspnum/faauto/outlinelevel2/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /af31503/afs28/alang1025 /ltrch/fcs0
            /fs28/cf19/lang1033/langfe2052/kerning2/loch/f31506/hich/af31506/dbch/af31501/cgrid/langnp1033/langfenp2052 /sbasedon0 /snext0 /slink17 /ssemihidden /sunhideused /sqformat /spriority9 /styrsid9247681 heading 3;}{/s4/ql /li0/ri0/sb80/sa40/sl278/slmult1
            /keep/keepn/widctlpar/wrapdefault/aspalpha/aspnum/faauto/outlinelevel3/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /ai/af31503/afs24/alang1025 /ltrch/fcs0
            /i/fs24/cf19/lang1033/langfe2052/kerning2/loch/f31506/hich/af31506/dbch/af31501/cgrid/langnp1033/langfenp2052 /sbasedon0 /snext0 /slink18 /ssemihidden /sunhideused /sqformat /spriority9 /styrsid9247681 heading 4;}{/s5/ql /li0/ri0/sb80/sa40/sl278/slmult1
            /keep/keepn/widctlpar/wrapdefault/aspalpha/aspnum/faauto/outlinelevel4/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /af31503/afs24/alang1025 /ltrch/fcs0
            /fs24/cf19/lang1033/langfe2052/kerning2/loch/f31506/hich/af31506/dbch/af31501/cgrid/langnp1033/langfenp2052 /sbasedon0 /snext0 /slink19 /ssemihidden /sunhideused /sqformat /spriority9 /styrsid9247681 heading 5;}{/s6/ql /li0/ri0/sb40/sl278/slmult1
            /keep/keepn/widctlpar/wrapdefault/aspalpha/aspnum/faauto/outlinelevel5/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /ai/af31503/afs24/alang1025 /ltrch/fcs0
            /i/fs24/cf20/lang1033/langfe2052/kerning2/loch/f31506/hich/af31506/dbch/af31501/cgrid/langnp1033/langfenp2052 /sbasedon0 /snext0 /slink20 /ssemihidden /sunhideused /sqformat /spriority9 /styrsid9247681 heading 6;}{/s7/ql /li0/ri0/sb40/sl278/slmult1
            /keep/keepn/widctlpar/wrapdefault/aspalpha/aspnum/faauto/outlinelevel6/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /af31503/afs24/alang1025 /ltrch/fcs0
            /fs24/cf20/lang1033/langfe2052/kerning2/loch/f31506/hich/af31506/dbch/af31501/cgrid/langnp1033/langfenp2052 /sbasedon0 /snext0 /slink21 /ssemihidden /sunhideused /sqformat /spriority9 /styrsid9247681 heading 7;}{/s8/ql /li0/ri0/sl278/slmult1
            /keep/keepn/widctlpar/wrapdefault/aspalpha/aspnum/faauto/outlinelevel7/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /ai/af31503/afs24/alang1025 /ltrch/fcs0
            /i/fs24/cf21/lang1033/langfe2052/kerning2/loch/f31506/hich/af31506/dbch/af31501/cgrid/langnp1033/langfenp2052 /sbasedon0 /snext0 /slink22 /ssemihidden /sunhideused /sqformat /spriority9 /styrsid9247681 heading 8;}{/s9/ql /li0/ri0/sl278/slmult1
            /keep/keepn/widctlpar/wrapdefault/aspalpha/aspnum/faauto/outlinelevel8/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /af31503/afs24/alang1025 /ltrch/fcs0
            /fs24/cf21/lang1033/langfe2052/kerning2/loch/f31506/hich/af31506/dbch/af31501/cgrid/langnp1033/langfenp2052 /sbasedon0 /snext0 /slink23 /ssemihidden /sunhideused /sqformat /spriority9 /styrsid9247681 heading 9;}{/*/cs10 /additive
            /ssemihidden /sunhideused /spriority1 Default Paragraph Font;}{/*
            /ts11/tsrowd/trftsWidthB3/trpaddl108/trpaddr108/trpaddfl3/trpaddft3/trpaddfb3/trpaddfr3/trcbpat1/trcfpat1/tblind0/tblindtype3/tsvertalt/tsbrdrt/tsbrdrl/tsbrdrb/tsbrdrr/tsbrdrdgl/tsbrdrdgr/tsbrdrh/tsbrdrv /ql /li0/ri0/sa160/sl278/slmult1
            /widctlpar/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /af31507/afs24/alang1025 /ltrch/fcs0 /fs24/lang1033/langfe2052/kerning2/loch/f31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052
            /snext11 /ssemihidden /sunhideused Normal Table;}{/*/cs15 /additive /rtlch/fcs1 /af31503/afs40 /ltrch/fcs0 /fs40/cf19/loch/f31502/hich/af31502/dbch/af31501 /sbasedon10 /slink1 /spriority9 /styrsid9247681 Heading 1 Char;}{/*/cs16 /additive /rtlch/fcs1
            /af31503/afs32 /ltrch/fcs0 /fs32/cf19/loch/f31502/hich/af31502/dbch/af31501 /sbasedon10 /slink2 /ssemihidden /spriority9 /styrsid9247681 Heading 2 Char;}{/*/cs17 /additive /rtlch/fcs1 /af31503/afs28 /ltrch/fcs0 /fs28/cf19/dbch/af31501
            /sbasedon10 /slink3 /ssemihidden /spriority9 /styrsid9247681 Heading 3 Char;}{/*/cs18 /additive /rtlch/fcs1 /ai/af31503 /ltrch/fcs0 /i/cf19/dbch/af31501 /sbasedon10 /slink4 /ssemihidden /spriority9 /styrsid9247681 Heading 4 Char;}{/*/cs19 /additive
            /rtlch/fcs1 /af31503 /ltrch/fcs0 /cf19/dbch/af31501 /sbasedon10 /slink5 /ssemihidden /spriority9 /styrsid9247681 Heading 5 Char;}{/*/cs20 /additive /rtlch/fcs1 /ai/af31503 /ltrch/fcs0 /i/cf20/dbch/af31501
            /sbasedon10 /slink6 /ssemihidden /spriority9 /styrsid9247681 Heading 6 Char;}{/*/cs21 /additive /rtlch/fcs1 /af31503 /ltrch/fcs0 /cf20/dbch/af31501 /sbasedon10 /slink7 /ssemihidden /spriority9 /styrsid9247681 Heading 7 Char;}{/*/cs22 /additive
            /rtlch/fcs1 /ai/af31503 /ltrch/fcs0 /i/cf21/dbch/af31501 /sbasedon10 /slink8 /ssemihidden /spriority9 /styrsid9247681 Heading 8 Char;}{/*/cs23 /additive /rtlch/fcs1 /af31503 /ltrch/fcs0 /cf21/dbch/af31501
            /sbasedon10 /slink9 /ssemihidden /spriority9 /styrsid9247681 Heading 9 Char;}{/s24/ql /li0/ri0/sa80/widctlpar/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0/contextualspace /rtlch/fcs1 /af31503/afs56/alang1025 /ltrch/fcs0
            /fs56/expnd-2/expndtw-10/lang1033/langfe2052/kerning28/loch/f31502/hich/af31502/dbch/af31501/cgrid/langnp1033/langfenp2052 /sbasedon0 /snext0 /slink25 /sqformat /spriority10 /styrsid9247681 Title;}{/*/cs25 /additive /rtlch/fcs1 /af31503/afs56 /ltrch/fcs0
            /fs56/expnd-2/expndtw-10/kerning28/loch/f31502/hich/af31502/dbch/af31501 /sbasedon10 /slink24 /spriority10 /styrsid9247681 Title Char;}{/s26/ql /li0/ri0/sa160/sl278/slmult1/widctlpar/wrapdefault/aspalpha/aspnum/faauto/ilvl1/adjustright/rin0/lin0/itap0
            /rtlch/fcs1 /af31503/afs28/alang1025 /ltrch/fcs0 /fs28/expnd3/expndtw15/cf20/lang1033/langfe2052/kerning2/loch/f31506/hich/af31506/dbch/af31501/cgrid/langnp1033/langfenp2052 /sbasedon0 /snext0 /slink27 /sqformat /spriority11 /styrsid9247681 Subtitle;}{/*
            /cs27 /additive /rtlch/fcs1 /af31503/afs28 /ltrch/fcs0 /fs28/expnd3/expndtw15/cf20/dbch/af31501 /sbasedon10 /slink26 /spriority11 /styrsid9247681 Subtitle Char;}{/s28/qc /li0/ri0/sb160/sa160/sl278/slmult1
            /widctlpar/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /ai/af31507/afs24/alang1025 /ltrch/fcs0 /i/fs24/cf22/lang1033/langfe2052/kerning2/loch/f31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052
            /sbasedon0 /snext0 /slink29 /sqformat /spriority29 /styrsid9247681 Quote;}{/*/cs29 /additive /rtlch/fcs1 /ai/af0 /ltrch/fcs0 /i/cf22 /sbasedon10 /slink28 /spriority29 /styrsid9247681 Quote Char;}{/s30/ql /li720/ri0/sa160/sl278/slmult1
            /widctlpar/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin720/itap0/contextualspace /rtlch/fcs1 /af31507/afs24/alang1025 /ltrch/fcs0 /fs24/lang1033/langfe2052/kerning2/loch/f31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052
            /sbasedon0 /snext30 /sqformat /spriority34 /styrsid9247681 List Paragraph;}{/*/cs31 /additive /rtlch/fcs1 /ai/af0 /ltrch/fcs0 /i/cf19 /sbasedon10 /sqformat /spriority21 /styrsid9247681 Intense Emphasis;}{/s32/qc /li864/ri864/sb360/sa360/sl278/slmult1
            /widctlpar/brdrt/brdrs/brdrw10/brsp200/brdrcf19 /brdrb/brdrs/brdrw10/brsp200/brdrcf19 /wrapdefault/aspalpha/aspnum/faauto/adjustright/rin864/lin864/itap0 /rtlch/fcs1 /ai/af31507/afs24/alang1025 /ltrch/fcs0
            /i/fs24/cf19/lang1033/langfe2052/kerning2/loch/f31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052 /sbasedon0 /snext0 /slink33 /sqformat /spriority30 /styrsid9247681 Intense Quote;}{/*/cs33 /additive /rtlch/fcs1 /ai/af0 /ltrch/fcs0 /i/cf19
            /sbasedon10 /slink32 /spriority30 /styrsid9247681 Intense Quote Char;}{/*/cs34 /additive /rtlch/fcs1 /ab/af0 /ltrch/fcs0 /b/scaps/expnd1/expndtw5/cf19 /sbasedon10 /sqformat /spriority32 /styrsid9247681 Intense Reference;}{
            /s35/ql /li0/ri0/sb100/sa100/sbauto1/saauto1/widctlpar/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /af0/afs24/alang1025 /ltrch/fcs0 /fs24/lang1033/langfe2052/cgrid/langnp1033/langfenp2052
            /sbasedon0 /snext35 /ssemihidden /sunhideused /styrsid9247681 Normal (Web);}{/s36/ql /li0/ri0/widctlpar/tqc/tx4680/tqr/tx9360/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /af31507/afs24/alang1025 /ltrch/fcs0
            /fs24/lang1033/langfe2052/kerning2/loch/f31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052 /sbasedon0 /snext36 /slink37 /sunhideused /styrsid9247681 header;}{/*/cs37 /additive /rtlch/fcs1 /af0 /ltrch/fcs0 /sbasedon10 /slink36 /styrsid9247681
            Header Char;}{/s38/ql /li0/ri0/widctlpar/tqc/tx4680/tqr/tx9360/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /af31507/afs24/alang1025 /ltrch/fcs0
            /fs24/lang1033/langfe2052/kerning2/loch/f31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052 /sbasedon0 /snext38 /slink39 /sunhideused /styrsid9247681 footer;}{/*/cs39 /additive /rtlch/fcs1 /af0 /ltrch/fcs0 /sbasedon10 /slink38 /styrsid9247681
            Footer Char;}}{/*/pgptbl {/pgp/ipgp0/itap0/li0/ri0/sb0/sa0}}{/*/rsidtbl /rsid551370/rsid3889742/rsid9128096/rsid9247681/rsid9312892/rsid11883584/rsid12801700/rsid13593564/rsid15815019/rsid16394011}{/mmathPr/mmathFont34/mbrkBin0/mbrkBinSub0/msmallFrac0
            /mdispDef1/mlMargin0/mrMargin0/mdefJc1/mwrapIndent1440/mintLim0/mnaryLim1}{/info{/author Kevin Rushforth}{/operator Andy Goryachev}{/creatim/yr2025/mo3/dy17/hr8/min7}{/revtim/yr2025/mo3/dy17/hr8/min7}{/version2}{/edmins0}{/nofpages1}{/nofwords48}
            {/nofchars280}{/nofcharsws327}{/vern3975}}{/*/xmlnstbl {/xmlns1 http://schemas.microsoft.com/office/word/2003/wordml}}
            /paperw12240/paperh15840/margl1440/margr1440/margt1440/margb1440/gutter0/ltrsect
            /widowctrl/ftnbj/aenddoc/trackmoves0/trackformatting1/donotembedsysfont1/relyonvml0/donotembedlingdata0/grfdocevents0/validatexml1/showplaceholdtext0/ignoremixedcontent0/saveinvalidxml0/showxmlerrors1/noxlattoyen
            /expshrtn/noultrlspc/dntblnsbdb/nospaceforul/formshade/horzdoc/dgmargin/dghspace180/dgvspace180/dghorigin1440/dgvorigin1440/dghshow1/dgvshow1
            /jexpand/viewkind1/viewscale100/pgbrdrhead/pgbrdrfoot/splytwnine/ftnlytwnine/htmautsp/nolnhtadjtbl/useltbaln/alntblind/lytcalctblwd/lyttblrtgr/lnbrkrule/nobrkwrptbl/snaptogridincell/allowfieldendsel/wrppunct
            /asianbrkrule/rsidroot9247681/newtblstyruls/nogrowautofit/usenormstyforlist/noindnmbrts/felnbrelev/nocxsptable/indrlsweleven/noafcnsttbl/afelev/utinl/hwelev/spltpgpar/notcvasp/notbrkcnstfrctbl/notvatxbx/krnprsnet/cachedcolbal /nouicompat /fet0
            {/*/wgrffmtfilter 2450}/nofeaturethrottle1/ilfomacatclnup0{/*/ftnsep /ltrpar /pard/plain /ltrpar/ql /li0/ri0/widctlpar/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0/pararsid9247681 /rtlch/fcs1 /af31507/afs24/alang1025 /ltrch/fcs0
            /fs24/lang1033/langfe2052/kerning2/loch/af31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052 {/rtlch/fcs1 /af31507 /ltrch/fcs0 /insrsid12801700 /chftnsep
            /par }}{/*/ftnsepc /ltrpar /pard/plain /ltrpar/ql /li0/ri0/widctlpar/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0/pararsid9247681 /rtlch/fcs1 /af31507/afs24/alang1025 /ltrch/fcs0
            /fs24/lang1033/langfe2052/kerning2/loch/af31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052 {/rtlch/fcs1 /af31507 /ltrch/fcs0 /insrsid12801700 /chftnsepc
            /par }}{/*/aftnsep /ltrpar /pard/plain /ltrpar/ql /li0/ri0/widctlpar/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0/pararsid9247681 /rtlch/fcs1 /af31507/afs24/alang1025 /ltrch/fcs0
            /fs24/lang1033/langfe2052/kerning2/loch/af31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052 {/rtlch/fcs1 /af31507 /ltrch/fcs0 /insrsid12801700 /chftnsep
            /par }}{/*/aftnsepc /ltrpar /pard/plain /ltrpar/ql /li0/ri0/widctlpar/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0/pararsid9247681 /rtlch/fcs1 /af31507/afs24/alang1025 /ltrch/fcs0
            /fs24/lang1033/langfe2052/kerning2/loch/af31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052 {/rtlch/fcs1 /af31507 /ltrch/fcs0 /insrsid12801700 /chftnsepc
            /par }}/ltrpar /sectd /ltrsect/linex0/endnhere/sectlinegrid360/sectdefaultcl/sftnbj {/headerl /ltrpar /pard/plain /ltrpar/s36/ql /li0/ri0/widctlpar/tqc/tx4680/tqr/tx9360/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0 /rtlch/fcs1
            /af31507/afs24/alang1025 /ltrch/fcs0 /fs24/lang1033/langfe2052/kerning2/loch/af31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052 {/rtlch/fcs1 /af31507 /ltrch/fcs0 /insrsid9247681
            /par }}{/headerr /ltrpar /pard/plain /ltrpar/s36/ql /li0/ri0/widctlpar/tqc/tx4680/tqr/tx9360/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /af31507/afs24/alang1025 /ltrch/fcs0
            /fs24/lang1033/langfe2052/kerning2/loch/af31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052 {/rtlch/fcs1 /af31507 /ltrch/fcs0 /insrsid9247681
            /par }}{/footerl /ltrpar /pard/plain /ltrpar/s38/ql /li0/ri0/widctlpar/tqc/tx4680/tqr/tx9360/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /af31507/afs24/alang1025 /ltrch/fcs0
            /fs24/lang1033/langfe2052/kerning2/loch/af31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052 {/rtlch/fcs1 /af31507 /ltrch/fcs0 /insrsid9247681
            /par }}{/footerr /ltrpar /pard/plain /ltrpar/s38/ql /li0/ri0/widctlpar/tqc/tx4680/tqr/tx9360/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /af31507/afs24/alang1025 /ltrch/fcs0
            /fs24/lang1033/langfe2052/kerning2/loch/af31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052 {/rtlch/fcs1 /af31507 /ltrch/fcs0 /insrsid9247681
            /par }}{/headerf /ltrpar /pard/plain /ltrpar/s36/ql /li0/ri0/widctlpar/tqc/tx4680/tqr/tx9360/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /af31507/afs24/alang1025 /ltrch/fcs0
            /fs24/lang1033/langfe2052/kerning2/loch/af31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052 {/rtlch/fcs1 /af31507 /ltrch/fcs0 /insrsid9247681
            /par }}{/footerf /ltrpar /pard/plain /ltrpar/s38/ql /li0/ri0/widctlpar/tqc/tx4680/tqr/tx9360/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0 /rtlch/fcs1 /af31507/afs24/alang1025 /ltrch/fcs0
            /fs24/lang1033/langfe2052/kerning2/loch/af31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052 {/rtlch/fcs1 /af31507 /ltrch/fcs0 /insrsid9247681
            /par }}{/*/pnseclvl1/pnucrm/pnqc/pnstart1/pnindent720/pnhang {/pntxta .}}{/*/pnseclvl2/pnucltr/pnqc/pnstart1/pnindent720/pnhang {/pntxta .}}{/*/pnseclvl3/pndec/pnqc/pnstart1/pnindent720/pnhang {/pntxta .}}{/*/pnseclvl4/pnlcltr/pnqc/pnstart1/pnindent720/pnhang
            {/pntxta )}}{/*/pnseclvl5/pndec/pnqc/pnstart1/pnindent720/pnhang {/pntxtb (}{/pntxta )}}{/*/pnseclvl6/pnlcltr/pnqc/pnstart1/pnindent720/pnhang {/pntxtb (}{/pntxta )}}{/*/pnseclvl7/pnlcrm/pnqc/pnstart1/pnindent720/pnhang {/pntxtb (}{/pntxta )}}
            {/*/pnseclvl8/pnlcltr/pnqc/pnstart1/pnindent720/pnhang {/pntxtb (}{/pntxta )}}{/*/pnseclvl9/pnlcrm/pnqc/pnstart1/pnindent720/pnhang {/pntxtb (}{/pntxta )}}/pard/plain /ltrpar
            /ql /li0/ri0/widctlpar/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0/pararsid9247681 /rtlch/fcs1 /af31507/afs24/alang1025 /ltrch/fcs0 /fs24/lang1033/langfe2052/kerning2/loch/af31506/hich/af31506/dbch/af31505/cgrid/langnp1033/langfenp2052
            {/rtlch/fcs1 /af551/afs52 /ltrch/fcs0 /f551/fs52/cf1/kerning0/insrsid9247681/charrsid9247681 A regular Arabic verb, /'df/'f3/'ca/'f3/'c8/'f3/'fd kataba (to write).}{/rtlch/fcs1 /af0 /ltrch/fcs0 /f0/kerning0/insrsid9247681/charrsid9247681
            /par }/pard /ltrpar/ql /li0/ri0/sb100/sa100/sbauto1/saauto1/widctlpar/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0/pararsid9247681 {/rtlch/fcs1 /af0/afs52 /ltrch/fcs0 /f1/fs52/cf1/kerning0/insrsid9247681/charrsid9247681 Emojis: [}{
            /rtlch/fcs1 /af51/afs52 /ltrch/fcs0 /f51/fs52/cf1/kerning0/insrsid9247681/charrsid9247681 /u-10179/'5f/u-8923/'5f/u-10178/'5f/u-8821/'5f/u-10179/'5f/u-8704/'5f/u-10179/'5f/u-8701/'5f/u-10179/'5f/u-8700/'5f/u-10179/'5f/u-8703/'5f/u-10179/'5f/u-8698/'5f
            /u-10179/'5f/u-8699/'5f/u-10178/'5f/u-8925/'5f/u-10179/'5f/u-8702/'5f/u-10179/'5f/u-8638/'5f/u-10179/'5f/u-8637/'5f/u-10179/'5f/u-8695/'5f/u-10179/'5f/u-8694/'5f/u-10179/'5f/u-8697/'5f}{/rtlch/fcs1 /af0/afs52 /ltrch/fcs0
            /f1/fs52/cf1/kerning0/insrsid9247681/charrsid9247681 ]}{/rtlch/fcs1 /af0 /ltrch/fcs0 /f0/kerning0/insrsid9247681/charrsid9247681
            /par }{/rtlch/fcs1 /af0/afs26 /ltrch/fcs0 /f1/fs26/ul/cf1/kerning0/insrsid9247681/charrsid9247681 Halfwidth and FullWidth Forms}{/rtlch/fcs1 /af0 /ltrch/fcs0 /f0/kerning0/insrsid9247681/charrsid9247681
            /par }{/rtlch/fcs1 /af11/afs26 /ltrch/fcs0 /fs26/cf1/kerning0/loch/af11/hich/af11/dbch/af11/insrsid9247681/charrsid9247681 /loch/af11/hich/af11/dbch/f11 /uc2/u-223/'82/'60/u-222/'82/'61/u-221/'82/'62/u-220/'82/'63/u-219/'82/'64/u-218/'82/'65/u-217/'82/'66
            /u-216/'82/'67/u-215/'82/'68/u-214/'82/'69/u-213/'82/'6a/u-212/'82/'6b/u-211/'82/'6c/u-210/'82/'6d}{/rtlch/fcs1 /af11/afs26 /ltrch/fcs0 /fs26/cf1/kerning0/loch/af11/hich/af11/dbch/af11/insrsid9247681/charrsid9247681 /loch/af11/hich/af11/dbch/f11
            /uc2/u-209/'82/'6e}{/rtlch/fcs1 /af0 /ltrch/fcs0 /f0/kerning0/insrsid9247681/charrsid9247681
            /par }{/rtlch/fcs1 /af2/afs26 /ltrch/fcs0 /f2/fs26/cf1/kerning0/insrsid9247681/charrsid9247681 ABCDEFGHIJKLMNO}{/rtlch/fcs1 /af0 /ltrch/fcs0 /f0/kerning0/insrsid9247681/charrsid9247681
            /par }{/rtlch/fcs1 /af2/afs26 /ltrch/fcs0 /f2/fs26/cf1/kerning0/insrsid9247681/charrsid9247681 leading and trailing whitespace }{/rtlch/fcs1 /af0 /ltrch/fcs0 /f0/kerning0/insrsid9247681/charrsid9247681
            /par }{/rtlch/fcs1 /af0/afs52 /ltrch/fcs0 /f1/fs52/cf1/kerning0/insrsid9247681/charrsid9247681 Behold various types of highlights, including overlapping highlights.}{/rtlch/fcs1 /af0 /ltrch/fcs0 /f0/kerning0/insrsid9247681/charrsid9247681
            /par Behold various types of highlights, including overlapping highlights.
            /par }/pard /ltrpar/ql /li0/ri0/sa160/sl278/slmult1/widctlpar/wrapdefault/aspalpha/aspnum/faauto/adjustright/rin0/lin0/itap0 {/rtlch/fcs1 /af31507 /ltrch/fcs0 /insrsid16394011
            /par }
            {/*/datastore }}""");

        assertEquals(10, model.size());
        assertEquals("A regular Arabic verb, كَتَبَ‎ kataba (to write).", model.getPlainText(0));
    }

    private <T> void checkCharAttr(int paragraphIndex, StyleAttribute<T> attribute, T value) {
        TextPos end = model.getEndOfParagraphTextPos(paragraphIndex);
        TextPos p = TextPos.ofLeading(paragraphIndex, end.charIndex() / 2);
        StyleAttributeMap attrs = model.getStyleAttributeMap(null, p);

        assertEquals(value, attrs.get(attribute));
    }

    private void initModel(String mangledRTF) {
        try {
            // demangle to RTF (replace / with \ characters)
            String rtf = mangledRTF.replace('/', '\\');
            ByteArrayInputStream in = new ByteArrayInputStream(rtf.getBytes(StandardCharsets.US_ASCII));
            model.read(null, DataFormat.RTF, in);
        } catch(Exception e) {
            fail(e);
        }
    }
}
