/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
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
package ensemble.compiletime;

import java.util.ArrayList;
import java.util.List;

import static ensemble.compiletime.CodeGenerationUtils.sampleArrayToCode;
import static ensemble.compiletime.CodeGenerationUtils.stringToCode;

/**
 * Descriptor for a category containing samples and sub categories.
 */
public class SampleCategory {
    public final String name;
    public final String ensemblePath;
    public final List<Sample> samples = new ArrayList<Sample>();
    public final List<Sample> samplesAll = new ArrayList<Sample>();
    public final List<SampleCategory> subCategories = new ArrayList<SampleCategory>();
    public final SampleCategory parent;

    public SampleCategory(String name, String ensemblePath, SampleCategory parent) {
        this.name = name;
        this.ensemblePath = ensemblePath;
        this.parent = parent;
    }

    public void addSample(Sample sample) {
        samples.add(sample);
        // find top most category before root
        System.out.println("******** FINDING TOP CATEGORY FOR ["+name+"]");
        SampleCategory topCategory = this;
        System.out.println("            topCategory = "+topCategory.name);
        while (topCategory.parent != null && topCategory.parent.parent != null) {
            topCategory = topCategory.parent;
            System.out.println("            topCategory = "+topCategory.name);
        }
        System.out.println("            FINAL topCategory = "+topCategory.name);
        // add sample to that categories all samples
        topCategory.samplesAll.add(sample);
    }

    public String generateCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("new SampleCategory(");
        sb.append(stringToCode(name)); sb.append(',');
        sb.append(sampleArrayToCode(samples)); sb.append(',');
        sb.append(sampleArrayToCode(samplesAll)); sb.append(',');
        categoryArrayToCode(sb, subCategories);
        sb.append(")");
        return sb.toString();
    }

    private void categoryArrayToCode(StringBuilder sb, List<SampleCategory> array) {
        if (array == null || array.isEmpty()) {
            sb.append("null");
        } else {
            sb.append("new SampleCategory[]{");
            for (SampleCategory category: array) {
                sb.append(category.generateCode());
                sb.append(',');
            }
            sb.append("}");
        }
    }
}
