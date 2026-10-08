package com.ar.crm2.adapter.out.ai;

import com.ar.crm2.adapter.out.ai.tool.AgendaTools;
import com.ar.crm2.adapter.out.ai.tool.AgentToolCallbackCatalog;
import com.ar.crm2.adapter.out.ai.tool.ColumnaTools;
import com.ar.crm2.adapter.out.ai.tool.ContactoTools;
import com.ar.crm2.adapter.out.ai.tool.EmpresaTools;
import com.ar.crm2.adapter.out.ai.tool.EtiquetaTools;
import com.ar.crm2.adapter.out.ai.tool.FichaTools;
import com.ar.crm2.adapter.out.ai.tool.TableroTools;
import com.ar.crm2.adapter.out.ai.tool.TareaTools;
import com.ar.crm2.adapter.out.ai.tool.TratoTools;
import com.ar.crm2.application.security.AuthorizationCapabilities;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceCapabilities;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.model.agent.vo.VisibleMessage;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SpringAiChatCompletionCapabilitiesTest {
    private static final AgentOwnerId OWNER = AgentOwnerId.from("capabilities-test");
    private static final UUID ACTOR = UUID.fromString("cccccccc-1111-2222-3333-444444444444");
    private static final TurnId TURN = TurnId.create();
    private static final List<Class<?>> TOOL_GROUPS = List.of(
            AgendaTools.class, ColumnaTools.class, ContactoTools.class,
            EmpresaTools.class, EtiquetaTools.class, FichaTools.class,
            TableroTools.class, TareaTools.class, TratoTools.class);

    @Test
    void bindsFreshRoleFilteredCallbacksAndCapabilitiesForEachTurn() {
        ChatClient chatClient = mock(ChatClient.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        AuthorizationCapabilities contactReader = new AuthorizationCapabilities(Map.of(
                RecursoCrm.CONTACTO, capability(AccionCrm.LEER),
                RecursoCrm.EMPRESA, capability(AccionCrm.LEER)));
        when(authorization.authorizationCapabilities()).thenReturn(contactReader, AuthorizationCapabilities.none());

        List<Object[]> callbackBatches = new ArrayList<>();
        AtomicReference<Consumer<ChatClient.PromptSystemSpec>> systemConsumer = new AtomicReference<>();
        ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
        when(response.content()).thenReturn("ok");
        ChatClient.ChatClientRequestSpec request = requestSpec(response, callbackBatches, systemConsumer);
        when(chatClient.prompt()).thenReturn(request);

        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(
                chatClient, new AgentToolCallbackCatalog(mockCallbacks()), authorization);
        List<VisibleMessage> history = List.of(VisibleMessage.user("hello"));

        assertThat(adapter.complete(OWNER, ACTOR, TURN, history, List.of(), "first turn")).isEqualTo("ok");
        ChatClient.PromptSystemSpec firstSystem = mock(ChatClient.PromptSystemSpec.class);
        when(firstSystem.param(anyString(), any())).thenReturn(firstSystem);
        systemConsumer.get().accept(firstSystem);
        verify(firstSystem).param(eq("agent_capabilities"), eq(
                "Role-level capabilities are an upper bound, not a promise of access to any record. "
                        + "Every tool call remains subject to authoritative record, relationship, and field checks; "
                        + "sensitive groups apply only after those checks permit the record.\n"
                        + "CONTACTO {actions=read; record_scope=TODO_COMPARTIDO; readable_sensitive_groups=none; writable_sensitive_groups=none}\n"
                        + "EMPRESA {actions=read; record_scope=TODO_COMPARTIDO; readable_sensitive_groups=none; writable_sensitive_groups=none}"));

        assertThat(adapter.complete(OWNER, ACTOR, TURN, history, List.of(), "second turn")).isEqualTo("ok");

        assertThat(callbackBatches).hasSize(2);
        assertThat(toolNames(callbackBatches.get(0)))
                .containsExactlyInAnyOrder("find_contacts", "get_contact", "list_companies");
        assertThat(callbackBatches.get(1)).isEmpty();
        verify(authorization, times(2)).authorizationCapabilities();
    }

    @Test
    void capabilityLookupFailureFailsClosedBeforeCallingChatClient() {
        ChatClient chatClient = mock(ChatClient.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        when(authorization.authorizationCapabilities()).thenThrow(new IllegalStateException("secret db detail"));
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(
                chatClient, AgentToolCallbackCatalog.empty(), authorization);

        assertThatThrownBy(() -> adapter.complete(OWNER, ACTOR, TURN,
                List.of(VisibleMessage.user("hello")), List.of(), "prompt"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Spring AI chat completion failed")
                .hasNoCause();
        verifyNoInteractions(chatClient);
    }

    private static ChatClient.ChatClientRequestSpec requestSpec(
            ChatClient.CallResponseSpec response,
            List<Object[]> callbackBatches,
            AtomicReference<Consumer<ChatClient.PromptSystemSpec>> systemConsumer) {
        return mock(ChatClient.ChatClientRequestSpec.class, invocation -> {
            if (invocation.getMethod().getName().equals("system")
                    && invocation.getArgument(0) instanceof Consumer<?> consumer) {
                @SuppressWarnings("unchecked")
                Consumer<ChatClient.PromptSystemSpec> typedConsumer =
                        (Consumer<ChatClient.PromptSystemSpec>) consumer;
                systemConsumer.set(typedConsumer);
            }
            if (invocation.getMethod().getName().equals("tools")) {
                callbackBatches.add(invocation.getArguments());
            }
            if (invocation.getMethod().getName().equals("call")) {
                return response;
            }
            if (invocation.getMethod().getReturnType().isInstance(invocation.getMock())) {
                return invocation.getMock();
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
    }

    private static ResourceCapabilities capability(AccionCrm... actions) {
        return new ResourceCapabilities(AlcanceCrm.TODO_COMPARTIDO,
                EnumSet.copyOf(List.of(actions)), Set.of(), Set.of());
    }

    private static Set<String> toolNames(Object[] callbacks) {
        return Arrays.stream(callbacks)
                .map(ToolCallback.class::cast)
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.toSet());
    }

    private static ToolCallback[] mockCallbacks() {
        return TOOL_GROUPS.stream()
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .map(method -> method.getAnnotation(Tool.class))
                .filter(java.util.Objects::nonNull)
                .map(Tool::name)
                .map(SpringAiChatCompletionCapabilitiesTest::mockCallback)
                .toArray(ToolCallback[]::new);
    }

    private static ToolCallback mockCallback(String name) {
        ToolCallback callback = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);
        when(callback.getToolDefinition()).thenReturn(definition);
        when(definition.name()).thenReturn(name);
        return callback;
    }
}