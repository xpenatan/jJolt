package jolt.example.samples.app.tests.playground.box.shader;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import net.mgsx.gltf.scene3d.shaders.PBRShader;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;

public class MyPBRShaderProvider extends PBRShaderProvider {
    public MyPBRShaderProvider() {
        super(PBRShaderProvider.createDefaultConfig());
    }

    // override this to force #version 140, needed for the inverse() built-in
    @Override
    public String createPrefixBase(Renderable renderable, PBRShaderConfig config) {
        if(Gdx.app.getType() == Application.ApplicationType.Desktop)
            config.glslVersion = "#version 140\n" + "#define GLSL3\n";
        else
            config.glslVersion = "#version 300 es\n" + "#define GLSL3\n";
        return super.createPrefixBase(renderable, config);
    }

    @Override
    protected PBRShader createShader(Renderable renderable, PBRShaderConfig config, String prefix){
        if( renderable.meshPart.mesh.isInstanced()) {
            prefix += "#define instanced\n";
        }
        config.vertexShader = Gdx.files.internal("shaders/pbr/pbr.vs.glsl").readString();
        return new MyPBRShader(renderable, config, prefix);
    }

}
