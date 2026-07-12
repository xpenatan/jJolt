package jolt.example.samples.app.tests;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import jolt.example.graphics.GraphicManagerApi;
import jolt.example.graphics.GraphicType;
import jolt.example.samples.app.tests.character.CharacterSpaceShipTest;
import jolt.example.samples.app.tests.character.CharacterTest;
import jolt.example.samples.app.tests.playground.box.BoxSpawnTest;
import jolt.example.samples.app.tests.raycast.NarrowPhaseQueryCastRayTest;
import jolt.example.samples.app.tests.shapes.BoxShapeTest;
import jolt.example.samples.app.tests.vehicle.TankTest;
import jolt.example.samples.app.tests.vehicle.VehicleConstraintTest;

public class Tests {
    private Array<Tests.JoltTestInstancer> tests;

    public Tests() {
        tests = Tests.setupDesktop();
    }

    private Tests(Array<JoltTestInstancer> tests) {
        this.tests = tests;
    }

    public static Tests createWebSafe() {
        return new Tests(setupWebSafe());
    }

    private static Array<JoltTestInstancer> setupWebSafe() {
        Array<JoltTestInstancer> tests = new Array<>();

        //Jolt tests
        tests.add(new JoltTestInstancer(NarrowPhaseQueryCastRayTest.class, NarrowPhaseQueryCastRayTest::new, "CastRay", "NarrowPhase CastRay"));
        tests.add(new JoltTestInstancer(BoxShapeTest.class, BoxShapeTest::new, "Shapes", "Box Shape"));
        tests.add(new JoltTestInstancer(VehicleConstraintTest.class, VehicleConstraintTest::new, "Vehicle", "Car (VehicleConstraint)"));
        tests.add(new JoltTestInstancer(TankTest.class, TankTest::new, "Vehicle", "Tank (VehicleConstraint)"));
        tests.add(new JoltTestInstancer(CharacterSpaceShipTest.class, CharacterSpaceShipTest::new, "Character", "Character Virtual vs Space Ship"));
        return tests;
    }

    private static Array<JoltTestInstancer> setupDesktop() {
        Array<JoltTestInstancer> tests = setupWebSafe();
        if(Gdx.app.getType() != Application.ApplicationType.WebGL) {
            // FIXME not working on the web
            tests.add(new JoltTestInstancer(CharacterTest.class, CharacterTest::new, "Character", "Character Test"));
        }

        if(GraphicManagerApi.graphicApi.getGraphicType() == GraphicType.OpenGL) {
            // BoxSpawnTest not supported in WGPU

            //Custom tests
            tests.add(new JoltTestInstancer(BoxSpawnTest.class, BoxSpawnTest::new, "Playground", "BoxTest"));
        }
        return tests;
    }

    public Test getTest(Class type) {
        for(JoltTestInstancer test : tests) {
            if(test.type == type) {
                return test.newInstance();
            }
        }
        return null;
    }

    public Class getNextTest(Class curTest) {
        Class firstTest = null;
        Class nextTest = null;
        boolean getNext = false;
        for(JoltTestInstancer test : tests) {
            if(firstTest == null) {
                firstTest = test.type;
            }
            if(!getNext && test.type == curTest) {
                getNext = true;
            }
            else if(getNext) {
                nextTest = test.type;
                break;
            }
        }
        if(nextTest == null) {
            nextTest = firstTest;
        }
        return nextTest;
    }

    public TestGroup getAllTests() {
        TestGroup root = new TestGroup();
        root.name = "Samples";
        Array<TestGroup> allTests = root.children;
        for(JoltTestInstancer testInstancer : tests) {
            TestGroup parent = null;
            int length = testInstancer.groups.length;
            for(int i = 0; i < length; i++) {
                boolean isLast = length-1 == i;
                String group = testInstancer.groups[i];
                if(parent == null) {
                    parent = findGroup(group, allTests);
                    if(parent == null) {
                        parent = new TestGroup();
                        parent.name = group;
                        allTests.add(parent);
                    }
                }
                else {
                    TestGroup child = findGroup(group, parent.children);
                    if(child == null) {
                        child = new TestGroup();
                        child.name = group;
                        parent.addChild(child);
                    }
                    parent = child;
                }

                if(isLast) {
                    parent.testClass = testInstancer.type;
                }
            }
        }
        return root;
    }

    static TestGroup findGroup(String name, Array<TestGroup> allTests) {
        for(TestGroup testGroup : allTests) {
            String groupName = testGroup.getName();

            if(groupName.equals(name)) {
                return testGroup;
            }
        }
        return null;
    }

    public static class JoltTestInstancer {
        private Class<Test> type;
        private NewTest<?> instancer;
        String [] groups;

         public <T extends Test> JoltTestInstancer(Class<T> type, NewTest<T> instancer, String ... groups) {
            this.type = (Class<Test>)type;
            this.instancer = instancer;
            this.groups = groups;
        }

        public Test newInstance() {
            return (Test)instancer.newTest();
        }

        public String getName() {
            return type.getSimpleName();
        }
    }

    @FunctionalInterface
    private interface NewTest<T> {
        T newTest();
    }
}
