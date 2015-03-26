package ca.ubc.ece.salt.sdjsb.test.cfg;

import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;

import ca.ubc.ece.salt.sdjsb.cfg.CFG;
import ca.ubc.ece.salt.sdjsb.cfg.CFGFactory;
import ca.ubc.ece.salt.sdjsb.cfg.CFGPrinter;
import junit.framework.TestCase;

public class TestCFG extends TestCase {

	protected void runTest(String file, List<String> expectedCFGs, boolean printCFGs) throws IOException {

		/* Parse the artifact with Rhino. */
		Parser parser = new Parser();
		AstRoot root = parser.parse(new FileReader(file), file, 1);
		
		/* Create the CFG. */
		List<CFG> cfgs = CFGFactory.createCFGs(root);

		/* Get the serialized CFGs. */
		List<String> actualCFGs = new LinkedList<String>();
        int n = 1;
        for(CFG cfg : cfgs) {
            CFGPrinter printer = new CFGPrinter(cfg);
            String serialized = printer.print();
            actualCFGs.add(serialized);
            if(printCFGs) System.out.println("CFG" + n + ": " + printer.print());
            n++;
        }
        
        /* Check the CFGs. */
        check(actualCFGs, expectedCFGs);

	}
	
	protected void check(List<String> actualCFGs, List<String> expectedCFGs) {
		
		/* Check the CFGs. */
		for(String expectedCFG : expectedCFGs) {
			int index = actualCFGs.indexOf(expectedCFG);
            TestCase.assertTrue("a CFG was not produced correctly", index >= 0);
		}
		
		/* Check that no additional CFGs were produced. */
		TestCase.assertEquals("an unexpected CFG was produced", actualCFGs.size(), expectedCFGs.size());

	}

	@Test
	public void testNestedIf() throws IOException {
		
		String file = "./test/input/cfg/basic.js";
		
		List<String> expectedCFGs = new LinkedList<String>();
		expectedCFGs.add("SCRIPT ENTRY->VAR->EXPR_RESULT->SCRIPT EXIT");
		expectedCFGs.add("FUNCTION ENTRY->EXPR_VOID->NAME?{EXPR_VOID:EXPR_VOID->TRUE?{EXPR_VOID}}->FUNCTION EXIT");
		expectedCFGs.add("FUNCTION ENTRY->EXPR_VOID->FUNCTION EXIT");
		
		this.runTest(file, expectedCFGs, true);

	}

	@Test
	public void testWhile() throws IOException {
		
		String file = "./test/input/cfg/whileloop.js";
		
		List<String> expectedCFGs = new LinkedList<String>();
		expectedCFGs.add("SCRIPT ENTRY->VAR->EXPR_RESULT->SCRIPT EXIT");
		expectedCFGs.add("FUNCTION ENTRY->EXPR_VOID->VAR->LT?{EXPR_VOID->EXPR_VOID->EXPR_VOID}->FUNCTION EXIT");
		expectedCFGs.add("FUNCTION ENTRY->EXPR_VOID->FUNCTION EXIT");
		
		this.runTest(file, expectedCFGs, true);

	}

	@Test
	public void testWhileBreak() throws IOException {
		
		String file = "./test/input/cfg/whilebreak.js";
		
		List<String> expectedCFGs = new LinkedList<String>();
		expectedCFGs.add("SCRIPT ENTRY->VAR->EXPR_RESULT->SCRIPT EXIT");
		expectedCFGs.add("FUNCTION ENTRY->EXPR_VOID->VAR->LT?{EXPR_VOID->SHEQ?{BREAK(FUNCTION EXIT)}->EXPR_VOID->EXPR_VOID}->FUNCTION EXIT");
		expectedCFGs.add("FUNCTION ENTRY->EXPR_VOID->FUNCTION EXIT");
		
		this.runTest(file, expectedCFGs, true);

	}

	@Test
	public void testWhileContinue() throws IOException {
		
		String file = "./test/input/cfg/whilecontinue.js";
		
		List<String> expectedCFGs = new LinkedList<String>();
		expectedCFGs.add("SCRIPT ENTRY->VAR->EXPR_RESULT->SCRIPT EXIT");
		expectedCFGs.add("FUNCTION ENTRY->EXPR_VOID->VAR->LT?{EXPR_VOID->SHEQ?{EXPR_VOID->CONTINUE(LT)}->EXPR_VOID->EXPR_VOID}->FUNCTION EXIT");
		expectedCFGs.add("FUNCTION ENTRY->EXPR_VOID->FUNCTION EXIT");
		
		this.runTest(file, expectedCFGs, true);

	}

	@Test
	public void testReturn() throws IOException {
		
		String file = "./test/input/cfg/return.js";
		
		List<String> expectedCFGs = new LinkedList<String>();
		expectedCFGs.add("SCRIPT ENTRY->VAR->EXPR_RESULT->SCRIPT EXIT");
		expectedCFGs.add("FUNCTION ENTRY->EXPR_VOID->VAR->LT?{EXPR_VOID->SHEQ?{RETURN(FUNCTION EXIT)}->EXPR_VOID->EXPR_VOID}->FUNCTION EXIT");
		expectedCFGs.add("FUNCTION ENTRY->EXPR_VOID->FUNCTION EXIT");
		
		this.runTest(file, expectedCFGs, true);

	}

	@Test
	public void testFor() throws IOException {
		
		String file = "./test/input/cfg/forloop.js";
		
		List<String> expectedCFGs = new LinkedList<String>();
		expectedCFGs.add("SCRIPT ENTRY->VAR->EXPR_RESULT->SCRIPT EXIT");
		expectedCFGs.add("FUNCTION ENTRY->EXPR_VOID->VAR->VAR->LT?{EXPR_VOID->EXPR_VOID->EXPR_VOID->INC}->FUNCTION EXIT");
		expectedCFGs.add("FUNCTION ENTRY->EXPR_VOID->FUNCTION EXIT");
		
		this.runTest(file, expectedCFGs, true);

	}

}
