# BatchVerarbeitung mit Akka

Dieses Projekt dient dazu, die Batchverarbeitung mit Hilfe von [Akka](http://akka.io) zu implementieren.

Es wird eine Datei eingelesen (csv), die Zeilen werden in Datensätze konvertiert und diese dann verarbeitet.
Anschliessend werden die Daten wieder in Zeilen konvertiert und in eine Ausgabedatei geschrieben. Die benötigte Zeit
wird gemessen. Zum Vergleich mit einer nicht parallelisierten Version gibt es eine serielle Variante

*Es gelten die folgenden Anforderungen:*

- die Reihenfolge der Daten in der Ausgabedatei entspricht der in der Eingabedatei, auch ohne dass in der Eingabedatei
 eine laufende Nummer enthalten ist.
- die Verarbeitung muss auch mit beliebig grossen Dateien funktionieren, ohne dass Speicherprobleme auftreten.

*Für diesen Test wird die folgende Verarbeitung durchgeführt:*

 - Erzeugen eines Datensatzes, der ausser den erzeugten Daten auch den Originalstring enthält
 - Umwandeln aller Felder in Großbuchstaben
 - Beim Schreiben eines Datensatzes wird der Originalstring verwendet, damit hinterher mit dem diff-Befehl geprüft
 werden kann, ob die Dateien gleich sind

# die verwendeten Aktoren

Im folgenden werden die beteiligten Aktoren und die zwischen ihnen ausgetauschten Nachrichten beschrieben. Es wird
ein Pull-Konzept verwendet, das dafür sorgt, dass die anstehende Arbeit auf die Worker verteilt wird,
welche gerade frei sind. Die Idee hierfür stammt aus diesem [Blogeintrag](http://www.michaelpollmeier.com/akka-work-pulling-pattern/)

![Aktoren-Grafik](https://bitbucket.org/sothawo/akkabatch/downloads/AkkaBatch.svg)

## Reader

Der Reader ist die Steuerungszentrale des Verarbeitungsprozesses. Bei ihm registrieren sich die CSV2Record Aktoren,
er bekommt den Auftrag zur Verarbeitung einer Datei. Der Reader liest die Daten satzweise ein,
so dass immer nur eine maximal definierte Anzahl von Recods im System ist. Dadurch wird verhindert,
dass zum einen das System mit Daten überflutet wird, und zum anderen, das der Writer zu viele Daten puffern muss,
wenn ein Datensatz zu lange in der Verarbeitung  bleibt.

### eingehende Nachrichten

- Register wird von einem CSV2Record gesendet um sich zu registrieren
- InitReader enthält den Namen der zu verarbeitenden Datei nd wird zum Beginn der Verarbeitung von Inbox gesendet.
- GetWork wird von einem CSV2Record gesendet, wenn dieser einen Datensatz verarbeiten kann.
- RecordReceived, enthält eine Record-ID und wird vom Writer gesendet, wenn dieser den entsprechenden Datensatz
erhalten hat. Der Reader entfernt diesen aus seiner internen List mit evtl. noch einmal zu sendenden Daten.
- RecordsWritten wird vom Writer gesendet, wenn dieser Ausgabedatensätze geschrieben hat. Anhand der übermittelten
Anzahl kann der Reader entsprechend neue Daten einlesen und in die Verarbeitung schicken und auch feststellen,
ob alle Daten verarbeitet wurden.
- SendAgain wird über einen Scheduler regelmässig gesendet und dient dazu, das erneute Verarbeiten von Datensätzen,
die nicht in der vorgegebenen Zeit beim Writer angekommen sind, anzustossen.

### ausgehende Nachrichten

- WorkAvailable wird an alle registrierten CSV2Record Aktoren gesendet, wenn neue Daten eingelesen oder aber schon
einmal versendete Daten nicht nnerhalb einer konfigurierten Zeit beim Writer angekommen sind.
- DoWork, enthält Record-Id und CSV Zeile, wird an einen CVS2Record Aktor gesendet,
wenn dieser die Nachricht GetWork an den Reader gesendet hat und es Daten zur Verarbeitung gibt.
- WorkDone wird an Inbox gesendet, wenn alle Daten verarbeitet wurden

## Writer

Die Aufgabe des Writers ist es, die erhaltenen Datensätze in die Ausgabedatei zu schreiben,
so dass die ursprüngliche Reihenfolge beibehalten wird. Hierfür hat der Writer einen Puffer,
in welchen  die Sätze gespeichert werden, die noch nicht geschrieben werden können,
weil sie im Laufe der Verarbeitung eine anderen Datensatz überholt haben. Die Verwaltung dieser Sätze geschieht
anhand der Record-ID, welche bei 0 beginnt und sequentiell steigt.

Der Writer hat eine Referenz auf den Reader, welche aus der Konfiguration ermittelt wird.

### eingehende Nachrichten

- InitWriter, enthält den Namen der Ausgabedatei. Wird vom Inbox Objekt des Programms aufgerufen und setzt die
internen Datenstrukturen zurück.
- ProcessRecord (enthält die Record-ID, die Originalzeile und den Datensatz), enthält die zu schreibenden Daten

### ausgehende Nachrichten

- InitReady wird an Inbox gesendet wenn ein InitWriter empfangen wurde und die internen Strukturen initialisiert
wurden, enthält ein Flag, ob die Initialisierung erfolgreich war.
- RecordReceived, enthält eine Record-ID und wird an den Reader gesendet, wenn ein ProcessRecord empfangen wurde,
damit der Reader den entsprechenden Record nicht noch einmal in die Verarbeitung schickt.
- RecordsWritten wird an den Reader gesendet, wenn Datensätze in die Ausgabe geschrieben wurden. Die Nachricht
enthält die Anzahl der geschriebenen Records

## CSV2Record
Der Aktor ist dafür zuständig, aus einer Zeile im CSV Format einen Datensatz zu erzeugen.

### eingehende Nachrichten
- WorkAvailable, wird vom Reader an die registrierten CSV2Record Aktoren gesendet,
wenn Daten zur Verarbeitung vorhanden sind.
- DoWork, (enthält die Record-ID und die csv-Zeile), wird vom Reader gesendet, nachdem der CSV2Record sich bei diesem
 mit GetWork gemeldet hat

### ausgehende Nachrichten
- Register (enthält die Kennung des CSV2Record), wird am Anfang an den Reader gesendet,
um sich bei diesem zu registrieren.
- GetWork, wird an den Reader gesendet, wenn der Actor bereit ist zum Arbeiten. Dies ist der Fall nach der
Verarbeitung eines vorhergegangene Record oder wenn der Actor die Nachricht WorkAvailable empfangen hat.
- ProcessRecord (enthält die Record-ID, die Originalzeile und den Datensatz), wird an den RecordModifier gesendet.
Der Name des RecordModifier wird über die Konfiguration ermittelt.

## RecordModifier
Der Aktor führt die eigentliche Verarbeitung durch.

### eingehende Nachrichten
- ProcessRecord (siehe CSV2Record)

### ausgehende Nachrichten
- ProcessRecord (der enthaltene Datensatz ist bearbeitet, die Nachricht ist **nicht** die eingelieferte!),
wird an den Writer gesendet. Der Name des Writer wird über die Konfiguration ermittelt.

