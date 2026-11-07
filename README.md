<p align="center">
  <img src="docs/icon.png" alt="Escriba — el gallo escritor" width="180">
</p>

<h1 align="center">Escriba</h1>

<p align="center">
  <b>Editor, visor y creador de documentos de texto para Linux Mint XFCE.</b><br>
  Sencillo, ligero y 100&nbsp;% offline. Sin publicidad, sin telemetría, sin conexiones a Internet.
</p>

<p align="center">
  <img alt="Plataforma" src="https://img.shields.io/badge/Linux%20Mint-21%2B%20XFCE-87cf3e">
  <img alt="Python" src="https://img.shields.io/badge/Python-3.8%2B-3776ab">
  <img alt="Qt" src="https://img.shields.io/badge/PyQt5-5.14%2B-41cd52">
  <img alt="Licencia" src="https://img.shields.io/badge/licencia-MIT-blue">
</p>

Escriba es algo intermedio entre el Bloc de notas de Windows y un procesador de textos básico: abre y guarda
documentos de texto con o sin formato, los exporta a PDF, tiene vista previa de impresión y, sobre todo, **no pierde
su trabajo** si el programa o el equipo se cierran de golpe.

<p align="center">
  <img src="docs/captura-claro.png" alt="Escriba en tema claro" width="48%">
  <img src="docs/captura-oscuro.png" alt="Escriba en tema oscuro" width="48%">
</p>

## Índice

- [Qué hace](#qué-hace)
- [Requisitos del sistema](#requisitos-del-sistema)
- [Instalación](#instalación)
- [Uso](#uso)
- [Formatos de archivo](#formatos-de-archivo)
- [Recuperación automática](#recuperación-automática)
- [Vista previa de impresión](#vista-previa-de-impresión)
- [Atajos de teclado](#atajos-de-teclado)
- [Desinstalar](#desinstalar)
- [Para desarrolladores](#para-desarrolladores)
- [Limitaciones conocidas](#limitaciones-conocidas)
- [Solución de problemas](#solución-de-problemas)
- [Licencia y créditos](#licencia-y-créditos)

## Qué hace

**Documentos**
- Crear documentos nuevos, abrir los existentes, editarlos, guardar y *Guardar como…* eligiendo carpeta, nombre y formato.
- Varios documentos a la vez en **pestañas**; abrir varios archivos de una vez o arrastrarlos a la ventana.
- **Modo solo lectura** para visualizar un documento sin riesgo de modificarlo.
- Avisa de los **cambios sin guardar** antes de cerrar una pestaña o la aplicación.
- Recuerda la última carpeta usada, mantiene una lista de archivos recientes y avisa antes de sobrescribir un archivo.
- Si ya hay una ventana abierta, los archivos nuevos se abren como pestañas en ella.

**Formato del texto** (en documentos ODT, RTF y HTML)
- Tipo de fuente (cualquiera instalada: Liberation Serif/Sans, DejaVu Sans, etc.) y tamaño.
- Negrita, cursiva y subrayado.
- Color del texto y color de fondo del texto (resaltado).
- Alineación: izquierda, centro, derecha y justificada.
- Interlineado (1,0 a 3,0), saltos de página y márgenes de página.

**Archivos**
- Abre y guarda **TXT, MD, HTML, RTF y ODT**; exporta además a **PDF**.
- Distingue entre **texto plano** (TXT, MD) y **texto con formato** (ODT, RTF, HTML) y nunca promete conservar
  fuentes o colores donde no es posible.
- UTF-8 completo y documentos en español; rutas con espacios y caracteres especiales.
- Escritura **atómica**: nunca deja un archivo a medio escribir.

**Vista previa de impresión y exportación**
- Tamaño de papel (A3, A4, A5, B5, Carta, Legal), orientación, márgenes, saltos de página y zoom.
- Desde la vista previa: exportar a PDF, guardar una copia ODT o imprimir. Se puede volver al editor sin perder nada.

**Recuperación automática**
- Copias de recuperación cada 30 s (configurable), incluso de documentos que nunca se guardaron.
- Restaurar, descartar o revisar cada documento tras un cierre inesperado. **Nunca sobrescribe el original.**

**Interfaz**
- Menús Archivo, Editar, Formato, Ver y Ayuda; barras de herramientas; barra de estado con palabras, caracteres,
  líneas, posición del cursor, formato actual y hora de la última copia de recuperación.
- Buscar y reemplazar (con mayúsculas/palabra completa y resaltado de coincidencias).
- **Tema claro y oscuro**, zoom, pantalla completa y ajuste de línea.

## Requisitos del sistema

| | Requisito |
|---|---|
| Sistema | **Linux Mint 21 o superior (XFCE)**. También debería funcionar en otras distribuciones basadas en Ubuntu/Debian y en otros escritorios, porque usa Qt. |
| Python | 3.8 o superior (Mint 21 trae 3.10) |
| Biblioteca | **PyQt5 5.14 o superior** (Mint 21 trae 5.15) — paquete `python3-pyqt5` |
| Permisos | `.deb`: `sudo`. `install.sh`: instalación por usuario, sin root (solo `sudo` si hay que instalar `python3-pyqt5` con apt) |
| RAM | ≈ 70 MB con un documento abierto (medido en las pruebas) |
| Red | Ninguna. Solo se necesita Internet para descargar `python3-pyqt5` si no lo tiene |

No hacen falta pip, compiladores ni herramientas de desarrollo.

## Instalación

### Opción A — Paquete `.deb` (la más fácil)

Descargue `escriba_1.0.0_all.deb` desde [Releases](https://github.com/CocoLabsHQ/escriba/releases) y, desde la
carpeta donde lo guardó:

```bash
sudo apt install ./escriba_1.0.0_all.deb
```

(También sirve doble clic sobre el archivo e *Instalar paquete*.) `apt` instala solo la dependencia
`python3-pyqt5`. Escriba queda disponible para todos los usuarios, en el menú de aplicaciones (*Oficina*) y como
comando `escriba`. Para quitarlo: `sudo apt remove escriba`.

### Opción B — Desde el código fuente (instalación solo para su usuario)

```bash
sudo apt install git            # si todavía no tiene git
git clone https://github.com/CocoLabsHQ/escriba.git
cd escriba
./install.sh
```

También puede descargar el `.tar.gz` o el `.zip` desde [Releases](https://github.com/CocoLabsHQ/escriba/releases),
descomprimirlo y ejecutar `./install.sh` dentro de la carpeta. El instalador:

1. Comprueba Python y PyQt5. Si falta PyQt5, ejecuta `sudo apt install python3-pyqt5` (pide su contraseña y reintenta
   las descargas si la conexión falla; es seguro volver a ejecutarlo).
2. Copia la aplicación a `~/.local/share/escriba/app`, crea el comando `~/.local/bin/escriba`, instala el icono y añade
   **Escriba** al menú de aplicaciones (categoría *Oficina*). Todo queda dentro de su carpeta personal.

Para probarlo sin instalar: `./run.sh`. No instale las dos opciones a la vez (el menú mostraría dos entradas).

## Uso

- Menú de aplicaciones → **Escriba**, o en terminal: `escriba [archivos…]` (`-r` abre en solo lectura).
- **Archivo → Nuevo / Abrir / Guardar / Guardar como / Exportar / Vista previa / Imprimir**.
- La barra de formato (fuente, tamaño, N/K/S, colores, alineación, interlineado) afecta a la selección o, sin
  selección, a lo que escriba a continuación.
- **Editar → Preferencias**: tema, intervalo de guardado automático y fuentes predeterminadas.
- **Ver → Modo solo lectura** para consultar sin editar.

## Formatos de archivo

| Formato | Tipo | ¿Conserva fuentes, colores y estilos? |
|---|---|---|
| **ODT** | con formato | Sí; incluye tamaño de papel, orientación y márgenes |
| **RTF** | con formato | Sí; incluye papel y márgenes |
| **HTML** | con formato | Fuentes, tamaños, colores y alineación (sin tamaño de papel) |
| **PDF** | solo exportación | Sí; misma paginación que la vista previa |
| **MD** | texto plano | **No**: solo el texto y marcas Markdown básicas (negrita/cursiva) |
| **TXT** | texto plano | **No**: solo el texto |

Al guardar un documento con formato como TXT o MD, Escriba avisa de que se perderá el formato y, tras guardar, el
documento pasa a texto plano para que lo que ve sea lo que hay en el archivo. En un documento de texto plano los
botones de estilo se desactivan; *Formato → Convertir a documento con formato* lo pasa a ODT.

TXT/MD se guardan en UTF-8 y conservan el final de línea (LF/CRLF) y la marca BOM del archivo original. Si un archivo
no está en UTF-8 se abre como Windows-1252 y se avisa. «Arial» y «Times New Roman» solo aparecen si están instaladas;
Mint incluye sus equivalentes métricos Liberation Sans/Serif.

## Recuperación automática

- Cada **30 s** (configurable de 5 a 600 s) se guarda una copia de cada documento con cambios sin guardar, y solo si
  hubo cambios. La escritura es atómica, así que un corte a mitad no deja una copia dañada.
- Las copias viven en `~/.local/share/escriba/recovery/`, **separadas de sus documentos**. Recuperar **nunca
  sobrescribe el archivo original**: abre una pestaña con los cambios y el original solo se toca cuando usted guarda.
- Al abrir Escriba tras un cierre inesperado, fallo del sistema o apagado, aparece un aviso y el diálogo
  **Recuperar documentos**: para cada documento puede *revisar* su contenido, *restaurar* o *descartar* (o hacerlo con
  todos, o decidir más tarde). Si el original cambió después de la copia, se avisa.
- La copia se elimina cuando guarda el documento, cuando deshace hasta el estado guardado o cuando cierra la pestaña
  con «No guardar».
- **Cierre normal con cambios**: *Guardar todo*, *Salir y conservar copias*, *Descartar y salir* o *Cancelar*. Ante
  una señal del sistema (SIGTERM, cierre de sesión) guarda las copias y sale sin perder nada.
- Las copias pendientes se pueden revisar siempre en *Archivo → Recuperar documentos…*.

## Vista previa de impresión

<p align="center"><img src="docs/captura-vista-previa.png" alt="Vista previa de impresión" width="75%"></p>

Muestra el documento paginado con las guías de márgenes; permite cambiar papel, orientación y márgenes (mm), ver una,
dos o todas las páginas, acercar/alejar y navegar. La paginación del ODT al abrirlo en LibreOffice puede variar
ligeramente respecto a la del PDF.

## Atajos de teclado

| Acción | Atajo | Acción | Atajo |
|---|---|---|---|
| Nuevo | Ctrl+N | Buscar | Ctrl+F |
| Abrir | Ctrl+O | Buscar siguiente / anterior | F3 / Mayús+F3 |
| Guardar | Ctrl+S | Reemplazar | Ctrl+H |
| Guardar como | Ctrl+Mayús+S | Negrita / Cursiva / Subrayado | Ctrl+B / I / U |
| Cerrar pestaña | Ctrl+W | Alinear izq. / centro / der. / justif. | Ctrl+L / E / R / J |
| Deshacer / Rehacer | Ctrl+Z / Ctrl+Y | Salto de página | Ctrl+Intro |
| Imprimir | Ctrl+P | Vista previa | Ctrl+Mayús+P |
| Zoom | Ctrl++ / Ctrl+- / Ctrl+0 | Cambiar de pestaña | Ctrl+Re Pág / Av Pág |
| Pantalla completa | F11 | Lista completa | F1 |

## Desinstalar

Si lo instaló con el `.deb`: `sudo apt remove escriba`. Si usó `install.sh`:

```bash
./uninstall.sh            # quita aplicación, comando, icono y entrada del menú (conserva preferencias y copias)
./uninstall.sh --purge    # además borra preferencias y copias de recuperación (pide confirmación)
```

`python3-pyqt5` no se elimina; si no lo usa para otra cosa: `sudo apt remove python3-pyqt5`.

## Para desarrolladores

```
escriba/
  app.py          arranque, argumentos, instancia única, señales del sistema
  mainwindow.py   ventana: menús, pestañas, abrir/guardar/exportar, cierre, recuperación
  editor.py       editor de una pestaña (estado del documento, zoom, saltos de página)
  formatbar.py    barra y acciones de formato         findbar.py   buscar y reemplazar
  fileio.py       lectura/escritura atómica, exportación, paginación y PDF
  docmodel.py     modelo neutro de párrafos y estilos (lo comparten RTF, ODT y recuperación)
  rtf.py odt.py   lectores y escritores propios       formats.py   tabla de formatos
  recovery.py     copias de recuperación              preview.py   vista previa e impresión
  pagesetup.py    papel, orientación, márgenes        dialogs.py   preferencias, recuperación…
  theme.py icons.py config.py   tema, iconos vectoriales y preferencias (~/.config/escriba/escriba.conf)
  resources/      icono de la aplicación (PNG)
tests/            pruebas automáticas
assets/ tools/    imagen fuente del icono y script que genera los PNG (requiere Pillow)
install.sh uninstall.sh run.sh run_tests.sh escriba.desktop requirements*.txt
packaging/build-deb.sh   construye el instalador .deb  ->  dist/escriba_<versión>_all.deb
```

**Pruebas** (sin pantalla, `QT_QPA_PLATFORM=offscreen`):

```bash
pip install -r requirements-dev.txt   # opcional: solo para la prueba que inspecciona el PDF (pypdf)
./run_tests.sh                         # 26 pruebas
```

Cubren la ida y vuelta de los seis formatos, la validez del paquete ODT, el PDF (páginas, tamaño y márgenes), las
codificaciones, la escritura atómica, los flujos de interfaz y la **recuperación con un cierre inesperado real**: un
proceso con la aplicación completa edita varios documentos (uno nunca guardado), recibe `SIGKILL`, y un proceso
nuevo comprueba que todo se restaura con su contenido y estilos, que el original no cambió y que las copias se
limpian al guardar.

Con un entorno virtual en lugar de apt: `python3 -m venv --system-site-packages .venv && . .venv/bin/activate && pip install -r requirements.txt`.

Las contribuciones y los reportes de errores son bienvenidos en [Issues](https://github.com/CocoLabsHQ/escriba/issues).

## Limitaciones conocidas

- ODT y RTF se leen y escriben con código propio y soportan texto, fuentes, tamaños, estilos, colores, alineación,
  interlineado, saltos de página, papel y márgenes. **No** soportan imágenes, tablas ni listas (las listas se
  muestran con viñetas simples). Si abre un ODT/RTF que las contiene, Escriba lo avisa y pide confirmación antes de
  guardar sobre el original, porque ese contenido se perdería: use «Guardar como…» para conservar una copia.
- El zoom es de vista (no se guarda en el documento).
- No incluye corrector ortográfico, encabezados/pies ni numeración de páginas.
- Las pruebas automáticas se ejecutan sin pantalla en Ubuntu 24.04 con PyQt5 5.15; si encuentra algo distinto en su
  Mint, abra un *issue* con la versión de Mint y el mensaje de error.

## Solución de problemas

- **«Falta PyQt5»**: `sudo apt update && sudo apt install python3-pyqt5`.
- **El icono no aparece en el menú**: cierre sesión y vuelva a entrar, o ejecute
  `gtk-update-icon-cache -f -t ~/.local/share/icons/hicolor`.
- **Los diálogos de archivos se ven distintos al resto del escritorio**: son los de Qt; instalar
  `qt5-gtk-platformtheme` hace que la aplicación use el tema GTK de XFCE.
- **El comando `escriba` no se encuentra en la terminal**: `~/.local/bin` entra en el PATH al iniciar sesión; el menú
  de aplicaciones funciona igualmente.

## Licencia y créditos

- Código bajo licencia [MIT](LICENSE) © 2026 CocoLabsHQ. Usa [PyQt5](https://www.riverbankcomputing.com/software/pyqt/)
  (GPLv3), que no se incluye en este repositorio: se instala desde los repositorios de su distribución.
- El icono (el gallo escritor) es obra del autor del proyecto e incluye el logotipo de Linux Mint, marca de sus
  respectivos titulares; su aparición no implica afiliación ni respaldo de Linux Mint.
