package de.erethon.questsxl.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface QParamDoc {
    String name() default "";
    String description() default "";
    String def() default "";
    boolean required() default false;
}
