package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.CreateContactOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FindContactsOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.UpdateDealStageOutput;
import com.ar.crm2.application.contacto.command.CreateContactoCommand;
import com.ar.crm2.application.contacto.command.GetAllContactosCommand;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.enums.EstadoTrato;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Strict TDD contract for the A3 review cleanup {@link CrmToolMapper} —
 * pure, deterministic, dependency-free. The mapper is the single trust
 * boundary that validates required fields, enforces the GANADO/PERDIDO
 * + motivo rules, normalizes string inputs, forces the find cap of 20,
 * converts raw tool parameter values plus the trusted actor into the
 * existing Application commands and the typed stage arguments, and
 * projects domain entities to bounded model-visible output records.
 */
class CrmToolMapperTest {

    private static final UUID TRUSTED_ACTOR =
            UUID.fromString("aaaa1111-2222-3333-4444-555566667777");

    @Test
    void findContactsMapsAllOptionalFiltersAndPreservesTrustedActor() {
        GetAllContactosCommand command = CrmToolMapper.toGetAllContactosCommand(
                "acme", "PROSPECTO",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "LinkedIn",
                TRUSTED_ACTOR);

        assertThat(command.actorUsuarioId()).isEqualTo(TRUSTED_ACTOR);
        assertThat(command.search()).isEqualTo("acme");
        assertThat(command.estadoRelacion()).isEqualTo("PROSPECTO");
        assertThat(command.empresaId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(command.responsableId()).isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        assertThat(command.comoNosConocio()).isEqualTo("LinkedIn");
        assertThat(command.maxResults())
                .as("the mapper must always apply the hard cap of 20")
                .isEqualTo(20);
    }

    @Test
    void findContactsAcceptsNullFiltersAndPreservesTrustedActor() {
        GetAllContactosCommand command = CrmToolMapper.toGetAllContactosCommand(
                null, null, null, null, null, TRUSTED_ACTOR);

        assertThat(command.actorUsuarioId()).isEqualTo(TRUSTED_ACTOR);
        assertThat(command.search()).isNull();
        assertThat(command.estadoRelacion()).isNull();
        assertThat(command.empresaId()).isNull();
        assertThat(command.responsableId()).isNull();
        assertThat(command.comoNosConocio()).isNull();
        assertThat(command.maxResults()).isEqualTo(20);
    }

    @Test
    void findContactsRejectsMissingTrustedActor() {
        assertThatThrownBy(() -> CrmToolMapper.toGetAllContactosCommand(
                null, null, null, null, null, null))
                .as("find_contacts requires a trusted actor")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createContactMapsRequiredAndOptionalFieldsAndThreadsTrustedActor() {
        CreateContactoCommand command = CrmToolMapper.toCreateContactoCommand(
                UUID.fromString("99999999-9999-9999-9999-999999999999"),
                "Acme Inc",
                "ops@acme.com",
                "ACTIVO",
                UUID.fromString("77777777-7777-7777-7777-777777777777"),
                "+525500000000",
                "Director",
                "Referral",
                TRUSTED_ACTOR);

        assertThat(command.empresaId()).isEqualTo(UUID.fromString("99999999-9999-9999-9999-999999999999"));
        assertThat(command.nombre()).isEqualTo("Acme Inc");
        assertThat(command.correo()).isEqualTo("ops@acme.com");
        assertThat(command.estadoRelacion()).isEqualTo(EstadoRelacion.ACTIVO);
        assertThat(command.responsableId()).isEqualTo(UUID.fromString("77777777-7777-7777-7777-777777777777"));
        assertThat(command.creadoPor())
                .as("trusted actor from per-call ToolContext must reach the command")
                .isEqualTo(TRUSTED_ACTOR);
        assertThat(command.telefono()).isEqualTo("+525500000000");
        assertThat(command.cargo()).isEqualTo("Director");
        assertThat(command.comoNosConocio()).isEqualTo("Referral");
    }

    @Test
    void createContactRejectsNullAndBlankRequiredInputsAndUnknownEstadoRelacion() {
        UUID empresaId = UUID.randomUUID();
        assertThatThrownBy(() -> CrmToolMapper.toCreateContactoCommand(
                empresaId, null, null, "ACTIVO", null, null, null, null, TRUSTED_ACTOR))
                .as("missing nombre")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toCreateContactoCommand(
                empresaId, "   ", null, "ACTIVO", null, null, null, null, TRUSTED_ACTOR))
                .as("blank nombre")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toCreateContactoCommand(
                empresaId, "Acme", null, null, null, null, null, null, TRUSTED_ACTOR))
                .as("missing estadoRelacion")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toCreateContactoCommand(
                empresaId, "Acme", null, "NOT_A_STATE", null, null, null, null, TRUSTED_ACTOR))
                .as("unknown estadoRelacion")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createContactRejectsMissingEmpresaIdOrMissingTrustedActor() {
        assertThatThrownBy(() -> CrmToolMapper.toCreateContactoCommand(
                null, "Acme", null, "ACTIVO", null, null, null, null, TRUSTED_ACTOR))
                .as("missing empresaId")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toCreateContactoCommand(
                UUID.randomUUID(), "Acme", null, "ACTIVO", null, null, null, null, null))
                .as("missing trusted actor")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateDealStageMapsGanadoArguments() {
        CrmToolMapper.UpdateDealStageArguments args = CrmToolMapper.toUpdateDealStageArguments(
                UUID.fromString("99999999-9999-9999-9999-999999999999"),
                "GANADO",
                null);

        assertThat(args.tratoId()).isEqualTo(UUID.fromString("99999999-9999-9999-9999-999999999999"));
        assertThat(args.status()).isEqualTo("GANADO");
        assertThat(args.motivo()).isNull();
    }

    @Test
    void updateDealStageMapsPerdidoArgumentsIncludingMotivo() {
        CrmToolMapper.UpdateDealStageArguments args = CrmToolMapper.toUpdateDealStageArguments(
                UUID.fromString("99999999-9999-9999-9999-999999999999"),
                "PERDIDO",
                "Budget");

        assertThat(args.tratoId()).isEqualTo(UUID.fromString("99999999-9999-9999-9999-999999999999"));
        assertThat(args.status()).isEqualTo("PERDIDO");
        assertThat(args.motivo()).isEqualTo("Budget");
    }

    @Test
    void updateDealStageRejectsMissingIdMissingStatusAndUnsupportedStatus() {
        assertThatThrownBy(() -> CrmToolMapper.toUpdateDealStageArguments(
                null, "GANADO", null))
                .as("missing id")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toUpdateDealStageArguments(
                UUID.randomUUID(), null, null))
                .as("missing status")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toUpdateDealStageArguments(
                UUID.randomUUID(), "OPEN", null))
                .as("unsupported status")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateDealStageRejectsPerdidoWithoutNonBlankMotivo() {
        assertThatThrownBy(() -> CrmToolMapper.toUpdateDealStageArguments(
                UUID.randomUUID(), "PERDIDO", null))
                .as("PERDIDO without motivo")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toUpdateDealStageArguments(
                UUID.randomUUID(), "PERDIDO", "   "))
                .as("PERDIDO with blank motivo")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findContactsProjectsDomainEntitiesToBoundedOutputRecords() {
        Contacto c1 = Contacto.create(
                EmpresaId.from(UUID.randomUUID()),
                "Acme",
                null,
                EstadoRelacion.PROSPECTO,
                null, null, null, null, null);
        Contacto c2 = Contacto.create(
                EmpresaId.from(UUID.randomUUID()),
                "Beta",
                "beta@example.com",
                EstadoRelacion.ACTIVO,
                null, null, null, null, null);

        FindContactsOutput output = CrmToolMapper.toFindContactsOutput(List.of(c1, c2));

        assertThat(output.contacts()).hasSize(2);
        assertThat(output.contacts().get(0).nombre()).isEqualTo("Acme");
        assertThat(output.contacts().get(0).estadoRelacion()).isEqualTo("PROSPECTO");
        assertThat(output.contacts().get(1).nombre()).isEqualTo("Beta");
        assertThat(output.contacts().get(1).correo()).isEqualTo("beta@example.com");
    }

    @Test
    void findContactsMapsEmptyAndNullListToEmptyOutput() {
        assertThat(CrmToolMapper.toFindContactsOutput(List.of()).contacts()).isEmpty();
        assertThat(CrmToolMapper.toFindContactsOutput(null).contacts()).isEmpty();
    }

    @Test
    void createContactProjectsDomainEntityToBoundedOutput() {
        Contacto contact = Contacto.create(
                EmpresaId.from(UUID.randomUUID()),
                "Acme",
                null,
                EstadoRelacion.PROSPECTO,
                null, null, null, null, null);

        CreateContactOutput output = CrmToolMapper.toCreateContactOutput(contact);

        assertThat(output.id()).isEqualTo(String.valueOf(contact.getId().value()));
        assertThat(output.nombre()).isEqualTo("Acme");
        assertThat(output.estadoRelacion()).isEqualTo("PROSPECTO");
        assertThat(output.correo()).isNull();
    }

    @Test
    void updateDealStageProjectsDomainEntityToBoundedOutput() {
        Trato initial = Trato.create(
                ContactoId.create(), UsuarioId.from(UUID.randomUUID()),
                "Deal", null, null, null, null);
        Trato ganado = initial.ganar();

        UpdateDealStageOutput output = CrmToolMapper.toUpdateDealStageOutput(ganado);

        assertThat(output.id()).isEqualTo(String.valueOf(initial.getId().value()));
        assertThat(output.status()).isEqualTo(EstadoTrato.GANADO.name());
    }
}