package com.ar.crm2.adapter.out.ai.tool.dto.output;

import java.math.BigDecimal;
import java.util.List;

/** Bounded, model-visible output contracts for CRM resource tools. */
public final class ResourceToolOutput {

    private ResourceToolOutput() {
    }

    public record DeleteResult(String resource, String id, boolean deleted) {
    }

    public record Contact(String id, String nombre, String estadoRelacion) {
    }

    public record Contacts(List<Contact> contacts, int total, boolean truncated) {
        public Contacts {
            contacts = contacts == null ? List.of() : List.copyOf(contacts);
        }
    }

    public record Company(String id, String nombre, String sector, String estadoRelacion) {
    }

    public record Companies(List<Company> companies, int total, boolean truncated) {
        public Companies {
            companies = companies == null ? List.of() : List.copyOf(companies);
        }
    }

    public record Deal(String id, String nombre, String estado, String contactoId,
                       String tipoContrato, BigDecimal valorEstimado, Integer probabilidad,
                       String fechaCierreEsperada) {
    }

    public record Deals(List<Deal> deals, int total, boolean truncated) {
        public Deals {
            deals = deals == null ? List.of() : List.copyOf(deals);
        }
    }

    public record Task(String id, String titulo, String tipo, String prioridad,
                       String fechaLimite, String tratoId) {
    }

    public record Tasks(List<Task> tasks, int total, boolean truncated) {
        public Tasks {
            tasks = tasks == null ? List.of() : List.copyOf(tasks);
        }
    }

    public record Label(String id, String nombre, String tipoEtiqueta, String color) {
    }

    public record Labels(List<Label> labels, int total, boolean truncated) {
        public Labels {
            labels = labels == null ? List.of() : List.copyOf(labels);
        }
    }

    public record AgendaItem(String id, String tipo, String asunto, String fecha,
                             String horaInicio, String horaFin, String tareaId, String tratoId) {
    }

    public record AgendaItems(List<AgendaItem> agendas, int total, boolean truncated) {
        public AgendaItems {
            agendas = agendas == null ? List.of() : List.copyOf(agendas);
        }
    }
}
