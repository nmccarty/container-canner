package edu.uky.rc;

import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nmccarty on 2/23/17.
 */
public class CannedContainer {
    // TODO: make container optional to facilitate uncanning
    private DockerContainer container;
    private HashMap<String,String> volumeMap;


    public CannedContainer(String id){
        container = new DockerContainer(id);
        volumeMap = new HashMap<>();
    }

    public File can() throws IOException {
        // Get a logger
        Logger logger = LoggerFactory.getLogger(CannedContainer.class);
        // First, stop the container
        // TODO: Snapshot and stop instead of just stop
        logger.info("Stopping container " + container.getContainerID());
        container.stop();

        // Save the state of the volumes
        Map<String,File> vols = container.saveVolumes();

        // Create a temporary scratch directory
        Path tmpDir = Files.createTempDirectory("containercanner"+container.getContainerID());
        logger.info("Created temp dir at: " + tmpDir.toFile().getAbsolutePath());

        // Move pickled volumes and add them to volume map
        int volID = 0;
        for(String s : vols.keySet()){
            volumeMap.put(s,volID+".tar");
            File f = vols.get(s);
            File newFile = new File(tmpDir.toFile().getAbsolutePath(), volID+".tar");
            // Delete the created file so we can move on top of it
            newFile.delete();
            Files.move(f.toPath(),newFile.toPath());
            logger.info("Moved " + f.toPath() + " to " + newFile.toPath());
        }

        // Build the volume map file up
        StringBuilder volMap = new StringBuilder();
        for(String path : volumeMap.keySet()){
            // Format is:
            // "/path/goes/here":"nameInArchive.tar"
            volMap.append("\"").append(path).append("\"");
            volMap.append(":");
            volMap.append("\"").append(volumeMap.get(path)).append("\"");
            volMap.append("\n");
        }

        // Write volume map
        File volumeMapFile = new File(tmpDir.toFile().getAbsolutePath(),"volumeMap");
        Files.write(volumeMapFile.toPath(),
                volMap.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);

        // Tar it all up
        File tarchive = File.createTempFile("cannedContainer"+container.getContainerID(),".tar");
        try {
            // TODO: Replace with jTar
            String command = "tar -cf " + tarchive.getAbsolutePath() + " -C " + tmpDir.toFile().getAbsolutePath() + " .";
            logger.info(command);

            StringBuffer output = new StringBuffer();

            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line = "";
            while((line = reader.readLine()) != null){
                output.append(line + "\n");
            }

            if(output.toString().length() !=0) {
                logger.info(output.toString());
            }

        } catch (InterruptedException e){
            logger.error("Error making final tarball",e);
            // TODO: Proper error handling
            throw new RuntimeException(e);
        }


        return tarchive;
    }

}
