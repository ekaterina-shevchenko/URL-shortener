package de.tum.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;

public class HTTPServerUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private HTTPServerUtils(){}

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String s = bufferedReader.readLine();
        inputStream.close();
        return s;
    }

    public static void writeResponse(HttpExchange exchange, Integer code, String responseBody) throws IOException {
        exchange.sendResponseHeaders(code, responseBody.getBytes().length);
        OutputStream outputStream = exchange.getResponseBody();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        bufferedWriter.write(responseBody);
        bufferedWriter.flush();
        outputStream.close();
    }


    public static void writeResponse(HttpExchange exchange, Integer code, Object response) throws IOException {
        String json = objectMapper.writeValueAsString(response);
        writeResponse(exchange, code, json);
    }
}
