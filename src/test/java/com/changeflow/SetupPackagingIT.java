package com.changeflow;

import java.nio.file.Path;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SetupPackagingIT {

    @Test
    void executableJarContainsApplicationAndExcludesTestCode() throws Exception {
        try (var jar = new JarFile(Path.of("target", "change-flow-0.0.1-SNAPSHOT.jar").toFile())) {
            assertEquals("com.changeflow.ChangeFlowApplication",
                    jar.getManifest().getMainAttributes().getValue("Start-Class"));
            assertNotNull(jar.getEntry("BOOT-INF/classes/com/changeflow/ChangeFlowApplication.class"));
            assertNotNull(jar.getEntry("BOOT-INF/classes/application.yaml"));
            assertNull(jar.getEntry("BOOT-INF/classes/com/changeflow/SetupPackagingIT.class"));
            assertNull(jar.getEntry("BOOT-INF/classes/com/changeflow/DevApplication.class"));
            assertNull(jar.getEntry("BOOT-INF/classes/com/changeflow/DevServicesConfiguration.class"));
            assertNull(jar.getEntry("BOOT-INF/classes/application-dev.yaml"));
        }
    }
}
