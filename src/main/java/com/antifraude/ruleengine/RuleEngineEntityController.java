package com.antifraude.ruleengine;

import com.antifraude.alerts.*;
import com.antifraude.audit.AuditoriaService;
import com.antifraude.audit.Auditoria;
import com.antifraude.common.entity.*;
import com.antifraude.licensing.*;
import com.antifraude.lists.ElementoListaControlCliente;
import com.antifraude.lists.ImportacionListaControlCliente;
import com.antifraude.lists.ListaControlCliente;
import com.antifraude.profile.DisponibilidadUsuario;
import com.antifraude.profile.PerfilUsuario;
import com.antifraude.reports.ReporteRos;
import com.antifraude.rules.EjecucionRegla;
import com.antifraude.rules.ReglaRiesgo;
import com.antifraude.transactions.Transaccion;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/rule-engine/entities")
public class RuleEngineEntityController {

    private final EntityManager entityManager;
    private final AuditoriaService auditoriaService;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, Class<?>> entities;

    public RuleEngineEntityController(EntityManager entityManager, AuditoriaService auditoriaService,
                                      UsuarioRepository usuarioRepository, ObjectMapper objectMapper) {
        this.entityManager = entityManager;
        this.auditoriaService = auditoriaService;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
        this.entities = buildEntities();
    }

    @GetMapping
    @Transactional
    public ResponseEntity<List<EntitySummary>> listarEntidades() {
        List<EntitySummary> summaries = entities.values().stream()
                .distinct()
                .map(type -> new EntitySummary(tableName(type), tableName(type), count(type), isEditable(type)))
                .sorted(Comparator.comparing(EntitySummary::table))
                .toList();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/{entity}")
    @Transactional
    public ResponseEntity<List<Map<String, Object>>> listar(@PathVariable String entity) {
        Class<?> type = resolve(entity);
        List<?> rows = entityManager.createQuery("select e from " + type.getSimpleName() + " e", type)
                .setMaxResults(500)
                .getResultList();
        return ResponseEntity.ok(rows.stream().map(this::flatten).toList());
    }

    @GetMapping("/{entity}/schema")
    public ResponseEntity<EntitySchema> schema(@PathVariable String entity) {
        Class<?> type = resolve(entity);
        List<FieldSchema> fields = fields(type).stream()
                .filter(field -> !"id".equals(field.getName()))
                .map(field -> new FieldSchema(
                        field.getName(),
                        field.getType().getSimpleName(),
                        relationType(field),
                        field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class),
                        isEditableField(field)))
                .toList();
        return ResponseEntity.ok(new EntitySchema(entity, tableName(type), isEditable(type), fields));
    }

    @GetMapping("/{entity}/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> detalle(@PathVariable String entity, @PathVariable Long id) {
        Object row = findRequired(resolve(entity), id);
        return ResponseEntity.ok(flatten(row));
    }

    @PostMapping("/{entity}")
    @Transactional
    public ResponseEntity<Map<String, Object>> crear(@PathVariable String entity, @RequestBody Map<String, Object> payload,
                                                     Authentication authentication, HttpServletRequest request) {
        Class<?> type = resolve(entity);
        Object row = instantiate(type);
        applyPayload(row, payload);
        entityManager.persist(row);
        entityManager.flush();
        registrarAuditoria(authentication, request, "CREAR_REGISTRO", tableName(type), readId(row),
                null, toJson(flatten(row)));
        return ResponseEntity.ok(flatten(row));
    }

    @PutMapping("/{entity}/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> actualizar(@PathVariable String entity,
                                                          @PathVariable Long id,
                                                          @RequestBody Map<String, Object> payload,
                                                          Authentication authentication,
                                                          HttpServletRequest request) {
        Object row = findRequired(resolve(entity), id);
        String anterior = toJson(flatten(row));
        applyPayload(row, payload);
        entityManager.flush();
        registrarAuditoria(authentication, request, "EDITAR_REGISTRO", tableName(row.getClass()), id,
                anterior, toJson(flatten(row)));
        return ResponseEntity.ok(flatten(row));
    }

    @DeleteMapping("/{entity}/{id}")
    @Transactional
    public ResponseEntity<Void> eliminar(@PathVariable String entity, @PathVariable Long id,
                                         Authentication authentication, HttpServletRequest request) {
        Object row = findRequired(resolve(entity), id);
        String anterior = toJson(flatten(row));
        entityManager.remove(row);
        registrarAuditoria(authentication, request, "ELIMINAR_REGISTRO", tableName(row.getClass()), id,
                anterior, null);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Class<?>> buildEntities() {
        Map<String, Class<?>> map = new LinkedHashMap<>();
        register(map, Accion.class, Actuacion.class, Alerta.class, Auditoria.class, CalendarioRiesgo.class,
                Canal.class, Caso.class, CasoAlerta.class, ClienteObservado.class, ClientePEP.class,
                ComentarioCaso.class, ControlFrecuencia.class, ControlImporte.class,
                DisponibilidadUsuario.class, Documento.class, EjecucionRegla.class, ElementoLista.class,
                EmpeOperador.class, Escenario.class, EstadisticaCargaAnalista.class, Evidencia.class,
                HistorialAsignacion.class, HistorialEstadoCaso.class, HorarioLaboralUsuario.class, HorarioRiesgo.class,
                ListaRegulatoria.class, Moneda.class, NivelRiesgo.class, Pais.class, PaisRiesgo.class,
                PerfilCliente.class, PerfilUsuario.class, Persona.class, ProcesadoraTarjeta.class, Producto.class,
                ReglaRiesgo.class, ReporteRos.class, ServicioExterno.class, TipoDocumento.class,
                TipoTransaccion.class, Transaccion.class, Usuario.class,
                Empresa.class, PlanLicencia.class, Suscripcion.class, Contrato.class, Pago.class,
                UsoSuscripcion.class, RolSistema.class, PermisoSistema.class, RolPermiso.class, UsuarioEmpresa.class,
                ResolucionAlerta.class, ConsultaKycAlerta.class, DecisionCaso.class, AprobacionSupervisor.class,
                FuenteDatosRiesgo.class, SujetoRiesgo.class, SujetoRiesgoAlias.class, SujetoRiesgoDocumento.class,
                SujetoRiesgoRelacion.class, CoincidenciaListaAlerta.class, ListaControlCliente.class,
                ElementoListaControlCliente.class, ImportacionListaControlCliente.class);
        return map;
    }

    private void register(Map<String, Class<?>> map, Class<?>... types) {
        for (Class<?> type : types) {
            map.put(tableName(type), type);
            map.put(type.getSimpleName(), type);
        }
    }

    private Class<?> resolve(String entity) {
        Class<?> type = entities.get(entity);
        if (type == null) type = entities.get(entity.toLowerCase());
        if (type == null) throw new IllegalArgumentException("Entidad no soportada: " + entity);
        return type;
    }

    private long count(Class<?> type) {
        return entityManager.createQuery("select count(e) from " + type.getSimpleName() + " e", Long.class)
                .getSingleResult();
    }

    private Object findRequired(Class<?> type, Long id) {
        Object row = entityManager.find(type, id);
        if (row == null) throw new IllegalArgumentException("Registro no encontrado: " + type.getSimpleName() + "#" + id);
        return row;
    }

    private Object instantiate(Class<?> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo crear entidad " + type.getSimpleName(), e);
        }
    }

    private void applyPayload(Object target, Map<String, Object> payload) {
        for (Field field : fields(target.getClass())) {
            if ("id".equals(field.getName()) || isSensitive(field.getName()) || !isEditableField(field)) continue;
            field.setAccessible(true);
            try {
                if (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class)) {
                    Object idValue = payload.get(field.getName() + "Id");
                    if (idValue != null && !String.valueOf(idValue).isBlank()) {
                        field.set(target, entityManager.getReference(field.getType(), convertId(idValue, field.getType())));
                    }
                } else if (payload.containsKey(field.getName())) {
                    field.set(target, convert(payload.get(field.getName()), field.getType()));
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Campo invalido: " + field.getName(), e);
            }
        }
    }

    private Map<String, Object> flatten(Object row) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Method method : row.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || !method.getName().startsWith("get") || "getClass".equals(method.getName())) continue;
            String name = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
            if (isSensitive(name)) continue;
            try {
                Object value = method.invoke(row);
                result.put(name, flattenValue(value));
            } catch (Exception ignored) {
                result.put(name, null);
            }
        }
        return result;
    }

    private Object flattenValue(Object value) {
        if (value == null) return null;
        if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Enum<?>
                || value instanceof LocalDate || value instanceof LocalDateTime || value instanceof LocalTime
                || value instanceof UUID) {
            return value;
        }
        if (value instanceof Collection<?>) return "[coleccion]";
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", readId(value));
        ref.put("label", label(value));
        return ref;
    }

    private Object readId(Object value) {
        try {
            return value.getClass().getMethod("getId").invoke(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String label(Object value) {
        for (String getter : List.of("getNombre", "getCodigo", "getEmail", "getCodigoIso", "getDescripcion")) {
            try {
                Object label = value.getClass().getMethod(getter).invoke(value);
                if (label != null) return String.valueOf(label);
            } catch (Exception ignored) {
            }
        }
        Object id = readId(value);
        return value.getClass().getSimpleName() + (id != null ? " #" + id : "");
    }

    private List<Field> fields(Class<?> type) {
        List<Field> result = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            result.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return result;
    }

    private boolean isEditable(Class<?> type) {
        return !List.of(EjecucionRegla.class, Auditoria.class, HistorialAsignacion.class, EstadisticaCargaAnalista.class)
                .contains(type);
    }

    private boolean isEditableField(Field field) {
        return !java.lang.reflect.Modifier.isStatic(field.getModifiers())
                && !java.lang.reflect.Modifier.isFinal(field.getModifiers())
                && !isSensitive(field.getName())
                && !"serialVersionUID".equals(field.getName());
    }

    private boolean isSensitive(String fieldName) {
        return Set.of("passwordHash", "password", "token", "secret").contains(fieldName);
    }

    private String relationType(Field field) {
        return (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class))
                ? tableName(field.getType())
                : null;
    }

    private Object convert(Object value, Class<?> type) {
        if (value == null) return null;
        String text = String.valueOf(value);
        if (text.isBlank()) return null;
        if (type.equals(String.class)) return text;
        if (type.equals(Long.class) || type.equals(long.class)) return Long.valueOf(text);
        if (type.equals(Integer.class) || type.equals(int.class)) return Integer.valueOf(text);
        if (type.equals(Short.class) || type.equals(short.class)) return Short.valueOf(text);
        if (type.equals(Boolean.class) || type.equals(boolean.class)) return Boolean.valueOf(text);
        if (type.equals(BigDecimal.class)) return new BigDecimal(text);
        if (type.equals(LocalDate.class)) return LocalDate.parse(text);
        if (type.equals(LocalDateTime.class)) return LocalDateTime.parse(text);
        if (type.equals(LocalTime.class)) return LocalTime.parse(text);
        if (type.equals(UUID.class)) return UUID.fromString(text);
        if (type.isEnum()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object enumValue = Enum.valueOf((Class<Enum>) type.asSubclass(Enum.class), text);
            return enumValue;
        }
        return value;
    }

    private Object convertId(Object value, Class<?> entityType) {
        Class<?> idType = idType(entityType);
        if (idType.equals(UUID.class)) return value instanceof UUID ? value : UUID.fromString(String.valueOf(value));
        if (idType.equals(Long.class) || idType.equals(long.class)) {
            if (value instanceof Number number) return number.longValue();
            return Long.valueOf(String.valueOf(value));
        }
        return value;
    }

    private Class<?> idType(Class<?> entityType) {
        return fields(entityType).stream()
                .filter(field -> field.isAnnotationPresent(jakarta.persistence.Id.class))
                .findFirst()
                .map(Field::getType)
                .orElse(Long.class);
    }

    private String tableName(Class<?> type) {
        jakarta.persistence.Table table = type.getAnnotation(jakarta.persistence.Table.class);
        return table != null && !table.name().isBlank() ? table.name() : type.getSimpleName().toLowerCase();
    }

    private void registrarAuditoria(Authentication authentication, HttpServletRequest request, String accion,
                                    String entidad, Object entidadId, String anterior, String nuevo) {
        UUID usuarioId = null;
        if (authentication != null && authentication.getName() != null) {
            usuarioId = usuarioRepository.findByEmail(authentication.getName()).map(Usuario::getId).orElse(null);
        }
        auditoriaService.registrar(usuarioId, null, accion,
                accion + " en " + entidad + (entidadId != null ? " #" + entidadId : ""),
                request != null ? request.getRemoteAddr() : null,
                request != null ? request.getHeader("User-Agent") : null,
                entidad, entidadId, anterior, nuevo);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    public record EntitySummary(String key, String table, Long count, Boolean editable) {}
    public record EntitySchema(String key, String table, Boolean editable, List<FieldSchema> fields) {}
    public record FieldSchema(String name, String type, String relationType, Boolean relation, Boolean editable) {}
}
