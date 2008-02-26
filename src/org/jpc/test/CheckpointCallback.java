package org.jpc.test;

import org.jpc.emulator.processor.Processor;

public interface CheckpointCallback {
	void checkpointPassed(Processor cpu);
}
