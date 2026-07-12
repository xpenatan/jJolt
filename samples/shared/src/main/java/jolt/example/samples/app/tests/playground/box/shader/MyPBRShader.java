package jolt.example.samples.app.tests.playground.box.shader;

import com.badlogic.gdx.graphics.g3d.Renderable;
import net.mgsx.gltf.scene3d.shaders.PBRShader;

public class MyPBRShader extends PBRShader {

    private boolean isInstancedShader;

    public MyPBRShader(Renderable renderable, Config config, String prefix) {
        super(renderable, config, prefix);
        isInstancedShader = renderable.meshPart.mesh.isInstanced();
    }

    @Override
    public boolean canRender(Renderable renderable) {
        if(renderable.meshPart.mesh.isInstanced() != isInstancedShader ) {
            return false;
        }
        return super.canRender(renderable);
    }
}
