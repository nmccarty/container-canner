package edu.uky.rc;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by nmccarty on 2/28/17.
 */
public class CanningUtils {
    public static boolean runCommand(String command, Logger logger) throws IOException, InterruptedException {
        logger.info("Running:" + command);

        StringBuffer err = new StringBuffer();

        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String line;
        while((line = reader.readLine()) != null){
            err.append(line + "\n");
        }

        int returnCode = p.exitValue();

        if(returnCode != 0){
            logger.error("Command \'" + command +"\' failed.");
            logger.error(err.toString());
            return false;
        } else {
            if(err.toString().length() !=0){
                logger.info(err.toString());
            }

            return true;
        }
    }
}
