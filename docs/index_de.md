---
title: Barcode Scanner Dashboard
identifier: intranda_dashboard_barcode
description: Dashboard Plugin für das automatische Übernehmen bzw. Abschließen von Arbeitsschritten sowie zur Änderung von Standortangaben mittels Barcode-Scanner
published: true
keywords:
    - Goobi workflow
    - Plugin
    - Dashboard Plugin
---

## Einführung
Dieses Dashboard-Plugin wurde entwickelt, um die Verwendung eines Barcode-Scanners in Goobi Workflow zu erleichtern. Es ermöglicht auf der rechten Seite der Oberfläche verschiedene Aktionen, wie z.B. das Annehmen und Abschließen von Aufgaben oder auch das Ändern des Standorts für Objekte.

## Installation
Um das Plugin nutzen zu können, müssen folgende Dateien installiert werden:

```bash
/opt/digiverso/goobi/plugins/dashboard/plugin_intranda_dashboard_barcode.jar
/opt/digiverso/goobi/plugins/GUI/plugin_intranda_dashboard_barcode-GUI.jar
```

Um zu konfigurieren, wie sich das Plugin verhalten soll, können verschiedene Werte in der Konfigurationsdatei angepasst werden. Die Konfigurationsdatei befindet sich üblicherweise hier:

```bash
/opt/digiverso/goobi/config/plugin_intranda_dashboard_barcode.xml
```

Für eine Nutzung dieses Plugins muss der Nutzer innerhalb der Einstellungen für das Dashboard den Wert `intranda_dashboard_barcode` auswählen. 

![Auswahl des Dashboards in den Nutzereinstellungen](screen1_de.png)


## Überblick und Funktionsweise
Um dieses Dashboard-Plugin zu nutzen, muss man es zunächst über `Einstellungen` -> `Allgemein` -> `Dashboard` aktivieren und sich dann ggf. neu anmelden. Wenn das Plugin korrekt installiert und konfiguriert wurde, sollte es bereits unter dem Menüpunkt `Dashboard` aktiviert sein.

![Das Annehmen und Abschließen von Aufgaben](screen2_de.png)

Auf der rechten Seite befindet sich ein Formular mit verschiedenen Aktionen. Sie können eine auswählen, indem Sie darauf klicken. Wird die Aktion `Nur Ortsänderung` gewählt, gibt es ein zusätzliches Eingabefeld, das den Namen des neuen Orts erwartet. 

![Die Erfassung eines Ortswechsels](screen3_de.png)

Für alle Aktionen gibt es ein obligatorisches Eingabefeld, in dem der Titel des Goobi-Vorgangs erwartet wird. Dieses Feld wird nach dem Laden automatisch fokussiert, um die Verwendung eines Barcodescanners zu erleichtern. Durch Anklicken des Buttons `Ausführen` wird die gewählte Aktion ausgeführt, und es werden Meldungen über den Erfolg ausgegeben. Die durchgeführte Aktion sowie der Eingabeort werden zur Erleichterung weiterer Anwendungen gespeichert. Sie bleiben unverändert, bis eine manuelle Änderung vorgenommen wird.

Im Fall, dass Ortwechsel erfasst werden, sind diese auch zu einem späteren Zeitpunkt jederzeit innerhalb des Journals noch nachvollziehbar.

![Gespeicherte Informationen zum Ortswechsel im Journal](screen4_de.png)

Der jeweils aktuelle Aufenthaltsort des Objektes wird darüber hinaus in einer eigenen Eigenschaft gespeichert.

![Aktueller Ort innerhalb einer Eigenschaft](screen5_de.png)

## Konfiguration
Die Konfiguration des Plugins erfolgt in der Datei `plugin_intranda_dashboard_barcode.xml` wie hier aufgezeigt:

{{CONFIG_CONTENT}}

Die folgende Tabelle enthält eine Zusammenstellung der Parameter und ihrer Beschreibungen:

Parameter               | Erläuterung
------------------------|------------------------------------
| `tasks-latestChanges-size` | Dieser Parameter legt fest, wie viele erledigten Aufgaben in der linken Tabelle angezeigt werden sollen. |
| `show-accept-option` | Dieser Parameter legt fest, ob der Button für die Annahme von Aufgaben aktiviert werden soll. Default ist hierfür `false`. |
| `show-finish-option` | Dieser Parameter legt fest, ob der Button für die Beendigung von Aufgaben aktiviert werden soll. Default ist hierfür `false`.|
| `show-accept-and-finish-option` | Dieser Parameter legt fest, ob der Button für die Annahme von Aufgaben und deren Beendigung aktiviert werden soll. Default ist hierfür `false`. |
| `show-change-location-option` | Dieser Parameter legt fest, ob der Button für den Ortswechsel aktiviert werden soll. Default ist hierfür `false`. |