package com.github.fonimus.ssh.shell;

import com.github.fonimus.ssh.shell.auth.SshShellAuthenticationProvider;
import com.github.fonimus.ssh.shell.auth.SshShellPasswordAuthenticationProvider;
import com.github.fonimus.ssh.shell.auth.SshShellSecurityAuthenticationProvider;
import com.github.fonimus.ssh.shell.postprocess.PostProcessor;
import com.github.fonimus.ssh.shell.postprocess.TypePostProcessorResultHandler;
import com.github.fonimus.ssh.shell.postprocess.provided.*;
import com.github.fonimus.ssh.shell.providers.AnyOsFileValueProvider;
import org.apache.sshd.server.SshServer;
import org.jline.reader.LineReader;
import org.jline.reader.Parser;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.shell.ResultHandler;
import org.springframework.shell.Shell;
import org.springframework.shell.SpringShellAutoConfiguration;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.shell.standard.ValueProvider;

import java.util.List;

import static com.github.fonimus.ssh.shell.SshShellCommandFactory.SSH_THREAD_CONTEXT;
import static com.github.fonimus.ssh.shell.SshShellProperties.SSH_SHELL_ENABLE;
import static com.github.fonimus.ssh.shell.SshShellProperties.SSH_SHELL_PREFIX;

/**
 * <p>Ssh shell auto configuration</p>
 * <p>Can be disabled by property <b>ssh.shell.enable=false</b></p>
 */
@Configuration
@ConditionalOnClass(SshServer.class)
@ConditionalOnProperty(name = SSH_SHELL_ENABLE, havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({SshShellProperties.class})
@AutoConfigureAfter(value = {
        SpringShellAutoConfiguration.class
}, name = {
        "org.springframework.boot.actuate.autoconfigure.audit.AuditEventsEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.condition.ConditionsReportEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.context.properties" +
                ".ConfigurationPropertiesReportEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.context.ShutdownEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.endpoint.jmx.JmxEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.flyway.FlywayEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.jolokia.JolokiaEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.liquibase.LiquibaseEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.logging.LogFileWebEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.logging.LoggersEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.management.HeapDumpWebEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.management.ThreadDumpEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.MetricsEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.scheduling.ScheduledTasksEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.session.SessionsEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.trace.http.HttpTraceEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.web.mappings.MappingsEndpointAutoConfiguration"
})
@ComponentScan(basePackages = {"com.github.fonimus.ssh.shell"})
public class SshShellAutoConfiguration {

    private static final String TERMINAL_DELEGATE = "terminalDelegate";

    public ApplicationContext context;

    private ConfigurableEnvironment environment;

    public SshShellAutoConfiguration(ApplicationContext context, ConfigurableEnvironment environment) {
        this.context = context;
        this.environment = environment;
    }

    @Bean
    @Primary
    public Shell sshShell(@Qualifier("main") ResultHandler<Object> resultHandler, List<PostProcessor> postProcessors) {
        return new ExtendedShell(new TypePostProcessorResultHandler(resultHandler, postProcessors));
    }

    // value providers

    @Bean
    public ValueProvider anyOsFileValueProvider(SshShellProperties properties) {
        return new AnyOsFileValueProvider(properties.isAnyOsFileProvider());
    }

    // post processors

    @Bean
    @ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
    public JsonPointerPostProcessor jsonPointerPostProcessor() {
        return new JsonPointerPostProcessor();
    }

    @Bean
    @ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
    public PrettyJsonPostProcessor prettyJsonPostProcessor() {
        return new PrettyJsonPostProcessor();
    }

    @Bean
    public SavePostProcessor savePostProcessor() {
        return new SavePostProcessor();
    }

    @Bean
    public GrepPostProcessor grepPostProcessor() {
        return new GrepPostProcessor();
    }

    @Bean
    public HighlightPostProcessor highlightPostProcessor() {
        return new HighlightPostProcessor();
    }

    @Bean
    public SshShellHelper sshShellHelper(SshShellProperties properties) {
        return new SshShellHelper(properties.getConfirmationWords());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.security.authentication.AuthenticationManager")
    @ConditionalOnProperty(value = SSH_SHELL_PREFIX + ".authentication", havingValue = "security")
    public SshShellAuthenticationProvider sshShellSecurityAuthenticationProvider(SshShellProperties properties) {
        return new SshShellSecurityAuthenticationProvider(context, properties.getAuthProviderBeanName());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = SSH_SHELL_PREFIX + ".authentication", havingValue = "simple", matchIfMissing = true)
    public SshShellAuthenticationProvider sshShellSimpleAuthenticationProvider(SshShellProperties properties) {
        return new SshShellPasswordAuthenticationProvider(properties.getUser(), properties.getPassword());
    }

    /**
     * Primary terminal which delegates with right session
     *
     * @param terminal   jline terminal
     * @param lineReader jline line reader
     * @param properties ssh shell properties
     * @return terminal
     */
    @Bean(TERMINAL_DELEGATE)
    @Primary
    public Terminal terminal(Terminal terminal, LineReader lineReader, SshShellProperties properties) {
        if (properties.getPrompt().getLocal().isEnable()) {
            // local prompt enable, add ssh context in main thread
            SSH_THREAD_CONTEXT.set(new SshContext(null, terminal, lineReader, null));
        } else {
            InteractiveShellApplicationRunner.disable(environment);
        }
        return new SshShellTerminalDelegate(terminal);
    }

    /**
     * Primary prompt provider
     *
     * @param properties ssh shell properties
     * @return prompt provider
     */
    @Bean
    @ConditionalOnMissingBean
    public PromptProvider sshPromptProvider(SshShellProperties properties) {
        return () -> new AttributedString(properties.getPrompt().getText(),
                AttributedStyle.DEFAULT.foreground(properties.getPrompt().getColor().toJlineAttributedStyle()));
    }

    /**
     * Primary shell application runner which answers true to {@link InteractiveShellApplicationRunner#isEnabled()}
     *
     * @param lineReader     line reader
     * @param promptProvider prompt provider
     * @param parser         parser
     * @param shell          spring shell
     * @param environment    spring environment
     * @return shell application runner
     */
    @Bean
    @Primary
    public InteractiveShellApplicationRunner sshInteractiveShellApplicationRunner(LineReader lineReader,
                                                                                  PromptProvider promptProvider,
                                                                                  Parser parser, Shell shell,
                                                                                  Environment environment) {
        return new InteractiveShellApplicationRunner(lineReader, promptProvider, parser, shell, environment) {

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public void run(ApplicationArguments args) {
                // do nothing
            }
        };
    }
}

