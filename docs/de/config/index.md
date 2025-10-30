---
title: Konfiguration
---

## Globale Konfiguration

### Allgemeine Einstellungen

Im Bereich "Allgemeine Einstellungen" unter "Administration" - "Einstellungen" - "Custom Properties"
kann das Plugin generell aktiviert bzw. deaktiviert werden.
Zudem ist es möglich, Konfigurationen auf Namespace-Ebene zu verhindern.
Dazu muss der Button zum Bearbeiten der jeweiligen Einstellung betätigt werden.
Dies navigiert den Benutzer zu einer Unterseite, auf der die Einstellung vorgenommen werden kann.

![Allgemeine Einstellungen der globalen Konfiguration](./assets/general_settings_globally.png)

### Vordefinierte Schlüssel

Um den Benutzer beim Anlegen von Custom Properties zu unterstützen, können vordefinierte Schlüssel angelegt werden.
Um einen neuen Schlüssel anzulegen, muss der Button "Custom Property vordefinieren" betätigt werden.
Dadurch wird der Benutzer zu einer neuen Unterseite navigiert.
Hier kann der Benutzer in einem Text-Input einen neuen Schlüssel definieren.
Für vordefinierte Schlüssel gelten die gleichen Validierungsregeln, wie beim Anlegen einer Custom Property ([siehe Dokumentation zur Übersicht](../overview)).

Die vordefinierten Schlüssel können auch in der Schlüsselübersicht mithilfe der jeweiligen Aktions-Buttons bearbeitet und gelöscht werden.

![Definition von Schlüsseln auf globaler Ebene](./assets/predefined_keys_globally.png)

## Namespace-Konfiguration

Auf Namespace-Ebene können weitere Schlüssel definiert werden.
Diese werden entsprechend nur für Repositories des jeweiligen Namespaces vorgeschlagen.
Das Erstellen, Bearbeiten und Löschen der Schlüssel erfolgt dabei analog zu der globalen Konfiguration.
Zusätzlich werden global definierte Schlüssel ebenfalls in der Schlüsselübersicht aufgelistet und mit dem Tag "Globale Vorlage" gekennzeichnet.
Diese können in der Namespace-Ebene nicht bearbeitet werden.

![Definition von Schlüsseln auf Namespace-Ebene](./assets/predefined_keys_namespace.png)
