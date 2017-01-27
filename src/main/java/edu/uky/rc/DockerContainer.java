package edu.uky.rc;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.spotify.docker.client.*;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.Volume;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by nmccarty on 1/27/17.
 */
public class DockerContainer {
    protected static DockerClient docker = new DefaultDockerClient("unix:///var/run/docker.sock");

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

    public List<DockerVolume> getVolumes(){
        try {
            ArrayList<DockerVolume> dockerVolumes = new ArrayList<>();
            ImmutableMap<String, java.util.Map> volumeMap = docker.inspectContainer(containerID).config().volumes();
            ImmutableSet<String> volumes = volumeMap.keySet();
            for(String vol:volumes){
                dockerVolumes.add(new DockerVolume(this, vol));
            }
            return dockerVolumes;
        } catch (DockerException|InterruptedException  e) {
            e.printStackTrace();
            return new ArrayList<>();
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
