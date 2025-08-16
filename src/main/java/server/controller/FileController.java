package server.controller;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import server.service.FileSharer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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
        this.uploadDir = System.getProperty("java.io.tmpdir") + File.separator + "zipdrop-uploads"; // renamed for consistency
        this.executorService = Executors.newFixedThreadPool(10);

        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists()) {
            uploadDirFile.mkdirs();
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
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1); // standard empty response for preflight
                return;
            }
            String response = "NOT FOUND";
            exchange.sendResponseHeaders(404, response.getBytes().length);
            try (OutputStream ops = exchange.getResponseBody()) {
                ops.write(response.getBytes());
            }
        }
    }

    /**
     * UploadHandler: Handles multipart file uploads.
     * Modified to support multi-file upload via MultipartParser.parseAll().
     */
    private class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String response = "Only POST requests are supported.";
                exchange.sendResponseHeaders(405, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            // Validate Content-Type and extract boundary
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.contains("multipart/form-data")) {
                String response = "Invalid request. Content-Type must be multipart/form-data.";
                exchange.sendResponseHeaders(400, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
            String boundary = contentType.split("boundary=")[1];

            // Read request body
            byte[] requestData;
            try (InputStream is = exchange.getRequestBody()) {
                requestData = is.readAllBytes();
            }

            // Parse multipart body into multiple files
            MultipartParser parser = new MultipartParser(requestData, boundary);
            List<MultipartParser.ParseResult> results = parser.parseAll();

            if (results.isEmpty()) {
                String response = "File upload failed. No files found in request.";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            StringBuilder responseBuilder = new StringBuilder();
            for (MultipartParser.ParseResult result : results) {
                Path uploadDirPath = Paths.get(uploadDir);
                Files.createDirectories(uploadDirPath);

                String uniqueFilename = UUID.randomUUID() + "_" + result.fileName;
                Path outputPath = uploadDirPath.resolve(uniqueFilename);
                Files.write(outputPath, result.fileContent);

                responseBuilder.append("File uploaded successfully: ")
                        .append(uniqueFilename)
                        .append(" (").append(result.contentType).append(")\n");
            }

            byte[] responseBytes = responseBuilder.toString().getBytes();
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    /**
     * MultipartParser: Extracts multiple files from multipart/form-data requests.
     * Extended with parseAll() method for multi-file support.
     */
    static class MultipartParser {
        private final byte[] data;
        private final String boundary;

        public MultipartParser(byte[] data, String boundary) {
            this.data = data;
            this.boundary = boundary;
        }

        public List<ParseResult> parseAll() {
            List<ParseResult> results = new ArrayList<>();
            int pos = 0;
            byte[] boundaryBytes = ("\r\n--" + this.boundary).getBytes(StandardCharsets.ISO_8859_1);

            while (true) {
                int headerEndIndex = findSequence(data, "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1), pos);
                if (headerEndIndex == -1) break;

                String headers = new String(data, pos, headerEndIndex - pos, StandardCharsets.ISO_8859_1);
                int contentStartIndex = headerEndIndex + 4;

                int contentEndIndex = findSequence(data, boundaryBytes, contentStartIndex);
                if (contentEndIndex == -1) break;

                byte[] fileContent = Arrays.copyOfRange(data, contentStartIndex, contentEndIndex);

                String filename = null;
                String contentType = "application/octet-stream";

                for (String line : headers.split("\r\n")) {
                    if (line.contains("filename=")) {
                        int start = line.indexOf("filename=\"") + 10;
                        int end = line.indexOf("\"", start);
                        if (end > start) {
                            filename = line.substring(start, end);
                        }
                    }
                    if (line.startsWith("Content-Type:")) {
                        contentType = line.substring(13).trim();
                    }
                }

                if (filename != null) {
                    results.add(new ParseResult(filename, contentType, fileContent));
                }

                pos = contentEndIndex + boundaryBytes.length;
            }
            return results;
        }

        private static int findSequence(byte[] data, byte[] sequence, int start) {
            outer:
            for (int i = start; i <= data.length - sequence.length; i++) {
                for (int j = 0; j < sequence.length; j++) {
                    if (data[i + j] != sequence[j]) {
                        continue outer;
                    }
                }
                return i;
            }
            return -1;
        }

        public static class ParseResult {
            public final String fileName;
            public final String contentType;
            public final byte[] fileContent;

            public ParseResult(String fileName, String contentType, byte[] fileContent) {
                this.fileName = fileName;
                this.contentType = contentType;
                this.fileContent = fileContent;
            }
        }
    }

    private class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
              }
    }
}
