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

public abstract class SpanningCodeBlock implements CodeBlock
{
    private CodeBlock lastBlock;

    public int getX86Length()
    {
        return 0;
    }

    public int getX86Count()
    {
	try {
	    return lastBlock.getX86Count();
	} catch (NullPointerException e) {
	    return 0;
	}
    }
    
    // Returns the number of equivalent x86 instructions executed. Negative results indicate an error
    public int execute(Processor cpu)
    {
	lastBlock = decode(cpu);
	return lastBlock.execute(cpu);
    }

    protected abstract CodeBlock decode(Processor cpu);

    public boolean handleMemoryRegionChange(int startAddress, int endAddress)
    {
        return true;
    }
}
