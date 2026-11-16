package com.changeflow;

import org.springframework.boot.SpringApplication;

/** Test-classpath entry point; never shipped in the executable JAR. */
public class DevApplication {
    public static void main(String[] args) {
        SpringApplication.from(ChangeFlowApplication::main).with(DevServicesConfiguration.class).run(args);
    }
}
