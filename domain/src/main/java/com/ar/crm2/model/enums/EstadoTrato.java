package com.ar.crm2.model.enums;

/**
 * Current lifecycle state of a {@code Trato} (deal/opportunity).
 *
 * <ul>
 *   <li>{@link #ABIERTO}: deal is active in the pipeline.</li>
 *   <li>{@link #CERRADO}: deal is closed (won or lost — bookkeeping detail is not
 *       carried on the Trato aggregate; the CRM is not the source of truth for
 *       won/lost detail).</li>
 * </ul>
 */
public enum EstadoTrato {
    ABIERTO,
    CERRADO
}
