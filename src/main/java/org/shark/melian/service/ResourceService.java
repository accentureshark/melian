package org.shark.melian.service;

import java.util.List;
import java.util.Map;

public interface ResourceService {
    List<String> listResources();

    Map<String, Object> capabilities();

    Object readResource(String uri);
}