package jolt.example.samples.app.tests;

import com.badlogic.gdx.utils.Array;

public class TestGroup {
    public String name;
    public Array<TestGroup> children = new Array<>();
    public Class<Test> testClass; // May be null if not child

    public void addChild(TestGroup child) {
        children.add(child);
    }

    public String getName() {
        return name;
    }
}
