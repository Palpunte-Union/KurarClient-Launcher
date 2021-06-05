package com.github.eighty88.kurar.launcher;

import com.github.eighty88.kurar.launcher.resources.Manager;
import com.github.eighty88.kurar.launcher.resources.type.Library;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        new Main(args);
    }

    public Main(String[] args) {
        if(!Utils.getKurarDirectory().exists()) {
            if(Utils.getKurarDirectory().mkdir()) {
                try {
                    new Utils().downloadJson();
                    new Utils().downloadVersionJson();
                    new Downloader("https://github.com/Palpunte-Union/KurarClient-Binary/releases/download/" + Utils.readURL() + "/KurarClient.jar").DownloadTo(Utils.getKurarDirectory().getPath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                throw new NullPointerException();
            }
        } else {
            if(Utils.checkUpdate()) {
                try {
                    new Utils().downloadJson();
                    new Utils().downloadVersionJson();
                    if(new File(Utils.getKurarDirectory(), "KurarClient.jar").delete())
                        new Downloader("https://github.com/Palpunte-Union/KurarClient-Binary/releases/download/" + Utils.readURL() + "/KurarClient.jar").DownloadTo(Utils.getKurarDirectory().getPath());
                    if(!new File(Utils.getKurarDirectory(), "KurarClient.jar").exists())
                        new Downloader("https://github.com/Palpunte-Union/KurarClient-Binary/releases/download/" + Utils.readURL() + "/KurarClient.jar").DownloadTo(Utils.getKurarDirectory().getPath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        launch(args);
    }

    public void launch(String[] args) {
        System.out.println("Starting...");
        String temp;
        Manager manager = new Manager(new File(Utils.getKurarDirectory(), "KurarClient.json").getAbsolutePath());
        System.out.print("Download Assets and Libraries...");
        try {
            manager.download();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Success!");
        switch (Utils.getPlatformName()) {
            case "windows":
                temp = "\"C:" + File.separator + "Program Files (x86)" + File.separator + "Minecraft Launcher" + File.separator + "runtime" + File.separator + "jre-x64" + File.separator + "bin" + File.separator + "javaw.exe\"";
                break;
            case "osx":
                temp = "Library/Application Support/minecraft/jre-x64/jre.bundle/Contents/Home/bin/java";
                break;
            default:
                temp = "java";
        }
        String cpseparator = Utils.getPlatformName() == "windows" ? ";" : ":";
        StringBuilder classPathBuilder = new StringBuilder();
        classPathBuilder.append(new File(Utils.getKurarDirectory(), "KurarClient.jar").getAbsolutePath());
        for (Library lib : manager.getLibraries()) {
            classPathBuilder.append(cpseparator);
            classPathBuilder.append(lib.getFilePath().getAbsolutePath());
        }

        String finalTemp = temp;
        System.out.println("Starting Thread...");
        new Thread(() -> {
            String[] launchArgs = args;
            List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            ArrayList<String> jvmArgs = new ArrayList<>();
            jvmArgs.add(finalTemp);
            if (Utils.getPlatformName() == "osx")
                jvmArgs.add("-XstartOnFirstThread");
            if (launchArgs == null || launchArgs.length == 0) {
                String assets = (new File(Utils.getWorkingDirectory(), "assets")).getAbsolutePath();
                launchArgs = new String[]{"--version", "mcp", "--accessToken", "0", "--assetsDir", assets, "--assetIndex", Utils.getAssetsVersion(), "--userProperties", "{}"};
            } else {
                jvmArgs.addAll(inputArguments);
            }
            jvmArgs.add("-cp");
            jvmArgs.add(classPathBuilder.toString());
            jvmArgs.add("net.minecraft.client.main.Main");
            jvmArgs.addAll(Arrays.asList(launchArgs));
            StringBuilder sb = new StringBuilder("Launching game");
            for (String arg : jvmArgs)
                sb.append(" ").append(arg);
            System.out.println(sb);
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(jvmArgs);
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();
                InputStream is = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null)
                    System.out.println(line);
                process.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.exit(0);
        }).start();
    }
}
