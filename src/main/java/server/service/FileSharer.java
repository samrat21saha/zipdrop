package server.service;

import server.utils.UploadUtils;

import java.util.HashMap;

public class FileSharer {
    private HashMap<Integer, String> availableFiles;

    public FileSharer(){
        availableFiles = new HashMap<>();
    }
    public int offerFile(String filePath){
        int port;
        while(true){ // we will move untill we find an available/unreserved port
            port = UploadUtils.generateCode();
            if(!availableFiles.containsKey(port)){
                availableFiles.put(port, filePath);
            }
        }
    }
}
