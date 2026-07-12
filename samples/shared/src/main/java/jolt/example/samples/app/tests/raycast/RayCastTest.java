package jolt.example.samples.app.tests.raycast;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import jolt.example.graphics.GraphicManagerApi;
import jolt.example.samples.app.tests.shapes.BoxShapeTest;

public abstract class RayCastTest extends BoxShapeTest {

    protected ImmediateModeRenderer renderer;
    protected Vector3 start = new Vector3();
    protected Vector3 end = new Vector3();
    protected Array<RayCastData> rayCastUserData = new Array<RayCastData>();

    @Override
    public void initialize() {
        super.initialize();
        renderer = GraphicManagerApi.graphicApi.createImmediateModeRenderer();
    }


    @Override
    public void postPhysicsUpdate(boolean isPlaying, float deltaTime) {
        renderer.begin(camera.combined, ShapeRenderer.ShapeType.Line.getGlType());
        renderer.color(1, 0, 0, 1);
        renderer.vertex(start.x, start.y, start.z);
        renderer.vertex(end.x, end.y, end.z);

        for(int i = 0; i < rayCastUserData.size; i++) {
            RayCastData rayCastData = rayCastUserData.get(i);
            Vector3 hitPosition = rayCastData.hitPosition;
            renderer.color(1, 0, 0, 1);
            renderer.vertex(hitPosition.x, hitPosition.y, hitPosition.z);
            renderer.color(1, 0, 0, 1);
            renderer.vertex(hitPosition.x + 1, hitPosition.y, hitPosition.z);
            renderer.color(0, 1, 0, 1);
            renderer.vertex(hitPosition.x, hitPosition.y, hitPosition.z);
            renderer.color(0, 1, 0, 1);
            renderer.vertex(hitPosition.x, hitPosition.y + 1, hitPosition.z);
            renderer.color(0, 0, 1, 1);
            renderer.vertex(hitPosition.x, hitPosition.y, hitPosition.z);
            renderer.color(0, 0, 1, 1);
            renderer.vertex(hitPosition.x, hitPosition.y, hitPosition.z + 1);
        }
        renderer.end();
    }

    @Override
    public void renderUI(Batch batch, BitmapFont font) {
        int y = 200;
        font.draw(batch, "RayCast Size: " + rayCastUserData.size, 50, y);
        for(int i = 0; i < rayCastUserData.size; i++) {
            RayCastData rayCastData = rayCastUserData.get(i);
            y -= 20;
            font.draw(batch, "UserData[" + i + "]: " + rayCastData.userData, 100, y);
        }
    }
}