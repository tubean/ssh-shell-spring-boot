/*
 * Copyright (c) 2020 François Onimus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.fonimus.ssh.shell.commands;

import com.github.fonimus.ssh.shell.postprocess.PostProcessor;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.github.fonimus.ssh.shell.SshShellProperties.SSH_SHELL_PREFIX;

/**
 * Command to list available post processors
 */
@SshShellComponent
@ShellCommandGroup("Built-In Commands")
@ConditionalOnProperty(
        value = {
                SSH_SHELL_PREFIX + ".default-commands.postprocessors",
                SSH_SHELL_PREFIX + ".defaultCommands.postprocessors"
        }, havingValue = "true", matchIfMissing = true
)
public class Postprocessors {

    private List<PostProcessor> postProcessors;

    public Postprocessors(List<PostProcessor> postProcessors) {
        this.postProcessors = new ArrayList<>(postProcessors);
        this.postProcessors.sort(Comparator.comparing(PostProcessor::getName));
    }

    @ShellMethod(value = "Display the available post processors")
    public CharSequence postprocessors() {
        AttributedStringBuilder result = new AttributedStringBuilder();
        result.append("Available Post-Processors\n\n", AttributedStyle.BOLD);
        for (PostProcessor postProcessor : postProcessors) {
            result.append("\t" + postProcessor.getName() + ": ", AttributedStyle.BOLD);
            Class<?> cls =
                    ((Class) ((ParameterizedType) (postProcessor.getClass().getGenericInterfaces())[0]).getActualTypeArguments()[0]);
            result.append(cls.getName() + "\n", AttributedStyle.DEFAULT);
        }

        return result;
    }

}
