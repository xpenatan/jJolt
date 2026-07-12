package jolt.gdx.gl;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import jolt.gdx.JoltDebugRenderer;

public class GdxDebugRenderer extends JoltDebugRenderer {

    private ModelBatch batch;

    public GdxDebugRenderer() {
        batch = new ModelBatch();
    }

    @Override
    protected Mesh createMesh(boolean isStatic, int maxVertices, int maxIndices, VertexAttribute... attributes) {
        return new Mesh(isStatic, maxVertices, maxIndices, attributes);
    }

    @Override
    protected Texture createTexture(Pixmap pixmap, boolean useMipMaps) {
        return new Texture(pixmap, useMipMaps);
    }

    @Override
    protected MeshPart createMeshPart() {
        return new MeshPart();
    }

    @Override
    protected MeshPart createMeshPart(String id, Mesh mesh, int offset, int size, int type) {
        return new MeshPart(id, mesh, offset, size, type);
    }

    @Override
    protected Model createModel() {
        return new Model();
    }

    @Override
    protected void batchBegin(Camera camera) {
        batch.begin(camera);
    }

    @Override
    protected void batchEnd() {
        batch.end();
    }

    @Override
    protected void batchRender(RenderableProvider renderableProvider, Environment environment) {
        batch.render(renderableProvider, environment);
    }

    @Override
    public void onNativeDispose() {
        super.onNativeDispose();
        batch.dispose();
    }
}
