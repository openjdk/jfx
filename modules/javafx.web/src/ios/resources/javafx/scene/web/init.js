/*
 * Copyright (c) 2012, 2013 Oracle and/or its affiliates. All rights reserved.
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
(function (key) {
var JavaBridge = {
        callbackCnt: 0,
        callbacks: {},
        callBack: function(id, success, result) {
            var cb = JavaBridge.callbacks[id];
            try {
                if (cb) {
                    cb['success'] = success;
                    cb['result'] = result;
                }
            } catch (e) {
                alert(e);
            }
        },
        call: function(method, args) {
            var cbId = cbId = ++JavaBridge.callbackCnt;
            // TODO: set 'success': false, and process callBack, but so far it happens
            // too late. As an alternative, use JSObject::call to send values from the
            // Java class back to JavaScript.
            JavaBridge.callbacks[cbId] = {'success': true, 'result': null};

            if (args !== null && args instanceof Array) {//we always encode args as an Array ...
                for (var i = 0; i < args.length; i++) {
                    args[i] = JavaBridge.encodeObject(args[i]);
                }
            }
            var iframe = document.createElement('iframe');
            iframe.setAttribute("width","1");
            iframe.setAttribute("height","1");
            iframe.setAttribute("frameborder",0);
            iframe.setAttribute("style","display:none");
            iframe.setAttribute('src', 'javacall:' + key + ':' + cbId + ':' + method + ':' + encodeURIComponent(JSON.stringify(args)));
            document.documentElement.appendChild(iframe);
            iframe.parentNode.removeChild(iframe);
            iframe = null;

            var success = JavaBridge.callbacks[cbId]['success'];
            var result = JavaBridge.callbacks[cbId]['result']
            delete JavaBridge.callbacks[cbId];

            if (success) {
                return result;
            }
            // on failure result should contain error (exception) message
            if (result == null) {
                // Java didn't set error message. it means something went wrong
                throw new Error("Internal java call error");
            }
            throw new Error(result);
        },
        encodeObject: function(evaluated) {
            var typeOfEvaluated = typeof evaluated;
            if (typeOfEvaluated === 'undefined') {
                return 'undefined'; // undefined
            } else if (typeOfEvaluated === 'object' || typeOfEvaluated === 'function') {
                if (evaluated !== null) {
                    return 'o' + JavaBridge.exportJSObject(evaluated);//object id
                }
            } else if (typeOfEvaluated === 'string') {
                return 's' + evaluated;
            }
            return evaluated;
        },
        exportedJSObjects: [],
        exportJSObject: function(o) {
            var i = JavaBridge.exportedJSObjects.indexOf(o);
            if (i >= 0) {
                return i;
            }
            return JavaBridge.exportedJSObjects.push(o) - 1;
        },
        fxEvaluate: function(javaScript) {
            return JSON.stringify(JavaBridge.encodeObject(eval(javaScript)));
        }
    };

    window.mustek = function(token) {
        if (token === key) {
            return JavaBridge;
        }
        return null;
    };
    
    window.alert = function(message) {
        JavaBridge["jsEventHandler"].onAlertNotify(message);
    };
})