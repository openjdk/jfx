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

package ensemble;

import ensemble.generated.Samples;
import ensemble.util.FeatureChecker;

/**
 * Descriptor for a category containing samples and sub categories.
 */
public class SampleCategory {
    public final String name;
    /* samples contained in this category directly */
    public final SampleInfo[] samples;
    /* samples contained in this category directly and all sub categories recursively */
    public final SampleInfo[] samplesAll;
    public final SampleCategory[] subCategories;

    public SampleCategory(String name, SampleInfo[] samples, SampleInfo[] samplesAll, SampleCategory[] subCategories) {
        this.name = name;
        this.samples = FeatureChecker.filterSamples(samples);
        this.samplesAll = FeatureChecker.filterSamples(samplesAll);
        this.subCategories = FeatureChecker.filterEmptyCategories(subCategories);
    }

    public SampleInfo sampleForPath(String path) {
        if (path.charAt(0) == '/') { // absolute path
            return Samples.ROOT.sampleForPath(path.split("/"),1);
        } else {
            return sampleForPath(path.split("/"),0);
        }
    }

    private SampleInfo sampleForPath(String[] pathParts, int index) {
        String part = pathParts[index];
        if (samples!=null) for (SampleInfo sample: samples) {
            if (sample.name.equals(part)) return sample;
        }
        if (subCategories!=null) for (SampleCategory category: subCategories) {
            if (category.name.equals(part)) return category.sampleForPath(pathParts, index + 1);
        }
        return null;
    }

    @Override public String toString() {
        return name;
    }
}
