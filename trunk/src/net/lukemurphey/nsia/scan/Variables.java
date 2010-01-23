package net.lukemurphey.nsia.scan;

import java.util.Vector;

public class Variables {
	
	private Vector<String> vars = new Vector<String>();
	
	public boolean isSet( String variable ){
		for( int c = 0; c < vars.size(); c++){
			if( vars.get(c).equalsIgnoreCase(variable) ){
				return true;
			}
		}
		
		return false;
	}
	
	public void unSet( String variable ){
		for( int c = 0; c < vars.size(); c++){
			if( vars.get(c).equalsIgnoreCase(variable) ){
				vars.remove(c);
				return;
			}
		}
	}
	
	public void set(String variable){
		if( !isSet(variable) ){
			vars.add(variable);
		}
	}
	
	public int size(){
		return vars.size();
	}
	
	public String get(int c){
		return vars.get(c);
	}

}
