-- V22: Semilla de documentos legales (Términos y Condiciones / Política de Privacidad)
-- Cumple: Ley 7593/2025, Ley 1334/1998, Ley 4868/2013, Ley 6534/2020
-- Ejecuta SIEMPRE (no depende de perfil demo)

-- ============================================================
-- TÉRMINOS Y CONDICIONES DE USO — v1
-- ============================================================
INSERT INTO documento_legal (tipo, version, titulo, contenido, activo, fecha_creacion, fecha_publicacion)
VALUES (
    'TERMINOS',
    1,
    'Términos y Condiciones de Uso — Plataforma Antifraude Regula',
    $tc$
TERMINOS Y CONDICIONES DE USO
Plataforma Antifraude Regula — Versión 1.0
Fecha de última actualización: Agosto 2026

1. OBJETO Y ALCANCE

1.1. Los presentes Términos y Condiciones (en adelante, los "Términos") regulan el uso de la plataforma antifraude Regula (en adelante, la "Plataforma"), proporcionada por Regula S.A. (en adelante, "REGULA"), persona jurídica constituida bajo las leyes de la República del Paraguay.

1.2. La Plataforma está destinada exclusivamente al análisis, prevención y detección de fraude financiero, lavado de activos y financiamiento del terrorismo, en cumplimiento de la normativa paraguaya aplicable.

1.3. Al acceder, registrarse o utilizar la Plataforma, el usuario (en adelante, el "USUARIO") acepta expresamente e irrevocablemente los presentes Términos. Si el USUARIO no está de acuerdo con alguno de los términos aquí establecidos, deberá abstenerse de utilizar la Plataforma.


2. DEFINICIONES

Para los fines de los presentes Términos, se entenderá por:

• "Plataforma": el sistema antifraude proporcionado por REGULA, incluyendo su interfaz web, APIs, motor de reglas y todos sus módulos.
• "USUARIO": toda persona física que acceda o utilice la Plataforma en nombre de una empresa cliente.
• "EMPRESA": la persona jurídica que contrata los servicios de REGULA y cuyos usuarios acceden a la Plataforma.
• "Datos Personales": toda información referida a una persona física identificada o identificable, conforme a la Ley N° 7.593/2025.
• "Dato Sensible": aquel que revela origen racial o étnico, salud, orientación sexual, creencias religiosas o filosóficas, afiliación sindical, datos biométricos o genéticos, conforme al Art. 3 de la Ley N° 7.593/2025.
• "KYC": Procedimiento de Conocimiento del Cliente (Know Your Customer).
• "PEP": Persona Expuesta Políticamente.
• "RLS": Row Level Security (aislamiento de datos por empresa).


3. REGISTRO Y CUENTA

3.1. El registro en la Plataforma es exclusivo por invitación. Cada EMPRESA recibe un código de invitación que permite el registro de sus USUARIOS autorizados.

3.2. El USUARIO es responsable de mantener la confidencialidad de sus credenciales de acceso (correo electrónico y contraseña) y de todas las actividades que ocurran bajo su cuenta.

3.3. El USUARIO se compromete a notificar inmediatamente a REGULA sobre cualquier uso no autorizado de su cuenta.

3.4. REGULA se reserva el derecho de suspender o cancelar cuentas que incumplan los presentes Términos, previa notificación al USUARIO y a la EMPRESA.


4. USO DE LA PLATAFORMA

4.1. La Plataforma está destinada exclusivamente a:
    a) Evaluar transacciones financieras en busca de patrones sospechosos.
    b) Generar alertas de fraude basadas en reglas de negocio configurables.
    c) Gestionar casos de investigación de operaciones sospechosas.
    d) Verificar identidad de clientes (KYC) mediante consultas a fuentes externas.
    e) Generar reportes regulatorios para organismos de control.

4.2. El USUARIO se compromete a:
    a) Utilizar la Plataforma de conformidad con la normativa vigente del Paraguay.
    b) No intentar acceder no autorizado a áreas restringidas o a datos de otras EMPRESAS.
    c) No interferir con el funcionamiento de la Plataforma ni intentar vulnerar sus sistemas de seguridad.
    d) Reportar cualquier incidente de seguridad o vulnerabilidad descubierta.
    e) No utilizar la Plataforma para fines distintos a los previstos en el presente documento.
    f) No compartir credenciales de acceso con terceros.


5. ROLES Y PERMISOS

5.1. La Plataforma cuenta con los siguientes roles:
    • Administrador: acceso total a la configuración, usuarios y datos de la EMPRESA.
    • Supervisor: acceso a reglas de negocio, escenarios y simulaciones.
    • Analista: acceso a casos, alertas y reportes de investigación.
    • Auditor: acceso de solo lectura a registros de auditoría.

5.2. Los permisos de cada rol son asignados por el Administrador de la EMPRESA y pueden ser modificados en cualquier momento.


6. PROPIEDAD INTELECTUAL

6.1. Todos los derechos de propiedad intelectual sobre la Plataforma, incluyendo pero no limitándose a software, algoritmos, bases de datos, interfaces y documentación, pertenecen exclusivamente a REGULA.

6.2. Los presentes Términos no conceden al USUARIO ningún derecho de propiedad intelectual sobre la Plataforma, salvo el derecho limitado de uso descrito en el presente documento.

6.3. Queda prohibida la reproducción, distribución, modificación o ingeniería inversa de cualquier parte de la Plataforma sin autorización previa y por escrito de REGULA.


7. CONFIDENCIALIDAD

7.1. El USUARIO se obliga a mantener la confidencialidad absoluta de toda información a la que tenga acceso en el marco del uso de la Plataforma, incluyendo pero no limitándose a:
    a) Datos de transacciones y clientes de la EMPRESA.
    b) Configuraciones de reglas de negocio y escenarios de fraude.
    c) Resultados de evaluaciones y alertas generadas.
    d) Metodologías y algoritmos del motor de reglas.

7.2. Esta obligación de confidencialidad subsiste aun después de finalizada la relación entre el USUARIO y REGULA o entre el USUARIO y la EMPRESA, conforme al Art. 4 inc. j) de la Ley N° 7.593/2025.


8. LIMITACIÓN DE RESPONSABILIDAD

8.1. La Plataforma proporciona herramientas de análisis y alerta. Las decisiones basadas en los resultados de la Plataforma son responsabilidad exclusiva de la EMPRESA y del USUARIO.

8.2. REGULA no será responsable por:
    a) Decisiones adoptadas o no adoptadas en base a los resultados de la Plataforma.
    b) Daños indirectos, incidentales o consecuentes derivados del uso de la Plataforma.
    c) Pérdidas financieras derivadas de operaciones detectadas o no detectadas.
    d) Disponibilidad temporal de la Plataforma por mantenimiento o causas de fuerza mayor.

8.3. La responsabilidad total de REGULA en ningún caso excederá el monto pagado por la EMPRESA durante los doce (12) meses anteriores al hecho que origine la reclamación.


9. DISPONIBILIDAD Y MANTENIMIENTO

9.1. REGULA se esforzará por mantener la Plataforma disponible las 24 horas del día, los 7 días de la semana, salvo por mantenimiento programado o causas de fuerza mayor.

9.2. REGULA notificará con antelación razonable cualquier mantenimiento programado que pueda afectar la disponibilidad de la Plataforma.


10. MODIFICACIONES

10.1. REGULA se reserva el derecho de modificar los presentes Términos en cualquier momento. Las modificaciones serán notificadas a la EMPRESA con al menos treinta (30) días de anticipación.

10.2. El uso continuado de la Plataforma después de la notificación de modificaciones constituirá la aceptación de las mismas.

10.3. Los presentes Términos serán versionados. Cada versión incluirá la fecha de última actualización.


11. TERMINACIÓN

11.1. Cualquiera de las partes podrá terminar la relación en los términos establecidos en el contrato de prestación de servicios suscrito entre REGULA y la EMPRESA.

11.2. En caso de terminación, el USUARIO deberá cesar inmediatamente el uso de la Plataforma y eliminar cualquier dato obtenido de la misma, salvo aquellos que la EMPRESA esté obligada a conservar por normativa aplicable.


12. LEY APLICABLE Y JURISDICCIÓN

12.1. Los presentes Términos se rigen por las leyes de la República del Paraguay.

12.2. Para la resolución de cualquier controversia derivada de los presentes Términos, las partes se someten a la jurisdicción de los tribunales competentes de la ciudad de Asunción, República del Paraguay.


13. NORMATIVA APLICABLE

Los presentes Términos complementan y se leen en conjunción con:
• Ley N° 7.593/2025 — De Protección de Datos Personales.
• Ley N° 1.334/1998 — De Defensa del Consumidor y del Usuario.
• Ley N° 4.868/2013 — De Comercio Electrónico.
• Ley N° 6.534/2020 — De Protección de Datos Personales Crediticios.
• Ley N° 6.022/2017 — De Prevención del Lavado de Activos y Financiamiento del Terrorismo.
• Contrato de Prestación de Servicios vigente entre REGULA y la EMPRESA.
$tc$,
    TRUE,
    now(),
    now()
)
ON CONFLICT (tipo, version) DO NOTHING;

-- ============================================================
-- POLÍTICA DE PRIVACIDAD — v1
-- ============================================================
INSERT INTO documento_legal (tipo, version, titulo, contenido, activo, fecha_creacion, fecha_publicacion)
VALUES (
    'POLITICA_PRIVACIDAD',
    1,
    'Política de Privacidad — Plataforma Antifraude Regula',
    $pp$
POLÍTICA DE PRIVACIDAD
Plataforma Antifraude Regula — Versión 1.0
Fecha de última actualización: Agosto 2026

En cumplimiento de la Ley N° 7.593/2025 "De Protección de Datos Personales" y su deber de transparencia (Art. 4, inc. f), Regula S.A. (en adelante, "REGULA") pone a disposición de sus usuarios la presente Política de Privacidad.


1. RESPONSABLE DEL TRATAMIENTO

Regula S.A.
Domicilio: Asunción, República del Paraguay
Correo electrónico de contacto: privacidad@regula.com.py
En adelante, el "RESPONSABLE".


2. DATOS PERSONALES RECOPILADOS

2.1. Datos de identificación del usuario:
    • Nombre completo
    • Correo electrónico institucional
    • Rol y permisos dentro de la Plataforma
    • Dirección IP de acceso
    • Registros de actividad (logs de auditoría)

2.2. Datos de clientes (personas sujetas a evaluación antifraude):
    • Número de documento de identidad
    • Nombre y datos de contacto
    • Información transaccional (montos, cuentas, fechas, canales)
    • Resultados de consultas KYC (verificación de identidad)
    • Clasificación de riesgo y nivel de alerta

2.3. Datos biométricos:
    • Imágenes de documentos de identidad (DNI, cédula) para verificación KYC
    • Estos datos son tratados exclusivamente para la verificación de identidad y no son almacenados permanentemente en la Plataforma

2.4. Datos de uso de la Plataforma:
    • Timestamps de acceso y acciones realizadas
    • Información del navegador y dispositivo
    • Registros de errores y rendimiento


3. FINALIDAD DEL TRATAMIENTO

Los datos personales son tratados para las siguientes finalidades:

    a) Prevención y detección de fraude financiero, lavado de activos y financiamiento del terrorismo (Ley N° 6.022/2017).

    b) Verificación de identidad de clientes (KYC) en cumplimiento de la normativa antilavado.

    c) Evaluación de riesgo y generación de alertas para operaciones sospechosas.

    d) Generación de reportes regulatorios para organismos de control (UAF, Banco Central del Paraguay).

    e) Gestión de casos de investigación y generación de Reportes de Operaciones Sospechosas (ROS).

    f) Cumplimiento de obligaciones legales y regulatorias aplicables.

    g) Gestión administrativa de la cuenta del USUARIO y auditoría de accesos.


4. BASE LEGAL DEL TRATAMIENTO

Conforme al Art. 5 de la Ley N° 7.593/2025, el tratamiento se fundamenta en:

    a) Consentimiento del titular: otorgado al aceptar los presentes Términos y Condiciones y la Política de Privacidad.

    b) Cumplimiento de obligación legal: la Plataforma es una herramienta de cumplimiento regulatorio obligatorio para entidades sujetas a la Ley N° 6.022/2017.

    c) Interés legítimo del responsable: la prevención del fraude financiero constituye un interés legítimo que prevalece sobre los derechos del titular en los casos previstos por ley.


5. PRINCIPIOS DEL TRATAMIENTO

El tratamiento de datos personales se rige por los principios establecidos en el Art. 4 de la Ley N° 7.593/2025:

    • Exactitud: los datos reflejan fielmente la información proporcionada.
    • Licitud: el tratamiento se realiza de manera lícita y leal.
    • Finalidad: los datos se recopilan para fines determinados y explícitos.
    • Minimización: se recopilan únicamente los datos necesarios para las finalidades descritas.
    • Limitación de conservación: los datos se conservan por el tiempo necesario.
    • Lealtad y transparencia: el titular es informado del tratamiento de sus datos.
    • Seguridad: se adoptan medidas técnicas y organizativas adecuadas.
    • Confidencialidad: las personas que intervienen en el tratamiento están sujetas al deber de confidencialidad.


6. COMPARTICIÓN DE DATOS

6.1. Los datos personales podrán ser compartidos con:
    a) Organismos de control y regulación (UAF, BCP) en cumplimiento de obligaciones legales.
    b) Entidades financieras habilitadas para consultas antifraude.
    c) Proveedores tecnológicos que prestan servicios de infraestructura (cloud hosting), bajo contratos de confidencialidad y procesamiento de datos.

6.2. Los datos NO serán compartidos con terceros para fines de marketing, publicidad o fines distintos a los descritos en la presente Política.

6.3. En caso de transferencia internacional de datos (Art. 19, Ley N° 7.593/2025), REGULA garantizará que el país receptor ofrezca un nivel de protección adecuado o adoptará garantías apropiadas (cláusulas contractuales estándar, normas corporativas vinculantes).


7. SEGURIDAD DE LOS DATOS

7.1. REGULA implementa las siguientes medidas de seguridad (Art. 16, Ley N° 7.593/2025):
    • Cifrado en tránsito (TLS 1.3) y en reposo (AES-256).
    • Autenticación mediante tokens JWT con expiración configurable.
    • Aislamiento de datos por empresa (Row Level Security).
    • Registro de auditoría de todas las acciones realizadas en la Plataforma.
    • Controles de acceso basados en roles y permisos.
    • Bloqueo de cuenta tras intentos fallidos de acceso.
    • Monitoreo de seguridad y detección de incidentes.

7.2. En caso de un incidente de seguridad que afecte datos personales, REGULA notificará oportunamente a los titulares afectados y a la autoridad de control competente, conforme al Art. 22 de la Ley N° 7.593/2025.


8. CONSERVACIÓN DE DATOS

8.1. Los datos personales serán conservados por el tiempo necesario para cumplir con las finalidades del tratamiento:
    • Datos de usuarios: durante la vigencia de la relación contractual y por los plazos legales aplicables.
    • Datos de transacciones y alertas: conforme a los plazos establecidos por la normativa antilavado (mínimo 10 años).
    • Registros de auditoría: mínimo 5 años conforme a buenas prácticas regulatorias.

8.2. Cumplido el plazo de conservación, los datos serán eliminados o anonimizados de manera segura.


9. DERECHOS DEL TITULAR

9.1. Conforme a los Arts. 17 y 18 de la Ley N° 7.593/2025, el titular de datos tiene derecho a:
    a) Acceso: conocer qué datos son tratados y cómo se utilizan.
    b) Rectificación: solicitar la corrección de datos inexactos.
    c) Supresión: solicitar la eliminación de datos cuando ya no sean necesarios.
    d) Oposición: oponerse al tratamiento de sus datos para fines específicos.
    e) Portabilidad: recibir sus datos en un formato estructurado y de uso común.
    f) Revocación del consentimiento: retirar el consentimiento otorgado en cualquier momento.

9.2. Para ejercer estos derechos, el titular podrá contactar a:
    • Correo electrónico: privacidad@regula.com.py
    • Canal de soporte dentro de la Plataforma

9.3. REGULA responderá a las solicitudes en un plazo máximo de quince (15) días hábiles, conforme al Art. 18 de la Ley N° 7.593/2025.

9.4. El ejercicio de estos derechos no podrá suponer costos adicionales al titular ni obligarlo a desplazamientos desproporcionados.


10. COOKIES Y TECNOLOGÍAS DE RASTREO

10.1. La Plataforma utiliza cookies de sesión estrictamente necesarias para su funcionamiento. Estas cookies son esenciales para mantener la autenticación del USUARIO.

10.2. No se utilizan cookies de rastreo, publicitarias o de análisis de terceros.


11. PROTECCIÓN DE MENORES

11.1. La Plataforma no está dirigida a menores de edad. No se recopilan intencionalmente datos de menores de 18 años.

11.2. En caso de detectarse el tratamiento de datos de un menor, se procederá a su eliminación inmediata.


12. EVALUACIÓN DE IMPACTO

12.1. Dado que la Plataforma trata datos biométricos (imágenes de documentos de identidad para KYC), REGULA ha realizado una Evaluación de Impacto a la Protección de Datos Personales conforme al Art. 14 de la Ley N° 7.593/2025.

12.2. Dicha evaluación está disponible para consulta por parte de la autoridad de control competente.


13. CAMBIOS EN LA POLÍTICA

13.1. REGULA se reserva el derecho de modificar la presente Política de Privacidad. Los cambios serán notificados a los USUARIOS con al menos treinta (30) días de anticipación.

13.2. El uso continuado de la Plataforma después de la notificación constituirá la aceptación de los cambios.

13.3. Cada versión de la Política será versionada y archivada con fecha de publicación.


14. CONTACTO

Para consultas sobre la presente Política de Privacidad:
• Correo electrónico: privacidad@regula.com.py
• Responsable de Protección de Datos: dpo@regula.com.py


15. NORMATIVA APLICABLE

• Ley N° 7.593/2025 — De Protección de Datos Personales.
• Ley N° 6.534/2020 — De Protección de Datos Personales Crediticios.
• Ley N° 4.868/2013 — De Comercio Electrónico.
• Ley N° 1.334/1998 — De Defensa del Consumidor y del Usuario.
• Ley N° 6.022/2017 — De Prevención del Lavado de Activos y Financiamiento del Terrorismo.
$pp$,
    TRUE,
    now(),
    now()
)
ON CONFLICT (tipo, version) DO NOTHING;
