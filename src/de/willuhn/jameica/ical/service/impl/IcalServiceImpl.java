/**********************************************************************
 * $Source: /cvsroot/jameica/jameica.ical/src/de/willuhn/jameica/ical/service/impl/IcalServiceImpl.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/01/20 18:37:06 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.ical.service.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;

import de.willuhn.jameica.ical.Settings;
import de.willuhn.jameica.ical.io.IcalWriter;
import de.willuhn.jameica.ical.service.IcalService;
import de.willuhn.jameica.util.DateUtil;
import de.willuhn.logging.Logger;

/**
 * Implementierung des Services zur Erzeugung der Ical-Datei.
 */
public class IcalServiceImpl implements IcalService
{
  private boolean started = false;
  
  /**
   * @see de.willuhn.datasource.Service#getName()
   */
  public String getName() throws RemoteException
  {
    return "ical-service";
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
    return this.started;
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
    
    this.started = true;
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
    this.started = false;
  }

  /**
   * @see de.willuhn.jameica.ical.service.IcalService#run()
   */
  public void run() throws RemoteException
  {
    if (!this.isStarted())
    {
      Logger.warn("service not started, skipping run");
      return;
    }

    File file = Settings.getFile();
    File dir = file.getParentFile();
    if (!dir.exists() && !dir.mkdirs())
      throw new RemoteException("unable to create dir " + dir);

    Logger.info("writing calendar file " + file);
    
    int range = Settings.getRange();
    Date now  = new Date();
    
    Calendar cal = Calendar.getInstance();
    
    cal.setTime(now);
    cal.add(Calendar.MONTH,-range);
    Date start = DateUtil.startOfDay(cal.getTime());
    
    cal.setTime(now);
    cal.add(Calendar.MONTH,range);
    Date end = DateUtil.endOfDay(cal.getTime());
    
    IcalWriter writer = new IcalWriter();
    writer.addRange(Settings.getPlugins(),start,end);
    
    
    OutputStream os = null;
    
    try
    {
      os = new BufferedOutputStream(new FileOutputStream(file));
      writer.write(os);
    }
    catch (Exception e)
    {
      throw new RemoteException("unable to write calendar file " + file,e);
    }
    finally
    {
      if (os != null)
      {
        try
        {
          os.close();
        }
        catch (Exception e)
        {
          Logger.error("unable to close stream to " + file,e);
        }
      }
    }
  }
}



/**********************************************************************
 * $Log: IcalServiceImpl.java,v $
 * Revision 1.1  2011/01/20 18:37:06  willuhn
 * @N initial checkin
 *
 * Revision 1.2  2011-01-20 00:40:01  willuhn
 * @N Erste funktionierende Version
 *
 * Revision 1.1  2011-01-19 16:59:45  willuhn
 * @N initial import
 *
 **********************************************************************/