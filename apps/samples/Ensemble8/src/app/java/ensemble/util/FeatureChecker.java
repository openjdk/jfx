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
package ensemble.util;

import ensemble.PlatformFeatures;
import ensemble.SampleCategory;
import ensemble.SampleInfo;
import java.util.ArrayList;
import java.util.List;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;

public class FeatureChecker {

    public static boolean isSampleSupported(SampleInfo sample) {
        ConditionalFeature[] cf = sample.conditionalFeatures;
        for (ConditionalFeature oneCF : cf) {
            if (!Platform.isSupported(oneCF)) {
                return false;
            }
        }

        if (PlatformFeatures.USE_EMBEDDED_FILTER && !sample.runsOnEmbedded) {
            return false;
        }
        return true;
    }

    public static SampleInfo[] filterSamples(SampleInfo[] samples) {
        if (samples != null) {
            List<SampleInfo> filteredSampleInfos = new ArrayList<>();
            for (SampleInfo oneSampleInfo : samples) {
                if (isSampleSupported(oneSampleInfo)) {
                    filteredSampleInfos.add(oneSampleInfo);
                }
            }
            return filteredSampleInfos.toArray(new SampleInfo[filteredSampleInfos.size()]);
        } else {
            return null;
        }
    }

    public static SampleCategory[] filterEmptyCategories(SampleCategory[] subCategories) {
        if (subCategories != null) {
            List<SampleCategory> filteredSubcategories = new ArrayList<>();
            for (SampleCategory subCategory : subCategories) {
                if (subCategory.samples != null && subCategory.samples.length > 0
                        || subCategory.samplesAll != null && subCategory.samplesAll.length > 0) {
                    filteredSubcategories.add(subCategory);
                }

            }
            return filteredSubcategories.toArray(new SampleCategory[filteredSubcategories.size()]);
        } else {
            return null;
        }
    }
}
