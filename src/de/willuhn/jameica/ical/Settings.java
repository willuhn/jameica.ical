/**********************************************************************
 * $Source: /cvsroot/jameica/jameica.ical/src/de/willuhn/jameica/ical/Settings.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/01/20 18:37:06 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn.webdesign
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.ical;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.willuhn.jameica.gui.calendar.AppointmentProvider;
import de.willuhn.jameica.gui.calendar.AppointmentProviderRegistry;
import de.willuhn.jameica.plugin.AbstractPlugin;
import de.willuhn.jameica.plugin.PluginLoader;
import de.willuhn.jameica.plugin.PluginResources;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;

/**
 * Container fuer die Einstellungen.
 */
public class Settings
{
  /**
   * Die Einstellungen des Plugins.
   */
  private final static de.willuhn.jameica.system.Settings SETTINGS = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getSettings();
  
  /**
   * Liefert den zu exportierenden Zeitraum in Monaten vor und nach dem aktuellen Datum.
   * Ein Wert von "3" bedeutet also "3 Monate vor und 3 Monate nach dem aktuellen Datum.
   * @return der zu exportierende Zeitraum.
   */
  public static int getRange()
  {
    return SETTINGS.getInt("ical.range.months",3);
  }
  
  /**
   * Speichert den zu exportierenden Zeitraum in Monaten vor und nach dem aktuellen Datum.
   * Ein Wert von "3" bedeutet also "3 Monate vor und 3 Monate nach dem aktuellen Datum.
   * @param m der zu exportierende Zeitraum.
   */
  public static void setRange(int m)
  {
    if (m <= 0)
    {
      SETTINGS.setAttribute("ical.range.months",(String) null); // reset
      return;
    }
    
    SETTINGS.setAttribute("ical.range.months",m);
  }
  
  /**
   * Liefert die Datei, in die exportiert werden soll.
   * @return die Datei, in die exportiert werden soll.
   */
  public static File getFile()
  {
    PluginResources res = Application.getPluginLoader().getPlugin(Plugin.class).getResources();
    return new File(SETTINGS.getString("ical.file",res.getWorkPath() + File.separator + "jameica.ics"));
  }
  
  /**
   * Speichert die Datei, in die exportiert werden soll.
   * @param file die Datei, in die exportiert werden soll.
   */
  public static void setFile(File file)
  {
    SETTINGS.setAttribute("ical.file", file != null ? file.toString() : (String) null);
  }
  
  /**
   * Liefert eine Liste der Plugins, zu denen Termine exportiert werden sollen.
   * @return Liste der Plugins, zu denen Termine exportiert werden sollen.
   */
  public static List<AbstractPlugin> getPlugins()
  {
    PluginLoader loader = Application.getPluginLoader();
    
    List<AbstractPlugin> list = new ArrayList<AbstractPlugin>();
    String[] names = SETTINGS.getList("ical.plugins",new String[0]);

    int loaded = 0;
    for (String s:names)
    {
      try
      {
        list.add(loader.getPlugin(s));
        loaded++;
      }
      catch (Exception e)
      {
        Logger.error("unable to load plugin " + s + ", skipping",e);
      }
    }
    
    // Wir checken noch, ob wir vielleicht ungueltige Plugins in der Liste
    // hatten. Die werfen wir bei der Gelegenheit gleich raus.
    if (list.size() != loaded)
      setPlugins(list);
    
    return list;
  }
  
  /**
   * Speichert die Liste der Plugins, zu denen Termine exportiert werden sollen.
   * @param plugins Liste der Plugins, zu denen Termine exportiert werden sollen.
   */
  public static void setPlugins(List<AbstractPlugin> plugins)
  {
    if (plugins == null || plugins.size() == 0)
    {
      SETTINGS.setAttribute("ical.plugins",(String[]) null);
      return;
    }
    
    List<String> classes = new ArrayList<String>();
    for (AbstractPlugin p:plugins)
    {
      // Checken, ob das Plugin ueberhaupt AppointmentProvider hat
      List<AppointmentProvider> providers = AppointmentProviderRegistry.getAppointmentProviders(p);
      if (providers == null || providers.size() == 0)
      {
        Logger.warn("plugin " + p.getManifest().getName() + " contains no appointmentproviders, skipping");
        continue;
      }
      classes.add(p.getClass().getName());
    }
    
    SETTINGS.setAttribute("ical.plugins",classes.toArray(new String[classes.size()]));
  }
}


/*********************************************************************
 * $Log: Settings.java,v $
 * Revision 1.1  2011/01/20 18:37:06  willuhn
 * @N initial checkin
 *
 **********************************************************************/