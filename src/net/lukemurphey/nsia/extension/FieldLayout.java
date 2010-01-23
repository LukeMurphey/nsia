package net.lukemurphey.nsia.extension;

import java.util.Hashtable;
import java.util.Vector;

/**
 * This class is used to represent a set of fields that must be completed to perform an operation or construct an object. This class
 * was created in order to create a system that allows extensions that can be incorporated into the application. The field layout
 * provides a description of the form that should be presented to configure the class. Thus, user interface can be created for the
 * to allow the user to edit the configure of the class.
 * @author Luke Murphey
 *
 */
public class FieldLayout {
	
	private Vector<PrototypeField> fields = new Vector<PrototypeField>();
	private Hashtable<String,String> fieldValueList = null;
	private int width;
	
	/**
	 * Constructs a layout with the given width. For example, a layout of width 2 can accept two fields of width 1 or one field of width 2
	 * per row. 
	 * @param width
	 */
	public FieldLayout(int width){
		
		// 0 -- Precondition check
		if( width <= 0 ){
			throw new IllegalArgumentException("The width cannot be less than zero");
		}
		
		// 1 -- Initialize the class
		this.width = width;
	}
	
	/**
	 * Add the given field to the list of fields that make the layout.
	 * @param field
	 * @return
	 */
	public boolean addField( PrototypeField field ){
		return fields.add( field );
	}
	
	/**
	 * Get a list of the fields in an array.
	 * @return
	 */
	public PrototypeField[] getFields(){
		if( this.fieldValueList != null ){
			return getFields(fieldValueList);
		}
		else{
			return getFields(null);
		}
	}
	
	/**
	 * Sets a list containing the default values for the fields.
	 * @param fieldValueList
	 */
	@SuppressWarnings("unchecked")
	public void setFieldsValues(Hashtable<String,String> fieldValueList){
		this.fieldValueList = (Hashtable<String,String>)fieldValueList.clone();
	}
	
	/**
	 * Get a list of the fields in an array.
	 * @return
	 */
	public PrototypeField[] getFields(Hashtable<String,String> fieldValues){
		
		Vector<PrototypeField> fields;
		
		if( fieldValues != null ){
			fields = getFieldsWithValues(fieldValues);
		}
		else{
			fields = this.fields;
		}
		
		PrototypeField[] fieldsArray = new PrototypeField[fields.size()];
		
		fields.toArray( fieldsArray );
		
		return fieldsArray;
	}
	
	private Vector<PrototypeField> getFieldsWithValues(Hashtable<String,String> fieldValues) {
		Vector<PrototypeField> newFields = new Vector<PrototypeField>(this.fields.size());
		
		for(PrototypeField field : fields){
			if( fieldValues.containsKey(field.getName())){
				try{
					PrototypeField addField = (PrototypeField)field.clone();
					addField.setDefaultValue(fieldValues.get(field.getName()));
					newFields.add(addField);
				}
				catch(CloneNotSupportedException e){
					newFields.add(field);
				}
			}
			else{
				newFields.add(field);
			}
		}
		
		return newFields;
		
	}
	
	/**
	 * Returns a collection of rows containing the fields; the fields should be represented graphically in a table format per the structure
	 * provided.
	 * @return
	 */
	public Vector<FieldRow> getLayout(){
		if( this.fieldValueList != null ){
			return getLayout(fieldValueList);
		}
		else{
			return getLayout(null);
		}
	}
	
	/**
	 * Returns a collection of rows containing the fields; the fields should be represented graphically in a table format per the structure
	 * provided.
	 * @return
	 */
	public Vector<FieldRow> getLayout(Hashtable<String,String> fieldValues){
		
		Vector<FieldRow> fieldRows = new Vector<FieldRow>();
		
		// 1 -- Get the fields with values if provided
		Vector<PrototypeField> fields;
		if( fieldValues != null ){
			fields = getFieldsWithValues(fieldValues);
		}
		else{
			fields = this.fields;
		}
		
		// 2 -- Loop through the layout and create a list of FieldRows that corresponds to the view that should be created
		FieldRow current = null;
		
		for(int c = 0; c < fields.size(); c++){
			
			// 2.1 -- If no field rows exist, then add one
			if( fieldRows.size() == 0 ){
				current = new FieldRow();
				fieldRows.add( current );
				current.addField(fields.get(c));
			}
			
			// 2.2 -- If the current field row has no fields, then add one
			else if( current != null && current.getLayoutWidth() == 0 ){
				current.addField(fields.get(c));
			}
			
			// 2.3 -- If the current field does not fit in the row, then add another row and add the field to the row
			else if( current != null && ( (current.getLayoutWidth() + fields.get(c).getLayoutWidth()) > this.width ) ){
				current = new FieldRow();
				fieldRows.add( current );
				current.addField(fields.get(c));
			}
			
			// 2.4 -- If the current field fits in the row, then add the field to the row
			else if( current != null ){
				current.addField(fields.get(c));
			}
		}
		
		return fieldRows;
		
	}
	
	/**
	 * Represents a row of fields, just like a row in a table.
	 * @author Luke Murphey
	 *
	 */
	public static class FieldRow{
		private Vector<PrototypeField> columns = new Vector<PrototypeField>();
		private int width = -1;
		
		protected boolean addField(PrototypeField field){
			if( width < 0){
				width = 0;
			}
			
			width += field.getLayoutWidth();
			return columns.add(field);
		}
		
		public int getLayoutWidth(){
			if( width < 0 ){
				for (PrototypeField field : columns) {
					width += field.getLayoutWidth();
				}
				
				return width;
			}
			else{
				return width;
			}
		}
		
		public PrototypeField[] getFields(){
			PrototypeField[] fields = new PrototypeField[columns.size()];
			
			columns.toArray(fields);
			return fields;
		}
	}
	
}
