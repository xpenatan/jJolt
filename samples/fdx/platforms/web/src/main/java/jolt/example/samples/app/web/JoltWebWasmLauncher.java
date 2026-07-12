package jolt.example.samples.app.web;

public final class JoltWebWasmLauncher {
    private JoltWebWasmLauncher() {
    }

    public static void main(String[] args) {
        JoltWebLauncherSupport.start("Wasm", args);
    }
}
