package org.jpc.emulator.peripheral;

public interface UserInputDevice {
	void keyPressed(byte scancode);
	void keyReleased(byte scancode);
	void putMouseEvent(int dx, int dy, int dz, int buttons);	
}
