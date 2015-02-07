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
#include "Package.h"

#include <sys/sysctl.h>
#include <pthread.h>
#include <vector>

#import <Foundation/Foundation.h>

#include <CoreFoundation/CoreFoundation.h>
#include <CoreFoundation/CFString.h>

#ifdef __OBJC__
#import <Cocoa/Cocoa.h>
#endif //__OBJC__

//--------------------------------------------------------------------------------------------------

NSString* StringToNSString(TString Value) {
    NSString* result = [NSString stringWithCString:Value.c_str()
                                          encoding:[NSString defaultCStringEncoding]];
    return result;
}

//--------------------------------------------------------------------------------------------------

MacPlatform::MacPlatform(void) : Platform(), GenericPlatform(), PosixPlatform() {
}

MacPlatform::~MacPlatform(void) {
}

bool MacPlatform::UsePListForConfigFile() {
    return FilePath::FileExists(GetConfigFileName()) == false;
}

void MacPlatform::ShowMessage(TString Title, TString Description) {
    NSString *ltitle = StringToNSString(Title);
    NSString *ldescription = StringToNSString(Description);

    NSLog(@"%@:%@", ltitle, ldescription);
}

void MacPlatform::ShowMessage(TString Description) {
    TString appname = GetModuleFileName();
    appname = FilePath::ExtractFileName(appname);
    ShowMessage(appname, Description);
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
    chdir(PlatformString(Value).toPlatformString());
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

TString MacPlatform::GetBundledJVMLibraryFileName(TString RuntimePath) {
    TString result;

    result = FilePath::IncludeTrailingSlash(RuntimePath) + _T("Contents/Home/jre/lib/jli/libjli.dylib");

    if (FilePath::FileExists(result) == false) {
        result = FilePath::IncludeTrailingSlash(RuntimePath) + _T("Contents/Home/lib/jli/libjli.dylib");

        if (FilePath::FileExists(result) == false) {
            result = _T("");
        }
    }

    return result;
}


TString MacPlatform::GetSystemJRE() {
    return _T("/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/lib/jli/libjli.dylib");
}

TString MacPlatform::GetSystemJVMLibraryFileName() {
    TString result = GetSystemJRE();

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

bool MacPlatform::IsMainThread() {
    bool result = (pthread_main_np() == 1);
    return result;
}

TPlatformNumber MacPlatform::GetMemorySize() {
    unsigned long long memory = [[NSProcessInfo processInfo] physicalMemory];
    TPlatformNumber result = memory / 1048576; // Convert from bytes to megabytes.
    return result;
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
        keys.insert(std::map<TString, TString>::value_type(CONFIG_APP_MEMORY,         _T("app.memory")));
    }

    return keys;
}

#ifdef DEBUG 
bool MacPlatform::IsNativeDebuggerPresent() {
    int state;
    int mib[4];
    struct kinfo_proc info;
    size_t size;
    
    info.kp_proc.p_flag = 0;
    
    mib[0] = CTL_KERN;
    mib[1] = KERN_PROC;
    mib[2] = KERN_PROC_PID;
    mib[3] = getpid();
    
    size = sizeof(info);
    state = sysctl(mib, sizeof(mib) / sizeof(*mib), &info, &size, NULL, 0);
    assert(state == 0);
    return ((info.kp_proc.p_flag & P_TRACED) != 0);
}

int MacPlatform::GetProcessID() {
    int pid = [[NSProcessInfo processInfo] processIdentifier];
    return pid;
}
#endif //DEBUG

//--------------------------------------------------------------------------------------------------

class UserDefaults {
private:
    TOrderedMap FData;
    TString FDomainName;
    
    bool ReadDictionary(NSDictionary *Items, TOrderedMap &Data) {
        bool result = false;
        int index = 1;
        
        for (id key in Items) {
            id option = [Items valueForKey:key];
            
            if ([key isKindOfClass:[NSString class]] && [option isKindOfClass:[NSString class]]) {
                TString name = [key UTF8String];
                TString value = [option UTF8String];
                
                if (name.empty() == false) {
                    TValueIndex item;
                    item.value = value;
                    item.index = index;
                    
                    Data.insert(TOrderedMap::value_type(name, item));
                    result = true;
                    index++;
                }
            }
        }
        
        return result;
    }
    
    // Open and read the defaults file specified by domain.
    bool ReadPreferences(NSDictionary *Defaults, std::list<TString> Keys, TOrderedMap &Data) {
        bool result = false;
        
        if (Keys.size() > 0 && Defaults != NULL) {
            NSDictionary *node = Defaults;
            
            while (Keys.size() > 0 && node != NULL) {
                TString key = Keys.front();
                Keys.pop_front();
                NSString *tempKey = StringToNSString(key);
                node = [node valueForKey:tempKey];
                
                if (Keys.size() == 0) {
                    break;
                }
            }
            
            if (node != NULL) {
                result = ReadDictionary(node, Data);
            }
        }
        
        return result;
    }
    
    NSDictionary* LoadPreferences(TString DomainName) {
        NSDictionary *result = NULL;
        
        if (DomainName.empty() == false) {
            NSUserDefaults *prefs = [[NSUserDefaults alloc] init];
            
            if (prefs != NULL) {
                NSString *lDomainName = StringToNSString(DomainName);
                result = [prefs persistentDomainForName: lDomainName];
            }
        }
        
        return result;
    }
    
public:
    UserDefaults(TString DomainName) {
        FDomainName = DomainName;
    }
    
    bool Read(std::list<TString> Keys) {
        NSDictionary *defaults = LoadPreferences(FDomainName);
        return ReadPreferences(defaults, Keys, FData);
    }
    
    TOrderedMap GetData() {
        return FData;
    }
};

//--------------------------------------------------------------------------------------------------

MacJavaUserPreferences::MacJavaUserPreferences(void) : JavaUserPreferences() {
}

TString toLowerCase(TString Value) {
    // Use Cocoa's lowercase method because it is better than the ones provided by C/C++.
    NSString *temp = StringToNSString(Value);
    temp = [temp lowercaseString];
    TString result = [temp UTF8String];
    return result;
}

// Split the string Value into using Delimiter.
std::list<TString> Split(TString Value, TString Delimiter) {
    std::list<TString> result;
    std::vector<char> buffer(Value.c_str(), Value.c_str() + Value.size() + 1);
    char *p = strtok(&buffer[0], Delimiter.data());
    
    while (p != NULL) {
        TString token = p;
        result.push_back(token);
        p = strtok(NULL, Delimiter.data());
    }
    
    return result;
}

// 1. If the path is fewer than three components (Example: one/two/three) then the domain is the
//    default domain "com.apple.java.util.prefs" stored in the plist file
//    ~/Library/Preferences/com.apple.java.util.prefs.plist
//
//    For example: If AppID = "hello", the path is "hello/JVMUserOptions and the
//    plist file is ~/Library/Preferences/com.apple.java.util.prefs.plist containing the contents:
//
//    <?xml version="1.0" encoding="UTF-8"?>
//    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
//    <plist version="1.0">
//    <dict>
//      <key>/</key>
//      <dict>
//        <key>hello/</key>
//        <dict>
//          <key>JVMUserOptions/</key>
//          <dict>
//            <key>-DXmx</key>
//            <string>512m</string>
//          </dict>
//        </dict>
//      </dict>
//    </dict>
//    </plist>
//
// 2. If the path is three or more, the first three become the domain name (even
//    if shared across applicaitons) and the remaining become individual keys.
//
//    For example: If AppID = "com/hello/foo", the path is "hello/JVMUserOptions and the
//    domain is "com.hello.foo" stored in the plist file ~/Library/Preferences/com.hello.foo.plist
//    containing the contents:
//
//    <?xml version="1.0" encoding="UTF-8"?>
//    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
//    <plist version="1.0">
//    <dict>
//      <key>/com/hello/foo/</key>
//      <dict>
//        <key>JVMUserOptions/</key>
//        <dict>
//          <key>-DXmx</key>
//          <string>512m</string>
//        </dict>
//      </dict>
//    </dict>
//    </plist>
//
// NOTE: To change these values use the command line utility "defaults":
// Example: defaults read com.apple.java.util.prefs /
// Since OS 10.9 Mavericks the defaults are cashed so directly modifying the files is not recommended.
bool MacJavaUserPreferences::Load(TString Appid) {
    bool result = false;
    
    if (Appid.empty() == false) {
        // This is for backwards compatability. Older packaged applications have an
        // app.preferences.id that is delimited by period (".") rather than
        // slash ("/") so convert to newer style.
        TString path = Helpers::ReplaceString(Appid, _T("."), _T("/"));
        
        path = path + _T("/JVMUserOptions");
        TString domainName;
        std::list<TString> keys = Split(path, _T("/"));
        
        // If there are less than three parts to the path then use the default preferences file.
        if (keys.size() < 3) {
            domainName = _T("com.apple.java.util.prefs");

            // Append slash to the end of each key.
            for (std::list<TString>::iterator iterator = keys.begin(); iterator != keys.end(); iterator++) {
                TString item = *iterator;
                item = item + _T("/");
                *iterator = item;
            }
            
            // The root key is /.
            keys.push_front(_T("/"));
        }
        else {
            // Remove the first three keys and use them for the root key and the preferencesID.
            TString one = keys.front();
            keys.pop_front();
            TString two = keys.front();
            keys.pop_front();
            TString three = keys.front();
            keys.pop_front();
            domainName = one + TString(".") + two + TString(".") + three;
            domainName = toLowerCase(domainName);
            
            // Append slash to the end of each key.
            for (std::list<TString>::iterator iterator = keys.begin(); iterator != keys.end(); iterator++) {
                TString item = *iterator;
                item = item + _T("/");
                *iterator = item;
            }
            
            // The root key is /one/two/three/
            TString key = TString("/") + one + TString("/") + two + TString("/") + three + TString("/");
            keys.push_front(key);
        }

        UserDefaults userDefaults(domainName);
        
        if (userDefaults.Read(keys) == true) {
            result = true;
            FMap = userDefaults.GetData();
        }
    }
    
    return result;
}

//--------------------------------------------------------------------------------------------------

#endif //MAC
