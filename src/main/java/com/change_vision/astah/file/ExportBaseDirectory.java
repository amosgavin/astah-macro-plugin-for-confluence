package com.change_vision.astah.file;

import java.io.File;

import com.atlassian.confluence.setup.BootstrapManager;

public class ExportBaseDirectory {
    
    private final File exportBase;
    
    public ExportBaseDirectory(BootstrapManager bootstrapManager){
        exportBase = new File("/Users/macos/Documents/YY/workspace/java/astah-macro-plugin-for-confluence/target/test-classes/com/change_vision/astah/exporter/", "astah-exported");
    }
    
    public File getDirectory(){
        return exportBase;
    }

}
