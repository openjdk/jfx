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

package com.sun.prism.es2;

import com.sun.prism.impl.BaseGraphicsResource;
import com.sun.prism.impl.Disposer;
import com.sun.prism.ps.Shader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an OpenGL shader program object, which can be constructed from
 * the source code for a vertex shader, a fragment shader, or both.
 * Contains convenience methods for enabling/disabling shader state.
 * <p>
 * Usage example:
 * <pre>
 *     String source =
 *         "uniform sampler2D myTex;" +
 *         "void main(void)" +
 *         "{" +
 *         "    vec4 src = texture2D(myTex, gl_TexCoord[0].st);" +
 *         "    gl_FragColor = src.bgra;" + // swizzle!
 *         "}";
 *     ES2Shader shader = new ES2Shader(source);
 *     shader.setConstant("myTex", 0); // myTex will be on texture unit 0
 *     ...
 *     shader.enable();
 *     texture.enable();
 *     texture.bind();
 *     ...
 *     texture.disable();
 *     shader.disable();
 * };
 * </pre>
 */
public class ES2Shader extends BaseGraphicsResource implements Shader {

    private static class Uniform {

        private int location;
        private Object values;
    }

    /**
     * The handle to the OpenGL shader program object.
     */
    private int programID;
    private final ES2Context context;
    private final Map<String, Uniform> uniforms = new HashMap<String, Uniform>();
    private final int maxTexCoordIndex;
    private final boolean isPixcoordUsed;
    private boolean valid;
    private float[] currentMatrix;

    private ES2Shader(ES2Context context, int programID,
            int vertexShaderID, int[] fragmentShaderID,
            Map<String, Integer> samplers,
            int maxTexCoordIndex, boolean isPixcoordUsed)
            throws RuntimeException {
        super(new ES2ShaderDisposerRecord(context,
                vertexShaderID,
                fragmentShaderID,
                programID));
        this.context = context;
        this.programID = programID;
        this.maxTexCoordIndex = maxTexCoordIndex;
        this.isPixcoordUsed = isPixcoordUsed;
        this.valid = (programID != 0);

        if (valid && samplers != null) {
            // save/restore the current program (creating an ES2Shader
            // should not affect context state)
            int currentProgram = context.getShaderProgram();
            context.setShaderProgram(programID);
            for (String key : samplers.keySet()) {
                setConstant(key, samplers.get(key));
            }
            context.setShaderProgram(currentProgram);
        }
    }

    static ES2Shader createFromSource(ES2Context context,
            String vert, String[] frag,
            Map<String, Integer> samplers,
            Map<String, Integer> attributes,
            int maxTexCoordIndex,
            boolean isPixcoordUsed) {
        GLContext glCtx = context.getGLContext();
        if (!glCtx.isShaderCompilerSupported()) {
            throw new RuntimeException("Shader compiler not available on this device");
        }

        if (vert == null || frag == null || frag.length == 0) {
            throw new RuntimeException(
                    "Both vertexShaderSource and fragmentShaderSource "
                    + "must be specified");
        }

        int vertexShaderID = glCtx.compileShader(vert, true);
        if (vertexShaderID == 0) {
            throw new RuntimeException("Error creating vertex shader");
        }

        int[] fragmentShaderID = new int[frag.length];
        for (int i = 0; i < frag.length; i++) {
            fragmentShaderID[i] = glCtx.compileShader(frag[i], false);
            if (fragmentShaderID[i] == 0) {
                glCtx.deleteShader(vertexShaderID);
                //TODO: delete any fragment shaders already created
                throw new RuntimeException("Error creating fragment shader");
            }
        }

        String[] attrs = new String[attributes.size()];
        int[] indexs = new int[attrs.length];
        int i = 0;
        for (String attr : attributes.keySet()) {
            attrs[i] = attr;
            indexs[i] = attributes.get(attr);
            i++;
        }
        int programID = glCtx.createProgram(vertexShaderID, fragmentShaderID,
                attrs, indexs);
        if (programID == 0) {
            // createProgram() will have already detached/deleted
            // vertexShader and fragmentShader resources
            throw new RuntimeException("Error creating shader program");
        }

        return new ES2Shader(context,
                programID, vertexShaderID, fragmentShaderID,
                samplers, maxTexCoordIndex, isPixcoordUsed);
    }

    static ES2Shader createFromSource(ES2Context context,
            String vert, InputStream frag,
            Map<String, Integer> samplers,
            Map<String, Integer> attributes,
            int maxTexCoordIndex,
            boolean isPixcoordUsed) {
        String[] fragmentShaderSource = new String[] {readStreamIntoString(frag)};
        return createFromSource(context, vert, fragmentShaderSource, samplers,
                attributes, maxTexCoordIndex, isPixcoordUsed);
    }

    static String readStreamIntoString(InputStream in) {
        StringBuffer sb = new StringBuffer(1024);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            char[] chars = new char[1024];
            int numRead = 0;
            while ((numRead = reader.read(chars)) > -1) {
                sb.append(String.valueOf(chars, 0, numRead));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading shader stream");
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException("Error closing reader");
            }
        }
        return sb.toString();
    }

    /**
     * Returns the underlying OpenGL program object handle for this fragment
     * shader. Most applications will not need to access this, since it is
     * handled automatically by the enable() and dispose() methods.
     *
     * @return the OpenGL program object handle for this fragment shader
     */
    public int getProgramObject() {
        return programID;
    }

    /**
     * Returns the maximum texcoord index referenced by this shader program.
     *
     * @return the maximum texcoord index referenced by this shader program
     */
    public int getMaxTexCoordIndex() {
        return maxTexCoordIndex;
    }

    /**
     * Returns true if this shader uses the special pixcoord variable,
     * otherwise returns false
     *
     * @return true if this shader uses the special pixcoord variable
     */
    public boolean isPixcoordUsed() {
        return isPixcoordUsed;
    }

    private Uniform getUniform(String name) {
        Uniform uniform = uniforms.get(name);
        if (uniform == null) {
            // cache native uniform locations in a hashmap for quicker access
            int loc = context.getGLContext().getUniformLocation(programID, name);
            uniform = new Uniform();
            uniform.location = loc;
            uniforms.put(name, uniform);
        }
        return uniform;
    }

    /**
     * Enables this shader program in the current GL context's state.
     *
     * @throws RuntimeException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void enable() throws RuntimeException {
        context.updateShaderProgram(programID);
    }

    /**
     * Disables this shader program in the current GL context's state.
     *
     * @throws RuntimeException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void disable() throws RuntimeException {
        // TODO: remove disable() method from Shader interface... (RT-27442)
        context.updateShaderProgram(0);
    }

    public boolean isValid() {
        return valid;
    }

    /**
     * Sets the uniform variable of the given name with the provided
     * integer value.
     *
     * @param name the name of the uniform variable to be set
     * @param i0 the first uniform parameter
     * @throws RuntimeException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void setConstant(String name, int i0)
            throws RuntimeException {
        Uniform uniform = getUniform(name);
        if (uniform.location == -1) {
            return;
        }
        if (uniform.values == null) {
            uniform.values = new int[1];
        }
        int[] values = (int[]) uniform.values;
        if (values[0] != i0) {
            values[0] = i0;
            context.getGLContext().uniform1i(uniform.location, i0);
        }
    }

    /**
     * Sets the uniform variable of the given name with the provided
     * integer values.
     *
     * @param name the name of the uniform variable to be set
     * @param i0 the first uniform parameter
     * @param i1 the second uniform parameter
     * @throws RuntimeException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void setConstant(String name, int i0, int i1)
            throws RuntimeException {
        Uniform uniform = getUniform(name);
        if (uniform.location == -1) {
            return;
        }
        if (uniform.values == null) {
            uniform.values = new int[2];
        }
        int[] values = (int[]) uniform.values;
        if (values[0] != i0 || values[1] != i1) {
            values[0] = i0;
            values[1] = i1;
            context.getGLContext().uniform2i(uniform.location, i0, i1);
        }
    }

    /**
     * Sets the uniform variable of the given name with the provided
     * integer values.
     *
     * @param name the name of the uniform variable to be set
     * @param i0 the first uniform parameter
     * @param i1 the second uniform parameter
     * @param i2 the third uniform parameter
     * @throws RuntimeException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void setConstant(String name, int i0, int i1, int i2)
            throws RuntimeException {
        Uniform uniform = getUniform(name);
        if (uniform.location == -1) {
            return;
        }
        if (uniform.values == null) {
            uniform.values = new int[3];
        }
        int[] values = (int[]) uniform.values;
        if (values[0] != i0 || values[1] != i1 || values[2] != i2) {
            values[0] = i0;
            values[1] = i1;
            values[2] = i2;
            context.getGLContext().uniform3i(uniform.location, i0, i1, i2);
        }
    }

    /**
     * Sets the uniform variable of the given name with the provided
     * integer values.
     *
     * @param name the name of the uniform variable to be set
     * @param i0 the first uniform parameter
     * @param i1 the second uniform parameter
     * @param i2 the third uniform parameter
     * @param i3 the fourth uniform parameter
     * @throws RuntimeException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void setConstant(String name, int i0, int i1, int i2, int i3)
            throws RuntimeException {
        Uniform uniform = getUniform(name);
        if (uniform.location == -1) {
            return;
        }
        if (uniform.values == null) {
            uniform.values = new int[4];
        }
        int[] values = (int[]) uniform.values;
        if (values[0] != i0 || values[1] != i1 || values[2] != i2 || values[3] != i3) {
            values[0] = i0;
            values[1] = i1;
            values[2] = i2;
            values[3] = i3;
            context.getGLContext().uniform4i(uniform.location, i0, i1, i2, i3);
        }
    }

    /**
     * Sets the uniform variable of the given name with the provided
     * float value.
     *
     * @param name the name of the uniform variable to be set
     * @param f0 the first uniform parameter
     * @throws RuntimeException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void setConstant(String name, float f0)
            throws RuntimeException {
        Uniform uniform = getUniform(name);
        if (uniform.location == -1) {
            return;
        }
        if (uniform.values == null) {
            uniform.values = new float[1];
        }
        float[] values = (float[]) uniform.values;
        if (values[0] != f0) {
            values[0] = f0;
            context.getGLContext().uniform1f(uniform.location, f0);
        }
    }

    /**
     * Sets the uniform variable of the given name with the provided
     * float values.
     *
     * @param name the name of the uniform variable to be set
     * @param f0 the first uniform parameter
     * @param f1 the second uniform parameter
     * @throws RuntimeException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void setConstant(String name, float f0, float f1)
            throws RuntimeException {
        Uniform uniform = getUniform(name);
        if (uniform.location == -1) {
            return;
        }
        if (uniform.values == null) {
            uniform.values = new float[2];
        }
        float[] values = (float[]) uniform.values;
        if (values[0] != f0 || values[1] != f1) {
            values[0] = f0;
            values[1] = f1;
            context.getGLContext().uniform2f(uniform.location, f0, f1);
        }
    }

    /**
     * Sets the uniform variable of the given name with the provided
     * float values.
     *
     * @param name the name of the uniform variable to be set
     * @param f0 the first uniform parameter
     * @param f1 the second uniform parameter
     * @param f2 the third uniform parameter
     * @throws RuntimeException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void setConstant(String name, float f0, float f1, float f2)
            throws RuntimeException {
        Uniform uniform = getUniform(name);
        if (uniform.location == -1) {
            return;
        }
        if (uniform.values == null) {
            uniform.values = new float[3];
        }
        float[] values = (float[]) uniform.values;
        if (values[0] != f0 || values[1] != f1 || values[2] != f2) {
            values[0] = f0;
            values[1] = f1;
            values[2] = f2;
            context.getGLContext().uniform3f(uniform.location, f0, f1, f2);
        }
    }

    /**
     * Sets the uniform variable of the given name with the provided
     * float values.
     *
     * @param name the name of the uniform variable to be set
     * @param f0 the first uniform parameter
     * @param f1 the second uniform parameter
     * @param f2 the third uniform parameter
     * @param f3 the fourth uniform parameter
     * @throws RuntimeException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void setConstant(String name, float f0, float f1, float f2, float f3)
            throws RuntimeException {
        Uniform uniform = getUniform(name);
        if (uniform.location == -1) {
            return;
        }
        if (uniform.values == null) {
            uniform.values = new float[4];
        }
        float[] values = (float[]) uniform.values;
        if (values[0] != f0 || values[1] != f1 || values[2] != f2 || values[3] != f3) {
            values[0] = f0;
            values[1] = f1;
            values[2] = f2;
            values[3] = f3;
            context.getGLContext().uniform4f(uniform.location, f0, f1, f2, f3);
        }
    }

    /**
     * Sets the uniform array variable of the given name with the provided
     * int array values.
     *
     * @param name the name of the uniform variable to be set
     * @param buf the array values to be set
     * @param off the offset into the vals array
     * @param count the number of ivec4 elements in the array
     * @throws RuntimeException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void setConstants(String name, IntBuffer buf, int off, int count)
            throws RuntimeException {
        // TODO: remove off param in favor of IntBuffer.position() (RT-27442)
        int loc = getUniform(name).location;
        if (loc == -1) {
            return;
        }
        context.getGLContext().uniform4iv(loc, count, buf);

    }

    /**
     * Sets the uniform array variable of the given name with the provided
     * float array values.
     *
     * @param name the name of the uniform variable to be set
     * @param buf the array values to be set
     * @param count the number of vec4 elements in the array
     * @param off the offset into the vals array
     * @throws RuntimeException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void setConstants(String name, FloatBuffer buf, int off, int count)
            throws RuntimeException {
        int loc = getUniform(name).location;
        if (loc == -1) {
            return;
        }
        context.getGLContext().uniform4fv(loc, count, buf);
    }

    /**
     * Sets the uniform matrix variable of the given name with the provided
     * float array values.
     *
     * @param name the name of the uniform variable to be set
     * @param buf the matrix values to be set
     * @throws RuntimeException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void setMatrix(String name, float buf[]) throws RuntimeException {
        int loc = getUniform(name).location;
        if (loc == -1) {
            return;
        }

        if (currentMatrix == null) {
            currentMatrix = new float[GLContext.NUM_MATRIX_ELEMENTS];
        }

        if (Arrays.equals(currentMatrix, buf) == false) {
            context.getGLContext().uniformMatrix4fv(loc, false, buf);
            System.arraycopy(buf, 0, currentMatrix, 0, buf.length);
        }
    }

    /**
     * Disposes the native resources used by this program object.
     *
     * @throws RuntimeException if no OpenGL context was current or if any
     * OpenGL-related errors occurred
     */
    public void dispose() throws RuntimeException {
        if (programID != 0) {
            disposerRecord.dispose();
            programID = 0;
        }
        valid = false;
    }

    private static class ES2ShaderDisposerRecord implements Disposer.Record {

        private final ES2Context context;
        private int vertexShaderID;
        private int[] fragmentShaderID;
        private int programID;

        private ES2ShaderDisposerRecord(ES2Context context,
                int vertexShaderID,
                int[] fragmentShaderID,
                int programID) {
            this.context = context;
            this.vertexShaderID = vertexShaderID;
            this.fragmentShaderID = fragmentShaderID;
            this.programID = programID;
        }

        public void dispose() {
            if (programID != 0) {
                context.getGLContext().disposeShaders(programID,
                        vertexShaderID, fragmentShaderID);
                programID = vertexShaderID = 0;
                fragmentShaderID = null;
            }
        }
    }
}
