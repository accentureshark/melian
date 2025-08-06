package org.shark.melian.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for MELIAN MCP Server using Spring Boot best practices.
 */
@Component
@ConfigurationProperties(prefix = "melian")
public class MelianProperties {

    private final Tmdb tmdb = new Tmdb();
    private final Mcp mcp = new Mcp();

    public Tmdb getTmdb() {
        return tmdb;
    }

    public Mcp getMcp() {
        return mcp;
    }

    public static class Tmdb {
        private String apiUrl = "https://api.themoviedb.org/3";
        private String accessToken;

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }

    public static class Mcp {
        private final Server server = new Server();

        public Server getServer() {
            return server;
        }

        public static class Server {
            private int port = 3000;
            private String host = "localhost";
            private final Http http = new Http();

            public int getPort() {
                return port;
            }

            public void setPort(int port) {
                this.port = port;
            }

            public String getHost() {
                return host;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public Http getHttp() {
                return http;
            }

            public static class Http {
                private boolean enabled = true;

                public boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }
            }
        }
    }
}