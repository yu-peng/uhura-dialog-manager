package server;

public class Constraint {

	public enum ConstraintType{DEPARTURE,ARRIVAL,DURATION};
	
	private ConstraintType type = null;
	private double value = 0;
	private boolean relaxable = false;
	
	public Constraint(ConstraintType _type, double _value){
		type = _type;
		value = _value;
	}
	
	public Constraint(ConstraintType _type, double _value, boolean _relaxable){
		type = _type;
		value = _value;
		relaxable = _relaxable;
	}
	
	public double getValue(){
		return value;
	}
	
	public ConstraintType getType(){
		return type;
	}
	
	public boolean isRelaxable(){
		return relaxable;
	}
	
}
