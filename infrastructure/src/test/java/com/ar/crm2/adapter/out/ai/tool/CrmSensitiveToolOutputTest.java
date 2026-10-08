package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.EditCompanyOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.EditContactOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.ResourceToolOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.TablerosOutput;
import com.ar.crm2.application.contacto.port.in.CambiarEstadoContactoUseCase;
import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.contacto.port.in.DeleteContactoUseCase;
import com.ar.crm2.application.contacto.port.in.EditContactoUseCase;
import com.ar.crm2.application.contacto.port.in.GetContactoByIdUseCase;
import com.ar.crm2.application.contacto.port.in.SearchContactosForActorUseCase;
import com.ar.crm2.application.empresa.port.in.CambiarEstadoEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.CreateEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.DeleteEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.EditEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase;
import com.ar.crm2.application.tablero.port.in.AsignarColumnaTableroUseCase;
import com.ar.crm2.application.tablero.port.in.CreateTableroUseCase;
import com.ar.crm2.application.tablero.port.in.DeleteTableroUseCase;
import com.ar.crm2.application.tablero.port.in.EditTableroUseCase;
import com.ar.crm2.application.tablero.port.in.EliminarColumnaDelTableroUseCase;
import com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase;
import com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase;
import com.ar.crm2.application.tablero.port.in.ReordenarColumnasUseCase;
import com.ar.crm2.application.trato.port.in.CreateTratoUseCase;
import com.ar.crm2.application.trato.port.in.DeleteTratoUseCase;
import com.ar.crm2.application.trato.port.in.EditTratoUseCase;
import com.ar.crm2.application.trato.port.in.GetAllTratosUseCase;
import com.ar.crm2.application.trato.port.in.GetTratoByIdUseCase;
import com.ar.crm2.model.entity.ColumnaTablero;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.entity.Empresa;
import com.ar.crm2.model.entity.Tablero;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.TableroId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrmSensitiveToolOutputTest {

    private static final UUID ACTOR = UUID.randomUUID();

    @Test
    void listTratosMasksFinancialFieldsBeforeReturningModelVisibleToolResult() {
        Trato trato = Trato.create(
                ContactoId.create(), UsuarioId.from(ACTOR), "Confidential deal",
                new BigDecimal("87500.00"), 73, LocalDate.of(2027, 4, 12),
                com.ar.crm2.model.enums.TipoContrato.values()[0]);
        GetAllTratosUseCase getAll = mock(GetAllTratosUseCase.class);
        when(getAll.getAll()).thenReturn(List.of(trato));

        TratoTools tools = new TratoTools(
                mock(CreateTratoUseCase.class), getAll,
                mock(GetTratoByIdUseCase.class), mock(EditTratoUseCase.class),
                mock(DeleteTratoUseCase.class), projector());

        ResourceToolOutput.Deal result = tools.listTratos(actorContext()).deals().getFirst();

        assertThat(result.valorEstimado()).isNull();
        assertThat(result.probabilidad()).isNull();
        assertThat(result.fechaCierreEsperada()).isNull();
    }

    @Test
    void editContactMasksPrivateFieldsBeforeReturningModelVisibleToolResult() {
        Contacto contact = Contacto.create(EmpresaId.create(), "Private contact", "private@example.test",
                EstadoRelacion.PROSPECTO, null, UsuarioId.from(ACTOR), "+525500000001", "Director", "Referral");
        GetContactoByIdUseCase getById = mock(GetContactoByIdUseCase.class);
        EditContactoUseCase edit = mock(EditContactoUseCase.class);
        when(getById.getById(any())).thenReturn(contact);
        when(edit.edit(any())).thenReturn(contact);

        ContactoTools tools = new ContactoTools(mock(SearchContactosForActorUseCase.class),
                mock(CreateContactoUseCase.class), edit, getById, mock(DeleteContactoUseCase.class),
                mock(CambiarEstadoContactoUseCase.class), projector());
        EditContactOutput result = tools.editContact(contact.getId().value(), "Private contact", null,
                EstadoRelacion.PROSPECTO, null, null, null, null, actorContext());

        assertThat(result.correo()).isNull();
        assertThat(result.telefono()).isNull();
    }

    @Test
    void editCompanyMasksPrivateFieldsBeforeReturningModelVisibleToolResult() {
        Empresa company = Empresa.create("Private company", "Software", "+525500000002", null,
                null, null, null, EstadoRelacion.ACTIVO, UsuarioId.from(ACTOR), UsuarioId.from(ACTOR),
                "confidential notes");
        GetAllEmpresasUseCase getAll = mock(GetAllEmpresasUseCase.class);
        EditEmpresaUseCase edit = mock(EditEmpresaUseCase.class);
        when(getAll.getAll()).thenReturn(List.of(company));
        when(edit.edit(any())).thenReturn(company);

        EmpresaTools tools = new EmpresaTools(mock(CreateEmpresaUseCase.class), getAll, edit,
                mock(DeleteEmpresaUseCase.class), mock(CambiarEstadoEmpresaUseCase.class), projector());
        EditCompanyOutput result = tools.editCompany(company.getId().value(), "Private company", null,
                null, null, null, null, null, null, null, null, actorContext());

        assertThat(result.telefono()).isNull();
        assertThat(result.notas()).isNull();
    }

    @Test
    void listTablerosMasksFinancialTotalsBeforeReturningModelVisibleToolResult() {
        ColumnaTablero assignment = ColumnaTablero.reconstitute(new ColumnaId(UUID.randomUUID()),
                TipoTablero.TRATOS, 10, null, new BigDecimal("130000.00"));
        Tablero tablero = Tablero.reconstitute(TableroId.create(), "Pipeline", "Deals",
                List.of(assignment), TipoTablero.TRATOS, LocalDateTime.now());
        GetAllTablerosUseCase getAll = mock(GetAllTablerosUseCase.class);
        when(getAll.getAll()).thenReturn(List.of(tablero));

        TableroTools tools = new TableroTools(getAll, mock(GetTableroByIdUseCase.class),
                mock(CreateTableroUseCase.class), mock(EditTableroUseCase.class),
                mock(DeleteTableroUseCase.class), mock(EliminarColumnaDelTableroUseCase.class),
                mock(AsignarColumnaTableroUseCase.class), mock(ReordenarColumnasUseCase.class), projector());
        TablerosOutput result = tools.listTableros(actorContext());

        assertThat(result.tableros().getFirst().columnas().getFirst().totalValorEstimado()).isNull();
    }

    private static CrmToolOutputProjector projector() {
        var authorization = mock(com.ar.crm2.application.security.CrmAuthorization.class);
        when(authorization.fieldPolicy(any())).thenReturn(new com.ar.crm2.application.security.ResourceReadPolicy(
                com.ar.crm2.model.autorizacion.AlcanceCrm.TODO_COMPARTIDO,
                java.util.Set.of(), java.util.Set.of(), java.util.Set.of()));
        return new CrmToolOutputProjector(authorization);
    }
    private static ToolContext actorContext() {
        return new ToolContext(Map.of("actorUsuarioId", ACTOR));
    }
}