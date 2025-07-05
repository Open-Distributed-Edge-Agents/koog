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

This demo app supports running Large Language Models (LLMs) locally on your Android device using LiteRT. It includes functionality to download compatible model files directly within the app.

### Supported Models & Downloading

The app comes with a predefined list of LiteRT-compatible models (e.g., Gemma 3n variants) that you can download. These models are typically sourced from Hugging Face (e.g., from `google` or `litert-community` organizations).

**To use a LiteRT model:**

1.  **Navigate to Settings:** Open the Koog Demo App and go to the "Settings" screen.
2.  **Select LiteRT Provider:** Choose "LiteRT" from the "LLM Provider" options. This will display the LiteRT model management section.
3.  **Manage LiteRT Models:**
    *   You will see a list of available LiteRT models (e.g., "Gemma 3n E2B", "Gemma 3n E4B").
    *   **Download:** If a model shows "Not Downloaded" or "Error", click the "Download" (or "Retry Download") button next to it. The app will download the model file and store it in its private storage (`Android/data/com.jetbrains.example.kotlin_agents_demo_app/files/litert_models/`). You can see the download progress.
    *   **Select:** Once a model is "Downloaded", click the "Select" button next to it to make it the active LiteRT model for the agents. The currently selected model will be indicated.
    *   **Delete:** If you want to remove a downloaded model from your device to free up space, click the "Delete" (trash icon) button.
4.  **Save Settings:** Ensure your selections are active by tapping the "Save" (check icon) button at the top right if you've made changes to the provider or selected a new LiteRT model. (Note: Downloading and selecting a model automatically updates and saves the relevant LiteRT model ID and path).

After these steps, any agent configured to use the selected LLM provider will use your chosen downloaded LiteRT model.

**Note on LiteRT Model Files:**
*   The download URLs and expected model details are currently hardcoded in the app (see `SupportedLiteRTModels.kt`).
*   Model files (`.task` format) can be large, so ensure you have sufficient storage space and a stable internet connection (preferably Wi-Fi) for downloading.

**Note on LiteRT Performance:**
*   LiteRT is optimized for on-device performance, but execution speed will vary significantly based on your device's hardware (CPU/GPU) and the model size.
*   The first run after selecting a new model might be slower due to model loading and initialization.
*   Emulator support for LiteRT can be unreliable; physical devices are recommended.

## Usage Examples

### Calculator Agent
An agent that can perform mathematical operations using tools for addition, subtraction, multiplication and division.

### Weather Agent
An agent that can provide weather information for a given location.
