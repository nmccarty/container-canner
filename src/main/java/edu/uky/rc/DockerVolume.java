package edu.uky.rc;

import com.google.common.collect.ImmutableList;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerMount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

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

            logger.info(output.toString());


            return temp;
        } catch (IOException|InterruptedException e) {
            // TODO: Error recovery?
            logger.error("Saving volume state failed, for whatever reason.",e);
            e.printStackTrace();
            return null;
        }
    }

    public void loadVolume(File archive){
        throw new UnsupportedOperationException();
    }


}
