package net.lukeMurphey.nsia;

import java.sql.*;
import java.util.regex.*;

/**
 * The application parameter class contains various name/value pairs that are intended to configure the application's behavior. The values are stored in a persistent manner.
 * @author luke
 *
 */
public class ApplicationParameters {
	private Pattern nameValidation = null;
	private static final String NAME_VALIDATION = "[.A-Z0-9a-z_]*";
	private Application application = null;
	
	public ApplicationParameters(){
		nameValidation = Pattern.compile(NAME_VALIDATION);
		application = Application.getApplication();
	}
	
	public ApplicationParameters( Application app ){
		nameValidation = Pattern.compile(NAME_VALIDATION);
		application = app;
	}
	
	/**
	 * Attempts to retrieve the value of the given attribute. Will return the default value if found.
	 * @precondition The argument shouldn't be null and the database connection must be set or an exception will be sent.
	 * @postcondition The value of the given parameter will be returned, or the default value if not found
	 * @param name The name of the value to retrieve.
	 * @param defaultValue The value to return if the value is not found.
	 * @return The value of the given parameter, or the default value if not found
	 */
	public String getParameter( String name, String defaultValue ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		
		// 0 -- Make sure preconditions are met
		
		//	 0.1 -- Make sure the name is not null
		if( name == null )
			return defaultValue;
		
		//	 0.2 -- Make sure that the name is allowed (security check)
		Matcher matcher = nameValidation.matcher(name);
		if( !matcher.matches() )
			throw new InputValidationException("Application parameter name is invalid; get operation failed", name, defaultValue);
		
		//	 0.3 -- Make sure a database connection is available
		Connection connection = null;
		
		connection = application.getDatabaseConnection( Application.DatabaseAccessType.ADMIN );
		
		if( connection == null )
			throw new NoDatabaseConnectionException();
		
		// 1 -- Attempt to retrieve the value
		PreparedStatement statement = null;
		ResultSet result = null;
		String value = null;
		
		try{
			statement = connection.prepareStatement("Select * from ApplicationParameters where Name = ?");
			statement.setString(1,name);
			
			result = statement.executeQuery();
			
			// 2 -- Return the result of found
			
			if( result.next() )
				value = result.getString("Value");
			else
				return defaultValue;
			
		} finally {
			if (result != null )
				result.close();
			
			if (statement != null )
				statement.close();
			
			if (connection != null )
				connection.close();
		}
		
		return value;
		
	}
	
	/**
	 * Attempts to retrieve the value of the given attribute. Will return the default value if found.
	 * @precondition The argument shouldn't be null and the database connection must be set or an exception will be sent.
	 * @postcondition The value of the given parameter will be returned, or the default value if not found
	 * @param name The name of the value to retrieve.
	 * @param defaultValue The value to return if the value is not found.
	 * @return The value of the given parameter, or the default value if not found
	 */
	public long getParameter( String name, long defaultValue ) throws NoDatabaseConnectionException, SQLException, InputValidationException{
		synchronized( this ){

			// 0 -- Make sure preconditions are met

			//	 0.1 -- Make sure the name is not null
			if( name == null )
				return defaultValue;

			//	 0.2 -- Make sure that the name is allowed (security check)
			Matcher matcher = nameValidation.matcher(name);
			if( !matcher.matches() )
				throw new InputValidationException("Application parameter name is invalid; get operation failed", name, String.valueOf(defaultValue));

			//	 0.3 -- Make sure a database connection is available
			Connection connection = null;

			connection = application.getDatabaseConnection( Application.DatabaseAccessType.ADMIN );

			if( connection == null )
				throw new NoDatabaseConnectionException();

			// 1 -- Attempt to retrieve the value
			PreparedStatement statement = null;
			ResultSet result = null;
			try{
				statement = connection.prepareStatement("Select * from ApplicationParameters where Name = ?");
				statement.setString(1,name);

				result = statement.executeQuery();

				// 2 -- Return the result of found
				if( result.next() )
					return result.getLong("Value");
				else
					return defaultValue;

			} finally {
				if (result != null )
					result.close();

				if (statement != null )
					statement.close();

				if (connection != null )
					connection.close();
			}
		}
	}
	
	/**
	 * Stores the value of the given parameter.
	 * @precondition The argument shouldn't be null and the database connection must be set or an exception will be sent.
	 * @postcondition The value of the given parameter will be returned, or the default value if not found
	 * @param name The name of the parameter
	 * @param value The value of the parameter
	 * @throws NoDatabaseConnectionException 
	 */
	public void setParameter( String name, String value ) throws InputValidationException, SQLException, NoDatabaseConnectionException{
		synchronized( this ){
			// 0 -- Precondition checks

			//	 0.1 -- Make sure name and value are not null
			if ( name == null )
				throw new IllegalArgumentException("Parameter name cannot be null");
			else if( value == null )
				throw new IllegalArgumentException("Parameter value cannot be null");

			//	 0.2 -- Name cannot be blank
			if(name.length() == 0)
				throw new IllegalArgumentException("Parameter value cannot be empty");

			//	 0.3 -- Make sure name and value are valid
			Matcher matcher = nameValidation.matcher(name);
			if( !matcher.matches() )
				throw new InputValidationException("Application parameter name is invalid; set operation failed", name, value);

			// 1 -- Set the value of the parameter
			synchronized( application ){ //This is synchronized to deal with possible race condition that exists if the value is deleted between the check and the update
				PreparedStatement statement = null;
				Connection connection = null;
				try{
					connection = Application.getApplication().getDatabaseConnection(Application.DatabaseAccessType.ADMIN);
					if( connection == null )
						throw new NoDatabaseConnectionException();

					if( doesParameterExist( name ) ){

						statement = connection.prepareStatement("Update ApplicationParameters set Value = ? where Name = ?");
						statement.setString(1, value);
						statement.setString(2, name);
						statement.executeUpdate();
					}
					else{
						statement = connection.prepareStatement("Insert into ApplicationParameters (Name, Value) values (?, ?)");
						statement.setString(1, name);
						statement.setString(2, value);
						statement.executeUpdate();
					}

				} finally {

					if (statement != null )
						statement.close();

					if (connection != null )
						connection.close();
				}
			}
		}
	}
	
	/**
	 * Determines if an application parameter matching the given name exists.
	 * @precondition A database connection must be made, the argument must not be null or blank or contain invalid characters
	 * @postcondition A boolean indicating if the name already exists 
	 * @param name The parameter name
	 * @return A boolean indicating if the name already exists
	 */
	public boolean doesParameterExist( String name ) throws InputValidationException, NoDatabaseConnectionException, SQLException{
		
		// 0 -- Precondition check
		// All checks will be performed in get method call below
		
		// 1 -- Determine if field exists
		String value = getParameter(name, null);
		
		//The method returns the default value (null) if the value does not exist.
		if( value == null )
			return false;
		else
			return true;
	}
	
}
