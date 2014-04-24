/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package hello;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

public class TestPackager {

    private static void createAndShowGUI() {
        //Create and set up the window.
        try {
            String preferencesId = System.getProperty("app.preferences.id");
            Preferences node = Preferences.userRoot().node(preferencesId);
            Preferences jvmOptions = node.node("JVMUserOptions");
            String[] keys = jvmOptions.keys();
            for (String key: keys) {
                System.out.println("key:" + key);
            }
            jvmOptions.putLong("lastRunMs", System.currentTimeMillis());
            node.flush();
        } catch (Exception ex) {
            Logger.getLogger(TestPackager.class.getName()).log(Level.WARNING, null, ex);
        }

        JFrame frame = new JFrame("Display Parameters");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setBounds(0, 0, dim.width / 4, dim.height / 4);


        long v = Runtime.getRuntime().maxMemory();
        Long value = v / 1048576;
        long t = Runtime.getRuntime().totalMemory();
        Long total = t / 1048576;

        JLabel label = new JLabel("Max" + value.toString() + "  Total"
                + total.toString());

        RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = RuntimemxBean.getInputArguments();

        JList<String> list = new JList<>(arguments.toArray(new String[arguments.size()]));
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(-1);
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(400, 200));
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 10, 30, 30));
        panel.add(label, BorderLayout.NORTH);
        panel.add(listScroller, BorderLayout.CENTER);

        frame.getContentPane().add(panel);
        frame.setLocationRelativeTo(null);

        //Display the window.
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(TestPackager::createAndShowGUI);
    }
}
