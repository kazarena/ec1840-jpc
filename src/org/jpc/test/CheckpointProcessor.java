package org.jpc.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jpc.emulator.processor.Processor;

public class CheckpointProcessor {
	private static Map<Integer, Checkpoint> checkpointMap = new HashMap<Integer, Checkpoint>();

	public static void setCheckpoints(List<Checkpoint> checkpoints) {
		checkpointMap.clear();
		for(Checkpoint cp : checkpoints) {
			checkpointMap.put(cp.getOffset(), cp);
		}
	}

	public static void processCheckpoints(Processor cpu, int currentIP) {
		Checkpoint cp = checkpointMap.get(cpu.cs.getBase() + currentIP);// cpu.cs.getBase()+cpu.eip);
		if(cp != null) {
			if(cp.isReentrant() || !cp.isPassed()) {
				if(cp.getCallback() != null) {
					cp.getCallback().checkpointPassed(cpu);
				}
				cp.setPassed(true);
			}
		}
	}
}
