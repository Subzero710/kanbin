package fr.uha.ensisa.gl.kanbin.eco;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

class PrometheusMetricsClient {

    private static final String DEFAULT_PROMETHEUS_BASE_URL = "http://localhost:9090";
    private static final List<String> CONTAINER_SELECTORS = List.of(
            "name=~\".*kanbin-app-eco.*\"",
            "container_label_com_docker_compose_service=\"kanbin-app\"",
            "container=\"kanbin-app-eco\""
    );

    private final HttpClient httpClient;
    private final String baseUrl;

    PrometheusMetricsClient() {
        this(System.getenv().getOrDefault("PROMETHEUS_BASE_URL", DEFAULT_PROMETHEUS_BASE_URL));
    }

    PrometheusMetricsClient(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.httpClient = HttpClient.newHttpClient();
    }

    ActionContainerMetrics collectForWindow(Instant startedAt, Instant endedAt) {
        long windowSeconds = Math.max(1, endedAt.getEpochSecond() - startedAt.getEpochSecond());

        double cpuSeconds = firstSuccessfulQuery("sum(increase(container_cpu_usage_seconds_total{%s}[" + windowSeconds + "s]))");
        double memoryAvgBytes = firstSuccessfulQuery("avg_over_time(container_memory_usage_bytes{%s}[" + windowSeconds + "s])");
        double memoryPeakBytes = firstSuccessfulQuery("max_over_time(container_memory_usage_bytes{%s}[" + windowSeconds + "s])");
        double networkRxBytes = firstSuccessfulQuery("sum(increase(container_network_receive_bytes_total{%s}[" + windowSeconds + "s]))");
        double networkTxBytes = firstSuccessfulQuery("sum(increase(container_network_transmit_bytes_total{%s}[" + windowSeconds + "s]))");

        return new ActionContainerMetrics(
                cpuSeconds,
                bytesToMegabytes(memoryAvgBytes),
                bytesToMegabytes(memoryPeakBytes),
                networkRxBytes,
                networkTxBytes
        );
    }

    private double firstSuccessfulQuery(String template) {
        for (String selector : CONTAINER_SELECTORS) {
            Optional<Double> value = executeInstantQuery(template.formatted(selector));
            if (value.isPresent()) {
                return value.get();
            }
        }
        return -1d;
    }

    private Optional<Double> executeInstantQuery(String promQl) {
        try {
            String encoded = URLEncoder.encode(promQl, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/query?query=" + encoded))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!"success".equals(root.get("status").getAsString())) {
                return Optional.empty();
            }

            JsonArray results = root.getAsJsonObject("data").getAsJsonArray("result");
            if (results.isEmpty()) {
                return Optional.empty();
            }

            JsonArray value = results.get(0).getAsJsonObject().getAsJsonArray("value");
            if (value.size() < 2) {
                return Optional.empty();
            }

            return Optional.of(Double.parseDouble(value.get(1).getAsString()));
        } catch (IOException | InterruptedException | RuntimeException ex) {
            return Optional.empty();
        }
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