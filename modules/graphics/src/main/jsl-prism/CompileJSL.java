/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.scenario.effect.compiler.JSLC;
import com.sun.scenario.effect.compiler.JSLC.JSLCInfo;
import com.sun.scenario.effect.compiler.JSLParser;
import com.sun.scenario.effect.compiler.model.BaseType;
import com.sun.scenario.effect.compiler.model.Qualifier;
import com.sun.scenario.effect.compiler.model.Variable;
import com.sun.scenario.effect.compiler.tree.ProgramUnit;
import com.sun.scenario.effect.compiler.tree.TreeScanner;
import com.sun.scenario.effect.compiler.tree.VariableExpr;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;

/**
 * This class is only used at build time to generate EffectPeer
 * implementations from JSL definitions, and shouldn't be included in the
 * resulting runtime jar file.
 *
 *
 * Each composed shader will follow the same basic pattern:
 *
 *   float mask(...) {
 *       return coverage;
 *   }
 *   float4 paint(...) {
 *       return rgbacolor;
 *   }
 *   void main(...) {
 *       color = mask(...) * paint(...) * jsl_vertexColor;
 *   }
 *
 *
 * The composable Mask+Paint parts include:
 *
 *   Masks
 *     Solid
 *     Texture
 *     {Fill,Draw}Pgram
 *     {Fill,Draw}Ellipse
 *     {Fill,Draw}RoundRect
 *
 *   Paints
 *     Color
 *     LinearGradient (3 cycle method variants)
 *     RadialGradient (3 cycle method variants)
 *     ImagePattern
 *     Texture{RGB,YV12,etc}
 */
public class CompileJSL {

    private static enum InputParam {
        TEXCOORD0("pos0"),
        TEXCOORD1("pos1"),
        WINCOORD ("pixcoord"),
        VERTEXCOLOR("jsl_vertexColor");

        private String varName;
        private InputParam(String varName) {
            this.varName = varName;
        }
        public String getVarName() {
            return varName;
        }
    }

    private static enum MaskType {
        SOLID          ("Solid"),
        TEXTURE        ("Texture",        InputParam.TEXCOORD0),
        FILL_PGRAM     ("FillPgram",      InputParam.TEXCOORD0, InputParam.TEXCOORD1),
        DRAW_PGRAM     ("DrawPgram",      InputParam.TEXCOORD0, InputParam.TEXCOORD1),
        FILL_CIRCLE    ("FillCircle",     InputParam.TEXCOORD0, InputParam.TEXCOORD1),
        DRAW_CIRCLE    ("DrawCircle",     InputParam.TEXCOORD0, InputParam.TEXCOORD1),
        FILL_ELLIPSE   ("FillEllipse",    InputParam.TEXCOORD0, InputParam.TEXCOORD1),
        DRAW_ELLIPSE   ("DrawEllipse",    InputParam.TEXCOORD0, InputParam.TEXCOORD1),
        FILL_ROUNDRECT ("FillRoundRect",  InputParam.TEXCOORD0, InputParam.TEXCOORD1),
        DRAW_ROUNDRECT ("DrawRoundRect",  InputParam.TEXCOORD0, InputParam.TEXCOORD1),
        DRAW_SEMIROUNDRECT
                       ("DrawSemiRoundRect", InputParam.TEXCOORD0, InputParam.TEXCOORD1),
        //FILL_QUADCURVE ("FillQuadCurve",  InputParam.TEXCOORD0, InputParam.TEXCOORD1),
        FILL_CUBICCURVE("FillCubicCurve", InputParam.TEXCOORD0, InputParam.TEXCOORD1);

        private String name;
        private InputParam[] inputParams;
        private MaskType(String name, InputParam... inputParams) {
            this.name = name;
            this.inputParams = inputParams;
        }
        public String getName() {
            return name;
        }
        public InputParam[] getInputParams() {
            return inputParams;
        }
    }

    private static enum AlphaMaskType {
        ALPHA_ONE            ("AlphaOne"),
        ALPHA_TEXTURE        ("AlphaTexture",           InputParam.TEXCOORD0),
        ALPHA_TEXTURE_DIFF   ("AlphaTextureDifference", InputParam.TEXCOORD0);

        private String name;
        private InputParam[] inputParams;
        private AlphaMaskType(String name, InputParam... inputParams) {
            this.name = name;
            this.inputParams = inputParams;
        }
        public String getName() {
            return name;
        }
        public InputParam[] getInputParams() {
            return inputParams;
        }
    }

    private static enum CycleType {
        PAD    ("None"),
        REFLECT("Reflect"),
        REPEAT ("Repeat");

        private String methodSuffix;
        private CycleType(String methodSuffix) {
            this.methodSuffix = methodSuffix;
        }
        public String getMethodSuffix() {
            return methodSuffix;
        }
    }

    private static enum PaintType {
        LINEAR("Linear"),
        RADIAL("Radial");

        private String name;
        private PaintType(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
    }

    private static class ShaderInfo {
        private String name;
        private String sourceCode;
        private long sourceTime;
        private InputParam[] inputParams;

        ShaderInfo(String name, String sourceCode, long sourceTime,
                   InputParam... inputParams)
        {
            this.name = name;
            this.sourceCode = sourceCode;
            this.sourceTime = sourceTime;
            this.inputParams = inputParams;
        }

        public String getName() {
            return name;
        }

        public String getSourceCode() {
            return sourceCode;
        }

        public long getSourceTime() {
            return sourceTime;
        }

        public InputParam[] getInputParams() {
            return inputParams;
        }
    }

    private static ShaderInfo getMaskInfo(JSLCInfo jslcinfo, MaskType maskType)
        throws Exception
    {
        return getMaskInfo(jslcinfo, maskType.getName(), maskType.getInputParams());
    }

    private static ShaderInfo getMaskInfo(JSLCInfo jslcinfo, AlphaMaskType maskType)
        throws Exception
    {
        return getMaskInfo(jslcinfo, maskType.getName(), maskType.getInputParams());
    }

    private static ShaderInfo getMaskInfo(JSLCInfo jslcinfo, String maskName,
                                          InputParam... inputParams)
        throws Exception
    {
        String shaderName = "Mask" + maskName;
        File maskFile = jslcinfo.getJSLFile(shaderName);
        String maskSource = readFile(maskFile);
        long maskTime = maskFile.lastModified();
        return new ShaderInfo(maskName, maskSource, maskTime, inputParams);
    }

    private static ShaderInfo getPaintInfo(JSLCInfo jslcinfo, String paintName,
                                           InputParam... inputParams)
        throws Exception
    {
        String shaderName = "Paint" + paintName;
        File paintFile = jslcinfo.getJSLFile(shaderName);
        String paintSource = readFile(paintFile);
        long paintTime = paintFile.lastModified();
        return new ShaderInfo(paintName, paintSource, paintTime, inputParams);
    }

    private static void compileColorPaint(JSLCInfo jslcinfo, ShaderInfo maskInfo, boolean alphaTest)
        throws Exception
    {
        ShaderInfo colorInfo = getPaintInfo(jslcinfo, "Color");
        String outputName = maskInfo.getName() + "_Color";
        ShaderInfo fullSource = getFullSource(outputName, maskInfo, colorInfo, alphaTest);
        compileShader(jslcinfo, fullSource);
    }

    private static void compileGradientPaint(JSLCInfo jslcinfo,
                                             ShaderInfo maskInfo,
                                             PaintType paintType,
                                             CycleType cycleType,
                                             boolean alphaTest)
        throws Exception
    {
        String paintName = paintType.getName() + "Gradient";
        String multiSource = readShaderFile(jslcinfo, "PaintMultiGradient");
        long multiTime = shaderFileTime(jslcinfo, "PaintMultiGradient");
        String paintSource = readShaderFile(jslcinfo, "Paint" + paintName);
        long paintTime = shaderFileTime(jslcinfo, "Paint" + paintName);
        paintSource = String.format(paintSource, cycleType.getMethodSuffix());
        String gradSource = multiSource + "\n" + paintSource;
        long gradTime = Math.max(multiTime, paintTime);
        ShaderInfo gradInfo = new ShaderInfo(paintName, gradSource, gradTime, InputParam.WINCOORD);
        String outputName =
            maskInfo.getName() + "_" +
            paintName + "_" +
            cycleType.toString();
        ShaderInfo fullSource = getFullSource(outputName, maskInfo, gradInfo, alphaTest);
        compileShader(jslcinfo, fullSource);
    }

    private static void compileAlphaGradientPaint(JSLCInfo jslcinfo,
                                                  ShaderInfo maskInfo,
                                                  PaintType paintType,
                                                  boolean alphaTest)
        throws Exception
    {
        String paintName = paintType.getName() + "Gradient";
        ShaderInfo paintInfo = getPaintInfo(jslcinfo, "AlphaTexture" + paintName, InputParam.TEXCOORD1);
        String outputName = maskInfo.getName() + "_" + paintName;
        ShaderInfo fullSource = getFullSource(outputName, maskInfo, paintInfo, alphaTest);
        compileShader(jslcinfo, fullSource);
    }

    private static void compilePatternPaint(JSLCInfo jslcinfo, ShaderInfo maskInfo, boolean alphaTest)
        throws Exception
    {
        String paintName = "ImagePattern";
        ShaderInfo paintInfo = getPaintInfo(jslcinfo, paintName, InputParam.WINCOORD);
        String outputName = maskInfo.getName() + "_" + paintName;
        ShaderInfo fullSource = getFullSource(outputName, maskInfo, paintInfo, alphaTest);
        compileShader(jslcinfo, fullSource);
    }

    private static void compileAlphaPatternPaint(JSLCInfo jslcinfo, ShaderInfo maskInfo, boolean alphaTest)
        throws Exception
    {
        String paintName = "ImagePattern";
        ShaderInfo paintInfo = getPaintInfo(jslcinfo, "AlphaTexture" + paintName, InputParam.TEXCOORD1);
        String outputName = maskInfo.getName() + "_" + paintName;
        ShaderInfo fullSource = getFullSource(outputName, maskInfo, paintInfo, alphaTest);
        compileShader(jslcinfo, fullSource);
    }

    private static void compileSolidTexture(JSLCInfo jslcinfo, String suffix, boolean alphaTest)
        throws Exception
    {
        ShaderInfo maskInfo = getMaskInfo(jslcinfo, MaskType.SOLID);
        ShaderInfo paintInfo = getPaintInfo(jslcinfo, "Texture" + suffix, InputParam.TEXCOORD0);
        ShaderInfo fullSource = getFullSource("Solid_Texture" + suffix, maskInfo, paintInfo, alphaTest);
        compileShader(jslcinfo, fullSource);
    }

    private static void compileMaskTexture(JSLCInfo jslcinfo, String suffix, boolean alphaTest)
        throws Exception
    {
        ShaderInfo maskInfo = getMaskInfo(jslcinfo, MaskType.SOLID);
        ShaderInfo paintInfo = getPaintInfo(jslcinfo, "MaskTexture" + suffix,
                                            InputParam.TEXCOORD0, InputParam.TEXCOORD1);
        ShaderInfo fullSource = getFullSource("Mask_Texture" + suffix, maskInfo, paintInfo, alphaTest);
        compileShader(jslcinfo, fullSource);
    }

    private static void compileLCDShader(JSLCInfo jslcinfo, String suffix, boolean alphaTest)
        throws Exception
    {
        ShaderInfo maskInfo = getMaskInfo(jslcinfo, MaskType.SOLID);
        ShaderInfo paintInfo = getPaintInfo(jslcinfo, "Texture" + suffix,
                                            InputParam.TEXCOORD0,
                                            InputParam.TEXCOORD1,
                                            InputParam.VERTEXCOLOR);
        ShaderInfo fullSource = getFullSource("Solid_Texture" + suffix, maskInfo, paintInfo, alphaTest);
        compileShader(jslcinfo, fullSource);
    }

    private static String getParamString(ShaderInfo info) {
        String params = "";
        boolean first = true;
        for (InputParam input : info.getInputParams()) {
            if (!first) {
                params += ", ";
            } else {
                first = false;
            }
            params += input.getVarName();
        }
        return params;
    }

    private static ShaderInfo getFullSource(String outputName,
                                            ShaderInfo maskInfo,
                                            ShaderInfo paintInfo,
                                            boolean alphaTest)
        throws Exception
    {
        String maskSource;
        String maskCall;
        if (maskInfo.getName().equals("Solid") ||
            maskInfo.getName().equals("AlphaOne"))
        {
            // can omit trivial mask() call
            maskSource = "";
            maskCall = "";
        } else {
            maskSource = maskInfo.getSourceCode();
            maskCall = "mask(" + getParamString(maskInfo) + ") *";
        }
        String paintSource;
        String paintCall;
        String vertexColor = InputParam.VERTEXCOLOR.getVarName();
        if (paintInfo.getName().equals("Color")) {
            // can omit trivial paint() call
            paintSource = "";
            paintCall = "";
        } else {
            paintSource = paintInfo.getSourceCode();
            paintCall = "paint(" + getParamString(paintInfo) + ")";
            /* if VERTEXCOLOR is passed into paint as parameter
             * i.e. "paint(...,jsl_vertexColor)" then change:
             * "color = mask(...) * paint(...) * jsl_vertexColor;"
             * into "color = mask(...) * paint(...);"
             */
            if (paintCall.contains(InputParam.VERTEXCOLOR.getVarName())) {
                vertexColor = "";
            } else {
                paintCall += " *";
            }
        }
        String mainTemplate =
            "%s\n%s\n" +
            "void main()\n" +
            "{\n" +
            "    color = %s %s " + vertexColor + ";\n";
        if (alphaTest) {
            mainTemplate += "    if (color.a == 0.0) discard;\n";
            outputName += "_AlphaTest";
        }
        mainTemplate += "}\n";
        String fullSource =
            String.format(mainTemplate,
                          maskSource, paintSource,
                          maskCall, paintCall);
        long fullTime = Math.max(maskInfo.getSourceTime(),
                                 paintInfo.getSourceTime());
        return new ShaderInfo(outputName, fullSource, fullTime);
    }

    private static void compileShader(JSLCInfo jslcinfo, ShaderInfo info)
        throws Exception
    {
        compileShader(jslcinfo, info.getSourceCode(), info.getSourceTime(), info.getName());
    }

    private static void compileShader(JSLCInfo jslcinfo,
                                      String source, long sourcetime, String name)
        throws Exception
    {
        jslcinfo.shaderName = name;
        JSLC.ParserInfo pinfo = JSLC.compile(jslcinfo, source, sourcetime);

        File outFile = jslcinfo.getOutputFile("prism-ps/build/gensrc/{pkg}/shader/{name}_Loader.java");
        if (JSLC.outOfDate(outFile, sourcetime)) {
            if (pinfo == null) pinfo = JSLC.getParserInfo(source);
            PrismLoaderBackend loaderBackend = new PrismLoaderBackend(pinfo.parser, pinfo.program);
            JSLC.write(loaderBackend.getGlueCode(name), outFile);
        }
    }

    private static String readStream(InputStream in) {
        StringBuilder sb = new StringBuilder(1024);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            char[] chars = new char[1024];
            int numRead = 0;
            while ((numRead = reader.read(chars)) > -1) {
                sb.append(String.valueOf(chars, 0, numRead));
            }
        } catch (IOException e) {
            System.err.println("Error reading stream");
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                System.err.println("Error closing reader");
            }
        }
        return sb.toString();
    }

    private static String readFile(File file) throws Exception {
        return readStream(new FileInputStream(file));
    }

    private static String readShaderFile(JSLCInfo jslcinfo, String name)
        throws Exception
    {
        return readFile(jslcinfo.getJSLFile(name));
    }

    private static long shaderFileTime(JSLCInfo jslcinfo, String name) {
        return jslcinfo.getJSLFile(name).lastModified();
    }

    public static void main(String[] args) throws Exception {
        JSLCInfo jslcinfo = new JSLCInfo();
        Map<Integer, String> nameMap = jslcinfo.outNameMap;
        nameMap.put(JSLC.OUT_D3D, "prism-d3d/build/gensrc/{pkg}/d3d/hlsl/{name}.hlsl");
        nameMap.put(JSLC.OUT_ES2, "prism-es2/build/gensrc/{pkg}/es2/glsl/{name}.frag");
        jslcinfo.parseAllArgs(args);

        boolean alphaTest = false;
        for (int i = 0; i < 2; i++) {
            alphaTest = (i == 0) ? false : true;
            // create all the computational Mask+Paint combinations
            for (MaskType maskType : MaskType.values()) {
                ShaderInfo maskInfo = getMaskInfo(jslcinfo, maskType);
                compileColorPaint(jslcinfo, maskInfo, alphaTest);
                compilePatternPaint(jslcinfo, maskInfo, alphaTest);
                for (PaintType paintType : PaintType.values()) {
                    for (CycleType cycleType : CycleType.values()) {
                        compileGradientPaint(jslcinfo, maskInfo, paintType, cycleType, alphaTest);
                    }
                }
            }

            // create all the new style AlphaTexture+Paint combinations
            for (AlphaMaskType maskType : AlphaMaskType.values()) {
                ShaderInfo maskInfo = getMaskInfo(jslcinfo, maskType);
                compileColorPaint(jslcinfo, maskInfo, alphaTest);
                compileAlphaPatternPaint(jslcinfo, maskInfo, alphaTest);
                for (PaintType paintType : PaintType.values()) {
                    compileAlphaGradientPaint(jslcinfo, maskInfo, paintType, alphaTest);
                }
            }

            // create the basic Solid+Texture* shaders
            compileSolidTexture(jslcinfo, "RGB", alphaTest);
            compileMaskTexture(jslcinfo, "RGB", alphaTest);
            compileMaskTexture(jslcinfo, "Super", alphaTest);
            compileSolidTexture(jslcinfo, "YV12", alphaTest);
            compileSolidTexture(jslcinfo, "FirstPassLCD", alphaTest);
            compileLCDShader(jslcinfo, "SecondPassLCD", alphaTest);
        }
    }
}

class PrismLoaderBackend extends TreeScanner {
    private JSLParser parser;
    private int maxTexCoordIndex = -1;
    private boolean isPixcoordReferenced = false;

    public PrismLoaderBackend(JSLParser parser, ProgramUnit program) {
        this.parser = parser;
        scan(program);
    }

    private StringTemplate getTemplate(String type) {
        Reader template = new InputStreamReader(getClass().getResourceAsStream(type + "Glue.stg"));
        StringTemplateGroup group = new StringTemplateGroup(template, DefaultTemplateLexer.class);
        return group.getInstanceOf("glue");
    }

    public String getGlueCode(String shaderName) {
        Map<String, Variable> vars = parser.getSymbolTable().getGlobalVariables();
        StringBuilder samplerInit = new StringBuilder();
        StringBuilder paramInit = new StringBuilder();

        for (Variable v : vars.values()) {
            if (v.getQualifier() == Qualifier.PARAM) {
                String vname = v.getName();
                if (v.getType().getBaseType() == BaseType.SAMPLER) {
                    samplerInit.append("samplers.put(\"" + vname + "\", " + v.getReg() + ");\n");
                } else {
                    paramInit.append("params.put(\"" + vname + "\", " + v.getReg() + ");\n");
                }
            }
        }

        StringTemplate glue = getTemplate("PrismLoader");
        glue.setAttribute("shaderName", shaderName);
        glue.setAttribute("samplerInit", samplerInit.toString());
        glue.setAttribute("paramInit", paramInit.toString());
        glue.setAttribute("maxTexCoordIndex", maxTexCoordIndex);
        glue.setAttribute("isPixcoordUsed", isPixcoordReferenced);
        return glue.toString();
    }

    @Override
    public void visitVariableExpr(VariableExpr e) {
        String varName = e.getVariable().getName();
        if (varName.equals("pixcoord")) {
            isPixcoordReferenced = true;
        } else if (varName.equals("pos0")) {
            maxTexCoordIndex = Math.max(maxTexCoordIndex, 0);
        } else if (varName.equals("pos1")) {
            maxTexCoordIndex = Math.max(maxTexCoordIndex, 1);
        }
    }
}
