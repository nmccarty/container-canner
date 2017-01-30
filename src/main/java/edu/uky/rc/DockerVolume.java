package edu.uky.rc;

import com.google.common.collect.ImmutableList;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerMount;

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
            ImmutableList<ContainerMount> containerMounts = DockerContainer.docker.inspectContainer(container.getContainerID()).mounts();
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


}
