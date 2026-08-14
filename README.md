# 🚨 CompaSOS

## Información del Proyecto

**Nombre del proyecto:** CompaSOS

**Nombre de los estudiantes:** José Andrés Gutiérrez Vargas, Ana María Barrientos Guerrero

**Grupo:** GIDS6093

---

# Beneficiario y problemática atendida

El proyecto fue validado por **Saul Aguayo Arredondo**, Sub director de la **Escuela Secundaria Técnica No 26**.

En una situación de emergencia (una caída, una descompensación de salud, un accidente doméstico o un riesgo en la vía pública) lo que determina el desenlace es el tiempo que transcurre entre el incidente y el momento en que alguien se entera. Actualmente ese aviso depende de una cadena frágil de condiciones: que la persona traiga el teléfono consigo, esté consciente, pueda desbloquearlo, ubique al contacto correcto y logre explicar dónde se encuentra. Basta con que una sola de esas condiciones falle para que la ayuda se retrase o no llegue.

La problemática se agudiza en quienes pasan tiempo solos o dependen de un tercero: adultos mayores, personas con enfermedades crónicas o alguna discapacidad, y quienes se trasladan por zonas inseguras. Del otro lado, sus familiares y cuidadores no tienen una manera sencilla de saber si están bien sin llamar de forma constante, lo que impone una carga de vigilancia permanente que, aun así, no garantiza una reacción oportuna.

A lo anterior se suma una limitación tecnológica de fondo: las alternativas disponibles viven en un solo dispositivo, casi siempre el teléfono, que es justamente el que no siempre está encima ni al alcance de la mano. No aprovechan el reloj inteligente que la persona porta durante todo el día, ni la televisión, que en muchos hogares —sobre todo donde hay adultos mayores— es la pantalla de mayor uso y la más fácil de operar. El resultado es una cobertura fragmentada que deja al usuario desprotegido precisamente en los momentos y lugares donde más necesita apoyo.

CompaSOS atiende esta problemática con una aplicación multiplataforma (móvil Android, wearable Wear OS y pantalla inteligente) que asegura que la alerta de emergencia salga sin importar en cuál de los tres dispositivos se encuentre la persona.

---

# Objetivo

Desarrollar una aplicación multiplataforma que permita brindar asistencia rápida en situaciones de emergencia mediante el envío de alertas, monitoreo de signos vitales y comunicación entre el módulo móvil, el wearable y la pantalla inteligente. El objetivo principal es mejorar la seguridad del usuario proporcionando herramientas que faciliten la solicitud de ayuda de forma inmediata, sin depender de un único dispositivo.

---

# Descripción de las funcionalidades

El módulo móvil constituye el núcleo del sistema. Permite al usuario:

- Registrar su perfil y su red de contactos de confianza, así como datos relevantes en caso de emergencia (tipo de sangre, padecimientos, medicamentos y alergias).
- Activar la alerta SOS mediante una acción simple: botón en pantalla o presión sostenida del botón físico.
- Enviar de forma automática la ubicación en tiempo real, la hora del incidente y un mensaje predefinido a todos los contactos registrados.
- Mantener la app activa en segundo plano sin intervención del usuario, mediante un servicio dedicado.
- Realizar marcación directa a servicios de emergencia.
- Confirmar el estado de "estoy bien" y consultar el historial de eventos.
- Acompañamiento y seguimiento en vivo de trayectos.
- Funcionar como centro de configuración y sincronización del resto de los dispositivos (reloj y pantalla) vinculados a la cuenta.

---

# Tecnologías utilizadas

## Lenguaje de programación

- Kotlin

## Frameworks y herramientas

- Android Studio
- Jetpack Compose
- Room (base de datos local)
- Servicios en segundo plano (Foreground Service)
- Google Play Services / Ubicación
- Material Design

---

# Instrucciones para ejecutar el proyecto

## Requisitos

- Android Studio Hedgehog o superior.
- JDK 17.
- Dispositivo o emulador Android.
- Permisos de ubicación y notificaciones habilitados en el dispositivo de prueba.

## Pasos

1. Clonar el repositorio.

```bash
git clone -b dev https://github.com/gutierrezvargasandy1/CompaSOS_Movil.git
```

2. Abrir el proyecto en Android Studio.

3. Sincronizar las dependencias de Gradle.

4. Ejecutar la aplicación en un dispositivo o emulador Android.

5. Conceder los permisos de ubicación y notificaciones solicitados por la app.

6. Registrar un perfil y contactos de confianza para probar el envío de alertas SOS.

---

# Capturas de pantalla de la aplicación

## Inicio de sesión

<img width="210" height="500" alt="image" src="https://github.com/user-attachments/assets/f8d96141-0763-43aa-aeed-d37c0fd17e42" />


---

## Registro de usuario

<img width="210" height="500" alt="image" src="https://github.com/user-attachments/assets/94db63ae-921d-4ec2-b580-6e07157aa831" />


---

## Alarma de emergencia

<img width="210" height="500" alt="image" src="https://github.com/user-attachments/assets/49470c80-2d4e-4aeb-a2c3-a7cddd41c73b" />


---

## Detalle de cuenta en aplicación

<img width="210" height="500" alt="image" src="https://github.com/user-attachments/assets/5c552515-56f1-49a7-8f17-659b7e9508dd" />



---

## Dispositivos vinculados

<img width="210" height="500" alt="image" src="https://github.com/user-attachments/assets/de4ff045-91c3-42bd-b103-04367171ead4" />



---

# Estructura del proyecto

```
CompaSOS_Movil/
├── app/
│   └── src/main/java/com/utng/compasos_movil/
│       ├── data/
│       │   ├── dao/
│       │   └── entity/
│       ├── navigation/
│       └── ui/
│           ├── screens/
│           └── theme/
├── gradle/
└── README.md
```

---

# Autores

**José Andrés Gutiérrez Vargas**

**Ana María Barrientos Guerrero**

**Grupo:** GIDS6093

---

# Licencia

Proyecto desarrollado con fines académicos para la Universidad Tecnológica del Norte de Guanajuato.
