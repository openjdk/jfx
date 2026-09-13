/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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

package embeddedswing;

import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

public class MusicRegister {

    private static final Map<String, JScrollPane> map = new HashMap<String, JScrollPane>();
    private static final JTabbedPane tabbedPane = new JTabbedPane();

    public static JComponent create() {
        //
        // SwingSet2 extract
        //
        DefaultMutableTreeNode catagory = null ;
        DefaultMutableTreeNode artist = null;
        DefaultMutableTreeNode record = null;

        URL url = MusicRegister.class.getResource("resources/tree.txt");

        try {
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            BufferedReader reader = new BufferedReader(isr);

            String line = reader.readLine();
            while (line != null) {
                char linetype = line.charAt(0);
                switch (linetype) {
                    case 'C':
                        String key = line.substring(2);
                        catagory = new DefaultMutableTreeNode(key);
                        JTree tree = new JTree(catagory) {
                            public Insets getInsets() {
                                return new Insets(5, 5, 5, 5);
                            }
                        };
                        tree.setEditable(true);
                        map.put(key, new JScrollPane(tree));
                        break;
                    case 'A':
                        if (catagory != null) {
                            catagory.add(artist = new DefaultMutableTreeNode(line.substring(2)));
                        }
                        break;
                    case 'R':
                        if (artist != null) {
                            artist.add(record = new DefaultMutableTreeNode(line.substring(2)));
                        }
                        break;
                    case 'S':
                        if (record != null) {
                            record.add(new DefaultMutableTreeNode(line.substring(2)));
                        }
                        break;
                    default:
                        break;
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
        }
        for (String item : items()) {
            tabbedPane.addTab(item, null, map.get(item));
        }
        return tabbedPane;
    }

    public static Collection<String> items() {
        assert !map.isEmpty() : "create first";
        List<String> list = new ArrayList<String>(map.keySet());
        Collections.sort(list);
        return list;
    }

    public static void select(String key) {
        assert !map.isEmpty() : "create first";
        tabbedPane.setSelectedComponent(map.get(key));
    }
}
