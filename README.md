# Repositorio de extensión Mihon — LectorXD

Extensión no oficial para leer **lectorxd.com** en Mihon (y forks: Komikku, TachiyomiSY, etc.).

Repositorio ya publicado y listo para usar. Solo tienes que **añadir su URL en Mihon**:

```
https://raw.githubusercontent.com/Ikiggai/mihon-lectorxd/main/index.min.json
```

## Contenido

```
index.min.json   ← índice que lee Mihon (no lo renombres)
index.json       ← misma info, legible (opcional)
apk/
  tachiyomi-es.lectorxd-v1.4.1.apk   ← la extensión compilada y firmada
icon/
  eu.kanade.tachiyomi.extension.es.lectorxd.png
```

- **Fuente:** LectorXD · idioma `es` · `https://lectorxd.com`
- **Paquete:** `eu.kanade.tachiyomi.extension.es.lectorxd`
- **Versión:** 1.4.1 (versionCode 104001)

---

## Opción A — Añadir el repositorio en Mihon (recomendado, con autoactualización)

1. En Mihon: **Más → Ajustes → Explorar → Repositorios de extensiones → Añadir**,
   pega esta URL y guarda:
   ```
   https://raw.githubusercontent.com/Ikiggai/mihon-lectorxd/main/index.min.json
   ```
   > Mihon descarga los APK automáticamente desde `.../main/apk/…`, por eso la
   > carpeta `apk/` está junto a `index.min.json` en el repo.
2. Ve a **Explorar → Extensiones**, busca **LectorXD** e instálala.
   - La primera vez Mihon avisará de que la firma es desconocida (es normal en
     repos de terceros): acepta / confía en la extensión para instalarla.

Para **actualizar** en el futuro: reemplaza el APK y `index.min.json` en el repo
con una versión nueva (subiendo el `versionCode`) y Mihon detectará la actualización.

## Opción B — Instalar el APK directamente (sin repositorio)

Si solo quieres probarla ya:

1. Pasa `apk/tachiyomi-es.lectorxd-v1.4.1.apk` a tu móvil.
2. Ábrelo y permite instalar apps de orígenes desconocidos.
3. Mihon la reconocerá como fuente instalada.
   (Con este método no hay autoactualización; para eso usa la Opción A.)

---

## Funciones incluidas

- **Populares** (más vistos) y **Recientes**.
- **Búsqueda** por texto.
- **Filtros completos:** ordenar por, estado, tipo (manga/manhwa/manhua/novela/one-shot),
  demografía, contenido (general / +18) y **géneros** (varios = cualquiera de ellos).
- **Detalle** (título, portada, géneros, sinopsis, estado), **capítulos** con fecha y **lector**.

## Notas técnicas

- lectorxd.com está tras **Cloudflare**. Mihon lo resuelve automáticamente (usa
  WebView si aparece un desafío). Si alguna vez ves un error de Cloudflare, abre
  la fuente en el WebView de Mihon una vez y vuelve a intentarlo.
- El APK está firmado con la clave de depuración de Android estándar. Mantén esa
  misma firma para que las actualizaciones se apliquen sin desinstalar.

*Extensión no oficial hecha a partir de la estructura pública del sitio. No está
afiliada a LectorXD ni a Mihon/keiyoushi.*
