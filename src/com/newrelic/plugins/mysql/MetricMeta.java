package com.newrelic.plugins.mysql;

import com.newrelic.data.in.processors.EpochCounter;

/**
 * This class holds additional meta data about a given metric
 * This enables the Agent to work more intelligently with the global MySQL commands,
 * by default knowing nothing, but then being able to define better unit names, identifying counters etc
 * 
 * TODO:  Enable a factor multiplication, so we can easily convert bytes to MB for example.
 * 
 * @author Ronald Bradford me@ronaldbradford.com
 *
 */
public class MetricMeta {

		private final static String DEFAULT_UNIT ="value";
		private final static String DEFAULT_COUNTER_UNIT = DEFAULT_UNIT + "/sec";

		public final static String INTEGER_TYPE = "int";
		public final static String FLOAT_TYPE = "float";
		
		private String unit;
		private String type = INTEGER_TYPE;
		private EpochCounter counter = null;
		
		public MetricMeta(boolean isCounter, String unit) {
			this.unit = unit;
			if (isCounter) counter = new EpochCounter();
		}

		public MetricMeta(boolean isCounter) {
			new MetricMeta(isCounter, isCounter ? DEFAULT_COUNTER_UNIT: DEFAULT_UNIT);
		}
		
		public static MetricMeta defaultMetricMeta() {
			return new MetricMeta(false);
		}

		public boolean isCounter() {
			return (counter == null ? false: true);
		}

		public String getUnit() {
			return unit;
		}
		
		public EpochCounter getCounter() {
			return counter;
		}
		
		public String getType() {
			return type;
		}

}
