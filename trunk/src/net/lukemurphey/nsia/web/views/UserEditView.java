package net.lukemurphey.nsia.web.views;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lukemurphey.nsia.web.RequestContext;
import net.lukemurphey.nsia.web.URLInvalidException;
import net.lukemurphey.nsia.web.View;
import net.lukemurphey.nsia.web.ViewFailedException;
import net.lukemurphey.nsia.web.ViewNotFoundException;
import net.lukemurphey.nsia.web.forms.Field;
import net.lukemurphey.nsia.web.forms.Form;
import net.lukemurphey.nsia.web.forms.PasswordValidator;
import net.lukemurphey.nsia.web.forms.PatternValidator;

public class UserEditView extends View {

	public static final String VIEW_NAME = "user_editor"; 
	
	public UserEditView() {
		super("User", VIEW_NAME, Pattern.compile("(New)|(Edit)", Pattern.CASE_INSENSITIVE), Pattern.compile("[0-9]*"));
	}

	/**
	 * Get a form that can validate the site group.
	 * @return
	 */
	private Form getUserEditForm( ){
		Form form = new Form();
		
		form.addField( new Field("Name", new PatternValidator(Pattern.compile("[-A-Z0-9a-z_ .]{1,32}", Pattern.CASE_INSENSITIVE))) );
		form.addField( new Field("Full Name", new PatternValidator(Pattern.compile("[-A-Z0-9a-z_ ().]{1,128}", Pattern.CASE_INSENSITIVE))) );
		form.addField( new Field("Email Address", new PatternValidator(Pattern.compile("[-a-z0-9 _]{1,32}", Pattern.CASE_INSENSITIVE))) );
		form.addField( new Field("Password", new PasswordValidator()) );
		form.addField( new Field("Password Confirmation") );
		form.addField( new Field("Account Type") );
		
		return form;
	}
	
	@Override
	protected boolean process(HttpServletRequest request,
			HttpServletResponse response, RequestContext context,
			String[] args, Map<String, Object> data)
			throws ViewFailedException, URLInvalidException, IOException,
			ViewNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

}
