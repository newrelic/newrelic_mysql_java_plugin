package com.newrelic.plugins.mysql;

import com.newrelic.metrics.publish.processors.EpochCounter;;

/**
 * This class holds additional meta data about a given metric
 * This enables the Agent to work more intelligently with the global MySQL commands,
 * by default knowing nothing, but then being able to define better unit names, identifying counters etc
 * 
 * Currently a Metric can have the following attributes
 * 
 * - Counter  (Yes/No).   The default is Yes
 * - Unit Name
 * 
 * @author Ronald Bradford me@ronaldbradford.com
 *
 */
public class MetricMeta {

		public final static String DEFAULT_UNIT ="Operations";
		public final static String DEFAULT_COUNTER_UNIT = DEFAULT_UNIT + "/Second";

		private String unit;
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
}
