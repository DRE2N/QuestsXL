package de.erethon.questsxl.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface QLoadableDoc {
    String value() default "";
    String description() default "";
    String shortExample() default "";
    String[] longExample() default {};
}
