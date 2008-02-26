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

package org.jpc.emulator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.jpc.emulator.memory.LinearAddressSpace;
import org.jpc.emulator.memory.PhysicalAddressSpace;
import org.jpc.emulator.memory.codeblock.CodeBlock;
import org.jpc.emulator.pci.peripheral.VGACard;
import org.jpc.emulator.pci.peripheral.VideoCard;
import org.jpc.emulator.peripheral.Keyboard;
import org.jpc.emulator.peripheral.UserInputDevice;
import org.jpc.emulator.processor.Processor;
import org.jpc.support.BlockDevice;
import org.jpc.support.Clock;
import org.jpc.support.DriveSet;
import org.jpc.test.Checkpoint;

/**
 * The main parent class for JPC.
 */
public interface PC {
	public void start();

	public void stop();

	public void dispose();

	public void setFloppy(org.jpc.support.BlockDevice drive, int i);

	public void runBackgroundTasks();

	public DriveSet getDrives();

	public BlockDevice getBootDevice();

	public int getBootType();

	public boolean saveState(ZipOutputStream zip) throws IOException;

	public void loadState(File f) throws IOException;

	public void reset();

	public UserInputDevice getKeyboard();

	public Processor getProcessor();

	public VideoCard getGraphicsCard();

	public PhysicalAddressSpace getPhysicalMemory();

	public LinearAddressSpace getLinearMemory();

	public Clock getSystemClock();

	public int execute();

	public CodeBlock decodeCodeBlockAt(int address);

	public int executeStep();
}
