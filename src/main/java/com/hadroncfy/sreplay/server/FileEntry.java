package com.hadroncfy.sreplay.server;

import java.io.File;
import java.util.Date;

public class FileEntry {
    private final File file;
    private final Date expiresOn;
    public FileEntry(File file, long last){
        this.file = file;
        expiresOn = new Date(new Date().getTime() + last);
    }

    public boolean isExpired(){
        return new Date().after(expiresOn);
    }

    public File getFile(){
        return file;
    }
}