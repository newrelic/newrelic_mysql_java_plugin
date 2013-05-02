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

		public final static String DEFAULT_UNIT ="value";
		public final static String DEFAULT_COUNTER_UNIT = DEFAULT_UNIT + "/sec";

		public final static String INTEGER_TYPE = "int";
		public final static String FLOAT_TYPE = "float";
		
		private String unit;
		private String type = INTEGER_TYPE;
		private EpochCounter counter = null;
		
		public MetricMeta(boolean isCounter, String unit) {
			this.unit = unit;
			if (isCounter) this.counter = new EpochCounter();
		}

		public MetricMeta(boolean isCounter) {
			this.unit = isCounter ? DEFAULT_COUNTER_UNIT: DEFAULT_UNIT;
			if (isCounter) this.counter = new EpochCounter();
		}
		
		public static MetricMeta defaultMetricMeta() {
			return new MetricMeta(true);
		}

		public boolean isCounter() {
			return (this.counter == null ? false: true);
		}

		public String getUnit() {
			return this.unit;
		}
		
		public EpochCounter getCounter() {
			return this.counter;
		}
		
		public String getType() {
			return this.type;
		}

}
