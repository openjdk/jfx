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

import jdk.packager.services.UserJvmOptionsService;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.JOptionPane;

public class TestPackager {

    private static String[] args;

    private static void createAndShowGUI() {
        //Create and set up the window.
        UserJvmOptionsService ujo = UserJvmOptionsService.getUserJVMDefaults();
        Map<String, String> userOptions = ujo.getUserJVMOptions();

        for (Map.Entry <String, String> entry : userOptions.entrySet()) {
            System.out.println("key:" + entry.getKey() + " value:" + entry.getValue());
        }
        if (!userOptions.containsKey("-DfirstRunMs=")) {
            userOptions.put("-DfirstRunMs=", Long.toString(System.currentTimeMillis()));
        }
        userOptions.put("-DlastRunMs=", Long.toString(System.currentTimeMillis()));
        ujo.setUserJVMOptions(userOptions);

        JFrame frame = new JFrame("Display Parameters");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setBounds(0, 0, dim.width / 4, dim.height / 4);


        long v = Runtime.getRuntime().maxMemory();
        Long value = v / 1048576;
        long t = Runtime.getRuntime().totalMemory();
        Long total = t / 1048576;

        JLabel label = new JLabel("Max: " + value.toString() + "m"
                                  + "  Total: " + total.toString() + "m");

        RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = RuntimemxBean.getInputArguments();

        JList<String> list = new JList<>(arguments.toArray(new String[arguments.size()]));
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(400, 200));

        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Key");
        model.addColumn("Effective");
        model.addColumn("Default");
        model.addTableModelListener(new TableModelListener() {
          public void tableChanged(TableModelEvent e) {
             System.out.println(e);

             switch (e.getType()) {
               case TableModelEvent.UPDATE:
                 int column = e.getColumn();
                 int row = e.getFirstRow();

                 if (column == 1) {
                   String key = model.getValueAt(row, 0).toString();
                   String value = model.getValueAt(row, column).toString();
                   JOptionPane.showMessageDialog(null, key + "=" + value + " column=" + String.valueOf(column) + " row=" + String.valueOf(row), "Changed", JOptionPane.INFORMATION_MESSAGE);
                   UserJvmOptionsService ujo = UserJvmOptionsService.getUserJVMDefaults();
                   Map<String, String> userOptions = ujo.getUserJVMOptions();
                   userOptions.put(key, value);
                   ujo.setUserJVMOptions(userOptions);
                 }
                 break;
             }
          }
        });

        Map<String, String> defaults = ujo.getUserJVMOptionDefaults();
        for (Map.Entry <String, String> entry : userOptions.entrySet()) {
            String def = defaults.get(entry.getKey());
            model.addRow(new Object[] {entry.getKey(), entry.getValue(), def == null ? "<no default>" : def});
        }
        JTable prefs = new JTable(model);
        JScrollPane prefsScroller = new JScrollPane(prefs);
        prefsScroller.setPreferredSize(new Dimension(400, 100));

        JList<String> argList = new JList<>(args);
        argList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        argList.setLayoutOrientation(JList.VERTICAL);
        JScrollPane argListScroller = new JScrollPane(argList);
        argListScroller.setPreferredSize(new Dimension(400, 100));


        Box box = Box.createVerticalBox();

        box.add(new JLabel("JVM Arguments (user, options, and properties"));
        box.add(Box.createVerticalStrut(5));
        box.add(listScroller);

        box.add(Box.createVerticalStrut(10));
        box.add(new JLabel("User JVM Options, as set and with defaults"));
        box.add(Box.createVerticalStrut(5));
        box.add(prefsScroller);

        box.add(Box.createVerticalStrut(10));
        box.add(new JLabel("Command Line Arguments"));
        box.add(Box.createVerticalStrut(5));
        box.add(argListScroller);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 10, 30, 30));
        panel.add(label, BorderLayout.NORTH);

        panel.add(box, BorderLayout.CENTER);
        panel.setSize(panel.getPreferredSize());

        frame.getContentPane().add(panel);
        frame.setLocationRelativeTo(null);
        frame.setSize(frame.getPreferredSize());

        //Display the window.
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        TestPackager.args = args;
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(TestPackager::createAndShowGUI);
    }
}
