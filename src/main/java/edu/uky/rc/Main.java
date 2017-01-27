package edu.uky.rc;

import javax.print.Doc;

public class Main {

    public static void main(String[] args) {
        DockerContainer wiki = new DockerContainer("f31eecda1905");
        wiki.getVolumes();
    }
}
