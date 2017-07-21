/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import jdk.packager.services.singleton.SingleInstanceService;
import jdk.packager.services.singleton.SingleInstanceListener;


public class HelloWorld implements SingleInstanceListener {

  @Override
  public void newActivation(String... params) {
      for (int i = 0; i < params.length; i++) {
          System.out.println("Arg[" + i + "]: " + params[i]);
      }
  }

  private static void createAndShowGUI() {
    //Create and set up the window.
    JFrame frame = new JFrame("Hello World");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    frame.setBounds(0, 0, dim.width/2, dim.height/2);

    //Add the ubiquitous "Hello World" label.
    JLabel label = new JLabel("Hello World");
    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createEmptyBorder(30, 10, 30, 30));
    panel.add(label);

    frame.getContentPane().add(panel);

    //Display the window.
    frame.setVisible(true);
  }

  public void launchApp(String[] args) {
    //the app will be single instance only if the option
    //"singleton" is specified in the .xml file for ant
    SingleInstanceService.registerSingleInstance(HelloWorld.this);

    Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
            SingleInstanceService.unregisterSingleInstance(HelloWorld.this);
        }
    });

    //Schedule a job for the event-dispatching thread:
    //creating and showing this application's GUI.
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI();
      }
    });
  }

  public static void main(String[] args) {
    new HelloWorld().launchApp(args);
  }
}
