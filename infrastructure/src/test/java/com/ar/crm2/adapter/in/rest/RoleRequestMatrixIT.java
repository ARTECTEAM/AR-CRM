package com.ar.crm2.adapter.in.rest;

import com.ar.crm2.adapter.in.rest.projection.CrmSensitiveFieldResponseAdvice;
import com.ar.crm2.adapter.out.persistence.ContactoRepositoryAdapter;
import com.ar.crm2.adapter.out.persistence.RoleManagerGovernance;
import com.ar.crm2.adapter.out.persistence.FichaRepositoryAdapter;
import com.ar.crm2.adapter.out.persistence.RolRepositoryAdapter;
import com.ar.crm2.adapter.out.persistence.TableroRepositoryAdapter;
import com.ar.crm2.adapter.out.persistence.TratoRepositoryAdapter;
import com.ar.crm2.adapter.out.persistence.entity.RolEntity;
import com.ar.crm2.adapter.out.persistence.entity.RolPermisoEmbeddable;
import com.ar.crm2.adapter.out.persistence.entity.ContactoEntity;
import com.ar.crm2.adapter.out.persistence.entity.TratoEntity;
import com.ar.crm2.adapter.out.persistence.entity.UsuarioEntity;
import com.ar.crm2.adapter.out.persistence.repository.RolRepository;
import com.ar.crm2.adapter.out.persistence.repository.ContactoRepository;
import com.ar.crm2.adapter.out.persistence.repository.ColumnaRepository;
import com.ar.crm2.adapter.out.persistence.repository.FichaRepository;
import com.ar.crm2.adapter.out.persistence.repository.TableroRepository;
import com.ar.crm2.adapter.out.persistence.repository.TratoRepository;
import com.ar.crm2.adapter.out.persistence.repository.UsuarioRepository;
import com.ar.crm2.adapter.out.security.CommercialResourceScopeAdapter;
import com.ar.crm2.adapter.out.persistence.mapper.TableroMapper;
import com.ar.crm2.application.rol.port.out.FindRolByIdPort;
import com.ar.crm2.application.contacto.port.in.CambiarEstadoContactoUseCase;
import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.contacto.port.in.DeleteContactoUseCase;
import com.ar.crm2.application.contacto.port.in.EditContactoUseCase;
import com.ar.crm2.application.contacto.port.in.GetContactoByIdUseCase;
import com.ar.crm2.application.contacto.service.CambiarEstadoContactoService;
import com.ar.crm2.application.contacto.service.CreateContactoService;
import com.ar.crm2.application.contacto.service.DeleteContactoService;
import com.ar.crm2.application.contacto.service.EditContactoService;
import com.ar.crm2.application.contacto.service.GetContactoByIdService;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.application.security.port.out.ResourceScopePort;
import com.ar.crm2.application.security.service.DefaultCrmAuthorization;
import com.ar.crm2.application.trato.port.in.CreateTratoUseCase;
import com.ar.crm2.application.trato.port.in.DeleteTratoUseCase;
import com.ar.crm2.application.trato.port.in.EditTratoUseCase;
import com.ar.crm2.application.trato.port.in.GetAllTratosUseCase;
import com.ar.crm2.application.trato.port.in.GetTratoByIdUseCase;
import com.ar.crm2.application.trato.service.EditTratoService;
import com.ar.crm2.application.trato.service.DeleteTratoService;
import com.ar.crm2.application.trato.service.CreateTratoService;
import com.ar.crm2.application.trato.service.GetAllTratosService;
import com.ar.crm2.application.trato.service.GetTratoByIdService;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.enums.EstadoTrato;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.enums.TipoContrato;
import com.ar.crm2.security.ActorContextFilterConfiguration;
import com.ar.crm2.security.ActorContextRequestAttributeFilter;
import com.ar.crm2.security.CorsConfig;
import com.ar.crm2.security.CurrentActorAdapter;
import com.ar.crm2.security.KeycloakJwtActorContextMapper;
import com.ar.crm2.security.KeycloakJwtAuthoritiesConverter;
import com.ar.crm2.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

/** Request-level role matrix using synthetic users and an isolated H2 database. */
@SpringBootTest(classes = RoleRequestMatrixIT.RoleRequestHarness.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:role-request-matrix;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
class RoleRequestMatrixIT {

    private static final String READER_SUBJECT = "matrix-reader";
    private static final String EDITOR_SUBJECT = "matrix-editor";
    private static final String NO_GRANTS_SUBJECT = "matrix-no-grants";
    private static final String INACTIVE_ROLE_SUBJECT = "matrix-inactive-role";
    private static final String INACTIVE_USER_SUBJECT = "matrix-inactive-user";
    private static final String CLAIM_ADMIN_SUBJECT = "matrix-token-claim-admin";

    private static final UUID READER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID EDITOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID NO_GRANTS_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID INACTIVE_ROLE_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final UUID INACTIVE_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000005");
    private static final UUID TOKEN_CLAIM_ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000006");

    private static final UUID READER_DEAL_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID EDITOR_DEAL_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_OWNER_DEAL_ID = UUID.fromString("20000000-0000-0000-0000-000000000003");
    private static final UUID SHARED_CONTACT_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_CONTACT_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID COMPANY_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");
    private static final BigDecimal ORIGINAL_VALUE = new BigDecimal("125000.00");

    @Autowired private MockMvc mockMvc;
    @Autowired private RolRepository rolRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ContactoRepository contactoRepository;
    @Autowired private TratoRepository tratoRepository;

    @BeforeEach
    void seedSyntheticRolesUsersAndDeals() {
        tratoRepository.deleteAll();
        contactoRepository.deleteAll();
        usuarioRepository.deleteAll();
        rolRepository.deleteAll();

        UUID readerRoleId = UUID.fromString("40000000-0000-0000-0000-000000000001");
        UUID editorRoleId = UUID.fromString("40000000-0000-0000-0000-000000000002");
        UUID noGrantsRoleId = UUID.fromString("40000000-0000-0000-0000-000000000003");
        UUID inactiveRoleId = UUID.fromString("40000000-0000-0000-0000-000000000004");
        UUID claimAdminRoleId = UUID.fromString("40000000-0000-0000-0000-000000000005");

        rolRepository.save(role(readerRoleId, "Synthetic reader", true,
                grant(RecursoCrm.TRATO, "LEER", AlcanceCrm.PROPIOS_O_ASIGNADOS),
                grant(RecursoCrm.CONTACTO, "LEER", AlcanceCrm.TODO_COMPARTIDO),
                grant(RecursoCrm.EMPRESA, "LEER", AlcanceCrm.TODO_COMPARTIDO)));
        rolRepository.save(role(editorRoleId, "Synthetic editor", true,
                grant(RecursoCrm.TRATO, "LEER,ACTUALIZAR", AlcanceCrm.PROPIOS_O_ASIGNADOS),
                grant(RecursoCrm.CONTACTO, "LEER", AlcanceCrm.TODO_COMPARTIDO),
                grant(RecursoCrm.EMPRESA, "LEER", AlcanceCrm.TODO_COMPARTIDO)));
        rolRepository.save(role(noGrantsRoleId, "Synthetic no-grants", true));
        rolRepository.save(role(inactiveRoleId, "Synthetic inactive", false,
                grant(RecursoCrm.TRATO, "LEER", AlcanceCrm.TODO_COMPARTIDO),
                grant(RecursoCrm.CONTACTO, "LEER", AlcanceCrm.TODO_COMPARTIDO),
                grant(RecursoCrm.EMPRESA, "LEER", AlcanceCrm.TODO_COMPARTIDO)));
        rolRepository.save(role(claimAdminRoleId, "Synthetic token-claim target", true,
                grant(RecursoCrm.TRATO, "LEER", AlcanceCrm.TODO_COMPARTIDO),
                grant(RecursoCrm.CONTACTO, "LEER", AlcanceCrm.TODO_COMPARTIDO),
                grant(RecursoCrm.EMPRESA, "LEER", AlcanceCrm.TODO_COMPARTIDO)));

        usuarioRepository.save(user(READER_ID, READER_SUBJECT, readerRoleId, true));
        usuarioRepository.save(user(EDITOR_ID, EDITOR_SUBJECT, editorRoleId, true));
        usuarioRepository.save(user(NO_GRANTS_ID, NO_GRANTS_SUBJECT, noGrantsRoleId, true));
        usuarioRepository.save(user(INACTIVE_ROLE_USER_ID, INACTIVE_ROLE_SUBJECT, inactiveRoleId, true));
        usuarioRepository.save(user(INACTIVE_USER_ID, INACTIVE_USER_SUBJECT, readerRoleId, false));
        usuarioRepository.save(user(TOKEN_CLAIM_ADMIN_ID, CLAIM_ADMIN_SUBJECT, claimAdminRoleId, true));

        contactoRepository.save(contact(SHARED_CONTACT_ID, "Shared synthetic contact"));
        tratoRepository.save(deal(READER_DEAL_ID, READER_ID, SHARED_CONTACT_ID, "Reader deal"));
        tratoRepository.save(deal(EDITOR_DEAL_ID, EDITOR_ID, SHARED_CONTACT_ID, "Editor deal"));
        tratoRepository.save(deal(OTHER_OWNER_DEAL_ID, UUID.fromString("50000000-0000-0000-0000-000000000001"),
                OTHER_CONTACT_ID, "Other owner's deal"));
    }

    @Test
    void reader_canReadOwnedDeal_butCannotReadForeignDealOrSeeFinancialFields() throws Exception {
        mockMvc.perform(get("/api/tratos/get-by-id")
                        .param("id", READER_DEAL_ID.toString())
                        .header("Authorization", bearer(READER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Reader deal"))
                .andExpect(jsonPath("$.valorEstimado").value(nullValue()))
                .andExpect(jsonPath("$.probabilidad").value(nullValue()));

        // Token claims deliberately point at a broader local role; business access must still
        // follow the active role resolved from the JWT subject in the local CRM database.
        mockMvc.perform(get("/api/tratos/get-by-id")
                        .param("id", OTHER_OWNER_DEAL_ID.toString())
                        .header("Authorization", bearer(READER_SUBJECT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reader_canReadContact_butPrivateContactFieldsAreRedacted() throws Exception {
        mockMvc.perform(get("/api/contactos/get-by-id")
                        .param("id", SHARED_CONTACT_ID.toString())
                        .header("Authorization", bearer(READER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Shared synthetic contact"))
                .andExpect(jsonPath("$.correo").value(nullValue()))
                .andExpect(jsonPath("$.telefono").value(nullValue()));
    }

    @Test
    void editor_canUpdateOwnedNonSensitiveFields_butCannotWriteFinancialFields() throws Exception {
        mockMvc.perform(put("/api/tratos/edit")
                        .param("id", EDITOR_DEAL_ID.toString())
                        .header("Authorization", bearer(EDITOR_SUBJECT))
                        .contentType("application/json")
                        .content(editBody(EDITOR_ID, "Updated by editor", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Updated by editor"))
                .andExpect(jsonPath("$.valorEstimado").value(nullValue()));

        mockMvc.perform(put("/api/tratos/edit")
                        .param("id", EDITOR_DEAL_ID.toString())
                        .header("Authorization", bearer(EDITOR_SUBJECT))
                        .contentType("application/json")
                        .content(editBody(EDITOR_ID, "Must not persist", "999999.99")))
                .andExpect(status().isForbidden());

        TratoEntity persisted = tratoRepository.findById(EDITOR_DEAL_ID.toString()).orElseThrow();
        assertThat(persisted.getNombre()).isEqualTo("Updated by editor");
        assertThat(persisted.getValorEstimado()).isEqualByComparingTo(ORIGINAL_VALUE);
    }

    @Test
    void reader_cannotDeleteOwnedDeal_andDeniedMutationDoesNotChangePersistence() throws Exception {
        mockMvc.perform(delete("/api/tratos/delete")
                        .param("id", READER_DEAL_ID.toString())
                        .header("Authorization", bearer(READER_SUBJECT)))
                .andExpect(status().isForbidden());

        assertThat(tratoRepository.findById(READER_DEAL_ID.toString())).isPresent();
    }

    @Test
    void noGrantInactiveRoleAndInactiveUser_areDeniedEvenWithAuthenticatedJwt() throws Exception {
        for (String subject : List.of(NO_GRANTS_SUBJECT, INACTIVE_ROLE_SUBJECT, INACTIVE_USER_SUBJECT)) {
            mockMvc.perform(get("/api/tratos/get-by-id")
                            .param("id", READER_DEAL_ID.toString())
                            .header("Authorization", bearer(subject)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void missingJwt_isRejectedBeforeAnyBusinessRequest() throws Exception {
        mockMvc.perform(get("/api/tratos/get-by-id").param("id", READER_DEAL_ID.toString()))
                .andExpect(status().isUnauthorized());
    }

    private static String editBody(UUID responsibleId, String name, String financialValue) {
        return "{" +
                "\"responsableId\":\"" + responsibleId + "\"," +
                "\"nombre\":\"" + name + "\"," +
                "\"valorEstimado\":" + (financialValue == null ? "null" : financialValue) + "," +
                "\"probabilidad\":null," +
                "\"fechaCierreEsperada\":null," +
                "\"tipoContrato\":\"SERVICIO\"" +
                "}";
    }

    private static String bearer(String subject) {
        return "Bearer " + subject;
    }

    private static RolEntity role(UUID id, String name, boolean active, PermissionGrant... grants) {
        Map<String, RolPermisoEmbeddable> permissions = new HashMap<>();
        for (PermissionGrant grant : grants) {
            permissions.put(grant.resource().name(), grant.permission());
        }
        return RolEntity.builder()
                .id(id.toString())
                .nombre(name)
                .descripcion("Synthetic role fixture")
                .activo(active)
                .permisos(permissions)
                .build();
    }

    private static PermissionGrant grant(RecursoCrm resource, String actions, AlcanceCrm scope) {
        return new PermissionGrant(resource, new RolPermisoEmbeddable(actions, scope.name(), "", "", ""));
    }

    private static UsuarioEntity user(UUID id, String subject, UUID roleId, boolean active) {
        return UsuarioEntity.builder()
                .id(id.toString())
                .nombre("Synthetic " + subject)
                .correo(subject + "@example.invalid")
                .rolId(roleId.toString())
                .creadoEn(LocalDateTime.now())
                .activo(active)
                .keycloakId(subject)
                .build();
    }

    private static TratoEntity deal(UUID id, UUID responsibleId, UUID contactId, String name) {
        return TratoEntity.builder()
                .id(id.toString())
                .contactoId(contactId.toString())
                .responsableId(responsibleId.toString())
                .nombre(name)
                .valorEstimado(ORIGINAL_VALUE)
                .probabilidad(65)
                .fechaCierreEsperada(LocalDate.now().plusDays(30))
                .tipoContrato(TipoContrato.SERVICIO)
                .estado(EstadoTrato.ABIERTO)
                .creadoEn(LocalDateTime.now())
                .actualizadoEn(null)
                .build();
    }

    private static ContactoEntity contact(UUID id, String name) {
        return ContactoEntity.builder()
                .id(id.toString())
                .empresaId(COMPANY_ID.toString())
                .responsableId(READER_ID.toString())
                .creadoPor(READER_ID.toString())
                .nombre(name)
                .correo("private@example.invalid")
                .telefono("555-0101")
                .cargo("Synthetic contact")
                .comoNosConocio("Integration test")
                .estadoRelacion(EstadoRelacion.ACTIVO)
                .creadoEn(LocalDateTime.now())
                .actualizadoEn(null)
                .build();
    }

    private record PermissionGrant(RecursoCrm resource, RolPermisoEmbeddable permission) {}

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ar.crm2.adapter.out.persistence.entity")
    @EnableJpaRepositories(basePackages = "com.ar.crm2.adapter.out.persistence.repository")
    @Import({
            TratoController.class,
            ContactoController.class,
            GlobalExceptionHandler.class,
            SecurityConfig.class,
            CorsConfig.class,
            ActorContextFilterConfiguration.class,
            ActorContextRequestAttributeFilter.class,
            KeycloakJwtActorContextMapper.class,
            KeycloakJwtAuthoritiesConverter.class,
            CurrentActorAdapter.class,
            RoleManagerGovernance.class,
            CommercialResourceScopeAdapter.class,
            CrmSensitiveFieldResponseAdvice.class,
            HarnessBeans.class
    })
    static class RoleRequestHarness {}

    @Configuration
    static class HarnessBeans {
        @Bean
        JwtDecoder jwtDecoder() {
            UUID tokenClaimUserId = TOKEN_CLAIM_ADMIN_ID;
            return token -> Jwt.withTokenValue(token)
                    .headers(headers -> headers.put("alg", "RS256"))
                    .claims(claims -> {
                        claims.put("sub", token);
                        claims.put("usuario_id", tokenClaimUserId.toString());
                        claims.put("realm_access", Map.of("roles", List.of("SUPER_USUARIO")));
                    })
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .issuer("http://localhost/test")
                    .build();
        }

        @Bean
        TratoRepositoryAdapter tratoRepositoryAdapter(TratoRepository repository) {
            return new TratoRepositoryAdapter(repository);
        }

        @Bean
        ContactoRepositoryAdapter contactoRepositoryAdapter(ContactoRepository repository) {
            return new ContactoRepositoryAdapter(repository);
        }

        @Bean
        FichaRepositoryAdapter fichaRepositoryAdapter(FichaRepository repository) {
            return new FichaRepositoryAdapter(repository);
        }

        @Bean
        TableroRepositoryAdapter tableroRepositoryAdapter(TableroRepository repository,
                                                          ColumnaRepository columnaRepository) {
            return new TableroRepositoryAdapter(repository, columnaRepository,
                    new TableroMapper(columnaRepository));
        }

        @Bean
        RolRepositoryAdapter rolRepositoryAdapter(RolRepository repository,
                                                  RoleManagerGovernance governance) {
            return new RolRepositoryAdapter(repository, governance);
        }

        @Bean
        CrmAuthorization crmAuthorization(CurrentActorPort currentActorPort,
                                          FindRolByIdPort findRolByIdPort,
                                          List<ResourceScopePort> scopePorts) {
            return new DefaultCrmAuthorization(currentActorPort, findRolByIdPort, scopePorts);
        }

        @Bean
        GetTratoByIdUseCase getTratoByIdUseCase(CrmAuthorization authorization,
                                                TratoRepositoryAdapter repository) {
            return new GetTratoByIdService(authorization, repository);
        }

        @Bean
        GetContactoByIdUseCase getContactoByIdUseCase(ContactoRepositoryAdapter repository,
                                                      CrmAuthorization authorization) {
            return new GetContactoByIdService(repository, authorization);
        }

        @Bean
        CreateContactoUseCase createContactoUseCase(ContactoRepositoryAdapter repository,
                                                    CrmAuthorization authorization,
                                                    CurrentActorPort currentActorPort) {
            return new CreateContactoService(repository, authorization, currentActorPort);
        }

        @Bean
        EditContactoUseCase editContactoUseCase(ContactoRepositoryAdapter repository,
                                                CrmAuthorization authorization) {
            return new EditContactoService(repository, repository, authorization);
        }

        @Bean
        DeleteContactoUseCase deleteContactoUseCase(ContactoRepositoryAdapter repository,
                                                     CrmAuthorization authorization) {
            return new DeleteContactoService(repository, repository, repository, authorization);
        }

        @Bean
        CambiarEstadoContactoUseCase cambiarEstadoContactoUseCase(ContactoRepositoryAdapter repository,
                                                                  CrmAuthorization authorization) {
            return new CambiarEstadoContactoService(repository, repository, repository, authorization);
        }

        @Bean
        EditTratoUseCase editTratoUseCase(CrmAuthorization authorization,
                                          TratoRepositoryAdapter repository) {
            return new EditTratoService(authorization, repository, repository);
        }

        @Bean
        DeleteTratoUseCase deleteTratoUseCase(CrmAuthorization authorization,
                                              TratoRepositoryAdapter repository) {
            return new DeleteTratoService(authorization, repository, repository);
        }

        @Bean
        CreateTratoUseCase createTratoUseCase(CrmAuthorization crmAuthorization,
                                             TratoRepositoryAdapter tratoRepository,
                                             FichaRepositoryAdapter fichaRepository,
                                             TableroRepositoryAdapter tableroRepository) {
            return new CreateTratoService(crmAuthorization, tratoRepository, fichaRepository, tableroRepository);
        }

        @Bean
        GetAllTratosUseCase getAllTratosUseCase(CrmAuthorization crmAuthorization,
                                                TratoRepositoryAdapter tratoRepository) {
            return new GetAllTratosService(crmAuthorization, tratoRepository);
        }

    }
}
