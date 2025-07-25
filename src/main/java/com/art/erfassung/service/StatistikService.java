package com.art.erfassung.service;

import com.art.erfassung.model.Erfassung;
import com.art.erfassung.model.Studenten;
import com.art.erfassung.repository.ErfassungRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Serviceklasse zur Berechnung von Statistikdaten für Studenten.
 * <p>
 * Diese Klasse nutzt das {@link ErfassungRepository}, um Erfassungen eines Studenten abzurufen
 * und daraus verschiedene Anwesenheitsstatistiken zu ermitteln.
 */
@Service
public class StatistikService {

    // Repository zur Abfrage der Erfassungsdaten
    private final ErfassungRepository erfassungRepository;

    public StatistikService(ErfassungRepository erfassungRepository) {
        this.erfassungRepository = erfassungRepository;
    }

    /**
     * Berechnet die Anwesenheitsstatistik für einen Studenten anhand seiner ID.
     * <p>
     * Es werden alle Erfassungen des Studenten abgefragt und folgende Statistiken ermittelt:
     * <ul>
     *     <li>Gesamte Anwesenheitsquote (in Prozent) basierend auf dem Anteil der "Anwesend"-Einträge</li>
     *     <li>Anzahl der "Entschuldigt"-Einträge</li>
     *     <li>Anzahl der "Unentschuldigt"-Einträge</li>
     *     <li>Anzahl der "Krank"-Einträge</li>
     *     <li>Anzahl der Verspätungen (Erfassungen, deren Kommentar das Wort "verspätung" enthält)</li>
     * </ul>
     *
     * @param studentId die eindeutige ID des Studenten
     * @return ein {@link StatistikErgebnis} Objekt, das die berechneten Statistiken enthält
     */
    public StatistikErgebnis berechneStudentenstatistik(Integer studentId) {
        // Alle Erfassungen (Attendance Records) für den gegebenen Studenten abrufen.
        List<Erfassung> erfassungen = erfassungRepository.findByStudenten_id(studentId);
        long total = erfassungen.size();
        // Gruppierung der Erfassungen nach Status-Bezeichnung und Zählung der Anzahl pro Status.
        Map<String, Long> statusCount = erfassungen.stream()
                .collect(Collectors.groupingBy(e -> e.getStatus().
                        getBezeichnung(), Collectors.counting()));
        // Berechne die Anzahl der Erfassungen, bei denen im Kommentar der Begriff "verspätung"
        // (unabhängig von Groß-/Kleinschreibung) vorkommt. Falls der Kommentar null ist, wird ein leerer String verwendet.
        long verspaetet = erfassungen.stream()
                .filter(e -> Optional.ofNullable(e.getKommentar()).
                        orElse("").toLowerCase().contains("verspätung"))
                .count();
        // Anzahl der "Anwesend"-Erfassungen aus der Status-Gruppe extrahieren.
        long anwesend = statusCount.getOrDefault("Anwesend", 0L);
        // Berechne den prozentualen Anteil der Anwesenheit. Falls keine Erfassung vorliegt, wird 0.0 Prozent verwendet.
        double prozent = (total > 0) ? ((double) anwesend / total * 100.0) : 0.0;
        // Falls Erfassungen vorhanden sind, wird der Student aus dem ersten Eintrag übernommen.
        // Andernfalls ist der Student null.
        Studenten student = erfassungen.isEmpty() ? null : erfassungen.get(0).getStudenten();
        // Erstelle und gebe das Statistik-Ergebnis zurück.
        return new StatistikErgebnis(student, prozent,
                statusCount.getOrDefault("Entschuldigt", 0L),
                statusCount.getOrDefault("Unentschuldigt", 0L),
                statusCount.getOrDefault("Krank", 0L),
                verspaetet);
    }

    /**
     * Record zur Darstellung des Statistik-Ergebnisses eines Studenten.
     *
     * @param student           der Student, für den die Statistik erstellt wurde
     * @param gesamtAnwesenheit der prozentuale Anteil der Anwesenheit
     * @param entschuldigt      Anzahl der entschuldigten Fehlzeiten
     * @param unentschuldigt    Anzahl der unentschuldigten Fehlzeiten
     * @param krank             Anzahl der Krankmeldungen
     * @param verspaetungen     Anzahl der Verspätungen
     */
    public record StatistikErgebnis(
            Studenten student,
            double gesamtAnwesenheit,
            long entschuldigt,
            long unentschuldigt,
            long krank,
            long verspaetungen) {
    }
}
