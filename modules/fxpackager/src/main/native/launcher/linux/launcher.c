/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <stdlib.h>
#include <malloc.h>
#include <memory.h>
#include <errno.h>
#include <unistd.h>
#include <sys/stat.h>
#include <dlfcn.h>
#include <pwd.h>

#include "jni.h"

#include "xmlparser.h"

#define TRUE  1
#define FALSE 0

#define MAX_PATH 4096

int fileExists(char* name) {
  struct stat buf;
  return (stat(name, &buf) == 0) ? TRUE : FALSE;
}

void makeFullFileName(char* basedir, char *relative_path, char *fullpath, int buffer_size) {
    fullpath[0] = 0;
    strcat(fullpath, basedir);
    strcat(fullpath, relative_path);
}

//constructs full file name for file in the package
// and check for it existence
int getFileInPackage(char* basedir, char *relative_path, char *fullpath, int buffer_size) {
     makeFullFileName(basedir, relative_path, fullpath, buffer_size);
    return fileExists(fullpath);
}

#define MAINJAR_FOLDER        "/app/"
#define CONFIG_FILE           "/app/package.cfg"
#define CONFIG_MAINJAR_KEY    "app.mainjar"
#define CONFIG_MAINCLASS_KEY  "app.mainclass"
#define CONFIG_CLASSPATH_KEY  "app.classpath"
#define CONFIG_APP_ID_KEY     "app.preferences.id"

//remove trailing end of line character
//modifies buffer in place
void strip_endofline(char *buf) {
    size_t ln = strlen(buf);

    while (ln > 0 && (buf[ln-1] == '\r' || buf[ln-1] == '\n')) {
        buf[ln-1] = 0;
        ln--;
    }
}

int getSystemJRE(char*jreRoot, unsigned long buflen) {
    char *jh = NULL;

    jh = getenv("JRE_HOME");
    if (jh != NULL) {
        char path[MAX_PATH];
        sprintf(path, "%s/lib/rt.jar", jh);
        if (fileExists(path) == TRUE) {
            strcpy(jreRoot, jh);
            return TRUE;
        } else {
            printf("$JRE_HOME is set but $JRE_HOME/lib/rt.jar does not exist. Look elsewhere.\n");
        }
    }

    jh = getenv("JAVA_HOME");
    if (jh != NULL) {
        char path[MAX_PATH];
        sprintf(path, "%s/jre/lib/rt.jar", jh);
        if (fileExists(path) == TRUE) {
            sprintf(jreRoot, "%s/jre", jh);
            return TRUE;
        } else {
            printf("$JAVA_HOME is set but $JAVA_HOME/jre/lib/rt.jar does not exist. Look elsewhere.\n");
        }
    }

    //check redhat location
    if (fileExists("/usr/java/latest/jre/lib/rt.jar") == TRUE) {
        strcpy(jreRoot, "/usr/java/latest/jre");
        return TRUE;
    }

    //check redhat location
    if (fileExists("/usr/lib/jvm/default-java/jre/lib/rt.jar") == TRUE) {
        strcpy(jreRoot, "/usr/lib/jvm/default-java/jre");
        return TRUE;
    }

    return FALSE;
}

//REWRITE: this is inefficient. We better read and parse file once
int getConfigValue(char* basedir, char* lookupKey, char* outValue, int buf_size) {
    char config[MAX_PATH] = {0};
    char buffer[MAX_PATH*2];
    char *value;
    FILE *fp;

    if (!getFileInPackage(basedir, CONFIG_FILE, config, MAX_PATH)) {
        printf("Configuration file (%s) is not found!\n", config);
        return FALSE;
    }

    //scan file for the key
    fp = fopen(config, "r");
    if (fp == NULL) {
         return FALSE;
    }

    while (fgets(buffer, MAX_PATH*2, fp)) {
        value = strchr(buffer, '=');
        if (value != NULL) {
          //end key on the '=', value will point to the value string
          *value = 0;
          value++;

          if (!strcmp(buffer, lookupKey)) { //found it
             fclose(fp);
             strip_endofline(value);
             strncpy(outValue, value, buf_size);
             return TRUE;
          }
        }

     }
     fclose(fp);

     return FALSE;
}

//Constructs full path to the main jar file
//return false if not found
int getMainJar(char* basedir, char* jar, int buffer_size) {
    char jarname[MAX_PATH] = {0};
    char jar_relative[MAX_PATH] = {0};
    char jar_full[MAX_PATH] = {0};

    if (!getConfigValue(basedir, CONFIG_MAINJAR_KEY, jarname, MAX_PATH)) {
        return FALSE;
    }

    strcat(jar_relative, MAINJAR_FOLDER);
    strcat(jar_relative, jarname);

    int ret = getFileInPackage(basedir, jar_relative, jar_full, MAX_PATH);

    strcat(jar, jar_full);

    return ret;
}

int getExecPath(char *path, size_t len) {
    //this should work for linux
    if (readlink("/proc/self/exe", path, len) != -1) {
        dirname(path);
        return TRUE;
    }

    return FALSE;
}

// Private typedef for function pointer casting
typedef jint (JNICALL *JVM_CREATE)(JavaVM **, JNIEnv **, void *);

int getJvmPath(char* basedir, char *jvmPath, int buffer_size) {
    jvmPath[0] = 0;
    if (!getFileInPackage(basedir, "/runtime/jre/lib/"JAVAARCH"/client/libjvm.so",
            jvmPath, MAX_PATH)) {
        if (!getFileInPackage(basedir, "/runtime/jre/lib/"JAVAARCH"/server/libjvm.so",
                jvmPath, MAX_PATH)) {
            return FALSE;
        }
    }
    return TRUE;
}

int getSystemJvmPath(char *jvmPath, int buffer_size) {
    char basedir[MAX_PATH];
    if (!getSystemJRE(basedir, MAX_PATH)) {
        return FALSE;
    }

    jvmPath[0] = 0;
    if (!getFileInPackage(basedir, "/lib/"JAVAARCH"/client/libjvm.so",
            jvmPath, MAX_PATH)) {
        if (!getFileInPackage(basedir, "/lib/"JAVAARCH"/server/libjvm.so",
                jvmPath, MAX_PATH)) {
            return FALSE;
        }
    }
    return TRUE;
}

/*
 * Replace a pattern in a string (not regex, straight replace) with another
 * string.
 *
 * All strings returned can be passed to free, i.e a new string is always created
 * even if there is an error and the pattern can't be replaced
 *
 * @param str          - original string
 * @param pattern      - pattern to replace (not regex)
 * @param replaceWith  - string to replace pattern
 * @return if pattern not found strdup(str) is returned
 *         or a new str with the pattern replaced (strdup as well)
 */
char *dupAndReplacePattern(char *str, char *pattern, char *replaceWith) {
    static char buffer[MAX_PATH*2] = {0};
    char *p;

    //Return orig if str is not in orig.
    if(!(p = strstr(str, pattern))) {
      return strdup(str);
    }

    int loc = p-str;
    if (loc >= sizeof(buffer)) {
        printf("Failed to replace pattern \"%s\" in string \"%s\" with \"%s\" because buffer not big enough\n",
                pattern, str, replaceWith);
        return strdup(str);
    }

    strncpy(buffer, str, loc); // Copy characters from 'str' start to 'orig' st$
    buffer[loc] = '\0';

    int remaingBufferSize = sizeof(buffer) - loc;
    int len = snprintf(buffer+(loc), remaingBufferSize, "%s%s", replaceWith, p+strlen(pattern));
    if(len > remaingBufferSize ) {
        printf("Failed to replace pattern \"%s\" in string \"%s\" with \"%s\" because buffer not big enough\n",
                pattern, str, replaceWith);
        return strdup(str);
    }
    return strdup(buffer);
}

#define MAX_OPTIONS 100
#define MAX_ARGUMENT_LEN 1000

typedef struct {
    char* name;
    char* value;
} JVMUserArg;

typedef struct {
    JVMUserArg *args;
    int maxSize;
    int currentSize;
    int initialElements;           
} JVMUserArgs;

/**
 * Creates an array of string pointer where each non null entry is malloced and
 * needs to be freed
 * 
 * @param basedir
 * @param keys
 * @param size
 */
void JVMUserArgs_initializeDefaults(JVMUserArgs *this, char* basedir) {
    char jvmArgID[40 + 1];
    char argvalue[MAX_ARGUMENT_LEN + 1] = {0};
    JVMUserArg* keys = this->args;
    
    int index = 0;
    int found = 0;
    do {
        snprintf(jvmArgID, 40, "jvmuserarg.%d.name", (index+1));
        found = getConfigValue(basedir, jvmArgID, argvalue, MAX_ARGUMENT_LEN);
        if (found) {
          keys->name = strdup(argvalue);
          snprintf(jvmArgID, 40, "jvmuserarg.%d.value", (index+1));
          found = getConfigValue(basedir, jvmArgID, argvalue, MAX_ARGUMENT_LEN);
          if (found) {
              //allow use to specify everything in name only
              keys->value = strdup(argvalue);
          }
          else {
              keys->value = strdup("");
          }
          index++;
          keys++;
          this->initialElements++;
          this->currentSize++;
       }
    } while (found && index < this->maxSize);
}

int makeDirRecursively(char *path, mode_t mode) {
    char parent[MAX_PATH], *p;

    if (fileExists(path)) {
        return 0;
    }
    
    /* make a parent directory path */
    strncpy(parent, path, sizeof (parent));
    parent[sizeof (parent) - 1] = '\0';

    for (p = parent + strlen(parent); *p != '/' && p != parent; p--) {
    }
    *p = '\0';

    /* try make parent directory */
    if (p != parent && makeDirRecursively(parent, mode) != 0) {
        return -1;
    }

    //If we got here the parent has already been made so make this one
    //or if it already exists that is ok as well
    if (mkdir(path, mode) == 0 || errno == EEXIST) {
        return 0;
    }
    return -1;
}


/*
 * Assumes that userPref can hold the size of this path 
 */
int getUserPrefFile(char* userPref, char* appid) {
    userPref[0] = 0;
    struct passwd *pw = getpwuid(getuid());
    const char *homedir = pw->pw_dir;    
    
    strcat(userPref, homedir);
    strcat(userPref, "/.java/.userPrefs/");
    strcat(userPref, appid);
    strcat(userPref, "/JVMUserOptions");
    if (fileExists(userPref) == FALSE) {
        makeDirRecursively(userPref,  0777);
    }
    
    strcat(userPref, "/prefs.xml");
    return fileExists(userPref);
}       

void addModifyArgs(JVMUserArgs* this, char* name, char* value) {
    int i;
    
    if (name == NULL || value == NULL) {
        return;
    }
    
    JVMUserArg* arg = this->args;
    for (i = 0; i < this->initialElements; i++) {
        if (strcmp(arg[i].name, name) == 0) {
            free(arg[i].value);
            arg[i].value = malloc((strlen(value) + 1) * sizeof (char));
            strcpy(arg[i].value, value);
            return; //Replaced
        }
    }
    
    //Add new jvm arg from name value 
    int newIndex = this->currentSize;
    arg[newIndex].name = malloc((strlen(name) + 1) * sizeof (char));
    strcpy(arg[newIndex].name, name);
    arg[newIndex].value = malloc((strlen(value) + 1) * sizeof (char));
    strcpy(arg[newIndex].value, value);
    this->currentSize++;
}

void findAndModifyNode(XMLNode* node, JVMUserArgs* args) {
        XMLNode* keyNode = NULL;
        char* key;
        char* value;
        keyNode = FindXMLChild(node->_sub, "entry");
        
    while (keyNode != NULL && args->currentSize < args->maxSize) {
            key = FindXMLAttribute(keyNode->_attributes, "key");
            value = FindXMLAttribute(keyNode->_attributes, "value");
            addModifyArgs(args, key, value);
            keyNode = keyNode->_next;
        }
}

int getJvmUserArgs(JVMUserArgs* args, char* userPrefsPath) {
    FILE *fp;

    if (fileExists(userPrefsPath)) {
        //scan file for the key
        fp = fopen(userPrefsPath, "r");
        if (fp == NULL) {
             return;
        }

        fseek(fp, 0, SEEK_END);
        long fsize = ftell(fp);
        rewind(fp);
        char *buf = malloc(fsize + 1);
        fread(buf, fsize, 1, fp);
        fclose(fp);
        buf[fsize] = 0;
        
        XMLNode* node = NULL;
        XMLNode* doc = ParseXMLDocument(buf);
        if (doc != NULL) {
            node = FindXMLChild(doc, "map");
            if (node != NULL) {
                findAndModifyNode(node, args);
            }
        }
        free(buf);
    }
    return args->currentSize; 
}

/*
 * Converts JVMUserArg to a single jvm argument.
 * 
 * This is used to convert to the actual string passed into the jvm, so has
 * option to free memory of the sub strings 
 * 
 * Returned string can be freed
 */
char* JVMUserArg_toString(char* basedir, JVMUserArg arg, int freeMemory) {
    int len = strlen(arg.name);
    len += strlen(arg.value);
    char* newString = calloc(len + 1, sizeof (char));
    if (newString != NULL) {
        strcat(newString, arg.name);
        strcat(newString, arg.value);
        if (freeMemory == TRUE) {
            free(arg.name);
            free(arg.value);
        }
        char* jvmOption = dupAndReplacePattern(newString, "$APPDIR", basedir);
        free(newString);
        return jvmOption;
    }
    return NULL;
}

int addUserOptions(char* basedir, JavaVMOption* options, int cnt) {
    JVMUserArg args[MAX_OPTIONS - cnt];
    JVMUserArgs jvmUserArgs;
    char appid[MAX_ARGUMENT_LEN + 1] = {0};
    char userPref[MAX_ARGUMENT_LEN + 1] = {0};
    char argvalue[MAX_ARGUMENT_LEN + 1] = {0};


    jvmUserArgs.args = args;
    jvmUserArgs.currentSize = 0;
    jvmUserArgs.initialElements = 0;
    jvmUserArgs.maxSize = MAX_OPTIONS - cnt;
    memset(&args, 0, sizeof (JVMUserArg)*(MAX_OPTIONS - cnt + 1));

    //Add property to command line for preferences id
    if (getConfigValue(basedir, CONFIG_APP_ID_KEY, appid, MAX_ARGUMENT_LEN)) {
        snprintf(argvalue, MAX_ARGUMENT_LEN, "-D%s=%s", CONFIG_APP_ID_KEY, appid);
        options[cnt].optionString = strdup(argvalue);
        cnt++;

        JVMUserArgs_initializeDefaults(&jvmUserArgs, basedir);
        if (getUserPrefFile(userPref, appid)) {
            getJvmUserArgs(&jvmUserArgs, userPref);
        }
        else {
            //If file doesn't exist create it and populate with the default values
            printf("MESSAGE: Creating user preferences file exist: %s", userPref);
            FILE *fp = fopen(userPref, "w");
            if (fp == NULL) {
                printf("MESSAGE: Can not create user preferences: %s", userPref);
            }
            else {
                fprintf(fp, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
                fprintf(fp, "<!DOCTYPE map SYSTEM \"http://java.sun.com/dtd/preferences.dtd\">\n");
                fprintf(fp, "<map MAP_XML_VERSION=\"1.0\">\n");   
                int i;
                for (i; i < jvmUserArgs.currentSize; i++) {
                    fprintf(fp, "    <entry key=\"%s\" value=\"%s\"/>\n", 
                            jvmUserArgs.args[i].name,
                            jvmUserArgs.args[i].value);
                }
                fprintf(fp, "</map>\n");
            }
            fclose(fp);
        }

        //Copy all user args to options
        int i;
        for (i = 0; i < jvmUserArgs.currentSize; i++) {
            char* jvmOption = JVMUserArg_toString(basedir, args[i], TRUE);
            if (jvmOption != NULL) {
                options[cnt].optionString = jvmOption;
                cnt++;
            }
        }
    } else {
        printf("WARNING: %s not defined:", CONFIG_APP_ID_KEY);
    }
    return cnt;
}


int startJVM(char* basedir, char *appFolder, char* jar, int argc, const char**argv) {
    char jvmPath[MAX_PATH+1] = {0},
         tmpPath[MAX_PATH+1];
    JavaVMInitArgs jvmArgs;
    JavaVMOption options[MAX_OPTIONS];
    JVM_CREATE createProc;
    JNIEnv* env;
    JavaVM* jvm = NULL;
    char classpath[MAX_PATH*2] = {0};
    jclass cls;
    jmethodID mid;
    char argname[20];
    char argvalue[MAX_ARGUMENT_LEN];
    char mainclass[MAX_PATH];
    
    memset(&options, 0, sizeof(JavaVMOption)*(MAX_OPTIONS + 1));
    memset(&jvmArgs, 0, sizeof(JavaVMInitArgs));

    makeFullFileName(basedir, "/runtime", tmpPath, sizeof(tmpPath));
    if (fileExists(tmpPath)) {
       if (!getJvmPath(basedir, jvmPath, sizeof(jvmPath))) {
            printf("libjvm.so is not found in the bundled runtime.\n");
            return FALSE;
       }
    } else {
        if (!getSystemJvmPath(jvmPath, sizeof(jvmPath))) {
            printf("Failed to find system runtime.\n");
            return FALSE;
        }
    }

    // Dynamically load the JVM
    void* jvmLibHandle = dlopen(jvmPath, RTLD_LAZY);
    if (jvmLibHandle == NULL) {
        printf("Error loading libjvm.so\n");
        return FALSE;
    }

    strcpy(classpath, "-Djava.class.path=");
    strcat(classpath, jar);

    if (getConfigValue(basedir, CONFIG_CLASSPATH_KEY, argvalue, sizeof(argvalue))) {
        char *in = argvalue;
        char *out = argvalue;
        int needSemicolon = FALSE;

        //compress spaces and replaces them with :
        while (*in != 0) {
            if (*in == ' ') {
                if (needSemicolon == TRUE) {
                    *out = ':';
                    out++;
                    needSemicolon = FALSE;
                }
            } else {
                needSemicolon = TRUE;
                *out = *in;
                out++;
            }
            in++;
        }
        *out = 0;

        if (strlen(argvalue) > 0) {
            strcat(classpath, ":");
            strcat(classpath, argvalue);
        }
    }
    
    // Set up the VM init args
    jvmArgs.version = JNI_VERSION_1_2;

    options[0].optionString = classpath;

    //add application specific JVM parameters
    int cnt = 1;

    //Note: should not try to quote the path. Spaces are fine here
    sprintf(argvalue, "-Djava.library.path=%s", appFolder);
    options[cnt].optionString = strdup(argvalue);
    cnt++;

    int found = 0;
    int idx = 1;
    do {
       sprintf(argname, "jvmarg.%d", idx);
       found = getConfigValue(basedir, argname, argvalue, sizeof(argvalue));
       if (found) {
          //jvmOption is always a new string via strdup
          char* jvmOption = dupAndReplacePattern(argvalue, "$APPDIR", basedir);
          if (jvmOption != NULL) {
            options[cnt].optionString = jvmOption;
            cnt++;
          }
          idx++;
       }
    } while (found && cnt < MAX_OPTIONS);
    
    cnt = addUserOptions(basedir, options, cnt);
    
    jvmArgs.version = 0x00010002;
    jvmArgs.options = options;
    jvmArgs.nOptions = cnt;
    jvmArgs.ignoreUnrecognized = JNI_TRUE;

    // Create the JVM
    createProc = (JVM_CREATE) dlsym(jvmLibHandle, "JNI_CreateJavaVM");
    if (createProc == NULL) {
        printf("Failed to locate JNI_CreateJavaVM\n");
        return FALSE;
    }

    if ((*createProc)(&jvm, &env, &jvmArgs) < 0) {
        // Should not happen
        printf("Failed to create JVM\n");
        return FALSE;
    }

    if (!getConfigValue(basedir, CONFIG_MAINCLASS_KEY, mainclass, sizeof(mainclass))) {
        printf("Packaging error: no main class specified.\n");
        return FALSE;
    }
    
    cls = (*env)->FindClass(env, mainclass);
    if (cls != NULL) {
        mid = (*env)->GetStaticMethodID(env, cls, "main", "([Ljava/lang/String;)V");
         if (mid != NULL) {
            int i;
            jclass stringClass = (*env)->FindClass(env, "java/lang/String");

            //prepare app arguments if any. Skip value at index 0 - this is path to executable ...
            //NOTE:
            //  - what if user run in non-English/UTF-8 locale? do we need to convert args?
            //  - extend to pass jvm args and debug args (allow them in front, use marker option to separate them?)
            jobjectArray args = (*env)->NewObjectArray(env, argc-1, stringClass, NULL);
            for(i=1; i<argc; i++) {
                (*env)->SetObjectArrayElement(env, args, i-1,
                    (*env)->NewStringUTF(env, argv[i]));
            }

            (*env)->CallStaticVoidMethod(env, cls, mid, args);
        } else {
            printf("Expected to find main method in %s.\n", mainclass);
        }
    } else {
        printf("Expected to find launcher class: [%s]\n", mainclass);
    }

    if ((*env)->ExceptionOccurred(env)) {
        printf("Exception thrown from main method of %s\n", mainclass);
        (*env)->ExceptionDescribe(env);
    }

    // If application main() exits quickly but application is run on some other thread
    //  (e.g. Swing app performs invokeLater() in main and exits)
    // then if we return execution to tWinMain it will exit.
    // This will cause process to exit and application will not actually run.
    //
    // To avoid this we are trying to detach jvm from current thread (java.exe does the same)
    // Because we are doing this on the main JVM thread (i.e. one that was used to create JVM)
    // this call will spawn "Destroy Java VM" java thread that will shut JVM once there are
    // no non-daemon threads running, and then return control here.
    // I.e. this will happen when EDT and other app thread will exit.
    if ((*jvm)->DetachCurrentThread(jvm) != 0) {
        printf("Failed to detach from JVM.\n");
    }
    (*jvm)->DestroyJavaVM(jvm);

    return TRUE;
}

int getAppFolder(char* basedir, char* appFolder, int buffer_size) {
    return getFileInPackage(basedir, MAINJAR_FOLDER, appFolder, MAX_PATH);
}

int main(int argc, const char** argv) {
    char basedir[MAX_PATH] = {0};
    char appFolder[MAX_PATH] = {0};
    char jar[MAX_PATH] = {0};

    if (getExecPath(basedir, MAX_PATH) == TRUE) {
        if (!getMainJar(basedir, jar, MAX_PATH)) {
            if (jar[0] == 0) {
                    printf("Failed to parse package configuration file\n");
            } else {
                    printf("Failed to find main application jar! (%s)\n", jar);
            }
            return -1;
        }

        getAppFolder(basedir, appFolder, MAX_PATH);

        //DO Launch
        //this will concatenate arguments using space,
        // we need to make sure spaces are properly escaped if we have any
        chdir(appFolder);

        if (!startJVM(basedir, appFolder, jar, argc, argv)) {
            printf("Failed to launch JVM\n");
            return -1;
        }
    }

    return 1;
}

