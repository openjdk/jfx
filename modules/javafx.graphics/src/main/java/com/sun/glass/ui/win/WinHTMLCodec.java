/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.win;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

final class WinHTMLCodec {
    public static final String defaultCharset = "UTF-8";

    public static byte[] encode(byte[] html) {
        return HTMLCodec.convertToHTMLFormat(html);
    }

    public static byte[] decode(byte[] data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            InputStream is = new HTMLCodec(bais, EHTMLReadMode.HTML_READ_SELECTION);

            // the initial size is larger, but we avoid relocations
            ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);

            // Very inefficient. But clipboard operations shouldn't be super fast either.
            // Might use available() to estimate a buffer size, and copy blocks instead.
            int b;
            while ((b = is.read()) != -1) {
                baos.write(b);
            }

            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Unexpected IOException caught", ex);
        }
    }
}

// ***************************************************************************
// The code below is copied from JDK unmodified
// ***************************************************************************

enum EHTMLReadMode {
    HTML_READ_ALL,
    HTML_READ_FRAGMENT,
    HTML_READ_SELECTION
}

/**
 * on decode: This stream takes an InputStream which provides data in CF_HTML format,
 * strips off the description and context to extract the original HTML data.
 *
 * on encode: static convertToHTMLFormat is responsible for HTML clipboard header creation
 */
class HTMLCodec extends InputStream {
    //static section
    public static final String ENCODING = "UTF-8";

    public static final String VERSION = "Version:";
    public static final String START_HTML = "StartHTML:";
    public static final String END_HTML = "EndHTML:";
    public static final String START_FRAGMENT = "StartFragment:";
    public static final String END_FRAGMENT = "EndFragment:";
    public static final String START_SELECTION = "StartSelection:"; //optional
    public static final String END_SELECTION = "EndSelection:"; //optional

    public static final String START_FRAGMENT_CMT = "<!--StartFragment-->";
    public static final String END_FRAGMENT_CMT = "<!--EndFragment-->";
    public static final String SOURCE_URL = "SourceURL:";
    public static final String DEF_SOURCE_URL = "about:blank";

    public static final String EOLN = "\r\n";

    private static final String VERSION_NUM = "1.0";
    private static final int PADDED_WIDTH = 10;

    private static String toPaddedString(int n, int width) {
        String string = "" + n;
        int len = string.length();
        if (n >= 0 && len < width) {
            char[] array = new char[width - len];
            Arrays.fill(array, '0');
            StringBuffer buffer = new StringBuffer(width);
            buffer.append(array);
            buffer.append(string);
            string = buffer.toString();
        }
        return string;
    }

    /**
     * convertToHTMLFormat adds the MS HTML clipboard header to byte array that
     * contains the parameters pairs.
     *
     * The consequence of parameters is fixed, but some or all of them could be
     * omitted. One parameter per one text line.
     * It looks like that:
     *
     * Version:1.0\r\n                -- current supported version
     * StartHTML:000000192\r\n        -- shift in array to the first byte after the header
     * EndHTML:000000757\r\n          -- shift in array of last byte for HTML syntax analysis
     * StartFragment:000000396\r\n    -- shift in array jast after <!--StartFragment-->
     * EndFragment:000000694\r\n      -- shift in array before start  <!--EndFragment-->
     * StartSelection:000000398\r\n   -- shift in array of the first char in copied selection
     * EndSelection:000000692\r\n     -- shift in array of the last char in copied selection
     * SourceURL:http://sun.com/\r\n  -- base URL for related referenses
     * <HTML>...<BODY>...<!--StartFragment-->.....................<!--EndFragment-->...</BODY><HTML>
     * ^                                     ^ ^                ^^                                 ^
     * \ StartHTML                           | \-StartSelection | \EndFragment              EndHTML/
     *                                       \-StartFragment    \EndSelection
     *
     *Combinations with tags sequence
     *<!--StartFragment--><HTML>...<BODY>...</BODY><HTML><!--EndFragment-->
     * or
     *<HTML>...<!--StartFragment-->...<BODY>...</BODY><!--EndFragment--><HTML>
     * are vailid too.
     */
    public static byte[] convertToHTMLFormat(byte[] bytes) {
        // Calculate section offsets
        String htmlPrefix = "";
        String htmlSuffix = "";
        {
            //we have extend the fragment to full HTML document correctly
            //to avoid HTML and BODY tags doubling
            String stContext = new String(bytes);
            String stUpContext = stContext.toUpperCase();
            if( -1 == stUpContext.indexOf("<HTML") ) {
                htmlPrefix = "<HTML>";
                htmlSuffix = "</HTML>";
                if( -1 == stUpContext.indexOf("<BODY") ) {
                    htmlPrefix = htmlPrefix +"<BODY>";
                    htmlSuffix = "</BODY>" + htmlSuffix;
                }
            }
            htmlPrefix = htmlPrefix + START_FRAGMENT_CMT;
            htmlSuffix = END_FRAGMENT_CMT + htmlSuffix;
        }

        String stBaseUrl = DEF_SOURCE_URL;
        int nStartHTML =
            VERSION.length() + VERSION_NUM.length() + EOLN.length()
            + START_HTML.length() + PADDED_WIDTH + EOLN.length()
            + END_HTML.length() + PADDED_WIDTH + EOLN.length()
            + START_FRAGMENT.length() + PADDED_WIDTH + EOLN.length()
            + END_FRAGMENT.length() + PADDED_WIDTH + EOLN.length()
            + SOURCE_URL.length() + stBaseUrl.length() + EOLN.length()
            ;
        int nStartFragment = nStartHTML + htmlPrefix.length();
        int nEndFragment = nStartFragment + bytes.length - 1;
        int nEndHTML = nEndFragment + htmlSuffix.length();

        StringBuilder header = new StringBuilder(
            nStartFragment
            + START_FRAGMENT_CMT.length()
        );
        //header
        header.append(VERSION);
        header.append(VERSION_NUM);
        header.append(EOLN);

        header.append(START_HTML);
        header.append(toPaddedString(nStartHTML, PADDED_WIDTH));
        header.append(EOLN);

        header.append(END_HTML);
        header.append(toPaddedString(nEndHTML, PADDED_WIDTH));
        header.append(EOLN);

        header.append(START_FRAGMENT);
        header.append(toPaddedString(nStartFragment, PADDED_WIDTH));
        header.append(EOLN);

        header.append(END_FRAGMENT);
        header.append(toPaddedString(nEndFragment, PADDED_WIDTH));
        header.append(EOLN);

        header.append(SOURCE_URL);
        header.append(stBaseUrl);
        header.append(EOLN);

        //HTML
        header.append(htmlPrefix);

        byte[] headerBytes = null, trailerBytes = null;

        try {
            headerBytes = header.toString().getBytes(ENCODING);
            trailerBytes = htmlSuffix.getBytes(ENCODING);
        } catch (UnsupportedEncodingException cannotHappen) {
            return null;
        }

        byte[] retval = new byte[headerBytes.length + bytes.length +
                                 trailerBytes.length];

        System.arraycopy(headerBytes, 0, retval, 0, headerBytes.length);
        System.arraycopy(bytes, 0, retval, headerBytes.length,
                       bytes.length - 1);
        System.arraycopy(trailerBytes, 0, retval,
                         headerBytes.length + bytes.length - 1,
                         trailerBytes.length);
        retval[retval.length-1] = 0;

        return retval;
    }

    //----------------------------------
    //decoder instance data and methods:

    private final BufferedInputStream bufferedStream;
    private boolean descriptionParsed = false;
    private boolean closed = false;

     // InputStreamReader uses an 8K buffer. The size is not customizable.
    public static final int BYTE_BUFFER_LEN = 8192;

    // CharToByteUTF8.getMaxBytesPerChar returns 3, so we should not buffer
    // more chars than 3 times the number of bytes we can buffer.
    public static final int CHAR_BUFFER_LEN = BYTE_BUFFER_LEN / 3;

    private static final String FAILURE_MSG =
        "Unable to parse HTML description: ";
    private static final String INVALID_MSG =
        " invalid";

    //HTML header mapping:
    private long   iHTMLStart,// StartHTML -- shift in array to the first byte after the header
                   iHTMLEnd,  // EndHTML -- shift in array of last byte for HTML syntax analysis
                   iFragStart,// StartFragment -- shift in array jast after <!--StartFragment-->
                   iFragEnd,  // EndFragment -- shift in array before start <!--EndFragment-->
                   iSelStart, // StartSelection -- shift in array of the first char in copied selection
                   iSelEnd;   // EndSelection -- shift in array of the last char in copied selection
    private String stBaseURL; // SourceURL -- base URL for related referenses
    private String stVersion; // Version -- current supported version

    //Stream reader markers:
    private long iStartOffset,
                 iEndOffset,
                 iReadCount;

    private EHTMLReadMode readMode;

    public HTMLCodec(
        InputStream _bytestream,
        EHTMLReadMode _readMode)
    {
        bufferedStream = new BufferedInputStream(_bytestream, BYTE_BUFFER_LEN);
        readMode = _readMode;
    }

    public synchronized String getBaseURL() throws IOException
    {
        if( !descriptionParsed ) {
            parseDescription();
        }
        return stBaseURL;
    }
    public synchronized String getVersion() throws IOException
    {
        if( !descriptionParsed ) {
            parseDescription();
        }
        return stVersion;
    }

    /**
     * parseDescription parsing HTML clipboard header as it described in
     * comment to convertToHTMLFormat
     */
    private void parseDescription() throws IOException
    {
        stBaseURL = null;
        stVersion = null;

        // initialization of array offset pointers
        // to the same "uninitialized" state.
        iHTMLEnd =
            iHTMLStart =
                iFragEnd =
                    iFragStart =
                        iSelEnd =
                            iSelStart = -1;

        bufferedStream.mark(BYTE_BUFFER_LEN);
        String astEntries[] = new String[] {
            //common
            VERSION,
            START_HTML,
            END_HTML,
            START_FRAGMENT,
            END_FRAGMENT,
            //ver 1.0
            START_SELECTION,
            END_SELECTION,
            SOURCE_URL
        };
        BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(
                bufferedStream,
                ENCODING
            ),
            CHAR_BUFFER_LEN
        );
        long iHeadSize = 0;
        long iCRSize = EOLN.length();
        int iEntCount = astEntries.length;
        boolean bContinue = true;

        for( int  iEntry = 0; iEntry < iEntCount; ++iEntry ){
            String stLine = bufferedReader.readLine();
            if( null==stLine ) {
                break;
            }
            //some header entries are optional, but the order is fixed.
            for( ; iEntry < iEntCount; ++iEntry ){
                if( !stLine.startsWith(astEntries[iEntry]) ) {
                    continue;
                }
                iHeadSize += stLine.length() + iCRSize;
                String stValue = stLine.substring(astEntries[iEntry].length()).trim();
                if( null!=stValue ) {
                    try{
                        switch( iEntry ){
                        case 0:
                            stVersion = stValue;
                            break;
                        case 1:
                            iHTMLStart = Integer.parseInt(stValue);
                            break;
                        case 2:
                            iHTMLEnd = Integer.parseInt(stValue);
                            break;
                        case 3:
                            iFragStart = Integer.parseInt(stValue);
                            break;
                        case 4:
                            iFragEnd = Integer.parseInt(stValue);
                            break;
                        case 5:
                            iSelStart = Integer.parseInt(stValue);
                            break;
                        case 6:
                            iSelEnd = Integer.parseInt(stValue);
                            break;
                        case 7:
                            stBaseURL = stValue;
                            break;
                        }
                    } catch ( NumberFormatException e ) {
                        throw new IOException(FAILURE_MSG + astEntries[iEntry]+ " value " + e + INVALID_MSG);
                    }
                }
                break;
            }
        }
        //some entries could absent in HTML header,
        //so we have find they by another way.
        if( -1 == iHTMLStart )
            iHTMLStart = iHeadSize;
        if( -1 == iFragStart )
            iFragStart = iHTMLStart;
        if( -1 == iFragEnd )
            iFragEnd = iHTMLEnd;
        if( -1 == iSelStart )
            iSelStart = iFragStart;
        if( -1 == iSelEnd )
            iSelEnd = iFragEnd;

        //one of possible modes
        switch( readMode ){
        case HTML_READ_ALL:
            iStartOffset = iHTMLStart;
            iEndOffset = iHTMLEnd;
            break;
        case HTML_READ_FRAGMENT:
            iStartOffset = iFragStart;
            iEndOffset = iFragEnd;
            break;
        case HTML_READ_SELECTION:
        default:
            iStartOffset = iSelStart;
            iEndOffset = iSelEnd;
            break;
        }

        bufferedStream.reset();
        if( -1 == iStartOffset ){
            throw new IOException(FAILURE_MSG + "invalid HTML format.");
        }

        int curOffset = 0;
        while (curOffset < iStartOffset){
            curOffset += bufferedStream.skip(iStartOffset - curOffset);
        }

        iReadCount = curOffset;

        if( iStartOffset != iReadCount ){
            throw new IOException(FAILURE_MSG + "Byte stream ends in description.");
        }
        descriptionParsed = true;
    }

    @Override
    public synchronized int read() throws IOException {
        if( closed ){
            throw new IOException("Stream closed");
        }

        if( !descriptionParsed ){
            parseDescription();
        }
        if( -1 != iEndOffset && iReadCount >= iEndOffset ) {
            return -1;
        }

        int retval = bufferedStream.read();
        if( retval == -1 ) {
            return -1;
        }
        ++iReadCount;
        return retval;
    }

    @Override
    public synchronized void close() throws IOException {
        if( !closed ){
            closed = true;
            bufferedStream.close();
        }
    }
}


