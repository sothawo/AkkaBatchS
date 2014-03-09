# BatchVerarbeitung mit Akka

Dieses Projekt dient dazu, die Batchverarbeitung mit Hilfe von [Akka](http://akka.io) zu implementieren.

Es wird eine Datei eingelesen (csv), die Zeilen werden in Datensätze konvertiert und diese dann verarbeitet.
Anschliessend werden die Daten wieder in Zeilen konvertiert und in eine Ausgabedatei geschrieben. Die benötigte Zeit
wird gemessen. Zum Vergleich mit einer nicht parallelisierten Version gibt es eine serielle Variante

*Es gelten die folgenden Anforderungen:*

- die Reihenfolge der Daten in der Ausgabedatei entspricht der in der Eingabedatei, auch ohne dass in der Eingabedatei
 eine laufende Nummer enthalten ist.
- die Verarbeitung muss auch mit beliebig grossen Dateien funktionieren, ohne dass Speicherprobleme auftreten.

## Für diesen Test wird die folgende Verarbeitung durchgeführt:

 - Erzeugen eines Datensatzes, der ausser den erzeugten Daten auch den Originalstring enthält
 - Umwandeln aller Felder in Großbuchstaben
 - Beim Schreiben eines Datensatzes wird der Originalstring verwendet, damit hinterher mit dem diff-Befehl geprüft
 werden kann, ob die Dateien gleich sind


