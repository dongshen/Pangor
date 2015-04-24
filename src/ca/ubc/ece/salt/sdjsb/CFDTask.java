package ca.ubc.ece.salt.sdjsb;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import ca.ubc.ece.salt.sdjsb.alert.Alert;
import ca.ubc.ece.salt.sdjsb.analysis.FlowAnalysis;

/**
 * Wraps ControlFlowDifferencing as Callable.
 */
public class CFDTask implements Callable<Set<Alert>> {
	
	private ControlFlowDifferencing cfd;
	private FlowAnalysis<?> analysis;
	
	public CFDTask(ControlFlowDifferencing cfd, FlowAnalysis<?> analysis) {
		this.cfd = cfd;
		this.analysis = analysis;
	}

	@Override
	public Set<Alert> call() throws Exception {
		return cfd.analyze(this.analysis);
	}

}