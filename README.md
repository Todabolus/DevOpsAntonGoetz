# CleverCash - Semesterprojekt Java2
Verbesserungsversuch Java1 Gruppe Stratmann - (Richard Prax, Anton Götz)

***
## Inhaltsverzeichnis
1. [Projektidee](#projektidee)
2. [Technologien](#technologien)
3. [Abhängigkeiten](#abhängigkeiten)
4. [How to run](#how-to-run)
5. [Links und Login](#links-and-login-data)
6. [pgAdmin Einweisung](#pg-admin-instructions)
7. [REST Endpunkte testen](#rest-endpunkte)
8. [Weitere Befehle](#weitere-befehle)
9. [Dokumentation](#dokumentation)
10. [Personal](#personal)
***
## Projektidee
CleverCash ist eine Finanzverwaltungsplattform, die Teams mit mehreren Beteiligten
einen einfachen Zugang und Überblick über ihre Finanzen bietet. Die Plattform
ermöglicht eine transparente Darstellung der gesamten finanziellen Situation
eines Teams, sowohl insgesamt als auch für einzelne Beteiligte. Einnahmen
und Ausgaben werden übersichtlich und visuell dargestellt, um eine einfache
Nachverfolgung zu ermöglichen.

Mit CleverCash können Beteiligte flexibel Geld zur Verfügung stellen und 
haben die Möglichkeit, mehr Mitbestimmung über Finanzentscheidungen zu erlangen.
Darüber hinaus erleichtert die Plattform alltägliche Aufgaben wie das Bestellen
von Pizza für die Kollegschaft oder das Buchen von Mietwagen. Ein weiteres
Highlight ist die Möglichkeit, gemeinsam auf größere Anschaffungen, wie einen
hochwertigen Kaffee-Vollautomaten, zu sparen. CleverCash macht die Finanzverwaltung
kollaborativer und flexibler, um den Bedürfnissen von Teams und Gemeinschaften gerecht
zu werden.
***
## Technologien
Technologien, welche innerhalb des Projektes verwendet wurden:
* Java 21
* SpringBoot
* Gitlab
* JUnit5
* Docker
* pgAdmin
* PostgreSQL
* Postman
* Maven
* JaCoCo
* Jackson
* Lombok
***
## Abhängigkeiten
1. Maven
2. Docker engine
***
## How to run
1. Copy the git repo into local directory
```bash
git clone https://git.ai.fh-erfurt.de/prgj2-24/clevercash.git
```
2. start the Docker engine
3. open bash in project folder
4. make sure ports 8080, 5050 and 5432 are available on your maschine
5. start application
```bash
docker compose up --build
```
***
## Links and login data
- [PGAdmin](http://localhost:5050/browser/)
  - email:      Admin@Admin.com
  - password:   admin
- [BackEnd](http://localhost:8080/) Port: 8080
- DB Port:         5432
***
## PG Admin instructions
If there is no database at first login follow these steps
1. right click servers
2. register -> server
3. pick a name of your choice
4. click on connection
5. host name/address = postgres_cc
6. password = admin
7. click save
You can see the db tables under:
- [Name]->Databases->CleverCashDB->Schemas->Tables
***
## Rest Endpunkte
### Swagger
- SWAGGER-Dokumentation der REST API (nach Start der Application erreichbar unter)
    - http://localhost:8080/swagger-ui/index.html
- Anweisungen und Testdaten sind in der Dokumentation zu finden
### Postman
- um mit Postman Endpunkte zu testen muss die Applikation zuerst gestartet werden
- Postman öffnen und über file->Import die postman collection importieren
- Postman collection ist ebenfalls zu finden im docs Ordner des Projektes
- weitere Anweisungen sind in der Dokumentation zu finden
***
## Weitere Befehle
- JavaDoc erstellen lassen
```bash
mvn javadoc:javadoc
```

- Test laufen lassen mit CodeCoverage
```bash
mvn clean test jacoco:report
```
- Report unter: api/target/site/jacoco/index.html
***
## Dokumentation
- Dokumentation und Zeitplanung zu finden unter /docs/doku

***
## Personal
### Mitglieder
- Anton Götz
- Jakob Roch
- Lilou Steffen
- Richard Prax
### Betreuer
- Robert Zeranski
- Christian Chatron
- Xander Van Der Weken
### Institution
- Fachhochschule Erfurt
***