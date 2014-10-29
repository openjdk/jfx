/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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


#include "Platform.h"

#ifdef MAC

#include "MacPlatform.h"
#include "Helpers.h"

#include <sys/sysctl.h>
#include <pthread.h>

#import <Foundation/Foundation.h>

#include <CoreFoundation/CoreFoundation.h>
#include <CoreFoundation/CFString.h>

#ifdef __OBJC__
#import <Cocoa/Cocoa.h>
#endif //__OBJC__


MacPlatform::MacPlatform(void) : Platform(), GenericPlatform(), PosixPlatform() {
}

MacPlatform::~MacPlatform(void) {
}

bool MacPlatform::UsePListForConfigFile() {
    return FilePath::FileExists(GetConfigFileName()) == false;
}

void MacPlatform::ShowError(TString Title, TString Description) {
    NSString *ltitle = [NSString stringWithCString:Title.c_str()
                                            encoding:[NSString defaultCStringEncoding]];

    NSString *ldescription = [NSString stringWithCString:Description.c_str()
                                            encoding:[NSString defaultCStringEncoding]];

    NSLog(@"%@:%@", ltitle, ldescription);
}

void MacPlatform::ShowError(TString Description) {
    TString appname = GetModuleFileName();
    appname = FilePath::ExtractFileName(appname);
    ShowError(appname, Description);
}


TCHAR* MacPlatform::ConvertStringToFileSystemString(TCHAR* Source, bool &release) {
    TCHAR* result = NULL;
    release = false;
    CFStringRef StringRef = CFStringCreateWithCString(kCFAllocatorDefault, Source, kCFStringEncodingUTF8);
    
    if (StringRef != NULL) {
        @try {
            CFIndex length = CFStringGetMaximumSizeOfFileSystemRepresentation(StringRef);
            result = new char[length + 1];
            
            if (CFStringGetFileSystemRepresentation(StringRef, result, length)) {
                release = true;
            }
            else {
                delete[] result;
                result = NULL;
            }
        }
        @finally {
            CFRelease(StringRef);
        }
    }
    
    return result;
}

TCHAR* MacPlatform::ConvertFileSystemStringToString(TCHAR* Source, bool &release) {
    TCHAR* result = NULL;
    release = false;
    CFStringRef StringRef = CFStringCreateWithFileSystemRepresentation(kCFAllocatorDefault, Source);
    
    if (StringRef != NULL) {
        @try {
            CFIndex length = CFStringGetLength(StringRef);
            
            if (length > 0) {
                CFIndex maxSize = CFStringGetMaximumSizeForEncoding(length, kCFStringEncodingUTF8);
                
                result = new char[maxSize + 1];
                
                if (CFStringGetCString(StringRef, result, maxSize, kCFStringEncodingUTF8) == true) {
                    release = true;
                }
                else {
                    delete[] result;
                    result = NULL;
                }
            }
        }
        @finally {
            CFRelease(StringRef);
        }
    }
    
    return result;
}

void MacPlatform::SetCurrentDirectory(TString Value) {
    chdir([NSHomeDirectory() UTF8String]);
}

TString MacPlatform::GetPackageRootDirectory() {
    NSBundle *mainBundle = [NSBundle mainBundle];
    NSString *mainBundlePath = [mainBundle bundlePath];
    NSString *contentsPath = [mainBundlePath stringByAppendingString:@"/Contents"];
    TString result = [contentsPath UTF8String];
    return result;
}

TString MacPlatform::GetAppDataDirectory() {
    TString result;
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES);
    NSString *applicationSupportDirectory = [paths firstObject];
    result = [applicationSupportDirectory UTF8String];
    return result;
}

TString MacPlatform::GetJvmPath() {
    TString result;
    TString runtimePath;

    @try {
        NSBundle *mainBundle = [NSBundle mainBundle];
        NSDictionary *infoDictionary = [mainBundle infoDictionary];
        NSString *runtime = [infoDictionary objectForKey:@"JVMRuntime"];

        if ([runtime length] != 0) {
            TString libjliPath = [[[NSBundle mainBundle] builtInPlugInsPath] UTF8String];
            runtimePath = FilePath::IncludeTrailingSlash(libjliPath) + [runtime UTF8String];
        }
    } @catch (NSException *exception) {
        NSLog(@"%@: %@", exception, [exception callStackSymbols]);
        return _T("");
    }

    result = FilePath::IncludeTrailingSlash(runtimePath) + _T("Contents/Home/jre/lib/jli/libjli.dylib");

    if (FilePath::FileExists(result) == false) {
        result = FilePath::IncludeTrailingSlash(runtimePath) + _T("Contents/Home/lib/jli/libjli.dylib");

        if (FilePath::FileExists(result) == false) {
            result = _T("");
        }
    }

    return result;
}

TString MacPlatform::GetSystemJvmPath() {
    TString result = _T("/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/lib/jli/libjli.dylib");

    if (FilePath::FileExists(result) == false) {
        result = _T("");
    }

    return result;
}

TString MacPlatform::GetAppName() {
    NSString *appName = [[NSProcessInfo processInfo] processName];
    TString result = [appName UTF8String];
    return result;
}

// Convert parts of the info.plist to the INI format the rest of the packager uses unless
// a packager config file exists.
PropertyContainer* MacPlatform::GetConfigFile(TString FileName) {
    if (UsePListForConfigFile() == false) {
        return new PropertyFile(FileName);
    }

    NSBundle *mainBundle = [NSBundle mainBundle];
    NSDictionary *infoDictionary = [mainBundle infoDictionary];

    std::map<TString, TString> data;

    // Packager options.
    for (id key in [infoDictionary allKeys]) {
        id option = [infoDictionary valueForKey:key];

        if ([key isKindOfClass:[NSString class]] && [option isKindOfClass:[NSString class]]) {
            TString name = [key UTF8String];
            TString value = [option UTF8String];
            data.insert(std::map<TString, TString>::value_type(name, value));
        }
    }

    // jvmargs
    NSArray *options = [infoDictionary objectForKey:@"JVMOptions"];
    int index = 1;

    for (id option in options) {
        if ([option isKindOfClass:[NSString class]]) {
            TString value = [option UTF8String];
            TString argname = TString(_T("jvmarg.")) + PlatformString(index).toString();
            data.insert(std::map<TString, TString>::value_type(argname, value));
            index++;
        }
    }

    // jvmuserargs
    NSDictionary *defaultOverrides = [infoDictionary objectForKey:@"JVMUserOptions"];
    index = 1;

    for (id key in defaultOverrides) {
        id option = [defaultOverrides valueForKey:key];

        if ([key isKindOfClass:[NSString class]] && [option isKindOfClass:[NSString class]]) {
            TString name = [key UTF8String];
            TString value = [option UTF8String];
            TString prefix = TString(_T("jvmuserarg.")) + PlatformString(index).toString();
            TString argname = prefix + _T(".name");
            TString argvalue = prefix + _T(".value");
            data.insert(std::map<TString, TString>::value_type(argname, name));
            data.insert(std::map<TString, TString>::value_type(argvalue, value));
            index++;
        }
    }

    // args
    NSDictionary *args = [infoDictionary objectForKey:@"ArgOptions"];
    index = 1;

    for (id option in args) {
        if ([option isKindOfClass:[NSString class]]) {
            TString value = [option UTF8String];
            TString argname = TString(_T("arg.")) + PlatformString(index).toString();
            data.insert(std::map<TString, TString>::value_type(argname, value));
            index++;
        }
    }

    return new PropertyFile(data);
}

TString MacPlatform::GetModuleFileName() {
    return "";
}

int MacPlatform::GetProcessID() {
    int pid = [[NSProcessInfo processInfo] processIdentifier];
    return pid;
}

bool MacPlatform::IsMainThread() {
    bool result = (pthread_main_np() == 1);
    return result;
}

size_t MacPlatform::GetMemorySize() {
    unsigned long long memory = [[NSProcessInfo processInfo] physicalMemory];
    return memory;
}

std::map<TString, TString> MacPlatform::GetKeys() {
    std::map<TString, TString> keys;

    if (UsePListForConfigFile() == false) {
        return GenericPlatform::GetKeys();
    }
    else {
        keys.insert(std::map<TString, TString>::value_type(CONFIG_MAINJAR_KEY,        _T("JVMMainJarName")));
        keys.insert(std::map<TString, TString>::value_type(CONFIG_MAINCLASSNAME_KEY,  _T("JVMMainClassName")));
        keys.insert(std::map<TString, TString>::value_type(CONFIG_CLASSPATH_KEY,      _T("JVMAppClasspath")));
        keys.insert(std::map<TString, TString>::value_type(APP_NAME_KEY,              _T("CFBundleName")));
        keys.insert(std::map<TString, TString>::value_type(CONFIG_SPLASH_KEY,         _T("app.splash")));
        keys.insert(std::map<TString, TString>::value_type(CONFIG_APP_ID_KEY,         _T("JVMPreferencesID")));
        keys.insert(std::map<TString, TString>::value_type(JVM_RUNTIME_KEY,           _T("JVMRuntime")));
        keys.insert(std::map<TString, TString>::value_type(PACKAGER_APP_DATA_DIR,     _T("CFBundleIdentifier")));
    }

    return keys;
}

//--------------------------------------------------------------------------------------------------

MacJavaUserPreferences::MacJavaUserPreferences(void) : JavaUserPreferences() {
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
 <TString>-verbose:class</TString>
 <TString>-Djava.policy.file=$APPDIR/app/whatever.policy</TString>
 </array>
 <key>JVMUserOptions</key>
 <dict>
 <key>-Djava.util.logging.config.file=</key>
 <TString>~/logging.properties</TString>
 <key>-Xmx</key>
 <TString>768m</TString>
 </dict>

 * and initially set up the applications preference file in ~/Library/Preferences/..name based on bundleid..plist,
 * i.e. just the JVMUserOptions

 <key>JVMUserOptions</key>
 <dict>
 <key>-Djava.util.logging.config.file=</key>
 <TString>~/logging.properties</TString>
 <key>-Xmx</key>
 <TString>860m</TString>
 </dict>

 */

#define JVM_OPTIONS_KEY "JVMOptions"
#define JVM_USER_OPTIONS_KEY "JVMUserOptions"
#define JVM_PREFERENCES_ID "JVMPreferencesID"
#define DEFAULT_JAVA_PREFS_DOMAIN "com.apple.java.util.prefs"


NSArray *getJVMOptions(NSDictionary *infoDictionary, NSString *mainBundlePath) {
    NSArray *options = [infoDictionary objectForKey:@JVM_OPTIONS_KEY];
    NSDictionary *defaultOverrides = [infoDictionary objectForKey:@JVM_USER_OPTIONS_KEY];

    if (options == nil) {
        options = [NSArray array];
    }


    //Do String substitutions - for now only one is $APPDIR, if a second one is added this will
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
    NSDictionary *parentNode = NULL;
    NSUInteger pathLength = [dictPath count];
    for (unsigned int i = 0; i < pathLength; i++) {
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
            //[parentNode setObject: defaultOverrides forKey:nodeKey];
            userOverrides = [parentNode objectForKey: nodeKey];
        }
    }

    // some writes may have occured, sync
    [userDefaults synchronize];
    //[userDefaults release];

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

bool MacJavaUserPreferences::Load(TString Appid) {
    bool result = false;

    //TODO implement

    return result;
}

//--------------------------------------------------------------------------------------------------

#endif //MAC
