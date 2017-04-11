package edu.uky.rc;

import com.google.common.collect.ImmutableList;
import com.spotify.docker.client.*;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerMount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public boolean start(){
        try {
            docker.startContainer(containerID);
        } catch (DockerException|InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean stop(){
        try {
            docker.killContainer(containerID);
        } catch (DockerException|InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public File checkpoint() throws IOException {
        Logger logger = LoggerFactory.getLogger(DockerContainer.class);

        Path tmpDir = Files.createTempDirectory("snapshot"+getContainerID());
        logger.info("Created tmpDir: "+tmpDir.toFile().getAbsolutePath());

        // Use ACLs to ensure that we have access to the snapshot
        try{
            boolean status;
            String command1 = "setfacl -Rm g:docker:rwX " + tmpDir.toString();
            status = CanningUtils.runCommand(command1,logger);
            String command2 =  "setfacl -Rm default:g:docker:rwX " + tmpDir.toString();
            status = status & CanningUtils.runCommand(command2,logger);
        } catch (InterruptedException e){
            logger.error("Failed to set ACLs, aborting",e);
            return null;
        }

        // Do the snapshot
        // TODO: Make sure this did what it was supposed to, so we don't hand other people junk
        try{
            String command = "docker checkpoint create --checkpoint-dir "
                    + tmpDir.toFile().getAbsolutePath() + " --leave-running=false " +
                    getContainerID() + " checkpoint";
            boolean result = CanningUtils.runCommand(command,logger);

            if(!result){
                return null;
            }

        } catch (InterruptedException e){
            logger.error("Failed to checkpoint container",e);
            throw new RuntimeException(e);
        }

        // Tar it up
        File temp = File.createTempFile("docker-checkpoint"+getContainerID(),".tar");
        try{
            String command = "tar -cf " + temp.getAbsolutePath() +
                    " -C " + tmpDir.toFile().getAbsolutePath() + " .";
            CanningUtils.runCommand(command,logger);
            // Note: we intentionally ignore the exit value here
            // CRIU does some magic that ignores default ACLs resulting in (unimportant) files we can't read
            // If we aren't run as root


        } catch (InterruptedException e){
            logger.error("Failed to tar checkpoint",e);
            throw new RuntimeException(e);
        }

        // Clean it up
        try{
            String command = "rm -rf " + tmpDir.toFile().getAbsolutePath();
            CanningUtils.runCommand(command,logger);

        } catch (InterruptedException e){
            logger.error("Failed to checkpoint container",e);
            throw new RuntimeException(e);
        }

        return temp;
    }

    public List<DockerVolume> getVolumes(){
        try {
            ArrayList<DockerVolume> dockerVolumes = new ArrayList<>();
            ImmutableList<ContainerMount> containerMounts = docker.inspectContainer(containerID).mounts();
            for(ContainerMount vol : containerMounts){
                dockerVolumes.add(new DockerVolume(this, vol.destination()));
            }
            return dockerVolumes;
        } catch (DockerException|InterruptedException  e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public Map<String, File> saveVolumes(){
        HashMap<String, File> vols = new HashMap<>();
        List<DockerVolume> volumes = getVolumes();
        for(DockerVolume v : volumes){
            File output = v.saveVolume();
            vols.put(v.getInternalPath(),output);
        }

        return vols;
    }

    public boolean loadVolumes(Map<String,File> vols){
        // TODO: Check and make sure all volumes in map exist in Container
        List<DockerVolume> volumes = getVolumes();
        boolean status = true;

        for(DockerVolume v: volumes){
            status = status && v.loadVolume(vols.get(v.getInternalPath()));
        }

        return status;
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

    public File exportContainer() throws IOException {
        Logger logger = LoggerFactory.getLogger(DockerContainer.class);
        File containerFile = File.createTempFile("exportedContainer-"+getContainerID(),".tar");
        containerFile.delete();
        try {
            Files.copy(DockerContainer.docker.exportContainer(getContainerID()),containerFile.toPath());
        } catch (DockerException|InterruptedException e){
            logger.error("Exporting docker container Failed",e);
            throw new RuntimeException(e);
        }

        return containerFile;
    }

}
