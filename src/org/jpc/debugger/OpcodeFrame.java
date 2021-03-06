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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.jpc.debugger.util.BasicTableModel;
import org.jpc.debugger.util.UtilityFrame;
import org.jpc.emulator.PC;
import org.jpc.emulator.memory.AddressSpace;
import org.jpc.emulator.memory.LinearAddressSpace;
import org.jpc.emulator.memory.Memory;
import org.jpc.emulator.memory.PhysicalAddressSpace;
import org.jpc.emulator.memory.codeblock.CodeBlock;
import org.jpc.emulator.memory.codeblock.SpanningCodeBlock;
import org.jpc.emulator.processor.Processor;
import org.jpc.emulator.processor.ProcessorException;

public class OpcodeFrame extends UtilityFrame implements PCListener, ActionListener, ListSelectionListener
{
    private int nextInstructionAddress, currentIndex;
    private BreakpointsFrame breakpoints;
    private LinearAddressSpace linearMemory;
    private PhysicalAddressSpace physicalMemory;
    private Processor processor;
    private PC pc;

    private Font f;
    private CodeBlockRecord codeBlocks;
    private CodeBlockModel codeModel;
    private MemoryBlockTableModel model;
    private JTable memoryBlockTable;
    private MicrocodeOverlayTable codeBlockTable;
    private JMenuItem scrollToCurrent, scrollToSelected, findCodeBlock, findAllCodeBlocks, findCodeBlockFromHere, toggleBPSelected, toggleBPInstruction, decodeAtSelected, showAddress, scrollToNext, scrollToPrevious;
    private JCheckBoxMenuItem trackInstructions;

    public OpcodeFrame() 
    {
        super("Opcode Frame");
        nextInstructionAddress = 0;
        currentIndex = -1;

        JMenuBar bar = new JMenuBar();
        JMenu options = new JMenu("Options");

        trackInstructions = new JCheckBoxMenuItem("Track Instruction Pointer");
        trackInstructions.setSelected(true);
        trackInstructions.addActionListener(this);
        options.add(trackInstructions);

        JMenu actions = new JMenu("Actions");
        scrollToCurrent = actions.add("Scroll to Current Instruction");
        scrollToCurrent.addActionListener(this);

        scrollToSelected = actions.add("Scroll to Selected Instruction");
        scrollToSelected.addActionListener(this);

        scrollToNext = actions.add("Scroll to Next Instruction");
        scrollToNext.addActionListener(this);
        scrollToNext.setAccelerator(KeyStroke.getKeyStroke("ctrl N"));

        scrollToPrevious = actions.add("Scroll to Previous Instruction");
        scrollToPrevious.addActionListener(this);
        scrollToPrevious.setAccelerator(KeyStroke.getKeyStroke("ctrl P"));
        
        actions.addSeparator();

        findCodeBlock = actions.add("Find a CodeBlock in memory...");
        findCodeBlock.addActionListener(this);

        findCodeBlockFromHere = actions.add("Find a CodeBlock in memory (starting here)...");
        findCodeBlockFromHere.addActionListener(this);

        findAllCodeBlocks = actions.add("Find all CodeBlocks in memory...");
        findAllCodeBlocks.addActionListener(this);

        actions.addSeparator();
        decodeAtSelected = actions.add("Decode block from selected address");
        decodeAtSelected.addActionListener(this);
        showAddress = actions.add("Show address");
        showAddress.addActionListener(this);

        JMenu breakPoints = new JMenu("Breakpoints");
        toggleBPSelected = breakPoints.add("Toggle Breakpoint - Selected Row");
        toggleBPSelected.addActionListener(this);
        toggleBPSelected.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0, false));

        toggleBPInstruction = breakPoints.add("Toggle Breakpoint - Current Instruction");
        toggleBPInstruction.addActionListener(this);
        toggleBPInstruction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0, false));

        bar.add(actions);
        bar.add(breakPoints);
        bar.add(options);
        setJMenuBar(bar);
        
        f = new Font("Monospaced", Font.PLAIN, 12);
        model = new MemoryBlockTableModel();
        memoryBlockTable = new JTable(model);
        model.setupColumnWidths(memoryBlockTable);
        memoryBlockTable.getSelectionModel().addListSelectionListener(this);

        codeModel = new CodeBlockModel();
        codeBlockTable = new MicrocodeOverlayTable(codeModel, 2, true);
        codeModel.setupColumnWidths(codeBlockTable);
        codeBlockTable.setRowHeight(18);
        codeBlockTable.setDefaultRenderer(Object.class, new CellRenderer());
        codeBlockTable.getSelectionModel().addListSelectionListener(this);
        codeBlockTable.addMouseListener(new BlockListener());

        JSplitPane sp1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(memoryBlockTable), new JScrollPane(codeBlockTable));
        sp1.setDividerLocation(165);
        sp1.setResizeWeight(0.0f);

        add("Center", sp1);

        setPreferredSize(new Dimension(900, 600));
        JPC.getInstance().objects().addObject(this);
        
        refreshDetails();
        scrollToCurrent.doClick();
    }

    class BlockListener extends MouseAdapter
    {
        public void mouseClicked(MouseEvent evt)
        {
            int row = codeBlockTable.getHeadRowForBlockRect(evt.getPoint());
            if (row < 0)
                return;
            
            codeBlockTable.setRowSelectionInterval(row, row);
        }
    }
    
    public void valueChanged(ListSelectionEvent e) 
    {
        if (linearMemory == null)
            return;

        if (e.getSource() == memoryBlockTable.getSelectionModel())
        {
            int row = memoryBlockTable.getSelectedRow();
            int newBase = row * AddressSpace.BLOCK_SIZE;
            codeModel.setBaseAddress(newBase);
            codeBlockTable.setRowSelectionInterval(0, 0);

            if ((nextInstructionAddress >= codeModel.baseAddress) && (nextInstructionAddress < codeModel.baseAddress + AddressSpace.BLOCK_SIZE))
            {
                row = nextInstructionAddress & AddressSpace.BLOCK_MASK;
                codeBlockTable.setRowSelectionInterval(row, row);
            }
        }

        codeBlockTable.repaint();
    }

    public void actionPerformed(ActionEvent evt)
    {
        Object src = evt.getSource();
        if (evt.getSource() == scrollToCurrent)
        {
            showAddress(nextInstructionAddress);
            setSelectedAddress(nextInstructionAddress);
        }
        else if (evt.getSource() == scrollToSelected)
            showAddress(getSelectedAddress());
        else if (evt.getSource() == scrollToNext)
        {
            currentIndex = Math.min(currentIndex+1, codeBlocks.getTraceLength());
            
            int addressToShow = nextInstructionAddress;
            if ((currentIndex >= 0) && (currentIndex < codeBlocks.getTraceLength()))
                addressToShow = codeBlocks.getBlockAddress(currentIndex);
            
            showAddress(addressToShow);
            setSelectedAddress(addressToShow);
        }
        else if (evt.getSource() == scrollToPrevious)
        {
            currentIndex = Math.max(currentIndex-1, 0);
            
            int addressToShow = 0;
            if (currentIndex < codeBlocks.getTraceLength())
                addressToShow = codeBlocks.getBlockAddress(currentIndex);
            
            showAddress(addressToShow);
            setSelectedAddress(addressToShow);
        }
        else if ((evt.getSource() == findCodeBlock) || (evt.getSource() == findCodeBlockFromHere))
        {
            String codeBlockID = getUserInput("Enter CodeBlockID (eg \"ORFS Block: 24442607\")", "Find CodeBlock");
            if ((codeBlockID == null) || (codeBlockID.length() == 0))
                return;

            int endAddress = (int) linearMemory.getSize();
            if (endAddress == 0)
                endAddress = Integer.MAX_VALUE;
            endAddress--;
            int startAddress = 0;
            if (evt.getSource() == findCodeBlockFromHere)
                startAddress = getSelectedAddress() + 1;
            int address = findCodeBlockID(codeBlockID, startAddress, endAddress);
            if (address == endAddress)
                return;

            showAddress(address);
            setSelectedAddress(address);
        }
        else if (evt.getSource() == findAllCodeBlocks)
        {
            String codeBlockID = getUserInput("Enter CodeBlockID (eg \"ORFS Block: 24442607\")", "Find All CodeBlocks");
            if ((codeBlockID == null) || (codeBlockID.length() == 0))
                return;

            int count = 0;
            int endAddress = (int) linearMemory.getSize();
            if (endAddress == 0)
                endAddress = Integer.MAX_VALUE;
            endAddress--;
            int lastAddress = -1;
            for (int address = 0; address < endAddress; address++)
            {
                address = findCodeBlockID(codeBlockID, address, endAddress);
                count++;
                lastAddress = address;
                System.out.print(".");
                //message += (Integer.toHexString( 0x1000000 | address).substring(1).toUpperCase()) + "\n";
            }
            count--;  // uncount the result from end of memory

            System.out.println();
            
            if (lastAddress >= 0)
            {
                setSelectedAddress(lastAddress);
                showAddress(lastAddress);
            }
            alert("Found "+count+" instances of "+ codeBlockID);
        }
        else if (evt.getSource() == toggleBPSelected)
        {
            int address = getSelectedAddress();
            BreakpointsFrame bf = (BreakpointsFrame) JPC.getObject(BreakpointsFrame.class);
            if (bf == null)
                return;

            Rectangle r1 = ((JViewport) codeBlockTable.getParent()).getViewRect();
            if (bf.isBreakpoint(address))
                bf.removeBreakpoint(address);
            else
                bf.setBreakpoint(address);

            setSelectedAddress(address);
            codeBlockTable.scrollRectToVisible(r1);
        }
        else if (evt.getSource() == toggleBPInstruction)
        {
            BreakpointsFrame bf = (BreakpointsFrame) JPC.getObject(BreakpointsFrame.class);
            if (bf == null)
                return;

            Rectangle r1 = ((JViewport) codeBlockTable.getParent()).getViewRect();
            if (bf.isBreakpoint(nextInstructionAddress))
                bf.removeBreakpoint(nextInstructionAddress);
            else
                bf.setBreakpoint(nextInstructionAddress);

            setSelectedAddress(nextInstructionAddress);
            codeBlockTable.scrollRectToVisible(r1);
        }
        else if (evt.getSource() == decodeAtSelected)
        {
            if (codeBlocks == null)
                return;

            int address = getSelectedAddress();
            codeBlocks.decodeBlockAt(address, true);
            refreshDetails();
            
            showAddress(address);
            setSelectedAddress(address);
        }
        else if (evt.getSource() == showAddress)
        {
            String address = getUserInput("Enter address (hex)", "Show Address");
            try
            {
                int addr = Integer.valueOf(address, 16).intValue();
                showAddress(addr);
                setSelectedAddress(addr);
            }
            catch (Exception e) {}
        }
    }

    /**
     * find a code block that has a matching string ID and return its address. on fail return endAddress
     */
    private int findCodeBlockID(String codeBlockID, int startAddress, int endAddress)
    {
        int startRow = startAddress & AddressSpace.BLOCK_MASK;
        startAddress = startAddress & AddressSpace.INDEX_MASK;
        
        for(int baseAddress = startAddress; (baseAddress < (endAddress - AddressSpace.BLOCK_SIZE)); baseAddress += AddressSpace.BLOCK_SIZE)
        {
            Memory mem = null;
            try
            {
                mem = MemoryViewer.getReadMemoryBlockAt(linearMemory, baseAddress);
                mem.getByte(0);
            }
            catch (Exception e) 
            {
                mem = LinearMemoryViewer.translateLinearAddress(physicalMemory, processor, baseAddress);
            }
            
            if (mem != null)
            {
                for(int row = startRow; (row < AddressSpace.BLOCK_SIZE);  row++)
                {
                    CodeBlock cb = null;
                    try
                    {
// 			cb = mem.queryCodeBlockAt(row);
			cb = null;

                        if (cb instanceof SpanningCodeBlock)
                            cb = null;
                    }
                    catch (Exception e) 
                    {
                        cb = null;
                    }
                    
                    if ((cb != null) && (cb.toString().equals(codeBlockID)))
                        return baseAddress + row;
                }
            }
            startRow = 0;
        }
        return endAddress;
    }


    public void frameClosed()
    {
        JPC.getInstance().objects().removeObject(this);
    }

    public void PCCreated()
    {
        refreshDetails();
    }

    public void PCDisposed()
    {
        nextInstructionAddress = 0;
        pc = null;
        linearMemory = null;
        physicalMemory = null;
        codeModel.fireTableDataChanged();
        model.fireTableDataChanged();
    }
    
    public void executionStarted() {}

    public void executionStopped() 
    {
        refreshDetails();
    }

    public void setVisible(boolean value)
    {
        super.setVisible(value);
        if (value)
            refreshDetails();
    }

    public void refreshDetails()
    {
        int selected = getSelectedAddress();
        breakpoints =  (BreakpointsFrame) JPC.getObject(BreakpointsFrame.class);
        codeBlocks = (CodeBlockRecord) JPC.getObject(CodeBlockRecord.class);
        linearMemory = (LinearAddressSpace) JPC.getObject(LinearAddressSpace.class);
        physicalMemory = (PhysicalAddressSpace) JPC.getObject(PhysicalAddressSpace.class);
        pc = (PC) JPC.getObject(PC.class);
        
        processor = (Processor) JPC.getObject(Processor.class);
        if (processor == null)
            nextInstructionAddress = 0;
        else
            nextInstructionAddress = processor.getInstructionPointer();

        if (!isVisible())
            return;
            
        codeModel.setBaseAddress(codeModel.baseAddress);

        if (trackInstructions.getState())
        {
            showAddress(nextInstructionAddress);
            setSelectedAddress(nextInstructionAddress);
            currentIndex = codeBlocks.getTraceLength();
        }
        else
            setSelectedAddress(selected);
    }

    public int getSelectedAddress()
    {
        int selectedAddress = memoryBlockTable.getSelectedRow();
        if (selectedAddress < 0)
            return -1;

        selectedAddress *= AddressSpace.BLOCK_SIZE;
        return selectedAddress + Math.max(0, codeBlockTable.getSelectedRow());
    }

    public void setSelectedAddress(int address)
    {
        int blockRow = address >>> AddressSpace.INDEX_SHIFT;
        int offset = (address & AddressSpace.BLOCK_MASK);

        memoryBlockTable.setRowSelectionInterval(blockRow, blockRow);
        codeBlockTable.setRowSelectionInterval(offset, offset);
    }

    public void showAddress(int address)
    {
        int blockRow = address >>> AddressSpace.INDEX_SHIFT;
        int offset = (address & AddressSpace.BLOCK_MASK);

        Rectangle rect = memoryBlockTable.getCellRect(blockRow, 0, true);
        memoryBlockTable.scrollRectToVisible(rect);

        memoryBlockTable.setRowSelectionInterval(blockRow, blockRow);
        codeModel.setBaseAddress(address & AddressSpace.INDEX_MASK);

        rect = codeBlockTable.getOverlayRect(offset, 20);
        codeBlockTable.scrollRectToVisible(rect);
    }

    class MemoryBlockTableModel extends BasicTableModel
    {
        MemoryBlockTableModel()
        {
            super(new String[]{"Block", "Type"}, new int[]{80, 60});
        }

        public int getRowCount()
        {
            if (linearMemory == null)
                return 0;
            return (int) (linearMemory.getSize() / AddressSpace.BLOCK_SIZE);
        }

        public Object getValueAt(int row, int column)
        {
            int address = (int) (((long) row)*AddressSpace.BLOCK_SIZE);

            if (column == 0)
                return MemoryViewPanel.zeroPadHex(address, 8);
            else
            {
                Memory mem = MemoryViewer.getReadMemoryBlockAt(linearMemory, address);
                if (!linearMemory.isPagingEnabled())
                    mem = MemoryViewer.getReadMemoryBlockAt(physicalMemory, address);

                if (mem == null)
                    return "";
                else if (mem instanceof PhysicalAddressSpace.MapWrapper)
                    return "MAPPED";
                else if (mem instanceof PhysicalAddressSpace.UnconnectedMemoryBlock)
                    return "---";
                else if (mem instanceof LinearAddressSpace.PageFaultWrapper)
                {
                    ProcessorException pe = ((LinearAddressSpace.PageFaultWrapper) mem).getFault();
                    return pe.getClass().getName();
                }
                else
                    return "DATA";
            }
        }
    }

    class CodeBlockModel extends BasicTableModel
    {
        int baseAddress;

        CodeBlockModel()
        {
            super(new String[]{"Address", "Raw Bytes", "uCode", "X86 Length", "X86 Count"}, new int[]{80, 80, 330, 80, 80});
            baseAddress = 0;
        }

        public int getRowCount()
        {
            if (linearMemory == null)
                return 0;
            return AddressSpace.BLOCK_SIZE;
        }

        void setBaseAddress(int addr)
        {
            baseAddress = addr;
            codeBlockTable.recalculateBlockPositions();
            fireTableDataChanged();
        }

        public Object getValueAt(int row, int column)
        {
            int address = baseAddress + row;
            Memory mem = null;
            try
            {
                mem = MemoryViewer.getReadMemoryBlockAt(linearMemory, address);
                mem.getByte(0);
            }
            catch (Exception e) 
            {
                mem = LinearMemoryViewer.translateLinearAddress(physicalMemory, processor, address);
            }

            CodeBlock cb = null;
            try
            {
// 		cb = mem.queryCodeBlockAt(row);
		cb = null;

		if (cb instanceof SpanningCodeBlock)
                {
//                     if (processor.isProtectedMode())
//                         ((SpanningCodeBlock) cb).setAddress(linearMemory, address, processor.cs.getDefaultSizeFlag());
//                     else
//                         ((SpanningCodeBlock) cb).setAddress(physicalMemory, address, processor.cs.getDefaultSizeFlag());
                }
            }
            catch (Exception e) {}

            switch (column)
            {
            case 0:
                return MemoryViewPanel.zeroPadHex(address, 8);
            case 1:
                if (mem == null)
                    return "";
                byte b = mem.getByte(row);
                return MemoryViewPanel.zeroPadHex(0xFF & b, 2);
            case 2:
                if (mem == null)
                    return "Not present";
		if (cb == null)
		    return "";
                return cb;
            case 3:
                if (cb == null)
                    return null;
                return cb.getX86Length();
            case 4:
                if (cb == null)
                    return null;
                return cb.getX86Count();
            default:
                return null;
            }
        }
    }

    class CellRenderer extends DefaultTableCellRenderer
    {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(f);
            setHorizontalAlignment(JLabel.LEFT);
            setBackground(Color.white);
            setForeground(Color.black);

            if (isSelected)
                setBackground(Color.yellow);
            
            int address = codeModel.baseAddress + row;
            if (nextInstructionAddress == address)
            {
                setBackground(Color.magenta);
                setForeground(Color.white);
            }

            if ((column == 0) && (breakpoints != null))
            {
                if (breakpoints.isBreakpoint((int) address))
                    setBackground(Color.orange);
            }

            if (value instanceof CodeBlock)
                setText(value.toString());

            return this;
        }
    }
}
