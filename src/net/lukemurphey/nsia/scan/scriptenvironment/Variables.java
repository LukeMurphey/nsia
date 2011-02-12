package net.lukemurphey.nsia.scan.scriptenvironment;

import java.util.Vector;

public class Variables {
	
	private class Variable{
		String name;
		Object value = null;
		
		public Variable(String name){
			
			// Precondition check
			if( name == null ){
				throw new IllegalArgumentException();
			}
			this.name = name;
		}
		
		public Variable(String name, Object value){
			this(name);
			
			this.value = value;
		}
		
		public String getName(){
			return name;
		}
		
		public Object getValue(){
			return value;
		}
		
		public void setValue(Object value){
			this.value = value;
		}
	}
	
	private Vector<Variable> vars = new Vector<Variable>();
	
	public boolean isSet( String variable ){
		for( int c = 0; c < vars.size(); c++){
			if( vars.get(c).getName().equalsIgnoreCase(variable) ){
				return true;
			}
		}
		
		return false;
	}
	
	public void unSet( String variable ){
		for( int c = 0; c < vars.size(); c++){
			if( vars.get(c).getName().equalsIgnoreCase(variable) ){
				vars.remove(c);
				return;
			}
		}
	}
	
	public void set(String variable){
		set( variable, null );
	}
	
	public void set(String name, Object value){
		
		Variable variable = getVariable(name);
		
		if( variable == null ){
			vars.add( new Variable(name, value) );
		}
		else{
			variable.setValue(value);
		}
	}
	
	private Variable getVariable(String name){
		for( int c = 0; c < vars.size(); c++){
			if( vars.get(c).getName().equalsIgnoreCase(name) ){
				return vars.get(c);
			}
		}
		
		return null;
	}
	
	public Object get(String name){
		Variable variable = getVariable(name);
		
		if( variable != null ){
			return variable.getValue();
		}
		
		return null;
	}
	
	public int size(){
		return vars.size();
	}
	
	public String get(int c){
		Variable var = vars.get(c);
		
		if( var != null ){
			return var.getName();
		}
		
		return null;
	}

}
