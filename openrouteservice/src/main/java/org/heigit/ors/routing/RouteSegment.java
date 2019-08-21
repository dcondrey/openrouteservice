/*  This file is part of Openrouteservice.
 *
 *  Openrouteservice is free software; you can redistribute it and/or modify it under the terms of the 
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1 
 *  of the License, or (at your option) any later version.

 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details.

 *  You should have received a copy of the GNU Lesser General Public License along with this library; 
 *  if not, see <https://www.gnu.org/licenses/>.  
 */
package org.heigit.ors.routing;

import java.util.ArrayList;
import java.util.List;

import com.graphhopper.PathWrapper;

import org.heigit.ors.common.DistanceUnit;
import org.heigit.ors.util.DistanceUnitUtil;
import org.heigit.ors.util.FormatUtility;

public class RouteSegment {
	private double _distance;
	private double _duration;
	private double _ascent;
	private double _descent;
	private double _detourFactor = 0.0;
	private List<RouteStep> _steps;

	public RouteSegment(PathWrapper path, DistanceUnit units) throws Exception
	{
		_distance = FormatUtility.roundToDecimalsForUnits(DistanceUnitUtil.convert(path.getDistance(), DistanceUnit.METERS, units), units);
		_duration = FormatUtility.roundToDecimals(path.getTime()/1000.0, 1);
		_ascent = path.getAscend();
		_descent = path.getDescend();

		_steps = new ArrayList<>();
	}

	public double getDistance()
	{
		return _distance;
	}   

	public double getDuration()
	{
		return _duration;
	}

	public double getAscent()
	{
		return _ascent;
	}

	public double getDescent()
	{
		return _descent;
	}

	public double getAscentRounded()
	{
		return FormatUtility.roundToDecimals(_ascent, 1);
	}

	public double getDescentRounded()
	{
		return FormatUtility.roundToDecimals(_descent, 1);
	}

	public void addStep(RouteStep step)
	{
		_steps.add(step);
	}

	public List<RouteStep> getSteps() {
		return _steps;
	}

	public double getDetourFactor() {
		return _detourFactor;
	}

	public void setDetourFactor(double detourFactor) {
		_detourFactor = detourFactor;
	}
}
