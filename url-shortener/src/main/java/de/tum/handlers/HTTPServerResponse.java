package de.tum.handlers;

import lombok.Data;

@Data
public class HTTPServerResponse {
    private int code = 200;
    private String body = "";
}
