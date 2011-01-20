/**********************************************************************
 * $Source: /cvsroot/jameica/jameica.ical/src/de/willuhn/jameica/ical/service/impl/SchedulerServiceImpl.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/01/20 23:56:18 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.ical.service.impl;

import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

import de.willuhn.jameica.ical.Plugin;
import de.willuhn.jameica.ical.service.IcalService;
import de.willuhn.jameica.ical.service.SchedulerService;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;

/**
 * Implementierung des Scheduler-Service.
 */
public class SchedulerServiceImpl implements SchedulerService
{
  private Timer timer = null;

  /**
   * @see de.willuhn.datasource.Service#getName()
   */
  public String getName() throws RemoteException
  {
    return "ical-scheduler";
  }

  /**
   * @see de.willuhn.datasource.Service#isStartable()
   */
  public boolean isStartable() throws RemoteException
  {
    return !this.isStarted();
  }

  /**
   * @see de.willuhn.datasource.Service#isStarted()
   */
  public boolean isStarted() throws RemoteException
  {
    return this.timer != null;
  }

  /**
   * @see de.willuhn.datasource.Service#start()
   */
  public void start() throws RemoteException
  {
    if (this.isStarted())
    {
      Logger.warn("service allready started, skipping request");
      return;
    }
    
    this.timer = new Timer(this.getName(),true);
    this.timer.schedule(new MyTask(),60 * 1000L, 30 * 60 * 1000L); // alle halbe Stunde, wir starten in 1 Minute
  }

  /**
   * @see de.willuhn.datasource.Service#stop(boolean)
   */
  public void stop(boolean arg0) throws RemoteException
  {
    if (!this.isStarted())
    {
      Logger.warn("service not started, skipping request");
      return;
    }
    
    try
    {
      this.timer.cancel();
    }
    finally
    {
      this.timer = null;
    }
  }
  
  /**
   * Hier machen wir die eigentliche Arbeit.
   */
  private class MyTask extends TimerTask
  {
    /**
     * @see java.util.TimerTask#run()
     */
    public void run()
    {
      try
      {
        IcalService service = (IcalService) Application.getServiceFactory().lookup(Plugin.class,"ical");
        service.run();
      }
      catch (Exception e)
      {
        Logger.error("error while saving ical file, stopping scheduler",e);
        try
        {
          stop(true);
        }
        catch (Exception e2)
        {
          Logger.error("unable to stop scheduler",e2);
        }
      }
    }
  }

}



/**********************************************************************
 * $Log: SchedulerServiceImpl.java,v $
 * Revision 1.1  2011/01/20 23:56:18  willuhn
 * @N Scheduler zum automatischen Speichern alle 30 Minuten
 * @C Support fuer leere Kalender-Datei
 *
 **********************************************************************/