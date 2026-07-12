package jolt.example.samples.app;

import com.badlogic.gdx.ScreenAdapter;
//import imgui.ImGuiLoader;
import imgui.ImGuiLoader;
import jolt.JoltLoader;

public class InitScreen extends ScreenAdapter {

    private JoltGame game;

    private boolean init = false;

    public InitScreen(JoltGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        ImGuiLoader.init((isSuccess, e) -> {
            if(isSuccess) {
                JoltLoader.init((joltSuccess, e2) -> init = joltSuccess);
            }
        });
    }

    @Override
    public void render(float delta) {
        if(init) {
            init = false;
            game.setScreen(new GameScreen());
        }
    }
}
