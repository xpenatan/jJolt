package jolt.example.graphics;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.utils.ScreenUtils;
import imgui.gdx.ImGuiGdxGLImpl;
import imgui.gdx.ImGuiGdxImpl;
import jolt.gdx.JoltDebugRenderer;
import jolt.gdx.gl.GdxDebugRenderer;

public class GdxGraphicApi implements GraphicApi {
    @Override
    public void clearScreen(float r, float g, float b, float a, boolean clearDepth) {
        ScreenUtils.clear(r, g, b, a, clearDepth);
    }

    @Override
    public JoltDebugRenderer createDebugRenderer() {
        return new GdxDebugRenderer();
    }

    @Override
    public ImmediateModeRenderer createImmediateModeRenderer() {
        return new ImmediateModeRenderer20(false, true, 0);
    }

    @Override
    public Batch createSpriteBatch() {
        return new SpriteBatch();
    }

    @Override
    public BitmapFont createBitmapFont() {
        return new BitmapFont();
    }

    @Override
    public GraphicType getGraphicType() {
        return GraphicType.OpenGL;
    }

    @Override
    public ImGuiGdxImpl getImGuiImpl() {
        return new ImGuiGdxGLImpl();
    }
}