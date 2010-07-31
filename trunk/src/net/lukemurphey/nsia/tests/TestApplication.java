package net.lukemurphey.nsia.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.martiansoftware.jsap.JSAPException;

import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GenericUtils;
import net.lukemurphey.nsia.NoDatabaseConnectionException;

public class TestApplication {

	private static Application app = null;
	public static String DEFAULT_TEST_DATABASE_PATH = "tmp/test_database";
	
	public static Application getApplication() throws TestApplicationException{
		return getApplication( false );
	}
	public synchronized static Application getApplication( boolean startServices ) throws TestApplicationException{
		try{
			if( app == null ){
				createDatabaseCopy();
				app = new Application( DEFAULT_TEST_DATABASE_PATH, startServices );
			}
		}
		catch(IOException e ){
			throw new TestApplicationException(e);
		} catch (NoDatabaseConnectionException e) {
			throw new TestApplicationException(e);
		} catch (JSAPException e) {
			throw new TestApplicationException(e);
		}
		
		return app;
	}
	
	public synchronized static void stopApplication(){
		
		if( app != null ){
			app.shutdown(true);
			app = null;
		}
	}
	
	private static void createDatabaseCopy() throws IOException{
		
		deleteDatabase();
		
		copyDirectory( new File("dev/test/test_database"), new File(DEFAULT_TEST_DATABASE_PATH) );
	}
	
	private static void deleteDatabase(){
		File test_db = new File(DEFAULT_TEST_DATABASE_PATH);
		GenericUtils.deleteDirectory( test_db );
	}
	
    private static void copyDirectory(File sourceLocation , File targetLocation) throws IOException {
        
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            
            String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {
            
            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);
            
            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }
	
}
