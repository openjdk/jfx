/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include <PiscesMath.h>

#include <PiscesSysutils.h>

#define PISCES_SINTAB_LG_ENTRIES 10
#define PISCES_SINTAB_ENTRIES (1 << PISCES_SINTAB_LG_ENTRIES)

static jint* sintab = NULL;

jboolean
piscesmath_moduleInitialize() {
    if (sintab == NULL) {
        jint i;
        sintab = (jint*)PISCESmalloc((PISCES_SINTAB_ENTRIES + 1)
                 * sizeof(jint));
        if (sintab == NULL) {
            return XNI_FALSE;
        }
        for (i = 0; i < PISCES_SINTAB_ENTRIES + 1; i++) {
            double theta = i*(PI_DOUBLE/2.0)/PISCES_SINTAB_ENTRIES;
            sintab[i] = (jint)(PISCESsin(theta)*65536.0);
        }
    }

    return XNI_TRUE;
}

void
piscesmath_moduleFinalize() {
    PISCESfree(sintab);
    sintab = NULL;
}

jint
piscesmath_sin(jint theta) {
    jint sign = 1;
    jint itheta;
    if (theta < 0) {
        theta = -theta;
        sign = -1;
    }
    // 0 <= theta
    while (theta >= PISCES_TWO_PI) {
        theta -= PISCES_TWO_PI;
    }
    // 0 <= theta < 2*PI
    if (theta >= PISCES_PI) {
        theta = PISCES_TWO_PI - theta;
        sign = -sign;
    }
    // 0 <= theta < PI
    if (theta > PISCES_PI_OVER_TWO) {
        theta = PISCES_PI - theta;
    }
    // 0 <= theta <= PI/2
    itheta = (jint)((jlong)theta*PISCES_SINTAB_ENTRIES/(PISCES_PI_OVER_TWO));

    return sign*sintab[itheta];
}

jint
piscesmath_cos(jint theta) {
    return piscesmath_sin(PISCES_PI_OVER_TWO - theta);
}

jdouble
piscesmath_dhypot(jdouble x, jdouble y) {
    return PISCESsqrt(x*x + y*y);
}

jint piscesmath_toRadians(jint thetaDegrees) {
    return (jint)(((jlong)(thetaDegrees % PISCES_360_DEGREES) * (jlong) PISCES_DEGREES_TO_RADIANS_MULTIPLIER) >> 16);
}

jint piscesmath_toDegrees(jint thetaRadians) {
    return (jint)(((jlong)(thetaRadians % PISCES_TWO_PI) * (jlong) PISCES_RADIANS_TO_DEGREES_MULTIPLIER) >> 16);
}

jint piscesmath_abs(jint x) {
    return (x >= 0) ? x: (-x);
}

float piscesmath_acos(float val) {
    return ((float)PI_DOUBLE / 2.0f) - piscesmath_asin(val);
}

jint piscesmath_ceil(float x) {
    float dx, sign = 1;

    if (x < 0) {
        x = -x;
        sign = -1;
    }

    dx = x - (jint)x;
    if (dx > 0) {
        return (jint)(x+1);
    }
    return (jint)x;

}

float piscesmath_btan(float increment) {
    float increment2 = increment / 2.0f;
    return (float)((4.0f / 3.0f)* PISCESsin(increment2) / (1.0f + PISCEScos(increment2)));
}


    /**
     * Pregenerated asin values from 0 -> 1.
     * First value == asin(0.0), second asin(0.005), ... step 0.005
     * last value == asin(1.0)
     */
    static double arcsinTable[] = {
        0.0,
        0.005000020833567712,
        0.010000166674167114,
        0.015000562556960754,
        0.02000133357339049,
        0.02500260489936114,
        0.030004501823476935,
        0.03500714977534865,
        0.040010674353988925,
        0.045015201356314066,
        0.050020856805770016,
        0.05502776698110088,
        0.06003605844527842,
        0.0650458580746109,
        0.07005729308805025,
        0.07507049107671654,
        0.08008558003365901,
        0.08510268838387339,
        0.09012194501459525,
        0.09514347930589002,
        0.1001674211615598,
        0.10519390104038849,
        0.11022304998774664,
        0.1152549996675776,
        0.12028988239478806,
        0.1253278311680654,
        0.1303689797031455,
        0.13541346246655556,
        0.14046141470985582,
        0.1455129725044066,
        0.15056827277668602,
        0.15562745334418546,
        0.1606906529519106,
        0.16575801130951626,
        0.17082966912910452,
        0.1759057681637163,
        0.1809864512465477,
        0.1860718623309233,
        0.19116214653105962,
        0.19625745016365348,
        0.2013579207903308,
        0.20646370726099242,
        0.2115749597580956,
        0.21669182984191085,
        0.22181447049679442,
        0.22694303617851996,
        0.23207768286271319,
        0.2372185680944353,
        0.24236585103896321,
        0.24751969253381592,
        0.25268025514207865,
        0.2578477032070788,
        0.2630222029084689,
        0.26820392231977536,
        0.27339303146747324,
        0.2785897023916506,
        0.28379410920832787,
        0.28900642817350136,
        0.29422683774898245,
        0.2994555186701077,
        0.3046926540153975,
        0.30993842927824544,
        0.31519303244072444,
        0.3204566540495979,
        0.32572948729463014,
        0.3310117280892945,
        0.33630357515398035,
        0.3416052301018077,
        0.34691689752716176,
        0.3522387850970648,
        0.3575711036455103,
        0.3629140672708885,
        0.36826789343663996,
        0.373632803075281,
        0.3790090206959508,
        0.3843967744956391,
        0.38979629647426056,
        0.3952078225537514,
        0.40063159270137194,
        0.4060678510574098,
        0.41151684606748806,
        0.4169788306196941,
        0.4224540621867558,
        0.42794280297350573,
        0.43344532006988595,
        0.4389618856097607,
        0.444492776935819,
        0.45003827677086705,
        0.4555986733958234,
        0.46117426083475366,
        0.4667653390472964,
        0.4723722141288549,
        0.4779951985189524,
        0.48363461121817014,
        0.48929077801411575,
        0.4949640317168946,
        0.5006547124045881,
        0.5063631676792726,
        0.5120897529341477,
        0.5178348316323792,
        0.5235987755982989,
        0.5293819653216489,
        0.5351847902755998,
        0.5410076492493221,
        0.5468509506959441,
        0.5527151130967832,
        0.5586005653428008,
        0.5645077471342955,
        0.570437109399922,
        0.5763891147361973,
        0.5823642378687435,
        0.5883629661366032,
        0.5943858000010622,
        0.6004332535805235,
        0.6065058552130871,
        0.6126041480486225,
        0.6187286906722511,
        0.6248800577613086,
        0.6310588407780213,
        0.6372656487003039,
        0.6435011087932844,
        0.6497658674243729,
        0.6560605909249226,
        0.662385966501789,
        0.6687427032023717,
        0.6751315329370317,
        0.6815532115631169,
        0.6880085200352017,
        0.694498265626556,
        0.7010232832273195,
        0.7075844367253556,
        0.7141826204763189,
        0.7208187608700897,
        0.727493818001415,
        0.7342087874533589,
        0.74096470220302,
        0.7477626346599207,
        0.7546036988485377,
        0.7614890527476333,
        0.7684199008003771,
        0.7753974966107532,
        0.782423145843429,
        0.7894982093461719,
        0.7966241065160413,
        0.80380231893303,
        0.8110343942875815,
        0.8183219506315598,
        0.8256666809858231,
        0.8330703583416478,
        0.8405348410979318,
        0.848062078981481,
        0.855654119503876,
        0.8633131150155536,
        0.8710413304260044,
        0.8788411516685797,
        0.8867150949995675,
        0.8946658172342352,
        0.9026961270378197,
        0.9108089974073983,
        0.9190075795017759,
        0.9272952180016123,
        0.935675468211854,
        0.9441521151541561,
        0.9527291949396819,
        0.9614110187641017,
        0.9702021999288457,
        0.9791076843683528,
        0.9881327852555816,
        0.9972832223717998,
        1.0065651670673148,
        1.015985293814825,
        1.0255508395762682,
        1.0352696724805088,
        1.045150371660533,
        1.0552023205488061,
        1.0654358165107394,
        1.075862200454001,
        1.0864940110489767,
        1.0973451695228305,
        1.1084312027754692,
        1.1197695149986342,
        1.1313797213386234,
        1.143284061850027,
        1.1555079206898065,
        1.1680804852142352,
        1.181035593997422,
        1.1944128444771684,
        1.2082590645069227,
        1.2226303055219359,
        1.2375946027743465,
        1.2532358975033755,
        1.2696597812415247,
        1.2870022175865685,
        1.3054433771972465,
        1.3252308092796046,
        1.3467210414930773,
        1.3704614844717768,
        1.397374005699292,
        1.4292568534704693,
        1.4707546131833564,
        1.5707963267948966
    };

static jfloat findClosestASin(jfloat x) {
    double lowerValue;
    double upperValue;
    double rest, retVal;
    int index, sign = 1;
    double arg = x;
    if (x < 0.0) {
        sign = -1;
        arg = -x;
    }
    index = (int) (arg * 200);


    //we don't need to take care about situation when index == 200 as this
    //case is catched in asin(x==1.0) itself
    if (index == 200) {
        index = 199;
    }

    lowerValue = arcsinTable[index];
    upperValue = arcsinTable[index + 1];

    rest = (arg) - index * 0.005;

    retVal = lowerValue + rest * (upperValue - lowerValue) / (0.005);

    return (jfloat) (sign * retVal);
}

jfloat piscesmath_asin(jfloat arg) {
    if (arg == 0) return 0;
    if (arg == -1.0f) return -1.5707963267948966f;
    if (arg == 1.0f) return 1.5707963267948966f;

    return findClosestASin(arg);
}

jfloat piscesmath_mod(jfloat x, int y) {
    float b = x;
    float sign = 1;
    if (x < 0) {
        sign = -1;
        b = -x;
    }
    while(b > y) {
        b -= (float)y;
    }
    if (b == y) return 0.0f;
    return b * sign;
}
