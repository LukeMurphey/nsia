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

public class WebDiscoveryRuleEditView extends View {

	public static final String VIEW_NAME = "";
	
	public WebDiscoveryRuleEditView() {
		super("Rule", VIEW_NAME, Pattern.compile("New|Edit", Pattern.CASE_INSENSITIVE), Pattern.compile("[0-9]*"));
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
