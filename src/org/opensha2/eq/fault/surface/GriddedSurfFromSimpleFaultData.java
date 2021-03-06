package org.opensha2.eq.fault.surface;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.opensha2.eq.fault.Faults;
import org.opensha2.geo.Location;
import org.opensha2.geo.LocationList;

/**
 * <p>Title:  GriddedSurfFromSimpleFaultData </p>
 *
 * <p>Description: This creates and GriddedSurface from SimpleFaultData</p>
 *
 * @author Nitin Gupta
 */
@Deprecated
public abstract class GriddedSurfFromSimpleFaultData extends AbstractGriddedSurface {

	protected LocationList faultTrace;
	protected double upperSeismogenicDepth = Double.NaN;
	protected double lowerSeismogenicDepth = Double.NaN;
	protected double aveDip;
	

	/**
	 * This constructor will adjust the grid spacings along strike and down dip to exactly fill the surface
	 * (not cut off ends), leaving the grid spacings just less then the originals.
	 * @param faultTrace
	 * @param aveDip
	 * @param upperSeismogenicDepth
	 * @param lowerSeismogenicDepth
	 * @param maxGridSpacingAlong - maximum grid spacing along strike
	 * @param maxGridSpacingDown - maximum grid spacing down dip
	 * @throws FaultException
	 */
	protected GriddedSurfFromSimpleFaultData(LocationList faultTrace, double aveDip,
			double upperSeismogenicDepth, double lowerSeismogenicDepth, double maxGridSpacingAlong,
			double maxGridSpacingDown) {
		
		double length = faultTrace.length();
		double gridSpacingAlong = length/Math.ceil(length/maxGridSpacingAlong);
		double downDipWidth = (lowerSeismogenicDepth-upperSeismogenicDepth)/Math.sin(aveDip*Math.PI/180 );
		double gridSpacingDown = downDipWidth/Math.ceil(downDipWidth/maxGridSpacingDown);
/*		
		System.out.println(faultTrace.getName()+"\n\t"+
				maxGridSpacingAlong+"\t"+(float)strikeSpacing+"\t"+(float)dipSpacing+"\t"+
				(float)(faultTrace.getTraceLength()/strikeSpacing)+"\t"+
				(float)(downDipWidth/dipSpacing));
*/				

		set(faultTrace, aveDip, upperSeismogenicDepth, lowerSeismogenicDepth, gridSpacingAlong, gridSpacingDown);
	}


	protected void set(LocationList faultTrace, double aveDip, double upperSeismogenicDepth,
			double lowerSeismogenicDepth, double gridSpacingAlong, double gridSpacingDown)	{
		this.faultTrace =faultTrace;
		this.aveDip =aveDip;
		this.upperSeismogenicDepth = upperSeismogenicDepth;
		this.lowerSeismogenicDepth =lowerSeismogenicDepth;
		this.strikeSpacing = gridSpacingAlong;
		this.dipSpacing = gridSpacingDown;
//		this.sameGridSpacing = true;
//		if(gridSpacingDown != gridSpacingAlong) sameGridSpacing = false;
	}


	// ***************************************************************
	/** @todo  Serializing Helpers - overide to increase performance */
	// ***************************************************************


//	public LocationList getFaultTrace() { return faultTrace; }
//
//	public double getUpperSeismogenicDepth() { return upperSeismogenicDepth; }
//
//	public double getLowerSeismogenicDepth() { return lowerSeismogenicDepth; }
//

	/**
	 * This method checks the simple-fault data to make sure it's all OK.
	 * @throws FaultException
	 */
	protected void assertValidData() {

//		checkNotNull(faultTrace, "Fault Trace is null");
//		if( faultTrace == null ) throw new FaultException(C + "Fault Trace is null");

		Faults.validateDip(aveDip);
		Faults.validateDepth(lowerSeismogenicDepth);
		Faults.validateDepth(upperSeismogenicDepth);
		checkArgument(upperSeismogenicDepth < lowerSeismogenicDepth);
		
		checkArgument(!Double.isNaN(strikeSpacing), "invalid gridSpacing");
//		if( strikeSpacing == Double.NaN ) throw new FaultException(C + "invalid gridSpacing");

		double depth = faultTrace.first().depth();
		checkArgument(depth <= upperSeismogenicDepth,
			"depth on faultTrace locations %s must be <= upperSeisDepth %s",
			depth, upperSeismogenicDepth);
		//		if(depth > upperSeismogenicDepth)
//			throw new FaultException(C + "depth on faultTrace locations must be < upperSeisDepth");

//		Iterator<Location> it = faultTrace.iterator();
//		while(it.hasNext()) {
//			if(it.next().getDepth() != depth){
//				throw new FaultException(C + ":All depth on faultTrace locations must be equal");
//			}
//		}
		for (Location loc : faultTrace) {
			if (loc.depth() != depth) {
				checkArgument(loc.depth() == depth, "All depth on faultTrace locations must be equal");
//				throw new FaultException(C + ":All depth on faultTrace locations must be equal");
			}
		}
	}
	
//	@Override
//	public double dip() {
//		return rupDip;
//	}
//
//	@Override
//	public double depth() {
//		return upperSeismogenicDepth;
//	}
//
//	@Override
//	public double strike() {
//		return Faults.strike(faultTrace);
//	}

//	@Override
//	public LocationList getUpperEdge() {
//		// check that the location depths in faultTrace are same as
//		// upperSeismogenicDepth
//		double aveTraceDepth = 0;
//		for (Location loc : faultTrace)
//			aveTraceDepth += loc.depth();
//		aveTraceDepth /= faultTrace.size();
//		double diff = Math.abs(aveTraceDepth - upperSeismogenicDepth); // km
//		if (diff < 0.001) return faultTrace;
//		throw new RuntimeException(
//			" method not yet implemented where depths in the " +
//				"trace differ from upperSeismogenicDepth (and projecting will create " +
//				"loops for FrankelGriddedSurface projections; aveTraceDepth=" +
//				aveTraceDepth + "\tupperSeismogenicDepth=" +
//				upperSeismogenicDepth);
//	}

}
