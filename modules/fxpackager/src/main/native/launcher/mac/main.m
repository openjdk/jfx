/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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
#include <dlfcn.h>
#include <pthread.h>
//#include <jni.h>

//essential imports from jni.h
#define JNICALL
typedef unsigned char   jboolean;
#if defined(__LP64__) && __LP64__ /* for -Wundef */
typedef int jint;
#else
typedef long jint;
#endif
////////////////////////// end of imports from jni.h


#define JAVA_LAUNCH_ERROR "JavaLaunchError"

#define JVM_RUNTIME_KEY "JVMRuntime"
#define JVM_MAIN_CLASS_NAME_KEY "JVMMainClassName"
#define JVM_MAIN_JAR_NAME_KEY "JVMMainJarName"
#define JVM_OPTIONS_KEY "JVMOptions"
#define JVM_ARGUMENTS_KEY "JVMArguments"
#define JVM_USER_OPTIONS_KEY "JVMUserOptions"
#define JVM_CLASSPATH_KEY "JVMAppClasspath"
#define JVM_PREFERENCES_ID "JVMPreferencesID"
#define JVM_LAUNCHER_DEBUG_OPTIONS_KEY "JVMLauncherDebugOptions"
#define DEFAULT_JAVA_PREFS_DOMAIN "com.apple.java.util.prefs"

#define LIBJLI_DYLIB "/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/lib/jli/libjli.dylib"

typedef int (JNICALL *JLI_Launch_t)(int argc, char ** argv,
        int jargc, const char** jargv,
        int appclassc, const char** appclassv,
        const char* fullversion,
        const char* dotversion,
        const char* pname,
        const char* lname,
        jboolean javaargs,
        jboolean cpwildcard,
        jboolean javaw,
        jint ergo);

int launch(int appArgc, char *appArgv[]);

NSArray *getJVMOptions(NSDictionary *infoDictionary, NSString *mainBundlePath);
void logCommandLine(NSString *tag, int argc, char* argv[]);

int main(int argc, char *argv[]) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    int result;
    @try {
        launch(argc, argv);
        result = 0;
    } @catch (NSException *exception) {
        NSLog(@"%@: %@", exception, [exception callStackSymbols]);
        result = 1;
    }

    [pool drain];

    return result;
}

int launch(int appArgc, char *appArgv[]) {
    // Get the main bundle
    NSBundle *mainBundle = [NSBundle mainBundle];

    char *commandName = appArgv[0];

    // Set the working directory to the user's home directory
    chdir([NSHomeDirectory() UTF8String]);

    // Get the main bundle's info dictionary
    NSDictionary *infoDictionary = [mainBundle infoDictionary];

    logCommandLine(@"launch", appArgc, appArgv);

    // Locate the JLI_Launch() function
    NSString *runtime = [infoDictionary objectForKey:@JVM_RUNTIME_KEY];

    JLI_Launch_t jli_LaunchFxnPtr = NULL;
    if ([runtime length] != 0) { //missing key or empty value
        NSString *runtimePath = [[[NSBundle mainBundle] builtInPlugInsPath] stringByAppendingPathComponent:runtime];
        NSString *libjliPath = [runtimePath stringByAppendingPathComponent:@"Contents/Home/jre/lib/jli/libjli.dylib"];

        if ([[NSFileManager defaultManager] fileExistsAtPath:libjliPath]) {
            const char *jliPath = [libjliPath fileSystemRepresentation];
            void *libJLI = dlopen(jliPath, RTLD_LAZY);
            if (libJLI != NULL) {
                jli_LaunchFxnPtr = dlsym(libJLI, "JLI_Launch");
            }
        }
    } else {
        void *libJLI = dlopen(LIBJLI_DYLIB, RTLD_LAZY);
        if (libJLI != NULL) {
            jli_LaunchFxnPtr = dlsym(libJLI, "JLI_Launch");
        }
    }

    if (jli_LaunchFxnPtr == NULL) {
        [NSException raise:@JAVA_LAUNCH_ERROR format:@"Could not get function pointer for JLI_Launch."];
    }

    // Get the main class name
    NSString *mainClassName = [infoDictionary objectForKey:@JVM_MAIN_CLASS_NAME_KEY];
    if (mainClassName == nil) {
        [NSException raise:@JAVA_LAUNCH_ERROR format:@"%@ is required.", @JVM_MAIN_CLASS_NAME_KEY];
    }

    // Get the main jar name
    NSString *mainJarName = [infoDictionary objectForKey:@JVM_MAIN_JAR_NAME_KEY];
    if (mainJarName == nil) {
        [NSException raise:@JAVA_LAUNCH_ERROR format:@"%@ is required.", @JVM_MAIN_JAR_NAME_KEY];
    }

    // Set the class path
    // Assume we are given main executable jar file that knows how to set classpath
    //  and launch the app (i.e. it works for doubleclick on jar)
    NSString *mainBundlePath = [mainBundle bundlePath];
    NSString *javaPath = [mainBundlePath stringByAppendingString:@"/Contents/Java"];
    NSMutableString *classPath = [NSMutableString stringWithFormat:@"-Djava.class.path=%@/%@",
                                                                   javaPath, mainJarName];

    NSString *extraClasspath = [infoDictionary objectForKey:@JVM_CLASSPATH_KEY];
    if ([extraClasspath length] > 0) { //unless key missing or has empty value
        NSArray *elements = [extraClasspath componentsSeparatedByString:@" "];
        for (NSString *file in elements) {
            if ([file length] > 0) {
                [classPath appendFormat:@":%@/%@", javaPath, file];
            }
        }
    }
    // Set the library path
    NSString *libraryPath = [NSString stringWithFormat:@"-Djava.library.path=%@/Contents/Java", mainBundlePath];

    NSArray *options= getJVMOptions(infoDictionary, mainBundlePath);

    // Get the application arguments
    NSArray *arguments = [infoDictionary objectForKey:@JVM_ARGUMENTS_KEY];
    if (arguments == nil) {
        arguments = [NSArray array];
    }

    // Initialize the arguments to JLI_Launch()
    //
    // On Mac OS X we spawn a new thread that actually starts the JVM. This
    // new thread simply re-runs main(argc, argv). Therefore we do not want
    // to add new args if we are still in the original main thread so we
    // will treat them as command line args provided by the user ...
    // Only propagate original set of args first time
    int mainThread = (pthread_main_np() == 1);
    int argc;
    if (!mainThread) {
        argc = 1 + [options count] + 2 + 1 +
                (appArgc > 1 ? (appArgc - 1) : [arguments count]);
    } else {
        argc = 1 + (appArgc > 1 ? (appArgc - 1) : 0);
    }

    // argv[argc] == NULL by convention, so allow one extra space
    // for the null termination.
    char *argv[argc + 1];

    int i = 0;
    argv[i++] = strdup(commandName);

    if (!mainThread) {
        argv[i++] = strdup([classPath UTF8String]);
        argv[i++] = strdup([libraryPath UTF8String]);

        for (NSString *option in options) {
            argv[i++] = strdup([option UTF8String]);
        }

        argv[i++] = strdup([mainClassName UTF8String]);

        //command line arguments override plist
        if (appArgc > 1) {
            for (int j=1; j<appArgc; j++) {
                //PSN already filtered out on second time through
                argv[i++] = strdup(appArgv[j]);
            }
        } else {
            for (NSString *argument in arguments) {
                argv[i++] = strdup([argument UTF8String]);
            }
        }
    } else {
        for (int j=1; j < appArgc; j++) {
            //Mac adds a ProcessSerialNumber to args when launched from .app
            //filter out the psn since they it's not expected in the app
            if (strncmp("-psn_", appArgv[j], 5) != 0) {
                argv[i++] = strdup(appArgv[j]);
            }
            else {
                argc--;
            }
        }
    }
    [options release];

    argv[i] = NULL;

    logCommandLine(@"jli_LaunchFxnPtr", argc, argv);

    // Invoke JLI_Launch()
    return jli_LaunchFxnPtr(argc, argv,
            0, NULL,
            0, NULL,
            "",
            "",
            "java",
            "java",
            FALSE,
            FALSE,
            FALSE,
            0);
}

//Call every time before to get debug options
NSSet *getDebugOptions() {
    static NSSet *debugOptions;
    if (debugOptions == nil) {
        NSBundle *mainBundle = [NSBundle mainBundle];
        // Get the main bundle's info dictionary
        NSDictionary *infoDictionary = [mainBundle infoDictionary];
        NSArray *options = [infoDictionary objectForKey:@JVM_LAUNCHER_DEBUG_OPTIONS_KEY];
        if (options == nil) {
            options = [NSArray array];
        }
        debugOptions = [NSSet setWithArray: options];
    }
    return debugOptions;
}

//Print command line args
void logCommandLine(NSString *tag, int argc, char* argv[]) {
    if ([getDebugOptions() containsObject:@"log.args"]) {
        for(int i=0;i<argc;i++) {
            NSLog(@"%@ (%i, %s)", tag, i, argv[i]);
        }
    }
}



/**
* This gets the JVMOptions, both the internal developer only options and set of options the developer
* wants a user to be able to override.
*
* The developer would set the options they required, for instance:

            <fx:platform>
              <fx:jvmarg value="-verbose:class"/>
              <fx:jvmarg value="-Djava.policy.file=$APPDIR/app/whatever.policy"/>
              <fx:jvmuserarg name="-Xmx" value="768m" />
              <fx:jvmuserarg name="-Djava.util.logging.config.file=" value="~/logging.properties" />
            </fx:platform>

* this will result in Info.plist having (default)

  <key>JVMOptions</key>
  <array>
    <string>-verbose:class</string>
    <string>-Djava.policy.file=$APPDIR/app/whatever.policy</string>
  </array>
  <key>JVMUserOptions</key>
    <dict>
      <key>-Djava.util.logging.config.file=</key>
      <string>~/logging.properties</string>
      <key>-Xmx</key>
      <string>768m</string>
    </dict>

* and initially set up the applications preference file in ~/Library/Preferences/..name based on bundleid..plist,
* i.e. just the JVMUserOptions

	<key>JVMUserOptions</key>
	<dict>
		<key>-Djava.util.logging.config.file=</key>
		<string>~/logging.properties</string>
		<key>-Xmx</key>
		<string>860m</string>
	</dict>

*/
NSArray *getJVMOptions(NSDictionary *infoDictionary, NSString *mainBundlePath) {
    NSArray *options = [infoDictionary objectForKey:@JVM_OPTIONS_KEY];
    NSDictionary *defaultOverrides = [infoDictionary objectForKey:@JVM_USER_OPTIONS_KEY];

    if (options == nil) {
        options = [NSArray array];
    }


    //Do string substitutions - for now only one is $APPDIR, if a second one is added this will
    //be generalized and use a set of options
    NSString *contentsPath = [mainBundlePath stringByAppendingString:@"/Contents"];

    //Create some extra room for user options and preferences id
    NSMutableArray *expandedOptions = [[NSMutableArray alloc] initWithCapacity:(
            [options count] + [defaultOverrides count] + 5)];

    //Add preferences ID
    NSString *preferencesID = [infoDictionary objectForKey:@JVM_PREFERENCES_ID];
    if (preferencesID != nil) {
        [expandedOptions addObject: [@"-Dapp.preferences.id=" stringByAppendingString: preferencesID]];
    }

    for (id option in options) {
        NSString *expandedOption =
                [option stringByReplacingOccurrencesOfString:@"$APPDIR" withString:contentsPath];
        [expandedOptions addObject:expandedOption];
    }


    // calculate a normalized path including the JVMUserOptions key
    BOOL leadingSlash = [preferencesID hasPrefix: @"/"];
    BOOL trailingSlash = [preferencesID hasSuffix: @"/"] || ([preferencesID length] == 0);
    
    NSString *fullPath = [NSString stringWithFormat:@"%@%@%@%@/", leadingSlash?@"":@"/",
                          preferencesID, trailingSlash?@"":@"/", @JVM_USER_OPTIONS_KEY];
    
    // now pull out the parts...
    NSString *strippedPath = [fullPath stringByTrimmingCharactersInSet: [NSCharacterSet
                                                                         characterSetWithCharactersInString:@"/"]];
    NSArray *pathParts = [strippedPath componentsSeparatedByString:@"/"];
    
    // calculate our persistent domain and the path of dictionaries to descend
    NSString *persistentDomain;
    NSMutableArray *dictPath = [NSMutableArray arrayWithArray: pathParts];
    if ([pathParts count] > 2) {
        // for 3 or more steps, the domain is first.second.third and the keys are "/first/second/third/", "fourth/", "fifth/"... etc
        persistentDomain = [[NSString stringWithFormat: @"%@.%@.%@", [pathParts objectAtIndex: 0],
                            [pathParts objectAtIndex: 1], [pathParts objectAtIndex: 2]] lowercaseString];

        [dictPath replaceObjectAtIndex: 0 withObject: [NSString stringWithFormat:@"/%@/%@/%@", [pathParts objectAtIndex: 0],
                                                       [pathParts objectAtIndex: 1], [pathParts objectAtIndex: 2]]];
        [dictPath removeObjectAtIndex: 2];
        [dictPath removeObjectAtIndex: 1];
    } else {
        // for 1 or two steps, the domain is first.second.third and the keys are "/", "first/", "second/"
        persistentDomain = @DEFAULT_JAVA_PREFS_DOMAIN;
        [dictPath insertObject: @"" atIndex:0];
    }

    // set up the user defaults for the appropriate persistent domain
    NSUserDefaults *userDefaults = [[NSUserDefaults alloc] init];
    NSDictionary *userOverrides = [userDefaults persistentDomainForName: persistentDomain];
    
    // walk down our path parts, making dictionaries along the way if they are missing
    NSMutableDictionary *parentNode = NULL;
    int pathLength = [dictPath count];
    for (int i = 0; i < pathLength; i++) {
        NSString *nodeKey = [[dictPath objectAtIndex: i] stringByAppendingString: @"/"];
        parentNode = userOverrides;
        userOverrides = [parentNode objectForKey: nodeKey];
        if (userOverrides == Nil) {
            userOverrides = [NSMutableDictionary dictionaryWithCapacity: 2];
            [parentNode setValue: userOverrides forKey: nodeKey];
        }
    }
    
    //If overrides don't exist add them - the intent is to make it easier for the user to actually modify them
    if ([userOverrides count] == 0) {
        if (parentNode == NULL) {
            [userDefaults setPersistentDomain: defaultOverrides forName: persistentDomain];
            userOverrides = [userDefaults persistentDomainForName: persistentDomain];
        } else {
            NSString *nodeKey = [[dictPath objectAtIndex: ([dictPath count] - 1)] stringByAppendingString: @"/"];
            [parentNode setObject: defaultOverrides forKey:nodeKey];
            userOverrides = [parentNode objectForKey: nodeKey];
        }
    }
    
    // some writes may have occured, sync
    [userDefaults synchronize];
    [userDefaults release];
    
    // now we examine the prefs node for defaulted values and substitute user options
    for (id key in defaultOverrides) {
        NSString *newOption;
        if ([userOverrides valueForKey: key] != nil &&
            [[userOverrides valueForKey:key] isNotEqualTo:[defaultOverrides valueForKey:key]]) {
            newOption = [key stringByAppendingString:[userOverrides valueForKey:key]];
        }
        else {
            newOption = [key stringByAppendingString:[defaultOverrides valueForKey:key]];
        }
        NSString *expandedOption =
                [newOption stringByReplacingOccurrencesOfString:@"$APPDIR" withString:contentsPath];
        [expandedOptions addObject: expandedOption];
    }

    //Loop through all user override keys again looking for ones we haven't already uses
    for (id key in userOverrides) {
        //If the default object for key is nil, this is an option the user added so include
        if ([defaultOverrides objectForKey: key] == nil) {
            NSString *newOption = [key stringByAppendingString:[userOverrides valueForKey:key]];
            NSString *expandedOption =
                    [newOption stringByReplacingOccurrencesOfString:@"$APPDIR" withString:contentsPath];
            [expandedOptions addObject: expandedOption];
        }
    }

    return expandedOptions;
}