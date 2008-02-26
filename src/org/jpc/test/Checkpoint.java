package org.jpc.test;

public class Checkpoint {
	private int offset;
	private CheckpointCallback callback;
	private boolean reentrant;
	private boolean passed = false;
	
	public Checkpoint(int offset, CheckpointCallback callback) {
		this.offset = offset;
		this.callback = callback;
		this.reentrant = true;
	}
	
	public Checkpoint(int offset, CheckpointCallback callback, boolean reentrant) {
		this.offset = offset;
		this.callback = callback;
		this.reentrant = reentrant;
	}
	
	public int getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}
	public CheckpointCallback getCallback() {
		return callback;
	}
	public void setCallback(CheckpointCallback callback) {
		this.callback = callback;
	}

	public boolean isReentrant() {
		return reentrant;
	}

	public void setReentrant(boolean reentrant) {
		this.reentrant = reentrant;
	}

	public boolean isPassed() {
		return passed;
	}

	public void setPassed(boolean passed) {
		this.passed = passed;
	}
}
