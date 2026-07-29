package com.ar.crm2.application.contacto.service;

import com.ar.crm2.application.contacto.command.GetAllContactosCommand;
import com.ar.crm2.application.contacto.port.out.SearchContactosPort;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GetAllContactosServiceTest {

    private static final UUID ACTOR = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void getAll_convertsAllOptionalFiltersPlusActorAndDelegatesToScopedPort() {
        List<Contacto> expected = List.of();
        UUID empresaId = UUID.randomUUID();
        UUID responsableId = UUID.randomUUID();
        CapturingSearchPort port = new CapturingSearchPort(expected);
        GetAllContactosService service = new GetAllContactosService(port);

        List<Contacto> result = service.getAll(new GetAllContactosCommand(
                ACTOR,
                " Alice ",
                "ACTIVO",
                empresaId,
                responsableId,
                " LinkedIn ",
                20
        ));

        assertSame(expected, result);
        assertEquals(UsuarioId.from(ACTOR), port.actorUsuarioId,
                "Service must convert the trusted actor UUID to UsuarioId and "
                        + "pass it as the mandatory scope parameter to the port");
        assertEquals("Alice", port.search);
        assertEquals(EstadoRelacion.ACTIVO, port.estadoRelacion);
        assertEquals(EmpresaId.from(empresaId), port.empresaId);
        assertEquals(UsuarioId.from(responsableId), port.responsableId,
                "responsableId is an OPTIONAL filter and must be converted to UsuarioId "
                        + "independent of the mandatory actor scope");
        assertEquals("LinkedIn", port.comoNosConocio);
        assertEquals(20, port.maxResults);
    }

    @Test
    void getAll_passesActorAsFirstPortParameterSeparateFromOptionalResponsableId() {
        UUID actor = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID responsableId = UUID.randomUUID();
        CapturingSearchPort port = new CapturingSearchPort(List.of());
        GetAllContactosService service = new GetAllContactosService(port);

        service.getAll(new GetAllContactosCommand(
                actor, null, null, null, responsableId, null, null));

        assertEquals(UsuarioId.from(actor), port.actorUsuarioId,
                "actor scope must be a distinct, mandatory first port parameter");
        assertEquals(UsuarioId.from(responsableId), port.responsableId,
                "responsableId must be passed as the separate, optional filter");
    }

    @Test
    void getAll_passesNullResponsableIdWhenAbsent() {
        CapturingSearchPort port = new CapturingSearchPort(List.of());
        GetAllContactosService service = new GetAllContactosService(port);

        service.getAll(new GetAllContactosCommand(
                ACTOR, null, null, null, null, null, null));

        assertEquals(UsuarioId.from(ACTOR), port.actorUsuarioId);
        assertNull(port.responsableId,
                "omitted responsableId must remain an absent (null) filter");
    }

    @Test
    void getAll_normalizesBlankAndWhitespaceEstadoRelacionToAbsentFilter() {
        CapturingSearchPort port = new CapturingSearchPort(List.of());
        GetAllContactosService service = new GetAllContactosService(port);

        List<Contacto> result = service.getAll(new GetAllContactosCommand(
                ACTOR,
                "Alice",
                "   ",
                null,
                null,
                null,
                null
        ));

        assertEquals(List.of(), result);
        assertNull(port.estadoRelacion);
    }

    @Test
    void getAll_normalizesTabAndNewlineEstadoRelacionToAbsentFilter() {
        CapturingSearchPort port = new CapturingSearchPort(List.of());
        GetAllContactosService service = new GetAllContactosService(port);

        service.getAll(new GetAllContactosCommand(
                ACTOR,
                "Alice",
                "\t\n",
                null,
                null,
                null,
                null
        ));

        assertNull(port.estadoRelacion);
    }

    @Test
    void getAll_preservesUnfilteredResultsWithoutApplicationCap() {
        Contacto contacto = Contacto.create(
                EmpresaId.create(),
                "Alice",
                null,
                EstadoRelacion.ACTIVO,
                null,
                null,
                null,
                null,
                null);
        List<Contacto> expected = Collections.nCopies(25, contacto);
        CapturingSearchPort port = new CapturingSearchPort(expected);
        GetAllContactosService service = new GetAllContactosService(port);

        List<Contacto> result = service.getAll(new GetAllContactosCommand(
                ACTOR, null, null, null, null, null, null));

        assertEquals(25, result.size());
        assertEquals(UsuarioId.from(ACTOR), port.actorUsuarioId);
        assertNull(port.search);
        assertNull(port.estadoRelacion);
        assertNull(port.empresaId);
        assertNull(port.responsableId);
        assertNull(port.comoNosConocio);
        assertNull(port.maxResults);
    }

    private static final class CapturingSearchPort implements SearchContactosPort {
        private final List<Contacto> result;
        private UsuarioId actorUsuarioId;
        private String search;
        private EstadoRelacion estadoRelacion;
        private EmpresaId empresaId;
        private UsuarioId responsableId;
        private String comoNosConocio;
        private Integer maxResults;

        private CapturingSearchPort(List<Contacto> result) {
            this.result = result;
        }

        @Override
        public List<Contacto> search(
                UsuarioId actorUsuarioId,
                String search,
                EstadoRelacion estadoRelacion,
                EmpresaId empresaId,
                UsuarioId responsableId,
                String comoNosConocio,
                Integer maxResults
        ) {
            this.actorUsuarioId = actorUsuarioId;
            this.search = search;
            this.estadoRelacion = estadoRelacion;
            this.empresaId = empresaId;
            this.responsableId = responsableId;
            this.comoNosConocio = comoNosConocio;
            this.maxResults = maxResults;
            return result;
        }
    }

}
