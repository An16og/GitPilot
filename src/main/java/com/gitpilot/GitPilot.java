package com.gitpilot;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import org.fusesource.jansi.AnsiConsole;
import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;

@Command(name = "gitpilot", mixinStandardHelpOptions = true, version = "1.0.0",
        description = "Batch-processing TUI Automator with streamlined progress tracking.")
public class GitPilot implements Callable<Integer> {

    @Parameters(index = "0", description = "The target root directory of your git project.")
    private File targetDir;

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        int exitCode = new CommandLine(new GitPilot()).execute(args);
        AnsiConsole.systemUninstall();
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println(ansi().fg(RED).bold().a("Error: GEMINI_API_KEY environment variable is not set!").reset());
            return 1;
        }

        if (targetDir == null || !targetDir.exists() || !targetDir.isDirectory()) {
            System.err.println(ansi().fg(RED).bold().a("Error: Target path is invalid or is not a directory!").reset());
            return 1;
        }

        System.out.println(ansi().fg(CYAN).bold().a("\n====================================================").reset());
        System.out.println(ansi().fg(CYAN).bold().a(" [GitPilot Streamlined Engine] Active Path: ").reset().a(targetDir.getAbsolutePath()));
        System.out.println(ansi().fg(CYAN).bold().a("====================================================").reset());

        List<String> changedFiles = getGitStatusFiles();
        if (changedFiles.isEmpty()) {
            System.out.println(ansi().fg(GREEN).bold().a(" Working tree completely clean! Nothing to process.").reset());
            return 0;
        }

        Scanner scanner = new Scanner(System.in);
        List<String> targetFiles = new ArrayList<>();

        System.out.println(ansi().fg(YELLOW).bold().a("\nReviewing modified repository assets:\n").reset());
        for (String filePath : changedFiles) {
            if (filePath.startsWith("target/") || filePath.contains("/target/")) {
                continue;
            }

            System.out.print(ansi().a(" Staging target -> ").fg(YELLOW).bold().a(filePath).reset().a("? (Y/n): "));
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if (choice.isEmpty() || choice.equals("y")) {
                targetFiles.add(filePath);
            }
        }

        if (targetFiles.isEmpty()) {
            System.out.println(ansi().fg(WHITE).a("\n No files selected. Closing active session safely.").reset());
            return 0;
        }

        System.out.println(ansi().fg(CYAN).a("\n Processing batch updates... (Hitting Gemini API)\n").reset());
        
        HttpClient client = HttpClient.newHttpClient();
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
        
        int totalFiles = targetFiles.size();
        
        for (int i = 0; i < totalFiles; i++) {
            String filePath = targetFiles.get(i);
            int currentStep = i + 1;

            String promptText = "Generate a short, single-line conventional git commit message for changes made to the file: " + filePath;
            String escapedPrompt = promptText.replace("\"", "\\\"");

            String jsonPayload = """
            {
                "contents": [{
                    "parts":[{"text": "%s"}]
                }],
                "systemInstruction": {
                    "parts": [{"text": "You are a strict, automated CLI tool. Output ONLY a single, brief conventional Git commit message string. Do not include markdown blocks, backticks (```), conversational commentary, or bullet points. Output exactly the raw text line of the commit message itself."}]
                }
            }
            """.formatted(escapedPrompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String targetedMessage;
            boolean isError = false;

            if (response.statusCode() != 200) {
                targetedMessage = "chore: update " + filePath + " due to system configuration requirements";
                isError = true;
            } else {
                String rawResponse = response.body();
                String startMarker = "\"text\": \"";
                int startIdx = rawResponse.indexOf(startMarker);

                if (startIdx == -1) {
                    targetedMessage = "chore: patch adjustments in " + filePath;
                } else {
                    startIdx += startMarker.length();
                    int endIdx = rawResponse.indexOf("\"", startIdx);
                    targetedMessage = rawResponse.substring(startIdx, endIdx).trim();
                    
                    targetedMessage = targetedMessage.replace("```", "")
                                                     .replace("`", "")
                                                     .replace("\\n", "")
                                                     .trim();
                }
            }

            executeGitCommandSilently("git", "add", filePath);
            executeGitCommandSilently("git", "commit", "-m", targetedMessage);

            printProgress(currentStep, totalFiles, "Committed: " + filePath, isError);
            
            if (isError) {
                System.out.println(ansi().a("    └─ ").fg(RED).bold().a("Fallback Message: ").reset().a("\"" + targetedMessage + "\""));
            } else {
                System.out.println(ansi().a("    └─ ").fg(GREEN).bold().a("Generated Message: ").reset().a("\"" + targetedMessage + "\""));
            }

            Thread.sleep(2000);
        }

        System.out.println(ansi().fg(CYAN).bold().a("\n====================================================").reset());
        System.out.print(ansi().fg(GREEN).bold().a("Push local batch modifications to remote branch origin? (Y/n): ").reset());
        String pushChoice = scanner.nextLine().trim().toLowerCase();
        if (pushChoice.isEmpty() || pushChoice.equals("y")) {
            System.out.println(ansi().fg(BLUE).a("Synchronizing changes upstream...").reset());
            executeGitCommandWithOutput("git", "push");
        } else {
            System.out.println(ansi().fg(YELLOW).a("Changes preserved locally. Skipping remote transmission.").reset());
        }

        System.out.println(ansi().fg(GREEN).bold().a("GitPilot dashboard session closed cleanly!").reset());
        return 0;
    }

    public static void printProgress(int current, int total, String label, boolean isError) {
        int width = 30;
        int filled = (int) ((double) current / total * width);
        
        String filledBar = ansi().fg(GREEN).a("█".repeat(filled)).reset().toString();
        String emptyBar = ansi().fg(DEFAULT).a("░".repeat(width - filled)).reset().toString();
        String bar = filledBar + emptyBar;
        
        int percent = (int) ((double) current / total * 100);
        String cleanLabel = label.length() > 40 ? label.substring(0, 37) + "..." : label;
        
        org.fusesource.jansi.Ansi.Color labelColor = isError ? RED : (current == total ? GREEN : BLUE);
        
        String statsTag = ansi().fg(CYAN).a(String.format("%3d%%", percent)).reset().toString();
        String labelTag = ansi().fg(labelColor).a(String.format(" %-45s", cleanLabel)).reset().toString();
        
        System.out.println("[" + bar + "] " + statsTag + " " + labelTag);
        System.out.flush();
    }

    private List<String> getGitStatusFiles() {
        List<String> files = new ArrayList<>();
        try {
            Process process = new ProcessBuilder("git", "status", "--short")
                    .directory(targetDir)
                    .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.length() > 3) {
                        files.add(line.substring(3).trim());
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Failed to scan git status: " + e.getMessage());
        }
        return files;
    }

    private void executeGitCommandSilently(String... command) {
        try {
            Process process = new ProcessBuilder(command)
                    .directory(targetDir)
                    .start();
            process.waitFor();
        } catch (Exception e) {
        }
    }

    private void executeGitCommandWithOutput(String... command) {
        try {
            Process process = new ProcessBuilder(command)
                    .directory(targetDir)
                    .inheritIO()
                    .start();
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Command execution failure: " + e.getMessage());
        }
    }
}