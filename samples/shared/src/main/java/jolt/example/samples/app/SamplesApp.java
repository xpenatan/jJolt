package jolt.example.samples.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
//import imgui.ImGui;
//import imgui.ImGuiCond;
//import imgui.ImGuiTabBarFlags;
//import imgui.ImVec2;
import imgui.ImGui;
import imgui.ImTemp;
import imgui.enums.ImGuiCond;
import imgui.enums.ImGuiTabBarFlags;
import jolt.example.graphics.GraphicManagerApi;
import jolt.example.samples.app.imgui.FPSRenderer;
import jolt.example.samples.app.imgui.ImGuiSettingsRenderer;
import jolt.example.samples.app.jolt.JoltInstance;
import jolt.example.samples.app.tests.Test;
import jolt.example.samples.app.tests.TestGroup;
import jolt.example.samples.app.tests.Tests;
import jolt.gdx.JoltDebugRenderer;
import jolt.physics.body.BodyManagerDrawSettings;
import jolt.renderer.DebugRenderer;

public class SamplesApp extends InputAdapter {
    private boolean isPaused;

    private Test test;

    private JoltDebugRenderer debugRenderer;
    private BitmapFont font;
    private Batch batch;
    private OrthographicCamera batchCamera;

    private BodyManagerDrawSettings debugSettings;

    private PerspectiveCamera camera;
    private ScreenViewport viewport;
    private CameraInputController cameraController;
    private Tests tests;

    private JoltInstance joltInstance;

    private ImGuiSettingsRenderer settingsRenderer;
    private TestGroup allTests;

    private FPSRenderer fpsRenderer;

    String fontText;

    public void setup(InputMultiplexer input) {
        fpsRenderer = new FPSRenderer();
        settingsRenderer = new ImGuiSettingsRenderer();
        setupCore(input, new Tests());
    }

    public void setupWithoutUI(InputMultiplexer input) {
        setupCore(input, Tests.createWebSafe());
    }

    private void setupCore(InputMultiplexer input, Tests testRegistry) {
        tests = testRegistry;
        allTests = tests.getAllTests();

        debugRenderer = GraphicManagerApi.graphicApi.createDebugRenderer();
        debugSettings = new BodyManagerDrawSettings();
        camera = new PerspectiveCamera();
        viewport = new ScreenViewport(camera);
        camera.far = 1000f;

        cameraController = new CameraInputController(camera);
        cameraController.autoUpdate = false;
        cameraController.forwardTarget = false;
        cameraController.translateTarget = false;

        input.addProcessor(this);
        input.addProcessor(cameraController);

        batch = GraphicManagerApi.graphicApi.createSpriteBatch();
        font = GraphicManagerApi.graphicApi.createBitmapFont();
        batchCamera = new OrthographicCamera();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void render(float delta) {
        camera.update();
        // Don't go below 30 Hz to prevent spiral of death
        float deltaTime = (float)Math.min(delta, 1.0 / 30.0);
        if(test != null) {
            if(!isPaused) {
                test.processInput();
            }
            test.updateCamera(cameraController);

            cameraController.update();
        }
        drawPhysics();
        if(deltaTime > 0) {
            stepPhysics(deltaTime);
        }
    }

    public void renderUI() {
        Class<Test> newTest = null;
        ImGui.SetNextWindowSize(ImTemp.ImVec2_1(250, 400), ImGuiCond.FirstUseEver);
        ImGui.Begin("Settings");

        fpsRenderer.render();

        newTest = settingsRenderer.render(allTests);

        settingsRenderer.idlBool.set(isPaused);
        if(ImGui.Checkbox("IsPaused", settingsRenderer.idlBool)) {
            isPaused = settingsRenderer.idlBool.getValue();
        }

        settingsRenderer.idlBool.set(debugRenderer.isEnable());
        if(ImGui.Checkbox("DebugRenderer", settingsRenderer.idlBool)) {
            debugRenderer.setEnable(settingsRenderer.idlBool.getValue());
        }

        if(ImGui.BeginTabBar("##Settings", ImGuiTabBarFlags.FittingPolicyScroll.or(ImGuiTabBarFlags.Reorderable))) {
            if(ImGui.BeginTabItem("Physics")) {
                settingsRenderer.render(joltInstance);
                ImGui.EndTabItem();
            }
            if(ImGui.BeginTabItem("DebugRenderer")) {
                settingsRenderer.render(debugSettings);
                ImGui.EndTabItem();
            }
            if(ImGui.BeginTabItem("Test")) {
                test.renderUI();
                ImGui.EndTabItem();
            }
            ImGui.EndTabBar();
        }
        ImGui.End();

        if(newTest != null) {
            startTest(newTest);
        }

        renderTestUI();
    }

    private void renderTestUI() {
        batchCamera.setToOrtho(false);
        batch.setProjectionMatrix(batchCamera.combined);
        batch.begin();
        font.draw(batch, fontText, 50, Gdx.graphics.getHeight()-50);
        test.renderUI(batch, font);
        batch.end();
    }

    public void startTest(Class<? extends Test> testClass) {
        if(test != null) {
            test.dispose();
            test = null;
        }
        if(joltInstance != null) {
            joltInstance.dispose();
            joltInstance = null;
        }
        joltInstance = new JoltInstance();

        isPaused = true;
        cameraController.target.set(0, 0,0);
        camera.up.set(0, 1, 0);
        camera.position.set(30, 10, 30);
        camera.lookAt(0, 0, 0);
        test = tests.getTest(testClass);
        debugRenderer.setEnable(true);
        test.setJoltInstance(joltInstance);
        test.setRenderer(debugRenderer);
        test.initializeCamera(camera);
        test.initialize();
        test.processInput();

        fontText = "Test: " + test.getClass().getSimpleName() + "\n" +
                "Hotkeys:\n" +
                "P: Pause/Unpause\n" +
                "R: Reset\n" +
                "Shift + R: Next Test\n" +
                "D: Debug Renderer On/Off\n" +
                "W: Wireframe On/Off";
    }

    private void drawPhysics() {
        debugRenderer.begin(camera);
        debugRenderer.DrawBodies(joltInstance.getPhysicsSystem(), debugSettings);
        debugRenderer.end();
    }

    public void stepPhysics(float deltaTime) {
        // When running below 55 Hz, do 2 steps instead of 1
        int numSteps = deltaTime > 1.0 / 55.0 ? 2 : 1;
        boolean isPlaying = !isPaused;
        if(test != null) {
            test.prePhysicsUpdate(isPlaying);
        }
        if(isPlaying) {
            joltInstance.update(deltaTime, numSteps);
        }
        if(test != null) {
            test.postPhysicsUpdate(isPlaying, deltaTime);
        }
    }

    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    public void dispose() {
        if(fpsRenderer != null) {
            fpsRenderer.dispose();
        }
        if(settingsRenderer != null) {
            settingsRenderer.dispose();
        }
        clearBodies();
        debugRenderer.dispose();
        debugSettings.dispose();
        joltInstance.dispose();
    }

    private void clearBodies() {
        debugRenderer.clear();
        joltInstance.clearWorld();
    }

    @Override
    public boolean keyUp(int keycode) {
        if(keycode == Input.Keys.P) {
            isPaused = !isPaused;
            return true;
        }
        else if(keycode == Input.Keys.R) {
            if(test != null) {
                Class<? extends Test> aClass = test.getClass();
                if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                    aClass = tests.getNextTest(aClass);
                }
                startTest(aClass);
            }
            return true;
        }
        else if(keycode == Input.Keys.D) {
            debugRenderer.setEnable(!debugRenderer.isEnable());
        }
        else if(keycode == Input.Keys.W) {
            boolean mDrawShapeWireframe = debugSettings.get_mDrawShapeWireframe();
            debugSettings.set_mDrawShapeWireframe(!mDrawShapeWireframe);
        }
        return false;
    }
}
