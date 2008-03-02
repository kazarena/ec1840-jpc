package org.jpc.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jpc.emulator.processor.Processor;

public class CheckpointProcessor {
	private static Map<Integer, Checkpoint> checkpointMap = new HashMap<Integer, Checkpoint>();
	private static boolean testing = false;

	public static void setCheckpoints(List<Checkpoint> checkpoints, boolean testing) {
		checkpointMap.clear();
		for(Checkpoint cp : checkpoints) {
			checkpointMap.put(cp.getOffset(), cp);
		}
		CheckpointProcessor.testing = testing;
	}

	public static void processCheckpoints(Processor cpu, int currentIP) {
		Checkpoint cp = checkpointMap.get(cpu.cs.getBase() + currentIP);// cpu.cs.getBase()+cpu.eip);
		if(cp != null) {
			if(cp.isReentrant() || !cp.isPassed()) {
				if(cp.getCallback() != null) {
					try {
					cp.getCallback().checkpointPassed(cpu);
					} catch(Error e) {
						if(testing) {
							// let AssertionError through
							throw e;
						}
					}
				}
				cp.setPassed(true);
			}
		}
	}
}
