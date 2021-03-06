JPC: x86 PC Hardware Emulator

Build Instructions.

1) You will need a Java Development Kit 1.5 or later.

2) Javac the source files under org (no external dependencies)

3) The BIOS used in JPC is the Bochs BIOS; see http://bochs.sourceforge.net/

4) The VGA BIOS used in JPC is the Plex86/Bochs LGPL'd bios; see http://www.nongnu.org/vgabios

5) Ensure both BIOS files are in the root directory of the classpath (easy way; run JPC from the root of the directory where you expanded the files)

6) The test Floppy image "odin070.img" is from the Odin FreeDOS project; see http://odin.fdos.org/

7) To run JPC:
	java org.jpc.j2se.PCMonitor -fda odin070.img 

8) To run the JPC debugger: 
	java org.jpc.debugger.JPC -fda odin070.img

9) To run some games you might like to download, put them in a directory on your real computer and use JPC's ability to view a directory tree as a virtual FAT32 drive. For example, if some games are in "dosgames" in the directory where you expanded all the JPC files then type:
	java org.jpc.j2se.PCMonitor -fda odin070.img -hda dir:dosgames -boot fda

10) Have Fun! Please report bugs and contribute fixes and suggestions via our website:
	www.physics.ox.ac.uk/jpc