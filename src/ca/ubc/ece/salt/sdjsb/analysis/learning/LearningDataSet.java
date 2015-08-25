package ca.ubc.ece.salt.sdjsb.analysis.learning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import weka.clusterers.DBSCAN;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.ManhattanDistance;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.RemoveByName;
import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;
import ca.ubc.ece.salt.sdjsb.analysis.DataSet;
import ca.ubc.ece.salt.sdjsb.analysis.learning.KeywordFilter.FilterType;
import ca.ubc.ece.salt.sdjsb.learning.apis.KeywordDefinition;
import ca.ubc.ece.salt.sdjsb.learning.apis.KeywordDefinition.KeywordType;
import ca.ubc.ece.salt.sdjsb.learning.apis.KeywordUse;
import ca.ubc.ece.salt.sdjsb.learning.apis.KeywordUse.KeywordContext;

/**
 * The {@code FeatureVectorManager} is a pre-processing step for data mining
 * and machine learning. {@code FeatureVectorManager} manages the feature
 * vectors that were generated during the AST analysis.
 *
 * Once all feature vectors have been built, they will contain some meta info
 * (commit {@link #clone()}, file, project, etc.) and zero or more
 * {@code Keyword}s (where a {@code Keyword} = name + context + package.
 *
 * The {@code FeatureVectorManager} filters out {@code FeatureVector}s that
 * aren't wanted (i.e., those that aren't related to a package we are
 * investigating) and features that are not used or hardly used.
 */
public class LearningDataSet implements DataSet<FeatureVector> {

	/**
	 * The packages we want to investigate. FeatureVectorManager
	 * filters out any FeatureVector which does not contain one of these
	 * packages.
	 */
	private List<KeywordFilter> filters;

	/**
	 * The path to the file where the data set will be cached. This allows us
	 * to limit our memory use and cache results for the future by storing the
	 * keyword extraction results on the disk.
	 */
	private String dataSetPath;

	/**
	 * The path to the folder where any supplementary files should be stored.
	 * In the case of the LearningDataSet, these files contain the function
	 * source code from the feature vectors.
	 */
	private String supplementaryPath;

	/** An ordered list of the keywords to print in the feature vector. **/
	private Set<KeywordDefinition> keywords;

	/** The feature vectors generated by the AST analysis. **/
	private List<FeatureVector> featureVectors;

	/** Weka-format data */
	private Instances wekaData;

	/**
	 * Used to produce a Weka data set. Create a {@code LearningDataSet} from
	 * a file on disk. This {@code LearningDataSet} can pre-process the data
	 * set and create a data set file for Weka.
	 * @param filters Filters out rows by requiring keywords to be present.
	 * @throws Exception Throws an exception when the {@code dataSetPath}
	 * 					 cannot be read.
	 */
	public LearningDataSet(String dataSetPath, List<KeywordFilter> filters) throws Exception {
		this.filters = filters;
		this.keywords = new HashSet<KeywordDefinition>();
		this.featureVectors = new LinkedList<FeatureVector>();
		this.dataSetPath = dataSetPath;
		this.supplementaryPath = null;

		/* Read the data set file and de-serialize the feature vectors. */
		this.importDataSet(dataSetPath);
	}

	/**
	 * Used for keyword analysis. Create a {@code LearningDataSet} to write
	 * the analysis results to disk.
	 * @param dataSetPath The file path to store the data set.
	 * @param supplementaryPath The folder path to store supplementary files.
	 */
	public LearningDataSet(String dataSetPath, String supplementaryPath) {
		this.filters = null;
		this.keywords = new HashSet<KeywordDefinition>();
		this.featureVectors = new LinkedList<FeatureVector>();
		this.dataSetPath = dataSetPath;
		this.supplementaryPath = supplementaryPath;
	}

	/**
	 * Used for testing. Creates a {@code LearningDataSet} that will add
	 * features directly to the data set (instead of writing them to a file).
	 * @param filters Filters out rows by requiring keywords to be present.
	 */
	public LearningDataSet(List<KeywordFilter> filters) {

		this.filters = filters;
		this.keywords = new HashSet<KeywordDefinition>();
		this.featureVectors = new LinkedList<FeatureVector>();
		this.dataSetPath = null;
		this.supplementaryPath = null;

	}

	/**
	 * Import a data set from a file to this {@code LearningDataSet}.
	 * @param dataSetPath The file path where the data set is stored.
	 * @throws Exception Occurs when the data set file cannot be read.
	 */
	public void importDataSet(String dataSetPath) throws Exception {

		try(BufferedReader reader = new BufferedReader(new FileReader(dataSetPath))) {

			for (String serialFeatureVector = reader.readLine();
					serialFeatureVector != null;
					serialFeatureVector = reader.readLine()) {

				FeatureVector featureVector = FeatureVector.deSerialize(serialFeatureVector);

				this.featureVectors.add(featureVector);

			}

		}
		catch(Exception e) {
			throw e;
		}

	}

	/**
	 * Adds a feature vector to the data set. If a data set file exists
	 * ({@code dataSetPath}), serializes the feature vector and writes it to
	 * the file. Otherwise, the feature vector is stored in memory in
	 * {@code LearningDataSet}.
	 * @param featureVector The feature vector to be managed by this class.
	 */
	@Override
	public void registerAlert(FeatureVector featureVector) throws Exception {

		if(this.dataSetPath != null) {
			this.storeFeatureVector(featureVector);
		}
		else {
			this.featureVectors.add(featureVector);
		}

	}

	/**
	 * Stores the feature vector in the file specified by {@code dataSetPath}.
	 * This method is synchronized because it may be used by several
	 * GitProjectAnalysis thread at the same time, which may cause race
	 * conditions when writing to the output file.
	 *
	 * @param featureVector The feature vector to be managed by this class.
	 */
	private synchronized void storeFeatureVector(FeatureVector featureVector) throws Exception {

		/* The KeywordFilter that will filter out unwanted feature vectors. */
		KeywordFilter insertedFilter = new KeywordFilter(FilterType.INCLUDE,
				KeywordType.UNKNOWN, KeywordContext.UNKNOWN,
				ChangeType.INSERTED, "", "");
		KeywordFilter removedFilter = new KeywordFilter(FilterType.INCLUDE,
				KeywordType.UNKNOWN, KeywordContext.UNKNOWN,
				ChangeType.REMOVED, "", "");

		List<KeywordFilter> filters = Arrays.asList(insertedFilter, removedFilter);

		/* Include this row in the output if it passes the filters. */
		if(LearningDataSet.includeRow(featureVector.keywordMap.keySet(), filters)) {

			/* The path to the file may not exist. Create it if needed. */
			File path = new File(this.dataSetPath);
			path.getParentFile().mkdirs();
			path.createNewFile();

			/* May throw IOException if the path does not exist. */
			PrintStream stream = new PrintStream(new FileOutputStream(path, true));

			/* Write the data set. */
			stream.println(featureVector.serialize());

			/* Finished writing the feature vector. */
			stream.close();

			/* Write the source code to a folder so we can examine it later. */
			this.printSupplementaryFiles(featureVector);

		}

	}

	/**
	 * Converts the feature vector header into a list of Weka attributes.
	 * @return The feature vector header as a list of Weka attributes.
	 */
	public ArrayList<Attribute> getWekaAttributes() {

		ArrayList<Attribute> attributes = new ArrayList<Attribute>();

		attributes.add(new Attribute("ID", 0));
		attributes.add(new Attribute("ProjectID", (ArrayList<String>)null, 1));
		attributes.add(new Attribute("ProjectHomepage", (ArrayList<String>)null, 2));
		attributes.add(new Attribute("BuggyFile", (ArrayList<String>)null, 3));
		attributes.add(new Attribute("RepairedFile", (ArrayList<String>)null, 4));
		attributes.add(new Attribute("BuggyCommitID", (ArrayList<String>)null, 5));
		attributes.add(new Attribute("RepairedCommitID", (ArrayList<String>)null, 6));
		attributes.add(new Attribute("FunctionName", (ArrayList<String>)null, 7));
		attributes.add(new Attribute("Cluster", (ArrayList<String>) null, 8));

		int i = 9;
		for(KeywordDefinition keyword : this.keywords) {
			attributes.add(new Attribute(keyword.toString(), i));
			i++;
		}

		return attributes;

	}

	/**
	 * Builds the feature vector header by filtering out features (columns)
	 * that are not used or hardly used.
	 * @return The feature vector header as a CSV list.
	 */
	public String getFeatureVectorHeader() {

		String header = String.join(",", "ID", "ProjectID", "ProjectHomepage", "BuggyFile",
				"RepairedFile", "BuggyCommitID", "RepairedCommitID",
				"FunctionName");

		for(KeywordDefinition keyword : this.keywords) {
			header += "," + keyword.toString();
		}

		return header;

	}

	/**
	 * Builds the feature vector by filtering out feature vectors (rows)
	 * that do not contain the packages specified in {@code packagesToExtract}.
	 * @return The data set as a CSV file.
	 */
	public String getFeatureVector() {

		String dataSet = "";

		for(FeatureVector featureVector : this.featureVectors) {
			dataSet += featureVector.getFeatureVector(keywords) + "\n";
		}

		return dataSet;

	}

	/**
	 * @return The list of feature vectors in this data set.
	 */
	public List<FeatureVector> getFeatureVectors() {
		return this.featureVectors;
	}

	/**
	 * Computes metrics about the data set:
	 *
	 *	Common keywords: A ranked list of the most common keywords sorted by
	 *					 their change type and the number of occurrences.
	 *
	 * @return The metrics for the data set (in the {@code LearningMetrics}
	 * 		   object).
	 */
	public LearningMetrics getMetrics() {

		/* The metrics object. */
		LearningMetrics metrics = new LearningMetrics();

		/* Compute the frequency of keywords. */
		Map<KeywordUse, Integer> counts = new HashMap<KeywordUse, Integer>();
		for(FeatureVector featureVector : this.featureVectors) {

			/* Increment all the keywords that appear in this feature vector. */
			for(KeywordUse keyword : featureVector.keywordMap.keySet()) {
				Integer count = counts.get(keyword);
				count = count == null ?  1 : count + 1;
				counts.put(keyword, count);
			}

		}

		/* Create the ordered set of keywords. */
		for(KeywordUse keyword : counts.keySet()) {
			metrics.addKeywordFrequency(keyword, counts.get(keyword));
		}


		return metrics;

	}

	/**
	 * Performs pre-processing operations for data-mining. Specifically,
	 * filters out rows which do not use the specified packages and filters
	 * out columns which do not contain any data.
	 */
	public void preProcess() {

		/* Remove rows that do not reference the packages we are interested in. */

		List<FeatureVector> toRemove = new LinkedList<FeatureVector>();
		for(FeatureVector featureVector : this.featureVectors) {

			/* Check if the feature vector references the any of the interesting packages. */
			if(!includeRow(featureVector.keywordMap.keySet())) {

				/* Schedule this FeatureVector for removal. */
				toRemove.add(featureVector);

			}

		}

		for(FeatureVector featureVector : toRemove) {
			this.featureVectors.remove(featureVector);
		}

		/* Remove rows that do not fall within the desired change score. */

		toRemove = new LinkedList<FeatureVector>();
		for(FeatureVector featureVector : this.featureVectors) {

			/* Check if the feature vector references the any of the interesting packages. */
			if(getChangeScore(featureVector.keywordMap.keySet()) == 0) {

				/* Schedule this FeatureVector for removal. */
				toRemove.add(featureVector);

			}

		}

		for(FeatureVector featureVector : toRemove) {
			this.featureVectors.remove(featureVector);
		}

		/* Get the set of keywords from all the feature vectors. */

		for(FeatureVector featureVector : this.featureVectors) {
			for(KeywordDefinition keyword : featureVector.keywordMap.keySet()) keywords.add(keyword);
		}

	}

	/**
	 * Converts this data set to a set of Weka Instances.
	 * @return The Weka data set.
	 */
	public Instances getWekaDataSet() {

		ArrayList<Attribute> attributes = this.getWekaAttributes();

		Instances dataSet = new Instances("DataSet", attributes, 0);
		dataSet.setClassIndex(-1);

		for(FeatureVector featureVector : this.featureVectors) {
			dataSet.add(featureVector.getWekaInstance(dataSet, attributes, this.keywords));
		}

		return dataSet;
	}

	/**
	 * Generates the clusters for this data set using DBScan, epsilon = 0.01 and
	 * min = 25.
	 *
	 * @return The number of instances in each cluster. The array index is the
	 *         cluster number.
	 * @throws Exception
	 */
	public int[] getWekaClusters() throws Exception {

		/* Convert the data set to a Weka-usable format. */
		wekaData = this.getWekaDataSet();

		/* Filter out the columns we don't want. */
		String[] removeOptions = new String[2];
		removeOptions[0] = "-R";
		removeOptions[1] = "1-9";
		Remove remove = new Remove();
		remove.setOptions(removeOptions);
		remove.setInputFormat(wekaData);
		Instances filteredData = Filter.useFilter(wekaData, remove);

		/* Filter out the UNCHANGED columns. */
		String[] removeByNameOptions = new String[2];
		removeByNameOptions[0] = "-E";
		removeByNameOptions[1] = ".*UNCHANGED.*";
		RemoveByName removeByName = new RemoveByName();
		removeByName.setOptions(removeByNameOptions);
		removeByName.setInputFormat(filteredData);
		filteredData = Filter.useFilter(filteredData, removeByName);

		/* Filter out the statement columns. */
		String[] removeKeywordOptions = new String[2];
		removeKeywordOptions[0] = "-E";
		/* Attribute filter for Statements */
//		removeKeywordOptions[1] = "(.*_global_test)";
		/* Attribute filter for Reserved Words and Operators */
		removeKeywordOptions[1] = "(.*_STATEMENT.*)|(.*_global_test)";
		/* Attribute filter for API Methods and Properties. */
//		removeKeywordOptions[1] = "(.*typeof.*)|(.*null.*)|(.*undefined.*)|(.*falsey.*)|(.*this.*)|(.*true.*)|(.*false.*)|(.*_STATEMENT.*)|(.*_global_test)";
		RemoveByName removeKeyword = new RemoveByName();
		removeKeyword.setOptions(removeKeywordOptions);
		removeKeyword.setInputFormat(filteredData);
		filteredData = Filter.useFilter(filteredData, removeKeyword);

		/* Set up the distance function. We want Manhattan Distance. */
		ManhattanDistance distanceFunction = new ManhattanDistance();
		String[] distanceFunctionOptions = "-R first-last".split("\\s");
		distanceFunction.setOptions(distanceFunctionOptions);

		/* DBScan Clusterer. */
		DBSCAN dbScan = new DBSCAN();
		String[] dbScanClustererOptions = "-E 0.01 -M 30".split("\\s");
		dbScan.setOptions(dbScanClustererOptions);
		dbScan.setDistanceFunction(distanceFunction);
		dbScan.buildClusterer(filteredData);

		/* Initialize the array for storing cluster metrics. */
		int[] clusters = new int[dbScan.numberOfClusters()];
		for(int i = 0; i < clusters.length; i++) clusters[i] = 0;

		/* Compute the metrics for the clustering. */
		for (Instance instance : wekaData) {
			try {
				Integer cluster = dbScan.clusterInstance(instance);
				instance.setValue(8, "cluster" + cluster.toString());
				clusters[cluster]++;
			} catch (Exception ignore) { }
		}

		return clusters;
	}


	/**
	 * Print the data set to a file. The filtered data set will be in a CSV
	 * format that can be imported directly into Weka.
	 * @param outFile The file to write the filtered data set to.
	 */
	public void writeFilteredDataSet(String outFile) {

		/* Open the file stream for writing if a file has been given. */
		PrintStream stream = System.out;

		if(outFile != null) {
			try {
				/*
				 * The path to the output folder may not exist. Create it if
				 * needed.
				 */
				File path = new File(outFile);
				path.getParentFile().mkdirs();

				stream = new PrintStream(new FileOutputStream(outFile));
			}
			catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}

		/* Write the header for the feature vector. */
		stream.println(this.getFeatureVectorHeader());

		/* Write the data set. */
		stream.println(this.getFeatureVector());

	}

	/**
	 * @param keywords The keywords from a feature vector.
	 * @return True if the keyword set matches an include filter.
	 */
	private boolean includeRow(Set<KeywordUse> keywords) {
		return includeRow(keywords, this.filters);
	}

	/**
	 * @param keywords The keywords from a feature vector.
	 * @param filers The filters to apply to the row.
	 * @return True if the keyword set matches an include filter.
	 */
	private static boolean includeRow(Set<KeywordUse> keywords, List<KeywordFilter> filters) {
		for(KeywordUse keyword : keywords) {
			for(KeywordFilter filter : filters) {

				/* This keyword must match the filter to be included. */
				if(filter.type != KeywordType.UNKNOWN && filter.type != keyword.type) continue;
				if(filter.context != KeywordContext.UNKNOWN && filter.context != keyword.context) continue;
				if(filter.changeType != ChangeType.UNKNOWN && filter.changeType != keyword.changeType) continue;
				if(!filter.pack.isEmpty() && !filter.pack.equals(keyword.getPackageName())) continue;
				if(!filter.keyword.isEmpty() && !filter.keyword.equals(keyword.keyword)) continue;

				/* The keyword matches the filter. */
				if(filter.filterType == FilterType.INCLUDE) return true;
				if(filter.filterType == FilterType.EXCLUDE) return false;

			}
		}
		return false;
	}

	/**
	 * Computes a score to determine how much a function has changed (using the
	 * number of inserted/removed/updated keywords).
	 * @param keywords The keywords from a feature vector.
	 * @return A score that represents how much the function has changed with
	 * 		   respect to its keywords. A score of zero means no keywords
	 * 		   changed.
	 */
	private int getChangeScore(Set<KeywordUse> keywords) {

		int score = 0;

		for(KeywordUse keyword : keywords) {
			if(keyword.changeType != ChangeType.UNCHANGED &&
					keyword.changeType != ChangeType.UNKNOWN) {
				score++;
			}
		}

		return score;

	}

	/**
	 * Writes the result of a clustering on an ARFF file
	 *
	 * @param outputFolder The folder were the ARFF files will be stored
	 * @param filename The filename (usually keyword toString representation)
	 */
	public void writeArffFile(String outputFolder, String filename) {
		/* The path may not exist. Create it if needed. */
		File path = new File(outputFolder, filename);
		path.mkdirs();

		ArffSaver saver = new ArffSaver();
		saver.setInstances(wekaData);

		try {
			saver.setFile(path);
			saver.writeBatch();
		} catch (IOException e) {
			System.err.println("Not possible to write Arff file.");

			e.printStackTrace();
		}

	}

	/**
	 * Writes the source code from each of the inspected functions to a file.
	 * @param supplementaryFolder The folder to place the files in.
	 */
	private void printSupplementaryFiles(FeatureVector featureVector) {

		/* The path to the supplementary folder may not exist. Create
		 * it if needed. */
		File path = new File(this.supplementaryPath);
		path.mkdirs();

		File src = new File(this.supplementaryPath, featureVector.id + "_src.js");
		File dst = new File(this.supplementaryPath, featureVector.id + "_dst.js");

		try (PrintStream srcStream = new PrintStream(new FileOutputStream(src));
			 PrintStream dstStream = new PrintStream(new FileOutputStream(dst));) {

			srcStream.print(featureVector.buggyFunctionCode);
			dstStream.print(featureVector.repairedFunctionCode);

			srcStream.close();
			dstStream.close();

		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
		}

	}

	/**
	 * Checks if the feature vector manager contains the keyword inside a
	 * feature vector. Used for testing.
	 * @param function The name of the function.
	 * @param keywords The keywords to look for.
	 * @return True if the list of keywords matches the list of keywords form
	 * 		   one or more functions.
	 */
	public boolean contains(String function, List<Pair<KeywordUse, Integer>> keywords) {
		outer:
		for(FeatureVector featureVector : this.featureVectors) {
			for(Pair<KeywordUse, Integer> keyword : keywords) {
				if(keyword.getRight() > 0 && !featureVector.keywordMap.containsKey(keyword.getLeft())) continue outer;
				if(keyword.getRight() > 0 && !featureVector.keywordMap.get(keyword.getLeft()).equals(keyword.getRight())) continue outer;
			}
			return true;
		}
		return false;
	}

}
