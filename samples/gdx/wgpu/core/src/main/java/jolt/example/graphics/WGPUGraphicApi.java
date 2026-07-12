package jolt.example.graphics;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.monstrous.gdx.webgpu.graphics.g2d.WgBitmapFont;
import com.monstrous.gdx.webgpu.graphics.g2d.WgSpriteBatch;
import com.monstrous.gdx.webgpu.graphics.utils.WgImmediateModeRenderer;
import com.monstrous.gdx.webgpu.graphics.utils.WgScreenUtils;
import imgui.gdx.ImGuiGdxImpl;
import imgui.gdx.ImGuiGdxWGPUImpl;
import jolt.gdx.JoltDebugRenderer;
import jolt.gdx.wgpu.WGPUDebugRenderer;

public class WGPUGraphicApi implements GraphicApi {
    @Override
    public void clearScreen(float r, float g, float b, float a, boolean clearDepth) {
        WgScreenUtils.clear(r, g, b, a, clearDepth);
    }

    @Override
    public JoltDebugRenderer createDebugRenderer() {
        return new WGPUDebugRenderer();
    }

    @Override
    public ImmediateModeRenderer createImmediateModeRenderer() {
        return new WgImmediateModeRenderer(false, true, 0);
    }

    @Override
    public Batch createSpriteBatch() {
        return new WgSpriteBatch();
    }

    @Override
    public BitmapFont createBitmapFont() {
        return new WgBitmapFont();
    }

    @Override
    public GraphicType getGraphicType() {
        return GraphicType.WGPU;
    }

    @Override
    public ImGuiGdxImpl getImGuiImpl() {
        return new ImGuiGdxWGPUImpl();
    }
}