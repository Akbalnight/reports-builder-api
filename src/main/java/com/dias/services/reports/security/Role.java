package com.dias.services.reports.security;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class Role
{
    @Getter
    @Setter
    public static class RoleJsonObject
    {
        private Map<String, String> objects;
    }

    private String name;
    private RoleJsonObject jsonData;
}
