package macrobase;

import macrobase.analysis.classify.PercentileClassifier;
import macrobase.analysis.summary.BatchSummarizer;
import macrobase.analysis.summary.Explanation;
import macrobase.datamodel.DataFrame;
import macrobase.datamodel.Schema;
import macrobase.ingest.CSVDataFrameLoader;
import macrobase.ingest.DataFrameLoader;
import org.apache.commons.math3.analysis.function.Exp;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test looks at data with 1000 inliers and 20 outliers.
 * The outliers have lower usage and all have
 * location=CAN, version=v3
 */
public class UnsupervisedCSVTest {
    private DataFrame df;

    @Before
    public void setUp() throws Exception {
        Map<String, Schema.ColType> schema = new HashMap<>();
        schema.put("usage", Schema.ColType.DOUBLE);
        schema.put("latency", Schema.ColType.DOUBLE);
        schema.put("location", Schema.ColType.STRING);
        schema.put("version", Schema.ColType.STRING);
        DataFrameLoader loader = new CSVDataFrameLoader(
                "src/test/resources/sample.csv"
        ).setColumnTypes(schema);
        df = loader.load();
    }

    @Test
    public void testGetSummaries() throws Exception {
        PercentileClassifier pc = new PercentileClassifier("usage")
                .setPercentile(1.0);
        pc.process(df);
        DataFrame df_classified = pc.getResults();

        List<String> explanationAttributes = Arrays.asList(
                "location",
                "version"
        );
        BatchSummarizer summ = new BatchSummarizer()
                .setAttributes(explanationAttributes);
        summ.process(df_classified);
        Explanation results = summ.getResults();
        assertEquals(3, results.getItemsets().size());
    }

    @Test
    public void testCustomizedSummaries() throws Exception {
        PercentileClassifier pc = new PercentileClassifier("usage")
                .setPercentile(1.0);
        pc.process(df);
        DataFrame df_classified = pc.getResults();

        List<String> explanationAttributes = Arrays.asList(
                "location",
                "version"
        );
        // Increase risk ratio
        BatchSummarizer summ = new BatchSummarizer()
                .setAttributes(explanationAttributes)
                .setMinRiskRatio(5.0);
        summ.process(df_classified);
        Explanation results = summ.getResults();
        assertEquals(1, results.getItemsets().size());

        // Increase support requirement
        summ = new BatchSummarizer()
                .setAttributes(explanationAttributes)
                .setMinSupport(0.55);
        summ.process(df_classified);
        results = summ.getResults();
        assertEquals(2, results.getItemsets().size());

        // Restrict to only simple explanations
        summ = new BatchSummarizer()
                .setAttributes(explanationAttributes)
                .setUseAttributeCombinations(false);
        summ.process(df_classified);
        results = summ.getResults();
        assertEquals(2, results.getItemsets().size());

        // Invert outlier classes
        summ = new BatchSummarizer()
                .setAttributes(explanationAttributes)
                .setOutlierPredicate(d -> d == 0.0);
        summ.process(df_classified);
        results = summ.getResults();
        assertEquals(1000, results.getNumOutliers());
        assertEquals(0, results.getItemsets().size());
    }

    @Test
    public void testCustomizedClassifier() throws Exception {
        PercentileClassifier pc = new PercentileClassifier("usage")
                .setPercentile(2.0)
                .setIncludeHigh(false)
                .setIncludeLow(true);
        pc.process(df);
        DataFrame df_classified = pc.getResults();

        List<String> explanationAttributes = Arrays.asList(
                "location",
                "version"
        );
        BatchSummarizer summ = new BatchSummarizer()
                .setAttributes(explanationAttributes)
                .setUseAttributeCombinations(false);
        summ.process(df_classified);
        Explanation results = summ.getResults();
        assertEquals(2, results.getItemsets().size());
        assertTrue(results.getItemsets().get(0).getSupport() > .9);
    }
}
