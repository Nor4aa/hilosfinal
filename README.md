# QuizLiveHilos
Aplicación tipo **Quiz Live** multi-sala desarrollada con **Spring Boot (MVC)** y un motor concurrente con **hilos** para gestionar varias salas en paralelo.

Este proyecto responde a los dos enunciados de la práctica:

- **ENUNCIADO 1 — SPRING BOOT (MVC)**: estructura en capas (Controller, Service, Repository, Model) y vistas Thymeleaf.
- **ENUNCIADO 2 — PSP (HILOS / CONCURRENCIA)**: motor concurrente para gestionar salas, temporizadores y respuestas simultáneas.

---
## 1. Arquitectura MVC (ENUNCIADO 1)

### 1.1. Capas principales
- **Model / Entity** (`com.example.demo.model`):
  - `User`: anfitrión del juego. Tiene una lista de `Block` (banco de preguntas por usuario).
  - `Block`: bloque/colección de preguntas. Relación `ManyToOne` con `User` y `OneToMany` con `Question` (borrado en cascada de preguntas cuando se elimina el bloque).
  - `Question`: enunciado + 4 opciones (`op1..op4`) + índice de la correcta (`respuCorrect`).
  - `Room`: sala con PIN único, anfitrión, bloque elegido y configuración: nº de preguntas, segundos por pregunta, modo aleatorio y estado `WAITING/RUNNING/FINISHED`.
  - `Player`: jugador dentro de una sala, con su puntuación.
  - `RoomQuestion`: asociación sala–pregunta con campo `order` para posibles órdenes manuales.
  - `Answer`: respuesta de un jugador a una pregunta (no imprescindible para el motor concurrente pero preparada para extensiones).

- **Repository (JPA)** (`com.example.demo.repository`):
  - `UserRepository`, `BlockRepository`, `QuestionRepository`, `RoomRepository`.

- **Service** (`com.example.demo.service`):
  - `UserService`: registro/login simplificados de anfitriones.
  - `BlockService`: lógica de bloques y preguntas (incluye validaciones: enunciado obligatorio, 4 opciones no vacías, índice correcto 1–4).
  - `RoomService`: creación de salas con **PIN único** y validación de mínimo **20 preguntas por bloque** para poder crear una sala.

- **Controller** (`com.example.demo.controller`):
  - `LobbyController`: flujo de anfitrión (login sencillo, dashboard, creación de salas, lobby con PIN) y flujo de jugador para unirse mediante PIN + nombre.
  - `GameController`: endpoints que el host y los jugadores consultan en vivo (polling) para recibir preguntas, estado y ranking, y para enviar respuestas.

- **Vistas Thymeleaf** (`src/main/resources/templates`):
  - `index.html`: pantalla inicial "Soy Anfitrión" / "Soy Jugador" (login anfitrión + unirse con PIN y nombre).
  - `dashboard.html`: dashboard del anfitrión con resumen de "Mis Bloques" y formulario para crear una sala eligiendo bloque, nº de preguntas y tiempo por pregunta.
  - `lobby-host.html`: vista del anfitrión con el **PIN de la sala**, botón de inicio, pregunta actual, temporizador y **ranking en vivo / lista de jugadores conectados**.
  - `game-player.html`: vista del jugador para recibir preguntas en tiempo real, responder y ver el **ranking final** al terminar la partida.

> Nota: La gestión completa CRUD de bloques/preguntas se apoya en `BlockService` y entidades JPA. La UI actual muestra los bloques del anfitrión y valida que el bloque tenga ≥ 20 preguntas antes de poder crear una sala.

---
## 2. Motor concurrente (ENUNCIADO 2 – PSP)

El motor concurrente vive en el paquete `com.example.demo.engine` y está formado por dos clases principales:

- `GameEngine`: gestor global de salas activas.
- `ActiveRoom`: representación en memoria de una sala que está en juego.

### 2.1. Multi-sala concurrente (A)

- `GameEngine` mantiene un **mapa concurrente**:
  - `private final ConcurrentHashMap<String, ActiveRoom> activeRooms = new ConcurrentHashMap<>();`
  - Clave = `PIN` de la sala, Valor = instancia `ActiveRoom`.
- Esto permite tener **varias salas activas en paralelo**, cada una con su estado totalmente aislado:
  - pregunta actual (`currentQuestionIndex`),
  - temporizador propio,
  - mapa de jugadores y puntuaciones,
  - registro de quién ha contestado ya la pregunta actual.

### 2.2. Temporizador por sala y pregunta (B)

En `ActiveRoom` se usa:

- `ScheduledExecutorService timerScheduler = Executors.newSingleThreadScheduledExecutor();`
- Cada vez que se llama a `nextQuestion()` se programa una tarea:
  - Cierra la pregunta cuando expira el tiempo (`secondsPerQuestion`).
  - Marca la pregunta como cerrada (`isQuestionOpen = false`).
  - Lanza la siguiente pregunta tras una breve pausa.
- El log del temporizador incluye **PIN de sala y nombre del hilo** (por ejemplo):
  - `[Room 4321] [pool-2-thread-1] [Timer] Time up for question 3!`

Esto cumple el requisito de tener un temporizador por sala y pregunta ejecutado en un hilo independiente al procesamiento de respuestas.

### 2.3. Procesamiento concurrente de respuestas (C)

También en `ActiveRoom` se usa un **pool de hilos** para procesar respuestas:

- `ExecutorService answerProcessor = Executors.newFixedThreadPool(10);`
- Cada llamada a `submitAnswer(playerName, selectedOption)` envía un `Runnable` al pool.
- Dentro de cada tarea se comprueba:
  - Si la pregunta sigue abierta (`isQuestionOpen`).
  - Si el jugador ya ha respondido a esa pregunta (`playerAnsweredCurrentQuestion.putIfAbsent(...)`).
  - Si la respuesta es correcta (`q.getRespuCorrect() == selectedOption`).
  - Se actualiza la puntuación con `playerScores.compute(...)` (+1 por acierto, 0 por fallo).
- Los logs incluyen **PIN + nombre del hilo** para cada respuesta procesada, por ejemplo:
  - `[Room 4321] [pool-1-thread-2] Processing answer for Ana: 2 (Correct: true)`

### 2.4. Sincronización y consistencia (D)

Para evitar condiciones de carrera se usan las siguientes estructuras y primitivas concurrentes:

- `ConcurrentHashMap<String, Integer> playerScores` para puntuaciones.
- `ConcurrentHashMap<String, Boolean> playerAnsweredCurrentQuestion` para marcar si un jugador ya ha respondido a la pregunta actual.
- `AtomicInteger currentQuestionIndex` para el índice de la pregunta actual.
- `AtomicBoolean isQuestionOpen` para saber si se aceptan o no respuestas.

Reglas que se garantizan:

- **No hay respuestas duplicadas**: `putIfAbsent` en `playerAnsweredCurrentQuestion` hace que la segunda respuesta del mismo jugador se rechace.
- **No se aceptan respuestas fuera de tiempo**: si `isQuestionOpen` es `false`, la respuesta se rechaza y se loguea como fuera de tiempo.
- **Ranking coherente**: las actualizaciones de puntuación se hacen con `compute` sobre `ConcurrentHashMap`, evitando pérdida de actualizaciones entre hilos.

### 2.5. Demostración de concurrencia (E)

Para ver la concurrencia en ejecución:

1. Levanta la aplicación (`mvn spring-boot:run` o desde tu IDE).
2. Crea **dos o más salas** desde diferentes navegadores o pestañas (anfitriones distintos).
3. Haz que varios jugadores se unan a cada sala y respondan en paralelo.
4. Observa en la consola los logs con el siguiente patrón:
   - `"[Room {PIN}] [pool-1-thread-X] Processing answer ..."`
   - `"[Room {PIN}] [pool-2-thread-1] [Timer] Time up for question ..."`

Se aprecia así que hay **múltiples hilos** ejecutándose (distintos `pool-*-thread-*`) y **varias salas activas** gestionadas de forma independiente.

---
## 3. Notas sobre requisitos funcionales

- **Bloques y preguntas (CRUD)**: las entidades y servicios están preparados para gestionar bloques propios de cada usuario (`BlockService`, relación `User`–`Block`–`Question`). La UI actual muestra los bloques del anfitrión y utiliza un bloque sembrado automáticamente (`DataSeeder`) para garantizar un bloque con ≥ 20 preguntas.
- **Mínimo 20 preguntas por bloque**: validado en `RoomService.createRoom(...)`; si el bloque tiene menos de 20 preguntas se lanza una excepción y no se crea la sala.
- **Propiedad por usuario**: los bloques se consultan siempre filtrando por `owner` (método `getBlocksByUser`), por lo que un anfitrión solo ve sus propios bloques.
- **Evitar nombres duplicados en una sala**: `ActiveRoom.addPlayer` devuelve `false` si el nombre ya existe en el mapa de puntuaciones, y el `LobbyController` redirige con error si un jugador intenta entrar con un nombre ya usado en esa sala.
- **Un jugador en una sola sala a la vez**: a nivel de implementación web se usa la sesión HTTP; cada pestaña/navegador mantiene un único `pin` asociado a un jugador.
- **Ranking y resultados**:
  - El anfitrión ve un ranking en vivo en `lobby-host.html`.
  - Los jugadores, al finalizar la partida, ven el **ranking final** en `game-player.html` gracias a los datos devueltos por `/game/player/poll`.

Esta documentación sirve como justificación de las decisiones de diseño tomadas para la parte de **PSP (hilos/concurrencia)** y como guía rápida de la estructura del proyecto para la parte **Spring Boot MVC**.
