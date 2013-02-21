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

#import <Cocoa/Cocoa.h>
#import <mach-o/dyld.h>
#import <pthread.h>
#import <sys/time.h>
#import <mach/mach.h>

#import "ProcessInfo.h"

#define LSOF_COMMAND "%s -l -F -M -P -n -p %d"

#define UNINITIALIZED 0
#define UNAVAILABLE 1
#define AVAILABLE 2

static void _sortArray(NSMutableArray *array)
{
    NSSet *uniqueElements = [NSSet setWithArray:array];
    [array removeAllObjects];
    [array setArray:[uniqueElements allObjects]];
    
    [array sortUsingComparator:^NSComparisonResult(id obj1, id obj2)
        {
            NSString *string1 = obj1;
            NSString *string2 = obj2;
            return [string1 compare:string2];
        }
     ];
}

static void _printArray(FILE *stream, char *title, NSMutableArray *array)
{
    pthread_yield_np();
    
    fflush(stream);
    fprintf(stream, title, (int)[array count]);
    for (NSUInteger i=0; i<[array count]; i++)
    {
        NSString *string = [array objectAtIndex:i];
        fprintf(stream, "   [%04ld]: %s\n", (long)(i+1), [string UTF8String]);
    }
    fflush(stream);
}

void printLoadedLibraries(FILE *stream)
{
    uint32_t count = _dyld_image_count();
    NSMutableArray *array = [NSMutableArray arrayWithCapacity:count];
    for (uint32_t i=0; i<count; i++)
    {
        [array addObject:[NSString stringWithUTF8String:_dyld_get_image_name(i)]];
    }
    _sortArray(array);
    _printArray(stream, "Loaded %d libraries\n", array);
}

static inline void _collectProcess(void)
{
    pthread_yield_np();
    waitpid(-1, NULL, WNOHANG);
}

static inline char *_runCommand(char *command)
{
    static char *buffer = NULL;
    NSUInteger bufferSize = 0;
    
    fflush(NULL);
    FILE *file = popen(command, "r");
    if (file != NULL)
    {
        char chunk[128];
        int chunkIndex = 0;
        int chunkSize = sizeof(chunk);
        while (fgets(chunk, chunkSize, file))
        {
            size_t chunkLength = strlen(chunk);
            buffer = realloc(buffer, bufferSize+chunkLength);
            memcpy((char*)(buffer+bufferSize), chunk, chunkLength);
            bufferSize += chunkLength;
            
            chunkIndex++;
        }
        
        buffer = realloc(buffer, bufferSize+1);
        buffer[bufferSize] = '\0';
        
        pclose(file);
        
        _collectProcess();
    }
    
    if (bufferSize > 0)
    {
        return buffer;
    }
    else
    {
        return NULL;
    }
}

static inline char *_getCommandPath(char *command)
{
    static char *full = NULL;
    full = realloc(full, strlen("whereis ")+strlen(command)+1);
    strcpy(full, "whereis ");
    strcat(full, command);
    char *path = _runCommand(full);
    if ((path != NULL) && (strlen(path) > 0))
    {
        path[strlen(path)-1] = '\0';
    }
    return path;
}

void printLoadedFiles(FILE *stream)
{
    char *lsof = _getCommandPath("lsof");
    if (lsof != NULL)
    {
        size_t size = strlen(lsof)+strlen(LSOF_COMMAND)+128;
        char *command = malloc(size);
        memset(command, 0x00, size);
        sprintf(command, LSOF_COMMAND, lsof, getpid());
        
        NSMutableArray *array = [NSMutableArray arrayWithCapacity:100];
        char *output = _runCommand(command);
        if (output != NULL)
        {
            char *token = strtok(output, "\n");
            output += strlen(token)+1;
            token = strtok(output, "\n");
            
            if (output != NULL)
            {
                while (token != NULL)
                {
                    if ((token[0] == 'n') && (token[1] == '/'))
                    {
                        int startIndex = 1;
                        int endIndex = startIndex;
                        while (token[endIndex] != '\0')
                        {
                            endIndex++;
                        }
                        
                        [array addObject:[NSString stringWithUTF8String: &token[startIndex]]];
                    }
                    output += strlen(token)+1;
                    token = strtok(output, "\n");
                }
            }
        }
        _sortArray(array);
        _printArray(stream, "Loaded %d files\n", array);
    }
}

int64_t getTimeMicroseconds(void)
{
        struct timeval now;
        gettimeofday(&now, NULL);
    
    int64_t seconds = now.tv_sec;
    seconds *= 1000000L;
    int64_t useconds = now.tv_usec;
    
        return (seconds+useconds);
}

vm_size_t getRam(void)
{
    struct task_basic_info info;
    memset(&info, 0x00, sizeof(struct task_basic_info));
    mach_msg_type_number_t info_count = TASK_BASIC_INFO_COUNT;
    kern_return_t kerr = task_info(mach_task_self(), TASK_BASIC_INFO, (task_info_t)&info, &info_count);
    if (kerr == KERN_SUCCESS)
    {
        return info.resident_size;
    } 
    else 
    {
        return 0;
    }
}
