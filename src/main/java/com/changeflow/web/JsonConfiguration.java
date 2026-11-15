package com.changeflow.web;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.*;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.*;
import tools.jackson.databind.type.LogicalType;

@Configuration(proxyBeanMethods=false)
public class JsonConfiguration {
    @Bean JsonMapperBuilderCustomizer strictApiJson() {
        return builder->builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .withCoercionConfig(LogicalType.Textual,config->{
                config.setCoercion(CoercionInputShape.Integer,CoercionAction.Fail);
                config.setCoercion(CoercionInputShape.Float,CoercionAction.Fail);
                config.setCoercion(CoercionInputShape.Boolean,CoercionAction.Fail);
            });
    }
}
