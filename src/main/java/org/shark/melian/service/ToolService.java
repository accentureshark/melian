package org.shark.melian.service;

import java.util.List;
import java.util.Map;

public interface ToolService {
    List<String> listTools();

    Map<String, Object> capabilities();

    Object callTool(String name, Map<String, Object> arguments);
}