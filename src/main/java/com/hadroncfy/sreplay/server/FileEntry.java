package com.hadroncfy.sreplay.server;

import java.io.File;
import java.util.Date;
import java.util.Random;

public class FileEntry {
    private static final Random random = new Random();
    private final File file;
    private final String path;
    private Date expiresOn;

    private static String randomString(int len){
        final StringBuilder sb = new StringBuilder();
        while (len --> 0){
            int i = random.nextInt(16);
            if (i >= 10){
                sb.append((char)(i - 10 + 'a'));
            }
            else {
                sb.append((char)('0' + i));
            }
        }
        return sb.toString();
    }

    public FileEntry(File file, long last){
        this.file = file;
        path = "/" + randomString(32) + "/" + file.getName();
        expiresOn = new Date(new Date().getTime() + last);
    }

    public boolean isExpired(){
        return new Date().after(expiresOn);
    }

    public String getPath(){
        return path;
    }

    public void touch(long last){
        expiresOn = new Date(new Date().getTime() + last);
    }

    public File getFile(){
        return file;
    }
}