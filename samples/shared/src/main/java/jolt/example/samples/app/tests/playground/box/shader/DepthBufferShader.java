package jolt.example.samples.app.tests.playground.box.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class DepthBufferShader implements Shader {
        public ShaderProgram program;

        private int u_projTrans;

        String SHADER_NAME = "depthbuffer";

        @Override
        public void init () {
            String vert = Gdx.files.internal("shaders\\" + SHADER_NAME + ".vertex.glsl").readString();
            String frag = Gdx.files.internal("shaders\\" + SHADER_NAME + ".fragment.glsl").readString();
            program = new ShaderProgram(vert, frag);
            if (!program.isCompiled())
                throw new GdxRuntimeException(program.getLog());
            u_projTrans = program.getUniformLocation("u_projTrans");
        }


        @Override
        public void dispose () {
            program.dispose();
        }

        @Override
        public void begin (Camera camera, RenderContext context) {
            program.bind();
            program.setUniformMatrix(u_projTrans, camera.combined);
        }

        @Override
        public void end() {
            //super.end();

        }

    @Override
    public int compareTo(Shader other) {
        return 0;
    }

    @Override
    public boolean canRender(Renderable instance) {
        return true;
    }

    @Override
        public void render (Renderable renderable) {
            renderable.meshPart.render(program);
        }
}

