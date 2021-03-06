package org.opensha2.calc;

import static com.google.common.base.Preconditions.checkState;

import java.util.EnumMap;
import java.util.Map;

import org.opensha2.data.ArrayXY_Sequence;
import org.opensha2.gmm.Gmm;
import org.opensha2.gmm.Imt;

/**
 * Container class for the combined hazard curves derived from the
 * {@code Rupture}s in an individual {@code Source}, one for each
 * {@code GroundMotionModel} and {@code Imt} of interest. The curves will have
 * been scaled by the associated Mfd or rupture weights, but not by
 * {@code GroundMotionModel} weights.
 * 
 * @author Peter Powers
 */
final class HazardCurves {

	final GroundMotions groundMotions;
	final Map<Imt, Map<Gmm, ArrayXY_Sequence>> curveMap;

	private HazardCurves(GroundMotions groundMotions,
		Map<Imt, Map<Gmm, ArrayXY_Sequence>> curveMap) {
		this.groundMotions = groundMotions;
		this.curveMap = curveMap;
	}

	static Builder builder(GroundMotions groundMotions) {
		return new Builder(groundMotions);
	}

	static class Builder {

		private static final String ID = "HazardCurves.Builder";
		private boolean built = false;

		private final GroundMotions groundMotions;
		private final Map<Imt, Map<Gmm, ArrayXY_Sequence>> curveMap;

		private Builder(GroundMotions groundMotions) {
			this.groundMotions = groundMotions;
			curveMap = new EnumMap<>(Imt.class);
			for (Imt imt : groundMotions.means.keySet()) {
				Map<Gmm, ArrayXY_Sequence> gmmMap = new EnumMap<>(Gmm.class);
				curveMap.put(imt, gmmMap);
			}
		}

		Builder addCurve(Imt imt, Gmm gmm, ArrayXY_Sequence curve) {
			curveMap.get(imt).put(gmm, curve);
			return this;
		}

		HazardCurves build() {
			checkState(!built, "This %s instance has already been used", ID);
			// TODO check that all gmms have been set? it'll be difficult to
			// track whether all curves for all inputs have been added
			built = true;
			return new HazardCurves(groundMotions, curveMap);
		}

	}

}
