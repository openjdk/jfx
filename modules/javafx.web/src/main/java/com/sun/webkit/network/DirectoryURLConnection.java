/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PushbackInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sun.webkit.network.URLs.newURL;

/*
 * Interposing class that will transform the raw stream from a FtpURLConnection
 * or FileURLConnection into an HTML page listing the content of the directory
 */
final class DirectoryURLConnection extends URLConnection {
    // Patterns used to parse the output from a FTP directory list.
    private static final String[] patStrings = {
        // drwxr-xr-x  1 user01        ftp   512 Jan 29 23:32 prog
        // drwxr-xr-x  1 user01        ftp   512 Jan 29 1997 prog
        "([\\-ld](?:[r\\-][w\\-][x\\-]){3})\\s*\\d+ (\\w+)\\s*(\\w+)\\s*(\\d+)\\s*([A-Z][a-z][a-z]\\s*\\d+)\\s*((?:\\d\\d:\\d\\d)|(?:\\d{4}))\\s*(\\p{Print}*)",
        // 04/28/2006  09:12a               3,563 genBuffer.sh
        "(\\d{2}/\\d{2}/\\d{4})\\s*(\\d{2}:\\d{2}[ap])\\s*((?:[0-9,]+)|(?:<DIR>))\\s*(\\p{Graph}*)",
        // 01-29-97    11:32PM <DIR> prog
        "(\\d{2}-\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}[AP]M)\\s*((?:[0-9,]+)|(?:<DIR>))\\s*(\\p{Graph}*)"
    };
    private static final int[][] patternGroups = {
        // file, size, date1, date2, permissions
        {7, 4, 5, 6, 1},
        {4, 3, 1, 2, 0},
        {4, 3, 1, 2, 0}
    };
    private static final Pattern[] patterns;
    private static final Pattern linkp = Pattern.compile("(\\p{Print}+) \\-\\> (\\p{Print}+)$");
    // Style sheet to make the TABLE better looking
    private static final String styleSheet =
            "<style type=\"text/css\" media=\"screen\">" +
            "TABLE { border: 0;}" +
            "TR.header { background: #FFFFFF; color: black; font-weight: bold; text-align: center;}" +
            "TR.odd { background: #E0E0E0;}" +
            "TR.even { background: #C0C0C0;}" +
            "TD.file { text-align: left;}" +
            "TD.fsize { text-align: right; padding-right: 1em;}" +
            "TD.dir { text-align: center; color: green; padding-right: 1em;}" +
            "TD.link { text-align: center; color: red; padding-right: 1em;}" +
            "TD.date { text-align: justify;}" +
            "</style>";

    static {
        patterns = new Pattern[patStrings.length];
        for (int i = 0; i < patStrings.length; i++) {
            patterns[i] = Pattern.compile(patStrings[i]);
        }
    }
    private final URLConnection inner;
    private final boolean sure;
    private String dirUrl = null;
    // Set toHTML to false when it is not a directory and we don't want  to
    // change the stream.
    private boolean toHTML = true;
    private final boolean ftp;
    private InputStream ins = null;

    /*
     * We need an interposing InputStream as well. We subclass PushbackInputStream
     * so that we can do some safe read-ahead if we have to guess whether the
     * URL points to a directory or not (the read-ahead is for FTP only).
     */
    private final class DirectoryInputStream extends PushbackInputStream {
        private final byte[] buffer;
        private boolean endOfStream = false;
        private ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        private PrintStream out = new PrintStream(bytesOut);
        private ByteArrayInputStream bytesIn = null;
        private final StringBuffer tmpString = new StringBuffer();
        private int lineCount = 0;

        private DirectoryInputStream(InputStream ins, boolean guess) {
            super(ins, 512);
            buffer = new byte[512];
            /*
             * If 'guess' is true, it means it's an FTP link and we're not sure
             * this is a directory
             */
            if (guess) {
                StringBuffer line = new StringBuffer();
                int l = 0;
                int c;
                try {
                    l = super.read(buffer, 0, buffer.length);
                } catch (IOException e) {
                }
                if (l <= 0) {
                    toHTML = false;
                } else {
                    for (int i = 0; i < l; i++) {
                        line.append((char) buffer[i]);
                    }
                    String line2 = line.toString();
                    toHTML = false;
                    for (Pattern p : patterns) {
                        Matcher m = p.matcher(line2);
                        if (m.find()) {
                            // One of the patterns matched
                            // Means it's a directory listing
                            toHTML = true;
                            break;
                        }
                    }
                    try {
                        super.unread(buffer, 0, l);
                    } catch (IOException ioe) {
                    // Shouldn't happen
                    }
                }
            }
            if (toHTML) {
                /*
                 * We're good to go. Let's generate the header for the table
                 */
                String parent = null;
                String path;
                URL prevUrl = null;
                if (!dirUrl.endsWith("/")) {
                    dirUrl = dirUrl + "/";
                }
                try {
                    prevUrl = newURL(dirUrl);
                } catch (Exception e) {
                // can't happen
                }
                path = prevUrl.getPath();
                if (path != null && !path.isEmpty()) {
                    int index = path.lastIndexOf("/", path.length() - 2);
                    if (index >= 0) {
                        int removed = path.length() - index - 1;
                        index = dirUrl.indexOf(path);
                        parent = dirUrl.substring(0, index + path.length() - removed) + dirUrl.substring(index + path.length());
                    }
                }
                out.print("<html><head><title>index of ");
                out.print(dirUrl);
                out.print("</title>");
                out.print(styleSheet);
                out.print("</head><body><h1>Index of ");
                out.print(dirUrl);
                out.print("</h1><hr></hr>");
                out.print("<TABLE width=\"95%\" cellpadding=\"5\" cellspacing=\"5\">");
                out.print("<TR class=\"header\"><TD>File</TD><TD>Size</TD><TD>Last Modified</TD></TR>");
                if (parent != null) {
                    lineCount++;
                    out.print("<TR class=\"odd\"><TD colspan=3 class=\"file\"><a href=\"");
                    out.print(parent);
                    out.print("\">Up to parent directory</a></TD></TR>");
                }
                out.close();
                bytesIn = new ByteArrayInputStream(bytesOut.toByteArray());
                out = null;
                bytesOut = null;
            }
        }

        private void parseFile(String s)
        {
            tmpString.append(s);
            int i;
            while ((i = tmpString.indexOf("\n")) >= 0) {
                String sb = tmpString.substring(0, i);
                tmpString.delete(0, i + 1);
                String filename = sb;
                String size = null;
                String date = null;
                boolean dir = false;
                boolean noaccess = false;
                URL furl = null;

                if (filename != null) {
                    lineCount++;
                    try {
                        furl = newURL(dirUrl + URLEncoder.encode(filename, "UTF-8"));
                        URLConnection fconn = furl.openConnection();
                        fconn.connect();
                        date = fconn.getHeaderField("last-modified");
                        size = fconn.getHeaderField("content-length");
                        if (size == null) {
                            dir = true;
                        }
                        fconn.getInputStream().close();
                    } catch (IOException e) {
                        // No access right
                        noaccess = true;
                    }
                    if (bytesOut == null) {
                        bytesOut = new ByteArrayOutputStream();
                        out = new PrintStream(bytesOut);
                    }
                    out.print("<TR class=\"" + ((lineCount % 2) == 0 ? "even" : "odd") + "\"><TD class=\"file\">");
                    if (noaccess) {
                        out.print(filename);
                    } else {
                        out.print("<a href=\"");
                        out.print(furl.toExternalForm());
                        out.print("\">");
                        out.print(filename);
                        out.print("</a>");
                    }
                    if (dir) {
                        out.print("</TD><TD class=\"dir\">&lt;Directory&gt;</TD>");
                    } else {
                        out.print("</TD><TD class=\"fsize\">" + (size == null ? " " : size) + "</TD>");
                    }
                    out.print("<TD class=\"date\">" + (date == null ? " " : date) + "</TD></TR>");
                }
            }
            if (bytesOut != null) {
                out.close();
                bytesIn = new ByteArrayInputStream(bytesOut.toByteArray());
                out = null;
                bytesOut = null;
            }
        }

        private void parseFTP(String s)
        {
            tmpString.append(s);
            int i;
            while ((i = tmpString.indexOf("\n")) >= 0) {
                String sb = tmpString.substring(0, i);
                tmpString.delete(0, i + 1);
                String filename = null;
                String link = null;
                String size = null;
                String date = null;
                boolean dir = false;

                Matcher m = null;
                for (int j = 0; j < patterns.length; j++) {
                    m = patterns[j].matcher(sb);
                    if (m.find()) {
                        filename = m.group(patternGroups[j][0]);
                        size = m.group(patternGroups[j][1]);
                        date = m.group(patternGroups[j][2]);
                        if (patternGroups[j][3] > 0) {
                            date += (" " + m.group(patternGroups[j][3]));
                        }
                        if (patternGroups[j][4] > 0) {
                            String perms = m.group(patternGroups[j][4]);
                            dir = perms.startsWith("d");
                        }
                        if ("<DIR>".equals(size)) {
                            dir = true;
                            size = null;
                        }
                    }
                }
                if (filename != null) {
                    m = linkp.matcher(filename);
                    if (m.find()) {
                        // There is a symbolic link
                        filename = m.group(1);
                        link = m.group(2);
                    }
                    if (bytesOut == null) {
                        bytesOut = new ByteArrayOutputStream();
                        out = new PrintStream(bytesOut);
                    }
                    lineCount++;
                    out.print("<TR class=\"" + ((lineCount % 2) == 0 ? "even" : "odd") + "\"><TD class=\"file\"><a href=\"");
                    try {
                        out.print(dirUrl + URLEncoder.encode(filename, "UTF-8"));
                    } catch (java.io.UnsupportedEncodingException e) {
                    // can't happen
                    }
                    if (dir) {
                        out.print("/");
                    }
                    out.print("\">");
                    out.print(filename);
                    out.print("</a>");
                    if (link != null) {
                        out.print(" &rarr; " + link + "</TD><TD class=\"link\">&lt;Link&gt;</TD>");
                    } else if (dir) {
                        out.print("</TD><TD class=\"dir\">&lt;Directory&gt;</TD>");
                    } else {
                        out.print("</TD><TD class=\"fsize\">" + size + "</TD>");
                    }
                    out.print("<TD class=\"date\">" + date + "</TD></TR>");
                }
            }
            if (bytesOut != null) {
                out.close();
                bytesIn = new ByteArrayInputStream(bytesOut.toByteArray());
                out = null;
                bytesOut = null;
            }
        }

        private void endOfList()
        {
            // Let's make sure we don't miss the last line because a new-line
            // is missing
            if (ftp) {
                parseFTP("\n");
            } else {
                parseFile("\n");
            }
            if (bytesOut == null) {
                bytesOut = new ByteArrayOutputStream();
                out = new PrintStream(bytesOut);
            }
            out.print("</TABLE><br><hr></hr></body></html>");
            out.close();
            bytesIn = new ByteArrayInputStream(bytesOut.toByteArray());
            out = null;
            bytesOut = null;
        }

        @Override
        public int read(byte[] buf) throws IOException
        {
            return read(buf, 0, buf.length);
        }

        @Override
        public int read(byte[] buf, int offset, int length) throws IOException
        {
            int l = 0;

            if (!toHTML) {
                return super.read(buf, offset, length);
            }
            if (bytesIn != null) {
                l = bytesIn.read(buf, offset, length);
                if (l == -1) {
                    bytesIn.close();
                    bytesIn = null;
                    if (endOfStream) {
                        return -1;
                    }
                } else {
                    return l;
                }
            }
            if (!endOfStream) {
                l = super.read(buffer, 0, buffer.length);
                if (l == -1) {
                    endOfStream = true;
                    endOfList();
                    return read(buf, offset, length);
                } else {
                    if (ftp) {
                        parseFTP(new String(buffer, 0, l));
                    } else {
                        parseFile(new String(buffer, 0, l));
                    }
                    if (bytesIn != null) {
                        return read(buf, offset, length);
                    }
                }
            }
            return 0;
        }
    }

    /*
     * Constructor to use for an FTP (or FTPS) URLConnection.
     * Set 'notsure' to true if it is necessary to look ahead to make sure
     * it is, indeed, a directory.
     */
    DirectoryURLConnection(URLConnection con, boolean notsure) {
        super(con.getURL());
        dirUrl = con.getURL().toExternalForm();
        inner = con;
        sure = !notsure;
        ftp = true;
    }

    /*
     * Constructor to use for a File URLConnection.
     */
    DirectoryURLConnection(URLConnection con) {
        super(con.getURL());
        dirUrl = con.getURL().toExternalForm();
        ftp = false;
        sure = true;
        inner = con;
    }

    @Override
    public void connect() throws IOException
    {
        inner.connect();
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        if (ins == null) {
            if (ftp) {
                ins = new DirectoryInputStream(inner.getInputStream(), !sure);
            } else {
                ins = new DirectoryInputStream(inner.getInputStream(), false);
            }
        }
        return ins;
    }

    @Override
    public String getContentType()
    {
        try {
            if (!sure) {
                getInputStream();
            }
        } catch (IOException e) {
        }

        if (toHTML) {
            return "text/html";
        }

        return inner.getContentType();
    }

    @Override
    public String getContentEncoding()
    {
        return inner.getContentEncoding();
    }

    @Override
    public int getContentLength()
    {
        return inner.getContentLength();
    }

    @Override
    public Map<String, List<String>> getHeaderFields()
    {
        return inner.getHeaderFields();
    }

    @Override
    public String getHeaderField(String key)
    {
        return inner.getHeaderField(key);
    }
}
