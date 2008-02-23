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

package org.jpc.emulator.memory.codeblock.fastcompiler;

import java.io.*;
import java.util.*;

import org.jpc.classfile.*;
import org.jpc.emulator.processor.*;

public abstract class ExceptionHandler
{
    private Map rootNodes;
    private RPNNode initialNode;

    private int minPC, maxPC;

    private int lastX86Position;

    public ExceptionHandler(int lastX86Position, RPNNode initialNode, Map stateMap)
    {
	rootNodes = stateMap;
	for (int i = FASTCompiler.PROCESSOR_ELEMENT_COUNT; i < FASTCompiler.ELEMENT_COUNT; i++)
	    rootNodes.remove(new Integer(i));

	this.lastX86Position = lastX86Position;
	this.initialNode = initialNode;

	minPC = Integer.MAX_VALUE;
	maxPC = Integer.MIN_VALUE;
    }

    public int getX86Index()
    {
	return initialNode.getX86Index();
    }

    public void assignRange(int min, int max)
    {
	minPC = Math.min(minPC, min);
	maxPC = Math.max(maxPC, max);
    }

    public boolean used()
    {
	return (minPC != Integer.MAX_VALUE);
    }

    public int start() { return minPC; }

    public int end() { return maxPC; }

    public void write(CountingOutputStream byteCodes, ClassFile cf) throws IOException
    {
	int affectedCount = 0;
	for (Iterator itt = rootNodes.values().iterator(); itt.hasNext();) {
            RPNNode rpn = (RPNNode) itt.next();

            if (rpn.getMicrocode() == -1)
                continue;

	    rpn.reset(minPC);
            affectedCount++;
        }
 
        int index = 0;
        RPNNode[] roots = new RPNNode[affectedCount];

        for (Iterator itt = rootNodes.values().iterator(); itt.hasNext();) {
            RPNNode rpn = (RPNNode) itt.next();            
            if ((rpn.getMicrocode() == -1) || (rpn.getID() == FASTCompiler.PROCESSOR_ELEMENT_EIP))
		continue;
	    
	    rpn.writeExceptionCleanup(byteCodes, cf, true);
	    roots[index++] = rpn;
        }
	
	for (int i=index-1; i>=0; i--)
	    RPNNode.writeBytecodes(byteCodes, cf, BytecodeFragments.popCode(roots[i].getID()));
	
	
	RPNNode.writeBytecodes(byteCodes, cf, BytecodeFragments.pushCode(FASTCompiler.PROCESSOR_ELEMENT_EIP));
// 	byteCodes.write(cf.addToConstantPool(new Integer(initialNode.getX86Position())));
	int cpIndex = cf.addToConstantPool(new Integer(lastX86Position));
	if (cpIndex < 0xff) {
	    byteCodes.write(JavaOpcode.LDC);
	    byteCodes.write(cpIndex);
	} else {
	    byteCodes.write(JavaOpcode.LDC_W);
	    byteCodes.write(cpIndex >>> 8);
	    byteCodes.write(cpIndex & 0xff);
	}
	byteCodes.write(JavaOpcode.IADD);
	RPNNode.writeBytecodes(byteCodes, cf, BytecodeFragments.popCode(FASTCompiler.PROCESSOR_ELEMENT_EIP));

	writeHandlerRoutine(byteCodes, cf);

        byteCodes.write(JavaOpcode.ILOAD);
        byteCodes.write(FASTCompiler.VARIABLE_EXECUTE_COUNT_INDEX);
        byteCodes.write(JavaOpcode.IRETURN);
    }

    protected abstract void writeHandlerRoutine(CountingOutputStream byteCodes, ClassFile cf) throws IOException;
}
