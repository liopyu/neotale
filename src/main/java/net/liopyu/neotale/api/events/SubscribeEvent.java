package net.liopyu.neotale.api.events;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {
    short priority() default 0;

    boolean global() default false;

    boolean unhandled() default false;

    boolean ecsAny() default true;
}
