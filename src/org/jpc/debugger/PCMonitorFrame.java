/*
    JPC: A x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 2.0

    A project from the Physics Dept, The University of Oxford

    Copyright (C) 2007 Isis Innovation Limited

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 2 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 
    Details (including contact information) can be found at: 

    www.physics.ox.ac.uk/jpc
*/


package org.jpc.debugger;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jpc.debugger.util.UtilityFrame;
import org.jpc.emulator.PC;
import org.jpc.j2se.PCMonitor;

public class PCMonitorFrame extends UtilityFrame implements PCListener
{
    private PC currentPC;
    private PCMonitor monitor;
    private JScrollPane main;

    public PCMonitorFrame()
    {
        super("PC Monitor");

        currentPC = null;
        monitor = null;
        main = new JScrollPane();

        add("Center", main);
        setPreferredSize(new Dimension(PCMonitor.WIDTH + 20, PCMonitor.HEIGHT + 40));
        JPC.getInstance().objects().addObject(this);
    }

    public void loadMonitorState(File f) throws IOException
    {
        monitor.loadState(f);
    }

    public void resizeDisplay()
    {
        currentPC.getGraphicsCard().resizeDisplay(monitor);
    }
    
    public void saveState(ZipOutputStream zip) throws IOException
    {
        monitor.saveState(zip);
    }

    public void frameClosed()
    {
        if (monitor != null)
            monitor.stopUpdateThread();
        JPC.getInstance().objects().removeObject(this);
    }

    public void PCCreated() {}

    public void PCDisposed()
    {
        dispose();
    }
    
    public void executionStarted() {}

    public void executionStopped() {}

    public void refreshDetails() 
    {
        PC pc = (PC) JPC.getObject(PC.class);
        if (pc != currentPC)
        {
            if (monitor != null)
            {
                monitor.stopUpdateThread();
                main.setViewportView(new JPanel());
            }

            currentPC = pc;
            if (pc != null)
            {
                monitor = new PCMonitor(pc);
                monitor.startUpdateThread();
                main.setViewportView(monitor);
                monitor.revalidate();
                main.revalidate();
                monitor.requestFocus();
            }
        }
    }
}
