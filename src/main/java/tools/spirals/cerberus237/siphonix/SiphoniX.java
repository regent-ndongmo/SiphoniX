package tools.spirals.cerberus237.siphonix;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SiphoniX {

    private static final String TARGET_SERVICE_URL = System.getenv().getOrDefault("TARGET_URL", "http://recommender:8080/tools.descartes.teastore.recommender/rest");
    private static final int POLL_INTERVAL_SECONDS = 5;
    
    // Reusable, lightweight HTTP Client
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public static void main(String[] args) {
        System.out.println("[AUTONAMIC MANAGER] Starting Autonomic Manager Sidecar...");
        System.out.println("[AUTONAMIC MANAGER] Monitoring Target: " + TARGET_SERVICE_URL);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        
        // Execute the MAPE-K loop at fixed intervals
        executor.scheduleAtFixedRate(SiphoniX::mapeLoop, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private static void mapeLoop() {
        try {
            // 1. MONITOR: Collect metrics via REST
            String metrics = monitor();
            
            // 2. ANALYZE: Check if adaptation is needed based on metrics
            boolean adaptationNeeded = analyze(metrics);
            
            if (adaptationNeeded) {
                // 3. PLAN: Decide what to do
                String plan = planAdaptation();
                
                // 4. EXECUTE: Apply the action via REST or CLI
                execute(plan);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] MAPE loop encountered an issue: " + e.getMessage());
        }
    }

    private static String monitor() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TARGET_SERVICE_URL + "/metrics/status"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            System.out.println("[MONITOR] Successfully fetched metrics: " + response.body());
            return response.body();
        } else {
            System.err.println("[MONITOR] Failed to fetch metrics. HTTP Status: " + response.statusCode());
            return null;
        }
    }

    private static boolean analyze(String metrics) {
        if (metrics == null) return false;
        return true; // Placeholder: In a real implementation, parse metrics and determine if adaptation is needed
    }

    private static String planAdaptation() {
        System.out.println("[PLAN] Determining adaptation strategy.");
        return "ADAPTATION_ACTION_EXAMPLE"; // Example payload or command
    }

    private static void execute(String plan) {
        System.out.println("[EXECUTE] Executing adaptation: " + plan);
        
        // Example 1: Execute via REST (Uncomment to use)
        /*
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TARGET_SERVICE_URL + "/adapt"))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"action\": \"" + plan + "\"}"))
                    .header("Content-Type", "application/json")
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[EXECUTE] REST Adaptation action sent.");
        } catch (Exception e) {
            System.err.println("[EXECUTE] REST adaptation failed.");
        }
        */

        // Example 2: Execute via CLI (simulated local script execution)
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"echo", "Applying CLI adaptation: " + plan});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[CLI OUTPUT] " + line);
            }
        } catch (Exception e) {
            System.err.println("[EXECUTE] CLI adaptation failed.");
        }
    }
}