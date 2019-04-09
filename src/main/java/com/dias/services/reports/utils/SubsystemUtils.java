package com.dias.services.reports.utils;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Утилита, загружающая ресурсы подсистемы.
 */
@Component
public class SubsystemUtils {

    @Value("${subsystem.path}")
    public String SUBSYSTEM_PATH;

    public byte[] loadResource(String path) throws IOException {
        if (StringUtils.isEmpty(SUBSYSTEM_PATH)) {
            return IOUtils.toByteArray(SubsystemUtils.class.getResourceAsStream(path));
        } else {
            if (new File(SUBSYSTEM_PATH + path).exists()) {
                // пытаемся загрузить по абсолютному пути
                return IOUtils.toByteArray(new FileInputStream(SUBSYSTEM_PATH + path));
            } else {
                // в противном случае доверяемся окончательно класслоудеру
                return IOUtils.toByteArray(SubsystemUtils.class.getResourceAsStream(SUBSYSTEM_PATH + path));
            }
        }
    }
}
