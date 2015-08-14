package ca.ubc.ece.salt.sdjsb.analysis;

import java.util.List;

import org.mozilla.javascript.ast.AstRoot;

import ca.ubc.ece.salt.sdjsb.batch.AnalysisMetaInformation;
import ca.ubc.ece.salt.sdjsb.cfg.CFG;

/**
 * Creates a data set of analysis results.
 *
 * @param <U> The type of alert the data set stores.
 * @param <T> The type of data set that stores the analysis results.
 */
public abstract class Analysis<U extends Alert, T extends DataSet<U>> {

	/**
	 * The data set manages the alerts by storing and loading alerts to and
	 * from the disk, performing pre-processing tasks and calculating metrics.
	 */
	private DataSet<U> dataSet;

	/**
	 * The meta info for the analysis (i.e., project id, file paths, commit IDs, etc.).
	 */
	protected AnalysisMetaInformation ami;

	/**
	 * @param dataSet The data set that will keep track of the alerts.
	 * @param ami The meta information from the bulk analysis.
	 */
	public Analysis(DataSet<U> dataSet, AnalysisMetaInformation ami) {
		this.dataSet = dataSet;
		this.ami = ami;
	}

	/**
	 * Perform a single-file analysis.
	 * @param root The script.
	 * @param cfgs The list of CFGs in the script (one for each function plus
	 * 			   one for the script).
	 */
	public abstract void analyze(AstRoot root, List<CFG> cfgs) throws Exception;

	/**
	 * Perform a source/destination analysis.
	 * @param srcRoot The source script.
	 * @param srcCFGs The list of source CFGs in the script.
	 * @param dstRoot The destination script.
	 * @param dstCFGs The list of destination CFGs in the script.
	 */
	public abstract void analyze(AstRoot srcRoot, List<CFG> srcCFGs, AstRoot dstRoot, List<CFG> dstCFGs) throws Exception;

	/**
	 * Complete the meta information for the alert and register it with the
	 * data set.
	 * @param alert The alert to store in the data set.
	 * @throws Exception
	 */
	protected void registerAlert(U alert) throws Exception  {

		/* Register the alert with the data set. */
		this.dataSet.registerAlert(alert);

	}

}