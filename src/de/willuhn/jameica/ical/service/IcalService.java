/**********************************************************************
 * $Source: /cvsroot/jameica/jameica.ical/src/de/willuhn/jameica/ical/service/IcalService.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/01/20 18:37:05 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.ical.service;

import java.rmi.RemoteException;

import de.willuhn.datasource.Service;

/**
 * Interface des Services, der die Ical-Datei erzeugt.
 */
public interface IcalService extends Service
{
  /**
   * Erzeugt die Ical-Datei.
   * @throws RemoteException
   */
  public void run() throws RemoteException;
}



/**********************************************************************
 * $Log: IcalService.java,v $
 * Revision 1.1  2011/01/20 18:37:05  willuhn
 * @N initial checkin
 *
 * Revision 1.1  2011-01-19 16:59:46  willuhn
 * @N initial import
 *
 **********************************************************************/