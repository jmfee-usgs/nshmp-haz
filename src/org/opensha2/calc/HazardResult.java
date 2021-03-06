package org.opensha2.calc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.opensha2.data.ArrayXY_Sequence.copyOf;
import static org.opensha2.eq.model.SourceType.CLUSTER;
import static org.opensha2.eq.model.SourceType.SYSTEM;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.opensha2.data.ArrayXY_Sequence;
import org.opensha2.eq.model.HazardModel;
import org.opensha2.eq.model.SourceType;
import org.opensha2.gmm.Imt;

import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

/**
 * The result of a hazard calculation. This container class is public for
 * reference by external packages but is not directly modifiable, nor it's field
 * accessible. The {@link Results} class provides HazardResult exporting and
 * processing utilities.
 * 
 * @author Peter Powers
 * @see Results
 */
public final class HazardResult {

	final SetMultimap<SourceType, HazardCurveSet> sourceSetMap;
	final Map<Imt, ArrayXY_Sequence> totalCurves;
	final HazardModel model;
	final Site site;
	final CalcConfig config;

	private HazardResult(
			SetMultimap<SourceType, HazardCurveSet> sourceSetMap,
			Map<Imt, ArrayXY_Sequence> totalCurves,
			HazardModel model,
			Site site,
			CalcConfig config) {
		
		this.sourceSetMap = sourceSetMap;
		this.totalCurves = totalCurves;
		this.model = model;
		this.site = site;
		this.config = config;
	}

	@Override public String toString() {
		String LF = StandardSystemProperty.LINE_SEPARATOR.value();
		StringBuilder sb = new StringBuilder("HazardResult:");
		sb.append(LF);
		for (SourceType type : sourceSetMap.keySet()) {
			sb.append(type).append("SourceSet:").append(LF);
			for (HazardCurveSet curveSet : sourceSetMap.get(type)) {
				sb.append("  ").append(curveSet.sourceSet);
				int used = (type == CLUSTER) ? curveSet.clusterGroundMotionsList.size() :
					(type == SYSTEM) ? curveSet.hazardGroundMotionsList.get(0).inputs.size() :
						curveSet.hazardGroundMotionsList.size();

				sb.append("Used: ").append(used);
				sb.append(LF);

				if (curveSet.sourceSet.type() == CLUSTER) {
					// TODO ??
					// List<ClusterGroundMotions> cgmsList =
					// curveSet.clusterGroundMotionsList;
					// for (ClusterGroundMotions cgms : cgmsList) {
					// sb.append( "|--" + LF);
					// for (HazardGroundMotions hgms : cgms) {
					// sb.append("  |--" + LF);
					// for (HazardInput input : hgms.inputs) {
					// sb.append("    |--" + input + LF);
					// }
					// }
					// sb.append(LF);
					// }
					// sb.append(curveSet.clusterGroundMotionsList);

				} else {
					// sb.append(curveSet.hazardGroundMotionsList);
				}
			}
		}
		return sb.toString();
	}

	/**
	 * The total mean hazard curves for each calculated {@code Imt}.
	 */
	public Map<Imt, ArrayXY_Sequence> curves() {
		return totalCurves;
	}
	
	/**
	 * The original configuration used to generate this result.
	 */
	public CalcConfig config() {
		return config;
	}

	static Builder builder(CalcConfig config) {
		return new Builder(config);
	}

	static class Builder {

		private static final String ID = "HazardResult.Builder";
		private boolean built = false;

		private HazardModel model;
		private Site site;
		private CalcConfig config;
		
		private ImmutableSetMultimap.Builder<SourceType, HazardCurveSet> resultMapBuilder;
		private Map<Imt, ArrayXY_Sequence> totalCurves;

		private Builder(CalcConfig config) {
			this.config = checkNotNull(config);
			totalCurves = new EnumMap<>(Imt.class);
			for (Entry<Imt, ArrayXY_Sequence> entry : config.logModelCurves.entrySet()) {
				totalCurves.put(entry.getKey(), copyOf(entry.getValue()).clear());
			}
			resultMapBuilder = ImmutableSetMultimap.builder();
		}

		Builder site(Site site) {
			checkState(this.site == null, "%s site already set", ID);
			this.site = checkNotNull(site);
			return this;
		}
		
		Builder model(HazardModel model) {
			checkState(this.model == null, "%s model already set", ID);
			this.model = checkNotNull(model);
			return this;
		}
		
		Builder addCurveSet(HazardCurveSet curveSet) {
			resultMapBuilder.put(curveSet.sourceSet.type(), curveSet);
			for (Entry<Imt, ArrayXY_Sequence> entry : curveSet.totalCurves.entrySet()) {
				totalCurves.get(entry.getKey()).add(entry.getValue());
			}
			return this;
		}

		private void validateState(String mssgID) {
			checkState(!built, "This %s instance has already been used", mssgID);
			checkState(site != null, "%s site not set", mssgID);
			checkState(model != null, "%s model not set", mssgID);
		}
		
		HazardResult build() {
			validateState(ID);
			return new HazardResult(
				resultMapBuilder.build(), 
				Maps.immutableEnumMap(totalCurves),
				model,
				site,
				config);
		}

	}

}
