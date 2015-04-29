package ca.ubc.ece.salt.sdjsb.analysis.specialtype;

import java.util.LinkedList;
import java.util.List;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;

/**
 * A visitor for finding special type checks in conditions.
 * @author qhanam
 */
public class SpecialTypeVisitor implements NodeVisitor {
    
    private List<SpecialTypeCheck> specialTypeChecks;
    private AstNode condition;
    
    /**
     * @param condition The branch condition to investigate.
     * @return A list of all the special type checks in the condition.
     */
    public static List<SpecialTypeCheck> getSpecialTypeChecks(AstNode condition) {
    	SpecialTypeVisitor visitor = new SpecialTypeVisitor(condition);
    	condition.visit(visitor);
    	return visitor.specialTypeChecks;
    }
    
    public SpecialTypeVisitor(AstNode condition) {
		this.specialTypeChecks = new LinkedList<SpecialTypeCheck>();
		this.condition = condition;
    }
    
    /**
     * @return the list of special type checks found by the visitor.
     */
    public List<SpecialTypeCheck> getSpecialTypeChecks() {
    	return this.specialTypeChecks;
    }
    
    /**
     * Visits each element of the condition and looks for special type checks.
     */
    public boolean visit(AstNode node) {
    	
    	/* We only inspect inserted conditions. */
    	if(node.getChangeType() == ChangeType.INSERTED || node.getChangeType() == ChangeType.REMOVED) {

            SpecialTypeCheck stc = SpecialTypeAnalysisUtilities.getSpecialTypeCheck(condition, node);

            if(stc != null) this.specialTypeChecks.add(stc);

    	}

        return true;

    }

}
