package server.service;

import server.utils.UploadUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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
                return port;
            }
        }
    }
    public void startFileServer(int port){ // when someone wants to download file
        String filePath = availableFiles.get(port);
        if(filePath == null){
            System.out.println("No File found on port:" + port);
            return;
        }try (ServerSocket serverSocket = new ServerSocket(port)){ //when file found filesharer opens a new server socket not the client
            System.out.println("Geting file "+ new File(filePath).getName() + "on port: "+port);
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connection: "+clientSocket.getInetAddress());
            new Thread(new FileSenderHandler(clientSocket, filePath)).start(); //whenever new client requests a file new socket created and new thread in created and start the tread
        } catch (IOException ex) {
            System.err.println("Error handling file server on port: "+ port);
        }
    }
    private static class FileSenderHandler implements Runnable{


        private final Socket clientSocket;
        private final String filePath;

        public FileSenderHandler(Socket clientSocket, String filePath){
            this.clientSocket = clientSocket;
            this.filePath = filePath;
        }
        @Override
        public void run(){
            try(FileInputStream fis = new FileInputStream(filePath)){
                OutputStream ops = clientSocket.getOutputStream(); //client socket is open someone wants to download a file so I have to send something
                String fileName = new File(filePath).getName();
                String header = "Filename: "+fileName+"\n";
                ops.write(header.getBytes());//send header as output stream

                byte[] buffer =new byte[4896]; //max byte anyone can send is 4896 it is a standard value
                int byteRead; // Reads a byte of data from this input stream. This method blocks if no input is yet available.
                while((byteRead = fis.read(buffer)) !=-1){ //The next byte of data, or -1 if the end of the file is reached.
                    ops.write(buffer, 0, byteRead);
                }
                System.out.println("File "+ fileName + "sent to "+ clientSocket.getInetAddress());

            } catch (Exception ex) {
                System.err.println("Error sending file to the client "+ ex.getMessage());
            }finally {
                try {
                    clientSocket.close();
                } catch (Exception e) {
                    System.out.println("Error closing socket: "+e.getMessage());
                }
            }
        }
    }
}
