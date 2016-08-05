/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _LOWLEVELPERF_H_
#define _LOWLEVELPERF_H_

#include <Common/ProductFlags.h>

#if ENABLE_LOWLEVELPERF

#include <map>
#include <string>
#include <stdio.h>

using namespace std;

#include <Utils/Singleton.h>

struct sValue
{
    long lMinValue;
    long lMaxValue;
    long long llTotalValue;
    long lSamples;
    long lDenominator;
    string units;
};

struct sExecTime
{
    long lStartTime;
};

struct sCounter
{
    long lCounter;
    long lStartTime;
};

struct sResetCounter
{
    long lCounter;
    long lStartTime;
};

// Macros to log execution time
// These macros can be called from any locations (meaning not only within one function), but call to START should match call to STOP.
// Example: measure execution time of particular function or
// Example: measure execution time between events.
#define LOWLEVELPERF_EXECTIMEUNITS "ms"
#define LOWLEVELPERF_EXECTIMESTART(n)  { CLowLevelPerf *pLowLevelPerf = NULL; CLowLevelPerf::s_Singleton.GetInstance(&pLowLevelPerf); if (pLowLevelPerf) pLowLevelPerf->ExecTimeStart(n); }
#define LOWLEVELPERF_EXECTIMESTOP(n)  { CLowLevelPerf *pLowLevelPerf = NULL; CLowLevelPerf::s_Singleton.GetInstance(&pLowLevelPerf); if (pLowLevelPerf) pLowLevelPerf->ExecTimeStop(n); }

// If modified adjust UNITS appropriately
#define LOWLEVELPERF_COUNTERTIMEPERIOD 1000 // 1 second

// Macros to log outstanding items with value logging per time period
// Call COUNTERINC when item created/released and call COUNTERDEC when item disposed.
// Counter value will be logged every COUNTERTIMEPERIOD.
// Example: measure number of allocated video frames during playback.
#define LOWLEVELPERF_COUNTERUNITS "items each sec"
#define LOWLEVELPERF_COUNTERINC(n, v, d)  { CLowLevelPerf *pLowLevelPerf = NULL; CLowLevelPerf::s_Singleton.GetInstance(&pLowLevelPerf); if (pLowLevelPerf) pLowLevelPerf->Counter(n, v, d); }
#define LOWLEVELPERF_COUNTERDEC(n, v, d)  { CLowLevelPerf *pLowLevelPerf = NULL; CLowLevelPerf::s_Singleton.GetInstance(&pLowLevelPerf); if (pLowLevelPerf) pLowLevelPerf->Counter(n, -v, d); }

// Macros to log number of items per time period
// When RESETCOUNTER is called internal counter will be incremented and after COUNTERTIMEPERIOD elapsed its' value
// will be logged and counter will be reseted to 0.
// Example: measure frame rate.
#define LOWLEVELPERF_RESETCOUNTERUNITS "items/sec"
#define LOWLEVELPERF_RESETCOUNTER(n)  { CLowLevelPerf *pLowLevelPerf = NULL; CLowLevelPerf::s_Singleton.GetInstance(&pLowLevelPerf); if (pLowLevelPerf) pLowLevelPerf->ResetCounter(n); }

// Macro to log a time series
// Example: quality of service (QoS) proportion.
#define LOWLEVELPERF_LOGVALUE(n, v, u, d) { CLowLevelPerf *pLowLevelPerf = NULL; CLowLevelPerf::s_Singleton.GetInstance(&pLowLevelPerf); if (pLowLevelPerf) pLowLevelPerf->LogValue(n, v, u, d); }

class CLowLevelPerf
{
public:
   ~CLowLevelPerf();

  void ExecTimeStart(char* name);
  void ExecTimeStop(char* name);

  void Counter(char* name, long value, long denominator);

  void ResetCounter(char* name);

  void LogValue(char* name, long value, char *units, long denominator);

private:
    long GetTime();

public:
  typedef Singleton<CLowLevelPerf> LSingleton;
  friend class Singleton<CLowLevelPerf>;
  static LSingleton s_Singleton;
  static uint32_t CreateInstance(CLowLevelPerf **ppLowLevelPerf);

private:
    map<string, sValue> m_Values;
    map<string, sExecTime> m_ExecTimeValues;
    map<string, sCounter> m_CounterValues;
    map<string, sResetCounter> m_ResetCounterValues;
};

#else // ENABLE_LOWLEVELPERF

#define LOWLEVELPERF_EXECTIMESTART(n)
#define LOWLEVELPERF_EXECTIMESTOP(n)
#define LOWLEVELPERF_COUNTERINC(n, v, d)
#define LOWLEVELPERF_COUNTERDEC(n, v, d)
#define LOWLEVELPERF_RESETCOUNTER(n)
#define LOWLEVELPERF_LOGVALUE(n, v, u, d)

#endif // ENABLE_LOWLEVELPERF

#endif // _LOWLEVELPERF_H_
