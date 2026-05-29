# GitPilot

GitPilot is a lightweight, high-performance terminal user interface (TUI) automation tool written in Java. It scans your local Git repository for unstaged or modified files, lets you interactively batch-select your staging targets, and uses the Gemini 2.5 Flash API to generate clean, standardized conventional commit messages for each file on the fly.

Designed for streamlined developer workflows, it cuts through terminal clutter with high-visibility progress metrics and real-time generation logs — no unnecessary visual noise.

---

## Core Features

- **Interactive Target Selection:** Scans your working directory and prompts you line-by-line to stage only the files you want to process.
- **AI-Powered Contextual Commits:** Integrates with the Gemini API to analyze file context and generate precise, conventional commit strings (`feat:`, `fix:`, `chore:`).
- **Streamlined TUI Engine:** Built with Picocli and Jansi for color-coded execution states, high-performance terminal rendering, and incremental progress bar logging.
- **Fail-Safe Fallbacks:** Robust error handling that automatically falls back to a stable commit message if API rate limits or network issues occur — preventing execution crashes.
- **Upstream Synchronization:** Automatically prompts you to push your compiled local commits to the remote origin branch upon completion.

---

## Architecture & Tech Stack

| Layer | Technology |
|---|---|
| Runtime Environment | Java 17 |
| Build Automation | Apache Maven 3+ |
| CLI Parsing Framework | Picocli v4.7.7 |
| ANSI Terminal Rendering | Jansi v2.4.1 |
| LLM Foundation | Google Gemini 2.5 Flash API |

---

## Installation & Setup

GitPilot packages into a standalone executable Fat JAR containing all necessary dependencies. You can build and bind it globally in minutes.

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/pico.git
cd pico
```

### 2. Compile and Package

Use the Maven wrapper to clean your target directory, download dependencies, and assemble the binary:

```bash
mvn clean package
```

This produces a fully integrated executable — `pico-1.0.0.jar` — inside your `target/` directory.

### 3. Configure Your API Key

GitPilot reads your Gemini API key from an environment variable:

```bash
export GEMINI_API_KEY="your_actual_gemini_api_key_here"
```

### 4. Create a Global Alias (Optional)

To run GitPilot from any directory without typing the full Java invocation, add an alias to your shell config:

```bash
# Open your shell config
nano ~/.bashrc

# Append this line (update the path to your actual file location)
alias gitpilot="java -jar /absolute/path/to/git_pilot/target/pico-1.0.0.jar"

# Save, exit, and reload
source ~/.bashrc
```

---

## Usage

Navigate to any active Git repository and run:

```bash
gitpilot .
```

### Step 1 — Interactive Target Confirmation

GitPilot scans your working directory and asks you to confirm each modified file:

```text
Reviewing modified repository assets:

 Staging target -> src/main/java/com/gitpilot/GitPilot.java? (Y/n): y
 Staging target -> pom.xml? (Y/n): y
```

### Step 2 — Live Processing Pipeline

The batch engine fires API calls, updates the colorized progress bar, and logs generated commit messages in real time:

```text
Processing batch updates... (Hitting Gemini API)

[███████████████░░░░░░░░░░░░░░]  50% Committed: src/main/java/com/gitpilot/GitPilot.java
    └─ Generated Message: "refactor(ui): optimize text stream output and eliminate terminal flicker"
[██████████████████████████████] 100% Committed: pom.xml
    └─ Generated Message: "chore: integrate maven assembly plugin for executable fat jar packaging"
```

### Step 3 — Upstream Synchronization

After all commits are processed locally, GitPilot prompts you to push to your remote branch:

```text
====================================================
Push local batch modifications to remote branch origin? (Y/n): y
Synchronizing changes upstream...
Everything up-to-date
GitPilot dashboard session closed cleanly!
```

---

## License

GitPilot is open-source and available under the [MIT License](LICENSE). Feel free to fork it, add features, or adapt its core tracking loop logic for your own projects.


```markdown
---

> **Wait, you read the whole thing?** 🤯 
> That deserves a medal—or better yet, a cold treat. Ping me on Instagram at [@billa_pandey16] and tell me your favorite flavor. I officially owe you an ice cream! 🍦
```
