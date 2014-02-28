/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.compiler;

import com.sun.scenario.effect.compiler.backend.hw.ES2Backend;
import com.sun.scenario.effect.compiler.backend.hw.HLSLBackend;
import com.sun.scenario.effect.compiler.backend.prism.PrismBackend;
import com.sun.scenario.effect.compiler.backend.sw.java.JSWBackend;
import com.sun.scenario.effect.compiler.backend.sw.me.MEBackend;
import com.sun.scenario.effect.compiler.backend.sw.sse.SSEBackend;
import com.sun.scenario.effect.compiler.tree.ProgramUnit;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.stringtemplate.CommonGroupLoader;
import org.antlr.stringtemplate.StringTemplateGroup;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class JSLC {

    public static final int OUT_NONE     = (0 << 0);
    public static final int OUT_D3D      = (1 << 0);
    public static final int OUT_ES2      = (1 << 1);
    public static final int OUT_JAVA     = (1 << 2);
    public static final int OUT_PRISM    = (1 << 3);

    public static final int OUT_SSE_JAVA        = (1 << 4);
    public static final int OUT_SSE_NATIVE      = (1 << 5);
    public static final int OUT_ME_JAVA         = (1 << 6);
    public static final int OUT_ME_NATIVE       = (1 << 7);

    public static final int OUT_ME       = OUT_ME_JAVA | OUT_ME_NATIVE;
    public static final int OUT_SSE      = OUT_SSE_JAVA | OUT_SSE_NATIVE;

    public static final int OUT_SW_PEERS   = OUT_JAVA | OUT_SSE;
    public static final int OUT_HW_PEERS   = OUT_PRISM;
    public static final int OUT_HW_SHADERS = OUT_D3D | OUT_ES2;
    public static final int OUT_ALL        = OUT_SW_PEERS | OUT_HW_PEERS | OUT_HW_SHADERS;

    private static final String rootPkg = "com/sun/scenario/effect";
    
    static {
        CommonGroupLoader loader = new CommonGroupLoader(rootPkg + "/compiler/backend", null);
        StringTemplateGroup.registerGroupLoader(loader);
    }

    public static class OutInfo {
        public String basePath;
        public String filePrefix;
        public String fileSuffix;
    }

    public static class ParserInfo {
        public JSLParser parser;
        public ProgramUnit program;

        public ParserInfo(JSLParser parser, ProgramUnit program) {
            this.parser = parser;
            this.program = program;
        }
    }

    public static ParserInfo getParserInfo(String source) throws Exception {
        return getParserInfo(new ByteArrayInputStream(source.getBytes()));
    }

    public static ParserInfo getParserInfo(InputStream stream) throws Exception {
        JSLParser parser = parse(stream);
        ProgramUnit program = parser.translation_unit();
        return new ParserInfo(parser, program);
    }

    /**
     * If trimToOutDir is provided by the user, then we will output all files
     * under the out directory, for example if outDir=/foo/bar:
     *   /foo/bar/ + rootPkg + /impl/sw/java
     *   /foo/bar/ + rootPkg + /impl/sw/sse
     *   /foo/bar/ + rootPkg + /impl/sw/me
     *   /foo/bar/ + rootPkg + /impl/hw/d3d/hlsl
     *   /foo/bar/ + rootPkg + /impl/es2/glsl
     *   /foo/bar/ + rootPkg + /impl/prism/ps
     * 
     * Otherwise, we use the layout currently expected by decora-runtime
     * for core effects:
     *   ../decora-jsw/build/gensrc/     + rootPkg + /impl/sw/java
     *   ../decora-sse/build/gensrc/     + rootPkg + /impl/sw/sse
     *   ../decora-me/build/gensrc/      + rootPkg + /impl/sw/me
     *   ../decora-d3d/build/gensrc/     + rootPkg + /impl/hw/d3d/hlsl
     *   ../decora-es2/build/gensrc/     + rootPkg + /impl/es2/glsl
     *   ../decora-prism-ps/build/gensrc/+ rootPkg + /impl/prism/ps
     */
    private static Map<Integer, String> initDefaultInfoMap() {
        Map<Integer, String> infoMap = new HashMap<Integer, String>();
        infoMap.put(OUT_D3D,        "decora-d3d/build/gensrc/{pkg}/impl/hw/d3d/hlsl/{name}.hlsl");
        infoMap.put(OUT_ES2,        "decora-es2/build/gensrc/{pkg}/impl/es2/glsl/{name}.frag");
        infoMap.put(OUT_JAVA,       "decora-jsw/build/gensrc/{pkg}/impl/sw/java/JSW{name}Peer.java");
        infoMap.put(OUT_PRISM,      "decora-prism-ps/build/gensrc/{pkg}/impl/prism/ps/PPS{name}Peer.java");
        infoMap.put(OUT_SSE_JAVA,   "decora-sse/build/gensrc/{pkg}/impl/sw/sse/SSE{name}Peer.java");
        infoMap.put(OUT_ME_JAVA,    "decora-me/build/gensrc/{pkg}/impl/sw/me/ME{name}Peer.java");
        infoMap.put(OUT_SSE_NATIVE, "decora-sse-native/build/gensrc/SSE{name}Peer.cc");
        infoMap.put(OUT_ME_NATIVE,  "decora-me-native/build/gensrc/ME{name}Peer.cc");
        return infoMap;
    }

    public static ParserInfo compile(JSLCInfo jslcinfo,
                                     String str,
                                     long sourceTime)
        throws Exception
    {
        return compile(jslcinfo,
                       new ByteArrayInputStream(str.getBytes()), sourceTime);
    }

    public static ParserInfo compile(JSLCInfo jslcinfo, File file)
        throws Exception
    {
        return compile(jslcinfo, new FileInputStream(file), file.lastModified());
    }

    public static JSLParser parse(String str)
        throws Exception
    {
        return parse(new ByteArrayInputStream(str.getBytes()));
    }

    private static JSLParser parse(InputStream stream)
        throws Exception
    {
        // Read input
        ANTLRInputStream input = new ANTLRInputStream(stream);

        // Lexer
        JSLLexer lexer = new JSLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Parser and AST construction
        return new JSLParser(tokens);
    }

    private static ParserInfo compile(JSLCInfo jslcinfo,
                                      InputStream stream,
                                      long sourceTime)
        throws Exception
    {
        return compile(jslcinfo, stream, sourceTime, null);
    }

    private static ParserInfo compile(JSLCInfo jslcinfo,
                                      InputStream stream,
                                      long sourceTime,
                                      ParserInfo pinfo)
        throws Exception
    {
        int outTypes = jslcinfo.outTypes;
        String genericsName = jslcinfo.genericsName;
        String interfaceName = jslcinfo.interfaceName;
        String peerName = jslcinfo.peerName;
        String shaderName = jslcinfo.shaderName;
        if (peerName == null) peerName = shaderName;

        // Compiler
        if ((outTypes & OUT_D3D) != 0) {
            File outFile = jslcinfo.getOutputFile(OUT_D3D);
            if (jslcinfo.force || outOfDate(outFile, sourceTime)) {
                if (pinfo == null) pinfo = getParserInfo(stream);
                HLSLBackend hlslBackend = new HLSLBackend(pinfo.parser, pinfo.program);
                write(hlslBackend.getShader(), outFile);
            }
        }

        if ((outTypes & OUT_ES2) != 0) {
            File outFile = jslcinfo.getOutputFile(OUT_ES2);
            if (jslcinfo.force || outOfDate(outFile, sourceTime)) {
                if (pinfo == null) pinfo = getParserInfo(stream);
                ES2Backend es2Backend = new ES2Backend(pinfo.parser, pinfo.program);
                write(es2Backend.getShader(), outFile);
            }
        }

        if ((outTypes & OUT_JAVA) != 0) {
            File outFile = jslcinfo.getOutputFile(OUT_JAVA);
            if (jslcinfo.force || outOfDate(outFile, sourceTime)) {
                if (pinfo == null) pinfo = getParserInfo(stream);
                JSWBackend javaBackend = new JSWBackend(pinfo.parser, pinfo.program);
                String genCode = javaBackend.getGenCode(shaderName, peerName, genericsName, interfaceName);
                write(genCode, outFile);
            }
        }
        
        if ((outTypes & OUT_SSE) != 0) {
            File outFile = jslcinfo.getOutputFile(OUT_SSE_JAVA);
            // TODO: native code is always generated into the same
            // destination directory for now; need to make this more flexible
            File genCFile = jslcinfo.getOutputFile(OUT_SSE_NATIVE);

            boolean outFileStale = outOfDate(outFile, sourceTime);
            boolean genCFileStale = outOfDate(genCFile, sourceTime);
            if (jslcinfo.force || outFileStale || genCFileStale) {
                if (pinfo == null) pinfo = getParserInfo(stream);
                SSEBackend sseBackend = new SSEBackend(pinfo.parser, pinfo.program);
                SSEBackend.GenCode gen =
                    sseBackend.getGenCode(shaderName, peerName, genericsName, interfaceName);

                // write impl class
                if (outFileStale) {
                    write(gen.javaCode, outFile);
                }

                // write impl native code
                if (genCFileStale) {
                    write(gen.nativeCode, genCFile);
                }
            }
        }
        
        if ((outTypes & OUT_ME) != 0) {
            File outFile = jslcinfo.getOutputFile(OUT_ME_JAVA);
            // TODO: native code is always generated into the same
            // destination directory for now; need to make this more flexible
            File genCFile = jslcinfo.getOutputFile(OUT_ME_NATIVE);

            boolean outFileStale = outOfDate(outFile, sourceTime);
            boolean genCFileStale = outOfDate(genCFile, sourceTime);
            if (jslcinfo.force || outFileStale || genCFileStale) {
                if (pinfo == null) pinfo = getParserInfo(stream);
                MEBackend sseBackend = new MEBackend(pinfo.parser, pinfo.program);
                MEBackend.GenCode gen =
                    sseBackend.getGenCode(shaderName, peerName, genericsName, interfaceName);

                // write impl class
                if (outFileStale) {
                    write(gen.javaCode, outFile);
                }

                // write impl native code
                if (genCFileStale) {
                    write(gen.nativeCode, genCFile);
                }
            }
        }
        
        if ((outTypes & OUT_PRISM) != 0) {
            File outFile = jslcinfo.getOutputFile(OUT_PRISM);
            if (jslcinfo.force || outOfDate(outFile, sourceTime)) {
                if (pinfo == null) pinfo = getParserInfo(stream);
                PrismBackend prismBackend = new PrismBackend(pinfo.parser, pinfo.program);
                String genCode = prismBackend.getGlueCode(shaderName, peerName, genericsName, interfaceName);
                write(genCode, outFile);
            }
        }

        return pinfo;
    }

    public static boolean outOfDate(File outFile, long sourceTime) {
        if (sourceTime < outFile.lastModified()) {
            return false;
        }
        return true;
    }

    public static void write(String str, File outFile) throws Exception {
        File outDir = outFile.getParentFile();
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        FileWriter fw = null;
        try {
            fw = new FileWriter(outFile);
            fw.write(str);
            fw.flush();
        } finally {
            if (fw != null) {
                fw.close();
            }
        }
    }

    public static class JSLCInfo {
        public int outTypes;
        public boolean force;
        public String outDir;
        public boolean trimToOutDir;
        public List<String> srcDirs = new ArrayList<String>();
        public String shaderName;
        public String peerName;
        public String genericsName;
        public String interfaceName;
        public String pkgName = rootPkg;
        public Map<Integer, String> outNameMap = initDefaultInfoMap();

        private String extraOpts;

        public JSLCInfo() {
        }

        public JSLCInfo(String extraOpts) {
            this.extraOpts = extraOpts;
        }

        public void usage(PrintStream out) {
            StackTraceElement callers[] = Thread.currentThread().getStackTrace();
            String prog = callers[callers.length - 1].getClassName();
            String prefix0 = "Usage: java "+prog+" ";
            String prefix1 = "";
            for (int i = 0; i < prefix0.length(); i++) prefix1 += " ";
            out.println(prefix0+"[-d3d | -es2 | -java | -sse | -me | -sw | -hw | -all]");
            out.println(prefix1+"[-o <outdir>] [-i <srcdir>] [-t]");
            out.println(prefix1+"[-name <name>] [-ifname <interface name>]");
            if (extraOpts != null) {
                out.println(prefix1+extraOpts);
            }
        }

        public void error(String error) {
            System.err.println(error);
            usage(System.err);
            System.exit(1);
        }

        public void parseAllArgs(String args[]) {
            int index = parseArgs(args);
            if (index != args.length) {
                error("unrecognized argument: "+args[index]);
            }
        }

        public int parseArgs(String args[]) {
            int i = 0;
            while (i < args.length) {
                int consumed = parseArg(args, i);
                if (consumed < 0) {
                    usage(System.err);
                    System.exit(1);
                } else if (consumed == 0) {
                    break;
                }
                i += consumed;
            }
            return i;
        }

        public int parseArg(String args[], int index) {
            String arg = args[index++];
            if (arg.equals("-force")) {
                force = true;
            } else if (arg.equals("-d3d")) {
                outTypes |= OUT_D3D;
            } else if (arg.equals("-es2")) {
                outTypes |= OUT_ES2;
            } else if (arg.equals("-java")) {
                outTypes |= OUT_JAVA;
            } else if (arg.equals("-sse")) {
                outTypes |= OUT_SSE;
            } else if (arg.equals("-me")) {
                outTypes |= OUT_ME;
            } else if (arg.equals("-sw")) {
                outTypes = OUT_SW_PEERS;
            } else if (arg.equals("-hw")) {
                outTypes = OUT_HW_PEERS | OUT_HW_SHADERS;
            } else if (arg.equals("-all")) {
                outTypes = OUT_ALL;
            } else if (arg.equals("-help")) {
                usage(System.out);
                System.exit(0);
            } else if (arg.equals("-t")) {
                trimToOutDir = true;
            } else {
                try {
                    // options with 1 argument
                    if (arg.equals("-o")) {
                        outDir = args[index];
                    } else if (arg.equals("-i")) {
                        srcDirs.add(args[index]);
                    } else if (arg.equals("-name")) {
                        shaderName = args[index];
                    } else if (arg.equals("-ifname")) {
                        interfaceName = args[index];
                    } else if (arg.equals("-pkg")) {
                        pkgName = args[index];
                    } else {
                        return 0;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    return -1;
                }
                return 2;
            }
            return 1;
        }

        public File getJSLFile() {
            return getJSLFile(shaderName);
        }

        public File getJSLFile(String jslBaseName) {
            return getInputFile(jslBaseName + ".jsl");
        }

        public File getInputFile(String filename) {
            if (srcDirs.isEmpty()) {
                File f = new File(filename);
                if (f.exists()) {
                    return f;
                }
            } else {
                for (String dir : srcDirs) {
                    File f = new File(dir, filename);
                    if (f.exists()) {
                        return f;
                    }
                }
            }

            error("Input file not found: "+filename);
            // NOT REACHED
            return null;
        }

        public File getOutputFile(String outName) {
            if (trimToOutDir) {
                outName = outName.substring(outName.indexOf("gensrc") + 7);
            }
            outName = outName.replace("{pkg}", pkgName);
            outName = outName.replace("{name}", peerName == null ? shaderName : peerName);
            return outDir == null ? new File(outName) : new File(outDir, outName);
        }

        public File getOutputFile(int outType) {
            String fileName = outNameMap.get(outType);
            return getOutputFile(fileName);
        }
    }

    public static void main(String[] args) throws Exception {
        JSLCInfo jslcinfo = new JSLCInfo("<inputfile>");
        int index = jslcinfo.parseArgs(args);

        if (index != args.length - 1) {
            jslcinfo.error("Must specify one input file");
        }
        String arg = args[index];
        if (!arg.endsWith(".jsl") || arg.length() < 5) {
            jslcinfo.error("Input file name must end with '.jsl'");
        }
        File inFile = jslcinfo.getInputFile(arg);
        if (jslcinfo.shaderName == null) {
            jslcinfo.shaderName = arg.substring(0, arg.length()-4);
        }

        compile(jslcinfo, inFile);
    }
}
