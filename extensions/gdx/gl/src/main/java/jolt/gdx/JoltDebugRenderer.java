package jolt.gdx;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.xpenatan.jparser.runtime.helper.NativeFloatArray;
import jolt.core.Color;
import jolt.enums.ECastShadow;
import jolt.enums.ECullMode;
import jolt.enums.EDrawMode;
import jolt.math.Mat44;
import jolt.math.Vec4;
import jolt.physics.PhysicsSystem;
import jolt.physics.body.BodyManagerDrawSettings;
import jolt.renderer.DebugRendererEm;

public abstract class JoltDebugRenderer extends DebugRendererEm {

    protected Environment environment;

    private Texture checkerboardTexture;

    private boolean enable;
    private boolean mDrawShapeWireframe;

    // Cache for models, keyed by triangle array hash and draw mode
    private IntMap<Model> modelBatch = new IntMap<>();
    private Array<ModelRenderer> modelRendererList = new Array<>();

    private Pool<ModelRenderer> modelInstancePool = new Pool<ModelRenderer>() {
        @Override
        protected ModelRenderer newObject() {
            return new ModelRenderer();
        }
    };

    protected abstract Mesh createMesh(boolean isStatic, int maxVertices, int maxIndices, VertexAttribute... attributes);
    protected abstract Texture createTexture(Pixmap pixmap, boolean useMipMaps);
    protected abstract MeshPart createMeshPart();
    protected abstract MeshPart createMeshPart(final String id, final Mesh mesh, final int offset, final int size, final int type);
    protected abstract Model createModel();
    protected abstract void batchBegin(Camera camera);
    protected abstract void batchEnd();
    protected abstract void batchRender(RenderableProvider renderableProvider, Environment environment);

    public Texture createCheckerBoardTexture() {
        // Create a simple 2x2 checkerboard texture
        Pixmap pixmap = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.3f, 0.3f, 0.3f, 1f); // Dark gray
        pixmap.fillRectangle(0, 0, 1, 1); // Top-left
        pixmap.fillRectangle(1, 1, 1, 1); // Bottom-right
        pixmap.setColor(0.7f, 0.7f, 0.7f, 1f); // Light gray
        pixmap.fillRectangle(1, 0, 1, 1); // Top-right
        pixmap.fillRectangle(0, 1, 1, 1); // Bottom-left
        Texture checkerboardTexture = createTexture(pixmap, true); // Enable mipmapping for smoothness
        checkerboardTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        pixmap.dispose();
        return checkerboardTexture;
    }

    public JoltDebugRenderer() {
        this( true);
    }

    public JoltDebugRenderer(boolean enable) {
        this.enable = enable;
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1.f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -0.5f, -1.0f, -0.8f));
        checkerboardTexture = createCheckerBoardTexture();
    }

    @Override
    protected void DrawMesh(int id, Mat44 inModelMatrix, NativeFloatArray vertices, Color inModelColor, ECullMode inCullMode, EDrawMode inDrawMode) {
        int verticesSize = vertices.getSize();
        if (!enable || verticesSize == 0) {
            return;
        }

        Model model = modelBatch.get(id);
        if(model == null) {
            model = createModel(vertices, GL20.GL_TRIANGLES);
            modelBatch.put(id, model);
        }

        Vec4 vec4 = inModelColor.ToVec4();
        float r1 = vec4.GetX();
        float g1 = vec4.GetY();
        float b1 = vec4.GetZ();
        float a1 = vec4.GetW();

        int primitiveType = inDrawMode == EDrawMode.EDrawMode_Wireframe ? GL20.GL_LINES : GL20.GL_TRIANGLES;
        ModelRenderer modelRenderer = modelInstancePool.obtain();
        modelRenderer.setModel(model, primitiveType);
        JoltGdx.convert(inModelMatrix, modelRenderer.transform);
        modelRenderer.diffuseColor.color.set(r1, g1, b1, a1);
        modelRendererList.add(modelRenderer);
    }

    private Model createModel(NativeFloatArray vertices, int primitiveType) {

        float [] array = new float[vertices.getSize()];
        for(int i = 0; i < vertices.getSize(); i++) {
            array[i] = vertices.getValue(i);
        }

        int localVerticesSize = vertices.getSize();
        Mesh mesh = createMesh(true, localVerticesSize / 12, 0, // 12 floats per vertex
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"),
                new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color"));
        mesh.setVertices(array);
        MeshPart meshPart = createMeshPart("meshpart1", mesh, 0, localVerticesSize / 12, primitiveType);
        Material material = new Material(TextureAttribute.createDiffuse(checkerboardTexture));
        ColorAttribute diffuseColor = ColorAttribute.createDiffuse(1, 1, 1, 1);
        material.set(diffuseColor);
        Node node = new Node();
        node.id = "node1";
        node.parts.add(new NodePart(meshPart, material));
        Model model = createModel();
        model.nodes.add(node);
        model.meshes.add(mesh);
        model.materials.add(material);
        model.meshParts.add(meshPart);
        return model;
    }

    public void clear() {
        for(IntMap.Entry<Model> entry : modelBatch) {
            entry.value.dispose();
        }
        modelBatch.clear();
        modelInstancePool.clear();
        modelRendererList.clear();
    }

    public void begin(Camera camera) {
        batchBegin(camera);
    }

    public void begin(Viewport viewport) {
        begin(viewport.getCamera());
    }

    public void end() {
        for (RenderableProvider instance : modelRendererList) {
            batchRender(instance, environment);
        }
        batchEnd();
        modelInstancePool.freeAll(modelRendererList);
        modelRendererList.clear();
    }

    @Override
    public void onNativeDispose() {
        checkerboardTexture.dispose();
        clear();
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isEnable() {
        return enable;
    }

    @Override
    public void DrawBodies(PhysicsSystem system, BodyManagerDrawSettings inDrawSettings) {
        mDrawShapeWireframe = inDrawSettings.get_mDrawShapeWireframe();
        super.DrawBodies(system, inDrawSettings);
    }

    // Other overridden methods remain unchanged
    @Override
    public void DrawBodies(PhysicsSystem system) {
        super.DrawBodies(system);
    }

    @Override
    public void DrawCylinder(Mat44 inMatrix, float inHalfHeight, float inRadius, Color inColor) {
        if (mDrawShapeWireframe) {
            super.DrawCylinder(inMatrix, inHalfHeight, inRadius, inColor, ECastShadow.ECastShadow_Off, EDrawMode.EDrawMode_Wireframe);
        } else {
            super.DrawCylinder(inMatrix, inHalfHeight, inRadius, inColor);
        }
    }

    @Override
    public void DrawCylinder(Mat44 inMatrix, float inHalfHeight, float inRadius, Color inColor, ECastShadow inCastShadow) {
        if (mDrawShapeWireframe) {
            super.DrawCylinder(inMatrix, inHalfHeight, inRadius, inColor, ECastShadow.ECastShadow_Off, EDrawMode.EDrawMode_Wireframe);
        } else {
            super.DrawCylinder(inMatrix, inHalfHeight, inRadius, inColor, inCastShadow);
        }
    }

    @Override
    public void DrawCylinder(Mat44 inMatrix, float inHalfHeight, float inRadius, Color inColor, ECastShadow inCastShadow, EDrawMode inDrawMode) {
        if (mDrawShapeWireframe) {
            super.DrawCylinder(inMatrix, inHalfHeight, inRadius, inColor, ECastShadow.ECastShadow_Off, EDrawMode.EDrawMode_Wireframe);
        } else {
            super.DrawCylinder(inMatrix, inHalfHeight, inRadius, inColor, inCastShadow);
        }
    }

    class ModelRenderer implements RenderableProvider {
        private Model model;
        private final Matrix4 transform = new Matrix4();

        private ColorAttribute diffuseColor;
        private NodePart nodePart;
        private MeshPart meshPart;

        public void setModel(Model model, int primitiveType) {
            this.model = model;
            if(diffuseColor == null) {
                Material material = model.materials.get(0).copy();
                diffuseColor = (ColorAttribute)material.get(ColorAttribute.Diffuse);
                meshPart = createMeshPart();
                nodePart = new NodePart(meshPart, material);

            }
            Node node = model.nodes.get(0);
            NodePart modelNodePart = node.parts.get(0);
            MeshPart modelMeshPart = modelNodePart.meshPart;
            meshPart.set(modelMeshPart);
            meshPart.primitiveType = primitiveType;
            nodePart.invBoneBindTransforms = modelNodePart.invBoneBindTransforms;
            nodePart.bones = modelNodePart.bones;
        }

        @Override
        public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
            Renderable renderable = pool.obtain();
            Node node = model.nodes.get(0);
            nodePart.setRenderable(renderable);
            if (nodePart.bones == null && this.transform != null) {
                renderable.worldTransform.set(this.transform).mul(node.globalTransform);
            } else if (this.transform != null) {
                renderable.worldTransform.set(this.transform);
            } else {
                renderable.worldTransform.idt();
            }
            renderable.environment = environment;
            renderables.add(renderable);
        }
    }
}
