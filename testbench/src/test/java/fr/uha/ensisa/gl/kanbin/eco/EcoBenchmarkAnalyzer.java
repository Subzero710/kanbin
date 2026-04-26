package fr.uha.ensisa.gl.kanbin.eco;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EcoBenchmarkAnalyzer {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: mvn exec:java -Dexec.mainClass=\"fr.uha.ensisa.gl.kanbin.eco.EcoBenchmarkAnalyzer\" -Dexec.args=\"target/eco-benchmark-1.json target/eco-benchmark-2.json\"");
            System.exit(1);
        }

        List<JsonObject> results = new ArrayList<>();
        for (String filename : args) {
            String content = Files.readString(Paths.get(filename));
            results.add(JsonParser.parseString(content).getAsJsonObject());
            System.out.println("✓ Chargé: " + filename);
        }

        printResults(results);
        if (results.size() > 1) {
            compareResults(results);
        }
    }

    private static void printResults(List<JsonObject> results) {
        System.out.println("\n" + "═".repeat(90));
        System.out.println("  🌱 ANALYSE ECO-BENCHMARK - KANBIN PROJEST");
        System.out.println("═".repeat(90) + "\n");

        for (int i = 0; i < results.size(); i++) {
            JsonObject result = results.get(i);
            JsonObject global = result.getAsJsonObject("global_metrics");

            System.out.printf("📊 RÉSULTAT %d : %s%n", i + 1, result.get("timestamp").getAsString());
            System.out.println("──────────────────────────────────────────────────────────────────────────────");
            System.out.printf("  ⏱️  Durée totale             : %,d ms (%.2f s)%n",
                    global.get("total_duration_ms").getAsLong(),
                    global.get("total_duration_ms").getAsLong() / 1000.0);
            System.out.printf("  💾 Pic mémoire JVM          : %d MB%n", global.get("max_jvm_memory_mb").getAsLong());
            System.out.printf("  📈 Δ mémoire JVM totale     : %+d KB%n", global.get("total_jvm_memory_delta_kb").getAsLong());
            System.out.printf("  ⚙️  CPU total conteneur      : %.4f s%n", global.get("total_cpu_seconds").getAsDouble());
            System.out.printf("  🧠 Pic mémoire conteneur    : %.2f MB%n", global.get("max_container_memory_peak_mb").getAsDouble());
            System.out.printf("  🌐 Réseau RX total          : %.0f B%n", global.get("total_network_rx_bytes").getAsDouble());
            System.out.printf("  🌐 Réseau TX total          : %.0f B%n", global.get("total_network_tx_bytes").getAsDouble());
            System.out.printf("  🎯 Actions exécutées        : %d%n", global.get("actions_count").getAsInt());

            JsonArray actions = result.getAsJsonArray("actions");
            System.out.println("\n  📋 Action par action :");
            actions.asList().stream()
                    .map(e -> e.getAsJsonObject())
                    .sorted(Comparator.comparing(a -> a.get("name").getAsString()))
                    .forEach(action -> System.out.printf(
                            "    • %-28s : %5d ms | CPU %7.4f s | Mem avg %7.2f MB | Mem peak %7.2f MB | RX %10.0f B | TX %10.0f B%n",
                            action.get("name").getAsString(),
                            action.get("duration_ms").getAsLong(),
                            action.get("cpu_seconds").getAsDouble(),
                            action.get("container_memory_avg_mb").getAsDouble(),
                            action.get("container_memory_peak_mb").getAsDouble(),
                            action.get("network_rx_bytes").getAsDouble(),
                            action.get("network_tx_bytes").getAsDouble()
                    ));
            System.out.println();
        }
    }

    private static void compareResults(List<JsonObject> results) {
        JsonObject baseline = results.get(0);
        JsonArray baselineActions = baseline.getAsJsonArray("actions");

        System.out.println("\n" + "═".repeat(90));
        System.out.println("  📊 COMPARAISON ENTRE RÉSULTATS");
        System.out.println("═".repeat(90) + "\n");

        baselineActions.asList().stream()
                .map(e -> e.getAsJsonObject())
                .sorted(Comparator.comparing(a -> a.get("name").getAsString()))
                .forEach(baseAction -> {
                    String actionName = baseAction.get("name").getAsString();
                    long baseDuration = baseAction.get("duration_ms").getAsLong();
                    double baseCpu = baseAction.get("cpu_seconds").getAsDouble();
                    double baseMemPeak = baseAction.get("container_memory_peak_mb").getAsDouble();

                    System.out.printf("  %s%n", actionName);
                    for (int i = 0; i < results.size(); i++) {
                        JsonObject runAction = findAction(results.get(i), actionName);
                        long duration = runAction.get("duration_ms").getAsLong();
                        double cpu = runAction.get("cpu_seconds").getAsDouble();
                        double memPeak = runAction.get("container_memory_peak_mb").getAsDouble();

                        double durationVar = percentage(baseDuration, duration);
                        double cpuVar = percentage(baseCpu, cpu);
                        double memVar = percentage(baseMemPeak, memPeak);

                        System.out.printf(
                                "    Run %d: durée %,d ms (%+.1f%%) | CPU %.4f s (%+.1f%%) | pic mémoire %.2f MB (%+.1f%%)%n",
                                i + 1, duration, durationVar, cpu, cpuVar, memPeak, memVar
                        );
                    }
                });

        System.out.println("\n" + "═".repeat(90) + "\n");
    }

    private static JsonObject findAction(JsonObject result, String actionName) {
        return result.getAsJsonArray("actions")
                .asList()
                .stream()
                .map(e -> e.getAsJsonObject())
                .filter(a -> a.get("name").getAsString().equals(actionName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Action not found: " + actionName));
    }

    private static double percentage(double baseline, double value) {
        if (baseline == 0d) {
            return 0d;
        }
        return ((value - baseline) / baseline) * 100d;
    }
}