/**********************************************************************
 * $Source: /cvsroot/jameica/jameica.ical/src/de/willuhn/jameica/ical/service/SchedulerService.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/01/20 23:56:18 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.ical.service;

import de.willuhn.datasource.Service;

/**
 * Der Service uebernimmt das regelmaessige Schreiben des Kalenders waehrend
 * der Jameica-Sitzung.
 */
public interface SchedulerService extends Service
{

}



/**********************************************************************
 * $Log: SchedulerService.java,v $
 * Revision 1.1  2011/01/20 23:56:18  willuhn
 * @N Scheduler zum automatischen Speichern alle 30 Minuten
 * @C Support fuer leere Kalender-Datei
 *
 **********************************************************************/