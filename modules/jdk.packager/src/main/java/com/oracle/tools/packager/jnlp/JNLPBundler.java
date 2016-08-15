/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.tools.packager.jnlp;

import com.oracle.tools.packager.AbstractBundler;
import com.oracle.tools.packager.BundlerParamInfo;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.StandardBundlerParam;
import com.oracle.tools.packager.UnsupportedPlatformException;
import com.sun.javafx.tools.packager.PackagerException;
import com.sun.javafx.tools.packager.PackagerLib;
import com.sun.javafx.tools.packager.TemplatePlaceholders;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.cert.CertificateEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.oracle.tools.packager.StandardBundlerParam.*;


public class JNLPBundler extends AbstractBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle(JNLPBundler.class.getName());

    private static final String dtFX = "dtjava.js";

    private static final String webfilesDir = "web-files";
    //Note: leading "." is important for IE8
    private static final String EMBEDDED_DT = "./"+webfilesDir+"/"+dtFX;

    private static final String PUBLIC_DT = "https://java.com/js/dtjava.js";

    private static final String JFX_NS_URI = "http://javafx.com";

    public static final StandardBundlerParam<String> OUT_FILE = new StandardBundlerParam<>(
            I18N.getString("param.out-file.name"),
            I18N.getString("param.out-file.description"),
            "jnlp.outfile",
            String.class,
            null,
            null);

    public static final StandardBundlerParam<Boolean> SWING_APP = new StandardBundlerParam<>(
            I18N.getString("param.swing-app.name"),
            I18N.getString("param.swing-app.description"),
            "jnlp.swingApp",
            Boolean.class,
            p -> Boolean.FALSE,
            (s, p) -> Boolean.parseBoolean(s));

    public static final StandardBundlerParam<Boolean> INCLUDE_DT = new StandardBundlerParam<>(
            I18N.getString("param.include-deployment-toolkit.name"),
            I18N.getString("param.include-deployment-toolkit.description"),
            "jnlp.includeDT",
            Boolean.class,
            p -> Boolean.FALSE,
            (s, p) -> Boolean.parseBoolean(s));

    public static final StandardBundlerParam<Boolean> EMBED_JNLP = new StandardBundlerParam<>(
            I18N.getString("param.embed-jnlp.name"),
            I18N.getString("param.embed-jnlp.description"),
            "jnlp.embedJnlp",
            Boolean.class,
            p -> Boolean.FALSE,
            (s, p) -> Boolean.parseBoolean(s));

    public static final StandardBundlerParam<Boolean> EXTENSION = new StandardBundlerParam<>(
            I18N.getString("param.extension.name"),
            I18N.getString("param.extension.description"),
            "jnlp.extension",
            Boolean.class,
            p -> Boolean.FALSE,
            (s, p) -> Boolean.parseBoolean(s));

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<Map<File, File>> TEMPLATES = new StandardBundlerParam<>(
            I18N.getString("param.templates.name"),
            I18N.getString("param.templates.description"),
            "jnlp.templates",
            (Class<Map<File, File>>) (Object) Map.class,
            p -> new LinkedHashMap<>(),
            null);

    public static final StandardBundlerParam<String> CODEBASE = new StandardBundlerParam<>(
            I18N.getString("param.codebase.name"),
            I18N.getString("param.codebase.description"),
            "jnlp.codebase",
            String.class,
            p -> null,
            null);

    public static final StandardBundlerParam<String> PLACEHOLDER = new StandardBundlerParam<>(
            I18N.getString("param.placeholder.name"),
            I18N.getString("param.placeholder.description"),
            "jnlp.placeholder",
            String.class,
            p -> "javafx-app-placeholder",
            (s, p) -> {
                if (!s.startsWith("'")) {
                    s = "'" + s;
                }
                if (!s.endsWith("'")) {
                    s = s + "'";
                }
                return s;
            });

    public static final StandardBundlerParam<Boolean> OFFLINE_ALLOWED = new StandardBundlerParam<>(
            I18N.getString("param.offline-allowed.name"),
            I18N.getString("param.offline-allowed.description"),
            "jnlp.offlineAllowed",
            Boolean.class,
            p -> true,
            (s, p) -> Boolean.valueOf(s));

    public static final StandardBundlerParam<Boolean> ALL_PERMISSIONS = new StandardBundlerParam<>(
            I18N.getString("param.all-permissions.name"),
            I18N.getString("param.all-permissions.description"),
            "jnlp.allPermisions",
            Boolean.class,
            p -> false,
            (s, p) -> Boolean.valueOf(s));

    public static final StandardBundlerParam<Integer> WIDTH = new StandardBundlerParam<>(
            I18N.getString("param.width.name"),
            I18N.getString("param.width.description"),
            "jnlp.width",
            Integer.class,
            p -> 0,
            (s, p) -> Integer.parseInt(s));

    public static final StandardBundlerParam<Integer> HEIGHT = new StandardBundlerParam<>(
            I18N.getString("param.height.name"),
            I18N.getString("param.height.description"),
            "jnlp.height",
            Integer.class,
            p -> 0,
            (s, p) -> Integer.parseInt(s));

    public static final StandardBundlerParam<String> EMBEDDED_WIDTH = new StandardBundlerParam<>(
            I18N.getString("param.embedded-width.name"),
            I18N.getString("param.embedded-width.description"),
            "jnlp.embeddedWidth",
            String.class,
            p -> Integer.toString(WIDTH.fetchFrom(p)),
            (s, p) -> s);

    public static final StandardBundlerParam<String> EMBEDDED_HEIGHT = new StandardBundlerParam<>(
            I18N.getString("param.embedded-height.name"),
            I18N.getString("param.embedded-height.description"),
            "jnlp.embeddedHeight",
            String.class,
            p -> Integer.toString(HEIGHT.fetchFrom(p)),
            (s, p) -> s);

    public static final StandardBundlerParam<String> FALLBACK_APP = new StandardBundlerParam<>(
            I18N.getString("param.fallback-app.name"),
            I18N.getString("param.fallback-app.description"),
            "jnlp.fallbackApp",
            String.class,
            p -> null,
            (s, p) -> s);

    public static final StandardBundlerParam<String> UPDATE_MODE = new StandardBundlerParam<>(
            I18N.getString("param.update-mode.name"),
            I18N.getString("param.update-mode.description"),
            "jnlp.updateMode",
            String.class,
            p -> "background",
            (s, p) -> s);

    public static final StandardBundlerParam<String> FX_PLATFORM = new StandardBundlerParam<>(
            I18N.getString("param.fx-platform.name"),
            I18N.getString("param.fx-platform.description"),
            "jnlp.fxPlatform",
            String.class,
            p -> "1.8+",
            (s, p) -> s);

    public static final StandardBundlerParam<String> JRE_PLATFORM = new StandardBundlerParam<>(
            I18N.getString("param.jre-platform.name"),
            I18N.getString("param.jre-platform.description"),
            "jnlp.jrePlatform",
            String.class,
            p -> "1.8+",
            (s, p) -> s);

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<List<Map<String, ? super Object>>> ICONS = new StandardBundlerParam<>(
            I18N.getString("param.icons.name"),
            I18N.getString("param.icons.description"),
            "jnlp.icons",
            (Class<List<Map<String, ? super Object>>>) (Object) List.class,
            params -> new ArrayList<>(1),
            null
    );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<Map<String, String>> APP_PARAMS = new StandardBundlerParam<>(
            I18N.getString("param.params.name"),
            I18N.getString("param.params.description"),
            "jnlp.params",
            (Class<Map<String, String>>) (Object) Map.class,
            params -> new HashMap<>(),
            null
    );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<Map<String, String>> ESCAPED_APPLET_PARAMS = new StandardBundlerParam<>(
            I18N.getString("param.escaped-applet-params.name"),
            I18N.getString("param.escaped-applet-params.description"),
            "jnlp.escapedAppletParams",
            (Class<Map<String, String>>) (Object) Map.class,
            params -> new HashMap<>(),
            null
    );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<Map<String, String>> APPLET_PARAMS = new StandardBundlerParam<>(
            I18N.getString("param.applet-params.name"),
            I18N.getString("param.applet-params.description"),
            "jnlp.appletParams",
            (Class<Map<String, String>>) (Object) Map.class,
            params -> new HashMap<>(),
            null
    );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<Map<String, String>> JS_CALLBACKS = new StandardBundlerParam<>(
            I18N.getString("param.js-callbacks.name"),
            I18N.getString("param.js-callbacks.description"),
            "jnlp.jsCallbacks",
            (Class<Map<String, String>>) (Object) Map.class,
            params -> new HashMap<>(),
            null
    );

    public static final StandardBundlerParam<Boolean> INSTALL_HINT =
            new StandardBundlerParam<>(
                    I18N.getString("param.menu-install-hint.name"),
                    I18N.getString("param.menu-install-hint.description"),
                    "jnlp.install",
                    Boolean.class,
                    params -> null,
                    // valueOf(null) is false, and we actually do want null in some cases
                    (s, p) -> (s == null || "null".equalsIgnoreCase(s))? null : Boolean.valueOf(s)
            );


    public static final StandardBundlerParam<String> ICONS_HREF =
            new StandardBundlerParam<>(
                    I18N.getString("param.icons-href.name"),
                    I18N.getString("param.icons-href.description"),
                    "jnlp.icons.href",
                    String.class,
                    null,
                    null
            );


    public static final StandardBundlerParam<String> ICONS_KIND =
            new StandardBundlerParam<>(
                    I18N.getString("param.icons-kind.name"),
                    I18N.getString("param.icons-kind.description"),
                    "jnlp.icons.kind",
                    String.class,
                    params -> null,
                    null
            );


    public static final StandardBundlerParam<String> ICONS_WIDTH =
            new StandardBundlerParam<>(
                    I18N.getString("param.icons-width.name"),
                    I18N.getString("param.icons-width.description"),
                    "jnlp.icons.width",
                    String.class,
                    params -> null,
                    null
            );


    public static final StandardBundlerParam<String> ICONS_HEIGHT =
            new StandardBundlerParam<>(
                    I18N.getString("param.icons-height.name"),
                    I18N.getString("param.icons-height.description"),
                    "jnlp.icons.height",
                    String.class,
                    params -> null,
                    null
            );


    public static final StandardBundlerParam<String> ICONS_DEPTH =
            new StandardBundlerParam<>(
                    I18N.getString("param.icons-depth.name"),
                    I18N.getString("param.icons-depth.description"),
                    "jnlp.icons.depth",
                    String.class,
                    params -> null,
                    null
            );


    private enum Mode {FX, APPLET, SwingAPP}

    @Override
    public String getName() {
        return I18N.getString("bundler.name");
    }

    @Override
    public String getDescription() {
        return I18N.getString("bundler.description");
    }

    @Override
    public String getID() {
        return "jnlp";
    }

    @Override
    public String getBundleType() {
        return "JNLP";
    }


    @Override
    public Collection<BundlerParamInfo<?>> getBundleParameters() {
        return Arrays.asList(
                ALL_PERMISSIONS,
                APPLET_PARAMS,
                APP_NAME,
                APP_PARAMS,
                APP_RESOURCES_LIST,
                ARGUMENTS,
                CODEBASE,
                DESCRIPTION,
                EMBED_JNLP,
                EMBEDDED_HEIGHT,
                EMBEDDED_WIDTH,
                ESCAPED_APPLET_PARAMS,
                EXTENSION,
//                FALLBACK_APP,
//                FX_PLATFORM,
                HEIGHT,
                ICONS,
                IDENTIFIER,
                INCLUDE_DT,
                INSTALL_HINT,
                JRE_PLATFORM,
                JS_CALLBACKS,
                JVM_OPTIONS,
                JVM_PROPERTIES,
                MAIN_CLASS,
                MENU_HINT,
                OFFLINE_ALLOWED,
                OUT_FILE,
                PRELOADER_CLASS,
                PLACEHOLDER,
                SHORTCUT_HINT,
                SWING_APP,
                TEMPLATES,
                TITLE,
                UPDATE_MODE,
                VENDOR,
                WIDTH
        );
    }

    @Override
    public boolean validate(Map<String, ? super Object> params) throws UnsupportedPlatformException, ConfigException {
        if (OUT_FILE.fetchFrom(params) == null) {
            throw new ConfigException(
                    I18N.getString("error.no-outfile"),
                    I18N.getString("error.no-outfile.advice"));
        }
        if (APP_RESOURCES_LIST.fetchFrom(params) == null) {
            throw new ConfigException(
                    I18N.getString("error.no-app-resources"),
                    I18N.getString("error.no-app-resources.advice"));
        }
        if (!EXTENSION.fetchFrom(params)) {
            StandardBundlerParam.validateMainClassInfoFromAppResources(params);
        }
        return true;
    }

    private String readTextFile(File in) throws PackagerException {
        StringBuilder sb = new StringBuilder();
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(in))) {
            char[] buf = new char[16384];
            int len;
            while ((len = isr.read(buf)) > 0) {
                sb.append(buf, sb.length(), len);
            }
        } catch (IOException ex) {
            throw new PackagerException(ex, "ERR_FileReadFailed",
                    in.getAbsolutePath());
        }
        return sb.toString();
    }

    private String processTemplate(Map<String, ? super Object> params, String inpText,
                                   Map<TemplatePlaceholders, String> templateStrings) {
        //Core pattern matches
        //   #DT.SCRIPT#
        //   #DT.EMBED.CODE.ONLOAD#
        //   #DT.EMBED.CODE.ONLOAD(App2)#
        String corePattern = "(#[\\w\\.\\(\\)]+#)";
        //This will match
        //   "/*", "//" or "<!--" with arbitrary number of spaces
        String prefixGeneric = "[/\\*-<!]*[ \\t]*";
        //This will match
        //   "/*", "//" or "<!--" with arbitrary number of spaces
        String suffixGeneric = "[ \\t]*[\\*/>-]*";

        //NB: result core match is group number 1
        Pattern mainPattern = Pattern.compile(
                prefixGeneric + corePattern + suffixGeneric);

        Matcher m = mainPattern.matcher(inpText);
        StringBuffer result = new StringBuffer();
        while (m.find()) {
            String match = m.group();
            String coreMatch = m.group(1);
            //have match, not validate it is not false positive ...
            // e.g. if we matched just some spaces in prefix/suffix ...
            boolean inComment =
                    (match.startsWith("<!--") && match.endsWith("-->")) ||
                            (match.startsWith("//")) ||
                            (match.startsWith("/*") && match.endsWith(" */"));

            //try to find if we have match
            String coreReplacement = null;
            //map with rules have no template ids
            //int p = coreMatch.indexOf("\\(");
            //strip leading/trailing #, then split of id part
            String parts[] = coreMatch.substring(1, coreMatch.length()-1).split("[\\(\\)]");
            String rulePart = parts[0];
            String idPart = (parts.length == 1) ?
                    //strip trailing ')'
                    null : parts[1];
            if (templateStrings.containsKey(
                    TemplatePlaceholders.fromString(rulePart))
                    && (idPart == null /* it is ok for templateId to be not null, e.g. DT.SCRIPT.CODE */
                    || idPart.equals(IDENTIFIER.fetchFrom(params)))) {
                coreReplacement = templateStrings.get(
                        TemplatePlaceholders.fromString(rulePart));
            }

            if (coreReplacement != null) {
                if (inComment || coreMatch.length() == match.length()) {
                    m.appendReplacement(result, coreReplacement);
                } else { // pattern matched something that is not comment
                    // Very unlikely but lets play it safe
                    int pp = match.indexOf(coreMatch);
                    String v = match.substring(0, pp) +
                            coreReplacement +
                            match.substring(pp + coreMatch.length());
                    m.appendReplacement(result, v);
                }
            }
        }
        m.appendTail(result);
        return result.toString();
    }

    @Override
    public File execute(Map<String, ? super Object> params, File outputParentDir) {

        Map<File, File> templates = TEMPLATES.fetchFrom(params);
        boolean templateOn = !templates.isEmpty();
        Map<TemplatePlaceholders, String> templateStrings = null;
        if (templateOn) {
            templateStrings =
                    new EnumMap<>(TemplatePlaceholders.class);
        }
        try {
            //In case of FX app we will have one JNLP and one HTML
            //In case of Swing with FX we will have 2 JNLP files and one HTML
            String outfile = OUT_FILE.fetchFrom(params);
            boolean isSwingApp = SWING_APP.fetchFrom(params);

            String jnlp_filename_webstart = outfile + ".jnlp";
            String jnlp_filename_browser
                    = isSwingApp ?
                    (outfile + "_browser.jnlp") : jnlp_filename_webstart;
            String html_filename = outfile + ".html";

            //create out dir
            outputParentDir.mkdirs();

            boolean includeDT = INCLUDE_DT.fetchFrom(params);

            if (includeDT && !extractWebFiles(outputParentDir)) {
                throw new PackagerException("ERR_NoEmbeddedDT");
            }

            ByteArrayOutputStream jnlp_bos_webstart = new ByteArrayOutputStream();
            ByteArrayOutputStream jnlp_bos_browser = new ByteArrayOutputStream();

            //for swing case we need to generate 2 JNLP files
            if (isSwingApp) {
                PrintStream jnlp_ps = new PrintStream(jnlp_bos_webstart);
                generateJNLP(params, jnlp_ps, jnlp_filename_webstart, Mode.SwingAPP);
                jnlp_ps.close();
                //save JNLP
                save(outputParentDir, jnlp_filename_webstart, jnlp_bos_webstart.toByteArray());

                jnlp_ps = new PrintStream(jnlp_bos_browser);
                generateJNLP(params, jnlp_ps, jnlp_filename_browser, Mode.APPLET);
                jnlp_ps.close();
                //save JNLP
                save(outputParentDir, jnlp_filename_browser, jnlp_bos_browser.toByteArray());

            } else {
                PrintStream jnlp_ps = new PrintStream(jnlp_bos_browser);
                generateJNLP(params, jnlp_ps, jnlp_filename_browser, Mode.FX);
                jnlp_ps.close();

                //save JNLP
                save(outputParentDir, jnlp_filename_browser, jnlp_bos_browser.toByteArray());

                jnlp_bos_webstart = jnlp_bos_browser;
            }

            //we do not need html if this is component and not main app
            boolean isExtension = EXTENSION.fetchFrom(params);
            if (!isExtension) {
                // even though the html is unused if templateOn,
                // the templateStrings is updated as a side effect.
                ByteArrayOutputStream html_bos =
                        new ByteArrayOutputStream();
                PrintStream html_ps = new PrintStream(html_bos);
                generateHTML(params, html_ps,
                        jnlp_bos_browser.toByteArray(), jnlp_filename_browser,
                        jnlp_bos_webstart.toByteArray(), jnlp_filename_webstart,
                        templateStrings, isSwingApp);
                html_ps.close();

                //process template file
                if (templateOn) {
                    for (Map.Entry<File, File> t: TEMPLATES.fetchFrom(params).entrySet()) {
                        File out = t.getValue();
                        if (out == null) {
                            System.out.println(
                                    "Perform inplace substitution for " +
                                            t.getKey().getAbsolutePath());
                            out = t.getKey();
                        }
                        save(out, processTemplate(params,
                                readTextFile(t.getKey()), templateStrings).getBytes());
                    }
                } else {
                    //save HTML
                    save(outputParentDir, html_filename, html_bos.toByteArray());
                }
            }

            //copy jar files
            for (RelativeFileSet rfs : APP_RESOURCES_LIST.fetchFrom(params)) {
                copyFiles(rfs, outputParentDir);
            }

            return outputParentDir;
        } catch (Exception ex) {
            Log.info("JNLP failed : " + ex.getMessage());
            ex.printStackTrace();
            Log.debug(ex);
            return null;
        }
    }

    private static void copyFiles(RelativeFileSet resources, File outdir) throws IOException, PackagerException {
        File rootDir = resources.getBaseDirectory();

        for (String s : resources.getIncludedFiles()) {
            final File srcFile = new File(rootDir, s);
            if (srcFile.exists() && srcFile.isFile()) {
                //skip file copying if jar is in the same location
                final File destFile = new File(outdir, s);

                if (!srcFile.getCanonicalFile().equals(destFile.getCanonicalFile())) {
                    copyFileToOutDir(new FileInputStream(srcFile), destFile);
                } else {
                    Log.verbose(MessageFormat.format(I18N.getString("error.jar-no-self-copy"), s));
                }
            }
        }
    }


    //return null if args are default
    private String getJvmArguments(Map<String, ? super Object> params, boolean includeProperties) {
        List<String> jvmargs = JVM_OPTIONS.fetchFrom(params);
        Map<String, String> properties = JVM_PROPERTIES.fetchFrom(params);

        StringBuilder sb = new StringBuilder();
        for (String v : jvmargs) {
            sb.append(v);  //may need to escape if parameter has spaces
            sb.append(" ");
        }
        if (includeProperties) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                sb.append("-D");
                sb.append(entry.getKey());
                sb.append("=");
                sb.append(entry.getValue()); //may need to escape if value has spaces
                sb.append(" ");
            }
        }
        if (sb.length() > 0) {
            return sb.toString();
        }
        return null;
    }

    private void generateJNLP(Map<String, ? super Object> params, PrintStream out, String jnlp_filename, Mode m)
            throws IOException, CertificateEncodingException
    {
        String codebase = CODEBASE.fetchFrom(params);
        String title = TITLE.fetchFrom(params);
        String vendor = VENDOR.fetchFrom(params);
        String description = DESCRIPTION.fetchFrom(params);

        try {
            XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            XMLStreamWriter xout = xmlOutputFactory.createXMLStreamWriter(baos, "utf-8");

            xout.writeStartDocument("utf-8", "1.0");
            xout.writeCharacters("\n");
            xout.writeStartElement("jnlp");
            xout.writeAttribute("spec", "1.0");
            xout.writeNamespace("jfx", "http://javafx.com");
            if (codebase != null) {
                xout.writeAttribute("codebase", codebase);
            }
            xout.writeAttribute("href", jnlp_filename);

            xout.writeStartElement("information");

            xout.writeStartElement("title");
            if (title != null) {
                xout.writeCharacters(title);
            } else {
                xout.writeCData("Sample JavaFX Application");
            }
            xout.writeEndElement();

            xout.writeStartElement("vendor");
            if (vendor != null) {
                xout.writeCharacters(vendor);
            } else {
                xout.writeCharacters("Unknown vendor");
            }
            xout.writeEndElement();

            xout.writeStartElement("description");
            if (description != null) {
                xout.writeCharacters(description);
            } else {
                xout.writeCharacters("Sample JavaFX 2.0 application.");
            }
            xout.writeEndElement();
            for (Map<String, ? super Object> iconInfo : ICONS.fetchFrom(params)) {
                String href =   ICONS_HREF.fetchFrom(iconInfo);
                String kind =   ICONS_KIND.fetchFrom(iconInfo);
                String width =  ICONS_WIDTH.fetchFrom(iconInfo);
                String height = ICONS_HEIGHT.fetchFrom(iconInfo);
                String depth =  ICONS_DEPTH.fetchFrom(iconInfo);

                xout.writeStartElement("icon");

                xout.writeAttribute("href", href);
                if (kind != null)   xout.writeAttribute("kind",   kind);
                if (width != null)  xout.writeAttribute("width",  width);
                if (height != null) xout.writeAttribute("height", height);
                if (depth != null)  xout.writeAttribute("depth",  depth);

                xout.writeEndElement();
            }

            boolean offlineAllowed = OFFLINE_ALLOWED.fetchFrom(params);
            boolean isExtension = EXTENSION.fetchFrom(params);
            if (offlineAllowed && !isExtension) {
                xout.writeEmptyElement("offline-allowed");
            }

            Boolean needShortcut = SHORTCUT_HINT.fetchFrom(params);
            Boolean needMenu = MENU_HINT.fetchFrom(params);
            Boolean needInstall = INSTALL_HINT.fetchFrom(params);

            if ((needShortcut != null && Boolean.TRUE.equals(needShortcut)) ||
                (needMenu != null && Boolean.TRUE.equals(needMenu)) ||
                (needInstall != null && Boolean.TRUE.equals(needInstall))) {

                xout.writeStartElement("shortcut");
                if (Boolean.TRUE.equals(needInstall)) {
                    xout.writeAttribute("installed", needInstall.toString());
                }
                if (Boolean.TRUE.equals(needShortcut)) {
                    xout.writeEmptyElement("desktop");
                }
                if (Boolean.TRUE.equals(needMenu)) {
                    xout.writeEmptyElement("menu");
                    //todo add submenu element
                }
                xout.writeEndElement();
            }

            //TODO file associations

            xout.writeEndElement(); // information

            boolean allPermissions = ALL_PERMISSIONS.fetchFrom(params);
            if (allPermissions) {
                xout.writeStartElement("security");
                xout.writeEmptyElement("all-permissions");
                xout.writeEndElement();
            }

            String updateMode = UPDATE_MODE.fetchFrom(params);
            if (updateMode != null) {
                xout.writeStartElement("update");
                xout.writeAttribute("check", UPDATE_MODE.fetchFrom(params));
                xout.writeEndElement(); // update
            }


            boolean needToCloseResourceTag = false;
            //jre is available for all platforms
            if (!isExtension) {
                xout.writeStartElement("resources");
                needToCloseResourceTag = true;

                xout.writeStartElement("j2se");
                xout.writeAttribute("version", JRE_PLATFORM.fetchFrom(params));
                String vmargs = getJvmArguments(params, false);
                if (vmargs != null) {
                    xout.writeAttribute("java-vm-args", vmargs);
                }
                xout.writeAttribute("href", "http://java.sun.com/products/autodl/j2se");
                for (Map.Entry<String, String> entry : JVM_PROPERTIES.fetchFrom(params).entrySet()) {
                    xout.writeStartElement("property");
                    xout.writeAttribute("name", entry.getKey());
                    xout.writeAttribute("value", entry.getValue());
                    xout.writeEndElement(); //property
                }
                xout.writeEndElement(); //j2se
            }
            String currentOS = null, currentArch = null;
//            //NOTE: This should sort the list by os+arch; it will reduce the number of resource tags
//            String pendingPrint = null;
            //for (DeployResource resource: deployParams.resources) {
            for (RelativeFileSet rfs : APP_RESOURCES_LIST.fetchFrom(params)) {
                //if not same OS or arch then open new resources element
                if (!needToCloseResourceTag ||
                        ((currentOS == null && rfs.getOs() != null) ||
                                currentOS != null && !currentOS.equals(rfs.getOs())) ||
                        ((currentArch == null && rfs.getArch() != null) ||
                                currentArch != null && !currentArch.equals(rfs.getArch())))
                {

                    //we do not print right a way as it may be empty block
                    // Not all resources make sense for JNLP (e.g. data or license)
                    if (needToCloseResourceTag) {
                        xout.writeEndElement();
                    }
                    needToCloseResourceTag = true;

                    currentOS = rfs.getOs();
                    currentArch = rfs.getArch();
                    xout.writeStartElement("resources");
                    if (currentOS != null) xout.writeAttribute("os", currentOS);
                    if (currentArch != null) xout.writeAttribute("arch", currentArch);
                }
                for (String relativePath : rfs.getIncludedFiles()) {

                    final File srcFile = new File(rfs.getBaseDirectory(), relativePath);
                    if (srcFile.exists() && srcFile.isFile()) {
                        RelativeFileSet.Type type = rfs.getType();
                        if (type == RelativeFileSet.Type.UNKNOWN) {
                            if (relativePath.endsWith(".jar")) {
                                type = RelativeFileSet.Type.jar;
                            } else if (relativePath.endsWith(".jnlp")) {
                                type = RelativeFileSet.Type.jnlp;
                            } else if (relativePath.endsWith(".dll")) {
                                type = RelativeFileSet.Type.nativelib;
                            } else if (relativePath.endsWith(".so")) {
                                type = RelativeFileSet.Type.nativelib;
                            } else if (relativePath.endsWith(".dylib")) {
                                type = RelativeFileSet.Type.nativelib;
                            }
                        }
                        switch (type) {
                            case jar:
                                xout.writeStartElement("jar");
                                xout.writeAttribute("href", relativePath);
                                xout.writeAttribute("size", Long.toString(srcFile.length()));
                                if (rfs.getMode() != null) {
                                    xout.writeAttribute("download", rfs.getMode());
                                }
                                xout.writeEndElement(); //jar
                                break;
                            case jnlp:
                                xout.writeStartElement("extension");
                                xout.writeAttribute("href", relativePath);
                                xout.writeEndElement(); //extension
                                break;
                            case nativelib:
                                xout.writeStartElement("nativelib");
                                xout.writeAttribute("href", relativePath);
                                xout.writeEndElement(); //nativelib
                                break;
                        }
                    }
                }
            }
            if (needToCloseResourceTag) {
                xout.writeEndElement();
            }

            if (!isExtension) {
                Integer width = WIDTH.fetchFrom(params);
                Integer height = HEIGHT.fetchFrom(params);
                if (width == null) {
                    width = 0;
                }
                if (height == null) {
                    height = 0;
                }

                String applicationClass = MAIN_CLASS.fetchFrom(params);
                String preloader = PRELOADER_CLASS.fetchFrom(params);
                Map<String, String> appParams = APP_PARAMS.fetchFrom(params);
                List<String> arguments = ARGUMENTS.fetchFrom(params);

                String appName = APP_NAME.fetchFrom(params);
                if (m == Mode.APPLET) {
                    xout.writeStartElement("applet-desc");
                    xout.writeAttribute("width", Integer.toString(width));
                    xout.writeAttribute("height", Integer.toString(height));
                    xout.writeAttribute("main-class", applicationClass);
                    xout.writeAttribute("name", appName);

                    xout.writeStartElement("param");
                    xout.writeAttribute("name", "requiredFXVersion");
                    xout.writeAttribute("value", FX_PLATFORM.fetchFrom(params));
                    xout.writeEndElement(); // param

                    for (Map.Entry<String, String> appParamEntry : appParams.entrySet()) {
                        xout.writeStartElement("param");
                        xout.writeAttribute("name", appParamEntry.getKey());
                        if (appParamEntry.getValue() != null) {
                            xout.writeAttribute("value", appParamEntry.getValue());
                        }
                        xout.writeEndElement(); // param
                    }
                    xout.writeEndElement(); // applet-desc
                } else if (m == Mode.SwingAPP) {
                    xout.writeStartElement("application-desc");
                    xout.writeAttribute("main-class", applicationClass);
                    xout.writeAttribute("name", appName);

                    for (String a : arguments) {
                        xout.writeStartElement("argument");
                        xout.writeCharacters(a);
                        xout.writeEndElement(); // argument
                    }
                    xout.writeEndElement();
                } else { //JavaFX application
                    //embed fallback application
                    String fallbackApp = FALLBACK_APP.fetchFrom(params);
                    if (fallbackApp != null) {
                        xout.writeStartElement("applet-desc");
                        xout.writeAttribute("width", Integer.toString(width));
                        xout.writeAttribute("height", Integer.toString(height));
                        xout.writeAttribute("main-class", fallbackApp);
                        xout.writeAttribute("name", appName);

                        xout.writeStartElement("param");
                        xout.writeAttribute("name", "requiredFXVersion");
                        xout.writeAttribute("value", FX_PLATFORM.fetchFrom(params));
                        xout.writeEndElement(); // param

                        xout.writeEndElement(); // applet-desc
                    }

                    xout.writeStartElement("jfx", "javafx-desc", JFX_NS_URI);
                    xout.writeAttribute("width", Integer.toString(width));
                    xout.writeAttribute("height", Integer.toString(height));
                    xout.writeAttribute("main-class", applicationClass);
                    xout.writeAttribute("name", appName);
                    if (preloader != null) {
                        xout.writeAttribute("preloader-class", preloader);
                    }

                    if (appParams != null) {
                        for (Map.Entry<String, String> appParamEntry : appParams.entrySet()) {
                            xout.writeStartElement("param");
                            xout.writeAttribute("name", appParamEntry.getKey());
                            if (appParamEntry.getValue() != null) {
                                xout.writeAttribute("value", appParamEntry.getValue());
                            }
                            xout.writeEndElement(); // param
                        }
                    }
                    if (arguments != null) {
                        for (String a : arguments) {
                            xout.writeStartElement("argument");
                            xout.writeCharacters(a);
                            xout.writeEndElement(); // argument
                        }
                    }

                    xout.writeEndElement(); //javafx-desc
                }
            }

            xout.writeEndElement(); // jnlp

            // now pretty print
            String s = baos.toString();
            out.println(xmlPrettyPrint(s));
        } catch (XMLStreamException | TransformerException e) {
            e.printStackTrace();
        }
    }

    private String xmlPrettyPrint(String s) throws TransformerException {
//        System.out.println(s);
        TransformerFactory factory = TransformerFactory.newInstance();

        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        StringWriter formattedStringWriter = new StringWriter();
        transformer.transform(new StreamSource(new StringReader(s)), new StreamResult(formattedStringWriter));
        return formattedStringWriter.toString();
    }


    private void addToList(List<String> l, String name, String value, boolean isString) {
        if (isString) {
            l.add(name + " : '" + value.replaceAll("(['\"\\\\])", "\\\\$1") + "'");
        } else {
            l.add(name + " : " + value);

        }
    }

    private String listToString(List<String> lst, String offset) {
        StringBuilder b = new StringBuilder();
        if (lst == null || lst.isEmpty()) {
            return offset + "{}";
        }

        b.append(offset).append("{\n");
        boolean first = true;
        for (String s : lst) {
            if (!first) {
                b.append(",\n");
            }
            first = false;
            b.append(offset).append("    ");
            b.append(s);
        }
        b.append("\n");
        b.append(offset).append("}");
        return b.toString();
    }

    private String encodeAsBase64(byte inp[]) {
        return Base64.getEncoder().encodeToString(inp);
    }

    private void generateHTML(Map<String, ? super Object> params,
                              PrintStream theOut,
                              byte[] jnlp_bytes_browser, String jnlpfile_browser,
                              byte[] jnlp_bytes_webstart, String jnlpfile_webstart,
                              Map<TemplatePlaceholders, String> templateStrings,
                              boolean swingMode) {
        String poff = "    ";
        String poff2 = poff + poff;
        String poff3 = poff2 + poff;

        try {
            XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            XMLStreamWriter xout = xmlOutputFactory.createXMLStreamWriter(baos, "utf-8");

            String appletParams = getAppletParameters(params);
            String jnlp_content_browser = null;
            String jnlp_content_webstart = null;

            boolean embedJNLP = EMBED_JNLP.fetchFrom(params);
            boolean includeDT = INCLUDE_DT.fetchFrom(params);

            if (embedJNLP) {
                jnlp_content_browser = encodeAsBase64(jnlp_bytes_browser);
                jnlp_content_webstart = encodeAsBase64(jnlp_bytes_webstart);
            }

            xout.writeStartElement("html");
            xout.writeStartElement("head");
            String dtURL = includeDT ? EMBEDDED_DT : PUBLIC_DT;
            if (templateStrings != null) {
                templateStrings.put(TemplatePlaceholders.SCRIPT_URL, dtURL);

                ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                XMLStreamWriter xo2 = xmlOutputFactory.createXMLStreamWriter(baos2, "utf-8");
                xo2.writeStartElement("SCRIPT");
                xo2.writeAttribute("src", dtURL);
                xo2.writeEndElement();
                xo2.close();
                templateStrings.put(TemplatePlaceholders.SCRIPT_CODE, baos2.toString());
            }
            xout.writeStartElement("SCRIPT");
            xout.writeAttribute("src", dtURL);
            xout.writeEndElement();

            List<String> w_app = new ArrayList<>();
            List<String> w_platform = new ArrayList<>();
            List<String> w_callback = new ArrayList<>();

            addToList(w_app, "url", jnlpfile_webstart, true);
            if (jnlp_content_webstart != null) {
                addToList(w_app, "jnlp_content", jnlp_content_webstart, true);
            }

            addToList(w_platform, "javafx", FX_PLATFORM.fetchFrom(params), true);
            String vmargs = getJvmArguments(params, true);
            if (vmargs != null) {
                addToList(w_platform, "jvmargs", vmargs, true);
            }

            if (!"".equals(appletParams)) {
                addToList(w_app, "params", "{" + appletParams + "}", false);
            }


            for (Map.Entry<String, String> callbackEntry : JS_CALLBACKS.fetchFrom(params).entrySet()) {
                addToList(w_callback, callbackEntry.getKey(), callbackEntry.getValue(), false);
            }

            //prepare content of launchApp function
            StringBuilder out_launch_code = new StringBuilder();
            out_launch_code.append(poff2).append("dtjava.launch(");
            out_launch_code.append(listToString(w_app, poff3)).append(",\n");
            out_launch_code.append(listToString(w_platform, poff3)).append(",\n");
            out_launch_code.append(listToString(w_callback, poff3)).append("\n");
            out_launch_code.append(poff2).append(");\n");

            xout.writeStartElement("script");
            xout.writeCharacters("\n" + poff + "function launchApplication(jnlpfile) {\n");
            xout.writeCharacters(out_launch_code.toString());
            xout.writeCharacters(poff2 + "return false;\n");
            xout.writeCharacters(poff + "}\n");
            xout.writeEndElement();

            if (templateStrings != null) {
                templateStrings.put(TemplatePlaceholders.LAUNCH_CODE,
                        out_launch_code.toString());
            }

            //applet deployment
            String appId = IDENTIFIER.fetchFrom(params);
            String placeholder = PLACEHOLDER.fetchFrom(params);

            //prepare content of embedApp()
            List<String> p_app = new ArrayList<>();
            List<String> p_platform = new ArrayList<>();
            List<String> p_callback = new ArrayList<>();

            if (appId != null) {
                addToList(p_app, "id", appId, true);
            }
            boolean isSwingApp = SWING_APP.fetchFrom(params);
            if (isSwingApp) {
                addToList(p_app, "toolkit", "swing", true);
            }
            addToList(p_app, "url", jnlpfile_browser, true);
            addToList(p_app, "placeholder", placeholder, true);
            addToList(p_app, "width", EMBEDDED_WIDTH.fetchFrom(params), true);
            addToList(p_app, "height", EMBEDDED_HEIGHT.fetchFrom(params), true);
            if (jnlp_content_browser != null) {
                addToList(p_app, "jnlp_content", jnlp_content_browser, true);
            }

            addToList(p_platform, "javafx", FX_PLATFORM.fetchFrom(params), true);
            if (vmargs != null) {
                addToList(p_platform, "jvmargs", vmargs, true);
            }

            for (Map.Entry<String, String> callbackEntry : JS_CALLBACKS.fetchFrom(params).entrySet()) {
                addToList(w_callback, callbackEntry.getKey(), callbackEntry.getValue(), false);
            }

            if (!"".equals(appletParams)) {
                addToList(p_app, "params", "{" + appletParams + "}", false);
            }

            if (swingMode) {
                //Splash will not work in SwingMode
                //Unless user overwrites onGetSplash handler (and that means he handles splash on his own)
                // we will reset splash function to be "none"
                boolean needOnGetSplashImpl = true;
                for (String callback : JS_CALLBACKS.fetchFrom(params).keySet()) {
                    if ("onGetSplash".equals(callback)) {
                        needOnGetSplashImpl = false;
                    }
                }

                if (needOnGetSplashImpl) {
                    addToList(p_callback, "onGetSplash", "function() {}", false);
                }
            }

            StringBuilder out_embed_dynamic = new StringBuilder();
            out_embed_dynamic.append("dtjava.embed(\n");
            out_embed_dynamic.append(listToString(p_app, poff3)).append(",\n");
            out_embed_dynamic.append(listToString(p_platform, poff3)).append(",\n");
            out_embed_dynamic.append(listToString(p_callback, poff3)).append("\n");

            out_embed_dynamic.append(poff2).append(");\n");

            //now wrap content with function
            String embedFuncName = "javafxEmbed" + IDENTIFIER.fetchFrom(params);
            ByteArrayOutputStream baos_embed_onload = new ByteArrayOutputStream();
            XMLStreamWriter xo_embed_onload = xmlOutputFactory.createXMLStreamWriter(baos_embed_onload, "utf-8");
            writeEmbeddedDynamic(out_embed_dynamic, embedFuncName, xo_embed_onload);
            xo_embed_onload.close();
            String out_embed_onload = xmlPrettyPrint(baos_embed_onload.toString());

            if (templateStrings != null) {
                templateStrings.put(
                        TemplatePlaceholders.EMBED_CODE_ONLOAD,
                        out_embed_onload);
                templateStrings.put(
                        TemplatePlaceholders.EMBED_CODE_DYNAMIC,
                        out_embed_dynamic.toString());
            }

            writeEmbeddedDynamic(out_embed_dynamic, embedFuncName, xout);

            xout.writeEndElement(); //head

            xout.writeStartElement("body");
            xout.writeStartElement("h2");
            xout.writeCharacters("Test page for ");
            xout.writeStartElement("b");
            xout.writeCharacters(APP_NAME.fetchFrom(params));
            xout.writeEndElement(); // b
            xout.writeEndElement(); // h2

            xout.writeStartElement("b");
            xout.writeCharacters("Webstart:");
            xout.writeEndElement();

            xout.writeStartElement("a");
            xout.writeAttribute("href", jnlpfile_webstart);
            xout.writeAttribute("onclick", "return launchApplication('" + jnlpfile_webstart + "');");
            xout.writeCharacters("click to launch this app as webstart");
            xout.writeEndElement(); // a

            xout.writeEmptyElement("br");
            xout.writeEmptyElement("hr");
            xout.writeEmptyElement("br");
            xout.writeCharacters("\n");
            xout.writeComment(" Applet will be inserted here ");

            xout.writeStartElement("div");
            xout.writeAttribute("id", placeholder);
            xout.writeEndElement(); //div
            xout.writeEndElement(); // body
            xout.writeEndElement(); // html
            xout.close();

            theOut.print(xmlPrettyPrint(baos.toString()));

        } catch (XMLStreamException | TransformerException e) {
            e.printStackTrace();
        }
    }

    private void writeEmbeddedDynamic(StringBuilder out_embed_dynamic, String embedFuncName, XMLStreamWriter xo_embed_onload) throws XMLStreamException {
        xo_embed_onload.writeStartElement("script");
        xo_embed_onload.writeCharacters("\n    function ");
        xo_embed_onload.writeCharacters(embedFuncName);
        xo_embed_onload.writeCharacters("() {\n        ");
        xo_embed_onload.writeCharacters(out_embed_dynamic.toString());
        xo_embed_onload.writeCharacters("    }\n    ");
        xo_embed_onload.writeComment(
                " Embed FX application into web page once page is loaded ");
        xo_embed_onload.writeCharacters("\n    dtjava.addOnloadCallback(");
        xo_embed_onload.writeCharacters(embedFuncName);
        xo_embed_onload.writeCharacters(");\n");

        xo_embed_onload.writeEndElement();
    }

    private void save(File outdir, String fname, byte[] content) throws IOException {
        save(new File(outdir, fname), content);
    }

    private void save(File f, byte[] content) throws IOException {
        if (f.exists()) {
            f.delete();
        }
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(content);
        fos.close();
    }

    private static void copyFileToOutDir(
            InputStream isa, File fout) throws PackagerException {

        final File outDir = fout.getParentFile();
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new PackagerException("ERR_CreatingDirFailed", outDir.getPath());
        }
        try (InputStream is = isa; OutputStream out = new FileOutputStream(fout)) {
            byte[] buf = new byte[16384];
            int len;
            while ((len = is.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException ex) {
            throw new PackagerException(ex, "ERR_FileCopyFailed", outDir.getPath()); //FIXME I18N
        }
    }

    private String getAppletParameters(Map<String, ? super Object> params) {
        StringBuilder result = new StringBuilder();
        boolean addComma = false;
        for (Map.Entry<String, String> entry : ESCAPED_APPLET_PARAMS.fetchFrom(params).entrySet()) {
            if (addComma) {
                result.append(", ");
            }
            addComma = true;
            result.append("'")
                    .append(quoteEscape(entry.getKey()))
                    .append("' : '")
                    .append(quoteEscape(entry.getValue()))
                    .append("'");

        }
        for (Map.Entry<String, String> entry : APPLET_PARAMS.fetchFrom(params).entrySet()) {
            if (addComma) {
                result.append(", ");
            }
            addComma = true;
            result.append("'")
                    .append(quoteEscape(entry.getKey()))
                    .append("' : ")
                    .append(entry.getValue());

        }
        return result.toString();
    }

    String quoteEscape(String s) {
        return s.replaceAll("(['\"\\\\])", "\\\\$1");
    }

    private static String[] webFiles = {
            "javafx-loading-100x100.gif",
            dtFX,
            "javafx-loading-25x25.gif",
            "error.png",
            "upgrade_java.png",
            "javafx-chrome.png",
            "get_java.png",
            "upgrade_javafx.png",
            "get_javafx.png"
    };

    private static String prefixWebFiles = "/resources/web-files/";

    private boolean extractWebFiles(File outDir) throws PackagerException {
        return doExtractWebFiles(webFiles, outDir, webfilesDir);
    }

    private boolean doExtractWebFiles(String lst[], File outDir, String webFilesDir) throws PackagerException {
        File f = new File(outDir, webFilesDir);
        f.mkdirs();

        for (String s: lst) {
            InputStream is =
                    //FIXME different root?
                    PackagerLib.class.getResourceAsStream(prefixWebFiles+s);
            if (is == null) {
                System.err.println("Internal error. Missing resources [" +
                        (prefixWebFiles+s) + "]");
                return false;
            } else {
                copyFileToOutDir(is, new File(f, s));
            }
        }
        return true;
    }


}
