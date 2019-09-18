package com.dias.services.reports.utils;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Утилита, загружающая ресурсы подсистемы.
 */
@Component
public class SubsystemUtils {

    @Value("${subsystem.path}")
    public String SUBSYSTEM_PATH;

    public byte[] loadResource(String path) throws IOException {
        if (StringUtils.isEmpty(SUBSYSTEM_PATH)) {
            InputStream resourceAsStream = SubsystemUtils.class.getResourceAsStream(path);
            if (resourceAsStream != null) {
                return IOUtils.toByteArray(resourceAsStream);
            }
        } else {
            String pathname = SUBSYSTEM_PATH + path;
            if (new File(pathname).exists()) {
                // пытаемся загрузить по абсолютному пути
                return IOUtils.toByteArray(new FileInputStream(pathname));
            } else {
                // в противном случае доверяемся окончательно класслоудеру
                InputStream resourceAsStream = SubsystemUtils.class.getResourceAsStream(pathname);
                if (resourceAsStream != null) {
                    return IOUtils.toByteArray(resourceAsStream);
                }
            }
        }
        return null;
    }
}
