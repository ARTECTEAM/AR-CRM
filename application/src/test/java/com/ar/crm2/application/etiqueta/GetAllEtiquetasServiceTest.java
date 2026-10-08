package com.ar.crm2.application.etiqueta;

import com.ar.crm2.application.etiqueta.port.out.FindAllEtiquetasPort;
import com.ar.crm2.application.etiqueta.service.GetAllEtiquetasService;
import com.ar.crm2.application.support.TestCrmAuthorization;
import com.ar.crm2.model.entity.Etiqueta;
import com.ar.crm2.model.enums.TipoEtiqueta;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetAllEtiquetasServiceTest {

    @Test
    void getAll_returnsAllWhenNoFilter() {
        List<Etiqueta> all = List.of(
            Etiqueta.create("Urgent", TipoEtiqueta.TAREA, "#FF0000"),
            Etiqueta.create("Premium", TipoEtiqueta.TRATO, "#0000FF")
        );
        GetAllEtiquetasService service = new GetAllEtiquetasService(new InMemoryFindAll(all), new TestCrmAuthorization());

        List<Etiqueta> result = service.getAll(Optional.empty());

        assertEquals(2, result.size());
    }

    @Test
    void getAll_filtersByTipo() {
        List<Etiqueta> tareas = List.of(
            Etiqueta.create("Urgent", TipoEtiqueta.TAREA, "#FF0000")
        );
        GetAllEtiquetasService service = new GetAllEtiquetasService(new InMemoryFindAll(tareas), new TestCrmAuthorization());

        List<Etiqueta> result = service.getAll(Optional.of(TipoEtiqueta.TAREA));

        assertEquals(1, result.size());
        assertEquals(TipoEtiqueta.TAREA, result.get(0).getTipoEtiqueta());
    }

    private static final class InMemoryFindAll implements FindAllEtiquetasPort {
        private final List<Etiqueta> etiquetas;

        InMemoryFindAll(List<Etiqueta> etiquetas) {
            this.etiquetas = etiquetas;
        }

        @Override
        public List<Etiqueta> findAll(Optional<TipoEtiqueta> tipoEtiqueta) {
            return tipoEtiqueta
                .map(t -> etiquetas.stream().filter(e -> e.getTipoEtiqueta().equals(t)).toList())
                .orElse(etiquetas);
        }
    }
}
