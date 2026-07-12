package jolt.example.samples.app;

import com.badlogic.gdx.Game;

public class JoltGame extends Game {

    @Override
    public void create() {
        setScreen(new InitScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
