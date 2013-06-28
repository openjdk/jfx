/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.dalvik.net;

import java.util.ArrayList;
import java.util.List;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.URI;


public class DalvikProxySelector {
    private String[] args;

    private DalvikProxySelector(String[] args){
        this.args = args;
    }

    //Result host and port are returned in a string in the format host:port
    public static String[] getProxyForURL(String target) {
        String[] proxyInfo = new String[0];
        List<String> proxies = new ArrayList<String>();
        URI uri = null;
        try {
            ProxySelector defaultProxySelector = ProxySelector.getDefault();
            uri = new URI(target);
            List<Proxy> proxyList = defaultProxySelector.select(uri);

            Proxy proxy = proxyList.get(0);
            if (proxy.equals(Proxy.NO_PROXY)) {
                System.out.println("DalvikProxySelector.getProxyForURL(): No proxy found");
                return null;
            }
            SocketAddress address = proxy.address();
            InetSocketAddress inetSocketAddress = (InetSocketAddress) address;
            String host = inetSocketAddress.getHostName();
            int port = inetSocketAddress.getPort();
            if (host == null) {
                System.out.println("DalvikProxySelector.getProxyForURL(): No proxy found");
                return null;
            }

            proxies.add(host);                      // even index, host
            proxies.add(Integer.toString(port));    // odd index, port

            System.out.println("DalvikProxySelector.getProxyForURL(): host=" + host + " port=" + port);
            return proxies.toArray(new String[0]);
        } catch (Exception e) {
            System.out.println("DalvikProxySelector.getProxyForURL(): exception(ignored): " + e.toString());
            return null;
        }
    }
}
