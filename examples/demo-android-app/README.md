# Koog Demo App

## Overview
This is a simple demo Android app built with Jetpack Compose that demonstrates the capabilities of Koog, a Kotlin AI agentic framework.

## Setup
1. Open the project in IntelliJ IDEA or Android Studio.
2. Build and run the application on an Android device or emulator.
3. **Configure LLM Provider**:
    - In the app, navigate to **Settings**.
    - **OpenAI**: If using OpenAI, enter your OpenAI API key.
    - **LiteRT (On-Device)**: See the "Using LiteRT On-Device Models" section below for LiteRT setup.
    - (Other providers like Anthropic may require API keys if configured).

## Using LiteRT On-Device Models (e.g., Gemma 3n)

This demo app supports running Large Language Models locally on your Android device using LiteRT.

### 1. Obtain a Model File

*   You need a LiteRT-compatible model file in `.task` format.
*   Google's Gemma 3n models are supported. You can find compatible LiteRT versions (e.g., `gemma-3n-e2b-it-cpu.task`, `gemma-3n-e4b-it-cpu.task`) on platforms like Hugging Face, often from the `google` or `litert-community` organizations.
    *   Example for Gemma 3n E2B (2 Billion parameters, instruction-tuned, CPU): Search for "gemma-3n-E2B-it-litert-preview" or similar on Hugging Face. Download the `.task` file.
*   Ensure you download a model variant suitable for your device (e.g., CPU version if you don't have a compatible GPU or are unsure).

### 2. Place the Model File on Your Device

You need to copy the downloaded `.task` file to a location on your Android device that the app can access and whose absolute path you can determine. Here are a few common methods:

*   **Using `adb push` (for developers):**
    ```bash
    adb push path/to/your/downloaded_model.task /data/local/tmp/model.task
    ```
    In this case, the path to enter in the app settings would be `/data/local/tmp/model.task`. This path is generally accessible for debugging purposes.

*   **Using Device File Manager (to app-specific directory - Recommended for ease):**
    1.  Connect your device to your computer.
    2.  Use your computer's file explorer or Android Studio's Device File Explorer to navigate to your device's storage.
    3.  Go to `Android/data/com.jetbrains.example.kotlin_agents_demo_app/files/`. (If the `files` directory doesn't exist, you can create it or the app might create it on first launch if it tries to access it).
    4.  Copy your `.task` model file into this `files` directory.
    5.  The path to enter in the app settings would then be: `/storage/emulated/0/Android/data/com.jetbrains.example.kotlin_agents_demo_app/files/your_model.task` (or similar, the initial part `/storage/emulated/0` might vary slightly depending on the device, but `Android/data/...` is standard for external app-specific storage). You can often get the base path by using `context.getExternalFilesDir(null).getAbsolutePath()` in code if you were building the path programmatically. For manual entry, you'll need to determine this full path.

*   **Using Device File Manager (to other accessible folders like Downloads):**
    1.  Copy the `.task` file to a common folder like `Downloads`.
    2.  You will then need to determine the absolute path to this file (e.g., `/storage/emulated/0/Download/your_model.task`). This can sometimes be tricky for users to find accurately.

### 3. Configure in App Settings

1.  Open the Koog Demo App.
2.  Go to **Settings**.
3.  Select **LiteRT** as the "LLM Provider".
4.  From the "LiteRT Model" dropdown, select the model you downloaded (e.g., "Gemma 3n E2B").
5.  In the "LiteRT Model Path" field, carefully enter the **full absolute path** to where you placed the `.task` file on your device.
6.  Save the settings.

Now, when you run an agent (e.g., Weather Agent), it should use the local LiteRT model.

**Note on LiteRT Performance:**
*   LiteRT is optimized for on-device performance, but execution speed will vary significantly based on your device's hardware (CPU/GPU) and the model size.
*   The first run after selecting a new model might be slower due to model loading and initialization.
*   Emulator support for LiteRT can be unreliable; physical devices are recommended.

## Usage Examples

### Calculator Agent
An agent that can perform mathematical operations using tools for addition, subtraction, multiplication and division.

### Weather Agent
An agent that can provide weather information for a given location.
