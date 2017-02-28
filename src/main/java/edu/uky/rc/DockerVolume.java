package edu.uky.rc;

import com.google.common.collect.ImmutableList;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerMount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.Buffer;

/**
 * Created by nmccarty on 1/27/17.
 */
public class DockerVolume {
    private DockerContainer container;
    private String internalPath;

    public DockerVolume(DockerContainer container, String internalPath){
        this.container = container;
        this.internalPath = internalPath;
    }

    public DockerContainer getContainer() {
        return container;
    }

    public String getInternalPath() {
        return internalPath;
    }

    public String getExternalPath(){
        try {
            ImmutableList<ContainerMount> containerMounts =
                    DockerContainer.docker.inspectContainer(container.getContainerID()).mounts();
            for(ContainerMount mount : containerMounts){
                String dest = mount.destination();
                if(dest.equals(internalPath)){
                    return mount.source();
                }
            }
            return null;
        } catch (DockerException|InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public File saveVolume(){
        // Create a temporary file to store the TARchive in
        Logger logger = LoggerFactory.getLogger(DockerVolume.class);
        try {
            File temp = File.createTempFile("docker-volume"+internalPath, ".tar");
            // TODO: Use jTar to remove platform dependencies
            String command = "tar -cf " + temp.getAbsolutePath() + " -C " + getExternalPath() + " .";
            boolean result = CanningUtils.runCommand(command,logger);
            if(result){
                return temp;
            } else {
                temp.delete();
                return null;
            }
        } catch (IOException|InterruptedException e) {
            // TODO: Error recovery?
            logger.error("Saving volume state failed, for whatever reason.",e);
            e.printStackTrace();
            return null;
        }
    }

    public Boolean loadVolume(File archive){
        // TODO Return failure if any of the commands run within fail
        Logger logger = LoggerFactory.getLogger(DockerVolume.class);
        try {
            // TODO: Make it so this doesn't nuke the everything if the restore goes wrong

            // Remove current contents of volume first, to prevent a conflict
            String command1 = "rm -rf " + getExternalPath() + "/*";
            boolean result1 = CanningUtils.runCommand(command1,logger);

            // TODO: Use jTar instead of tar command
            // Load contents of archive back into Volume
            String command2 = "tar -xf " + archive.getAbsolutePath() + " -C " + getExternalPath();
            boolean result2 = CanningUtils.runCommand(command2,logger);

            return result1 && result2;
        } catch (IOException|InterruptedException e){
            // TODO: Error recovery?
            logger.error("Restoring volume state failed, for whatever reason.",e);
            return false;
        }

    }


}
