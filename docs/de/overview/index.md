---
title: Übersicht
---

Mit diesem Plugin ist es möglich für jedes Repository eine Sammlung von benutzerdefinierten Eigenschaften anzulegen.
Diese Eigenschaften werden als Schlüssel-Wert-Paare definiert.
Um eine Übersicht in Tabellenform von den bereits existierenden Eigenschaften zu sehen,
kann der Reiter "Benutzerdefinierte Eigenschaften" in der Repository-Navigation angeklickt werden.

![Tabelle von bereits angelegten benutzerdefinierten Eigenschaften eines Repositorys](./assets/custom-properties-overview.png)

In dieser Übersicht ist es möglich die bereits angelegen Eigenschaften zu betrachten, zu bearbeiten, zu löschen oder neue Eigenschaften anzulegen.
Mit dem Button "Neuen Eintrag erstellen", welcher sich unterhalb der Tabelle befindet, 
kann sich der Benutzer auf eine weitere Ansicht navigieren lassen, um eine neue Eigenschaft anzulegen.

![Unausgefüllte Ansicht zum Erstellen einer neuen benutzerdefinierten Eigenschaft](./assets/custom-properties-create.png)

In dieser Ansicht kann mithilfe von zwei Text-Inputs der Schlüssel und der dazugehörige Wert definiert werden.
Durch das Betätigen des "Speichern"-Buttons wird die Eigenschaft angelegt.

Beim Anlegen der Eigenschaft ist Folgendes zu beachten: 
1. Der Schlüssel darf nicht länger als 255 Zeichen sein.
2. Es dürfen nur Buchstaben, Ziffern, Punkte, Leerzeichen und Unterstriche verwendet werden.
3. Jeder Schlüssel darf pro Repository nur einmal vergeben werden.

Mehrere Werte für denselben Schlüssel müssen als eine Eigenschaft angelegt werden. Die einzelnen Werte werden mit einem Komma getrennt.
Für den Wert selbst gibt es keine weiteren Begrenzungen oder Validierungsregeln.

Für das Bearbeiten und Löschen von Eigenschaften gibt es in jeder Zeile einen eigenen Button, um den jeweiligen Vorgang zu starten.
Das Bearbeiten einer Eigenschaft erfolgt Analog zum Erstellen eines Eintrags.

Für das Bearbeiten, Löschen und Erstellen von Eigenschaften ist es nötig die Berechtigung für das Modifizieren der Repository-Metadaten zu haben.
Sollte dem Benutzer diese Berechtigung fehlen, werden die entsprechenden Buttons nicht angezeigt.
