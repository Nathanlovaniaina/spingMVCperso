package com.monframework.core.util.Mapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple Model holder similar to Spring's Model for passing attributes from controller to view.
 */
public class Model {
    private final Map<String, Object> attributes = new HashMap<>();

    public Model addAttribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    public Model addAllAttributes(Map<String, Object> map) {
        if (map != null) {
            attributes.putAll(map);
        }
        return this;
    }

    public Object get(String name) {
        return attributes.get(name);
    }

    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(attributes);
    }
}
