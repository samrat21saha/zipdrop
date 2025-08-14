package server.controller;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import server.service.FileSharer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class FileController {

    private final FileSharer fileSharer;
    private final HttpServer server;
    private final String uploadDir;
    private final ExecutorService executorService;


    public FileController(int port) throws IOException {
        this.fileSharer = new FileSharer();
        this.server = HttpServer.create(new InetSocketAddress(port),0);
        this.uploadDir = System.getProperty("java.io.tmpdir") + File.separator +"zipdrop-uploads";
        this.executorService = Executors.newFixedThreadPool(10);

        File uploadDirfile = new File(uploadDir);
        if(!uploadDirfile.exists()){
            uploadDirfile.mkdirs();
        }

        server.createContext("/upload", new UploadHandler());
        server.createContext("/download", new DownloadHandler());
        server.createContext("/", new CORSHandler());
        server.setExecutor(executorService);
    }

    public void start(){
        server.start();
        System.out.println(("API server started on port "+ server.getAddress().getPort()));
    }

    public void stop(){
        server.stop(0);
        executorService.shutdown();
        System.out.println("Api Server stopped");
    }

    private class CORSHandler implements HttpHandler{
        public void handle(HttpExchange exchange) throws IOException{
            Headers headers = exchange.getResponseHeaders();            headers.add("Access.Control.Allow.Origin", "*");
            headers.add("Access.Control.Allow.Methods", "GET, POST, OPTIONS");
            headers.add("Access.Control.Allow.Origin", "Content-Type.Authorization");

            if(exchange.getRequestMethod().equals("OPTIONS")){
                exchange.sendResponseHeaders(286, -1);
                return;
            }
            String response = "NOT FOUND";
            exchange.sendResponseHeaders(404, response.getBytes().length);
            try(OutputStream ops = exchange.getResponseBody()){
                ops.write(response.getBytes());
            } //catch not required because already dicleared throws exception, I am propagating the error
        }
    }

    private class UploadHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange)throws IOException{
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            if(!exchange.getRequestMethod().equalsIgnoreCase("POST")){
                String response = "Method not allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try(OutputStream ops = exchange.getResponseBody()){
                    ops.write(response.getBytes());
                }
                return;
            }

            Headers requestHeaders = exchange.getRequestHeaders();
            String contentType = requestHeaders.getFirst("Content-Type");

            if(contentType == null || !contentType.startsWith("multipart/form-data")){
                String response = "Bad Request: Content-Type must be multipart/form-data";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try(OutputStream ops = exchange.getResponseBody()){
                    ops.write(response.getBytes());
                }return;
            }
            try{

                String boundary = contentType.substring(contentType.indexOf("boundary") + 9);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                IOUtils.copy(exchange.getRequestBody(), baos);
                byte[] requestDate = baos.toByteArray();

                Multiparser parser = new Multiparser(requestDate, boundary);

            }catch (Exception ex){

            }


        }
    }

    private static class Multiparser {
        private final byte[] data;
        private final String boundary;

        public Multiparser(byte[] data, String boundary){
            this.data = data;
            this.boundary = boundary;
        }
        public ParseResult parse() { //for parsing raw data
            try { //raw data is encoded
                //try to extend this part and raise a pull request create video encoding version we have to use generic objects (to be done)
                String dataAsString = new String(data); //json txt pdf csv all can be encoded but video can't
                String filenameMarker = "filename=\""; //file name will contain "" so extract the string between ""
                int filenameStart = dataAsString.indexOf(filenameMarker);
                if (filenameStart == -1) {
                    return null;
                }
                int filenameEnd = dataAsString.indexOf("\"", filenameStart);
                String fileName = dataAsString.substring(filenameStart, filenameEnd);

                String contentTypeMarker = "Content-Type: ";
                int contentTypeStart = dataAsString.indexOf(contentTypeMarker, filenameEnd);
                String contentType = "application/octet-stream";
                if (contentTypeStart != -1) {
                    contentTypeStart += contentTypeMarker.length();
                    int contentTypeEnd = dataAsString.indexOf("\r\n", contentTypeStart);
                    contentType = dataAsString.substring(contentTypeEnd, contentTypeEnd);
                }

                String headerEndMarker = "\r\n\r\n";
                int headerEnd = dataAsString.indexOf(headerEndMarker);
                if (headerEnd == -1) {
                    return null;
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    public static class ParseResult {
        public final String fileName;
        public final byte[] fileContent;
        public ParseResult(String fileName, byte[] fileContent){
            this.fileName=fileName;
            this.fileContent = fileContent;
        }
    }




    private class DownloadHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange)throws IOException{

        }
    }


}
