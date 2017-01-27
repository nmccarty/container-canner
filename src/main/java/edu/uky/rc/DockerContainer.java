package edu.uky.rc;

import com.spotify.docker.client.*;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;

import java.util.ArrayList;
import java.util.List;

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
    public boolean running(){
        try {
            return docker.inspectContainer(containerID).state().running();
        } catch (DockerException|InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<DockerContainer> listContainers(){
        List<Container> containers;
        try{
            containers = docker.listContainers();
        } catch (DockerException|InterruptedException ex){
            // TODO: Actually do something here, potentially retry or pass the error up
            ex.printStackTrace();
            containers = new ArrayList<>();
        }

        ArrayList<DockerContainer> dockerContainers = new ArrayList<>();
        for(Container c : containers){
            dockerContainers.add(new DockerContainer(c.id()));
        }

        return dockerContainers;
    }

    public static List<DockerContainer> listAllContainers(){
        List<Container> containers;
        try{
            containers = docker.listContainers(DockerClient.ListContainersParam.allContainers());
        } catch (DockerException|InterruptedException ex){
            // TODO: Actually do something here, potentially retry or pass the error up
            ex.printStackTrace();
            containers = new ArrayList<>();
        }

        ArrayList<DockerContainer> dockerContainers = new ArrayList<>();
        for(Container c : containers){
            dockerContainers.add(new DockerContainer(c.id()));
        }

        return dockerContainers;
    }
}
