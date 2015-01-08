package org.opensha.eq.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.opensha.eq.fault.Faults.validateDepth;
import static org.opensha.eq.fault.Faults.validateDip;
import static org.opensha.eq.fault.Faults.validateRake;
import static org.opensha.eq.fault.Faults.validateTrace;
import static org.opensha.eq.fault.Faults.validateWidth;
import static org.opensha.eq.model.FloatStyle.CENTERED;
import static org.opensha.eq.model.FloatStyle.FULL_DOWN_DIP;
import static org.opensha.util.TextUtils.validateName;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opensha.data.DataUtils;
import org.opensha.eq.fault.scaling.MagAreaRelationship;
import org.opensha.eq.fault.scaling.MagLengthRelationship;
import org.opensha.eq.fault.scaling.MagScalingRelationship;
import org.opensha.eq.fault.surface.GriddedSurface;
import org.opensha.eq.fault.surface.GriddedSurfaceWithSubsets;
import org.opensha.eq.fault.surface.RuptureFloating;
import org.opensha.eq.fault.surface.RuptureScaling;
import org.opensha.eq.fault.surface.RuptureSurface;
import org.opensha.geo.LocationList;
import org.opensha.mfd.IncrementalMfd;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;

/**
 * Fault source representation. This class wraps a model of a fault geometry and
 * a list of magnitude frequency distributions that characterize how the fault
 * might rupture (e.g. as one, single geometry-filling event, or as multiple
 * smaller events) during earthquakes. Smaller events are modeled as 'floating'
 * ruptures; they occur in multiple locations on the fault surface with
 * appropriately scaled rates.
 * 
 * <p>A {@code FaultSource} cannot be created directly; it may only be created
 * by a private parser.</p>
 * 
 * @author Peter Powers
 */
public class FaultSource implements Source {

	final String name;
	final LocationList trace;
	final double dip;
	final double width;
	final double rake;
	final List<IncrementalMfd> mfds;
	final double spacing;
	final RuptureScaling rupScaling;
	final RuptureFloating rupFloating;
	final GriddedSurface surface;

	private final List<List<Rupture>> ruptureLists; // 1:1 with Mfds

	// package privacy for subduction subclass
	FaultSource(String name, LocationList trace, double dip, double width, GriddedSurface surface,
		double rake, List<IncrementalMfd> mfds, double spacing, RuptureScaling rupScaling,
		RuptureFloating rupFloating) {

		this.name = name;
		this.trace = trace;
		this.dip = dip;
		this.width = width;
		this.surface = surface;
		this.rake = rake;
		this.mfds = mfds;
		this.spacing = spacing;
		this.rupScaling = rupScaling;
		this.rupFloating = rupFloating;

		ruptureLists = initRuptureLists();
		checkState(Iterables.size(Iterables.concat(ruptureLists)) > 0,
			"FaultSource has no ruptures");
	}

	private List<List<Rupture>> initRuptureLists() {
		ImmutableList.Builder<List<Rupture>> rupListsBuilder = ImmutableList.builder();
		for (IncrementalMfd mfd : mfds) {
			List<Rupture> rupList = createRuptureList(mfd);
			checkState(rupList.size() > 0, "Rupture list is empty");
			rupListsBuilder.add(rupList);
		}
		return rupListsBuilder.build();
	}

	@Override public int size() {
		return Iterables.size(this);
	}

	@Override public String name() {
		return name;
	}

	@Override public Iterator<Rupture> iterator() {
		return Iterables.concat(ruptureLists).iterator();
	}

	@Override public String toString() {
		// @formatter:off
		Map<Object, Object> data = ImmutableMap.builder()
				.put("name", name)
				.put("dip", dip)
				.put("width", width)
				.put("rake", rake)
				.put("mfds", mfds.size())
				.put("top", trace.first().depth())
				.build();
		return getClass().getSimpleName() + " " + data;
		// @formatter:on
	}

	private List<Rupture> createRuptureList(IncrementalMfd mfd) {
		ImmutableList.Builder<Rupture> rupListbuilder = ImmutableList.builder();

		// @formatter:off
		for (int i = 0; i < mfd.getNum(); ++i) {
			double mag = mfd.getX(i);
			double rate = mfd.getY(i);

			// TODO do we really want to do this??
			if (rate < 1e-14) continue; // shortcut low rates

			if (mfd.floats()) {

				// get global floating model
				// rupture dimensions
				double maxWidth = surface.width();
				double length = computeRuptureLength(msr, mag, maxWidth, aspectRatio);
				double width = Math.min(length / aspectRatio, maxWidth);

				// 2x width ensures full down-dip rupture
				if (floatStyle == FULL_DOWN_DIP) {
					width = 2 * maxWidth;
				}

				GriddedSurfaceWithSubsets surf = (GriddedSurfaceWithSubsets) surface;
				
				// rupture count
				double numRup = (floatStyle != CENTERED) ?
					surf.getNumSubsetSurfaces(length, width, offset) :
					surf.getNumSubsetSurfacesAlongLength(length, offset);

				for (int r = 0; r < numRup; r++) {
					RuptureSurface floatingSurface = (floatStyle != CENTERED) ?
						surf.getNthSubsetSurface(length, width, offset, r) :
						surf.getNthSubsetSurfaceCenteredDownDip(length, width, offset, r);
					double rupRate = rate / numRup;
					Rupture rup = Rupture.create(mag, rake, rupRate, floatingSurface);
					rupListbuilder.add(rup);
				}
			} else {
				Rupture rup = Rupture.create(mag, rate, rake, surface);
				rupListbuilder.add(rup);
			}
		}
		// @formatter:on
		return rupListbuilder.build();
	}

	private static double computeRuptureLength(MagScalingRelationship msr, double mag,
			double maxWidth, double aspectRatio) {

		if (msr instanceof MagLengthRelationship) {
			return msr.getMedianScale(mag);
		} else if (msr instanceof MagAreaRelationship) {
			double area = msr.getMedianScale(mag);
			double width = Math.sqrt(area / aspectRatio);
			return area / ((width > maxWidth) ? maxWidth : width);
		} else {
			throw new IllegalArgumentException("Unsupported MagScalingRelation: " + msr.getClass());
		}
	}

	/* Single use builder */
	static class Builder {

		private static final String ID = "FaultSource.Builder";
		private boolean built = false;

		private static final Range<Double> SURFACE_GRID_SPACING_RANGE = Range.closed(0.1, 20.0);

		// required
		String name;
		LocationList trace;
		Double dip;
		Double width;
		Double depth;
		Double rake;
		ImmutableList.Builder<IncrementalMfd> mfdsBuilder = ImmutableList.builder();
		List<IncrementalMfd> mfds;
		Double spacing;
		RuptureScaling rupScaling;
		RuptureFloating rupFloating;

		Builder name(String name) {
			this.name = validateName(name);
			return this;
		}

		Builder trace(LocationList trace) {
			this.trace = validateTrace(trace);
			return this;
		}

		Builder dip(double dip) {
			this.dip = validateDip(dip);
			return this;
		}

		Builder width(double width) {
			this.width = validateWidth(width);
			return this;
		}

		Builder depth(double depth) {
			this.depth = validateDepth(depth);
			return this;
		}

		Builder rake(double rake) {
			this.rake = validateRake(rake);
			return this;
		}

		// NPE checks in the two methods below will be redone by
		// ImmutableList.Builder which disallows nulls

		Builder mfd(IncrementalMfd mfd) {
			this.mfdsBuilder.add(checkNotNull(mfd, "MFD is null"));
			return this;
		}

		Builder mfds(List<IncrementalMfd> mfds) {
			checkNotNull(mfds, "MFD list is null");
			checkArgument(mfds.size() > 0, "MFD list is empty");
			this.mfdsBuilder.addAll(mfds);
			return this;
		}

		Builder surfaceSpacing(double spacing) {
			this.spacing = DataUtils
				.validate(SURFACE_GRID_SPACING_RANGE, "Floater Offset", spacing);
			return this;
		}

		Builder rupScaling(RuptureScaling rupScaling) {
			this.rupScaling = checkNotNull(rupScaling, "Rup-Scaling Relation is null");
			return this;
		}

		Builder rupFloating(RuptureFloating rupFloating) {
			this.rupFloating = checkNotNull(rupFloating, "Rup-Floating Model is null");
			return this;
		}

		void validateState(String id) {
			checkState(!built, "This %s instance as already been used", id);
			checkState(name != null, "%s name not set", id);
			checkState(trace != null, "%s trace not set", id);
			checkState(dip != null, "%s dip not set", id);
			checkState(width != null, "%s width not set", id);
			checkState(depth != null, "%s depth not set", id);
			checkState(rake != null, "%s rake not set", id);
			checkState(mfds.size() > 0, "%s has no MFDs", id);
			checkState(spacing != null, "%s surface grid spacing not set", id);
			checkState(rupScaling != null, "%s rupture-scaling relation not set", id);
			checkState(rupFloating != null, "%s rupture-floating model not set", id);
			built = true;
		}

		FaultSource buildFaultSource() {

			mfds = mfdsBuilder.build();

			validateState(ID);

			// create surface
			GriddedSurfaceWithSubsets surface = GriddedSurfaceWithSubsets.builder().trace(trace)
				.depth(depth).dip(dip).width(width).spacing(spacing).build();

			return new FaultSource(name, trace, dip, width, surface, rake,
				ImmutableList.copyOf(mfds), spacing, rupScaling, rupFloating);
		}
	}

}
