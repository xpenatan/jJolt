package jolt.example.graphics;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import imgui.gdx.ImGuiGdxImpl;
import jolt.gdx.JoltDebugRenderer;

public interface GraphicApi {
    void clearScreen(float r, float g, float b, float a, boolean clearDepth);
    JoltDebugRenderer createDebugRenderer();
    ImmediateModeRenderer createImmediateModeRenderer();
    Batch createSpriteBatch();
    BitmapFont createBitmapFont();
    GraphicType getGraphicType();
    ImGuiGdxImpl getImGuiImpl();
}