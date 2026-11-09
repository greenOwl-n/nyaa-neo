package dev.nyaaneo.model

import org.junit.Assert.*
import org.junit.Test

class ReleaseInfoTest {
    private fun info(title: String, code: String = "1_2") = ReleaseInfo.from(title, code, "Anime")

    @Test fun detectsFansubLanguagesAndVideoTags() {
        val release = info("[NeoSubs] Frieren - 02 [1080p WEB-DL] [ESP LATAM ENG] [Dual-Audio] [Multi-Subs]")
        assertEquals("NeoSubs", release.group)
        assertEquals(setOf(ReleaseLanguage.SPANISH, ReleaseLanguage.ENGLISH), release.languages)
        assertTrue(release.multiSub)
        assertTrue(release.multiAudio)
        assertEquals("1080p", release.resolution)
        assertEquals("WEB-DL", release.source)
        assertEquals("Frieren", release.animeQuery)
    }
    @Test fun doesNotTreatEraiRawsAsUntranslatedRelease() {
        val release = info("[Erai-raws] Anime - 01 [1080p][Multi-Subs]")
        assertEquals("Erai-raws", release.group)
        assertFalse(release.raw)
        assertTrue(release.multiSub)
        assertTrue(release.languages.isEmpty())
        assertFalse(LanguageFilter.SPANISH.accepts(release))
    }
    @Test fun englishCategoryDoesNotInventAudioOrSubtitleTracks() {
        val release = info("[SubsPlease] Anime - 01 [1080p]")
        assertTrue(release.englishCategory)
        assertTrue(release.languages.isEmpty())
        assertEquals("Inglés · categoría", release.languageLabel)
        assertTrue(LanguageFilter.ENGLISH.accepts(release))
    }
    @Test fun doesNotInferLanguageFromGroupName() {
        val release = info("[SpanishSub] Anime - 01", "1_3")
        assertTrue(release.languages.isEmpty())
        assertEquals("Idioma sin indicar", release.languageLabel)
    }
    @Test fun rawCategoryDoesNotInventJapaneseAudio() {
        val release = info("[BDMV] Some Anime", "1_4")
        assertTrue(release.raw)
        assertNull(release.group)
        assertTrue(release.languages.isEmpty())
        assertTrue(LanguageFilter.RAW.accepts(release))
    }
    @Test fun ignoresTechnicalPrefixesInsteadOfDisplayingThemAsFansubs() {
        assertNull(info("[1080p HEVC] Some Anime - 01").group)
        assertNull(info("[A1B2C3D4] Some Anime - 01").group)
    }
    @Test fun recognizesAccentedSpanishAndLanguageCodesInsideTags() {
        val release = info("[NeoSubs] Anime [Español][FR][PT-BR]", "1_3")
        assertTrue(release.languages.containsAll(listOf(ReleaseLanguage.SPANISH, ReleaseLanguage.FRENCH, ReleaseLanguage.PORTUGUESE)))
        assertTrue(LanguageFilter.SPANISH.accepts(release))
    }
    @Test fun cleansSeasonEpisodeAndAlternateTitleForPosterLookup() {
        assertEquals("JoJos Bizarre Adventure", info("[Group] JoJos Bizarre Adventure S06E08 [1080p] (Alternate name)").animeQuery)
        assertEquals("Some Anime", info("[NeoSubs] - Some Anime - 01v2 [720p].mkv").animeQuery)
    }
    @Test fun doesNotSearchAnimeArtworkForSoftwareOrAmvs() {
        assertNull(ReleaseInfo.from("[Group] Software", "6_1", "Software - Applications").animeQuery)
        assertNull(ReleaseInfo.from("[Group] Music Video", "1_1", "Anime - AMV").animeQuery)
    }
    @Test fun unknownLanguageRemainsUnknown() {
        val release = info("Anime - 01 [1080p]", "1_3")
        assertTrue(LanguageFilter.OTHER.accepts(release))
        assertTrue(LanguageFilter.ALL.accepts(release))
        assertFalse(LanguageFilter.SPANISH.accepts(release))
    }
    @Test fun leadingLanguageCodeIsNotConfusedWithGroup() {
        val release = info("[ES] Anime - 01 [1080p]", "1_3")
        assertNull(release.group)
        assertTrue(LanguageFilter.SPANISH.accepts(release))
    }
    @Test fun supportsSortingByLeechers() {
        val releases = listOf(Torrent("1", "A", "", leechers = 2), Torrent("2", "B", "", leechers = 12))
        assertEquals("2", releases.sortedByOrder(SortOrder.LEECHERS).first().id)
    }
}
