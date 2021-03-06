package ca.ubc.ece.salt.pangor.analysis.ast;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.NodeVisitor;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import ca.ubc.ece.salt.pangor.analysis.classify.ClassifierDataSet;
import ca.ubc.ece.salt.pangor.analysis.scope.Scope;
import ca.ubc.ece.salt.pangor.analysis.specialtype.SpecialTypeVisitor;
import ca.ubc.ece.salt.pangor.batch.AnalysisMetaInformation;
import ca.ubc.ece.salt.pangor.cfg.CFG;
import ca.ubc.ece.salt.pangor.classify.alert.ClassifierAlert;
import ca.ubc.ece.salt.pangor.classify.alert.SpecialTypeAlert;
import ca.ubc.ece.salt.pangor.js.analysis.SpecialTypeAnalysisUtilities.SpecialType;
import ca.ubc.ece.salt.pangor.js.analysis.SpecialTypeCheck;
import ca.ubc.ece.salt.pangor.js.analysis.UseTreeVisitor;
import ca.ubc.ece.salt.pangor.js.analysis.scope.ScopeAnalysis;

/**
 * Performs an AST-only special type handling analysis using AST visitors.
 *
 * This classifier is used for evaluation purposes only and should not be used
 * in actual data mining. Instead, use the SpecialTypeAnalysis classifier.
 */
public class STHScopeAnalysis extends ScopeAnalysis<ClassifierAlert, ClassifierDataSet> {

	/** Stores the possible callback error check repairs. */
	private Set<SpecialTypeCheckResult> specialTypeCheckResults;

	public STHScopeAnalysis(ClassifierDataSet dataSet, AnalysisMetaInformation ami) {
		super(dataSet, ami);
		this.specialTypeCheckResults = new HashSet<SpecialTypeCheckResult>();
	}

	/**
	 * @return The set of possible special type check repairs (or
	 * anti-patterns if this is the source file analysis.
	 */
	public Set<SpecialTypeCheckResult> getSpecialTypeCheckResults() {
		return this.specialTypeCheckResults;
	}

	@Override
	public void analyze(ClassifiedASTNode root, List<CFG> cfgs) throws Exception {

		super.analyze(root, cfgs);

		/* Look at each function. */
		this.inspectFunctions(this.dstScope);

	}

	@Override
	public void analyze(ClassifiedASTNode srcRoot, List<CFG> srcCFGs, ClassifiedASTNode dstRoot,
			List<CFG> dstCFGs) throws Exception {

		super.analyze(srcRoot, srcCFGs, dstRoot, dstCFGs);

		/* Look at each function. */
		this.inspectFunctions(this.dstScope);

	}

	/**
	 *
	 * @param scope The function to inspect.
	 */
	private void inspectFunctions(Scope<AstNode> scope) {

		/* Visit the function and look for STH patterns. */
		STHScopeAnalysisVisitor visitor = new STHScopeAnalysisVisitor();
		if(scope.getScope() instanceof FunctionNode) {
			FunctionNode function = (FunctionNode) scope.getScope();
            function.getBody().visit(visitor);
		}
		else {
            scope.getScope().visit(visitor);
		}

		/* Visit the child functions. */
		for(Scope<AstNode> child : scope.getChildren()) {
			inspectFunctions(child);
		}

	}

	/**
	 * Visits if statements and finds new special type checks.
	 */
	private class STHScopeAnalysisVisitor implements NodeVisitor {

		@Override
		public boolean visit(AstNode node) {

			if(node instanceof IfStatement) {

				IfStatement is = (IfStatement) node;

				/* Get the special type checks in the if statement. */
				List<SpecialTypeCheck> specialTypeChecks = SpecialTypeVisitor.getSpecialTypeChecks(is.getCondition(), true);

				/* Get the identifiers that were used in the then block. */
				Set<String> usedIdentifiers = UseTreeVisitor.getSpecialTypeChecks(is.getThenPart());
				usedIdentifiers.addAll(UseTreeVisitor.getSpecialTypeChecks(is.getCondition()));

				for(SpecialTypeCheck specialTypeCheck : specialTypeChecks) {
					if(!specialTypeCheck.isSpecialType && usedIdentifiers.contains(specialTypeCheck.identifier)) {

						/* Register an alert (for reporting). */
						STHScopeAnalysis.this.registerAlert(node, new SpecialTypeAlert(STHScopeAnalysis.this.ami, "[TODO: function name]", "NF_STH", specialTypeCheck.identifier, specialTypeCheck.specialType));

						/* Store the result (for meta analysis). */
						STHScopeAnalysis.this.specialTypeCheckResults.add(new SpecialTypeCheckResult(specialTypeCheck.identifier, specialTypeCheck.specialType));

					}
				}

			}

			else if(node instanceof FunctionNode) {
				return false;
			}

			return true;
		}

	}

	/**
	 * Stores an identifier that was used in a new special type check, and then
	 * used on the 'guaranteed not a special type' path.
	 */
	public class SpecialTypeCheckResult {

		public String identifier;
		public SpecialType specialType;

		public SpecialTypeCheckResult(String identifier, SpecialType specialType) {
			this.identifier = identifier;
			this.specialType = specialType;
		}

		@Override
		public boolean equals(Object o) {

			if(!(o instanceof SpecialTypeCheckResult)) return false;

			SpecialTypeCheckResult cec = (SpecialTypeCheckResult) o;

			if(this.identifier.equals(cec.identifier) && this.specialType.equals(cec.specialType)) return true;

			return false;

		}

		@Override
		public int hashCode() {
			return (this.identifier + "-" + this.identifier).hashCode();
		}
	}

}
