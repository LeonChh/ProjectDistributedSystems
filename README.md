Run eerst de file ServerSide\ServerMain.java, dan kan je meerdere clients opstarten door ClientSide\ClientMain.java te runnen.

Zolang je de server niet afzet kan je de clients zoveel opstarten, afsluiten of heropstarten als je wilt. 

Wanneer je de server afsluit moet je de json files verwijderen uit de folder ClientSide\jsonFiles en de json file ServerSide\subscribers.json leeg maken of verwijderen. (Beter dat je ze leegmaakt, want je krijgt dan geen initiele foutmelding. Zal wel correct opstarten in beide gevallen.)

Testen van de recoveries:
  * Symmetrische keys aanpassen in de json files van de clients
  * Voor de index kan je de next index corrupten in de terminal onder optie 1. Er zal om een index gevraagd worden en deze kan je aflezen in de terminals van de clients.
  * Voor de tag kan je de 2de optie gebruiken in de terminal. Hier zal je ook een index moeten specificieren, die je terug kan afkijken in de client terminals.

