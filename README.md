# Documentación del Proyecto: QuizLiveHilos

## Introducción
**QuizLiveHilos** es un sistema de juegos tipo "Quiz Live" desarrollado con **Spring Boot** que incorpora una arquitectura multijugador basada en **concurrencia y gestión de hilos**. La aplicación permite la creación de salas independientes donde los usuarios pueden competir en tiempo real, respondiendo preguntas gestionadas por un motor dinámico.

---

## 1. Implementación del Patrón MVC (Enunciado 1)

El proyecto sigue una organización limpia en capas, utilizando las capacidades de Spring Boot para separar responsabilidades.

### 1.1. Estructura de Capas e Interacciones
*   **Modelos y Entidades (`com.example.demo.model`):**
    *   `User`: Representa al anfitrión del juego, poseedor de bloques de preguntas.
    *   `Block`: Conjunto de preguntas asociadas a un usuario (incluye borrado en cascada para limpiar preguntas huérfanas).
    *   `Question`: Contiene el enunciado, cuatro opciones de respuesta y el índice de la opción correcta.
    *   `Room`: Define una sesión de juego viva. Controla el PIN, el bloque de preguntas activo, la configuración de tiempo y el estado actual (`WAITING`, `RUNNING`, `FINISHED`).
    *   `Player`: Entidad que rastrea a cada participante y su puntaje dentro de una sala.

*   **Persistencia - Repositorios (`com.example.demo.repository`):**
    *   Uso de interfaces JPA (`UserRepository`, `BlockRepository`, etc.) para gestionar el almacenamiento y recuperación de datos de forma eficiente.

*   **Capa de Servicio - Lógica de Negocio (`com.example.demo.service`):**
    *   `UserService`: Maneja el acceso de los anfitriones.
    *   `BlockService`: Gestiona la integridad de los bancos de preguntas, validando que los datos introducidos sean correctos.
    *   `RoomService`: Se encarga de la generación de PINs únicos y verifica que los bloques cumplan el requisito mínimo de **20 preguntas** antes de iniciar una sala.

*   **Controladores y Presentación (`com.example.demo.controller` y Thymeleaf):**
    *   `LobbyController`: Dirige el flujo de registro, el dashboard personal del anfitrión y la unión de jugadores a las salas.
    *   `GameController`: Expone endpoints para el polling continuo, permitiendo que tanto el host como los jugadores reciban actualizaciones de estado y ranking en vivo.
    *   **Vistas:** Interfaces desarrolladas con **Thymeleaf** (`index.html`, `dashboard.html`, `lobby-host.html`, `game-player.html`) para una experiencia de usuario fluida y reactiva.

---

## 2. Gestión de Concurrencia y Hilos (Enunciado 2 - PSP)

La inteligencia de la aplicación reside en el paquete `com.example.demo.engine`, donde se gestiona el ciclo de vida de las partidas mediante hilos independientes.

### 2.1. Soporte Multi-Sala con Claves Concurrentes
El `GameEngine` utiliza un `ConcurrentHashMap<String, ActiveRoom>` para supervisar todas las sesiones abiertas. El PIN actúa como clave, permitiendo que cada sala (`ActiveRoom`) funcione de manera autónoma, con su propia lógica de avance de preguntas y rankings sin interferencias.

### 2.2. Sistema de Temporizadores Autónomos
Cada sala en juego instancia un `ScheduledExecutorService` (hilo individual programado).
*   Al activar una pregunta, se lanza una tarea programada que expira según el tiempo configurado.
*   Al terminar el tiempo, la pregunta se bloquea automáticamente (`isQuestionOpen = false`) y se prepara la transición a la siguiente fase.
*   Los logs del sistema permiten rastrear qué hilo (`pool-X-thread-Y`) está gestionando el temporizador de cada PIN.

### 2.3. Procesamiento Masivo de Respuestas
Para evitar cuellos de botella al recibir muchas respuestas simultáneas, se utiliza un **Pool de hilos** (`ExecutorService` con 10 hilos fijos).
*   Cada respuesta entrante se delega a un hilo del pool para ser evaluada.
*   Se verifica la validez de la respuesta, si la ventana de tiempo sigue abierta y si el jugador ya ha respondido anteriormente.
*   La concurrencia permite procesar múltiples interacciones de usuarios de forma paralela.

### 2.4. Garantías de Sincronización
Se han empleado estructuras de datos y tipos atómicos para asegurar la consistencia sin bloqueos pesados:
*   `AtomicInteger` y `AtomicBoolean` para estados críticos como el índice de la pregunta y el estado de la ventana de respuesta.
*   `ConcurrentHashMap` junto con métodos como `compute` y `putIfAbsent` para prevenir condiciones de carrera al actualizar puntuaciones o registrar respuestas.

---

## 3. Guía de Ejecución y Validación

### Verificación de Requisitos Técnicos
1.  **Levantamiento:** Iniciar con `mvn spring-boot:run`.
2.  **Prueba de Concurrencia:** Abrir múltiples navegadores para simular varios anfitriones creando salas simultáneas.
3.  **Monitorización:** Observar la consola para verificar que los logs muestran distintos hilos procesando respuestas y temporizadores asociados a sus respectivos PINs.
4.  **Validación de Negocio:** Intentar crear una sala con un bloque de menos de 20 preguntas para confirmar que el sistema bloquea dicha acción.

*Nota: Se incluye un `DataSeeder` para poblar el sistema con datos de prueba iniciales de forma automática.*
