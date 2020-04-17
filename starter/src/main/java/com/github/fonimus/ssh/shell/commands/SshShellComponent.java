package com.github.fonimus.ssh.shell.commands;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.shell.standard.ShellComponent;

import java.lang.annotation.*;

import static com.github.fonimus.ssh.shell.SshShellProperties.SSH_SHELL_ENABLE;

/**
 * Conditional {@link org.springframework.shell.standard.ShellComponent}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@ShellComponent
@ConditionalOnProperty(name = SSH_SHELL_ENABLE, havingValue = "true", matchIfMissing = true)
public @interface SshShellComponent {

    /**
     * Used to indicate a suggestion for a logical name for the component.
     *
     * @return the suggested component name, if any
     */
    String value() default "";
}
