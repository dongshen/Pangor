package ca.ubc.ece.salt.sdjsb.analysis.callbackparam;

import java.util.List;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;
import ca.ubc.ece.salt.sdjsb.cfg.CFG;
import ca.ubc.ece.salt.sdjsb.alert.CallbackParameterAlert;
import ca.ubc.ece.salt.sdjsb.analysis.AnalysisUtilities;
import ca.ubc.ece.salt.sdjsb.analysis.scope.Scope;
import ca.ubc.ece.salt.sdjsb.analysis.scope.ScopeAnalysis;

/**
 * Classifies repairs that add an error parameter to a callback.
 * 
 * The error handling convention in node.js callbacks is that the first
 * parameter of every callback is an error parameter that indicates if an
 * error was generated by the code that called the callback. This repair
 * indicates either the callback was not handling parameters properly or
 * an exception was not handled by the caller.
 */
public class CallbackParamAnalysis extends ScopeAnalysis {
	
	@Override
	public void analyze(AstRoot root, List<CFG> cfgs) throws Exception {
		
		super.analyze(root, cfgs);
		
		/* Look at each function. */
		this.inspectFunctions(this.dstScope);

	}

	@Override
	public void analyze(AstRoot srcRoot, List<CFG> srcCFGs, AstRoot dstRoot,
			List<CFG> dstCFGs) throws Exception {

		super.analyze(srcRoot, srcCFGs, dstRoot, dstCFGs);

		/* Look at each function. */
		this.inspectFunctions(this.dstScope);

	}
	
	/**
	 * Inspect the parameters of each function. Trigger an alert when a
	 * parameter that looks like an error or data parameter is added
	 * as the first or second parameter respectively.
	 * @param scope The function to inspect.
	 */
	private void inspectFunctions(Scope scope) {
		
		if(scope.scope instanceof FunctionNode) {
			
			FunctionNode function = (FunctionNode) scope.scope;
			List<AstNode> parameters = function.getParams();
			
			/* Check for a new error parameter. */
			if(function.getChangeType() != ChangeType.INSERTED && !parameters.isEmpty() 
					&& function.getParent() instanceof FunctionCall 
					&& function.getParent().getChangeType() != ChangeType.MOVED 
					&& function.getParent().getChangeType() != ChangeType.INSERTED) {

                AstNode errorParameter = parameters.get(0);
                if(errorParameter instanceof Name) {

                	Name name = (Name) errorParameter;
                    if(name.getChangeType() == ChangeType.INSERTED && name.getIdentifier().matches("(?i)e(rr(or)?)?")) {
                    	/* Register an alert. */
                    	String signature = AnalysisUtilities.getFunctionSignature(function);
                    	this.registerAlert(name, new CallbackParameterAlert("CB", function.getName(), signature, name.getIdentifier()));
                    }

                }

			}
			
		}
		
		for(Scope child : scope.children) {
			inspectFunctions(child);
		}
		
	}

}
