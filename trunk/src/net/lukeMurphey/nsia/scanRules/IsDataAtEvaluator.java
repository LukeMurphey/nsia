package net.lukeMurphey.nsia.scanRules;

public class IsDataAtEvaluator extends Evaluator {
	
	public IsDataAtEvaluator( ){

	}
	

	@Override
	public int evaluate(DataSpecimen data, int lastMatch, boolean useBasicEncoding) {
		
		int startLocation = computeStartLocation(lastMatch);
		
		if( lastMatch <= UNDEFINED){
			lastMatch = 0;
		}
		
		if( startLocation < data.getBytesLength() ){
			//The next evaluator should start at the same place, so return the same match position
			return lastMatch;
		}
		else{
			return UNDEFINED;
		}
		
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.BYTE_LOCATION;
	}

}
