package com.ar.crm2.config;

import com.ar.crm2.adapter.out.persistence.ColumnaRepositoryAdapter;
import com.ar.crm2.adapter.out.persistence.repository.ColumnaRepository;
import com.ar.crm2.adapter.out.persistence.repository.ContactoRepository;
import com.ar.crm2.adapter.out.persistence.ContactoRepositoryAdapter;
import com.ar.crm2.adapter.out.persistence.EmpresaRepositoryAdapter;
import com.ar.crm2.adapter.out.persistence.repository.EmpresaRepository;
import com.ar.crm2.adapter.out.persistence.repository.FichaRepository;
import com.ar.crm2.adapter.out.persistence.FichaRepositoryAdapter;
import com.ar.crm2.adapter.out.persistence.repository.RolRepository;
import com.ar.crm2.adapter.out.persistence.RolRepositoryAdapter;
import com.ar.crm2.adapter.out.persistence.repository.SuperUsuarioRepository;
import com.ar.crm2.adapter.out.persistence.SuperUsuarioRepositoryAdapter;
import com.ar.crm2.adapter.out.persistence.repository.TableroRepository;
import com.ar.crm2.adapter.out.persistence.TableroRepositoryAdapter;
import com.ar.crm2.adapter.out.persistence.repository.TratoRepository;
import com.ar.crm2.adapter.out.persistence.TratoRepositoryAdapter;
import com.ar.crm2.adapter.out.persistence.repository.TareaRepository;
import com.ar.crm2.adapter.out.persistence.TareaRepositoryAdapter;
import com.ar.crm2.adapter.out.persistence.repository.UsuarioRepository;
import com.ar.crm2.adapter.out.persistence.UsuarioRepositoryAdapter;
import com.ar.crm2.adapter.out.persistence.mapper.TableroMapper;
import com.ar.crm2.adapter.out.keycloak.KeycloakUserProvisioningAdapter;
import com.ar.crm2.config.KeycloakAdminProperties;
import com.ar.crm2.application.columna.port.in.CreateColumnaUseCase;
import com.ar.crm2.application.columna.port.in.DeleteColumnaUseCase;
import com.ar.crm2.application.columna.port.in.EditColumnaUseCase;
import com.ar.crm2.application.columna.port.in.GetAllColumnasUseCase;
import com.ar.crm2.application.columna.port.in.GetColumnaByIdUseCase;
import com.ar.crm2.application.columna.port.out.DeleteColumnaByIdPort;
import com.ar.crm2.application.columna.port.out.FindAllColumnasPort;
import com.ar.crm2.application.columna.port.out.FindColumnaByIdPort;
import com.ar.crm2.application.columna.port.out.SaveColumnaPort;
import com.ar.crm2.application.columna.service.CreateColumnaService;
import com.ar.crm2.application.columna.service.DeleteColumnaService;
import com.ar.crm2.application.columna.service.EditColumnaService;
import com.ar.crm2.application.columna.service.GetAllColumnasService;
import com.ar.crm2.application.columna.service.GetColumnaByIdService;
import com.ar.crm2.application.identity.port.out.DeleteIdentityPort;
import com.ar.crm2.application.identity.port.out.ProvisionIdentityPort;
import com.ar.crm2.application.identity.port.out.SetIdentityEnabledPort;
import com.ar.crm2.application.identity.port.out.SendIdentityUpdatePasswordEmailPort;
import com.ar.crm2.application.identity.port.out.SetIdentityAttributesPort;
import com.ar.crm2.application.identity.port.out.SyncIdentityEmailPort;
import com.ar.crm2.application.superusuario.port.in.CreateSuperUsuarioUseCase;
import com.ar.crm2.application.superusuario.port.in.DeleteSuperUsuarioUseCase;
import com.ar.crm2.application.superusuario.port.in.EditSuperUsuarioUseCase;
import com.ar.crm2.application.superusuario.port.in.GetAllSuperUsuariosUseCase;
import com.ar.crm2.application.superusuario.port.in.GetSuperUsuarioByIdUseCase;
import com.ar.crm2.application.superusuario.port.out.DeleteSuperUsuarioByIdPort;
import com.ar.crm2.application.superusuario.port.out.FindAllSuperUsuariosPort;
import com.ar.crm2.application.superusuario.port.out.FindSuperUsuarioByIdPort;
import com.ar.crm2.application.superusuario.port.out.FindSuperUsuarioByKeycloakIdPort;
import com.ar.crm2.application.superusuario.port.out.SaveSuperUsuarioPort;
import com.ar.crm2.application.superusuario.service.CreateSuperUsuarioService;
import com.ar.crm2.application.superusuario.service.DeleteSuperUsuarioService;
import com.ar.crm2.application.superusuario.service.EditSuperUsuarioService;
import com.ar.crm2.application.superusuario.service.GetAllSuperUsuariosService;
import com.ar.crm2.application.superusuario.service.GetSuperUsuarioByIdService;
import com.ar.crm2.application.rol.port.in.CreateRolUseCase;
import com.ar.crm2.application.rol.port.in.DeleteRolUseCase;
import com.ar.crm2.application.rol.port.in.EditRolUseCase;
import com.ar.crm2.application.rol.port.in.GetAllRolesUseCase;
import com.ar.crm2.application.rol.port.in.GetRolByIdUseCase;
import com.ar.crm2.application.rol.port.out.DeleteRolByIdPort;
import com.ar.crm2.application.rol.port.out.ExistsUsuariosByRolIdPort;
import com.ar.crm2.application.rol.port.out.FindAllRolesPort;
import com.ar.crm2.application.rol.port.out.FindRolByIdPort;
import com.ar.crm2.application.rol.port.out.SaveRolPort;
import com.ar.crm2.application.rol.service.CreateRolService;
import com.ar.crm2.application.rol.service.DeleteRolService;
import com.ar.crm2.application.rol.service.EditRolService;
import com.ar.crm2.application.rol.service.GetAllRolesService;
import com.ar.crm2.application.rol.service.GetRolByIdService;
import com.ar.crm2.application.usuario.port.in.CreateUsuarioUseCase;
import com.ar.crm2.application.usuario.port.in.DeleteUsuarioUseCase;
import com.ar.crm2.application.usuario.port.in.EditUsuarioUseCase;
import com.ar.crm2.application.usuario.port.in.ForgotPasswordUseCase;
import com.ar.crm2.application.usuario.port.in.GetAllUsuariosUseCase;
import com.ar.crm2.application.usuario.port.in.GetUsuarioByIdUseCase;
import com.ar.crm2.application.usuario.port.in.RequestPasswordChangeUseCase;
import com.ar.crm2.application.usuario.port.out.DeleteUsuarioByIdPort;
import com.ar.crm2.application.usuario.port.out.FindAllUsuariosPort;
import com.ar.crm2.application.usuario.port.out.FindUsuarioByCorreoPort;
import com.ar.crm2.application.usuario.port.out.FindUsuarioByIdPort;
import com.ar.crm2.application.usuario.port.out.FindUsuarioByKeycloakIdPort;
import com.ar.crm2.application.usuario.port.out.SaveUsuarioPort;
import com.ar.crm2.application.usuario.service.CreateUsuarioService;
import com.ar.crm2.application.usuario.service.DeleteUsuarioService;
import com.ar.crm2.application.usuario.service.EditUsuarioService;
import com.ar.crm2.application.usuario.service.ForgotPasswordService;
import com.ar.crm2.application.usuario.service.GetAllUsuariosService;
import com.ar.crm2.application.usuario.service.GetUsuarioByIdService;
import com.ar.crm2.application.usuario.service.RequestPasswordChangeService;
import com.ar.crm2.application.contacto.port.in.CambiarEstadoContactoUseCase;
import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.contacto.port.in.DeleteContactoUseCase;
import com.ar.crm2.application.contacto.port.in.EditContactoUseCase;
import com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase;
import com.ar.crm2.application.contacto.port.in.GetContactoByIdUseCase;
import com.ar.crm2.application.contacto.port.out.DeleteContactoByIdPort;
import com.ar.crm2.application.contacto.port.out.FindContactoByIdPort;
import com.ar.crm2.application.contacto.port.out.SaveContactoPort;
import com.ar.crm2.application.contacto.service.CambiarEstadoContactoService;
import com.ar.crm2.application.contacto.service.CreateContactoService;
import com.ar.crm2.application.contacto.service.DeleteContactoService;
import com.ar.crm2.application.contacto.service.EditContactoService;
import com.ar.crm2.application.contacto.service.GetAllContactosService;
import com.ar.crm2.application.contacto.service.GetContactoByIdService;
import com.ar.crm2.application.empresa.port.in.CambiarEstadoEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.CreateEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.DeleteEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.EditEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase;
import com.ar.crm2.application.empresa.port.out.DeleteEmpresaByIdPort;
import com.ar.crm2.application.empresa.port.out.FindAllEmpresasPort;
import com.ar.crm2.application.empresa.port.out.FindEmpresaByIdPort;
import com.ar.crm2.application.empresa.port.out.SaveEmpresaPort;
import com.ar.crm2.application.empresa.service.CambiarEstadoEmpresaService;
import com.ar.crm2.application.empresa.service.CreateEmpresaService;
import com.ar.crm2.application.empresa.service.DeleteEmpresaService;
import com.ar.crm2.application.empresa.service.EditEmpresaService;
import com.ar.crm2.application.empresa.service.GetAllEmpresasService;
import com.ar.crm2.application.trato.port.in.CreateTratoUseCase;
import com.ar.crm2.application.trato.port.in.DeleteTratoUseCase;
import com.ar.crm2.application.trato.port.in.EditTratoUseCase;
import com.ar.crm2.application.trato.port.in.GetAllTratosUseCase;
import com.ar.crm2.application.trato.port.in.GetTratoByIdUseCase;
import com.ar.crm2.application.trato.port.out.DeleteTratoByIdPort;
import com.ar.crm2.application.trato.port.out.FindAllTratosPort;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.application.trato.port.out.SaveTratoPort;
import com.ar.crm2.application.trato.service.CreateTratoService;
import com.ar.crm2.application.trato.service.DeleteTratoService;
import com.ar.crm2.application.trato.service.EditTratoService;
import com.ar.crm2.application.trato.service.GetAllTratosService;
import com.ar.crm2.application.trato.service.GetTratoByIdService;
import com.ar.crm2.application.tarea.port.in.CreateTareaUseCase;
import com.ar.crm2.application.tarea.port.in.DeleteTareaUseCase;
import com.ar.crm2.application.tarea.port.in.EditTareaUseCase;
import com.ar.crm2.application.tarea.port.in.GetAllTareasUseCase;
import com.ar.crm2.application.tarea.port.in.GetTareaByIdUseCase;
import com.ar.crm2.application.tarea.port.out.DeleteTareaByIdPort;
import com.ar.crm2.application.tarea.port.out.FindAllTareasPort;
import com.ar.crm2.application.tarea.port.out.FindTareaByIdPort;
import com.ar.crm2.application.tarea.port.out.SaveTareaPort;
import com.ar.crm2.application.tarea.service.CreateTareaService;
import com.ar.crm2.application.tarea.service.DeleteTareaService;
import com.ar.crm2.application.tarea.service.EditTareaService;
import com.ar.crm2.application.tarea.service.GetAllTareasService;
import com.ar.crm2.application.tarea.service.GetTareaByIdService;
import com.ar.crm2.application.agenda.port.in.CreateAgendaUseCase;
import com.ar.crm2.application.agenda.port.in.DeleteAgendaUseCase;
import com.ar.crm2.application.agenda.port.in.EditAgendaUseCase;
import com.ar.crm2.application.agenda.port.in.GetAgendaByIdUseCase;
import com.ar.crm2.application.agenda.port.in.GetAgendasByUserUseCase;
import com.ar.crm2.application.agenda.port.out.DeleteAgendaByIdPort;
import com.ar.crm2.application.agenda.port.out.FindAgendaByIdPort;
import com.ar.crm2.application.agenda.port.out.SaveAgendaPort;
import com.ar.crm2.application.agenda.service.CreateAgendaService;
import com.ar.crm2.application.agenda.service.DeleteAgendaService;
import com.ar.crm2.application.agenda.service.EditAgendaService;
import com.ar.crm2.application.agenda.service.GetAgendaByIdService;
import com.ar.crm2.application.agenda.service.GetAgendasByUserService;
import com.ar.crm2.adapter.out.persistence.AgendaRepositoryAdapter;
import com.ar.crm2.adapter.out.persistence.repository.AgendaRepository;
import com.ar.crm2.application.etiqueta.port.in.CreateEtiquetaUseCase;
import com.ar.crm2.application.etiqueta.port.in.DeleteEtiquetaUseCase;
import com.ar.crm2.application.etiqueta.port.in.EditEtiquetaUseCase;
import com.ar.crm2.application.etiqueta.port.in.GetAllEtiquetasUseCase;
import com.ar.crm2.application.etiqueta.port.in.GetEtiquetaByIdUseCase;
import com.ar.crm2.application.etiqueta.service.CreateEtiquetaService;
import com.ar.crm2.application.etiqueta.service.DeleteEtiquetaService;
import com.ar.crm2.application.etiqueta.service.EditEtiquetaService;
import com.ar.crm2.application.etiqueta.service.GetAllEtiquetasService;
import com.ar.crm2.application.etiqueta.service.GetEtiquetaByIdService;
import com.ar.crm2.application.ficha.port.in.CreateFichaUseCase;
import com.ar.crm2.application.ficha.port.in.DeleteFichaUseCase;
import com.ar.crm2.application.ficha.port.in.EditFichaUseCase;
import com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase;
import com.ar.crm2.application.ficha.port.in.GetFichaByIdUseCase;
import com.ar.crm2.application.ficha.port.in.MoverColumnaFichaUseCase;
import com.ar.crm2.application.ficha.port.out.DeleteFichaByIdPort;
import com.ar.crm2.application.ficha.port.out.ExistsFichasByColumnaIdPort;
import com.ar.crm2.application.ficha.port.out.FindAllFichasPort;
import com.ar.crm2.application.ficha.port.out.FindFichaByIdPort;
import com.ar.crm2.application.ficha.port.out.SaveFichaPort;
import com.ar.crm2.application.ficha.service.CreateFichaService;
import com.ar.crm2.application.ficha.service.DeleteFichaService;
import com.ar.crm2.application.ficha.service.EditFichaService;
import com.ar.crm2.application.ficha.service.GetAllFichasService;
import com.ar.crm2.application.ficha.service.GetFichaByIdService;
import com.ar.crm2.application.ficha.service.MoverColumnaFichaService;
import com.ar.crm2.application.tablero.port.in.AsignarColumnaTableroUseCase;
import com.ar.crm2.application.tablero.port.in.CreateTableroUseCase;
import com.ar.crm2.application.tablero.port.in.DeleteTableroUseCase;
import com.ar.crm2.application.tablero.port.in.EditTableroUseCase;
import com.ar.crm2.application.tablero.port.in.EliminarColumnaDelTableroUseCase;
import com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase;
import com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase;
import com.ar.crm2.application.tablero.port.in.ReordenarColumnasUseCase;
import com.ar.crm2.application.tablero.port.out.DeleteTableroByIdPort;
import com.ar.crm2.application.tablero.port.out.FindAllTablerosPort;
import com.ar.crm2.application.tablero.port.out.FindTableroByIdPort;
import com.ar.crm2.application.tablero.port.out.SaveTableroPort;
import com.ar.crm2.application.tablero.port.out.ExistsColumnaEnTableroPort;
import com.ar.crm2.application.tablero.service.AsignarColumnaTableroService;
import com.ar.crm2.application.tablero.service.CreateTableroService;
import com.ar.crm2.application.tablero.service.DeleteTableroService;
import com.ar.crm2.application.tablero.service.EditTableroService;
import com.ar.crm2.application.tablero.service.EliminarColumnaDelTableroService;
import com.ar.crm2.application.tablero.service.GetAllTablerosService;
import com.ar.crm2.application.tablero.service.GetTableroByIdService;
import com.ar.crm2.application.tablero.service.ReordenarColumnasService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Application wiring configuration.
 * Wires application UseCases with infrastructure adapters without touching application/domain classes.
 *
 * Dependency flow: boot -> infrastructure -> application -> domain
 * This configuration lives in boot because boot is the composition root and can import
 * infrastructure adapters plus application services without contaminating inner layers.
 */
@Configuration
public class WiringConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder() {
        return org.springframework.web.reactive.function.client.WebClient.builder();
    }

    // ── Infrastructure Adapter Beans (created explicitly to avoid component-scan duplication) ──

    @Bean
    public EmpresaRepositoryAdapter empresaRepositoryAdapter(EmpresaRepository repository) {
        return new EmpresaRepositoryAdapter(repository);
    }

    @Bean
    public ContactoRepositoryAdapter contactoRepositoryAdapter(ContactoRepository repository) {
        return new ContactoRepositoryAdapter(repository);
    }

    @Bean
    public com.ar.crm2.adapter.out.persistence.ContactoUpsertAdapter contactoUpsertAdapter(ContactoRepository repository) {
        return new com.ar.crm2.adapter.out.persistence.ContactoUpsertAdapter(repository);
    }

    @Bean
    public TableroRepositoryAdapter tableroRepositoryAdapter(
            TableroRepository tableroRepository,
            ColumnaRepository columnaRepository,
            TableroMapper tableroMapper
    ) {
        return new TableroRepositoryAdapter(tableroRepository, columnaRepository, tableroMapper);
    }

    @Bean
    public TratoRepositoryAdapter tratoRepositoryAdapter(TratoRepository repository) {
        return new TratoRepositoryAdapter(repository);
    }

    @Bean
    public TareaRepositoryAdapter tareaRepositoryAdapter(TareaRepository repository) {
        return new TareaRepositoryAdapter(repository);
    }

    @Bean
    public FichaRepositoryAdapter fichaRepositoryAdapter(FichaRepository repository) {
        return new FichaRepositoryAdapter(repository);
    }

    @Bean
    public RolRepositoryAdapter rolRepositoryAdapter(RolRepository repository) {
        return new RolRepositoryAdapter(repository);
    }

    @Bean
    public ColumnaRepositoryAdapter columnaRepositoryAdapter(ColumnaRepository repository, TableroRepository tableroRepository) {
        return new ColumnaRepositoryAdapter(repository, tableroRepository);
    }

    @Bean
    public UsuarioRepositoryAdapter usuarioRepositoryAdapter(UsuarioRepository repository) {
        return new UsuarioRepositoryAdapter(repository);
    }

    @Bean
    public SuperUsuarioRepositoryAdapter superUsuarioRepositoryAdapter(SuperUsuarioRepository repository) {
        return new SuperUsuarioRepositoryAdapter(repository);
    }

    @Bean
    public KeycloakUserProvisioningAdapter keycloakUserProvisioningAdapter(KeycloakAdminProperties props) {
        return new KeycloakUserProvisioningAdapter(props);
    }

    // ── Empresa UseCase Beans ──

    @Bean
    public CreateEmpresaUseCase createEmpresaUseCase(EmpresaRepositoryAdapter adapter) {
        return new CreateEmpresaService(adapter);
    }

    @Bean
    public GetAllEmpresasUseCase getAllEmpresasUseCase(EmpresaRepositoryAdapter adapter) {
        return new GetAllEmpresasService(adapter);
    }

    @Bean
    public EditEmpresaUseCase editEmpresaUseCase(EmpresaRepositoryAdapter findPort, EmpresaRepositoryAdapter savePort) {
        return new EditEmpresaService(findPort, savePort);
    }

    @Bean
    public DeleteEmpresaUseCase deleteEmpresaUseCase(
            EmpresaRepositoryAdapter findPort,
            EmpresaRepositoryAdapter existsTratosPort,
            EmpresaRepositoryAdapter deletePort
    ) {
        return new DeleteEmpresaService(findPort, existsTratosPort, deletePort);
    }

    @Bean
    public CambiarEstadoEmpresaUseCase cambiarEstadoEmpresaUseCase(
            EmpresaRepositoryAdapter findPort,
            EmpresaRepositoryAdapter savePort,
            EmpresaRepositoryAdapter existsTratosPort
    ) {
        return new CambiarEstadoEmpresaService(findPort, savePort, existsTratosPort);
    }



    // ── Contacto UseCase Beans ──

    @Bean
    public CreateContactoUseCase createContactoUseCase(ContactoRepositoryAdapter adapter) {
        return new CreateContactoService(adapter);
    }

    @Bean
    public GetAllContactosUseCase getAllContactosUseCase(ContactoRepositoryAdapter adapter) {
        return new GetAllContactosService(adapter);
    }

    @Bean
    public GetContactoByIdUseCase getContactoByIdUseCase(ContactoRepositoryAdapter adapter) {
        return new GetContactoByIdService(adapter);
    }

    @Bean
    public EditContactoUseCase editContactoUseCase(ContactoRepositoryAdapter findPort, ContactoRepositoryAdapter savePort) {
        return new EditContactoService(findPort, savePort);
    }

    @Bean
    public DeleteContactoUseCase deleteContactoUseCase(
            ContactoRepositoryAdapter findPort,
            ContactoRepositoryAdapter existsTratosPort,
            ContactoRepositoryAdapter deletePort
    ) {
        return new DeleteContactoService(findPort, existsTratosPort, deletePort);
    }

    @Bean
    public CambiarEstadoContactoUseCase cambiarEstadoContactoUseCase(
            ContactoRepositoryAdapter findPort,
            ContactoRepositoryAdapter savePort,
            ContactoRepositoryAdapter existsTratosPort
    ) {
        return new CambiarEstadoContactoService(findPort, savePort, existsTratosPort);
    }



    // ── Tablero Mapper Bean ──

    /**
     * Stateful mapper for Tablero aggregate ↔ entities.
     *
     * <p>Requires {@link ColumnaRepository} to hydrate catalog column data when
     * reading {@link com.ar.crm2.model.entity.ColumnaTablero} from the relation entity.
     */
    @Bean
    public TableroMapper tableroMapper(ColumnaRepository columnaRepository) {
        return new TableroMapper(columnaRepository);
    }

    @Bean
    public com.ar.crm2.adapter.in.rest.mapper.TableroResponseAssembler tableroResponseAssembler(
            ColumnaRepository columnaRepository
    ) {
        return new com.ar.crm2.adapter.in.rest.mapper.TableroResponseAssembler(columnaRepository);
    }



    // ── Tablero UseCase Beans ──

    @Bean
    public CreateTableroUseCase createTableroUseCase(
            TableroRepositoryAdapter adapter,
            ColumnaRepositoryAdapter findAllColumnasPort,
            CreateColumnaUseCase createColumnaUseCase
    ) {
        return new CreateTableroService(adapter, findAllColumnasPort, createColumnaUseCase);
    }

    @Bean
    public GetAllTablerosUseCase getAllTablerosUseCase(TableroRepositoryAdapter adapter) {
        return new GetAllTablerosService(adapter);
    }

    @Bean
    public GetTableroByIdUseCase getTableroByIdUseCase(TableroRepositoryAdapter adapter) {
        return new GetTableroByIdService(adapter);
    }

    @Bean
    public EditTableroUseCase editTableroUseCase(TableroRepositoryAdapter findPort, TableroRepositoryAdapter savePort) {
        return new EditTableroService(findPort, savePort);
    }

    @Bean
    public DeleteTableroUseCase deleteTableroUseCase(
            TableroRepositoryAdapter findPort,
            TableroRepositoryAdapter deletePort
    ) {
        return new DeleteTableroService(findPort, deletePort);
    }

    @Bean
    public AsignarColumnaTableroUseCase asignarColumnaTableroUseCase(
            TableroRepositoryAdapter findTableroPort,
            TableroRepositoryAdapter findColumnaPort,
            TableroRepositoryAdapter existsColumnaEnTableroPort,
            TableroRepositoryAdapter savePort
    ) {
        return new AsignarColumnaTableroService(findTableroPort, findColumnaPort, existsColumnaEnTableroPort, savePort);
    }

    @Bean
    public EliminarColumnaDelTableroUseCase eliminarColumnaDelTableroUseCase(
            TableroRepositoryAdapter findPort,
            FichaRepositoryAdapter existsFichasPort,
            TableroRepositoryAdapter savePort
    ) {
        return new EliminarColumnaDelTableroService(findPort, existsFichasPort, savePort);
    }

    @Bean
    public ReordenarColumnasUseCase reordenarColumnasUseCase(
            TableroRepositoryAdapter findPort,
            TableroRepositoryAdapter savePort
    ) {
        return new ReordenarColumnasService(findPort, savePort);
    }



    // ── Trato UseCase Beans ──

    @Bean
    public CreateTratoUseCase createTratoUseCase(
            TratoRepositoryAdapter saveTratoPort,
            FichaRepositoryAdapter saveFichaPort,
            TableroRepositoryAdapter findInitialColumnPort
    ) {
        return new CreateTratoService(saveTratoPort, saveFichaPort, findInitialColumnPort);
    }

    @Bean
    public GetAllTratosUseCase getAllTratosUseCase(TratoRepositoryAdapter adapter) {
        return new GetAllTratosService(adapter);
    }

    @Bean
    public GetTratoByIdUseCase getTratoByIdUseCase(TratoRepositoryAdapter adapter) {
        return new GetTratoByIdService(adapter);
    }

    @Bean
    public EditTratoUseCase editTratoUseCase(TratoRepositoryAdapter findPort, TratoRepositoryAdapter savePort) {
        return new EditTratoService(findPort, savePort);
    }

    @Bean
    public com.ar.crm2.adapter.out.persistence.NotaTratoRepositoryAdapter notaTratoRepositoryAdapter(
            com.ar.crm2.adapter.out.persistence.repository.NotaTratoRepository repository) {
        return new com.ar.crm2.adapter.out.persistence.NotaTratoRepositoryAdapter(repository);
    }

    @Bean
    public com.ar.crm2.application.trato.port.in.CambiarEstadoTratoUseCase cambiarEstadoTratoUseCase(
            TratoRepositoryAdapter findPort, TratoRepositoryAdapter savePort,
            com.ar.crm2.adapter.out.persistence.NotaTratoRepositoryAdapter notaPort) {
        return new com.ar.crm2.application.trato.service.CambiarEstadoTratoService(findPort, savePort, notaPort);
    }

    @Bean
    public com.ar.crm2.application.notatrato.port.in.CrearNotaTratoUseCase crearNotaTratoUseCase(
            TratoRepositoryAdapter findTratoPort,
            com.ar.crm2.adapter.out.persistence.NotaTratoRepositoryAdapter notaPort) {
        return new com.ar.crm2.application.notatrato.service.CrearNotaTratoService(findTratoPort, notaPort);
    }

    @Bean
    public com.ar.crm2.application.notatrato.port.in.GetNotasByTratoUseCase getNotasByTratoUseCase(
            com.ar.crm2.adapter.out.persistence.NotaTratoRepositoryAdapter notaPort) {
        return new com.ar.crm2.application.notatrato.service.GetNotasByTratoService(notaPort);
    }

    @Bean
    public DeleteTratoUseCase deleteTratoUseCase(
            TratoRepositoryAdapter findPort,
            TratoRepositoryAdapter deletePort
    ) {
        return new DeleteTratoService(findPort, deletePort);
    }



    // ── Tarea UseCase Beans ──

    @Bean
    public CreateTareaUseCase createTareaUseCase(
            TareaRepositoryAdapter saveTareaPort,
            FichaRepositoryAdapter saveFichaPort,
            TableroRepositoryAdapter findInitialColumnPort
    ) {
        return new CreateTareaService(saveTareaPort, saveFichaPort, findInitialColumnPort);
    }

    @Bean
    public GetAllTareasUseCase getAllTareasUseCase(TareaRepositoryAdapter adapter) {
        return new GetAllTareasService(adapter);
    }

    @Bean
    public GetTareaByIdUseCase getTareaByIdUseCase(TareaRepositoryAdapter adapter) {
        return new GetTareaByIdService(adapter);
    }

    @Bean
    public EditTareaUseCase editTareaUseCase(TareaRepositoryAdapter findPort, TareaRepositoryAdapter savePort) {
        return new EditTareaService(findPort, savePort);
    }

    @Bean
    public DeleteTareaUseCase deleteTareaUseCase(
            TareaRepositoryAdapter findPort,
            TareaRepositoryAdapter deletePort
    ) {
        return new DeleteTareaService(findPort, deletePort);
    }

    // ── Agenda UseCase Beans ──

    @Bean
    public AgendaRepositoryAdapter agendaRepositoryAdapter(AgendaRepository repository) {
        return new AgendaRepositoryAdapter(repository);
    }

    @Bean
    public CreateAgendaUseCase createAgendaUseCase(
            AgendaRepositoryAdapter savePort,
            TareaRepositoryAdapter findTareaPort,
            TratoRepositoryAdapter findTratoPort,
            UsuarioRepositoryAdapter findUsuarioPort,
            com.ar.crm2.adapter.out.email.AgendaEmailAdapter agendaEmailAdapter
    ) {
        return new CreateAgendaService(savePort, findTareaPort, findTratoPort, findUsuarioPort, agendaEmailAdapter);
    }

    @Bean
    public GetAgendasByUserUseCase getAgendasByUserUseCase(AgendaRepositoryAdapter adapter) {
        return new GetAgendasByUserService(adapter);
    }

    @Bean
    public GetAgendaByIdUseCase getAgendaByIdUseCase(AgendaRepositoryAdapter adapter) {
        return new GetAgendaByIdService(adapter);
    }

    @Bean
    public EditAgendaUseCase editAgendaUseCase(
            AgendaRepositoryAdapter findPort,
            AgendaRepositoryAdapter savePort,
            TareaRepositoryAdapter findTareaPort,
            TratoRepositoryAdapter findTratoPort
    ) {
        return new EditAgendaService(findPort, savePort, findTareaPort, findTratoPort);
    }

    @Bean
    public DeleteAgendaUseCase deleteAgendaUseCase(
            AgendaRepositoryAdapter findPort,
            AgendaRepositoryAdapter deletePort
    ) {
        return new DeleteAgendaService(findPort, deletePort);
    }

    // ── Agenda Reminder Email Adapter ──

    @Bean
    public com.ar.crm2.adapter.out.email.AgendaEmailAdapter agendaEmailAdapter(
            org.springframework.mail.javamail.JavaMailSender mailSender,
            com.ar.crm2.adapter.out.email.config.EmailProperties emailProperties
    ) {
        return new com.ar.crm2.adapter.out.email.AgendaEmailAdapter(mailSender, emailProperties);
    }

    @Bean
    public com.ar.crm2.application.agenda.port.in.SendAgendaRemindersUseCase sendAgendaRemindersUseCase(
            AgendaRepositoryAdapter findDueAgendasPort,
            UsuarioRepositoryAdapter findUsuarioPort,
            com.ar.crm2.adapter.out.email.AgendaEmailAdapter sendEmailPort,
            AgendaRepositoryAdapter saveAgendaPort
    ) {
        return new com.ar.crm2.application.agenda.service.SendAgendaRemindersService(
                findDueAgendasPort, findUsuarioPort, sendEmailPort, saveAgendaPort
        );
    }


    // ── Ficha UseCase Beans ──
    // NOTE: CreateFichaService and EditFichaService (slice 1 of add-ficha-etiquetas)
    // require a FindEtiquetasByIdsPort in addition to SaveFichaPort / FindFichaByIdPort.
    // The EtiquetaRepositoryAdapter bean defined below in the Etiqueta section is the
    // project-wide implementation of that port and is reused here, matching the
    // single-adapter-per-port convention used for FichaRepositoryAdapter.

    @Bean
    public CreateFichaUseCase createFichaUseCase(
            FichaRepositoryAdapter savePort,
            com.ar.crm2.adapter.out.persistence.EtiquetaRepositoryAdapter findEtiquetasPort
    ) {
        return new CreateFichaService(savePort, findEtiquetasPort);
    }

    @Bean
    public GetAllFichasUseCase getAllFichasUseCase(FichaRepositoryAdapter adapter) {
        return new GetAllFichasService(adapter);
    }

    @Bean
    public GetFichaByIdUseCase getFichaByIdUseCase(FichaRepositoryAdapter adapter) {
        return new GetFichaByIdService(adapter);
    }

    @Bean
    public EditFichaUseCase editFichaUseCase(
            FichaRepositoryAdapter findPort,
            FichaRepositoryAdapter savePort,
            com.ar.crm2.adapter.out.persistence.EtiquetaRepositoryAdapter findEtiquetasPort
    ) {
        return new EditFichaService(findPort, savePort, findEtiquetasPort);
    }

    @Bean
    public DeleteFichaUseCase deleteFichaUseCase(
            FichaRepositoryAdapter findPort,
            FichaRepositoryAdapter deletePort
    ) {
        return new DeleteFichaService(findPort, deletePort);
    }

    @Bean
    public MoverColumnaFichaUseCase moverColumnaFichaUseCase(
            FichaRepositoryAdapter findPort,
            FichaRepositoryAdapter savePort
    ) {
        return new MoverColumnaFichaService(findPort, savePort);
    }



    // ── Usuario UseCase Beans ──

    @Bean
    public CreateUsuarioUseCase createUsuarioUseCase(
            UsuarioRepositoryAdapter adapter,
            ProvisionIdentityPort provisionPort,
            DeleteIdentityPort deleteIdentityPort,
            SetIdentityAttributesPort setAttributesPort
    ) {
        return new CreateUsuarioService(adapter, provisionPort, deleteIdentityPort, setAttributesPort);
    }

    @Bean
    public GetAllUsuariosUseCase getAllUsuariosUseCase(UsuarioRepositoryAdapter adapter) {
        return new GetAllUsuariosService(adapter);
    }

    @Bean
    public GetUsuarioByIdUseCase getUsuarioByIdUseCase(UsuarioRepositoryAdapter adapter) {
        return new GetUsuarioByIdService(adapter);
    }

    @Bean
    public EditUsuarioUseCase editUsuarioUseCase(
            UsuarioRepositoryAdapter findPort,
            UsuarioRepositoryAdapter savePort,
            SyncIdentityEmailPort syncEmailPort,
            SetIdentityEnabledPort setEnabledPort
    ) {
        return new EditUsuarioService(findPort, savePort, syncEmailPort, setEnabledPort);
    }

    @Bean
    public DeleteUsuarioUseCase deleteUsuarioUseCase(
            UsuarioRepositoryAdapter findPort,
            UsuarioRepositoryAdapter deletePort,
            KeycloakUserProvisioningAdapter identityAdapter
    ) {
        return new DeleteUsuarioService(findPort, deletePort, identityAdapter);
    }

    @Bean
    public RequestPasswordChangeUseCase requestPasswordChangeUseCase(
            UsuarioRepositoryAdapter findPort,
            SendIdentityUpdatePasswordEmailPort sendIdentityUpdatePasswordEmailPort
    ) {
        return new RequestPasswordChangeService(findPort, sendIdentityUpdatePasswordEmailPort);
    }

    @Bean
    public ForgotPasswordUseCase forgotPasswordUseCase(
            UsuarioRepositoryAdapter findByCorreoPort,
            SendIdentityUpdatePasswordEmailPort sendIdentityUpdatePasswordEmailPort
    ) {
        return new ForgotPasswordService(findByCorreoPort, sendIdentityUpdatePasswordEmailPort);
    }



    // ── Rol UseCase Beans ──

    @Bean
    public CreateRolUseCase createRolUseCase(RolRepositoryAdapter adapter) {
        return new CreateRolService(adapter);
    }

    @Bean
    public GetAllRolesUseCase getAllRolesUseCase(RolRepositoryAdapter adapter) {
        return new GetAllRolesService(adapter);
    }

    @Bean
    public GetRolByIdUseCase getRolByIdUseCase(RolRepositoryAdapter adapter) {
        return new GetRolByIdService(adapter);
    }

    @Bean
    public EditRolUseCase editRolUseCase(RolRepositoryAdapter findPort, RolRepositoryAdapter savePort) {
        return new EditRolService(findPort, savePort);
    }

    @Bean
    public DeleteRolUseCase deleteRolUseCase(
            RolRepositoryAdapter findPort,
            UsuarioRepositoryAdapter existsUsuariosPort,
            RolRepositoryAdapter deletePort
    ) {
        return new DeleteRolService(findPort, existsUsuariosPort, deletePort);
    }



    // ── SuperUsuario UseCase Beans ──

    @Bean
    public CreateSuperUsuarioUseCase createSuperUsuarioUseCase(
            SuperUsuarioRepositoryAdapter adapter,
            ProvisionIdentityPort provisionPort,
            DeleteIdentityPort deleteIdentityPort
    ) {
        return new CreateSuperUsuarioService(adapter, provisionPort, deleteIdentityPort);
    }

    @Bean
    public GetAllSuperUsuariosUseCase getAllSuperUsuariosUseCase(SuperUsuarioRepositoryAdapter adapter) {
        return new GetAllSuperUsuariosService(adapter);
    }

    @Bean
    public GetSuperUsuarioByIdUseCase getSuperUsuarioByIdUseCase(SuperUsuarioRepositoryAdapter adapter) {
        return new GetSuperUsuarioByIdService(adapter);
    }

    @Bean
    public EditSuperUsuarioUseCase editSuperUsuarioUseCase(
            SuperUsuarioRepositoryAdapter findPort,
            SuperUsuarioRepositoryAdapter savePort,
            SyncIdentityEmailPort syncEmailPort,
            SetIdentityEnabledPort setEnabledPort
    ) {
        return new EditSuperUsuarioService(findPort, savePort, syncEmailPort, setEnabledPort);
    }

    @Bean
    public DeleteSuperUsuarioUseCase deleteSuperUsuarioUseCase(
            SuperUsuarioRepositoryAdapter findPort,
            SuperUsuarioRepositoryAdapter deletePort,
            KeycloakUserProvisioningAdapter identityAdapter
    ) {
        return new DeleteSuperUsuarioService(findPort, deletePort, identityAdapter);
    }



    // ── Columna UseCase Beans ──

    @Bean
    public CreateColumnaUseCase createColumnaUseCase(
            ColumnaRepositoryAdapter savePort,
            ColumnaRepositoryAdapter findAllPort
    ) {
        return new CreateColumnaService(savePort, findAllPort);
    }

    @Bean
    public GetAllColumnasUseCase getAllColumnasUseCase(ColumnaRepositoryAdapter findAllPort) {
        return new GetAllColumnasService(findAllPort);
    }

    @Bean
    public GetColumnaByIdUseCase getColumnaByIdUseCase(ColumnaRepositoryAdapter findPort) {
        return new GetColumnaByIdService(findPort);
    }

    @Bean
    public EditColumnaUseCase editColumnaUseCase(
            ColumnaRepositoryAdapter findPort,
            ColumnaRepositoryAdapter findAllPort,
            ColumnaRepositoryAdapter savePort
    ) {
        return new EditColumnaService(findPort, findAllPort, savePort);
    }

    @Bean
    public DeleteColumnaUseCase deleteColumnaUseCase(
            ColumnaRepositoryAdapter findPort,
            ColumnaRepositoryAdapter existsColumnaAsignadaPort,
            FichaRepositoryAdapter existsFichasByColumnaIdPort,
            ColumnaRepositoryAdapter deletePort
    ) {
        return new DeleteColumnaService(findPort, existsColumnaAsignadaPort, existsFichasByColumnaIdPort, deletePort);
    }


    // ── Etiqueta UseCase Beans (add-ficha-etiquetas slice 3) ──────

    /**
     * Single {@code EtiquetaRepositoryAdapter} bean implements every
     * Etiqueta outbound port (Save / Find / FindAll / Exists / Delete /
     * Count / FindByIds). It is wired explicitly here, consistent with
     * the project convention for every other outbound adapter
     * ({@code FichaRepositoryAdapter}, {@code TableroRepositoryAdapter},
     * etc.). The adapter class is not annotated with {@code @Component};
     * this {@code @Bean} is the only definition in the application
     * context.
     */
    @Bean
    public com.ar.crm2.adapter.out.persistence.EtiquetaRepositoryAdapter etiquetaRepositoryAdapter(
            com.ar.crm2.adapter.out.persistence.repository.EtiquetaRepository etiquetaRepository,
            com.ar.crm2.adapter.out.persistence.repository.FichaEtiquetaRepository fichaEtiquetaRepository
    ) {
        return new com.ar.crm2.adapter.out.persistence.EtiquetaRepositoryAdapter(
            etiquetaRepository, fichaEtiquetaRepository);
    }

    @Bean
    public CreateEtiquetaUseCase createEtiquetaUseCase(
            com.ar.crm2.adapter.out.persistence.EtiquetaRepositoryAdapter adapter
    ) {
        return new CreateEtiquetaService(adapter, adapter);
    }

    @Bean
    public GetAllEtiquetasUseCase getAllEtiquetasUseCase(
            com.ar.crm2.adapter.out.persistence.EtiquetaRepositoryAdapter adapter
    ) {
        return new GetAllEtiquetasService(adapter);
    }

    @Bean
    public GetEtiquetaByIdUseCase getEtiquetaByIdUseCase(
            com.ar.crm2.adapter.out.persistence.EtiquetaRepositoryAdapter adapter
    ) {
        return new GetEtiquetaByIdService(adapter);
    }

    @Bean
    public EditEtiquetaUseCase editEtiquetaUseCase(
            com.ar.crm2.adapter.out.persistence.EtiquetaRepositoryAdapter adapter
    ) {
        return new EditEtiquetaService(adapter, adapter, adapter);
    }

    @Bean
    public DeleteEtiquetaUseCase deleteEtiquetaUseCase(
            com.ar.crm2.adapter.out.persistence.EtiquetaRepositoryAdapter adapter
    ) {
        return new DeleteEtiquetaService(adapter, adapter, adapter, adapter);
    }


    // ── WhatsApp Module: Adapter Beans ──────────────────────────────────────

    @Bean
    public com.ar.crm2.adapter.out.persistence.CanalWhatsappRepositoryAdapter canalWhatsappRepositoryAdapter(
            com.ar.crm2.adapter.out.persistence.repository.CanalWhatsappRepository repository
    ) {
        return new com.ar.crm2.adapter.out.persistence.CanalWhatsappRepositoryAdapter(repository);
    }

    @Bean
    public com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter conversacionRepositoryAdapter(
            com.ar.crm2.adapter.out.persistence.repository.ConversacionRepository repository
    ) {
        return new com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter(repository);
    }

    @Bean
    public com.ar.crm2.adapter.out.persistence.MensajeRepositoryAdapter mensajeRepositoryAdapter(
            com.ar.crm2.adapter.out.persistence.repository.MensajeRepository repository
    ) {
        return new com.ar.crm2.adapter.out.persistence.MensajeRepositoryAdapter(repository);
    }

    @Bean
    public com.ar.crm2.adapter.out.evolution.EvolutionWhatsappAdapter evolutionWhatsappAdapter(
            org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder,
            com.ar.crm2.security.WaProperties waProperties
    ) {
        return new com.ar.crm2.adapter.out.evolution.EvolutionWhatsappAdapter(webClientBuilder, waProperties);
    }

    @Bean
    public com.ar.crm2.adapter.out.ia.AnthropicSugerenciaAdapter anthropicSugerenciaAdapter(
            org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder,
            @org.springframework.beans.factory.annotation.Value("${anthropic.api-key:}") String apiKey,
            @org.springframework.beans.factory.annotation.Value("${anthropic.model:claude-haiku-4-5-20251001}") String model
    ) {
        return new com.ar.crm2.adapter.out.ia.AnthropicSugerenciaAdapter(webClientBuilder, apiKey, model);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.ia.port.in.SugerirRespuestaUseCase sugerirRespuestaUseCase(
            com.ar.crm2.adapter.out.persistence.MensajeRepositoryAdapter mensajeAdapter,
            com.ar.crm2.adapter.out.ia.AnthropicSugerenciaAdapter iaAdapter
    ) {
        return new com.ar.crm2.whatsapp.application.ia.service.SugerirRespuestaService(mensajeAdapter, iaAdapter);
    }

    @Bean
    public com.ar.crm2.adapter.out.media.LocalMediaStorageAdapter localMediaStorageAdapter() {
        return new com.ar.crm2.adapter.out.media.LocalMediaStorageAdapter();
    }

    @Bean
    public com.ar.crm2.adapter.out.sse.SseMensajeNotifyAdapter sseMensajeNotifyAdapter(
            com.ar.crm2.adapter.out.sse.SseEmitterRegistry registry
    ) {
        return new com.ar.crm2.adapter.out.sse.SseMensajeNotifyAdapter(registry);
    }

    @Bean
    public com.ar.crm2.adapter.out.persistence.BotRepositoryAdapter botRepositoryAdapter(
            com.ar.crm2.adapter.out.persistence.repository.BotRepository repository
    ) {
        return new com.ar.crm2.adapter.out.persistence.BotRepositoryAdapter(repository);
    }

    @Bean
    public com.ar.crm2.adapter.out.bot.BotWebhookNotifier botWebhookNotifier(
            org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder,
            com.ar.crm2.adapter.out.persistence.BotRepositoryAdapter botAdapter,
            com.ar.crm2.security.WaProperties waProperties
    ) {
        return new com.ar.crm2.adapter.out.bot.BotWebhookNotifier(webClientBuilder, botAdapter, waProperties);
    }

    @Bean
    public com.ar.crm2.adapter.out.persistence.FunnelMovementAdapter funnelMovementAdapter(
            com.ar.crm2.adapter.out.persistence.repository.TratoRepository tratoRepository,
            com.ar.crm2.adapter.out.persistence.repository.FichaRepository fichaRepository,
            com.ar.crm2.adapter.out.persistence.repository.TableroRepository tableroRepository,
            com.ar.crm2.adapter.out.persistence.repository.ColumnaRepository columnaRepository
    ) {
        return new com.ar.crm2.adapter.out.persistence.FunnelMovementAdapter(
                tratoRepository, fichaRepository, tableroRepository, columnaRepository);
    }


    // ── WhatsApp Module: Bot UseCase Beans ──────────────────────────────────

    @Bean
    public com.ar.crm2.whatsapp.application.bot.port.in.CreateBotUseCase createBotUseCase(
            com.ar.crm2.adapter.out.persistence.BotRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.bot.service.CreateBotService(adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.bot.port.in.EditBotUseCase editBotUseCase(
            com.ar.crm2.adapter.out.persistence.BotRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.bot.service.EditBotService(adapter, adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.bot.port.in.GetAllBotsUseCase getAllBotsUseCase(
            com.ar.crm2.adapter.out.persistence.BotRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.bot.service.GetAllBotsService(adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.bot.port.in.GetBotByIdUseCase getBotByIdUseCase(
            com.ar.crm2.adapter.out.persistence.BotRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.bot.service.GetBotByIdService(adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.bot.port.in.DeleteBotUseCase deleteBotUseCase(
            com.ar.crm2.adapter.out.persistence.BotRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.bot.service.DeleteBotService(adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.bot.port.in.FindBotByTokenUseCase findBotByTokenUseCase(
            com.ar.crm2.adapter.out.persistence.BotRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.bot.service.FindBotByTokenService(adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.bot.port.in.ToggleBotActivoUseCase toggleBotActivoUseCase(
            com.ar.crm2.adapter.out.persistence.BotRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.bot.service.ToggleBotActivoService(adapter, adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.funnel.port.in.MoverFunnelUseCase moverFunnelUseCase(
            com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter conversacionAdapter,
            com.ar.crm2.adapter.out.persistence.FunnelMovementAdapter funnelAdapter
    ) {
        return new com.ar.crm2.whatsapp.application.funnel.service.MoverFunnelService(conversacionAdapter, funnelAdapter);
    }


    // ── WhatsApp Module: Canal UseCase Beans ────────────────────────────────

    @Bean
    public com.ar.crm2.whatsapp.application.canal.port.in.CreateCanalUseCase createCanalUseCase(
            com.ar.crm2.adapter.out.persistence.CanalWhatsappRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.canal.service.CreateCanalService(adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.canal.port.in.GetAllCanalesUseCase getAllCanalesUseCase(
            com.ar.crm2.adapter.out.persistence.CanalWhatsappRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.canal.service.GetAllCanalesService(adapter, adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.canal.port.in.GetCanalByIdUseCase getCanalByIdUseCase(
            com.ar.crm2.adapter.out.persistence.CanalWhatsappRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.canal.service.GetCanalByIdService(adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.canal.port.in.EditCanalUseCase editCanalUseCase(
            com.ar.crm2.adapter.out.persistence.CanalWhatsappRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.canal.service.EditCanalService(adapter, adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.canal.port.in.DeleteCanalUseCase deleteCanalUseCase(
            com.ar.crm2.adapter.out.persistence.CanalWhatsappRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.canal.service.DeleteCanalService(adapter, adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.canal.port.in.ConectarCanalUseCase conectarCanalUseCase(
            com.ar.crm2.adapter.out.persistence.CanalWhatsappRepositoryAdapter adapter,
            com.ar.crm2.adapter.out.evolution.EvolutionWhatsappAdapter evolutionAdapter
    ) {
        return new com.ar.crm2.whatsapp.application.canal.service.ConectarCanalService(adapter, adapter, evolutionAdapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.canal.port.in.GetEstadoCanalUseCase getEstadoCanalUseCase(
            com.ar.crm2.adapter.out.persistence.CanalWhatsappRepositoryAdapter adapter,
            com.ar.crm2.adapter.out.evolution.EvolutionWhatsappAdapter evolutionAdapter
    ) {
        return new com.ar.crm2.whatsapp.application.canal.service.GetEstadoCanalService(adapter, adapter, evolutionAdapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.canal.port.in.ReconfigurarWebhookUseCase reconfigurarWebhookUseCase(
            com.ar.crm2.adapter.out.persistence.CanalWhatsappRepositoryAdapter adapter,
            com.ar.crm2.adapter.out.evolution.EvolutionWhatsappAdapter evolutionAdapter
    ) {
        return new com.ar.crm2.whatsapp.application.canal.service.ReconfigurarWebhookService(adapter, evolutionAdapter);
    }


    // ── WhatsApp Module: Conversacion UseCase Beans ─────────────────────────

    @Bean
    public com.ar.crm2.whatsapp.application.conversacion.port.in.GetOrCreateConversacionUseCase getOrCreateConversacionUseCase(
            com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.conversacion.service.GetOrCreateConversacionService(adapter, adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.conversacion.port.in.GetAllConversacionesUseCase getAllConversacionesUseCase(
            com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.conversacion.service.GetAllConversacionesService(adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.conversacion.port.in.GetConversacionByIdUseCase getConversacionByIdUseCase(
            com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.conversacion.service.GetConversacionByIdService(adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.conversacion.port.in.AsignarAgenteUseCase asignarAgenteUseCase(
            com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.conversacion.service.AsignarAgenteService(adapter, adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.conversacion.port.in.CerrarConversacionUseCase cerrarConversacionUseCase(
            com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter adapter,
            com.ar.crm2.adapter.out.persistence.AjustesWaAdapter ajustesAdapter,
            com.ar.crm2.adapter.out.persistence.CanalWhatsappRepositoryAdapter canalAdapter,
            com.ar.crm2.adapter.out.evolution.EvolutionWhatsappAdapter evolutionAdapter
    ) {
        return new com.ar.crm2.whatsapp.application.conversacion.service.CerrarConversacionService(
                adapter, adapter, ajustesAdapter, canalAdapter, evolutionAdapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.conversacion.port.in.GetCsatResumenUseCase getCsatResumenUseCase(
            com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.conversacion.service.GetCsatResumenService(adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.conversacion.port.in.MarcarConversacionLeidaUseCase marcarConversacionLeidaUseCase(
            com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.conversacion.service.MarcarConversacionLeidaService(adapter, adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.conversacion.port.in.RenombrarConversacionUseCase renombrarConversacionUseCase(
            com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter adapter,
            com.ar.crm2.adapter.out.persistence.ContactoUpsertAdapter contactoUpsertAdapter
    ) {
        return new com.ar.crm2.whatsapp.application.conversacion.service.RenombrarConversacionService(
                adapter, adapter, contactoUpsertAdapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.conversacion.port.in.ReabrirConversacionUseCase reabrirConversacionUseCase(
            com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.conversacion.service.ReabrirConversacionService(adapter, adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.conversacion.port.in.AplicarLabelsUseCase aplicarLabelsUseCase(
            com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.conversacion.service.AplicarLabelsService(adapter, adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.conversacion.port.in.AutoResolverConversacionesUseCase autoResolverConversacionesUseCase(
            com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.conversacion.service.AutoResolverConversacionesService(adapter, adapter);
    }


    // ── WhatsApp Module: Mensaje UseCase Beans ──────────────────────────────

    @Bean
    public com.ar.crm2.whatsapp.application.mensaje.port.in.ReceiveMensajeUseCase receiveMensajeUseCase(
            com.ar.crm2.adapter.out.persistence.MensajeRepositoryAdapter mensajeAdapter,
            com.ar.crm2.whatsapp.application.conversacion.port.in.GetOrCreateConversacionUseCase getOrCreate,
            com.ar.crm2.adapter.out.sse.SseMensajeNotifyAdapter notifyAdapter,
            com.ar.crm2.adapter.out.persistence.CanalWhatsappRepositoryAdapter canalAdapter,
            com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter conversacionAdapter,
            com.ar.crm2.adapter.out.persistence.ContactoUpsertAdapter contactoUpsertAdapter,
            com.ar.crm2.adapter.out.evolution.EvolutionWhatsappAdapter evolutionAdapter,
            com.ar.crm2.adapter.out.media.LocalMediaStorageAdapter mediaStorage,
            com.ar.crm2.adapter.out.persistence.AjustesWaAdapter ajustesAdapter,
            com.ar.crm2.adapter.out.persistence.SugerirResponsableAdapter sugerirResponsableAdapter,
            com.ar.crm2.adapter.out.bot.BotWebhookNotifier botWebhookNotifier
    ) {
        return new com.ar.crm2.whatsapp.application.mensaje.service.ReceiveMensajeService(
                mensajeAdapter, getOrCreate, mensajeAdapter, notifyAdapter,
                canalAdapter, conversacionAdapter, contactoUpsertAdapter, evolutionAdapter, mediaStorage,
                ajustesAdapter, evolutionAdapter, mensajeAdapter, sugerirResponsableAdapter, botWebhookNotifier);
    }

    @Bean
    public com.ar.crm2.adapter.out.persistence.AjustesWaAdapter ajustesWaAdapter(
            com.ar.crm2.adapter.out.persistence.repository.AjustesWaRepository repository
    ) {
        return new com.ar.crm2.adapter.out.persistence.AjustesWaAdapter(repository);
    }

    @Bean
    public com.ar.crm2.adapter.out.persistence.PlantillaAdapter plantillaAdapter(
            com.ar.crm2.adapter.out.persistence.repository.PlantillaRepository repository
    ) {
        return new com.ar.crm2.adapter.out.persistence.PlantillaAdapter(repository);
    }

    @Bean
    public com.ar.crm2.adapter.out.persistence.SugerirResponsableAdapter sugerirResponsableAdapter(
            com.ar.crm2.adapter.out.persistence.repository.UsuarioRepository usuarioRepository,
            com.ar.crm2.adapter.out.persistence.repository.ConversacionRepository conversacionRepository
    ) {
        return new com.ar.crm2.adapter.out.persistence.SugerirResponsableAdapter(usuarioRepository, conversacionRepository);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.mensaje.port.in.SendMensajeUseCase sendMensajeUseCase(
            com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter conversacionAdapter,
            com.ar.crm2.adapter.out.persistence.CanalWhatsappRepositoryAdapter canalAdapter,
            com.ar.crm2.adapter.out.evolution.EvolutionWhatsappAdapter evolutionAdapter,
            com.ar.crm2.adapter.out.persistence.MensajeRepositoryAdapter mensajeAdapter,
            com.ar.crm2.adapter.out.sse.SseMensajeNotifyAdapter notifyAdapter
    ) {
        return new com.ar.crm2.whatsapp.application.mensaje.service.SendMensajeService(
                conversacionAdapter, canalAdapter, evolutionAdapter, mensajeAdapter, notifyAdapter, conversacionAdapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.mensaje.port.in.ResponderBotUseCase responderBotUseCase(
            com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter conversacionAdapter,
            com.ar.crm2.adapter.out.persistence.CanalWhatsappRepositoryAdapter canalAdapter,
            com.ar.crm2.adapter.out.evolution.EvolutionWhatsappAdapter evolutionAdapter,
            com.ar.crm2.adapter.out.persistence.MensajeRepositoryAdapter mensajeAdapter,
            com.ar.crm2.adapter.out.sse.SseMensajeNotifyAdapter notifyAdapter
    ) {
        return new com.ar.crm2.whatsapp.application.mensaje.service.ResponderBotService(
                conversacionAdapter, canalAdapter, evolutionAdapter, mensajeAdapter, notifyAdapter, conversacionAdapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.mensaje.port.in.GetMensajesByConversacionUseCase getMensajesByConversacionUseCase(
            com.ar.crm2.adapter.out.persistence.MensajeRepositoryAdapter adapter
    ) {
        return new com.ar.crm2.whatsapp.application.mensaje.service.GetMensajesByConversacionService(adapter);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.canal.port.in.SyncChatsUseCase syncChatsUseCase(
            com.ar.crm2.adapter.out.persistence.CanalWhatsappRepositoryAdapter canalAdapter,
            com.ar.crm2.adapter.out.evolution.EvolutionWhatsappAdapter evolutionAdapter,
            com.ar.crm2.whatsapp.application.conversacion.port.in.GetOrCreateConversacionUseCase getOrCreate,
            com.ar.crm2.adapter.out.persistence.ConversacionRepositoryAdapter conversacionAdapter,
            com.ar.crm2.adapter.out.persistence.ContactoUpsertAdapter contactoUpsertAdapter,
            com.ar.crm2.adapter.out.persistence.MensajeRepositoryAdapter mensajeAdapter,
            com.ar.crm2.adapter.out.media.LocalMediaStorageAdapter mediaStorage
    ) {
        return new com.ar.crm2.whatsapp.application.canal.service.SyncChatsService(
                canalAdapter, evolutionAdapter, getOrCreate, conversacionAdapter, contactoUpsertAdapter,
                mensajeAdapter, mensajeAdapter, mediaStorage);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.mensaje.service.ActualizarStatusMensajeService actualizarStatusMensajeService(
            com.ar.crm2.adapter.out.persistence.MensajeRepositoryAdapter mensajeAdapter,
            com.ar.crm2.adapter.out.sse.SseMensajeNotifyAdapter notifyAdapter
    ) {
        return new com.ar.crm2.whatsapp.application.mensaje.service.ActualizarStatusMensajeService(
                mensajeAdapter, mensajeAdapter, notifyAdapter);
    }

    // ── WhatsApp Module: Grupos ─────────────────────────────────────────────

    @Bean
    public com.ar.crm2.adapter.out.persistence.GrupoRepositoryAdapter grupoRepositoryAdapter(
            com.ar.crm2.adapter.out.persistence.repository.GrupoRepository grupoRepository,
            com.ar.crm2.adapter.out.persistence.repository.MensajeGrupoRepository mensajeGrupoRepository
    ) {
        return new com.ar.crm2.adapter.out.persistence.GrupoRepositoryAdapter(grupoRepository, mensajeGrupoRepository);
    }

    @Bean
    public com.ar.crm2.whatsapp.application.grupo.service.GrupoService grupoService(
            com.ar.crm2.adapter.out.persistence.GrupoRepositoryAdapter grupoAdapter,
            com.ar.crm2.adapter.out.sse.SseMensajeNotifyAdapter notifyAdapter,
            com.ar.crm2.adapter.out.evolution.EvolutionWhatsappAdapter evolutionAdapter,
            com.ar.crm2.adapter.out.media.LocalMediaStorageAdapter mediaStorage,
            com.ar.crm2.adapter.out.persistence.CanalWhatsappRepositoryAdapter canalAdapter
    ) {
        return new com.ar.crm2.whatsapp.application.grupo.service.GrupoService(
                grupoAdapter, grupoAdapter, notifyAdapter, evolutionAdapter, mediaStorage, canalAdapter, evolutionAdapter);
    }


    // ── Pipely Agent: A3 Tool Components ─────────────────────────────────

    /**
     * Stateless shared Spring AI 2.0 CRM tools bean.
     *
     * <p>Stores only shared dependencies (the existing Application read
     * use case, the {@link com.ar.crm2.application.contacto.port.in.CreateContactoUseCase},
     * the C1 {@link com.ar.crm2.application.agent.tool.port.in.AgentCrmWriteUseCase}
     * orchestrator that enforces deal ownership, and the Jackson
     * {@code ObjectMapper}). It carries NO request actor, owner, or
     * turn: every trusted CRM identity piece reaches each tool through
     * the framework's per-request {@code ToolContext} parameter on the
     * {@link ChatClient#prompt()} call. Boot registers this same shared
     * object once via {@code ChatClient.Builder#defaultTools(tools)} in
     * {@link AgentConfig}; every request through the configured
     * {@code ChatClient} advertises exactly the three allowlisted tools
     * ({@code find_contacts}, {@code create_contact},
     * {@code update_deal_stage}) and Spring AI 2.0's
     * {@code JsonSchemaGenerator} excludes any {@code ToolContext} from
     * the generated JSON schema.
     *
     * <p>No REST endpoint is added for these tools: tool-only features
     * MUST NOT create controllers without a separate HTTP use case.
     */
    @Bean
    public com.ar.crm2.adapter.out.ai.tool.SpringAiCrmTools springAiCrmTools(
            com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase getAllContactosUseCase,
            com.ar.crm2.application.contacto.port.in.CreateContactoUseCase createContactoUseCase,
            com.ar.crm2.application.agent.tool.port.in.AgentCrmWriteUseCase agentCrmWriteUseCase,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper
    ) {
        return new com.ar.crm2.adapter.out.ai.tool.SpringAiCrmTools(
                getAllContactosUseCase,
                createContactoUseCase,
                agentCrmWriteUseCase,
                objectMapper);
    }

    /**
     * C1 Application {@link com.ar.crm2.application.agent.tool.port.in.AgentCrmWriteUseCase}
     * orchestrator bean. Wires the deal-authorization policy from
     * {@link com.ar.crm2.application.agent.tool.service.AgentCrmWriteService}
     * against the existing {@link com.ar.crm2.application.trato.port.in.CambiarEstadoTratoUseCase}
     * use case and the existing
     * {@link com.ar.crm2.application.trato.port.out.FindTratoByIdPort}
     * persistence port (implemented by
     * {@link TratoRepositoryAdapter}). The service is the only
     * composition that touches deal ownership; the tools bean delegates
     * {@code update_deal_stage} through this orchestrator.
     */
    @Bean
    public com.ar.crm2.application.agent.tool.port.in.AgentCrmWriteUseCase agentCrmWriteUseCase(
            TratoRepositoryAdapter findTratoByIdPort,
            com.ar.crm2.application.trato.port.in.CambiarEstadoTratoUseCase cambiarEstadoTratoUseCase
    ) {
        return new com.ar.crm2.application.agent.tool.service.AgentCrmWriteService(
                findTratoByIdPort, cambiarEstadoTratoUseCase);
    }

    /**
     * Provider-neutral Spring AI completion adapter.
     *
     * <p>The adapter consumes only the configured {@link ChatClient}
     * (produced by {@link AgentConfig} with the three shared CRM tools
     * registered via {@code defaultTools(tools)}) and forwards the
     * trusted CRM {@code actorUsuarioId} per request through the
     * framework {@code .toolContext(Map.of("actorUsuarioId", ...))} call.
     * The adapter does NOT call request {@code .tools(...)} because
     * Spring AI 2.0 runtime tools replace builder defaults — omitting
     * that call preserves the configured allowlist.
     *
     * <p>The adapter fills the {@code {durable_memories}} placeholder at
     * request time via
     * {@code ChatClient#prompt().system(Consumer<PromptSystemSpec>)}.
     *
     * <p>No REST endpoint is added for these tools: tool-only features
     * MUST NOT create controllers without a separate HTTP use case.
     *
     * <p>Exposed as the Application {@code ChatCompletionPort} so the
     * completion service picks it up by interface.
     */
    @Bean
    public com.ar.crm2.application.agent.turn.port.out.ChatCompletionPort chatCompletionPort(
            org.springframework.ai.chat.client.ChatClient chatClient
    ) {
        return new com.ar.crm2.adapter.out.ai.SpringAiChatCompletionAdapter(chatClient);
    }


    // ── Pipely Agent: Final Composition (5.x) ───────────────────────
    //
    // Single source of truth for the agent-conversation beans the REST
    // ingress consumes. Every adapter below implements the Application
    // ports the controller depends on; the use cases are wired under
    // their Application port interfaces so the controller resolves the
    // correct service through the composition root.

    /**
     * Single agent-conversation persistence adapter. Implements every
     * turn outbound port that the Application turn services depend on
     * ({@code CreateUserTurnPort}, {@code FindCompletedAssistantContentPort},
     * {@code FindCompletedVisibleHistoryPort}, {@code CompletePreparedTurnPort}).
     * The adapter is not annotated with {@code @Component}; this
     * {@code @Bean} is the only definition in the application context,
     * matching the single-adapter-per-port convention.
     */
    @Bean
    public com.ar.crm2.adapter.out.persistence.agent.AgentTurnAdapter agentTurnAdapter(
            com.ar.crm2.adapter.out.persistence.agent.repository.AgentConversationRepository conversationRepository,
            com.ar.crm2.adapter.out.persistence.agent.repository.AgentTurnRepository turnRepository,
            com.ar.crm2.adapter.out.persistence.agent.repository.AgentTurnRequestRepository requestRepository,
            com.ar.crm2.adapter.out.persistence.agent.repository.AgentVisibleHistoryRepository historyRepository
    ) {
        return new com.ar.crm2.adapter.out.persistence.agent.AgentTurnAdapter(
                conversationRepository, turnRepository, requestRepository, historyRepository);
    }

    /**
     * Single durable-memory persistence adapter. Implements every
     * durable-memory outbound port, including the turn-level
     * {@code FindEligibleDurableMemoriesPort} used by chat completion
     * to forward eligible memory bullets. Wired as a single bean under
     * every port type, preserving the one-adapter-per-port convention.
     */
    @Bean
    public com.ar.crm2.adapter.out.persistence.agent.memory.DurableMemoryPersistenceAdapter durableMemoryPersistenceAdapter(
            com.ar.crm2.adapter.out.persistence.agent.memory.DurableMemoryRepository repository
    ) {
        return new com.ar.crm2.adapter.out.persistence.agent.memory.DurableMemoryPersistenceAdapter(repository);
    }

    /**
     * Single agent tool-ledger persistence adapter. Implements every
     * tool-action outbound port and is constructed with the platform
     * transaction manager and a fixed UTC {@link java.time.Clock} for
     * deterministic completion timestamps.
     */
    @Bean
    public com.ar.crm2.adapter.out.persistence.agent.tool.AgentToolActionPersistenceAdapter agentToolActionPersistenceAdapter(
            com.ar.crm2.adapter.out.persistence.agent.tool.AgentToolActionRepository repository,
            org.springframework.transaction.PlatformTransactionManager transactionManager,
            java.time.Clock agentToolActionClock
    ) {
        return new com.ar.crm2.adapter.out.persistence.agent.tool.AgentToolActionPersistenceAdapter(
                repository, transactionManager, agentToolActionClock);
    }

    /**
     * Deterministic UTC {@link java.time.Clock} for tool-action
     * completion timestamps. A fixed-offset clock keeps tool-ledger
     * tests reproducible and avoids a {@code null} PlatformTransactionManager
     * injection failure in production wiring paths that exercise the
     * ledger without configuring a clock bean.
     */
    @Bean
    public java.time.Clock agentToolActionClock() {
        return java.time.Clock.systemUTC();
    }

    /**
     * Application turn service wired under the use case interface the
     * REST controller resolves by type. The single
     * {@code CreateUserTurnPort} bean is the {@code AgentTurnAdapter}
     * defined above.
     */
    @Bean
    public com.ar.crm2.application.agent.turn.port.in.CreateUserTurnUseCase createUserTurnUseCase(
            com.ar.crm2.application.agent.turn.port.out.CreateUserTurnPort createUserTurnPort
    ) {
        return new com.ar.crm2.application.agent.turn.service.CreateUserTurnService(createUserTurnPort);
    }

    /**
     * Application completion service wired under the use case interface
     * the REST controller resolves by type. Threads the four
     * turn-outbound ports, the turn-level
     * {@code FindEligibleDurableMemoriesPort}, and the Spring AI
     * {@code ChatCompletionPort} composed earlier in this class.
     */
    @Bean
    public com.ar.crm2.application.agent.turn.port.in.CompleteUserTurnUseCase completeUserTurnUseCase(
            com.ar.crm2.application.agent.turn.port.out.FindCompletedAssistantContentPort findCompletedAssistantContentPort,
            com.ar.crm2.application.agent.turn.port.out.FindCompletedVisibleHistoryPort findCompletedVisibleHistoryPort,
            com.ar.crm2.application.agent.turn.port.out.FindEligibleDurableMemoriesPort findEligibleDurableMemoriesPort,
            com.ar.crm2.application.agent.turn.port.out.CompletePreparedTurnPort completePreparedTurnPort,
            com.ar.crm2.application.agent.turn.port.out.ChatCompletionPort chatCompletionPortImpl
    ) {
        return new com.ar.crm2.application.agent.turn.service.CompleteUserTurnService(
                findCompletedAssistantContentPort,
                findCompletedVisibleHistoryPort,
                findEligibleDurableMemoriesPort,
                completePreparedTurnPort,
                chatCompletionPortImpl);
    }

    /**
     * Durable-memory recall service wired under its use case interface.
     * The single {@code FindEligibleDurableMemoriesPort} bean is the
     * {@code DurableMemoryPersistenceAdapter} defined above.
     */
    @Bean
    public com.ar.crm2.application.agent.memory.port.in.RecallDurableMemoriesUseCase recallDurableMemoriesUseCase(
            com.ar.crm2.application.agent.memory.port.out.FindEligibleDurableMemoriesPort findEligiblePort
    ) {
        return new com.ar.crm2.application.agent.memory.service.RecallDurableMemoriesService(findEligiblePort);
    }

    /**
     * Durable-memory remember service wired under its use case interface.
     */
    @Bean
    public com.ar.crm2.application.agent.memory.port.in.RememberDurableMemoryUseCase rememberDurableMemoryUseCase(
            com.ar.crm2.application.agent.memory.port.out.SaveDurableMemoryPort savePort
    ) {
        return new com.ar.crm2.application.agent.memory.service.RememberDurableMemoryService(savePort);
    }

    /**
     * Durable-memory replace service wired under its use case interface.
     */
    @Bean
    public com.ar.crm2.application.agent.memory.port.in.ReplaceDurableMemoryUseCase replaceDurableMemoryUseCase(
            com.ar.crm2.application.agent.memory.port.out.ReplaceDurableMemoryPort replacePort
    ) {
        return new com.ar.crm2.application.agent.memory.service.ReplaceDurableMemoryService(replacePort);
    }

    /**
     * Durable-memory delete service wired under its use case interface.
     */
    @Bean
    public com.ar.crm2.application.agent.memory.port.in.DeleteDurableMemoryUseCase deleteDurableMemoryUseCase(
            com.ar.crm2.application.agent.memory.port.out.DeleteDurableMemoryPort deletePort
    ) {
        return new com.ar.crm2.application.agent.memory.service.DeleteDurableMemoryService(deletePort);
    }

    /**
     * Durable-memory purge service wired under its use case interface.
     */
    @Bean
    public com.ar.crm2.application.agent.memory.port.in.PurgeDurableMemoriesUseCase purgeDurableMemoriesUseCase(
            com.ar.crm2.application.agent.memory.port.out.PurgeDurableMemoriesPort purgePort
    ) {
        return new com.ar.crm2.application.agent.memory.service.PurgeDurableMemoriesService(purgePort);
    }

}
