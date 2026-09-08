@file:Suppress("SpellCheckingInspection")

package eu.kanade.tachiyomi.extension.es.lectorxd

import eu.kanade.tachiyomi.source.model.Filter

/** Simple single-choice filter that maps the visible label to a query value. */
open class UriPartFilter(displayName: String, private val vals: Array<Pair<String, String>>) : Filter.Select<String>(displayName, vals.map { it.first }.toTypedArray()) {
    val selected get() = vals[state].second
}

class OrderByFilter :
    UriPartFilter(
        "Ordenar por",
        arrayOf(
            Pair("Recientes", "recent"),
            Pair("Mejor valorados", "rating"),
            Pair("Más vistos", "views"),
            Pair("Más gente leyendo", "leyendo"),
            Pair("Más gente por leer", "por_leer"),
            Pair("Más completados", "completado"),
            Pair("Más capítulos", "chapters"),
        ),
    )

class StatusFilter :
    UriPartFilter(
        "Estado",
        arrayOf(
            Pair("Todos", ""),
            Pair("En emisión", "en_emision"),
            Pair("Completado", "completado"),
        ),
    )

class TypeFilter :
    UriPartFilter(
        "Tipo",
        arrayOf(
            Pair("Todos", ""),
            Pair("Manga", "manga"),
            Pair("Manhwa", "manhwa"),
            Pair("Manhua", "manhua"),
            Pair("Novela", "novela"),
            Pair("One Shot", "one_shot"),
        ),
    )

class DemographyFilter :
    UriPartFilter(
        "Demografía",
        arrayOf(
            Pair("Todas", ""),
            Pair("Shounen", "shounen"),
            Pair("Seinen", "seinen"),
            Pair("Shoujo", "shoujo"),
            Pair("Josei", "josei"),
            Pair("Otros", "ninguno"),
        ),
    )

class ContentFilter :
    UriPartFilter(
        "Contenido",
        arrayOf(
            Pair("Solo general", "safe"),
            Pair("Todo", "all"),
            Pair("Solo +18", "adult"),
        ),
    )

class Genre(name: String, val id: Int) : Filter.CheckBox(name)

class GenreFilter :
    Filter.Group<Genre>(
        "Géneros (varios seleccionados = cualquiera de ellos)",
        listOf(
            Genre("Romance", 27),
            Genre("Drama", 30),
            Genre("Accion", 23),
            Genre("Fantasia", 24),
            Genre("Comedia", 28),
            Genre("Aventura", 25),
            Genre("Ecchi", 33),
            Genre("Harem", 21),
            Genre("Recuentos de la vida", 35),
            Genre("Artes Marciales", 54),
            Genre("Reencarnacion", 29),
            Genre("Sobrenatural", 36),
            Genre("Magia", 48),
            Genre("Tragedia", 46),
            Genre("Academia", 53),
            Genre("Ciencia Ficción", 55),
            Genre("Psicológico", 52),
            Genre("Misterio", 34),
            Genre("Isekai", 47),
            Genre("Superpoderes", 49),
            Genre("Venganza", 50),
            Genre("Sistema de Niveles", 26),
            Genre("Primer amor", 37),
            Genre("Horror", 56),
            Genre("Girls love", 61),
            Genre("Apocaliptico", 63),
            Genre("Cultivo", 60),
            Genre("Gore", 57),
            Genre("Milf", 31),
            Genre("NTR", 42),
            Genre("Thriller", 58),
            Genre("Mujer casada", 64),
            Genre("Mujer mayor", 51),
            Genre("Relacion secreta", 41),
            Genre("Amigos de la infancia", 22),
            Genre("Universidad", 38),
            Genre("Historias cortas", 45),
            Genre("Boys Love", 62),
            Genre("Rape", 59),
            Genre("Pareja casada", 43),
            Genre("Amigos con derechos", 44),
            Genre("Madre e hija", 32),
            Genre("Ejercito", 40),
            Genre("Realidad virtual", 66),
            Genre("Madrastra", 39),
            Genre("Vampiros", 67),
            Genre("Intercambio de parejas", 144),
            Genre("AV", 135),
            Genre("Hombres lobo", 65),
            Genre("Seinen", 12),
            Genre("Fantasía", 160),
            Genre("Acción", 147),
            Genre("Yaoi", 146),
            Genre("Historia", 149),
            Genre("Vida Escolar", 157),
            Genre("Slice of Life", 158),
            Genre("HETERO", 167),
            Genre("Puto-Amo", 168),
            Genre("Webtoon", 170),
            Genre("Reencarnación", 172),
            Genre("Renacimiento", 169),
            Genre("Murim", 171),
            Genre("Supervivencia", 151),
            Genre("Maduro", 148),
            Genre("DOUJINSHI", 19),
            Genre("Deportes", 164),
            Genre("Telenovela", 159),
            Genre("Escolar", 165),
            Genre("Apocalíptico", 162),
            Genre("Smut", 166),
            Genre("Bebés bonitos", 154),
            Genre("Serpiente x ratoncito", 152),
        ),
    )
