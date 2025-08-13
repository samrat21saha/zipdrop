package server.controller;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import server.service.FileSharer;

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

        }
    }
    private class DownloadHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange)throws IOException{

        }
    }


}
