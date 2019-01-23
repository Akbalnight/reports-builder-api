package com.dias.services.reports;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Util {

    public static String readResource(final String resourcePath) {
        try {
            URL url = Util.class.getClassLoader().getResource(resourcePath);
            return new String(Files.readAllBytes(Paths.get(url.toURI())), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            return "";
        }
    }

    public static <T> T readObjectFromJSON(String resource, Class<T> clazz){
        try {
            return (T)new ObjectMapper().readValue(readResource(resource), clazz);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
