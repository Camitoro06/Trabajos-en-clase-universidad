# Buscaminas con Sockets TCP en Java

Proyecto cliente-servidor de Buscaminas construido con los conceptos de la Sesion 8: `java.net`, `ServerSocket`, `Socket`, flujos de E/S, TCP, concurrencia con `ExecutorService`, timeouts y manejo seguro de recursos.

## Que hace

- El servidor escucha por TCP en el puerto `8080`.
- Puede atender varios clientes a la vez mediante un pool fijo de hilos.
- Cada cliente recibe una partida independiente.
- El estado del juego vive en el servidor; el cliente envia comandos de texto.
- Incluye cliente grafico Swing y cliente de consola.
- Las respuestas terminan con la linea `END`, que funciona como delimitador de mensajes a nivel de aplicacion.
- Usa `PrintWriter(..., true)` para auto-flushing.
- Usa `try-with-resources` para cerrar sockets y streams.
- Usa `setSoTimeout(...)` para evitar bloqueos eternos.

## Requisitos

- Java 17 o superior.
- IntelliJ IDEA es opcional. No hay dependencias externas.

## Estructura

```text
src/main/java/com/icesi/buscaminas/
  client/BuscaminasClient.java
  client/BuscaminasSwingClient.java
  server/BuscaminasServer.java
  server/ClientHandler.java
  game/MinesweeperGame.java
  game/GameStatus.java
  game/ActionResult.java
  game/GameSelfTest.java
  protocol/Protocol.java
```

## Ejecutar en IntelliJ

1. Abre la carpeta `BuscaminasSockets` como proyecto.
2. Configura un JDK 17 o superior.
3. Ejecuta primero `com.icesi.buscaminas.server.BuscaminasServer`.
4. Para jugar con interfaz grafica, ejecuta `com.icesi.buscaminas.client.BuscaminasSwingClient`.
5. Tambien puedes usar el cliente de consola `com.icesi.buscaminas.client.BuscaminasClient`.
6. Puedes ejecutar varios clientes a la vez para comprobar la concurrencia.

## Ejecutar en Windows sin Maven

Desde una terminal dentro del proyecto:

```bat
scripts\compile_windows.bat
scripts\run_server_windows.bat
```

En una segunda terminal:

```bat
scripts\run_gui_windows.bat
```

Si prefieres consola:

```bat
scripts\run_client_windows.bat
```

## Ejecutar con Maven

```bash
mvn compile
java -cp target/classes com.icesi.buscaminas.server.BuscaminasServer
```

En otra terminal:

```bash
java -cp target/classes com.icesi.buscaminas.client.BuscaminasClient
```

## Comandos del juego

```text
NUEVO
NUEVO 12 12 20
ABRIR 3 4
BANDERA 5 2
TABLERO
ESTADO
AYUDA
SALIR
```

Tambien se aceptan equivalentes en ingles: `NEW`, `OPEN`, `FLAG`, `BOARD`, `STATUS`, `HELP`, `QUIT`.

## Simbolos

- `#`: casilla oculta
- `F`: bandera
- `*`: mina (se revela al finalizar)
- `1..8`: cantidad de minas vecinas
- espacio en blanco: casilla abierta sin minas vecinas

## Protocolo sencillo

El cliente envia una orden por linea. El servidor responde con una o mas lineas y cierra cada respuesta con:

```text
END
```

Ejemplo:

```text
ABRIR 0 0
```

Respuesta aproximada:

```text
OK Casilla abierta.
STATUS PLAYING
BOARD
    0  1  2 ...
 0 |   1  # ...|
...
END
```

## Correspondencia con la Sesion 8

- `ServerSocket.accept()` espera conexiones TCP.
- `Socket` representa la conexion dedicada con un cliente.
- `BufferedReader` / `InputStreamReader` leen lineas desde la red.
- `PrintWriter(..., true)` escribe y hace auto-flush.
- `ExecutorService` con `newFixedThreadPool(...)` permite atender clientes concurrentes.
- `try-with-resources` evita fugas de sockets/streams.
- `setSoTimeout(...)` evita que una lectura quede bloqueada indefinidamente.
- `setTcpNoDelay(true)` reduce el retraso para mensajes pequenos interactivos.

## Prueba rapida de logica

Con assertions habilitadas:

```bash
java -ea -cp target/classes com.icesi.buscaminas.game.GameSelfTest
```
