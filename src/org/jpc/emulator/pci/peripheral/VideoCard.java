package org.jpc.emulator.pci.peripheral;

import org.jpc.support.GraphicsDisplay;

public interface VideoCard {
	public void resizeDisplay(GraphicsDisplay device);
	
	public void updateDisplay(GraphicsDisplay device);
}
