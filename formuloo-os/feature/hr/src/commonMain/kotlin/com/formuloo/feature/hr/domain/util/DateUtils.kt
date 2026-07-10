package com.formuloo.feature.hr.domain.util

/**
 * Petits utilitaires de date sans dépendance externe (pas de kotlinx-datetime dans le projet).
 * Algorithme epoch-day repris de java.time.LocalDate (domaine public) — fiable sur tout le
 * calendrier grégorien, ce qui suffit pour des dates ISO "YYYY-MM-DD" saisies côté formulaire.
 */
internal object DateUtils {

    private fun isLeapYear(year: Int) = (year % 4 == 0) && (year % 100 != 0 || year % 400 == 0)

    private fun toEpochDay(year: Int, month: Int, day: Int): Long {
        val y = year.toLong()
        val m = month.toLong()
        var total = 365 * y
        total += if (y >= 0) {
            (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400
        } else {
            -(-y / 4) + (-y / 100) - (-y / 400)
        }
        total += (367 * m - 362) / 12
        total += (day - 1)
        if (m > 2) {
            total -= 1
            if (!isLeapYear(year)) total -= 1
        }
        return total - 719528 // décalage de l'an 0 vers 1970-01-01
    }

    private fun parseIsoDate(iso: String): Triple<Int, Int, Int>? {
        val parts = iso.split("-")
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        return Triple(year, month, day)
    }

    /** 0 = lundi, … 6 = dimanche (1970-01-01 est un jeudi → index 3). */
    private fun dayOfWeek(epochDay: Long): Int = (((epochDay + 3) % 7 + 7) % 7).toInt()

    private fun fromEpochDay(epochDay: Long): Triple<Int, Int, Int> {
        var zeroDay = epochDay + 719528 - 60
        var adjust = 0L
        if (zeroDay < 0) {
            val adjustCycles = (zeroDay + 1) / 146097 - 1
            adjust = adjustCycles * 400
            zeroDay -= adjustCycles * 146097
        }
        var yearEst = (400 * zeroDay + 591) / 146097
        var doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
        if (doyEst < 0) {
            yearEst -= 1
            doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
        }
        yearEst += adjust
        val marchDoy0 = doyEst.toInt()
        val marchMonth0 = (marchDoy0 * 5 + 2) / 153
        val month = (marchMonth0 + 2) % 12 + 1
        val dayOfMonth = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1
        yearEst += (marchMonth0 / 10)
        return Triple(yearEst.toInt(), month, dayOfMonth)
    }

    /** Convertit des millisecondes époque UTC (ex: sélection d'un Material3 DatePicker) en "YYYY-MM-DD". */
    fun epochMillisToIso(millis: Long): String {
        val epochDay = millis / 86_400_000L
        val (year, month, day) = fromEpochDay(epochDay)
        return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }

    /**
     * Nombre de jours ouvrés (hors samedi/dimanche) entre [startIso] et [endIso] inclus,
     * au format "YYYY-MM-DD". Retourne 0 si les dates sont invalides ou si end < start.
     */
    fun businessDaysBetween(startIso: String, endIso: String): Int {
        val start = parseIsoDate(startIso) ?: return 0
        val end = parseIsoDate(endIso) ?: return 0
        val startEpoch = toEpochDay(start.first, start.second, start.third)
        val endEpoch = toEpochDay(end.first, end.second, end.third)
        if (endEpoch < startEpoch) return 0

        var count = 0
        var current = startEpoch
        while (current <= endEpoch) {
            val dow = dayOfWeek(current)
            if (dow != 5 && dow != 6) count++
            current++
        }
        return count
    }
}
