package server.controller;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import server.service.FileSharer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.SimpleTimeZone;
import java.util.UUID;
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
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.uploadDir = System.getProperty("java.io.tmpdir") + File.separator + "zipdrop-uploads";
        this.executorService = Executors.newFixedThreadPool(10);

        File uploadDirfile = new File(uploadDir);
        if (!uploadDirfile.exists()) {
            uploadDirfile.mkdirs();
        }

        server.createContext("/upload", new UploadHandler());
        server.createContext("/download", new DownloadHandler());
        server.createContext("/", new CORSHandler());
        server.setExecutor(executorService);
    }

    public void start() {
        server.start();
        System.out.println(("API server started on port " + server.getAddress().getPort()));
    }

    public void stop() {
        server.stop(0);
        executorService.shutdown();
        System.out.println("Api Server stopped");
    }

    private class CORSHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access.Control.Allow.Origin", "*");
            headers.add("Access.Control.Allow.Methods", "GET, POST, OPTIONS");
            headers.add("Access.Control.Allow.Origin", "Content-Type.Authorization");

            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.sendResponseHeaders(286, -1);
                return;
            }
            String response = "NOT FOUND";
            exchange.sendResponseHeaders(404, response.getBytes().length);
            try (OutputStream ops = exchange.getResponseBody()) {
                ops.write(response.getBytes());
            } //catch not required because already dicleared throws exception, I am propagating the error
        }
    }

    private class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");

            // Allow only POST requests
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String response = "Method not allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try (OutputStream ops = exchange.getResponseBody()) {
                    ops.write(response.getBytes());
                }
                return;
            }

            Headers requestHeaders = exchange.getRequestHeaders();
            String contentType = requestHeaders.getFirst("Content-Type");

            // Ensure the request is multipart/form-data
            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                String response = "Bad Request: Content-Type must be multipart/form-data";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream ops = exchange.getResponseBody()) {
                    ops.write(response.getBytes());
                }
                return;
            }

            String response;
            try {
                // Extract boundary from Content-Type header
                String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                // Read full request body into memory
                IOUtils.copy(exchange.getRequestBody(), baos);
                byte[] requestData = baos.toByteArray();

                // Parse multipart form-data
                Multiparser parser = new Multiparser(requestData, boundary);
                Multiparser.ParseResult result = parser.parse();

                if (result == null) {
                    response = "Bad Request: Could not parse file content";
                    exchange.sendResponseHeaders(400, response.getBytes().length);
                    try (OutputStream ops = exchange.getResponseBody()) {
                        ops.write(response.getBytes());
                    }
                    return;
                }

                // Handle file name - fallback if missing
                String fileName = result.fileName;
                if (fileName == null || fileName.trim().isEmpty()) {
                    fileName = "unnamed.file";
                }

                // Generate unique file name
                String uniqueFileName = UUID.randomUUID() + "_" + new File(fileName).getName();
                String filePath = uploadDir + File.separator + uniqueFileName;

                // Save file to disk
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    fos.write(result.fileContent);
                }

                // Share file through FileSharer service
                int port = fileSharer.offerFile(filePath);
                new Thread(() -> fileSharer.startFileServer(port)).start();

                // Prepare JSON response
                String jsonResponse = "{\"port\": " + port + "}";
                headers.add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                try (OutputStream ops = exchange.getResponseBody()) {
                    ops.write(jsonResponse.getBytes());
                }

            } catch (Exception ex) {
                // Handle internal server error
                response = "Internal Server Error: " + ex.getMessage();
                System.err.println("Error processing file upload: " + ex.getMessage());
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream ops = exchange.getResponseBody()) {
                    ops.write(response.getBytes());
                }
            }
        }
    }

    // Multipart parser utility class
    private static class Multiparser {
        private final byte[] data;
        private final String boundary;

        public Multiparser(byte[] data, String boundary) {
            this.data = data;
            this.boundary = boundary;
        }

        public ParseResult parse() {
            try {
                String dataAsString = new String(data);
                String filenameMarker = "filename=\"";
                int filenameStart = dataAsString.indexOf(filenameMarker);
                if (filenameStart == -1) return null;

                filenameStart += filenameMarker.length(); // Move to actual filename start
                int filenameEnd = dataAsString.indexOf("\"", filenameStart);
                String fileName = dataAsString.substring(filenameStart, filenameEnd);

                String contentTypeMarker = "Content-Type: ";
                int contentTypeStart = dataAsString.indexOf(contentTypeMarker, filenameEnd);
                String contentType = "application/octet-stream";
                if (contentTypeStart != -1) {
                    contentTypeStart += contentTypeMarker.length();
                    int contentTypeEnd = dataAsString.indexOf("\r\n", contentTypeStart);
                    contentType = dataAsString.substring(contentTypeStart, contentTypeEnd);
                }

                String headerEndMarker = "\r\n\r\n";
                int headerEnd = dataAsString.indexOf(headerEndMarker);
                if (headerEnd == -1) return null;

                int contentStart = headerEnd + headerEndMarker.length();

                // Locate boundary end
                byte[] boundaryBytes = ("\r\n--" + boundary + "--").getBytes();
                int contentEnd = findSequence(data, boundaryBytes, contentStart);
                if (contentEnd == -1) {
                    boundaryBytes = ("\r\n--" + boundary).getBytes();
                    contentEnd = findSequence(data, boundaryBytes, contentStart);
                }
                if (contentEnd == -1 || contentEnd <= contentStart) return null;

                // Extract file bytes
                byte[] fileContent = new byte[contentEnd - contentStart];
                System.arraycopy(data, contentStart, fileContent, 0, fileContent.length);

                return new ParseResult(fileName, fileContent, contentType);

            } catch (Exception e) {
                System.out.println("Error parsing multipart data " + e.getMessage());
                return null;
            }
        }

        public static class ParseResult {
            public final String fileName;
            public final byte[] fileContent;
            public final String contentType;

            public ParseResult(String fileName, byte[] fileContent, String contentType) {
                this.fileName = fileName;
                this.fileContent = fileContent;
                this.contentType = contentType;
            }
        }

        // Utility method to find byte sequence in byte array
        private static int findSequence(byte[] data, byte[] sequence, int startPos) {
            outer:
            for (int i = startPos; i <= data.length - sequence.length; i++) {
                for (int j = 0; j < sequence.length; j++) {
                    if (data[i + j] != sequence[j]) {
                        continue outer;
                    }
                }
                return i;
            }
            return -1;
        }
    }





    private class DownloadHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange)throws IOException{

        }
    }


}
