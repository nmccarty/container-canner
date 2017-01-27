package edu.uky.rc;

/**
 * Created by nmccarty on 1/27/17.
 */
public class DockerVolume {
    private DockerContainer container;
    private String internalPath;

    public DockerVolume(DockerContainer c, String ip){
        container = c;
        internalPath = ip;
    }

    public DockerContainer getContainer() {
        return container;
    }

    public String getInternalPath() {
        return internalPath;
    }

}
