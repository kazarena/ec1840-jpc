package org.jpc.emulator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.jpc.emulator.memory.AddressSpace;
import org.jpc.emulator.memory.LazyCodeBlockMemory;
import org.jpc.emulator.memory.LazyMemory;
import org.jpc.emulator.memory.LinearAddressSpace;
import org.jpc.emulator.memory.PhysicalAddressSpace;
import org.jpc.emulator.memory.codeblock.CodeBlock;
import org.jpc.emulator.motherboard.DMAController;
import org.jpc.emulator.motherboard.DualInterruptController;
import org.jpc.emulator.motherboard.GateA20Handler;
import org.jpc.emulator.motherboard.IOPortCapable;
import org.jpc.emulator.motherboard.IOPortHandler;
import org.jpc.emulator.motherboard.InterruptController;
import org.jpc.emulator.motherboard.IntervalTimer;
import org.jpc.emulator.motherboard.RTC;
import org.jpc.emulator.motherboard.SystemBIOS;
import org.jpc.emulator.motherboard.VGABIOS;
import org.jpc.emulator.pci.PCIBus;
import org.jpc.emulator.pci.PCIHostBridge;
import org.jpc.emulator.pci.PCIISABridge;
import org.jpc.emulator.pci.peripheral.EthernetCard;
import org.jpc.emulator.pci.peripheral.PIIX3IDEInterface;
import org.jpc.emulator.pci.peripheral.VGACard;
import org.jpc.emulator.peripheral.FloppyController;
import org.jpc.emulator.peripheral.Keyboard;
import org.jpc.emulator.peripheral.PCSpeaker;
import org.jpc.emulator.peripheral.SerialPort;
import org.jpc.emulator.peripheral.UserInputDevice;
import org.jpc.emulator.processor.ModeSwitchException;
import org.jpc.emulator.processor.Processor;
import org.jpc.support.BlockDevice;
import org.jpc.support.Clock;
import org.jpc.support.DriveSet;
import org.jpc.test.Checkpoint;

public class OxfordPC implements PC {
    private static final int SYS_RAM_SIZE = 256 * 1024 * 1024;

    private Processor processor;
    private IOPortHandler ioportHandler;
    private InterruptController irqController;
    private PhysicalAddressSpace physicalAddr;
    private LinearAddressSpace linearAddr;
    private IntervalTimer pit;
    private RTC rtc;
    private DMAController primaryDMA, secondaryDMA;
    private GateA20Handler gateA20;

    private PCIHostBridge pciHostBridge;
    private PCIISABridge pciISABridge;
    private PCIBus pciBus;
    private PIIX3IDEInterface ideInterface;

    private EthernetCard networkCard;
    private VGACard graphicsCard;
    private SerialPort serialDevice0;
    private Keyboard kbdDevice;
    private PCSpeaker speaker;
    private FloppyController fdc;

    private Clock vmClock;
    private DriveSet drives;

    private VGABIOS vgaBIOS;
    private SystemBIOS sysBIOS;

    private HardwareComponent[] myParts;

    public OxfordPC(Clock clock, DriveSet drives) throws IOException
    {
        this.drives = drives;
	processor = new Processor();
        vmClock = clock;

	//Motherboard
        physicalAddr = new PhysicalAddressSpace(SYS_RAM_SIZE);
        for (int i=0; i<SYS_RAM_SIZE; i+= AddressSpace.BLOCK_SIZE)
            //physicalAddr.allocateMemory(i, new ByteArrayMemory(blockSize));
            //physicalAddr.allocateMemory(i, new CompressedByteArrayMemory(blockSize));
            physicalAddr.allocateMemory(i, new LazyMemory(AddressSpace.BLOCK_SIZE));

        linearAddr = new LinearAddressSpace();
       	ioportHandler = new IOPortHandler();
	irqController = new DualInterruptController();
	primaryDMA = new DMAController(false, true);
	secondaryDMA = new DMAController(false, false);
	rtc = new RTC(0x70, 8, SYS_RAM_SIZE);
	pit = new IntervalTimer(0x40, 0);
	gateA20 = new GateA20Handler();

	//Peripherals
	ideInterface = new PIIX3IDEInterface();
	networkCard = new EthernetCard();
	graphicsCard = new VGACard();

	serialDevice0 = new SerialPort(0);
	kbdDevice = new Keyboard();
	fdc = new FloppyController();
	speaker = new PCSpeaker();

	//PCI Stuff
	pciHostBridge = new PCIHostBridge();
	pciISABridge = new PCIISABridge();
	pciBus = new PCIBus();

	//BIOSes
	sysBIOS = new SystemBIOS("bios.bin");
	vgaBIOS = new VGABIOS("vgabios.bin");

	myParts = new HardwareComponent[]{processor, vmClock, physicalAddr, linearAddr,
					  ioportHandler, irqController,
					  primaryDMA, secondaryDMA, rtc, pit, gateA20,
					  pciHostBridge, pciISABridge, pciBus,
					  ideInterface, drives, networkCard,
					  graphicsCard, serialDevice0,
					  kbdDevice, fdc, speaker,
					  sysBIOS, vgaBIOS};
	
	if (!configure())
            throw new IllegalStateException("PC Configuration failed");
    }

    public void start()
    {
	vmClock.resume();
    }

    public void stop()
    {
	vmClock.pause();
    }

    public void dispose()
    {
	stop();
        LazyCodeBlockMemory.dispose();
    }

    public void setFloppy(org.jpc.support.BlockDevice drive, int i)
    {
        if ((i < 0) || (i > 1))
            return;
        fdc.setDrive(drive, i);
    }

    public synchronized void runBackgroundTasks()
    {
        notify();
    }

    public DriveSet getDrives()
    {
        return drives;
    }

    public BlockDevice getBootDevice()
    {
        return drives.getBootDevice();
    }

    public int getBootType()
    {
        return drives.getBootType();
    }

    private boolean configure()
    {
	boolean fullyInitialised;
	int count = 0;
	do 
        {
	    fullyInitialised = true;
	    for (int j = 0; j < myParts.length; j++) 
            {
		if (myParts[j].initialised() == false) 
                {
		    for (int i = 0; i < myParts.length; i++)
			myParts[j].acceptComponent(myParts[i]);

		    fullyInitialised &= myParts[j].initialised();
		}
	    }
	    count++;
	} 
        while ((fullyInitialised == false) && (count < 100));

	if (count == 100) 
        {
            for (int i=0; i<myParts.length; i++)
                Logger.getLogger("PC").info("Part "+i+" ("+myParts[i].getClass()+") "+myParts[i].initialised());
	    return false;
        }

	for (int i = 0; i < myParts.length; i++)
        {
	    if (myParts[i] instanceof PCIBus)
		((PCIBus)myParts[i]).biosInit();
	}

	return true;
    }

    public boolean saveState(ZipOutputStream zip) throws IOException
    {
        //save state of of Hardware Components
        //processor     DONE (-fpu)
        //rtc           DONE (-calendar)
        //pit           DONE (-TImerChannel.timer/irq)

        try
        {
            saveComponent(zip, drives);
            saveComponent(zip, vmClock);
            saveComponent(zip, physicalAddr);
            saveComponent(zip, linearAddr);
            saveComponent(zip, processor);
            saveComponent(zip, ioportHandler);
            saveComponent(zip, irqController);
            saveComponent(zip, primaryDMA);
            saveComponent(zip, secondaryDMA);
            saveComponent(zip, rtc);
            saveComponent(zip, pit);
            saveComponent(zip, gateA20);
            saveComponent(zip, pciHostBridge);
            saveComponent(zip, pciISABridge);
            saveComponent(zip, pciBus);
            saveComponent(zip, ideInterface);
            saveComponent(zip, sysBIOS);
            saveComponent(zip, vgaBIOS);
            saveComponent(zip, kbdDevice);
            saveComponent(zip, fdc);
            saveComponent(zip, serialDevice0);
            saveComponent(zip, networkCard);
            saveComponent(zip, graphicsCard);
            saveComponent(zip, speaker);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("IO Error during state save.");
            return false;
        }

	return true;
    }

    private void saveComponent(ZipOutputStream zip, HardwareComponent component) throws IOException
    {
        ZipEntry entry = new ZipEntry(component.getClass().getName());
        try
        {
            zip.putNextEntry(entry);
        }
        catch (ZipException e)
        {
            entry = new ZipEntry(component.getClass().getName() + "2");
            zip.putNextEntry(entry);
        }
        component.dumpState(new DataOutputStream(zip));
        zip.closeEntry();
        System.out.println("component size " + entry.getSize() + " for " + component.getClass().getName());
    }

    private void loadComponent(ZipFile zip, HardwareComponent component) throws IOException
    {
        ZipEntry entry = zip.getEntry(component.getClass().getName());
        if (component == secondaryDMA)
            entry = zip.getEntry(component.getClass().getName() + "2");

        if (entry != null)
        {
            System.out.println("component size " + entry.getSize() + " for " + component.getClass().getName());
            DataInputStream in = new DataInputStream(zip.getInputStream(entry));
            if (component instanceof PIIX3IDEInterface)
                ((PIIX3IDEInterface) component).loadIOPorts(ioportHandler, in);
            else if (component instanceof EthernetCard)
                ((EthernetCard) component).loadIOPorts(ioportHandler, in);
            else
                component.loadState(in);

            if (component instanceof IOPortCapable)
            {
                ioportHandler.registerIOPortCapable((IOPortCapable) component);
            }
        }
    }

    private void linkComponents()
    {
	boolean fullyInitialised;
	int count = 0;
	do 
        {
	    fullyInitialised = true;
	    for (int j = 0; j < myParts.length; j++) 
            {
		if (myParts[j].updated() == false) 
                {
		    for (int i = 0; i < myParts.length; i++)
			myParts[j].updateComponent(myParts[i]);

		    fullyInitialised &= myParts[j].updated();
		}
	    }
	    count++;
	} 
        while ((fullyInitialised == false) && (count < 100));

	if (count == 100) 
        {
            for (int i=0; i<myParts.length; i++)
                System.out.println("Part "+i+" ("+myParts[i].getClass()+") "+myParts[i].updated());
        }
    }

    public void loadState(File f) throws IOException
    {
        try
        {
            ZipFile zip = new ZipFile(f);

            loadComponent(zip, drives);
            loadComponent(zip, vmClock);     
            loadComponent(zip, physicalAddr);
            loadComponent(zip, linearAddr);
            loadComponent(zip, processor);
            loadComponent(zip, irqController);    
            loadComponent(zip, ioportHandler);
            loadComponent(zip, primaryDMA);            
            loadComponent(zip, secondaryDMA);          
            loadComponent(zip, rtc);            
            loadComponent(zip, pit);            
            loadComponent(zip, gateA20);            
            loadComponent(zip, pciHostBridge);            
            loadComponent(zip, pciISABridge);            
            loadComponent(zip, pciBus);            
            loadComponent(zip, ideInterface);            
            loadComponent(zip, sysBIOS);            
            loadComponent(zip, vgaBIOS);            
            loadComponent(zip, kbdDevice);            
            loadComponent(zip, fdc);            
            loadComponent(zip, serialDevice0);            
            loadComponent(zip, networkCard);
            loadComponent(zip, graphicsCard);
            loadComponent(zip, speaker);

            linkComponents();
            //pciBus.biosInit();

            zip.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("IO Error during loading of Snapshot.");
            return;
        }
    }

    public void reset()
    {
	for (int i = 0; i < myParts.length; i++) 
        {
	    if (myParts[i] == this) 
                continue;
	    myParts[i].reset();
	}
        configure();
    }

    public UserInputDevice getKeyboard()
    {
        return kbdDevice;
    }

    public Processor getProcessor()
    {
        return processor;
    }

    public VGACard getGraphicsCard()
    {
        return graphicsCard;
    }

    public PhysicalAddressSpace getPhysicalMemory()
    {
        return physicalAddr;
    }

    public LinearAddressSpace getLinearMemory()
    {
        return linearAddr;
    }

    public Clock getSystemClock()
    {
        return vmClock;
    }

    public static PC createPC(String[] args, Clock clock) throws IOException
    {
        DriveSet disks = DriveSet.buildFromArgs(args);
        return new OxfordPC(clock, disks);
    }

    public final int execute()
    {
	int x86Count = 0;
	AddressSpace addressSpace = null;
        if (processor.isProtectedMode())
	    addressSpace = linearAddr;
	else
	    addressSpace = physicalAddr;

        // do it multiple times
	try 
        {
            for (int i=0; i<100; i++)
                x86Count += addressSpace.execute(processor, processor.getInstructionPointer());
	} 
        catch (ModeSwitchException e) {}

	return x86Count;
    }


    public final CodeBlock decodeCodeBlockAt(int address)
    {
	AddressSpace addressSpace = null;
	if (processor.isProtectedMode())
	    addressSpace = linearAddr;
	else
	    addressSpace = physicalAddr;
	CodeBlock block= addressSpace.decodeCodeBlockAt(processor, address);
	return block;
    }
        
    public final int executeStep()
    {
        try 
        {
            AddressSpace addressSpace = null;
            if (processor.isProtectedMode())
                addressSpace = linearAddr;
            else
                addressSpace = physicalAddr;
            
            return addressSpace.execute(processor, processor.getInstructionPointer());
        } 
        catch (ModeSwitchException e) 
        {
            return 1;
        }
    }

	public void setCheckpoints(List<Checkpoint> checkpoints) {
	}   
}
