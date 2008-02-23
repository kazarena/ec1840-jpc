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

package org.jpc.emulator.memory.codeblock;

import org.jpc.emulator.processor.*;
import org.jpc.emulator.memory.*;

public class SpanningRealModeCodeBlock extends SpanningCodeBlock implements RealModeCodeBlock
{
    private ByteSourceWrappedMemory byteSource = new ByteSourceWrappedMemory();

    private CodeBlockFactory[] factories;

    public SpanningRealModeCodeBlock(CodeBlockFactory[] factories)
    {
	this.factories = factories;
    }

    protected CodeBlock decode(Processor cpu)
    {
	RealModeCodeBlock block = null;
	AddressSpace memory = cpu.physicalMemory;
	int address = cpu.getInstructionPointer();

	for (int i = 0; (i < factories.length) && (block == null); i++) {
	    try {
		byteSource.set(memory, address);
		block = factories[i].getRealModeCodeBlock(byteSource);
	    } catch (IllegalStateException e) {}
	}

	return block;
    }

    public String getDisplayString()
    {
	return "Spanning Real Mode CodeBlock";
    }
}
