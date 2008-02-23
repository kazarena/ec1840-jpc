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

package org.jpc.j2se;

import java.util.*;
import java.util.zip.*;
import java.util.jar.*;
import java.io.*;
import java.beans.*;
import java.awt.*;
import java.text.*;
import java.net.*;
import java.awt.color.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.Desktop;
import java.awt.Toolkit;

import javax.swing.*;
import javax.swing.event.*;

import org.jpc.emulator.processor.*;
import org.jpc.emulator.*;
import org.jpc.support.*;
import org.jpc.emulator.motherboard.*;
import org.jpc.emulator.memory.*;
import org.jpc.emulator.memory.codeblock.*;
import org.jpc.emulator.peripheral.*;
import org.jpc.emulator.pci.peripheral.*;

  
public class JPCApplication extends PCMonitorFrame
{
    public static final int WIDTH = 720;
    public static final int HEIGHT = 400 + 100;
    private static final String[] defaultArgs = { "-fda", "mem:floppy.img", "-hda", "mem:dosgames.img", "-boot", "fda"};
//     private static final String[] defaultArgs = { "-hda", "mem:linux.img", "-boot", "hda"};
    private static final String aboutUsText = 
        "JPC: Developed since August 2005 in Oxford University's Subdepartment of Particle Physics.\n\n" + 
        "For more information visit our website at:\nhttp://www-jpc.physics.ox.ac.uk";
    private static final  String defaultLicence = 
        "JPC is released under GPL Version 2 and comes with absoutely no warranty<br/><br/>" +
        "See www-jpc.physics.ox.ac.uk for more details";
    

    private boolean running = false;
    private JMenuItem load, image, aboutUs, gettingStarted;
    private JMenuItem loadSnapshot, saveSnapshot;
    private JMenuItem changeFloppyA, changeFloppyB;
    private JMenuItem dosgamesImage, moregamesImage, mousegamesImage;

    private JEditorPane licence, instructionsText;
    private JScrollPane monitorPane;

    private static JFileChooser floppyImageChooser, diskImageChooser, diskDirChooser,snapshotChooser;

    
    public JPCApplication(String[] args, PC pc) throws Exception
    {
        super("JPC - " + ArgProcessor.findArg(args, "hda" , null), pc, args);

        String snapShot = ArgProcessor.findArg(args, "ss" , null);
        if (snapShot != null)
        {
            //load PC snapshot
            File f = new File(snapShot);
            System.out.println("Loading a snapshot of JPC");
            pc.loadState(f);
            System.out.println("Loading data");
            pc.getGraphicsCard().resizeDisplay(monitor);
            monitor.loadState(f);
            System.out.println("done");
        }

        JMenuBar bar = getJMenuBar();

        JMenu snap = new JMenu("Snapshot");
        saveSnapshot = snap.add("Save Snapshot");
        saveSnapshot.addActionListener(this);
        loadSnapshot = snap.add("Load Snapshot");
        loadSnapshot.addActionListener(this);
        bar.add(snap);

        JMenu drives = new JMenu("Drives");
        changeFloppyA = drives.add("Change Floppy A");
        changeFloppyA.addActionListener(this);
        changeFloppyB = drives.add("Change Floppy B");
        changeFloppyB.addActionListener(this);
        bar.add(drives);

        JMenu imageMenu = new JMenu("Disk Images");
        JMenu includedMenu= new JMenu("Included Images");
        dosgamesImage = includedMenu.add("dosgames.img");
        dosgamesImage.addActionListener(this);
        moregamesImage = includedMenu.add("moregames.img");
        moregamesImage.addActionListener(this);
        mousegamesImage = includedMenu.add("mousegames.img");
        mousegamesImage.addActionListener(this);

        // includedMenu for when bundling .img file with jar file
        imageMenu.add(includedMenu);

        load = imageMenu.add("Select directory");
        load.addActionListener(this);
        image = imageMenu.add("Load Hard Drive Image");
        image.addActionListener(this);
        bar.add(imageMenu);

        JMenu help = new JMenu("Help");
        gettingStarted = help.add("Getting Started");
        gettingStarted.addActionListener(this);
        aboutUs = help.add("About JPC");
        aboutUs.addActionListener(this);
        bar.add(help);

        floppyImageChooser =  new JFileChooser(System.getProperty("user.dir"));
        floppyImageChooser.setApproveButtonText("Load Floppy Drive Image");
        diskImageChooser = new JFileChooser(System.getProperty("user.dir"));
        diskImageChooser.setFileFilter(new ImageFileFilter());
        diskImageChooser.setApproveButtonText("Load Hard Drive Image");
        diskDirChooser = new JFileChooser(System.getProperty("user.dir"));
        diskDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        diskDirChooser.setApproveButtonText("Open Directory");
        snapshotChooser = new JFileChooser(System.getProperty("user.dir"));
        snapshotChooser.setApproveButtonText("Load JPC Snapshot");

        try
        {
            licence = new JEditorPane(ClassLoader.getSystemResource("resource/licence.html"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            try
            {
                licence = new JEditorPane("text/html", defaultLicence);
            }
            catch (Exception f) {}
        }
        licence.setEditable(false);
        getMonitorPane().setViewportView(licence);

        getContentPane().add("South", new KeyTypingPanel(monitor));

        getContentPane().validate();
    }

    protected synchronized void start()
    {
        super.start();

        getMonitorPane().setViewportView(monitor);
        monitor.validate();
        monitor.requestFocus();
    }

    private void load(String loadString, JFileChooser fileChooser, boolean reboot)
    {
        int load = 0;
        if (reboot)
            load = JOptionPane.showOptionDialog(this, "Selecting " + loadString + " now will cause JPC to reboot. Are you sure you want to continue?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[] {"Continue","Cancel"}, "Continue");
        else
            load = JOptionPane.showOptionDialog(this, "Selecting " + loadString + " now will lose the current state of JPC. Are you sure you want to continue?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[] {"Continue","Cancel"}, "Continue");
            
        System.out.println("load = " + load);

        if (load == 0)
        {
            if (running)
                stop();

            int returnVal = 0;
            if (fileChooser != null)
                returnVal = fileChooser.showDialog(this, null);
            
            if (returnVal == 0)
            {
                try
                {
                    if (fileChooser == null)
                    {
                        JarFile jarFile = new JarFile("JPC.jar");
                        InputStream in = jarFile.getInputStream(jarFile.getEntry(loadString));
                        File outFile = File.createTempFile(loadString, null);
                        outFile.deleteOnExit();
                        OutputStream out = new FileOutputStream(outFile);
                        
                        byte[] buffer = new byte[2048];
                        while (true)
                        {
                            int r = in.read(buffer);
                            if (r < 0)
                                break;
                            out.write(buffer, 0, r);
                        }
                        
                        in.close();
                        out.close();
                        jarFile.close();
                        
                        SeekableIODevice ioDevice = new FileBackedSeekableIODevice(outFile.getPath());
                        pc.getDrives().setHardDrive(0, new RawBlockDevice(ioDevice));
                        
                        setTitle("JPC - " + loadString);
                    }
                    else 
                    {
                        File file = fileChooser.getSelectedFile();
                        if (fileChooser == diskDirChooser)
                        {
                            BlockDevice hda = new TreeBlockDevice(file, true);
                            DriveSet drives = pc.getDrives();
                            drives.setHardDrive(0, hda);
                            setTitle("JPC - " + file);
                        }
                        else if (fileChooser == diskImageChooser)
                        {
                            System.out.println("loading image");

                            BlockDevice device = null;
                            Class blockClass = Class.forName("org.jpc.support.FileBackedSeekableIODevice");
                            SeekableIODevice ioDevice = (SeekableIODevice)(blockClass.newInstance());
                            ioDevice.configure(file.getPath());
                            device = new RawBlockDevice(ioDevice);
                            DriveSet drives = pc.getDrives();
                            drives.setHardDrive(0, device);
                        
                            setTitle("JPC - " + file);
                        }
                        else if (fileChooser == snapshotChooser)
                        {
                            /*DataInputStream in = new DataInputStream(zip.getInputStream(entry));
                              int len = in.readInt();
                              String[] args = new String[len];
                              for (int i=0; i<len; i++)
                              args[i] = in.readUTF();
                              zip.close();*/
                            //pc = PC.createPC(args, new VirtualClock()); 
                            //monitor = new PCMonitor(pc);
                            System.out.println("Loading a snapshot of JPC");
                            pc.loadState(file);
                            System.out.println("Loading data");
                            pc.getGraphicsCard().resizeDisplay(monitor);
                            monitor.loadState(file);
                            System.out.println("done");
                        }
                    }
                }
                catch (IndexOutOfBoundsException e)
                {
                    //there were too many files in the directory tree selected
                    System.out.println("too many files");
                    JOptionPane.showMessageDialog(this, "The directory you selected contains too many files. Try selecting a directory with fewer contents.", "Error loading directory", JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
                catch (Exception e)
                {
                    System.err.println(e);
                }
            }
            
            monitor.stopUpdateThread();
            if (reboot)
                pc.reset();
            monitor.revalidate();
            monitor.requestFocus();
            
            if (reboot)
                reset();
        }
    }

    private void saveSnapShot()
    {
        if (running)
            stop();
        int returnVal = snapshotChooser.showDialog(this, "Save JPC Snapshot");
        File file = snapshotChooser.getSelectedFile();
        
        if (returnVal == 0)
            try
            {
                DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
                ZipOutputStream zip = new ZipOutputStream(out);
                
                pc.saveState(zip);
                monitor.saveState(zip);
                zip.close();
            }
            catch (Exception e)
            {
                System.err.println(e);
            }
        
        start();
    }

    private void showAboutUs()
    {
        Object[] buttons = {"Visit our Website", "Ok"};
        int i =JOptionPane.showOptionDialog(this, aboutUsText, "JPC info", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, buttons, buttons[1]);
        if (i == 0)
        {
            Desktop desktop = null;
            if (Desktop.isDesktopSupported()) 
            {
                desktop = Desktop.getDesktop();
                try
                {
                    desktop.browse(new URI("http://www-jpc.physics.ox.ac.uk"));
                }
                catch (Exception e)
                {
                    System.err.println(e);
                }
            }
        }
    }

    private void changeFloppy(int i)
    {
        int returnVal = floppyImageChooser.showDialog(this, "Load Floppy Drive Image");
        File file = floppyImageChooser.getSelectedFile();
        
        if (returnVal == 0)
            try
            {
                BlockDevice device = null;
                Class blockClass = Class.forName("org.jpc.support.FileBackedSeekableIODevice");
                SeekableIODevice ioDevice = (SeekableIODevice)(blockClass.newInstance());
                ioDevice.configure(file.getPath());
                device = new RawBlockDevice(ioDevice);
                pc.setFloppy(device, i);
            }
            catch (Exception e)
            {
                System.err.println(e);
            }
    }

    public void actionPerformed(ActionEvent evt)
    {
        super.actionPerformed(evt);

        if (evt.getSource() == doubleSize)
        {
            if (doubleSize.isSelected())
                setBounds(100, 100, (WIDTH*2)+20, (HEIGHT*2)+70);
            else
                setBounds(100, 100, WIDTH+20, HEIGHT+70);
        }
        else if (evt.getSource() == load)
            load("a directory", diskDirChooser, true);
        else if (evt.getSource() == image)
        {
            System.out.println("received image event");
            load("a disk image", diskImageChooser, true);
        }
        else if ((evt.getSource() == dosgamesImage) || (evt.getSource() == moregamesImage) || (evt.getSource() == mousegamesImage))
        {
            String fileName = "dosgames.img";
            if (evt.getSource() == moregamesImage)
                fileName = "moregames.img";
            else if (evt.getSource() == mousegamesImage)
                fileName = "mousegames.img";
            load(fileName, null, true);
        }
        else if (evt.getSource() == loadSnapshot)
            load("a snapshot", snapshotChooser, false);
        else if (evt.getSource() == saveSnapshot)
            saveSnapShot();
        else if (evt.getSource() == changeFloppyA)
            changeFloppy(0);
        else if (evt.getSource() == changeFloppyB)
            changeFloppy(1);
        else if (evt.getSource() == gettingStarted)
        {
            stop();
            getMonitorPane().setViewportView(licence);
        }
        else if (evt.getSource() == aboutUs)
            showAboutUs();
    }
    
    private static class ImageFileFilter extends javax.swing.filechooser.FileFilter
    {
        public boolean accept(File f) 
        {
            if (f.isDirectory()) 
                return true;
                
            String extension = getExtension(f);
            if ((extension != null) && (extension.equals("img")))
                return true;
            return false;
        }
            
        private String getExtension(File f) 
        {
            String ext = null;
            String s = f.getName();
            int i = s.lastIndexOf('.');
                
            if (i > 0 &&  i < s.length() - 1) 
            {
                ext = s.substring(i+1).toLowerCase();
            }
            return ext;
        }

        public String getDescription()
        {
            return "Shows disk image files and directories";
        }
    }


    public static void main(String[] args) throws Exception
    { 
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {}

        if (args.length == 0)
            args = defaultArgs;
        
        PC pc = PC.createPC(args, new VirtualClock()); 
        JPCApplication app = new JPCApplication(args, pc);
        
        app.setBounds(100, 100, WIDTH+20, HEIGHT+70);
        try
        {
            app.setIconImage(Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("resource/jpcicon.png")));
        }
        catch (Exception e) {}
        
        app.validate();
        app.setVisible(true);
    }
}
