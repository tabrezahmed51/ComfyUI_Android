# ComfyUI Mobile

**ComfyUI Mobile** is a full-featured visual node-based generative AI workflow studio and ComfyUI client for Android built with Kotlin and Jetpack Compose (Material Design 3).

## Features

- **Interactive Node Graph Studio**:
  - Zoomable and pannable infinite 2D canvas with signature ComfyUI dot-grid aesthetics.
  - Interactive nodes with color-coded category headers (Loaders, Conditioning, Latent, Sampling, Image Output, Post-Processing).
  - Socket ports with type-checked color indicators (MODEL, CLIP, CONDITIONING, LATENT, VAE, IMAGE, MASK).
  - Smooth dynamic cubic Bezier connection wires with real-time glow animation during inference execution.
  - In-node parameter controllers: multiline prompt editor with quick style tags, seed generator with randomize toggle, steps slider, CFG slider, sampler selector, scheduler, and resolution pickers.
  - Move, duplicate, bypass, mute, and delete node controls.

- **Blueprints & Template Pipelines**:
  - Curated workflow presets directly ported from ComfyUI blueprints:
    - *Text to Image (Standard SD 1.5)*
    - *Text to Image (Flux.1 Dev 12B)*
    - *Text to Image (Z-Image-Turbo)*
    - *Image Upscale (UltraSharp 4x ESRGAN)*
    - *Remove Background (BiRefNet)*
  - One-tap "Load in Studio" to populate and configure the canvas.

- **Execution Engine & Queue Management**:
  - Asynchronous pipeline execution with live node-by-node step progress.
  - Dedicated Queue screen monitoring active job, pending prompts, and elapsed generation time.
  - Cancel/Interrupt running tasks.

- **Asset Gallery & Output History**:
  - High-resolution generated artwork viewer.
  - Complete generation metadata inspection: prompt, negative prompt, seed, sampler, CFG, steps, model, resolution, duration.
  - "Load in Studio" button to instantly restore the exact workflow state that generated the image.
  - Export and inspect workflow JSON in standard ComfyUI format.

- **Models & Server Configuration**:
  - Checkpoints, LoRAs, VAEs, ControlNets, and Upscaler weights library.
  - Seamless toggle between Standalone Mobile Engine and Remote ComfyUI Server (`http://10.0.2.2:8188` or LAN).

## Architecture

- **Platform**: Android SDK 36, Kotlin 2.2.10, Jetpack Compose, Material 3
- **Design System**: M3 dark cyber-slate palette with luminous cyan, violet, and amber accents
- **Build System**: Gradle 9.3.1 (Kotlin DSL)
