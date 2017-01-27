package edu.uky.rc;

import com.spotify.docker.client.*;
/**
 * Created by nmccarty on 1/27/17.
 */
public class DockerContainer {
    private static DockerClient docker = new DefaultDockerClient("unix:///var/run/docker.sock");

    private String containerID;

    public DockerContainer(String id){
        containerID = id;
    }

    public String getContainerID() {
        return containerID;
    }

}
