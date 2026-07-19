package com.antifraude.config;

import com.antifraude.licensing.*;
import com.antifraude.profile.DisponibilidadRepository;
import com.antifraude.profile.DisponibilidadUsuario;
import com.antifraude.transactions.Transaccion;
import com.antifraude.transactions.TransaccionRepository;
import com.antifraude.users.Usuario;
import com.antifraude.users.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@Order(4)
public class SaasSeedRunner implements CommandLineRunner {

    private final EmpresaRepository empresaRepository;
    private final PlanLicenciaRepository planLicenciaRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final ContratoRepository contratoRepository;
    private final PagoRepository pagoRepository;
    private final UsoSuscripcionRepository usoSuscripcionRepository;
    private final RolSistemaRepository rolSistemaRepository;
    private final PermisoSistemaRepository permisoSistemaRepository;
    private final RolPermisoRepository rolPermisoRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final TransaccionRepository transaccionRepository;

    public SaasSeedRunner(EmpresaRepository empresaRepository,
                          PlanLicenciaRepository planLicenciaRepository,
                          SuscripcionRepository suscripcionRepository,
                          ContratoRepository contratoRepository,
                          PagoRepository pagoRepository,
                          UsoSuscripcionRepository usoSuscripcionRepository,
                          RolSistemaRepository rolSistemaRepository,
                          PermisoSistemaRepository permisoSistemaRepository,
                          RolPermisoRepository rolPermisoRepository,
                          UsuarioEmpresaRepository usuarioEmpresaRepository,
                          UsuarioRepository usuarioRepository,
                          DisponibilidadRepository disponibilidadRepository,
                          TransaccionRepository transaccionRepository) {
        this.empresaRepository = empresaRepository;
        this.planLicenciaRepository = planLicenciaRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.contratoRepository = contratoRepository;
        this.pagoRepository = pagoRepository;
        this.usoSuscripcionRepository = usoSuscripcionRepository;
        this.rolSistemaRepository = rolSistemaRepository;
        this.permisoSistemaRepository = permisoSistemaRepository;
        this.rolPermisoRepository = rolPermisoRepository;
        this.usuarioEmpresaRepository = usuarioEmpresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.transaccionRepository = transaccionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Empresa empresa = empresaRepository.findByCodigo("DEMO").orElseGet(() -> empresaRepository.save(Empresa.builder()
                .codigo("DEMO")
                .nombre("Empresa Demo Regula")
                .ruc("80000000-1")
                .emailContacto("admin.empresa@demo.com")
                .telefonoContacto("+595981000000")
                .estado(Empresa.EstadoEmpresa.ACTIVA)
                .build()));
        PlanLicencia plan = planLicenciaRepository.findByCodigo("ANUAL_PRO").orElseGet(() -> planLicenciaRepository.save(PlanLicencia.builder()
                .codigo("ANUAL_PRO")
                .nombre("Plan Anual Pro")
                .descripcion("Licencia anual para operacion antifraude y AML")
                .limiteUsuarios(50)
                .limiteTransaccionesMensuales(100000)
                .limiteConsultasKycMensuales(10000)
                .limiteReportesMensuales(500)
                .modulosIncluidosJson("[\"motor_reglas\",\"alertas\",\"kyc\",\"reportes\",\"auditoria\"]")
                .precioAnual(new BigDecimal("12000.00"))
                .activo(true)
                .build()));

        seedRoles();
        Suscripcion suscripcion = suscripcionRepository.findByEmpresaId(empresa.getId()).stream().findFirst()
                .orElseGet(() -> suscripcionRepository.save(Suscripcion.builder()
                        .empresa(empresa)
                        .planLicencia(plan)
                        .fechaInicio(LocalDate.now())
                        .fechaFin(LocalDate.now().plusYears(1))
                        .estado(Suscripcion.EstadoSuscripcion.ACTIVA)
                        .renovacionAutomatica(true)
                        .build()));
        if (!contratoRepository.existsByNumero("CTR-DEMO-2026")) {
            contratoRepository.save(Contrato.builder()
                    .empresa(empresa)
                    .suscripcion(suscripcion)
                    .numero("CTR-DEMO-2026")
                    .fechaFirma(LocalDate.now())
                    .estado(Contrato.EstadoContrato.VIGENTE)
                    .observaciones("Contrato demo para pruebas de licenciamiento")
                    .build());
        }
        if (pagoRepository.findByEmpresaId(empresa.getId()).isEmpty()) {
            pagoRepository.save(Pago.builder()
                    .empresa(empresa)
                    .suscripcion(suscripcion)
                    .referencia("PAY-DEMO-001")
                    .monto(new BigDecimal("12000.00"))
                    .moneda("USD")
                    .fechaPago(LocalDate.now())
                    .estado(Pago.EstadoPago.PAGADO)
                    .build());
        }
        if (usoSuscripcionRepository.findByEmpresaIdOrderByAnioDescMesDesc(empresa.getId()).isEmpty()) {
            usoSuscripcionRepository.save(UsoSuscripcion.builder()
                    .empresa(empresa)
                    .anio(2026)
                    .mes(7)
                    .usuariosActivos(22)
                    .transaccionesProcesadas(15)
                    .consultasKyc(3)
                    .alertasGeneradas(2)
                    .reportesGenerados(1)
                    .build());
        }
        assignUsers(empresa);
        seedDisponibilidad();
        seedTransaccionesHistoricas(empresa);
    }

    private void seedRoles() {
        List<String> permisos = List.of("EMPRESAS_VER", "EMPRESAS_EDITAR", "LICENCIAS_VER", "LICENCIAS_GESTIONAR",
                "PAGOS_VER", "PAGOS_GESTIONAR", "USUARIOS_VER", "USUARIOS_CREAR", "USUARIOS_EDITAR",
                "REGLAS_VER", "REGLAS_CREAR", "REGLAS_EDITAR", "REGLAS_ACTIVAR", "CATALOGOS_VER",
                "CATALOGOS_EDITAR", "ALERTAS_VER", "ALERTAS_ASIGNAR", "ALERTAS_RESOLVER",
                "CASOS_VER", "CASOS_GESTIONAR", "CASOS_APROBAR", "REPORTES_VER", "REPORTES_GENERAR",
                "AUDITORIA_VER");
        permisos.forEach(codigo -> permisoSistemaRepository.findByCodigo(codigo)
                .orElseGet(() -> permisoSistemaRepository.save(PermisoSistema.builder()
                        .codigo(codigo)
                        .descripcion(codigo.replace('_', ' '))
                        .build())));
        role("ADMIN_GENERAL", "Admin General", RolSistema.TipoRol.GLOBAL);
        role("ADMIN_EMPRESA", "Admin Empresa", RolSistema.TipoRol.EMPRESA);
        role("GERENTE_SUPERVISOR", "Gerente Supervisor", RolSistema.TipoRol.EMPRESA);
        role("ANALISTA", "Analista", RolSistema.TipoRol.EMPRESA);
        role("AUDITOR", "Auditor", RolSistema.TipoRol.EMPRESA);

        grant("ADMIN_GENERAL", permisos);
        grant("ADMIN_EMPRESA", List.of("LICENCIAS_VER", "PAGOS_VER", "USUARIOS_VER", "USUARIOS_CREAR", "USUARIOS_EDITAR", "CATALOGOS_VER", "ALERTAS_VER", "REPORTES_VER", "AUDITORIA_VER"));
        grant("GERENTE_SUPERVISOR", List.of("REGLAS_VER", "REGLAS_CREAR", "REGLAS_EDITAR", "REGLAS_ACTIVAR", "CATALOGOS_VER", "CATALOGOS_EDITAR", "ALERTAS_VER", "ALERTAS_ASIGNAR", "ALERTAS_RESOLVER", "CASOS_VER", "CASOS_GESTIONAR", "CASOS_APROBAR", "REPORTES_VER", "REPORTES_GENERAR"));
        grant("ANALISTA", List.of("ALERTAS_VER", "ALERTAS_ASIGNAR", "ALERTAS_RESOLVER", "CASOS_VER", "CASOS_GESTIONAR", "REPORTES_VER", "REPORTES_GENERAR"));
        grant("AUDITOR", List.of("CATALOGOS_VER", "REPORTES_VER", "AUDITORIA_VER", "CASOS_VER", "ALERTAS_VER"));
    }

    private void role(String codigo, String nombre, RolSistema.TipoRol tipo) {
        rolSistemaRepository.findByCodigo(codigo).orElseGet(() -> rolSistemaRepository.save(RolSistema.builder()
                .codigo(codigo)
                .nombre(nombre)
                .descripcion("Rol inicial " + nombre)
                .tipo(tipo)
                .activo(true)
                .build()));
    }

    private void grant(String rolCodigo, List<String> permisos) {
        RolSistema rol = rolSistemaRepository.findByCodigo(rolCodigo).orElseThrow();
        for (String permisoCodigo : permisos) {
            if (rolPermisoRepository.existsByRolCodigoAndPermisoCodigo(rolCodigo, permisoCodigo)) continue;
            PermisoSistema permiso = permisoSistemaRepository.findByCodigo(permisoCodigo).orElseThrow();
            rolPermisoRepository.save(RolPermiso.builder().rol(rol).permiso(permiso).build());
        }
    }

    private void assignUsers(Empresa empresa) {
        for (int i = 1; i <= 3; i++) {
            assign("admin.general" + i + "@regula.com", "ADMIN_GENERAL", null);
            assign("admin.empresa" + i + "@demo.com", "ADMIN_EMPRESA", empresa);
            assign("supervisor" + i + "@demo.com", "GERENTE_SUPERVISOR", empresa);
            assign("auditor" + i + "@demo.com", "AUDITOR", empresa);
        }
        for (int i = 1; i <= 10; i++) {
            assign("analista" + i + "@demo.com", "ANALISTA", empresa);
        }
    }

    private void assign(String email, String rolCodigo, Empresa empresa) {
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        RolSistema rol = rolSistemaRepository.findByCodigo(rolCodigo).orElse(null);
        if (usuario == null || rol == null || usuarioEmpresaRepository.existsByUsuarioIdAndRolCodigo(usuario.getId(), rolCodigo)) return;
        usuarioEmpresaRepository.save(UsuarioEmpresa.builder()
                .usuario(usuario)
                .empresa(empresa)
                .rol(rol)
                .activo(true)
                .build());
    }

    private void seedDisponibilidad() {
        for (int i = 1; i <= 10; i++) {
            Usuario usuario = usuarioRepository.findByEmail("analista" + i + "@demo.com").orElse(null);
            if (usuario == null || !disponibilidadRepository.findByUsuarioIdAndActivoTrue(usuario.getId()).isEmpty()) continue;
            disponibilidadRepository.save(DisponibilidadUsuario.builder()
                    .usuario(usuario)
                    .tipoEstado("DISPONIBLE")
                    .fechaInicio(LocalDateTime.now())
                    .esProgramado(false)
                    .motivo("Seed para pruebas de asignacion")
                    .activo(true)
                    .build());
        }
    }

    private void seedTransaccionesHistoricas(Empresa empresa) {
        if (transaccionRepository.findByIdentificadorDocumento("12345678").size() >= 15) return;
        for (int i = 1; i <= 15; i++) {
            transaccionRepository.save(Transaccion.builder()
                    .empresa(empresa)
                    .transactionUuid(UUID.randomUUID())
                    .codigo("TX-HIST-" + String.format("%02d", i))
                    .identificadorDocumento("12345678")
                    .cuentaOrigen("CTA-DEMO-001")
                    .cuentaDestino("CTA-DEMO-" + String.format("%03d", i + 1))
                    .monto(BigDecimal.valueOf(250000L + (i * 75000L)))
                    .moneda(i % 3 == 0 ? "USD" : "PYG")
                    .canal(i % 2 == 0 ? "WEB" : "MOVIL")
                    .tipoTransaccion("TRANSFERENCIA")
                    .ipOrigen("192.168.10." + i)
                    .paisOrigen("PY")
                    .fechaTransaccion(LocalDateTime.now().minusDays(i))
                    .scoreRiesgo(BigDecimal.valueOf(10L + i))
                    .estadoEvaluacion(Transaccion.EstadoEvaluacion.APROBADA)
                    .estado("PROCESADA")
                    .procesada(true)
                    .fechaProcesamiento(LocalDateTime.now())
                    .build());
        }
    }
}
