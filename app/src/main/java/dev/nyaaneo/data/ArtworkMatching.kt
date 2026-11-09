package dev.nyaaneo.data

import dev.nyaaneo.model.ReleaseInfo

/** Keeps subtitle/release cleanup separate from matching actual catalog titles. */
object ArtworkMatching {
    fun normalize(title: String): String = ReleaseInfo.normalize(title)
        .replace("'", "").replace("’", "").replace("&", " and ")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")

    fun score(query: String, candidate: String): Double {
        val a = normalize(query)
        val b = normalize(candidate)
        if (a.isBlank() || b.isBlank()) return 0.0
        if (a == b) return 1.0
        val digits = Regex("\\d+")
        if (digits.findAll(a).map { it.value }.toList() != digits.findAll(b).map { it.value }.toList()) return 0.0
        if (a.replace(" ", "") == b.replace(" ", "")) return 0.99
        // Common romanization differences, while preserving all title/season tokens.
        fun romanize(text: String) = text.replace("ou", "o").replace("uu", "u")
        if (romanize(a) == romanize(b)) return 0.97
        val first = a.split(' ').toSet()
        val second = b.split(' ').toSet()
        val overlap = first.intersect(second).size.toDouble() / maxOf(first.size, second.size)
        if (overlap >= 0.8) return overlap
        if (a.length < 6 || b.length < 6 || a.split(' ').size != b.split(' ').size) return 0.0
        val similarity = 1.0 - distance(romanize(a), romanize(b)).toDouble() / maxOf(a.length, b.length)
        return if (similarity >= 0.90) similarity else 0.0
    }

    private fun distance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        a.forEachIndexed { index, char ->
            val next = IntArray(b.length + 1)
            next[0] = index + 1
            for (j in b.indices) next[j + 1] = minOf(next[j] + 1, previous[j + 1] + 1,
                previous[j] + if (char == b[j]) 0 else 1)
            previous = next
        }
        return previous[b.length]
    }
}
