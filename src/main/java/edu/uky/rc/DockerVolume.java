package edu.uky.rc;

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

}
