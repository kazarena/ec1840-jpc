package org.jpc.emulator.peripheral;

import org.jpc.emulator.HardwareComponent;

public interface UserInputDevice extends HardwareComponent {
	void keyPressed(byte scancode);

	void keyReleased(byte scancode);

	void putMouseEvent(int dx, int dy, int dz, int buttons);
}
