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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.jpc.debugger.util.BasicTableModel;
import org.jpc.debugger.util.UtilityFrame;
import org.jpc.emulator.memory.AddressSpace;
import org.jpc.emulator.memory.codeblock.AbstractCodeBlockWrapper;
import org.jpc.emulator.memory.codeblock.CodeBlock;
import org.jpc.emulator.memory.codeblock.optimised.MicrocodeSet;
import org.jpc.emulator.memory.codeblock.optimised.RealModeUBlock;

public class OpcodeFrequencyFrame extends UtilityFrame implements PCListener, ActionListener, CodeBlockListener, Comparator
{
    private long unknownBlockCount, unknownX86Count;

    private CodeBlockRecord record;
    private HashMap<Integer, OpcodeEntry> frequencies;
    private String[] uCodeNames;

    private OpcodeEntry[] frequentCodes;
    private FrequencyModel model;

    public OpcodeFrequencyFrame() 
    {
        super("Opcode Frequencies");
        frequencies = new HashMap<Integer, OpcodeEntry>();
        record = null;
        uCodeNames = MicrocodeOverlayTable.extractOrdinalNames(MicrocodeSet.class);
        frequentCodes = new OpcodeEntry[1000];

        setPreferredSize(new Dimension(600, 400));
        JPC.getInstance().objects().addObject(this);

        model = new FrequencyModel();
        JTable table = new JTable(model);
        model.setupColumnWidths(table);

        add("Center", new JScrollPane(table));
    }

    class OpcodeEntry
    {
        int op;
        int frequency;
        String opName;

        OpcodeEntry(int position, int[] uCodes, RealModeUBlock b)
        { 
            frequency = 0;

            op = uCodes[position++];
            try
            {
                opName = uCodeNames[op];
            }
            catch (Exception e) 
            {
                opName = "{"+op+"}";
            }
        }
        
        public int hashcode()
        {
            return op;
        }

        public boolean equals(Object obj)
        {
            OpcodeEntry e = (OpcodeEntry) obj;
            if (e.op != op)
                return false;

            return true;
        }

        public String toString()
        {
            return opName;
        }
    }

    public void actionPerformed(ActionEvent evt) {}

    public void codeBlockDecoded(int address, AddressSpace memory, CodeBlock block) {}

    public synchronized void codeBlockExecuted(int address, AddressSpace memory, CodeBlock block)
    {
	while (block instanceof AbstractCodeBlockWrapper)
	    block = ((AbstractCodeBlockWrapper)block).getBlock();

	if (!(block instanceof RealModeUBlock))
        {
            unknownBlockCount++;
            unknownX86Count += block.getX86Count();
            return;
        }

        try
        {
            RealModeUBlock b = (RealModeUBlock) block;
            int[] uCodes = b.getMicrocodes();
            for (int i=0; i<uCodes.length; i++)
            {
                OpcodeEntry e = frequencies.get(new Integer(uCodes[i]));
                if (e == null)
                {
                    e = new OpcodeEntry(i, uCodes, b);
                    frequencies.put(new Integer(uCodes[i]), e);
                }
                // if we are on an immediate byte increment i again to skip the following number
                String end = e.opName;
                end = end.substring(end.length()-2, end.length());
                if ((end.compareTo("IB")==0) || (end.compareTo("IW")==0) || (end.compareTo("ID")==0))
                    i++;
                e.frequency++;
            }
        }
        catch (Exception e)
        {
            System.out.println("-----------------------------");
            System.out.println("WARNING: "+e+" in Frequency Calculation");
            System.out.println(block.getClass()+"\n"+block);
            e.printStackTrace();
        }
    }

    public synchronized void frameClosed()
    {
        JPC.getInstance().objects().removeObject(this);
        if (record != null)
            record.setCodeBlockListener(null);
    }

    public void PCCreated()
    {
        refreshDetails();
    }

    public void PCDisposed()
    {
        record = null;
        model.fireTableDataChanged();
    }
    
    public void executionStarted() {}

    public void executionStopped() 
    {
        refreshDetails();
    }

    public int compare(Object o1, Object o2)
    {
        if (o1 == null)
        {
            if (o2 == null)
                return 0;
            else 
                return 1;
        }
        else if (o2 == null)
            return -1;

        OpcodeEntry e1 = (OpcodeEntry) o1;
        OpcodeEntry e2 = (OpcodeEntry) o2;
        
        return e2.frequency - e1.frequency;
    }

    public synchronized void refreshDetails()
    {
        CodeBlockRecord r = (CodeBlockRecord) JPC.getObject(CodeBlockRecord.class);
        if (r != record)
        {
            if (record != null)
                record.setCodeBlockListener(null);
            record = r;
            record.setCodeBlockListener(this);

            unknownBlockCount = unknownX86Count = 0;
            frequencies = new HashMap<Integer, OpcodeEntry>();
        }

        Object[] buffer = frequencies.values().toArray();
        Arrays.sort(buffer, this);
        Arrays.fill(frequentCodes, null);

        int limit = Math.min(buffer.length, frequentCodes.length);
        System.arraycopy(buffer, 0, frequentCodes, 0, limit);
        model.fireTableDataChanged();
    }

    class FrequencyModel extends BasicTableModel
    {
        FrequencyModel()
        {
            super(new String[]{"Rank", "Opcode", "Frequency"}, new int[]{80, 200, 80});
        }

        public int getRowCount()
        {
            return frequentCodes.length;
        }

        public Object getValueAt(int row, int column)
        {
            OpcodeEntry e = frequentCodes[row];
            if (e == null)
                return null;

            switch (column)
            {
	    case 0:
		return new Integer(row);
            case 1:
                return e.toString();
            case 2:
                return new Integer(e.frequency);
            }

            return null;
        }
    }
}
