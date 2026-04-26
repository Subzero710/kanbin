package fr.uha.ensisa.gl.kanbin.eco;

import com.google.gson.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Analyse et compare plusieurs résultats de benchmark JSON
 * Usage: mvn exec:java -Dexec.mainClass="fr.uha.ensisa.gl.kanbin.eco.EcoBenchmarkAnalyzer" -Dexec.args="file1.json file2.json ..."
 */
public class EcoBenchmarkAnalyzer {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("❌ Utilisation: java EcoBenchmarkAnalyzer <fichier.json> [fichier2.json ...]");
            System.exit(1);
        }

        List<JsonObject> results = new ArrayList<>();
        for (String filename : args) {
            try {
                String content = Files.readString(Paths.get(filename));
                results.add(JsonParser.parseString(content).getAsJsonObject());
                System.out.println("✓ Chargé: " + filename);
            } catch (IOException e) {
                System.err.println("⚠️ Erreur lecture: " + filename + " - " + e.getMessage());
            }
        }

        if (results.isEmpty()) {
            System.err.println("❌ Aucun fichier valide trouvé.");
            System.exit(1);
        }

        analyzeResults(results);
    }

    private static void analyzeResults(List<JsonObject> results) {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("  🌱 ANALYSE ECO-BENCHMARK - KANBIN PROJEST");
        System.out.println("═".repeat(70) + "\n");

        for (int i = 0; i < results.size(); i++) {
            JsonObject result = results.get(i);
            System.out.printf("📊 RÉSULTAT %d : %s\n", i + 1, result.get("timestamp").getAsString());
            System.out.println("───────────────────────────────────────────────────────────────");

            JsonObject global = result.getAsJsonObject("global_metrics");
            long totalDuration = global.get("total_duration_ms").getAsLong();
            long maxMemory = global.get("max_memory_mb").getAsLong();
            long memoryDelta = global.get("total_memory_delta_kb").getAsLong();

            System.out.printf("  ⏱️  Durée totale      : %,d ms (%.2f s)\n", totalDuration, totalDuration / 1000.0);
            System.out.printf("  💾 Pic mémoire       : %d MB\n", maxMemory);
            System.out.printf("  📈 Δ mémoire total   : %+d KB\n", memoryDelta);
            System.out.printf("  🎯 Actions exécutées : %d\n", global.get("actions_count").getAsInt());

            // Détails par action
            JsonArray actions = result.getAsJsonArray("actions");
            System.out.println("\n  📋 Action par action :");
            actions.forEach(elem -> {
                JsonObject action = elem.getAsJsonObject();
                long duration = action.get("duration_ms").getAsLong();
                long memDelta = action.get("memory_delta_kb").getAsLong();
                long totalMem = action.get("total_memory_mb").getAsLong();

                System.out.printf("    • %-30s : %5d ms, Δ %+7d KB, Total: %4d MB\n",
                        action.get("name").getAsString(),
                        duration,
                        memDelta,
                        totalMem);
            });

            System.out.println();
        }

        // Comparaison si plusieurs résultats
        if (results.size() > 1) {
            compareResults(results);
        }
    }

    private static void compareResults(List<JsonObject> results) {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("  📊 COMPARAISON ENTRE RÉSULTATS");
        System.out.println("═".repeat(70) + "\n");

        // Récupérer les actions du premier résultat
        JsonArray firstActions = results.get(0).getAsJsonArray("actions");

        firstActions.forEach(elem -> {
            JsonObject firstAction = elem.getAsJsonObject();
            String actionName = firstAction.get("name").getAsString();
            long firstDuration = firstAction.get("duration_ms").getAsLong();

            System.out.printf("  %s :\n", actionName);

            for (int i = 0; i < results.size(); i++) {
                long duration = StreamSupport.stream(results.get(i).getAsJsonArray("actions").spliterator(), false)
                        .map(JsonElement::getAsJsonObject)
                        .filter(a -> a.get("name").getAsString().equals(actionName))
                        .findFirst()
                        .map(a -> a.get("duration_ms").getAsLong())
                        .orElse(0L);

                double variance = firstDuration != 0 ? ((duration - firstDuration) / (double) firstDuration) * 100 : 0;
                String indicator = variance > 10 ? "⚠️ " : (variance < -10 ? "✅" : "  ");
                System.out.printf("    %s Run %d: %,d ms (%+.1f%%)\n",
                        indicator, i + 1, duration, variance);
            }
        });

        System.out.println("\n" + "═".repeat(70) + "\n");
    }
}