package net.lukemurphey.nsia.tools;

import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.NetworkManager;
import net.lukemurphey.nsia.Application.RunMode;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;

import javax.imageio.*;

public class AppLaunch {
	
	protected enum DialogState{
		NSIA_READY, NSIA_STARTING, NSIA_RUNNING, NSIA_TERMINATING;
	}
	
	protected enum Icon{
		OK, WARNING, INFORMATION, NONE;
	}
	
	protected enum Action{
		START, SHUTDOWN
	}
	
	private Button actionButton;
	private ProgressBar progressBar;
	private Link mainText;
	private Label mainImageLabel;
	private Label sectionLabel;
	private Display display;
	private Shell shell;
	private DialogState state = DialogState.NSIA_READY;
	protected boolean exitWhenDone = false;
	protected boolean trayIconMessageShown = false;
	protected TrayIcon trayIcon;
	
	protected Image getImage(String name){
		return new Image(display, this.getClass().getResourceAsStream(name));
	}
	
	protected java.awt.Image getAWTImage(String name){
		try {
			return ImageIO.read(this.getClass().getResourceAsStream(name));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private void open(){
		display = new Display ();
		
		shell = new Shell (display, SWT.MIN);
		shell.setSize(444, 333);
		shell.setMinimumSize(444, 333);
		shell.setText("ThreatFactor NSIA (version " + Application.getVersion() + ")");
		Image appIcon = getImage("appicon.png");
		shell.setImage(appIcon);
		
		// Create the header image
		Label headerImageLabel = new Label (shell, SWT.NONE);
		Image bannerImage = getImage("ThreatFactor.png");
		headerImageLabel.setImage(bannerImage);
		headerImageLabel.setLocation(0,0);
		headerImageLabel.setSize(444, 60);
		
		// Create the tray icon that is displayed when the application is minimized
		trayIcon = new TrayIcon( getAWTImage("appicon.png") );
		
		MouseListener mouseListener = new MouseListener() {
	        public void mouseClicked(MouseEvent e) { }
	        public void mouseEntered(MouseEvent e) { }
	        public void mouseExited(MouseEvent e) { }
	        public void mousePressed(MouseEvent e) { }
	        public void mouseReleased(MouseEvent e) {
	        	display.asyncExec(new Runnable() {
	                public void run() {
	                	shell.setVisible(true);
	    				shell.setMinimized(false);
	    			}
	        		}
	        	);
	        	
	        	
				SystemTray.getSystemTray().remove(trayIcon);
	        }
	    };
	    
	    trayIcon.addMouseListener(mouseListener);
		
		// Create the hide button
		Button hideButton = new Button(shell, SWT.NONE);
		hideButton.setLocation(370, 275);
		hideButton.setSize(64, 22);
		hideButton.setText("Hide");
		hideButton.addListener (SWT.Selection, new Listener(){
			public void handleEvent (Event event) {

				try{
					SystemTray.getSystemTray().add(trayIcon);
					if( trayIconMessageShown == false ){
						trayIcon.displayMessage("NSIA Is Minimized", "I'm still running; click the tray icon to show the dialog again", TrayIcon.MessageType.INFO );
						trayIcon.setToolTip("I'm still running; click the tray icon to show the dialog again");
						trayIconMessageShown = true;
					}
				}
				catch(AWTException e){
					//e.printStackTrace();
				}

				trayIconMessageShown = true;

				shell.setMinimized(true);
				shell.setVisible(false);
			}
		} );
		
		// Create the website link
		Link link = new Link(shell, SWT.NONE);
		link.setText("<a href=\"http://ThreatFactor.com\">http://ThreatFactor.com</a>");
		link.setSize(200, 22);
		link.setLocation(20, 275);
		link.addListener (SWT.Selection, new Listener(){
			public void handleEvent (Event event) {
				threatFactorLink(event);
			}
		} );
		
		// Create the separator
		Label separator = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setSize(444, 22);
		separator.setLocation(0, 250);
		
		// Create the action button
		actionButton = new Button(shell, SWT.NONE);
		actionButton.setLocation(100, 180);
		actionButton.setSize(128, 22);
		actionButton.setText("Start NSIA");
		actionButton.addListener (SWT.Selection, new Listener(){
			public void handleEvent (Event event) {
				runAction(event);
			}
		} );
		
		// Create the icon box
		mainImageLabel = new Label (shell, SWT.NONE);
		Image dialogImage = getImage("Information.png");
		mainImageLabel.setImage(dialogImage);
		mainImageLabel.setLocation(60, 110);
		mainImageLabel.setSize(40, 40);
		
		// Create the section title box
		sectionLabel = new Label (shell, SWT.NONE);
		sectionLabel.setFont( new Font(display, "Tahoma", 10, SWT.BOLD) );
		sectionLabel.setSize(300, 17);
		sectionLabel.setLocation(100, 110);
		
		// Create the section message box
		mainText = new Link (shell, SWT.WRAP);
		mainText.setSize(310, 40);
		mainText.setLocation(100, 130);
		mainText.addListener (SWT.Selection, new Listener(){
			public void handleEvent (Event event) {
				adminInterfaceLink(event);
			}
		} );
		
		// Create the progress bar
		progressBar = new ProgressBar(shell, SWT.INDETERMINATE);
		progressBar.setLocation(100, 180);
		progressBar.setSize(256, 16);
		progressBar.setVisible(false);

		shell.addListener (SWT.Close, new Listener () {
			public void handleEvent (Event event) {
				if(state == DialogState.NSIA_RUNNING){
					int style = SWT.APPLICATION_MODAL | SWT.YES | SWT.NO | SWT.ICON_QUESTION;
					MessageBox messageBox = new MessageBox (shell, style);
					messageBox.setText ("Close NSIA");
					messageBox.setMessage ("Are you sure you want to close NSIA? Closing this window will also shutdown the NSIA IDS.");
					
					if( messageBox.open() == SWT.YES ){
						exitWhenDone = true;
						runAction(null);
					}
					
					event.doit = false;
				}
				else if(state == DialogState.NSIA_TERMINATING || state == DialogState.NSIA_STARTING){
					event.doit = false;
				}
				else{
					System.exit(0);
				}
			}
		});
		
		setDialog(DialogState.NSIA_READY);
		
		shell.pack ();
		shell.open ();
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose ();
	}
	
	public void setDialog( DialogState state ){
		
		this.state = state;
		
		if( state == DialogState.NSIA_READY ){
			setDialog("ThreatFactor NSIA is Not Running", "NSIA is not currently running. Start the system to open the management interface and begin monitoring.", Icon.INFORMATION, false, "Start NSIA");
		}
		else if( state == DialogState.NSIA_RUNNING ){
			setDialog("ThreatFactor NSIA is Running", "NSIA is currently running. Browse to <a>" + getURL() + "</a> to access the management interface.", Icon.OK, false, "Shutdown NSIA");
			//setDialog("ThreatFactor NSIA is Running", "NSIA is currently running. Browse to <a>http://127.0.0.1:8080</a> to access the management interface.", Icon.OK, false, "Shutdown NSIA");
		}
		else if( state == DialogState.NSIA_STARTING ){
			setDialog("ThreatFactor NSIA is Starting", "NSIA has been given the start command. Please wait while the system initializes.", Icon.INFORMATION, true, null);
		}
		else if( state == DialogState.NSIA_TERMINATING ){
			setDialog("ThreatFactor NSIA is Terminating", "NSIA has been given the stop command. Please wait while the system terminates.", Icon.INFORMATION, true, null);
		}
	}
	
	private String stripHTML( String msg ){
		
		Pattern pattern = Pattern.compile("</?[a-zA-Z0-9]>");
		Matcher matcher = pattern.matcher(msg);
		return matcher.replaceAll("");
	}
	
	public void setDialog( String title, String message, Icon icon, boolean showProgressBar, String actionButtonText ){
		
		sectionLabel.setText(title);
		mainText.setText(message);
		
		trayIcon.displayMessage(title, stripHTML( message ) , TrayIcon.MessageType.INFO);
		trayIcon.setToolTip(stripHTML( message ));
		
		if( icon == Icon.WARNING ){
			mainImageLabel.setVisible(true);
			Image dialogImage = getImage("Warning.png");
			mainImageLabel.setImage(dialogImage);
		}
		else if( icon == Icon.OK ){
			mainImageLabel.setVisible(true);
			Image dialogImage = getImage("Check.png");
			mainImageLabel.setImage(dialogImage);
		}
		else if( icon == Icon.NONE ){
			mainImageLabel.setVisible(false);
		}
		else //if( icon == Icon.INFORMATION ){
		{
			mainImageLabel.setVisible(true);
			Image dialogImage = getImage("Information.png");
			mainImageLabel.setImage(dialogImage);
		}
		
		if( showProgressBar ){
			progressBar.setVisible(true);
		}
		else{
			progressBar.setVisible(false);
		}
		
		if( actionButtonText == null ){
			actionButton.setVisible(false);
		}
		else{
			actionButton.setVisible(true);
			actionButton.setText(actionButtonText);
		}
	}
	
	public static void main (String [] args) {
		AppLaunch launch = new AppLaunch();
		
		launch.open();
	}
	
	protected class CheckStatusThread extends Thread{
		
		@Override
		public void run(){
			boolean continueExecuting = true; 
			while(continueExecuting){
				
				// Pause for one second
				try{
					sleep(1000);
				}
				catch(InterruptedException e){
					
				}
				
				// Determine if the application is still running
				Application app = Application.getApplication();

				if( state == AppLaunch.DialogState.NSIA_RUNNING && app == null ){
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							setDialog(DialogState.NSIA_READY);
						}
					}
					);

					continueExecuting = false;
				}
				
				/*if( app != null ){
					ApplicationStatusDescriptor desc = app.getManagerStatus();
					if( desc.getStatusEntry("Scanner Status").getStatus() != ApplicationStatusDescriptor.STATUS_GREEN ){
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								ApplicationStatusDescriptor desc = Application.getApplication().getManagerStatus();
								setDialog("NSIA System Status Warning", desc.getLongDescription(), Icon.WARNING, false, "Shutdown NSIA" );
							}
						}
						);
					}
				}*/
			}
		}
		
		
	}
	
	protected class ActionWorker extends Thread{
		
		private Action action;
		
		public ActionWorker( Action action ){
			this.action = action;
		}
		
		@Override
		public void run(){

			/*try{
				Thread.sleep(5000);
			}
			catch(InterruptedException e){
				
			}*/
			
			if( action == Action.START ){
				try{
					Application.startApplication(new String[0], RunMode.GUI);
				}
				catch(Exception e){
					
				}
				
				Display.getDefault().asyncExec(new Runnable() {
		               public void run() {
		            	   setDialog(DialogState.NSIA_RUNNING);
		               }
		            }
				);
				
				CheckStatusThread checkStat = new CheckStatusThread();
				checkStat.setName("NSIA Status Monitor");
				checkStat.start();
				
			}
			else if( action == Action.SHUTDOWN ){
				Application.getApplication().shutdown();
				
				Display.getDefault().asyncExec(new Runnable() {
		               public void run() {
		            	   setDialog(DialogState.NSIA_READY);
		               }
		            }
				);
				
				if( exitWhenDone ){
					Display.getDefault().asyncExec(new Runnable() {
			               public void run() {
			            	   display.dispose(); //This ensures that the tray icon disappears
			               }
			            }
					);
					
					System.exit(0);
				}
			}
		}
		
	}
	
	public void runAction(Event event){
		
		if( state == DialogState.NSIA_READY ){
			setDialog(DialogState.NSIA_STARTING);
			
			ActionWorker worker = this.new ActionWorker(Action.START);
			worker.start();
		}
		else if( state == DialogState.NSIA_RUNNING ){
			setDialog(DialogState.NSIA_TERMINATING);
			
			ActionWorker worker = this.new ActionWorker(Action.SHUTDOWN);
			worker.start();
		}
	}
	
	public static String getURL(){
		String link = null;
		Application app = Application.getApplication();
		
		if( app != null ){
			NetworkManager netManager = app.getNetworkManager();
			boolean ssl = false;
			int port;
			
			if( netManager != null ){
				ssl = netManager.sslEnabled();
				port = netManager.getServerPort();
				
				if( ssl && port == 443){
					link = "https://127.0.0.1";
				}
				else if( ssl ){
					link = "https://127.0.0.1:" + port;
				}
				if( !ssl && port == 80){
					link = "https://127.0.0.1";
				}
				else{
					link = "http://127.0.0.1:" + port;
				}
			}
		}
		
		return link;
		
	}
	
	public static void adminInterfaceLink(Event event){
		String link = getURL();
		
		Program program = Program.findProgram("html");
		program.execute(link);
	}
	
	public static void threatFactorLink(Event event){
		Program program = Program.findProgram("html");
		program.execute("http://ThreatFactor.com");
	}

}
