package com.ar.crm2.config;

import com.ar.crm2.adapter.out.persistence.agent.AgentTurnAdapter;
import com.ar.crm2.adapter.out.persistence.agent.memory.DurableMemoryPersistenceAdapter;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentConversationRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentTurnRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentTurnRequestRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentVisibleHistoryRepository;
import com.ar.crm2.adapter.out.persistence.agent.memory.DurableMemoryRepository;
import com.ar.crm2.adapter.out.persistence.agent.tool.AgentToolActionPersistenceAdapter;
import com.ar.crm2.adapter.out.persistence.agent.tool.AgentToolActionRepository;
import com.ar.crm2.application.agent.memory.port.in.DeleteDurableMemoryUseCase;
import com.ar.crm2.application.agent.memory.port.in.PurgeDurableMemoriesUseCase;
import com.ar.crm2.application.agent.memory.port.in.RecallDurableMemoriesUseCase;
import com.ar.crm2.application.agent.memory.port.in.RememberDurableMemoryUseCase;
import com.ar.crm2.application.agent.memory.port.in.ReplaceDurableMemoryUseCase;
import com.ar.crm2.application.agent.memory.port.out.DeleteDurableMemoryPort;
import com.ar.crm2.application.agent.memory.port.out.FindEligibleDurableMemoriesPort;
import com.ar.crm2.application.agent.memory.port.out.PurgeDurableMemoriesPort;
import com.ar.crm2.application.agent.memory.port.out.ReplaceDurableMemoryPort;
import com.ar.crm2.application.agent.memory.port.out.SaveDurableMemoryPort;
import com.ar.crm2.application.agent.tool.port.out.FindAgentToolActionByIdPort;
import com.ar.crm2.application.agent.tool.port.out.MarkAgentToolActionCompletedPort;
import com.ar.crm2.application.agent.tool.port.out.SaveAgentToolActionPort;
import com.ar.crm2.application.agent.turn.port.in.CompleteUserTurnUseCase;
import com.ar.crm2.application.agent.turn.port.in.CreateUserTurnUseCase;
import com.ar.crm2.application.agent.turn.port.out.ChatCompletionPort;
import com.ar.crm2.application.agent.turn.port.out.CompletePreparedTurnPort;
import com.ar.crm2.application.agent.turn.port.out.CreateUserTurnPort;
import com.ar.crm2.application.agent.turn.port.out.FindCompletedAssistantContentPort;
import com.ar.crm2.application.agent.turn.port.out.FindCompletedVisibleHistoryPort;
import com.ar.crm2.application.agent.turn.service.CompleteUserTurnService;
import com.ar.crm2.application.agent.turn.service.CreateUserTurnService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Wiring-only proof for the final conversational composition (task 5.x).
 *
 * <p>Loads only {@code WiringConfig} — the boot composition root — and
 * verifies that the Agent conversation's Application services,
 * infrastructure adapters, and outbound ports are wired together
 * exactly the way the design requires:
 * <ul>
 *   <li>The single {@code AgentTurnAdapter} bean implements the four
 *       turn outbound ports ({@code CreateUserTurnPort},
 *       {@code FindCompletedAssistantContentPort},
 *       {@code FindCompletedVisibleHistoryPort},
 *       {@code CompletePreparedTurnPort}).</li>
 *   <li>The single {@code DurableMemoryPersistenceAdapter} bean
 *       implements every durable-memory port, including the
 *       {@code turn}-level {@code FindEligibleDurableMemoriesPort} that
 *       feeds bounded history/memory into chat completion.</li>
 *   <li>The single {@code AgentToolActionPersistenceAdapter} bean
 *       implements the three tool-ledger ports and is constructed with
 *       a {@link PlatformTransactionManager} and a {@link Clock}.</li>
 *   <li>{@code CreateUserTurnService} and {@code CompleteUserTurnService}
 *       are wired under the Application ports
 *       {@code CreateUserTurnUseCase} and {@code CompleteUserTurnUseCase},
 *       with the chat-completion port coming from the existing
 *       {@link com.ar.crm2.adapter.out.ai.SpringAiChatCompletionAdapter}.</li>
 *   <li>Every durable-memory Application service
 *       (recall/remember/replace/delete/purge) is wired under its use
 *       case interface.</li>
 * </ul>
 *
 * <p>This test loads {@code WiringConfig} with a focused
 * {@link TestConfiguration} that supplies the non-wired collaborators
 * needed by the composition (the {@link ChatClient} seam consumed by
 * the chat-completion adapter, the platform transaction manager
 * required by the tool ledger, the {@code Clock} for deterministic
 * timestamps, and the existing {@code ObjectMapper} bean). It mirrors
 * the no-network, focused pattern used by {@code FichaWiringTest}.
 */
@SpringJUnitConfig(classes = {WiringConfig.class, AgentConversationWiringTest.AgentWiringHarness.class})
class AgentConversationWiringTest {

    @Autowired private AgentTurnAdapter agentTurnAdapter;
    @Autowired private DurableMemoryPersistenceAdapter durableMemoryAdapter;
    @Autowired private AgentToolActionPersistenceAdapter agentToolActionAdapter;
    @Autowired private CreateUserTurnUseCase createUserTurnUseCase;
    @Autowired private CompleteUserTurnUseCase completeUserTurnUseCase;
    @Autowired private RecallDurableMemoriesUseCase recallDurableMemoriesUseCase;
    @Autowired private RememberDurableMemoryUseCase rememberDurableMemoryUseCase;
    @Autowired private ReplaceDurableMemoryUseCase replaceDurableMemoryUseCase;
    @Autowired private DeleteDurableMemoryUseCase deleteDurableMemoryUseCase;
    @Autowired private PurgeDurableMemoriesUseCase purgeDurableMemoriesUseCase;
    @Autowired private ApplicationContext applicationContext;

    /**
     * Reflectively reads the private {@code findEligibleDurableMemoriesPort}
     * field from {@code CompleteUserTurnService} so we can assert the
     * wiring actually injected the turn-level port — the type used to
     * pull eligible memories into chat completion.
     */
    @Test
    void completeUserTurnService_receivesTurnLevelFindEligibleDurableMemoriesPortFromWiring()
            throws Exception {
        Field field = CompleteUserTurnService.class.getDeclaredField("findEligibleDurableMemoriesPort");
        field.setAccessible(true);
        Object injected = field.get(completeUserTurnUseCase);
        assertThat(injected)
                .as("CompleteUserTurnService.findEligibleDurableMemoriesPort must be the turn-level port")
                .isInstanceOf(com.ar.crm2.application.agent.turn.port.out.FindEligibleDurableMemoriesPort.class)
                .isSameAs(durableMemoryAdapter);
    }

    /**
     * Reflectively proves that the wiring actually injected the
     * configured {@link ChatCompletionPort} (the existing Spring AI
     * adapter) into the completion service, not a fresh instance or a
     * {@code null} placeholder. Without this check, the test only proves
     * that {@code ChatCompletionPort} exists in the context; with it,
     * the test proves the composition root chose the bean.
     */
    @Test
    void completeUserTurnService_receivesTheConfiguredChatCompletionPort() throws Exception {
        Field field = CompleteUserTurnService.class.getDeclaredField("chatCompletionPort");
        field.setAccessible(true);
        Object injected = field.get(completeUserTurnUseCase);
        assertThat(injected)
                .as("CompleteUserTurnService must receive the ChatCompletionPort "
                        + "composed from the configured Spring AI ChatClient")
                .isSameAs(applicationContext().getBean(ChatCompletionPort.class));
    }

    /**
     * The {@code AgentTurnAdapter} bean is the single implementation
     * of all four turn outbound ports. This single test guards the
     * one-adapter-per-aggregate convention after the final composition.
     */
    @Test
    void agentTurnAdapter_implementsEveryTurnOutboundPort() {
        assertThat(agentTurnAdapter)
                .as("AgentTurnAdapter must implement CreateUserTurnPort")
                .isInstanceOf(CreateUserTurnPort.class);
        assertThat(agentTurnAdapter)
                .as("AgentTurnAdapter must implement FindCompletedAssistantContentPort")
                .isInstanceOf(FindCompletedAssistantContentPort.class);
        assertThat(agentTurnAdapter)
                .as("AgentTurnAdapter must implement FindCompletedVisibleHistoryPort")
                .isInstanceOf(FindCompletedVisibleHistoryPort.class);
        assertThat(agentTurnAdapter)
                .as("AgentTurnAdapter must implement CompletePreparedTurnPort")
                .isInstanceOf(CompletePreparedTurnPort.class);
    }

    /**
     * The {@code DurableMemoryPersistenceAdapter} bean is the single
     * implementation of every durable-memory port, including the
     * turn-level eligible-memory port. This is the cross-cutting
     * adapter the composition root must wire exactly once.
     */
    @Test
    void durableMemoryPersistenceAdapter_implementsEveryDurableMemoryPort() {
        assertThat(durableMemoryAdapter)
                .as("DurableMemoryPersistenceAdapter must implement SaveDurableMemoryPort")
                .isInstanceOf(SaveDurableMemoryPort.class);
        assertThat(durableMemoryAdapter)
                .as("DurableMemoryPersistenceAdapter must implement the durable-memory FindEligible port")
                .isInstanceOf(FindEligibleDurableMemoriesPort.class);
        assertThat(durableMemoryAdapter)
                .as("DurableMemoryPersistenceAdapter must implement ReplaceDurableMemoryPort")
                .isInstanceOf(ReplaceDurableMemoryPort.class);
        assertThat(durableMemoryAdapter)
                .as("DurableMemoryPersistenceAdapter must implement DeleteDurableMemoryPort")
                .isInstanceOf(DeleteDurableMemoryPort.class);
        assertThat(durableMemoryAdapter)
                .as("DurableMemoryPersistenceAdapter must implement PurgeDurableMemoriesPort")
                .isInstanceOf(PurgeDurableMemoriesPort.class);
        assertThat(durableMemoryAdapter)
                .as("DurableMemoryPersistenceAdapter must implement the turn-level FindEligibleDurableMemoriesPort")
                .isInstanceOf(com.ar.crm2.application.agent.turn.port.out.FindEligibleDurableMemoriesPort.class);
    }

    /**
     * The {@code AgentToolActionPersistenceAdapter} bean is the single
     * implementation of every tool-ledger port.
     */
    @Test
    void agentToolActionPersistenceAdapter_implementsEveryToolLedgerPort() {
        assertThat(agentToolActionAdapter)
                .as("AgentToolActionPersistenceAdapter must implement SaveAgentToolActionPort")
                .isInstanceOf(SaveAgentToolActionPort.class);
        assertThat(agentToolActionAdapter)
                .as("AgentToolActionPersistenceAdapter must implement MarkAgentToolActionCompletedPort")
                .isInstanceOf(MarkAgentToolActionCompletedPort.class);
        assertThat(agentToolActionAdapter)
                .as("AgentToolActionPersistenceAdapter must implement FindAgentToolActionByIdPort")
                .isInstanceOf(FindAgentToolActionByIdPort.class);
    }

    /**
     * The agent-turn services are exposed under their Application
     * use case interfaces. The controller (and the rest of the
     * Application layer) depends on these contract types.
     */
    @Test
    void createUserTurnUseCase_isWiredAsCreateUserTurnService() {
        assertThat(createUserTurnUseCase)
                .as("CreateUserTurnUseCase must be wired as a CreateUserTurnService instance")
                .isInstanceOf(CreateUserTurnService.class);
    }

    /**
     * The completion service is exposed under its Application use case
     * interface. The agent controller's @PostMapping handler depends on
     * this contract.
     */
    @Test
    void completeUserTurnUseCase_isWiredAsCompleteUserTurnService() {
        assertThat(completeUserTurnUseCase)
                .as("CompleteUserTurnUseCase must be wired as a CompleteUserTurnService instance")
                .isInstanceOf(CompleteUserTurnService.class);
    }

    /**
     * Triangulation: every durable-memory service is exposed under
     * its use case interface. The composition root must produce one
     * bean per use case type.
     */
    @Test
    void durableMemoryUseCases_areWiredUnderTheirContractTypes() {
        assertThat(recallDurableMemoriesUseCase)
                .as("RecallDurableMemoriesUseCase bean must exist")
                .isNotNull();
        assertThat(rememberDurableMemoryUseCase)
                .as("RememberDurableMemoryUseCase bean must exist")
                .isNotNull();
        assertThat(replaceDurableMemoryUseCase)
                .as("ReplaceDurableMemoryUseCase bean must exist")
                .isNotNull();
        assertThat(deleteDurableMemoryUseCase)
                .as("DeleteDurableMemoryUseCase bean must exist")
                .isNotNull();
        assertThat(purgeDurableMemoriesUseCase)
                .as("PurgeDurableMemoriesUseCase bean must exist")
                .isNotNull();
    }

    /**
     * The composition root must not expose a duplicate of any
     * turn / durable-memory / tool-ledger port bean. The agent
     * conversation depends on the single-adapter-per-port convention;
     * any duplicate would silently route writes to the wrong adapter.
     */
    @Test
    void wiringExposesExactlyOneBeanPerAgentPortType() {
        assertSingleBean(CreateUserTurnPort.class, agentTurnAdapter);
        assertSingleBean(FindCompletedAssistantContentPort.class, agentTurnAdapter);
        assertSingleBean(FindCompletedVisibleHistoryPort.class, agentTurnAdapter);
        assertSingleBean(CompletePreparedTurnPort.class, agentTurnAdapter);
        assertSingleBean(SaveDurableMemoryPort.class, durableMemoryAdapter);
        assertSingleBean(FindEligibleDurableMemoriesPort.class, durableMemoryAdapter);
        assertSingleBean(ReplaceDurableMemoryPort.class, durableMemoryAdapter);
        assertSingleBean(DeleteDurableMemoryPort.class, durableMemoryAdapter);
        assertSingleBean(PurgeDurableMemoriesPort.class, durableMemoryAdapter);
        assertSingleBean(SaveAgentToolActionPort.class, agentToolActionAdapter);
        assertSingleBean(MarkAgentToolActionCompletedPort.class, agentToolActionAdapter);
        assertSingleBean(FindAgentToolActionByIdPort.class, agentToolActionAdapter);
    }

    /**
     * C1 corrective wiring proof: the composition root must expose
     * exactly one {@link
     * com.ar.crm2.application.trato.port.in.EditTratoUseCase}
     * bean implemented by the canonical
     * {@link com.ar.crm2.application.trato.service.EditTratoService}
     * backing the {@code edit_trato} Spring AI tool.
     */
    @Test
    void editTratoUseCase_isWiredAsEditTratoServiceAndExposedOnce() {
        com.ar.crm2.application.trato.port.in.EditTratoUseCase editTratoUseCase =
                applicationContext.getBean(
                        com.ar.crm2.application.trato.port.in.EditTratoUseCase.class);
        assertThat(editTratoUseCase)
                .as("EditTratoUseCase must be wired as an EditTratoService instance")
                .isInstanceOf(com.ar.crm2.application.trato.service.EditTratoService.class);
        assertSingleBean(
                com.ar.crm2.application.trato.port.in.EditTratoUseCase.class,
                editTratoUseCase);
    }

    /**
     * Wiring proof for the {@code edit_contact} Spring AI tool: the
     * composition root must expose exactly one
     * {@link com.ar.crm2.application.contacto.port.in.EditContactoUseCase}
     * bean implemented by the canonical
     * {@link com.ar.crm2.application.contacto.service.EditContactoService}.
     */
    @Test
    void editContactoUseCase_isWiredAsEditContactoServiceAndExposedOnce() {
        com.ar.crm2.application.contacto.port.in.EditContactoUseCase editContactoUseCase =
                applicationContext.getBean(
                        com.ar.crm2.application.contacto.port.in.EditContactoUseCase.class);
        assertThat(editContactoUseCase)
                .as("EditContactoUseCase must be wired as an EditContactoService instance")
                .isInstanceOf(com.ar.crm2.application.contacto.service.EditContactoService.class);
        assertSingleBean(
                com.ar.crm2.application.contacto.port.in.EditContactoUseCase.class,
                editContactoUseCase);
    }

    /**
     * Wiring proof for the {@code find_companies} Spring AI tool: the
     * composition root must expose exactly one
     * {@link com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase}
     * bean implemented by the canonical
     * {@link com.ar.crm2.application.empresa.service.GetAllEmpresasService}.
     */
    @Test
    void getAllEmpresasUseCase_isWiredAsGetAllEmpresasServiceAndExposedOnce() {
        com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase getAllEmpresasUseCase =
                applicationContext.getBean(
                        com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase.class);
        assertThat(getAllEmpresasUseCase)
                .as("GetAllEmpresasUseCase must be wired as a GetAllEmpresasService instance")
                .isInstanceOf(com.ar.crm2.application.empresa.service.GetAllEmpresasService.class);
        assertSingleBean(
                com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase.class,
                getAllEmpresasUseCase);
    }

    /**
     * Wiring proof for the {@code create_company} Spring AI tool: the
     * composition root must expose exactly one
     * {@link com.ar.crm2.application.empresa.port.in.CreateEmpresaUseCase}
     * bean implemented by the canonical
     * {@link com.ar.crm2.application.empresa.service.CreateEmpresaService}.
     */
    @Test
    void createEmpresaUseCase_isWiredAsCreateEmpresaServiceAndExposedOnce() {
        com.ar.crm2.application.empresa.port.in.CreateEmpresaUseCase createEmpresaUseCase =
                applicationContext.getBean(
                        com.ar.crm2.application.empresa.port.in.CreateEmpresaUseCase.class);
        assertThat(createEmpresaUseCase)
                .as("CreateEmpresaUseCase must be wired as a CreateEmpresaService instance")
                .isInstanceOf(com.ar.crm2.application.empresa.service.CreateEmpresaService.class);
        assertSingleBean(
                com.ar.crm2.application.empresa.port.in.CreateEmpresaUseCase.class,
                createEmpresaUseCase);
    }

    /**
     * Wiring proof for the {@code edit_company} Spring AI tool: the
     * composition root must expose exactly one
     * {@link com.ar.crm2.application.empresa.port.in.EditEmpresaUseCase}
     * bean implemented by the canonical
     * {@link com.ar.crm2.application.empresa.service.EditEmpresaService}.
     */
    @Test
    void editEmpresaUseCase_isWiredAsEditEmpresaServiceAndExposedOnce() {
        com.ar.crm2.application.empresa.port.in.EditEmpresaUseCase editEmpresaUseCase =
                applicationContext.getBean(
                        com.ar.crm2.application.empresa.port.in.EditEmpresaUseCase.class);
        assertThat(editEmpresaUseCase)
                .as("EditEmpresaUseCase must be wired as an EditEmpresaService instance")
                .isInstanceOf(com.ar.crm2.application.empresa.service.EditEmpresaService.class);
        assertSingleBean(
                com.ar.crm2.application.empresa.port.in.EditEmpresaUseCase.class,
                editEmpresaUseCase);
    }

    /**
     * Defence-in-depth wiring proof: the shared {@code SpringAiCrmTools}
     * bean MUST NOT take any delete-company use case as a constructor
     * dependency. The REST controllers may still wire a
     * {@code DeleteEmpresaUseCase} for the HTTP surface, but the
     * Spring AI tools bean must remain a thin adapter over the
     * allowlisted canonical use cases — exposing a delete-company
     * tool would silently let the agent destroy company records.
     *
     * <p>This test reflects on the {@code SpringAiCrmTools}
     * constructor's parameter types and asserts that none of them
     * matches {@code DeleteEmpresaUseCase} or {@code DeleteContactoUseCase}.
     */
    @Test
    void springAiCrmToolsConstructorDoesNotInjectAnyDeleteUseCase() {
        java.lang.reflect.Constructor<?>[] constructors =
                com.ar.crm2.adapter.out.ai.tool.SpringAiCrmTools.class.getDeclaredConstructors();
        assertThat(constructors)
                .as("SpringAiCrmTools must expose exactly one constructor")
                .hasSize(1);
        Class<?>[] parameterTypes = constructors[0].getParameterTypes();
        for (Class<?> type : parameterTypes) {
            assertThat(type.getSimpleName())
                    .as("SpringAiCrmTools constructor must NOT depend on any Delete*UseCase")
                    .doesNotStartWith("Delete");
            assertThat(type)
                    .as("SpringAiCrmTools constructor must NOT depend on DeleteEmpresaUseCase")
                    .isNotEqualTo(com.ar.crm2.application.empresa.port.in.DeleteEmpresaUseCase.class);
            assertThat(type)
                    .as("SpringAiCrmTools constructor must NOT depend on DeleteContactoUseCase")
                    .isNotEqualTo(com.ar.crm2.application.contacto.port.in.DeleteContactoUseCase.class);
        }
    }

    private void assertSingleBean(Class<?> type, Object expected) {
        String[] names = applicationContext().getBeanNamesForType(type);
        assertThat(names)
                .as("Exactly one bean of type %s must be exposed by the composition root",
                        type.getSimpleName())
                .hasSize(1);
        Object resolved = applicationContext().getBean(names[0]);
        assertThat(resolved)
                .as("Bean for %s must be the expected adapter", type.getSimpleName())
                .isSameAs(expected);
    }

    private ApplicationContext applicationContext() {
        return applicationContext;
    }

    /**
     * Focused harness that supplies the non-{@code WiringConfig}
     * collaborators required by the composition:
     * <ul>
     *   <li>The {@link ChatClient} seam that the existing
     *       {@link com.ar.crm2.config.AgentConfig} provides in
     *       production (the chat-completion adapter consumes it).
     *       Here we use a deterministic mock so the composition root
     *       still wires end-to-end without contacting any provider.</li>
     *   <li>The platform transaction manager the tool-ledger adapter
     *       requires for its short-transaction claim/complete cycle.</li>
     *   <li>A UTC {@link Clock} so the tool ledger has a deterministic
     *       time source.</li>
     *   <li>Mocked JPA repositories for every entity the conversation
     *       stack depends on, plus the unrelated ones that
     *       {@code WiringConfig} needs to fully boot. The wiring proof
     *       never invokes any of these mocks.</li>
     *   <li>Other collaborators (Keycloak adapter, mail sender, etc.)
     *       that {@code WiringConfig} already wires through and that
     *       would otherwise fail with "no qualifying bean" — these
     *       are unrelated to the agent wiring proof.</li>
     * </ul>
     */
    @TestConfiguration
    static class AgentWiringHarness {

        // ── ChatClient seam consumed by the chat-completion adapter ──

        @Bean
        ChatClient chatClient() {
            return mock(ChatClient.class);
        }

        // ── Platform transaction manager + clock for the tool ledger ──

        @Bean
        PlatformTransactionManager platformTransactionManager() {
            return mock(PlatformTransactionManager.class);
        }

        @Bean
        Clock agentToolActionClock() {
            return Clock.fixed(
                    java.time.Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        }

        // ── Agent-conversation JPA repositories (real wiring) ──────

        @Bean AgentConversationRepository agentConversationRepository() { return mock(AgentConversationRepository.class); }
        @Bean AgentTurnRepository agentTurnRepository() { return mock(AgentTurnRepository.class); }
        @Bean AgentTurnRequestRepository agentTurnRequestRepository() { return mock(AgentTurnRequestRepository.class); }
        @Bean AgentVisibleHistoryRepository agentVisibleHistoryRepository() { return mock(AgentVisibleHistoryRepository.class); }
        @Bean DurableMemoryRepository durableMemoryRepository() { return mock(DurableMemoryRepository.class); }
        @Bean AgentToolActionRepository agentToolActionRepository() { return mock(AgentToolActionRepository.class); }

        // ── Unrelated JPA repositories WiringConfig needs to fully boot ──

        @Bean com.ar.crm2.adapter.out.persistence.repository.EmpresaRepository empresaRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.EmpresaRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.ContactoRepository contactoRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.ContactoRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.TableroRepository tableroRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.TableroRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.TratoRepository tratoRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.TratoRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.TareaRepository tareaRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.TareaRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.FichaRepository fichaRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.FichaRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.RolRepository rolRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.RolRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.ColumnaRepository columnaRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.ColumnaRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.UsuarioRepository usuarioRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.UsuarioRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.SuperUsuarioRepository superUsuarioRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.SuperUsuarioRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.EtiquetaRepository etiquetaRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.EtiquetaRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.FichaEtiquetaRepository fichaEtiquetaRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.FichaEtiquetaRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.AgendaRepository agendaRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.AgendaRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.NotaTratoRepository notaTratoRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.NotaTratoRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.CanalWhatsappRepository canalWhatsappRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.CanalWhatsappRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.ConversacionRepository conversacionRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.ConversacionRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.MensajeRepository mensajeRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.MensajeRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.BotRepository botRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.BotRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.AjustesWaRepository ajustesWaRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.AjustesWaRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.PlantillaRepository plantillaRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.PlantillaRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.GrupoRepository grupoRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.GrupoRepository.class);
        }
        @Bean com.ar.crm2.adapter.out.persistence.repository.MensajeGrupoRepository mensajeGrupoRepository() {
            return mock(com.ar.crm2.adapter.out.persistence.repository.MensajeGrupoRepository.class);
        }

        // ── Non-repository collaborators that WiringConfig wires ───

        @Bean com.ar.crm2.security.WaProperties waProperties() {
            return new com.ar.crm2.security.WaProperties(null, null);
        }
        @Bean com.ar.crm2.config.KeycloakAdminProperties keycloakAdminProperties() {
            return new com.ar.crm2.config.KeycloakAdminProperties();
        }
        @Bean com.ar.crm2.adapter.out.keycloak.KeycloakUserProvisioningAdapter keycloakUserProvisioningAdapter(
                com.ar.crm2.config.KeycloakAdminProperties props) {
            return new com.ar.crm2.adapter.out.keycloak.KeycloakUserProvisioningAdapter(props);
        }
        @Bean com.ar.crm2.adapter.out.email.config.EmailProperties emailProperties() {
            return new com.ar.crm2.adapter.out.email.config.EmailProperties();
        }
        @Bean org.springframework.mail.javamail.JavaMailSender javaMailSender() {
            return mock(org.springframework.mail.javamail.JavaMailSender.class);
        }
        @Bean com.ar.crm2.adapter.out.sse.SseEmitterRegistry sseEmitterRegistry() {
            return new com.ar.crm2.adapter.out.sse.SseEmitterRegistry();
        }
    }
}
