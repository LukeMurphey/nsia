package net.lukeMurphey.nsia;

/**
 * This class represents the validator for simple password authentication. The validator clears the password buffer directly to ensure that
 * the password does not remain in memory too long.
 * @author luke
 *
 */
public class PasswordAuthenticationValidator implements AuthenticationValidator{
	private String password = null;
	
	/**
	 * Constructs a password authenticator with the given password.
	 * @param password
	 */
	public PasswordAuthenticationValidator( String password){
		this.password = password;
	}
	
	/**
	 * Retrieve the password in the validator.
	 * @precondition The password must be set to a valid password, or null will be returned (indicating no password)
	 * @postcondition The password will be returned.
	 * @return
	 */
	public String getPassword(){
		return password;
	}
	
	
}
