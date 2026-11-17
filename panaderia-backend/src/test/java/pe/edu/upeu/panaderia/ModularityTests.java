package pe.edu.upeu.panaderia;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @Test
    void shouldVerifyModularStructure() {
        ApplicationModules.of(PanaderiaBackendApplication.class).verify();
    }
}