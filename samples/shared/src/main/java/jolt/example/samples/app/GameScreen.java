package jolt.example.samples.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.FPSLogger;
import imgui.ImDrawData;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.ImTemp;
import imgui.enums.ImGuiCol;
import imgui.enums.ImGuiConfigFlags;
import imgui.gdx.ImGuiGdxImpl;
import imgui.gdx.ImGuiGdxInput;
import imgui.gdx.ImGuiGdxInputMultiplexer;
import jolt.example.graphics.GraphicManagerApi;
import jolt.example.samples.app.tests.vehicle.TankTest;
import static com.badlogic.gdx.Gdx.input;

public class GameScreen extends ScreenAdapter {

    private SamplesApp samplesApp;
    private FPSLogger fpsLogger;

    private ImGuiGdxImpl impl;
    private ImGuiGdxInput input;
    private InputMultiplexer inputMultiplexer;

    @Override
    public void show() {
        inputMultiplexer = new InputMultiplexer();
        samplesApp = new SamplesApp();
        fpsLogger = new FPSLogger();

        ImGui.CreateContext();
        ImGuiIO io = ImGui.GetIO();
        io.set_ConfigFlags(ImGuiConfigFlags.DockingEnable);
        input = new ImGuiGdxInput();
        impl = GraphicManagerApi.graphicApi.getImGuiImpl();
        Gdx.input.setInputProcessor(inputMultiplexer);
        inputMultiplexer.addProcessor(input);
        samplesApp.setup(inputMultiplexer);
//        samplesApp.startTest(NarrowPhaseQueryCastRayTest.class);
//        samplesApp.startTest(BoxShapeTest.class);
        samplesApp.startTest(TankTest.class);
//        samplesApp.startTest(CharacterSpaceShipTest.class);

        ImGuiStyle imGuiStyle = ImGui.GetStyle();
        imGuiStyle.set_Colors(ImGuiCol.WindowBg.getValue(), ImTemp.ImVec4_1(0.00f, 0.00f, 0.00f, 0.6f));
    }

    @Override
    public void render(float delta) {
        GraphicManagerApi.graphicApi.clearScreen(0.1f, 0.1f, 0.8f, 1, true);
        samplesApp.render(delta);
        fpsLogger.log();
        impl.newFrame();
        samplesApp.renderUI();
        ImGui.Render();
        ImDrawData drawData = ImGui.GetDrawData();
        impl.render(drawData);
    }

    @Override
    public void resize(int width, int height) {
        samplesApp.resize(width, height);
    }

    @Override
    public void hide() {
        impl.dispose();
        samplesApp.dispose();
    }
}
