/* 
* ################################################################
* 
* ProActive: The Java(TM) library for Parallel, Distributed, 
*            Concurrent computing with Security and Mobility
* 
* Copyright (C) 1997-2002 INRIA/University of Nice-Sophia Antipolis
* Contact: proactive-support@inria.fr
* 
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or any later version.
*  
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
* 
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
* USA
*  
*  Initial developer(s):               The ProActive Team
*                        http://www.inria.fr/oasis/ProActive/contacts.html
*  Contributor(s): 
* 
* ################################################################
*/ 
package org.objectweb.proactive.ic2d.gui.process;

import org.objectweb.proactive.core.process.ExternalProcess;   
import org.objectweb.proactive.core.process.globus.GlobusProcess;


public class GlobusProcessControlFrame extends javax.swing.JFrame {

  private static final int DEFAULT_WIDTH = 600;
  private static final int DEFAULT_HEIGHT = 500;
  
  //
  // -- CONTRUCTORS -----------------------------------------------
  //

  public GlobusProcessControlFrame() {
    super("Globus Execution Control");
    this.setSize(new java.awt.Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
    
    // install Remote Control Panel
    java.awt.Container c = getContentPane();
    c.setLayout(new java.awt.BorderLayout());
    c.add(new GlobusProcessControlPanel(), java.awt.BorderLayout.CENTER);
  }

  public GlobusProcessControlFrame(ExternalProcess  ep) {
    super("Globus Execution Control");
    this.setSize(new java.awt.Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
    if (! this.isVisible()) {
	this.setVisible(true);
    }    
    // install Remote Control Panel
    java.awt.Container c = getContentPane();
    c.setLayout(new java.awt.BorderLayout());
    c.add(new GlobusProcessControlPanel((GlobusProcess)ep), java.awt.BorderLayout.CENTER);

  }
  
  

  //
  // -- PUBLIC METHODS -----------------------------------------------
  //
  
  public static void main(String[] args) {
    GlobusProcessControlFrame frame = new GlobusProcessControlFrame();
    // Listeners
    frame.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent e) {
        System.exit(0);
      }
    });
    frame.setVisible(true);
  }

  //
  // -- INNER CLASSES -----------------------------------------------
  //



}

