package edu.uky.rc;

import com.google.common.collect.ImmutableList;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerMount;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

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
        try {
            File temp = File.createTempFile("docker-volume"+internalPath, ".tar");
            // TODO: Use jTar to remove platform dependencies
            String command = "tar -cf " + temp.getAbsolutePath() + " -C " + getExternalPath() + " .";

            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();

            return temp;
        } catch (IOException|InterruptedException e) {
            // TODO: Error recovery?
            e.printStackTrace();
            return null;
        }
    }

    public void loadVolume(File archive){
        throw new UnsupportedOperationException();
    }


}
