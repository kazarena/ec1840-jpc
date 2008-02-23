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

public class BlankCodeBlock implements RealModeCodeBlock, ProtectedModeCodeBlock, Virtual8086ModeCodeBlock
{
    protected int x86Count, x86Length;

    private static final RuntimeException executeException = new NullPointerException();

    public BlankCodeBlock(int x86Count, int x86Length)
    {
        this.x86Count = x86Count;
        this.x86Length = x86Length;
    }

    public int getX86Length()
    {
        return x86Length;
    }

    public int getX86Count()
    {
        return x86Count;
    }

    public int execute(Processor cpu)
    {
	throw executeException;
    }

    public boolean handleMemoryRegionChange(int startAddress, int endAddress)
    {
        return false;
    }

    public String getDisplayString()
    {
        return "\n\n<<Blank Block>>\n\n";
    }

    public String toString()
    {
        return " -- Blank --\n";
    }
}
