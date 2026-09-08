package eu.kanade.tachiyomi.extension.es.lectorxd

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import keiyoushi.annotation.Source
import keiyoushi.network.rateLimit
import keiyoushi.utils.asJsoup
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.Calendar
import kotlin.time.Duration.Companion.seconds

@Source
abstract class LectorXD : HttpSource() {

    override val supportsLatest = true

    // The app's default client already handles Cloudflare; keep requests gentle to avoid challenges.
    override val client = network.client.newBuilder()
        .rateLimit(2, 2.seconds)
        .build()

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    // ============================== Popular ===============================

    override fun popularMangaRequest(page: Int): Request = catalogRequest(page, "views")

    override fun popularMangaParse(response: Response): MangasPage = parseMangaList(response)

    // ============================== Latest ================================

    override fun latestUpdatesRequest(page: Int): Request = catalogRequest(page, "recent")

    override fun latestUpdatesParse(response: Response): MangasPage = parseMangaList(response)

    private fun catalogRequest(page: Int, orderBy: String): Request {
        val url = "$baseUrl/catalogo".toHttpUrl().newBuilder()
            .addQueryParameter("orderBy", orderBy)
            .addQueryParameter("page", page.toString())
            .build()
        return GET(url, headers)
    }

    // ============================== Search ================================

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/catalogo".toHttpUrl().newBuilder()

        if (query.isNotBlank()) {
            url.addQueryParameter("search", query.trim())
        }

        filters.forEach { filter ->
            when (filter) {
                is OrderByFilter -> if (filter.selected.isNotBlank()) url.addQueryParameter("orderBy", filter.selected)
                is StatusFilter -> if (filter.selected.isNotBlank()) url.addQueryParameter("status", filter.selected)
                is TypeFilter -> if (filter.selected.isNotBlank()) url.addQueryParameter("_t", filter.selected)
                is DemographyFilter -> if (filter.selected.isNotBlank()) url.addQueryParameter("_d", filter.selected)
                is ContentFilter -> if (filter.selected.isNotBlank()) url.addQueryParameter("adult", filter.selected)
                is GenreFilter -> filter.state.filter { it.state }
                    .forEach { url.addQueryParameter("tags", it.id.toString()) }
                else -> {}
            }
        }

        url.addQueryParameter("page", page.toString())
        return GET(url.build(), headers)
    }

    override fun searchMangaParse(response: Response): MangasPage = parseMangaList(response)

    // ========================= Shared list parsing ========================

    private fun parseMangaList(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(MANGA_CARD_SELECTOR)
            .map(::mangaFromElement)
            .filter { it.url.isNotBlank() }
            .distinctBy { it.url }

        val currentPage = response.request.url.queryParameter("page")?.toIntOrNull() ?: 1
        return MangasPage(mangas, hasNextPage(document, currentPage))
    }

    private fun mangaFromElement(element: Element): SManga = SManga.create().apply {
        setUrlWithoutDomain(element.attr("href"))
        val img = element.selectFirst("img")
        title = img?.attr("alt")?.takeIf { it.isNotBlank() }
            ?: element.selectFirst("h1, h2, h3, h4")?.text().orEmpty()
        thumbnail_url = img?.absUrl("src")?.takeIf { it.isNotBlank() }
            ?: coverFromUrl(url)
    }

    private fun hasNextPage(document: Document, currentPage: Int): Boolean {
        val total = TOTAL_REGEX.find(document.text())
            ?.groupValues?.get(1)?.replace(".", "")?.replace(",", "")?.toIntOrNull()
        return if (total != null) {
            currentPage * PAGE_SIZE < total
        } else {
            document.select(MANGA_CARD_SELECTOR).size >= PAGE_SIZE
        }
    }

    // ============================== Details ===============================

    override fun getMangaUrl(manga: SManga): String = baseUrl + manga.url

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        val path = response.request.url.encodedPath // /manga/slug

        return SManga.create().apply {
            title = document.selectFirst("h1")?.text().orEmpty()
            thumbnail_url = document.selectFirst("img[src*=/covers/], img[data-src*=/covers/]")
                ?.let { it.absUrl("src").ifBlank { it.absUrl("data-src") } }
                ?.takeIf { it.isNotBlank() }
                ?: coverFromUrl(path)

            val genreElements = document.select(GENRE_SELECTOR)
                .ifEmpty { document.select("a[href*=catalogo?tags=]") }
            val genres = genreElements
                .map { it.text().trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .toMutableList()
            // Prepend the type (manga/manhwa/...) taken from the URL.
            path.trim('/').substringBefore('/').takeIf { it.isNotBlank() }?.let { type ->
                genres.add(0, type.replaceFirstChar { it.uppercase() })
            }
            genre = genres.distinct().joinToString(", ")

            description = parseDescription(document)
            status = parseStatus(document)
        }
    }

    private fun parseDescription(document: Document): String? {
        val syn = document.selectFirst(":matchesOwn(^\\s*Sinopsis\\s*\$)")
            ?: document.selectFirst("*:contains(Sinopsis)")
        val container = syn?.parent() ?: return null
        val text = container.text().substringAfter("Sinopsis").trim()
        return text.ifBlank { null }
    }

    private fun parseStatus(document: Document): Int {
        val header = document.selectFirst("h1")?.parent()?.parent()?.text().orEmpty() +
            " " + document.selectFirst("main")?.ownText().orEmpty()
        return when {
            header.contains("Completado", ignoreCase = true) -> SManga.COMPLETED
            header.contains("emisión", ignoreCase = true) ||
                header.contains("emision", ignoreCase = true) -> SManga.ONGOING
            else -> SManga.UNKNOWN
        }
    }

    // ============================== Chapters ==============================

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val mangaPath = response.request.url.encodedPath.removeSuffix("/")

        // The web page only renders the latest ~20 chapters, but the FULL list is embedded
        // (Astro island) inside a <script> as {"chapter":"N","groupId":...} objects.
        val dataScript = document.select("script")
            .maxByOrNull { CHAPTER_JSON_REGEX.findAll(it.data()).count() }
            ?.data()
            .orEmpty()

        val numbers = CHAPTER_JSON_REGEX.findAll(dataScript)
            .map { it.groupValues[1] }
            .distinct()
            .toList()

        // Dates are only present on the rendered rows; map them by chapter number as a bonus.
        val dateByNumber = document.select("a[href*=/leer/]").associate { anchor ->
            val key = anchor.attr("href").substringAfterLast('/').substringBefore('?')
            key to parseRelativeDate(anchor.parent()?.text().orEmpty())
        }

        if (numbers.isNotEmpty()) {
            return numbers.map { number ->
                SChapter.create().apply {
                    setUrlWithoutDomain("$mangaPath/leer/$number")
                    name = "Capítulo $number"
                    chapter_number = number.toFloatOrNull() ?: -1f
                    date_upload = dateByNumber[number] ?: 0L
                }
            }.sortedByDescending { it.chapter_number }
        }

        // Fallback: parse the rendered chapter anchors if the embedded list is missing.
        return document.select("a[href*=/leer/]")
            .mapNotNull { anchor ->
                val href = anchor.attr("href")
                if (href.isBlank() || !href.contains("/leer/")) return@mapNotNull null
                val number = CHAPTER_NUM_REGEX.find(anchor.attr("title"))?.groupValues?.get(1)
                    ?: href.substringAfterLast('/')
                SChapter.create().apply {
                    setUrlWithoutDomain(href)
                    name = "Capítulo $number"
                    chapter_number = number.toFloatOrNull() ?: -1f
                    date_upload = parseRelativeDate(anchor.parent()?.text().orEmpty())
                }
            }
            .distinctBy { it.url }
    }

    override fun getChapterUrl(chapter: SChapter): String = baseUrl + chapter.url

    // =============================== Pages ================================

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        return document.select("img").asSequence()
            .map { img ->
                img.absUrl("data-src").ifBlank { img.absUrl("src") }
                    .ifBlank { img.attr("data-src") }
                    .ifBlank { img.attr("src") }
            }
            .filter { it.contains("cdnlxd") && !it.contains("/covers/") }
            .map { it.substringBefore("?") }
            .distinct()
            .sortedBy { it.substringAfterLast('/').substringBefore('.').toIntOrNull() ?: Int.MAX_VALUE }
            .toList()
            .mapIndexed { index, imageUrl -> Page(index, imageUrl = imageUrl) }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    // =============================== Filters ==============================

    override fun getFilterList() = FilterList(
        Filter.Header("Los filtros se combinan con la búsqueda por texto."),
        OrderByFilter(),
        StatusFilter(),
        TypeFilter(),
        DemographyFilter(),
        ContentFilter(),
        Filter.Separator(),
        GenreFilter(),
    )

    // =============================== Helpers ==============================

    private fun coverFromUrl(path: String): String {
        val slug = path.trim('/').substringAfterLast('/')
        return "https://s1.cdnlxd.xyz/manga/covers/$slug.webp"
    }

    private fun parseRelativeDate(text: String): Long {
        // Strip the material-icon ligatures that surround the date ("schedule", "radio_button_*").
        val cleaned = text.replace(Regex("""radio_button_\w+"""), " ")
        val match = RELATIVE_DATE_REGEX.find(cleaned) ?: return 0L
        val amount = match.groupValues[1].toIntOrNull() ?: return 0L
        val unit = match.groupValues[2].lowercase()
        val calendar = Calendar.getInstance()
        // The site uses short English units (mo, y, w, d, h, min/m, s) plus some Spanish forms.
        // Order matters: check "mo"/"mes" (month) before "m"/"min" (minute).
        when {
            unit.startsWith("mo") || unit.startsWith("mes") || unit.startsWith("month") ->
                calendar.add(Calendar.MONTH, -amount)
            unit == "y" || unit.startsWith("yr") || unit.startsWith("year") ||
                unit.startsWith("añ") || unit.startsWith("an") || unit == "a" ->
                calendar.add(Calendar.YEAR, -amount)
            unit == "w" || unit.startsWith("wk") || unit.startsWith("week") || unit.startsWith("sem") ->
                calendar.add(Calendar.WEEK_OF_YEAR, -amount)
            unit == "d" || unit.startsWith("día") || unit.startsWith("dia") || unit.startsWith("day") ->
                calendar.add(Calendar.DAY_OF_YEAR, -amount)
            unit == "h" || unit.startsWith("hr") || unit.startsWith("hora") || unit.startsWith("hour") ->
                calendar.add(Calendar.HOUR_OF_DAY, -amount)
            unit == "m" || unit.startsWith("min") -> calendar.add(Calendar.MINUTE, -amount)
            unit == "s" || unit.startsWith("seg") || unit.startsWith("sec") ->
                calendar.add(Calendar.SECOND, -amount)
            else -> return 0L
        }
        return calendar.timeInMillis
    }

    companion object {
        private const val PAGE_SIZE = 24

        // Series cards link to /{type}/{slug} (no /leer/ suffix) and contain a cover image.
        private const val MANGA_CARD_SELECTOR =
            "a[href~=^/(manga|manhwa|manhua|novela|one[_-]?shot)/[^/]+\$]:has(img)"

        // Genre badges on the details page sit in a flex-wrap container and link to /catalogo?tags=<id>.
        private const val GENRE_SELECTOR = "div[class*=flex-wrap] a[href*=tags=]"

        private val TOTAL_REGEX = Regex("""de\s+([\d.,]+)\s+series""")
        private val CHAPTER_NUM_REGEX = Regex("""Cap[ií]tulo\s+([\d.]+)""", RegexOption.IGNORE_CASE)

        // Full chapter list embedded in the page: {"chapter":"12","groupId":null}
        private val CHAPTER_JSON_REGEX = Regex(""""chapter":"([^"]+)"""")

        // Matches the "schedule <n><unit>" relative date, e.g. "schedule 2d", "schedule 3 meses".
        private val RELATIVE_DATE_REGEX =
            Regex("""schedule\s*(\d+)\s*([a-záéíóúñ]+)""", RegexOption.IGNORE_CASE)
    }
}
