package edu.uky.rc;

import com.spotify.docker.client.exceptions.DockerException;
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

        // Create a temporary scratch directory
        Path tmpDir = Files.createTempDirectory("containercanner"+container.getContainerID());
        logger.info("Created temp dir at: " + tmpDir.toFile().getAbsolutePath());

        // Create the tarchive file
        File tarchive = File.createTempFile("cannedContainer"+container.getContainerID(),".tar");

        // First, stop the container
        // TODO: Check to see if the container is running
        // TODO: Snapshot and stop instead of just stop
        boolean checkpointCreated = false;
        File checkpoint = null;

        if(container.running()){
            logger.info("Stopping container " + container.getContainerID());
            checkpointCreated = true;
            checkpoint = container.checkpoint();
        } else {
            logger.info("Container already stopped.");
        }

        // Export the container
        File containerFile = container.exportContainer();


        // Save the state of the volumes
        Map<String,File> vols = container.saveVolumes();

        // Move pickled volumes and add them to volume map
        int volID = 0;
        for(String s : vols.keySet()){
            volumeMap.put(s,volID+".tar");
            File f = vols.get(s);
            File newFile = moveFile(f, fileInDir(tmpDir, volID+".tar"));
            logger.info("Moved " + f.toPath() + " to " + newFile.toPath());
            volID+=1;
        }
        // Move the exported container
        File newLocation = moveFile(containerFile, fileInDir(tmpDir,"container.tar"));
        logger.info("Moved " + containerFile.toPath() + " to " + newLocation.toPath());
        // If a checkpoint was created, move it
        if(checkpointCreated){
            File newFile = moveFile(checkpoint, fileInDir(tmpDir,"checkpoint.tar"));
            logger.info("Moved " + checkpoint.toPath() + " to " + newFile.toPath());
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
        File volumeMapFile = fileInDir(tmpDir,"volumeMap");
        Files.write(volumeMapFile.toPath(),
                volMap.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);

        // Tar it all up
        try {
            // TODO: Replace with jTar
            String command = "tar -cf " + tarchive.getAbsolutePath() + " -C " + tmpDir.toFile().getAbsolutePath() + " .";
            CanningUtils.runCommand(command,logger);

        } catch (InterruptedException e){
            logger.error("Error making final tarball",e);
            // TODO: Proper error handling
            throw new RuntimeException(e);
        }

        // Cleanup tempfiles that are no longer needed
        // Good thing we contained them all in one easy to manage folder
        for(File f : tmpDir.toFile().listFiles()){
            logger.debug("Removing tmpfile: "+f.getAbsolutePath());
            f.delete();
        }
        logger.debug("Removing tmpDir: "+tmpDir.toFile().getAbsolutePath());
        tmpDir.toFile().delete();

        try {
            // TODO: Compression Configuration and smart use of threads
            String command = "xz -1 " + tarchive.getAbsolutePath() + " --threads=4";
            CanningUtils.runCommand(command,logger);

        } catch (InterruptedException e){
            logger.error("Error compressing tar",e);
            throw new RuntimeException(e);
        }

        return new File(tarchive.getAbsolutePath().concat(".xz"));
    }

    private File moveFile(File from, File to) throws IOException {
        Files.move(from.toPath(), to.toPath());
        return to;
    }

    private File fileInDir(Path dir, String name){
        return new File(dir.toFile().getAbsoluteFile(),name);
    }

}
