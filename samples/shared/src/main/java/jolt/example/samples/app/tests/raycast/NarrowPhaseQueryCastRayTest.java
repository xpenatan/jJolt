package jolt.example.samples.app.tests.raycast;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.collision.Ray;
import jolt.JoltNew;
import jolt.RRayCast;
import jolt.math.Vec3;
import jolt.physics.body.BodyID;
import jolt.physics.collision.ArrayRayCastResult;
import jolt.physics.collision.CastRayAllHitCollisionCollector;
import jolt.physics.collision.NarrowPhaseQuery;
import jolt.physics.collision.RayCastResult;
import jolt.physics.collision.RayCastSettings;

public class NarrowPhaseQueryCastRayTest extends RayCastTest {

    @Override
    public void prePhysicsUpdate(boolean isPlaying) {
        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            Ray pickRay = camera.getPickRay(Gdx.input.getX(), Gdx.input.getY());
            start.set(pickRay.origin);
            pickRay.getEndPoint(end, 1000);

            final Vec3 rayOrigin = JoltNew.Vec3(start.x, start.y, start.z);
            final Vec3 rayDirection = JoltNew.Vec3(end.x, end.y, end.z);

            final RRayCast ray = new RRayCast(rayOrigin, rayDirection);
            rayOrigin.dispose();
            rayDirection.dispose();
            final RayCastSettings settings = new RayCastSettings();
            final CastRayAllHitCollisionCollector collector = new CastRayAllHitCollisionCollector();
            NarrowPhaseQuery narrowPhaseQuery = mPhysicsSystem.GetNarrowPhaseQuery();

            narrowPhaseQuery.CastRay(ray, settings, collector);
            settings.dispose();
            collector.Sort();

            ArrayRayCastResult mHits = collector.get_mHits();
            int size = mHits.size();
            System.out.println("RayCast Hit: " + collector.HadHit() + " Size: " + size);
            rayCastUserData.clear();
            for(int i = 0; i < size; i++) {
                RayCastResult result = mHits.at(i);
                float mFraction = result.get_mFraction();
                Vec3 vec3 = ray.GetPointOnRay(mFraction);
                BodyID mBodyID = result.get_mBodyID();
                long userData = mBodyInterface.GetUserData(mBodyID);
                RayCastData rayCastData = new RayCastData();
                rayCastData.hitPosition.set(vec3.GetX(), vec3.GetY(), vec3.GetZ());
                rayCastData.userData = userData;
                rayCastUserData.add(rayCastData);
                System.out.println("UserData[" + i + "]: " + userData);
            }
            ray.dispose();
            collector.dispose();
        }
    }
}