package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.ContactoEntity;
import com.ar.crm2.adapter.out.persistence.mapper.ContactoMapper;
import com.ar.crm2.adapter.out.persistence.repository.ContactoRepository;
import com.ar.crm2.application.contacto.port.out.DeleteContactoByIdPort;
import com.ar.crm2.application.contacto.port.out.ExistsTratosByContactoIdPort;
import com.ar.crm2.application.contacto.port.out.FindContactoByIdPort;
import com.ar.crm2.application.contacto.port.out.SaveContactoPort;
import com.ar.crm2.application.contacto.port.out.SearchContactosPort;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ContactoRepositoryAdapter implements SaveContactoPort, SearchContactosPort, FindContactoByIdPort, DeleteContactoByIdPort, ExistsTratosByContactoIdPort {

    private final ContactoRepository repository;

    @Override
    public Contacto save(Contacto contacto) {
        ContactoEntity entity = ContactoMapper.toEntity(contacto);
        ContactoEntity saved = repository.save(entity);
        return ContactoMapper.toDomain(saved);
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
        // The deterministic order (creadoEn DESC, id ASC) is fixed in the
        // repository @Query; the Pageable here only contributes the
        // database-level LIMIT when a cap is requested. Pageable.unpaged()
        // applies neither ORDER BY (the JPQL already has it) nor LIMIT.
        Pageable pageable = maxResults == null
                ? Pageable.unpaged()
                : PageRequest.ofSize(maxResults);

        List<ContactoEntity> entities = repository.searchScoped(
                actorUsuarioId.value().toString(),
                escapeLikePattern(search),
                estadoRelacion,
                empresaId != null ? empresaId.value().toString() : null,
                responsableId != null ? responsableId.value().toString() : null,
                comoNosConocio,
                pageable
        );

        return entities.stream()
                .map(ContactoMapper::toDomain)
                .toList();
    }

    private static String escapeLikePattern(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    @Override
    public Optional<Contacto> findById(ContactoId id) {
        return repository.findById(id.value().toString())
            .map(ContactoMapper::toDomain);
    }

    @Override
    public void deleteById(ContactoId id) {
        repository.deleteById(id.value().toString());
    }

    @Override
    public boolean existsTratosByContactoId(ContactoId contactoId) {
        return repository.existsTratosByContactoId(contactoId.value().toString());
    }
}
