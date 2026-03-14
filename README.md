# Klasio

Multitenant web platform for sports league and training program management. It centralizes membership control, payment validation, attendance tracking, and hour consumption.

## Project structure

```
klasio/
├── api/    # Backend
├── web/    # Frontend
```

## Main features

- **Multitenancy**: Each league operates in isolation with its own programs, students, and configuration.
- **Program management**: Programs with levels, professors, classes, and schedules.
- **Memberships**: Full lifecycle — purchase, activation, hour consumption, and automatic monthly expiration.
- **Payments**: Proof of payment upload by students and validation by administrators.
- **Attendance**: Students register intent, professors/managers mark attendance with automatic hour deduction.
- **Roles**: Superadmin, Admin, Manager, Professor, and Student with differentiated permissions.

---

## Español

Plataforma web multitenant para la gestión integral de ligas deportivas y programas de formación. Centraliza el control de membresías, validación de pagos, registro de asistencia y consumo de horas.

### Estructura del proyecto

```
klasio/
├── api/    # Backend
├── web/    # Frontend
```

### Funcionalidades principales

- **Multitenancy**: Cada liga opera de forma aislada con sus propios programas, estudiantes y configuración.
- **Gestión de programas**: Programas con niveles, profesores, clases y horarios.
- **Membresías**: Ciclo completo de compra, activación, consumo de horas y expiración mensual automática.
- **Pagos**: Subida de comprobantes por el estudiante y validación por el administrador.
- **Asistencia**: Registro de intención por el estudiante, marcación por profesor/manager con descuento automático de horas.
- **Roles**: Superadmin, Administrador, Manager, Profesor y Estudiante con permisos diferenciados.
