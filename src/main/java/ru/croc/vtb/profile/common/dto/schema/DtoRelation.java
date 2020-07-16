package ru.croc.vtb.profile.common.dto.schema;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD})
@Retention(RUNTIME)
public @interface DtoRelation {
    String relationFieldFrom() default "";
    String relationFieldTo() default "";
}
