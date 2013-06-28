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
#import <jni.h>

@interface GlassMenubar : NSObject
{
@public
        
        NSMenu        *menu;
        
        jlong        _insertjMenuPtr;
        jint        _insertjPos;
        
        jlong        _removejMenuPtr;
        jint        _removejPos;
}

- (id)init;

- (void)_insert;
- (void)_remove;

@end

@interface GlassMenu : NSObject<NSMenuDelegate>
{
    jobject     jMenu;
@public
        
    jobject     jDelegate;
    jobject     jCallback;

    NSMenuItem  *item;
    NSMenu      *menu;
                
    jlong       _insertSubmenuPtr;
    jint        _insertPos;
        
    jlong       _removeSubmenuPtr;
    jint        _removePos;
        
    jstring     _setTitle;
        
    jchar       _setShortcutShortcut;
    jint        _setShortcutModifiers;
        
    jboolean        _setEnabled;
        
    jboolean        _setChecked;
        
    jobject     _setCallback;
   
    jobject     _setPixels;
}

// Menu
- (id)initWithJavajdelegate:(jobject)jdelegate 
                     jtitle:(jstring)jtitle 
                   jenabled:(jboolean)jenabled;

// MenuItem
- (id)initWithJavajdelegate:(jobject)jdelegate jtitle:(jstring)jtitle jshortcut:(jchar)jshortcut jmodifiers:(int)jmodifiers jicon:(jobject)jicon jenabled:(jboolean)jenabled jchecked:(jboolean)jchecked jcallback:(jobject)jcallback;

- (void)action:(id)sender;

- (void)_insert;
- (void)_remove;
- (void)_setTitle;
- (void)_setShortcut;
- (void)_setEnabled;
- (void)_setChecked;
- (void)_setCallback;
- (void)_setPixels;

@end
