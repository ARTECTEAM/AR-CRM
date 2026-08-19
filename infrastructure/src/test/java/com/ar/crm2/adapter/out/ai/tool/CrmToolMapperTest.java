package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.CreateCompanyOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.CreateContactOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.EditCompanyOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.EditContactOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.EditTratoOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FindCompaniesOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FindContactsOutput;
import com.ar.crm2.application.contacto.command.CreateContactoCommand;
import com.ar.crm2.application.contacto.command.EditContactoCommand;
import com.ar.crm2.application.contacto.command.GetAllContactosCommand;
import com.ar.crm2.application.empresa.command.CreateEmpresaCommand;
import com.ar.crm2.application.empresa.command.EditEmpresaCommand;
import com.ar.crm2.application.empresa.query.EmpresaFilterCriteria;
import com.ar.crm2.application.trato.command.EditTratoCommand;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.entity.Empresa;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.enums.TipoContrato;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Strict TDD contract for the A3 review cleanup {@link CrmToolMapper} —
 * pure, deterministic, dependency-free. The mapper is the single trust
 * boundary that validates required fields, normalizes string inputs,
 * forces the find cap of 20, converts raw tool parameter values plus
 * the trusted actor into the existing Application commands (and the
 * canonical edit_trato command), and projects domain entities to
 * bounded model-visible output records.
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
    void editContactMapsRequiredAndOptionalFieldsToCanonicalEditContactoCommand() {
        UUID id = UUID.fromString("99999999-9999-9999-9999-999999999999");
        UUID responsableId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        EditContactoCommand command = CrmToolMapper.toEditContactoCommand(
                id, "Renamed Contact",
                "renamed@example.com", "ACTIVO",
                responsableId, "+525500000001", "VP Sales", "Referral");

        assertThat(command.id()).isEqualTo(id);
        assertThat(command.nombre()).isEqualTo("Renamed Contact");
        assertThat(command.correo()).isEqualTo("renamed@example.com");
        assertThat(command.estadoRelacion()).isEqualTo(EstadoRelacion.ACTIVO);
        assertThat(command.responsableId()).isEqualTo(responsableId);
        assertThat(command.telefono()).isEqualTo("+525500000001");
        assertThat(command.cargo()).isEqualTo("VP Sales");
        assertThat(command.comoNosConocio()).isEqualTo("Referral");
    }

    @Test
    void editContactAcceptsBlankOptionalStringsAndNullOptionalFields() {
        UUID id = UUID.fromString("99999999-9999-9999-9999-999999999999");
        EditContactoCommand command = CrmToolMapper.toEditContactoCommand(
                id, "Contact",
                null, "PROSPECTO", null, "", "", "");

        assertThat(command.correo()).isNull();
        assertThat(command.responsableId()).isNull();
        assertThat(command.telefono()).isNull();
        assertThat(command.cargo()).isNull();
        assertThat(command.comoNosConocio()).isNull();
    }

    @Test
    void editContactRejectsMissingIdAndBlankNombreAndBlankEstadoRelacion() {
        UUID validId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        assertThatThrownBy(() -> CrmToolMapper.toEditContactoCommand(
                null, "Contact", null, "ACTIVO", null, null, null, null))
                .as("missing id")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toEditContactoCommand(
                validId, null, null, "ACTIVO", null, null, null, null))
                .as("missing nombre")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toEditContactoCommand(
                validId, "   ", null, "ACTIVO", null, null, null, null))
                .as("blank nombre")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toEditContactoCommand(
                validId, "Contact", null, null, null, null, null, null))
                .as("missing estadoRelacion")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toEditContactoCommand(
                validId, "Contact", null, "   ", null, null, null, null))
                .as("blank estadoRelacion")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toEditContactoCommand(
                validId, "Contact", null, "NOT_A_STATE", null, null, null, null))
                .as("unknown estadoRelacion")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findCompaniesMapsOptionalFiltersToEmpresaFilterCriteriaAndRequiresTrustedActor() {
        EmpresaFilterCriteria criteria = CrmToolMapper.toFindCompaniesCriteria(
                "acme", "ACTIVO", "Software",
                UUID.fromString("77777777-7777-7777-7777-777777777777"),
                "CON_WEB", TRUSTED_ACTOR);

        assertThat(criteria.search()).isEqualTo("acme");
        assertThat(criteria.estadoRelacion()).isEqualTo(EstadoRelacion.ACTIVO);
        assertThat(criteria.sector()).isEqualTo("Software");
        assertThat(criteria.responsableId())
                .as("responsableId must be projected to the Domain UsuarioId type")
                .isEqualTo(UsuarioId.from(UUID.fromString("77777777-7777-7777-7777-777777777777")));
        assertThat(criteria.web()).isEqualTo(EmpresaFilterCriteria.WebFilter.CON_WEB);
    }

    @Test
    void findCompaniesAcceptsNullFiltersButRequiresTrustedActor() {
        EmpresaFilterCriteria criteria = CrmToolMapper.toFindCompaniesCriteria(
                null, null, null, null, null, TRUSTED_ACTOR);

        assertThat(criteria.search()).isNull();
        assertThat(criteria.estadoRelacion()).isNull();
        assertThat(criteria.sector()).isNull();
        assertThat(criteria.responsableId()).isNull();
        assertThat(criteria.web()).isNull();
    }

    @Test
    void findCompaniesRejectsMissingTrustedActor() {
        assertThatThrownBy(() -> CrmToolMapper.toFindCompaniesCriteria(
                null, null, null, null, null, null))
                .as("find_companies requires a trusted actor at the trust boundary")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findCompaniesRejectsUnknownEstadoRelacionAndWebFilter() {
        assertThatThrownBy(() -> CrmToolMapper.toFindCompaniesCriteria(
                null, "NOT_A_STATE", null, null, null, TRUSTED_ACTOR))
                .as("unknown estadoRelacion")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toFindCompaniesCriteria(
                null, null, null, null, "MAYBE_WEB", TRUSTED_ACTOR))
                .as("unknown web filter")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createCompanyMapsRequiredAndOptionalFieldsAndThreadsTrustedActor() {
        CreateEmpresaCommand command = CrmToolMapper.toCreateEmpresaCommand(
                "Acme Inc", "Software", "+525500000000",
                "https://acme.example", "https://facebook/acme",
                "https://instagram/acme", "https://twitter/acme",
                "ACTIVO",
                UUID.fromString("77777777-7777-7777-7777-777777777777"),
                "Some notes", TRUSTED_ACTOR);

        assertThat(command.nombre()).isEqualTo("Acme Inc");
        assertThat(command.sector()).isEqualTo("Software");
        assertThat(command.telefono()).isEqualTo("+525500000000");
        assertThat(command.paginaWeb()).isEqualTo("https://acme.example");
        assertThat(command.facebook()).isEqualTo("https://facebook/acme");
        assertThat(command.instagram()).isEqualTo("https://instagram/acme");
        assertThat(command.twitter()).isEqualTo("https://twitter/acme");
        assertThat(command.estadoRelacion()).isEqualTo(EstadoRelacion.ACTIVO);
        assertThat(command.responsableId()).isEqualTo(UUID.fromString("77777777-7777-7777-7777-777777777777"));
        assertThat(command.creadoPor())
                .as("trusted actor from per-call ToolContext must reach the command")
                .isEqualTo(TRUSTED_ACTOR);
        assertThat(command.notas()).isEqualTo("Some notes");
    }

    @Test
    void createCompanyRejectsNullAndBlankNombreAndUnknownEstadoRelacionAndMissingActor() {
        assertThatThrownBy(() -> CrmToolMapper.toCreateEmpresaCommand(
                null, null, null, null, null, null, null, null, null, null, TRUSTED_ACTOR))
                .as("missing nombre")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toCreateEmpresaCommand(
                "   ", null, null, null, null, null, null, null, null, null, TRUSTED_ACTOR))
                .as("blank nombre")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toCreateEmpresaCommand(
                "Acme", null, null, null, null, null, null, "NOT_A_STATE", null, null, TRUSTED_ACTOR))
                .as("unknown estadoRelacion")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toCreateEmpresaCommand(
                "Acme", null, null, null, null, null, null, null, null, null, null))
                .as("missing trusted actor")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editCompanyMapsRequiredAndOptionalFieldsToCanonicalEditEmpresaCommand() {
        UUID id = UUID.fromString("88888888-8888-8888-8888-888888888888");
        UUID responsableId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        EditEmpresaCommand command = CrmToolMapper.toEditEmpresaCommand(
                id, "Renamed Co", "Software", "+525500000001",
                "https://renamed.example", null, null, null,
                "ACTIVO", responsableId, "Updated notes");

        assertThat(command.id()).isEqualTo(id);
        assertThat(command.nombre()).isEqualTo("Renamed Co");
        assertThat(command.sector()).isEqualTo("Software");
        assertThat(command.telefono()).isEqualTo("+525500000001");
        assertThat(command.paginaWeb()).isEqualTo("https://renamed.example");
        assertThat(command.facebook()).isNull();
        assertThat(command.instagram()).isNull();
        assertThat(command.twitter()).isNull();
        assertThat(command.estadoRelacion()).isEqualTo(EstadoRelacion.ACTIVO);
        assertThat(command.responsableId()).isEqualTo(responsableId);
        assertThat(command.notas()).isEqualTo("Updated notes");
    }

    @Test
    void editCompanyAcceptsBlankOptionalStringsAndNullOptionalFields() {
        UUID id = UUID.fromString("88888888-8888-8888-8888-888888888888");
        EditEmpresaCommand command = CrmToolMapper.toEditEmpresaCommand(
                id, "Co", null, null, null, null, null, null, null, null, null);

        assertThat(command.sector()).isNull();
        assertThat(command.telefono()).isNull();
        assertThat(command.paginaWeb()).isNull();
        assertThat(command.facebook()).isNull();
        assertThat(command.instagram()).isNull();
        assertThat(command.twitter()).isNull();
        assertThat(command.estadoRelacion()).isNull();
        assertThat(command.responsableId()).isNull();
        assertThat(command.notas()).isNull();
    }

    @Test
    void editCompanyRejectsMissingIdAndBlankNombreAndUnknownEstadoRelacion() {
        UUID validId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        assertThatThrownBy(() -> CrmToolMapper.toEditEmpresaCommand(
                null, "Co", null, null, null, null, null, null, null, null, null))
                .as("missing id")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toEditEmpresaCommand(
                validId, null, null, null, null, null, null, null, null, null, null))
                .as("missing nombre")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toEditEmpresaCommand(
                validId, "   ", null, null, null, null, null, null, null, null, null))
                .as("blank nombre")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toEditEmpresaCommand(
                validId, "Co", null, null, null, null, null, null, "NOT_A_STATE", null, null))
                .as("unknown estadoRelacion")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editTratoMapsRequiredAndOptionalFieldsToCanonicalEditTratoCommand() {
        UUID id = UUID.fromString("99999999-9999-9999-9999-999999999999");
        UUID responsableId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        EditTratoCommand command = CrmToolMapper.toEditTratoCommand(
                id, responsableId, "Renamed Deal",
                new BigDecimal("1500.00"), 75,
                LocalDate.parse("2026-12-31"), "SERVICIO");

        assertThat(command.id()).isEqualTo(id);
        assertThat(command.responsableId()).isEqualTo(responsableId);
        assertThat(command.nombre()).isEqualTo("Renamed Deal");
        assertThat(command.valorEstimado()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(command.probabilidad()).isEqualTo(75);
        assertThat(command.fechaCierreEsperada()).isEqualTo(LocalDate.parse("2026-12-31"));
        assertThat(command.tipoContrato()).isEqualTo(TipoContrato.SERVICIO);
    }

    @Test
    void editTratoAcceptsBlankOptionalStringsAndNullOptionalFields() {
        UUID id = UUID.fromString("99999999-9999-9999-9999-999999999999");
        UUID responsableId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        EditTratoCommand command = CrmToolMapper.toEditTratoCommand(
                id, responsableId, "Deal",
                null, null, null, "");

        assertThat(command.valorEstimado()).isNull();
        assertThat(command.probabilidad()).isNull();
        assertThat(command.fechaCierreEsperada()).isNull();
        assertThat(command.tipoContrato()).isNull();
    }

    @Test
    void editTratoRejectsMissingIdResponsableIdAndBlankNombre() {
        UUID validResponsable = UUID.fromString("77777777-7777-7777-7777-777777777777");
        String validId = "99999999-9999-9999-9999-999999999999";
        assertThatThrownBy(() -> CrmToolMapper.toEditTratoCommand(
                null, validResponsable, "Deal", null, null, null, null))
                .as("missing id")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toEditTratoCommand(
                UUID.fromString(validId), null, "Deal", null, null, null, null))
                .as("missing responsableId")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toEditTratoCommand(
                UUID.fromString(validId), validResponsable, null, null, null, null, null))
                .as("missing nombre")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrmToolMapper.toEditTratoCommand(
                UUID.fromString(validId), validResponsable, "   ", null, null, null, null))
                .as("blank nombre")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editTratoRejectsUnknownTipoContratoName() {
        UUID validId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        UUID validResponsable = UUID.fromString("77777777-7777-7777-7777-777777777777");
        assertThatThrownBy(() -> CrmToolMapper.toEditTratoCommand(
                validId, validResponsable, "Deal", null, null, null, "NOT_A_TYPE"))
                .as("unknown TipoContrato")
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
    void editContactProjectsDomainEntityToBoundedOutputAndStripsAuditFields() {
        Contacto contact = Contacto.reconstitute(
                ContactoId.from(UUID.fromString("99999999-9999-9999-9999-999999999999")),
                EmpresaId.from(UUID.randomUUID()),
                UsuarioId.from(UUID.fromString("77777777-7777-7777-7777-777777777777")),
                UsuarioId.from(UUID.fromString("66666666-6666-6666-6666-666666666666")),
                "Renamed Contact",
                "renamed@example.com",
                "+525500000001",
                "VP Sales",
                "Referral",
                LocalDateTime.now(),
                LocalDateTime.now(),
                EstadoRelacion.ACTIVO);

        EditContactOutput output = CrmToolMapper.toEditContactOutput(contact);

        assertThat(output.id()).isEqualTo("99999999-9999-9999-9999-999999999999");
        assertThat(output.nombre()).isEqualTo("Renamed Contact");
        assertThat(output.correo()).isEqualTo("renamed@example.com");
        assertThat(output.estadoRelacion()).isEqualTo("ACTIVO");
        assertThat(output.responsableId()).isEqualTo("77777777-7777-7777-7777-777777777777");
        assertThat(output.telefono()).isEqualTo("+525500000001");
        assertThat(output.cargo()).isEqualTo("VP Sales");
        assertThat(output.comoNosConocio()).isEqualTo("Referral");
    }

    @Test
    void editContactProjectsNullDomainEntityToAllNulls() {
        EditContactOutput output = CrmToolMapper.toEditContactOutput(null);
        assertThat(output.id()).isNull();
        assertThat(output.nombre()).isNull();
        assertThat(output.correo()).isNull();
        assertThat(output.estadoRelacion()).isNull();
        assertThat(output.responsableId()).isNull();
        assertThat(output.telefono()).isNull();
        assertThat(output.cargo()).isNull();
        assertThat(output.comoNosConocio()).isNull();
    }

    @Test
    void findCompaniesProjectsDomainEntitiesToBoundedOutputRecords() {
        Empresa c1 = Empresa.create(
                "Acme", "Software", null, null, null, null, null,
                EstadoRelacion.PROSPECTO,
                null, null, null);
        Empresa c2 = Empresa.create(
                "Beta", "Retail", null, null, null, null, null,
                EstadoRelacion.ACTIVO,
                UsuarioId.from(UUID.fromString("77777777-7777-7777-7777-777777777777")),
                null, null);

        FindCompaniesOutput output = CrmToolMapper.toFindCompaniesOutput(List.of(c1, c2));

        assertThat(output.companies()).hasSize(2);
        assertThat(output.companies().get(0).nombre()).isEqualTo("Acme");
        assertThat(output.companies().get(0).estadoRelacion()).isEqualTo("PROSPECTO");
        assertThat(output.companies().get(0).sector()).isEqualTo("Software");
        assertThat(output.companies().get(1).nombre()).isEqualTo("Beta");
        assertThat(output.companies().get(1).estadoRelacion()).isEqualTo("ACTIVO");
        assertThat(output.companies().get(1).responsableId())
                .isEqualTo("77777777-7777-7777-7777-777777777777");
    }

    @Test
    void findCompaniesMapsEmptyAndNullListToEmptyOutput() {
        assertThat(CrmToolMapper.toFindCompaniesOutput(List.of()).companies()).isEmpty();
        assertThat(CrmToolMapper.toFindCompaniesOutput(null).companies()).isEmpty();
    }

    @Test
    void createCompanyProjectsDomainEntityToBoundedOutput() {
        Empresa company = Empresa.create(
                "Acme", "Software", null, null, null, null, null,
                EstadoRelacion.ACTIVO, null, null, null);

        CreateCompanyOutput output = CrmToolMapper.toCreateCompanyOutput(company);

        assertThat(output.id()).isEqualTo(String.valueOf(company.getId().value()));
        assertThat(output.nombre()).isEqualTo("Acme");
        assertThat(output.sector()).isEqualTo("Software");
        assertThat(output.estadoRelacion()).isEqualTo("ACTIVO");
        assertThat(output.responsableId()).isNull();
    }

    @Test
    void editCompanyProjectsDomainEntityToBoundedOutputAndStripsAuditAndSocialFields() {
        Empresa company = Empresa.reconstitute(
                EmpresaId.from(UUID.fromString("88888888-8888-8888-8888-888888888888")),
                "Renamed Co", "Software", "+525500000001",
                "https://renamed.example",
                "https://facebook.com/renamed",
                "https://instagram.com/renamed",
                "https://twitter.com/renamed",
                EstadoRelacion.ACTIVO,
                UsuarioId.from(UUID.fromString("77777777-7777-7777-7777-777777777777")),
                UsuarioId.from(UUID.fromString("66666666-6666-6666-6666-666666666666")),
                "notes",
                LocalDateTime.now(),
                LocalDateTime.now());

        EditCompanyOutput output = CrmToolMapper.toEditCompanyOutput(company);

        assertThat(output.id()).isEqualTo("88888888-8888-8888-8888-888888888888");
        assertThat(output.nombre()).isEqualTo("Renamed Co");
        assertThat(output.sector()).isEqualTo("Software");
        assertThat(output.estadoRelacion()).isEqualTo("ACTIVO");
        assertThat(output.responsableId()).isEqualTo("77777777-7777-7777-7777-777777777777");
        assertThat(output.paginaWeb()).isEqualTo("https://renamed.example");
        assertThat(output.telefono()).isEqualTo("+525500000001");
        assertThat(output.notas()).isEqualTo("notes");
    }

    @Test
    void editCompanyProjectsNullDomainEntityToAllNulls() {
        EditCompanyOutput output = CrmToolMapper.toEditCompanyOutput(null);
        assertThat(output.id()).isNull();
        assertThat(output.nombre()).isNull();
        assertThat(output.sector()).isNull();
        assertThat(output.estadoRelacion()).isNull();
        assertThat(output.responsableId()).isNull();
        assertThat(output.paginaWeb()).isNull();
        assertThat(output.telefono()).isNull();
        assertThat(output.notas()).isNull();
    }

    @Test
    void editTratoProjectsDomainEntityToBoundedOutputAndStripsStageAndLossReason() {
        Trato updated = Trato.reconstitute(
                TratoId.from(UUID.fromString("99999999-9999-9999-9999-999999999999")),
                ContactoId.create(),
                UsuarioId.from(UUID.fromString("77777777-7777-7777-7777-777777777777")),
                "Renamed Deal",
                new BigDecimal("1500.00"),
                75,
                LocalDate.parse("2026-12-31"),
                TipoContrato.SERVICIO,
                null,
                null,
                LocalDateTime.now(),
                null);

        EditTratoOutput output = CrmToolMapper.toEditTratoOutput(updated);

        assertThat(output.id()).isEqualTo("99999999-9999-9999-9999-999999999999");
        assertThat(output.nombre()).isEqualTo("Renamed Deal");
        assertThat(output.responsableId()).isEqualTo("77777777-7777-7777-7777-777777777777");
        assertThat(output.valorEstimado()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(output.probabilidad()).isEqualTo(75);
        assertThat(output.fechaCierreEsperada()).isEqualTo("2026-12-31");
        assertThat(output.tipoContrato()).isEqualTo("SERVICIO");
    }

    @Test
    void editTratoProjectsNullDomainEntityToAllNulls() {
        EditTratoOutput output = CrmToolMapper.toEditTratoOutput(null);
        assertThat(output.id()).isNull();
        assertThat(output.nombre()).isNull();
        assertThat(output.responsableId()).isNull();
        assertThat(output.valorEstimado()).isNull();
        assertThat(output.probabilidad()).isNull();
        assertThat(output.fechaCierreEsperada()).isNull();
        assertThat(output.tipoContrato()).isNull();
    }
}