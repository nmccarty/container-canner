package edu.uky.rc;

import com.spotify.docker.client.messages.Container;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        DockerContainer wiki = new DockerContainer("my_wiki");
        CannedContainer cannedWiki = new CannedContainer(wiki.getContainerID());

        try {
            System.out.println(wiki.checkpoint());
            System.out.println(cannedWiki.can().getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
