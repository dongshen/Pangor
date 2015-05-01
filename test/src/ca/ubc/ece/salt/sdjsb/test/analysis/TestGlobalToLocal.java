package ca.ubc.ece.salt.sdjsb.test.analysis;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import ca.ubc.ece.salt.sdjsb.alert.Alert;
import ca.ubc.ece.salt.sdjsb.alert.GlobalToLocalAlert;
import ca.ubc.ece.salt.sdjsb.analysis.globaltolocal.GlobalToLocalAnalysis;

public class TestGlobalToLocal extends TestAnalysis {
	
	private void runTest(String[] args, List<Alert> expectedAlerts, boolean printAlerts) throws Exception {
		GlobalToLocalAnalysis analysis = new GlobalToLocalAnalysis();
		super.runTest(args, expectedAlerts, printAlerts, analysis);
	}

	@Test
	public void testNotDefined() throws Exception{
		String src = "./test/input/not_defined/nd_old.js";
		String dst = "./test/input/not_defined/nd_new.js";
		List<Alert> expectedAlerts = new LinkedList<Alert>();
		expectedAlerts.add(new GlobalToLocalAlert("GTL", "a"));
		this.runTest(new String[] {src, dst}, expectedAlerts, false);
	}

	@Test
	public void testFieldAccess() throws Exception {
		String src = "./test/input/not_defined/nd_field_old.js";
		String dst = "./test/input/not_defined/nd_field_new.js";
		List<Alert> expectedAlerts = new LinkedList<Alert>();
		expectedAlerts.add(new GlobalToLocalAlert("GTL", "a"));
		this.runTest(new String[] {src, dst}, expectedAlerts, false);
	}

	@Test
	public void testUsedVariable() throws Exception {
		String src = "./test/input/not_defined/nd_used_variable_old.js";
		String dst = "./test/input/not_defined/nd_used_variable_new.js";
		List<Alert> expectedAlerts = new LinkedList<Alert>();
		this.runTest(new String[] {src, dst}, expectedAlerts, false);
	}

	@Test
	public void testUsedField() throws Exception {
		String src = "./test/input/not_defined/nd_used_field_old.js";
		String dst = "./test/input/not_defined/nd_used_field_new.js";
		List<Alert> expectedAlerts = new LinkedList<Alert>();
		this.runTest(new String[] {src, dst}, expectedAlerts, false);
	}

	@Test
	public void testUsedCall() throws Exception {
		String src = "./test/input/not_defined/nd_used_call_old.js";
		String dst = "./test/input/not_defined/nd_used_call_new.js";
		List<Alert> expectedAlerts = new LinkedList<Alert>();
		this.runTest(new String[] {src, dst}, expectedAlerts, false);
	}

	@Test
	public void testCallField() throws Exception {
		String src = "./test/input/not_defined/CliUx_old.js";
		String dst = "./test/input/not_defined/CliUx_new.js";
		List<Alert> expectedAlerts = new LinkedList<Alert>();
		this.runTest(new String[] {src, dst}, expectedAlerts, false);
	}

	@Test
	public void testDeletedHigher() throws Exception {
		String src = "./test/input/not_defined/nd_deleted_higher_old.js";
		String dst = "./test/input/not_defined/nd_deleted_higher_new.js";
		List<Alert> expectedAlerts = new LinkedList<Alert>();
		this.runTest(new String[] {src, dst}, expectedAlerts, false);
	}
	
	@Test
	public void testNested() throws Exception {
		String src = "./test/input/not_defined/nd_nested_old.js";
		String dst = "./test/input/not_defined/nd_nested_new.js";
		List<Alert> expectedAlerts = new LinkedList<Alert>();
		expectedAlerts.add(new GlobalToLocalAlert("GTL", "i"));
		this.runTest(new String[] {src, dst}, expectedAlerts, false);
	}

	/**
	 * This gives a false positive. The developer initializes 'fs' to the same
	 * thing twice... which is kind of a bad practice bug on their part.
	 */
	@Test
	public void testProcessContainer() throws Exception {
		String src = "./test/input/not_defined/ProcessContainer_old.js";
		String dst = "./test/input/not_defined/ProcessContainer_new.js";
		List<Alert> expectedAlerts = new LinkedList<Alert>();
		this.runTest(new String[] {src, dst}, expectedAlerts, false);
	}

	@Test
	public void testCLI1() throws Exception {
		String src = "./test/input/not_defined/CLI1_old.js";
		String dst = "./test/input/not_defined/CLI1_new.js";
		List<Alert> expectedAlerts = new LinkedList<Alert>();
		this.runTest(new String[] {src, dst}, expectedAlerts, false);
	}

}