package fr.uha.ensisa.gl.kanbin.eco;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class PrometheusMetricsClient {

    private static final String DEFAULT_CONTAINER_NAME = "kanbin-app-eco";

    private final HttpClient httpClient;
    private final String containerName;
    private final String containerId;
    private volatile List<String> cachedSelectors;

    PrometheusMetricsClient() {
        this(System.getenv().getOrDefault("BENCHMARK_CONTAINER_NAME", DEFAULT_CONTAINER_NAME));
    }

    PrometheusMetricsClient(String containerName) {
        this.containerName = containerName;
        this.httpClient = HttpClient.newHttpClient();
        this.containerId = resolveContainerId(containerName);
    }

    ActionContainerMetrics collectForWindow(Instant startedAt, Instant endedAt) {
        long actionSeconds = Math.max(1, endedAt.getEpochSecond() - startedAt.getEpochSecond());
        long windowSeconds = Math.max(5, actionSeconds + 2);

        double cpuSeconds = firstSuccessfulQuery(
                "sum(increase(container_cpu_usage_seconds_total{%s}[" + windowSeconds + "s]))"
        );
        double memoryAvgBytes = firstSuccessfulQuery(
                "avg_over_time(container_memory_usage_bytes{%s}[" + windowSeconds + "s])"
        );
        double memoryPeakBytes = firstSuccessfulQuery(
                "max_over_time(container_memory_usage_bytes{%s}[" + windowSeconds + "s])"
        );
        double networkRxBytes = firstSuccessfulQuery(
                "sum(increase(container_network_receive_bytes_total{%s}[" + windowSeconds + "s]))"
        );
        double networkTxBytes = firstSuccessfulQuery(
                "sum(increase(container_network_transmit_bytes_total{%s}[" + windowSeconds + "s]))"
        );

        return new ActionContainerMetrics(
                cpuSeconds,
                bytesToMegabytes(memoryAvgBytes),
                bytesToMegabytes(memoryPeakBytes),
                networkRxBytes,
                networkTxBytes
        );
    }

    private double firstSuccessfulQuery(String template) {
        for (String selector : selectors()) {
            Optional<Double> value = executeInstantQuery(String.format(template, selector));
            if (value.isPresent()) {
                return value.get();
            }
        }
        return -1d;
    }

    private List<String> selectors() {
        if (cachedSelectors != null) {
            return cachedSelectors;
        }

        synchronized (this) {
            if (cachedSelectors != null) {
                return cachedSelectors;
            }

            Set<String> selectors = new LinkedHashSet<>();
            discoverSelectorsFromSeries("container_cpu_usage_seconds_total").ifPresent(selectors::add);
            discoverSelectorsFromSeries("container_memory_usage_bytes").ifPresent(selectors::add);
            discoverSelectorsFromSeries("container_network_receive_bytes_total").ifPresent(selectors::add);
            discoverSelectorsFromSeries("container_network_transmit_bytes_total").ifPresent(selectors::add);

            String shortId = containerId != null && containerId.length() > 12 ? containerId.substring(0, 12) : containerId;
            if (shortId != null && !shortId.isBlank()) {
                selectors.add("id=~\".*" + shortId + ".*\"");
            }
            if (containerId != null && !containerId.isBlank()) {
                selectors.add("id=~\".*" + containerId + ".*\"");
            }

            selectors.add("name=~\".*" + escapeForPromQl(containerName) + ".*\"");
            selectors.add("container=~\".*" + escapeForPromQl(containerName) + ".*\"");
            selectors.add("container_label_com_docker_compose_service=\"kanbin-app\"");
            selectors.add("container_label_com_docker_compose_container_number=\"1\"");

            cachedSelectors = new ArrayList<>(selectors);
            return cachedSelectors;
        }
    }

    private Optional<String> discoverSelectorsFromSeries(String metricName) {
        try {
            String encoded = URLEncoder.encode(metricName, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(prometheusBaseUrl() + "/api/v1/series?match[]=" + encoded))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!root.has("status") || !"success".equals(root.get("status").getAsString())) {
                return Optional.empty();
            }

            JsonArray data = root.getAsJsonArray("data");
            if (data == null) {
                return Optional.empty();
            }

            String shortId = containerId != null && containerId.length() > 12 ? containerId.substring(0, 12) : containerId;

            for (JsonElement elem : data) {
                JsonObject labels = elem.getAsJsonObject();

                if (matches(labels, "__name__", metricName)) {
                    if (matchAny(labels, shortId, containerId, containerName, "kanbin-app")) {
                        Optional<String> selector = buildSelectorFromLabels(labels);
                        if (selector.isPresent()) {
                            return selector;
                        }
                    }
                }
            }
        } catch (IOException | InterruptedException | RuntimeException ignored) {
        }
        return Optional.empty();
    }

    private Optional<String> buildSelectorFromLabels(JsonObject labels) {
        String[] preferredKeys = new String[] {
                "id",
                "name",
                "container",
                "container_label_com_docker_compose_service"
        };

        for (String key : preferredKeys) {
            if (labels.has(key)) {
                String value = labels.get(key).getAsString();
                if (value != null && !value.isBlank()) {
                    if ("id".equals(key)) {
                        return Optional.of(key + "=~\".*" + escapeForPromQl(value) + ".*\"");
                    }
                    if ("name".equals(key) || "container".equals(key)) {
                        return Optional.of(key + "=~\".*" + escapeForPromQl(value) + ".*\"");
                    }
                    return Optional.of(key + "=\"" + escapeForPromQl(value) + "\"");
                }
            }
        }
        return Optional.empty();
    }

    private boolean matchAny(JsonObject labels, String... needles) {
        for (String needle : needles) {
            if (needle == null || needle.isBlank()) {
                continue;
            }
            for (String key : labels.keySet()) {
                String value = labels.get(key).getAsString();
                if (value != null && value.contains(needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matches(JsonObject labels, String key, String value) {
        return labels.has(key) && value.equals(labels.get(key).getAsString());
    }

    private Optional<Double> executeInstantQuery(String promQl) {
        try {
            String encoded = URLEncoder.encode(promQl, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(prometheusBaseUrl() + "/api/v1/query?query=" + encoded))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!root.has("status") || !"success".equals(root.get("status").getAsString())) {
                return Optional.empty();
            }

            JsonObject data = root.getAsJsonObject("data");
            if (data == null || !data.has("result")) {
                return Optional.empty();
            }

            JsonArray results = data.getAsJsonArray("result");
            if (results == null || results.isEmpty()) {
                return Optional.empty();
            }

            JsonArray value = results.get(0).getAsJsonObject().getAsJsonArray("value");
            if (value == null || value.size() < 2) {
                return Optional.empty();
            }

            return Optional.of(Double.parseDouble(value.get(1).getAsString()));
        } catch (IOException | InterruptedException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    private String prometheusBaseUrl() {
        String fromEnv = System.getenv("PROMETHEUS_BASE_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.replaceAll("/+$", "");
        }

        String containerName = resolveEcoExtensionContainerName("metrologie-prometheus");
        if (containerName != null) {
            String port = resolvePublishedPort(containerName, "9090/tcp");
            if (port != null) {
                return "http://127.0.0.1:" + port;
            }
        }

        return "http://localhost:9090";
    }

    private String resolveEcoExtensionContainerName(String prefix) {
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "ps", "--format", "{{.Names}}"
        );
        pb.redirectErrorStream(true);

        try {
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                String lastMatch = null;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(prefix)) {
                        lastMatch = line.trim();
                    }
                }
                int exit = p.waitFor();
                if (exit == 0) {
                    return lastMatch;
                }
            }
        } catch (IOException | InterruptedException ignored) {
        }
        return null;
    }

    private String resolvePublishedPort(String dockerContainerName, String containerPort) {
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "port", dockerContainerName, containerPort
        );
        pb.redirectErrorStream(true);

        try {
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = reader.readLine();
                int exit = p.waitFor();
                if (exit == 0 && line != null && !line.isBlank()) {
                    String[] parts = line.trim().split(":");
                    return parts[parts.length - 1];
                }
            }
        } catch (IOException | InterruptedException ignored) {
        }
        return null;
    }

    private String resolveContainerId(String containerName) {
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "inspect", "-f", "{{.Id}}", containerName
        );
        pb.redirectErrorStream(true);

        try {
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = reader.readLine();
                int exit = p.waitFor();
                if (exit == 0 && line != null && !line.isBlank()) {
                    return line.trim();
                }
            }
        } catch (IOException | InterruptedException ignored) {
        }
        return null;
    }

    private String escapeForPromQl(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private double bytesToMegabytes(double bytes) {
        return bytes < 0 ? -1d : bytes / (1024d * 1024d);
    }

    static final class ActionContainerMetrics {
        final double cpuSeconds;
        final double memoryAvgMb;
        final double memoryPeakMb;
        final double networkRxBytes;
        final double networkTxBytes;

        ActionContainerMetrics(double cpuSeconds,
                               double memoryAvgMb,
                               double memoryPeakMb,
                               double networkRxBytes,
                               double networkTxBytes) {
            this.cpuSeconds = cpuSeconds;
            this.memoryAvgMb = memoryAvgMb;
            this.memoryPeakMb = memoryPeakMb;
            this.networkRxBytes = networkRxBytes;
            this.networkTxBytes = networkTxBytes;
        }
    }
}