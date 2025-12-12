# FastqHeaderNormalizer Plugin

## Solving Build Errors (Group ID Mismatch)
If you see **"Plugin's descriptor contains the wrong group ID: io.cdap"**, it is due to a discrepancy in the CDAP maven plugin artifact metadata.

**Solution:**
1. Run the included manual installation script. It downloads the JAR but installs it with the correct internal Group ID (`io.cdap`).
   ```bash
   sh manual-install-plugin.sh
   ```

2. Build the project:
   ```bash
   mvn clean package
   ```

## How to Test in CDAP

1.  **Deploy**: Upload the JAR from `target/` to CDAP Studio.
2.  **Create Pipeline**:
    *   **Source**: Select **"File"**.
        *   *Format*: `text`
        *   *Path*: Path to a local file (e.g., `/tmp/test.fastq`) containing sample headers.
        *   *Output Schema*: Ensure it has a field named `body` (String).
    *   **Transform**: Select **"FastqHeaderNormalizer"**.
        *   *Input Field*: `body`
        *   *Format*: `Auto-Detect`
    *   **Sink**: Select **"Trash"** (just to run it) or **"JSON"** (File) to see output.
3.  **Run Preview**:
    *   Click the **"Preview"** button in the top toolbar.
    *   Once finished, click **"Preview Data"** on the Transform node to verify the parsed fields (RunID, Flowcell, etc.).
