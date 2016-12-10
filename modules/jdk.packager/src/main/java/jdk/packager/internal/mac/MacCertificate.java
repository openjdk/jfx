/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.packager.internal.mac;

import com.oracle.tools.packager.IOUtils;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final public class MacCertificate {
    private String certificate;
    private boolean verbose;

    public MacCertificate(String certificate) {
        this.certificate = certificate;
        this.verbose = false;
    }

    public MacCertificate(String certificate, boolean verbose) {
        this.certificate = certificate;
        this.verbose = verbose;
    }

    public boolean isValid() {
        return verifyCertificate(this.certificate, verbose);
    }

    private static File findCertificate(String certificate, boolean verbose) {
        File result = null;

        List<String> args = new ArrayList<>();
        args.add("security");
        args.add("find-certificate");
        args.add("-c");
        args.add(certificate);
        args.add("-a");
        args.add("-p");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PrintStream ps = new PrintStream(baos)) {
            ProcessBuilder security = new ProcessBuilder(args);
            IOUtils.exec(security, verbose, false, ps);

            File output = File.createTempFile("tempfile", ".tmp");
            PrintStream p = new PrintStream(new BufferedOutputStream(new FileOutputStream(output, true)));
            BufferedReader bfReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
            String line = null;

            while((line = bfReader.readLine()) != null){
                p.println(line);
            }

            p.close();
            result = output;
        }
        catch (IOException ioe) {
        }

        return result;
    }

    private static Date findCertificateDate(String filename, boolean verbose) {
        Date result = null;

        List<String> args = new ArrayList<>();
        args.add("/usr/bin/openssl");
        args.add("x509");
        args.add("-noout");
        args.add("-enddate");
        args.add("-in");
        args.add(filename);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PrintStream ps = new PrintStream(baos)) {
            ProcessBuilder security = new ProcessBuilder(args);
            IOUtils.exec(security, verbose, false, ps);
            String output = baos.toString();
            output = output.substring(output.indexOf("=") + 1);
            DateFormat df = new SimpleDateFormat("MMM dd kk:mm:ss yyyy z", Locale.ENGLISH);
            result = df.parse(output);
        }
        catch (IOException ioe) {
        }
        catch (ParseException ex) {
        }

        return result;
    }

    private static boolean verifyCertificate(String certificate, boolean verbose) {
        boolean result = false;

        try {
            File file = null;
            Date certificateDate = null;

            try {
                file = findCertificate(certificate, verbose);

                if (file != null) {
                    certificateDate = findCertificateDate(file.getCanonicalPath(), verbose);
                }
            }
            finally {
                if (file != null) {
                    file.delete();
                }
            }

            if (certificateDate != null) {
                Calendar c = Calendar.getInstance();
                Date today = c.getTime();

                if (certificateDate.after(today)) {
                    result = true;
                }
            }
        }
        catch (IOException ex) {
        }

        return result;
    }
}