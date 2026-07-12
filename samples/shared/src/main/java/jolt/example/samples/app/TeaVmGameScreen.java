package jolt.example.samples.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.FPSLogger;
import jolt.example.graphics.GraphicManagerApi;
import jolt.example.samples.app.tests.vehicle.TankTest;

public class TeaVmGameScreen extends ScreenAdapter {

    private SamplesApp samplesApp;
    private FPSLogger fpsLogger;

    @Override
    public void show() {
        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        samplesApp = new SamplesApp();
        fpsLogger = new FPSLogger();

        Gdx.input.setInputProcessor(inputMultiplexer);
        samplesApp.setupWithoutUI(inputMultiplexer);
        samplesApp.startTest(TankTest.class);
    }

    @Override
    public void render(float delta) {
        GraphicManagerApi.graphicApi.clearScreen(0.1f, 0.1f, 0.8f, 1, true);
        samplesApp.render(delta);
        fpsLogger.log();
    }

    @Override
    public void resize(int width, int height) {
        samplesApp.resize(width, height);
    }

    @Override
    public void hide() {
        samplesApp.dispose();
    }
}
