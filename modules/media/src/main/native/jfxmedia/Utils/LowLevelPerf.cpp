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

#include "LowLevelPerf.h"

#if ENABLE_LOWLEVELPERF

#if TARGET_OS_WIN32
#include <Windows.h>
#else
#include <time.h>
#endif

CLowLevelPerf::LSingleton CLowLevelPerf::s_Singleton;

#if TARGET_OS_WIN32
#pragma warning (disable : 4996)
#endif

CLowLevelPerf::~CLowLevelPerf()
{
    map<string, sValue>::iterator it;
    FILE *pFile = NULL;

    printf("\"name\",\"min\",\"max\",\"avg\",\"samples\",\"units\"\n");
    for (it = m_Values.begin() ; it != m_Values.end(); it++)
    {
        if (it->second.lDenominator == 1)
            printf("\"%s\",\"%d\",\"%d\",\"%.2f\",\"%d\",\"%s\"\n", it->first.c_str(), it->second.lMinValue, it->second.lMaxValue, (it->second.llTotalValue / (double)it->second.lSamples), it->second.lSamples, it->second.units.c_str());
        else if  (it->second.lDenominator > 1)
            printf("\"%s\",\"%.2f\",\"%.2f\",\"%.2f\",\"%d\",\"%s\"\n", it->first.c_str(), ((double)it->second.lMinValue / (double)it->second.lDenominator), ((double)it->second.lMaxValue / (double)it->second.lDenominator), ((it->second.llTotalValue / (double)it->second.lSamples) / (double)it->second.lDenominator), it->second.lSamples, it->second.units.c_str());
    }

    // Find available file name for log.
    // We want to create log files as output.1.csv, output.2.csv, etc...
    // So test system can match logs to test files
    char filenamecsv[1024];
    char filenametwiki[1024];
    for (int index = 1; index < 100; index++) // Upto 100 tests allowed
    {
        FILE *pFileCSV = NULL;
        FILE *pFileTWIKI = NULL;
        sprintf(filenamecsv, "output.%d.csv", index);
        sprintf(filenametwiki, "output.%d.twiki", index);
        pFileCSV = fopen(filenamecsv, "r");
        pFileTWIKI = fopen(filenametwiki, "r");
        if (pFileCSV != NULL && pFileTWIKI != NULL)
        {
            fclose(pFileCSV);
            fclose(pFileTWIKI);
        }
        else
        {
            break;
        }
    }

    pFile = fopen(filenamecsv, "w");
    if (pFile)
    {
        fprintf(pFile, "\"name\",\"min\",\"max\",\"avg\",\"samples\",\"units\"\n");
        for (it = m_Values.begin() ; it != m_Values.end(); it++)
        {
            if (it->second.lDenominator == 1)
                fprintf(pFile, "\"%s\",\"%d\",\"%d\",\"%.2f\",\"%d\",\"%s\"\n", it->first.c_str(), it->second.lMinValue, it->second.lMaxValue, (it->second.llTotalValue / (double)it->second.lSamples), it->second.lSamples, it->second.units.c_str());
            else if  (it->second.lDenominator > 1)
                fprintf(pFile, "\"%s\",\"%.2f\",\"%.2f\",\"%.2f\",\"%d\",\"%s\"\n", it->first.c_str(), ((double)it->second.lMinValue / (double)it->second.lDenominator), ((double)it->second.lMaxValue / (double)it->second.lDenominator), ((it->second.llTotalValue / (double)it->second.lSamples) / (double)it->second.lDenominator), it->second.lSamples, it->second.units.c_str());
        }
        fclose(pFile);
    }

    pFile = fopen(filenametwiki, "w");
    if (pFile)
    {
        fprintf(pFile, "| *Version* | *Build #* | *Date* | *Test Name* |\n");
        fprintf(pFile, "| TODO | TODO | TODO | TODO |\n\n");
        fprintf(pFile, "| *Name* | *Min* | *Max* | *Avg* | *Samples* | *Units* |\n");
        for (it = m_Values.begin() ; it != m_Values.end(); it++)
        {
            if (it->second.lDenominator == 1)
                fprintf(pFile, "| !%s | %d | %d | %.2f | %d | %s |\n", it->first.c_str(), it->second.lMinValue, it->second.lMaxValue, (it->second.llTotalValue / (double)it->second.lSamples), it->second.lSamples, it->second.units.c_str());
            else if  (it->second.lDenominator > 1)
                fprintf(pFile, "| !%s | %.2f | %.2f | %.2f | %d | %s |\n", it->first.c_str(), ((double)it->second.lMinValue / (double)it->second.lDenominator), ((double)it->second.lMaxValue / (double)it->second.lDenominator), ((it->second.llTotalValue / (double)it->second.lSamples) / (double)it->second.lDenominator), it->second.lSamples, it->second.units.c_str());
        }
        fclose(pFile);
    }

    m_Values.clear();
    m_ExecTimeValues.clear();
    m_CounterValues.clear();
    m_ResetCounterValues.clear();
}

#if TARGET_OS_WIN32
#pragma warning (default : 4996)
#endif

void CLowLevelPerf::ExecTimeStart(char* name)
{
    map<string, sExecTime>::iterator it;

    // Get counter data store
    it = m_ExecTimeValues.find(name);

    // If does not exist then create new one
    if (it == m_ExecTimeValues.end())
    {
        sExecTime time;

        time.lStartTime = 0;

        m_ExecTimeValues.insert(pair<string, sExecTime>(string(name), time));

        // Get it again
        it = m_ExecTimeValues.find(name);
    }

    // Get start time
    it->second.lStartTime = GetTime();
}

void CLowLevelPerf::ExecTimeStop(char* name)
{
    map<string, sExecTime>::iterator it;

    long lDuration = 0;
    long lStopTime = 0;

    // Get stop time
    lStopTime = GetTime();

    // Find counter data store to get start time
    // If not found do nothing
    it = m_ExecTimeValues.find(name);
    if (it != m_ExecTimeValues.end())
    {
        if (it->second.lStartTime != -1)
        {
            // Calculate duration and log it
            lDuration = lStopTime - it->second.lStartTime;
            it->second.lStartTime = -1;
            LogValue(name, lDuration, LOWLEVELPERF_EXECTIMEUNITS, 1);
        }
    }
}

void CLowLevelPerf::Counter(char* name, long value, long denominator)
{
    map<string, sCounter>::iterator it;

    // Get counter data store
    it = m_CounterValues.find(name);

    // If does not exist then create new one
    if (it == m_CounterValues.end())
    {
        sCounter counter;

        counter.lCounter = 0;
        counter.lStartTime = -1;

        m_CounterValues.insert(pair<string, sCounter>(string(name), counter));

        // Get it again
        it = m_CounterValues.find(name);
    }

    // Get start time so we can calculate when we need to log
    if (it->second.lStartTime == -1)
    {
        it->second.lStartTime = GetTime();
    }

    // Update counter
    it->second.lCounter += value;

    // Check if we need to log value
    if ((GetTime() - it->second.lStartTime) >= LOWLEVELPERF_COUNTERTIMEPERIOD)
    {
        it->second.lStartTime = -1;
        LogValue(name, it->second.lCounter, LOWLEVELPERF_COUNTERUNITS, denominator);
    }
}

void CLowLevelPerf::ResetCounter(char* name)
{
    map<string, sResetCounter>::iterator it;

    // Get counter data store
    it = m_ResetCounterValues.find(name);

    // If does not exist then create new one
    if (it == m_ResetCounterValues.end())
    {
        sResetCounter counter;

        counter.lCounter = 0;
        counter.lStartTime = -1;

        m_ResetCounterValues.insert(pair<string, sResetCounter>(string(name), counter));

        it = m_ResetCounterValues.find(name);
    }

    // Get start time so we can calculate when we need to log and reset
    if (it->second.lStartTime == -1)
    {
        it->second.lStartTime = GetTime();
    }

    // Increment counter
    it->second.lCounter++;

    // Check if need to log and reset
    if ((GetTime() - it->second.lStartTime) >= LOWLEVELPERF_COUNTERTIMEPERIOD)
    {
        it->second.lStartTime = -1;
        LogValue(name, it->second.lCounter, LOWLEVELPERF_RESETCOUNTERUNITS, 1);
        it->second.lCounter = 0; // Reset counter
    }
}

void CLowLevelPerf::LogValue(char* name, long value, char *units, long denominator)
{
    map<string, sValue>::iterator it;

    it = m_Values.find(name);

    if (it == m_Values.end())
    {
        sValue v;

        v.lMinValue = value;
        v.lMaxValue = value;
        v.llTotalValue = value;
        v.lSamples = 1;
        v.lDenominator = denominator;
        v.units.assign(units);

        m_Values.insert(pair<string, sValue>(string(name), v));
    }
    else
    {
        if (it->second.lMinValue > value)
            it->second.lMinValue = value;

        if (it->second.lMaxValue < value)
            it->second.lMaxValue = value;

        it->second.llTotalValue += value;
        it->second.lSamples++;
    }
}

inline long CLowLevelPerf::GetTime()
{
#if TARGET_OS_WIN32
    return timeGetTime();
#else // TARGET_OS_WIN32
    return (long)clock();
#endif // TARGET_OS_WIN32
}

uint32_t CLowLevelPerf::CreateInstance(CLowLevelPerf **ppLowLevelPerf)
{
    if (ppLowLevelPerf == NULL)
    {
        return ERROR_FUNCTION_PARAM_NULL;
    }

    *ppLowLevelPerf = new CLowLevelPerf();
    if (*ppLowLevelPerf == NULL)
    {
        return ERROR_MEMORY_ALLOCATION;
    }

    return ERROR_NONE;
}
#endif // ENABLE_LOWLEVELPERF
