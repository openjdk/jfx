package test.javafx.scene.control;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import javafx.scene.control.BehaviorAspect;
import javafx.scene.control.BehaviorConfiguration;
import javafx.scene.control.BehaviorConfiguration.Builder;
import javafx.scene.control.Control;

public class BehaviorConfigurationTest {

    @Test
    void shouldRejectDivergentControllers() {
        BehaviorAspect<Control, A> aspectA = BehaviorAspect.builder(A.class, A::new).build();
        BehaviorAspect<Control, B> aspectB = BehaviorAspect.builder(B.class, B::new).build();
        BehaviorAspect<Control, B2> aspectB2 = BehaviorAspect.builder(B2.class, B2::new).build();
        BehaviorAspect<Control, C> aspectC = BehaviorAspect.builder(C.class, C::new).build();

        assertEquals(
            "class test.javafx.scene.control.BehaviorConfigurationTest$A subtypes are not all in same hierarchy: [class test.javafx.scene.control.BehaviorConfigurationTest$B, class test.javafx.scene.control.BehaviorConfigurationTest$B2]",
            assertThrows(IllegalStateException.class, () -> BehaviorConfiguration.builder()
                .add(aspectA)
                .add(aspectB)
                .add(aspectB2)  // B and B2 are both sub types of A, unclear then which type to use for A
                .build()
            ).getMessage()
        );

        assertEquals(
            "class test.javafx.scene.control.BehaviorConfigurationTest$A subtypes are not all in same hierarchy: [class test.javafx.scene.control.BehaviorConfigurationTest$B2, class test.javafx.scene.control.BehaviorConfigurationTest$C]",
            assertThrows(IllegalStateException.class, () -> BehaviorConfiguration.builder()
                .add(aspectC)
                .add(aspectA)
                .add(aspectB2)  // C and B2 are both sub types of A, unclear then which type to use for A
                .build()
            ).getMessage()
        );
    }

    @Test
    void shouldAllowDeepSingleInheritanceForControllers() {
        Function<Control, A> factoryA = A::new;
        Function<Control, B> factoryB = B::new;
        Function<Control, C> factoryC = C::new;
        Function<Control, X> factoryX = X::new;
        Function<Control, Y> factoryY = Y::new;
        Function<Control, Z> factoryZ = Z::new;

        BehaviorAspect<Control, A> aspect1 = BehaviorAspect.builder(A.class, factoryA).build();
        BehaviorAspect<Control, B> aspect2 = BehaviorAspect.builder(B.class, factoryB).build();
        BehaviorAspect<Control, C> aspect3 = BehaviorAspect.builder(C.class, factoryC).build();
        BehaviorAspect<Control, X> aspect4 = BehaviorAspect.builder(X.class, factoryX).build();
        BehaviorAspect<Control, Y> aspect5 = BehaviorAspect.builder(Y.class, factoryY).build();
        BehaviorAspect<Control, Z> aspect6 = BehaviorAspect.builder(Z.class, factoryZ).build();

        List<BehaviorConfiguration.Builder<Control>> builders = List.of(
            BehaviorConfiguration.builder().add(aspect1).add(aspect2).add(aspect3).add(aspect4).add(aspect5).add(aspect6),
            BehaviorConfiguration.builder().add(aspect6).add(aspect5).add(aspect4).add(aspect3).add(aspect2).add(aspect1),
            BehaviorConfiguration.builder().add(aspect5).add(aspect2).add(aspect6).add(aspect1).add(aspect4).add(aspect3)
        );

        for (Builder<Control> builder : builders) {
            BehaviorConfiguration<Control> configuration = assertDoesNotThrow(builder::build);

            assertEquals(factoryC, configuration.getFactory(A.class));
            assertEquals(factoryC, configuration.getFactory(B.class));
            assertEquals(factoryC, configuration.getFactory(C.class));
            assertEquals(factoryZ, configuration.getFactory(X.class));
            assertEquals(factoryZ, configuration.getFactory(Y.class));
            assertEquals(factoryZ, configuration.getFactory(Z.class));
        }
    }

    private static class A { A(@SuppressWarnings("unused") Control c) {}}
    private static class B extends A { B(Control c) { super(c); }}
    private static class B2 extends A { B2(Control c) { super(c); }}
    private static class C extends B { C(Control c) { super(c); }}

    private static class X { X(@SuppressWarnings("unused") Control c) {}}
    private static class Y extends X { Y(Control c) { super(c); }}
    private static class Z extends Y { Z(Control c) { super(c); }}
}
