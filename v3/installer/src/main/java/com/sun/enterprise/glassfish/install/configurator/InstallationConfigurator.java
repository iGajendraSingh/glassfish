/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER. 
* 
* Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved. 
* 
* The contents of this file are subject to the terms of either the GNU 
* General Public License Version 2 only ("GPL") or the Common Development 
* and Distribution License("CDDL") (collectively, the "License").  You 
* may not use this file except in compliance with the License. You can obtain 
* a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html 
* or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific 
* language governing permissions and limitations under the License. 
* 
* When distributing the software, include this License Header Notice in each 
* file and include the License file at glassfish/bootstrap/legal/LICENSE.txt. 
* Sun designates this particular file as subject to the "Classpath" exception 
* as provided by Sun in the GPL Version 2 section of the License file that 
* accompanied this code.  If applicable, add the following below the License 
* Header, with the fields enclosed by brackets [] replaced by your own 
* identifying information: "Portions Copyrighted [year] 
* [name of copyright owner]" 
* 
* Contributor(s): 
* 
* If you wish your version of this file to be governed by only the CDDL or 
* only the GPL Version 2, indicate your decision by adding "[Contributor] 
* elects to include this software in this distribution under the [CDDL or GPL 
* Version 2] license."  If you don't indicate a single choice of license, a 
* recipient has the option to distribute your version of this file under 
* either the CDDL, the GPL Version 2 or to extend the choice of license to 
* its licensees as provided above.  However, if you add GPL Version 2 code 
* and therefore, elected the GPL Version 2 license, then the option applies 
* only if the new code is made subject to such option by the copyright 
* holder. 
*/ 

package com.sun.enterprise.glassfish.install.configurator;


import org.openinstaller.provider.conf.ResultReport;
import org.openinstaller.provider.conf.Configurator;
import org.openinstaller.config.PropertySheet;
import org.openinstaller.util.EnhancedException;
import org.openinstaller.util.ExecuteCommand;
import org.openinstaller.util.ClassUtils;
import javax.management.Notification;
import javax.management.NotificationListener;
import java.io.File;
import java.io.FileWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class InstallationConfigurator implements Configurator {

private final String productName;
private final String altRootDir;
private final String xcsFilePath;
private final String installDir;

private int gWaitCount = 0;
private String productError = null;

private final static String GLASSFISH_PRODUCT_NAME = "glassfish";
private final static String UPDATETOOL_PRODUCT_NAME = "updatetool";

private static final Logger LOGGER;

static {
    LOGGER = Logger.getLogger(ClassUtils.getClassName());
}

public InstallationConfigurator(String aProductName, String aAltRootDir,
     String aXCSFilePath, String aInstallDir) {

    LOGGER.log(Level.INFO, "In configurator constructor");
    productName = aProductName;
    altRootDir = aAltRootDir;
    xcsFilePath = aXCSFilePath;
    installDir = aInstallDir;

    
}


public ResultReport configure (PropertySheet aSheet, boolean aValidateFlag) throws EnhancedException {

    LOGGER.log(Level.INFO, "In configure method");

    try {
        if (productName.equals(GLASSFISH_PRODUCT_NAME)) {
            configureGlassfish(
                aSheet.getProperty("Administration.A_INSTALL_HOME"),
                aSheet.getProperty("Administration.A_ADMIN_PORT"),
                aSheet.getProperty("Administration.A_HTTP_PORT"));
	    }
     }
     catch (Exception e) {
         throw new EnhancedException(e);
     }
    
     return new ResultReport(ResultReport.ResultStatus.SUCCESS, "Documentation", "Next Steps", null, productError);
         
}


public PropertySheet getCurrentConfiguration() {

    return new PropertySheet();
}


public ResultReport unConfigure (PropertySheet aSheet, boolean aValidateFlag) {

    return new ResultReport(ResultReport.ResultStatus.SUCCESS, "Documentation", "Next Steps", null, productError);
}

public void handleNotification (final Notification aNotification,
    final Object aHandback) {
    /* We received a message from the configurator, so reset the count */
    synchronized(this) {
      gWaitCount = 0;
    }
}


void configureGlassfish(String installDir, String adminPort, String httpPort) throws Exception {

    // set executable permissions on asadmin, stopserv, startserv 

    boolean isWindows = false;
    if (System.getProperty("os.name").indexOf("Windows") !=-1 ) {
        isWindows=true;
    }

    if (!isWindows) {

        String CLInames[] = {"asadmin", "stopserv", "startserv"};
        for (int i = 0; i < CLInames.length; i++) {
            Runtime.getRuntime().exec("/bin/chmod a+x " +
                               installDir + "/glassfish/bin/" + CLInames[i]);
		}
    }
    
    //create temporary password file for asadmin create-domain

        FileWriter writer = null;
        File pwdFile = null;        
        try {            
            pwdFile = File.createTempFile("asadminTmp", null);                        
            pwdFile.deleteOnExit();            
            writer = new FileWriter(pwdFile);            
            writer.write("AS_ADMIN_ADMINPASSWORD=adminadmin\n");
            writer.write("AS_ADMIN_PASSWORD=adminadmin\n");
            writer.write("AS_ADMIN_MASTERPASSWORD=changeit\n");
            writer.close();
            writer = null;
            if (!isWindows)
	        {
	            Runtime.getRuntime().exec("/bin/chmod 600 " + pwdFile.getAbsolutePath());
	        }      
            
        } catch (Exception ex) {
            // ensure that we delete the file should any exception occur
            if (pwdFile != null) {
                try {
                    pwdFile.delete();
                } catch (Exception ex2) {
                    //ignore we are cleaning up on error
                }                
            }
            throw ex; 
        } finally {
            //ensure that we close the file no matter what.
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ex2) {
                    //ignore we are cleaning up on error
                }                
            }
        }
 
    //construct asadmin command

        try {

            String asadminCommand;
        
            if (isWindows) {
                 asadminCommand = installDir + "\\glassfish\\bin\\asadmin.bat";
            }
            else {
                asadminCommand = installDir + "/glassfish/bin/asadmin";
            }

            String[] asadminCommandArray = { asadminCommand, "create-domain",
                "--savelogin",
                "--adminport", adminPort,
                "--user", "admin",
                "--passwordfile", pwdFile.getAbsolutePath(),
                "--instanceport", httpPort,
                "domain1"};
                
            ExecuteCommand asadminExecuteCommand = new ExecuteCommand(asadminCommandArray);
            asadminExecuteCommand.setOutputType(ExecuteCommand.ERRORS);
            asadminExecuteCommand.setCollectOutput(true);
        
            asadminExecuteCommand.execute();

            productError = asadminExecuteCommand.getErrors();
       } catch (Exception e) {

            
       }
}

}