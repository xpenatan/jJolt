package jolt.web.teavm;

import org.teavm.extension.spi.substitution.SubstitutionPolicy;
import org.teavm.extension.spi.substitution.SubstitutionSink;

public class JoltTeaVMSubstitutionPolicy implements SubstitutionPolicy {
    @Override
    public void contribute(SubstitutionSink sink) {
        sink.substitutePackage("jolt", "gen.web.jolt");
    }
}
