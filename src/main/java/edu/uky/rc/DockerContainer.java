package edu.uky.rc;

import com.google.common.collect.ImmutableList;
import com.spotify.docker.client.*;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerMount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

        // Do the snapshot
        // TODO: Make sure this did what it was supposed to, so we don't hand other people junk
        try{
            String command = "docker checkpoint create --checkpoint-dir "
                    + tmpDir.toFile().getAbsolutePath() + " --leave-running=false " +
                    getContainerID() + " checkpoint";
            logger.info(command);

            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getErrorStream()));
            StringBuffer output = new StringBuffer();
            String line;
            while((line = reader.readLine())!=null){
                output.append(line + "\n");
            }
            if(output.toString().length() != 0){
                logger.info(output.toString());
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
            logger.info(command);

            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getErrorStream()));
            StringBuffer output = new StringBuffer();
            String line;
            while((line = reader.readLine())!=null){
                output.append(line + "\n");
            }
            if(output.toString().length() != 0){
                logger.info(output.toString());
            }

        } catch (InterruptedException e){
            logger.error("Failed to tar checkpoint",e);
            throw new RuntimeException(e);
        }

        // Clean it up
        try{
            String command = "rm -rf " + tmpDir.toFile().getAbsolutePath();
            logger.info(command);

            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getErrorStream()));
            StringBuffer output = new StringBuffer();
            String line;
            while((line = reader.readLine())!=null){
                output.append(line + "\n");
            }
            if(output.toString().length() != 0){
                logger.info(output.toString());
            }

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
}
