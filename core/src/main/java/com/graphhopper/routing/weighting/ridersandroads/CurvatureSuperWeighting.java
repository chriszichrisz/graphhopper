package com.graphhopper.routing.weighting.ridersandroads;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.PriorityCode;
import com.graphhopper.routing.weighting.PriorityWeighting;
import com.graphhopper.routing.weighting.TurnCostProvider;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;

import static com.graphhopper.routing.util.PriorityCode.BEST;
import static com.graphhopper.routing.weighting.TurnCostProvider.NO_TURN_COST_PROVIDER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class uses bendiness parameter to prefer curvy routes.
 */
public class CurvatureSuperWeighting extends PriorityWeighting {
    private final static Logger logger = LoggerFactory.getLogger(CurvatureSuperWeighting.class);

    private final double minFactor;
    private final DecimalEncodedValue priorityEnc;
    private final DecimalEncodedValue curvatureEnc;
    private final DecimalEncodedValue avSpeedEnc;

    public CurvatureSuperWeighting(FlagEncoder flagEncoder, PMap pMap) {
        this(flagEncoder, pMap, NO_TURN_COST_PROVIDER);
    }

    public CurvatureSuperWeighting(FlagEncoder flagEncoder, PMap pMap, TurnCostProvider turnCostProvider) {
        super(flagEncoder, pMap, turnCostProvider);

        priorityEnc = flagEncoder.getDecimalEncodedValue(EncodingManager.getKey(flagEncoder, "priority"));
        curvatureEnc = flagEncoder.getDecimalEncodedValue(EncodingManager.getKey(flagEncoder, "curvaturesuper"));
        avSpeedEnc = flagEncoder.getDecimalEncodedValue(EncodingManager.getKey(flagEncoder, "average_speed"));
        double minBendiness = 1; // see correctErrors
        minFactor = minBendiness / Math.log(flagEncoder.getMaxSpeed()) / PriorityCode.getValue(BEST.getValue());
    }

    @Override
    public double getMinWeight(double distance) {
        return minFactor * distance;
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {
        double priority = priorityEnc.getDecimal(false, edgeState.getFlags());
        double bendiness = curvatureEnc.getDecimal(false, edgeState.getFlags());
        //double bendiness = 21;
        double speed = getRoadSpeed(edgeState, reverse);
        double roadDistance = edgeState.getDistance();
        
        // We use the log of the speed to decrease the impact of the speed, therefore we don't use the highway
        double regularWeight = roadDistance / Math.log(speed);
        double lala = (bendiness * regularWeight) / priority;

        boolean hmmm = Double.compare(bendiness, 1.0) < 0;
        if (hmmm) {
            logger.info("bd:" + bendiness + " speed:" + speed + " rd:" + roadDistance + " rW:" + regularWeight + " P:" + priority + " => " + lala);
        }
        return (bendiness * regularWeight) / priority;
    }

    protected double getRoadSpeed(EdgeIteratorState edge, boolean reverse) {
        return reverse ? edge.getReverse(avSpeedEnc) : edge.get(avSpeedEnc);
    }

    @Override
    public String getName() {
        return "curvaturesuper";
    }
}
