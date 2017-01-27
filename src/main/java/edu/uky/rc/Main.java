package edu.uky.rc;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        DockerContainer wiki = new DockerContainer("f31eecda1905");
        List<DockerVolume> volumes = wiki.getVolumes();
        for (DockerVolume v : volumes){
            System.out.println(v.getInternalPath());
            System.out.print(v.getExternalPath());
            System.out.println("\n");
        }
    }
}
