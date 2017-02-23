package edu.uky.rc;

import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerChange;

import java.util.HashMap;

/**
 * Created by nmccarty on 2/23/17.
 */
public class CannedContainer {
    private DockerContainer container;
    private HashMap<String,String> volumeMap;

    public CannedContainer(String id){
        container = new DockerContainer(id);
        volumeMap = new HashMap<>();
    }
}
