package jolt.example.samples.app;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.ScreenAdapter;
import jolt.JoltLoader;

public class TeaVmJoltGame extends Game {

    private final boolean loadRuntime;

    public TeaVmJoltGame() {
        this(true);
    }

    public TeaVmJoltGame(boolean loadRuntime) {
        this.loadRuntime = loadRuntime;
    }

    @Override
    public void create() {
        if(loadRuntime) {
            setScreen(new TeaVmInitScreen(this));
        }
        else {
            setScreen(new TeaVmGameScreen());
        }
    }
}

final class TeaVmInitScreen extends ScreenAdapter {

    private final TeaVmJoltGame game;
    private boolean init;

    TeaVmInitScreen(TeaVmJoltGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        JoltLoader.init((isSuccess, e) -> init = isSuccess);
    }

    @Override
    public void render(float delta) {
        if(init) {
            init = false;
            game.setScreen(new TeaVmGameScreen());
        }
    }
}
