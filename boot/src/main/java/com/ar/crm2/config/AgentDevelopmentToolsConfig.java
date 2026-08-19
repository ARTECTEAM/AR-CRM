package com.ar.crm2.config;

import com.ar.crm2.adapter.out.ai.tool.SpringAiDevelopmentCrmTools;
import com.ar.crm2.application.columna.port.in.CreateColumnaUseCase;
import com.ar.crm2.application.columna.port.in.EditColumnaUseCase;
import com.ar.crm2.application.columna.port.in.GetAllColumnasUseCase;
import com.ar.crm2.application.columna.port.in.GetColumnaByIdUseCase;
import com.ar.crm2.application.ficha.port.in.CreateFichaUseCase;
import com.ar.crm2.application.ficha.port.in.EditFichaUseCase;
import com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase;
import com.ar.crm2.application.ficha.port.in.GetFichaByIdUseCase;
import com.ar.crm2.application.ficha.port.in.MoverColumnaFichaUseCase;
import com.ar.crm2.application.tablero.port.in.AsignarColumnaTableroUseCase;
import com.ar.crm2.application.tablero.port.in.CreateTableroUseCase;
import com.ar.crm2.application.tablero.port.in.EditTableroUseCase;
import com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase;
import com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase;
import com.ar.crm2.application.tablero.port.in.ReordenarColumnasUseCase;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/** Opt-in composition boundary for development-only unscoped CRM tools. */
@Configuration
public class AgentDevelopmentToolsConfig {

    @Bean
    AgentDevelopmentToolsEnvironmentGuard agentDevelopmentToolsEnvironmentGuard(Environment environment) {
        return new AgentDevelopmentToolsEnvironmentGuard(environment);
    }

    @Bean
    @ConditionalOnProperty(name = "crm2.agent.development-tools-enabled", havingValue = "true")
    SpringAiDevelopmentCrmTools springAiDevelopmentCrmTools(
            GetAllTablerosUseCase getAllTablerosUseCase,
            GetTableroByIdUseCase getTableroByIdUseCase,
            CreateTableroUseCase createTableroUseCase,
            EditTableroUseCase editTableroUseCase,
            AsignarColumnaTableroUseCase asignarColumnaTableroUseCase,
            ReordenarColumnasUseCase reordenarColumnasUseCase,
            GetAllColumnasUseCase getAllColumnasUseCase,
            GetColumnaByIdUseCase getColumnaByIdUseCase,
            CreateColumnaUseCase createColumnaUseCase,
            EditColumnaUseCase editColumnaUseCase,
            GetAllFichasUseCase getAllFichasUseCase,
            GetFichaByIdUseCase getFichaByIdUseCase,
            CreateFichaUseCase createFichaUseCase,
            EditFichaUseCase editFichaUseCase,
            MoverColumnaFichaUseCase moverColumnaFichaUseCase,
            AgentDevelopmentToolsEnvironmentGuard environmentGuard) {
        environmentGuard.requireAcceptedNonProductionProfile();
        return new SpringAiDevelopmentCrmTools(
                getAllTablerosUseCase, getTableroByIdUseCase, createTableroUseCase,
                editTableroUseCase, asignarColumnaTableroUseCase, reordenarColumnasUseCase,
                getAllColumnasUseCase, getColumnaByIdUseCase, createColumnaUseCase, editColumnaUseCase,
                getAllFichasUseCase, getFichaByIdUseCase, createFichaUseCase, editFichaUseCase,
                moverColumnaFichaUseCase);
    }
}

final class AgentDevelopmentToolsEnvironmentGuard {

    static final String CONFIGURATION_ERROR =
            "crm2.agent.development-tools-enabled=true requires only an explicitly recognized non-production Spring profile: noauth or test";
    private static final Set<String> ACCEPTED_PROFILES = Arrays.stream(NonProductionProfile.values())
            .map(NonProductionProfile::propertyValue)
            .collect(Collectors.toUnmodifiableSet());

    private final Set<String> activeProfiles;

    AgentDevelopmentToolsEnvironmentGuard(Environment environment) {
        this.activeProfiles = Set.of(environment.getActiveProfiles());
    }

    void requireAcceptedNonProductionProfile() {
        if (activeProfiles.size() != 1 || !ACCEPTED_PROFILES.contains(activeProfiles.iterator().next())) {
            throw new IllegalStateException(CONFIGURATION_ERROR);
        }
    }

    private enum NonProductionProfile {
        NOAUTH("noauth"),
        TEST("test");

        private final String propertyValue;

        NonProductionProfile(String propertyValue) {
            this.propertyValue = propertyValue;
        }

        String propertyValue() {
            return propertyValue;
        }
    }
}
