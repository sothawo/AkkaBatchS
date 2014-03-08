Mit diesem Projekt soll eine Batchverarbeitung mit Hilfe von Akka getestet werden.

es wird eine Datei eingelesen (csv), die Zeilen werden in Datensätze konvertiert und diese dann verarbeitet.
Anschliessend werden die Daten wieder in Zeilen konvertiert  und in eine Ausgabedatei geschrieben.

Dabei muss die Reihenfolge der Daten in der Ausgabedatei der in der Eingabedatei entsprechen,
auch ohne dass in der Eingabedatei eine laufenden Nummer enhalten ist.

Die Verarbeitung muss auch mit beliebig grossen Dateien funktionieren, ohne dass Speicherprobleme auftreten.

Für diesen Test wird die folgende Verarbeitung durchgeführt:

 - Erzeugen eines Datensatzes, der ausser den erzeugten Daten auch den Originalstring enthält
 - Umwandeln aller Felder in Großbuchstaben

 - Beim Schreiben eines Datensatzes wird der Originalstring verwendet, damit hinterher mit dem diff-Befehl geprüft
 werden kann, ob die Dateien gleich sind


Ferner gilt:

 - Die Anzahl der von Akka verwendeten Worker muss konfigurierbar sein

 - Das Programm misst die benötigte Gesamtzeit

 - die Ein- und Ausgabedatei sind iso-8859-1


Zum Vergleich mit einer nicht parallelisierten Version gibt es eine serielle Variante

Parameter: <infile> <outfile> <seriell|akka> [numWorker]

numWorker nur bei akka