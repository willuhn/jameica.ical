/**********************************************************************
 * $Source: /cvsroot/jameica/jameica.ical/src/de/willuhn/jameica/ical/io/IcalWriter.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/01/20 18:37:06 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.ical.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import de.willuhn.jameica.gui.calendar.Appointment;
import de.willuhn.jameica.gui.calendar.AppointmentProvider;
import de.willuhn.jameica.gui.calendar.AppointmentProviderRegistry;
import de.willuhn.jameica.plugin.AbstractPlugin;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;

/**
 * Wir kapseln hier die Ical-Implementierung, damit wir sie bei Bedarf ersetzen koennen.
 */
public class IcalWriter
{
  static
  {
    // Siehe http://wiki.modularity.net.au/ical4j/index.php?title=DateTime
    // Wir nehmen erstmal einfach die lokale Zeit.
    // Zeitzonen bringen uns alles nur durcheinander.
    System.setProperty("net.fortuna.ical4j.timezone.date.floating","true");
  }
  
  private Calendar cal = null;
  
  public IcalWriter()
  {
    this.cal = new Calendar();
    this.cal.getProperties().add(new ProdId("-//Jameica " + Application.getManifest().getVersion() + "/iCal4j 1.0//DE"));
    this.cal.getProperties().add(Version.VERSION_2_0);
    this.cal.getProperties().add(CalScale.GREGORIAN);
  }
  
  /**
   * Fuegt alle Termine aus dem angegebenen Zeitraum hinzu.
   * @param plugins Liste der Plugins, von denen die Termine exportiert werden sollen.
   * @param from Start-Datum.
   * @param to End-Datum.
   */
  public void addRange(List<AbstractPlugin> plugins, Date from, Date to)
  {
    if (plugins == null || plugins.size() == 0)
    {
      Logger.debug("no plugins given");
      return;
    }
    
    int count = 0;

    Logger.info("adding range " + from + " - " + to);
    
    for (AbstractPlugin plugin:plugins)
    {
      List<AppointmentProvider> providers = AppointmentProviderRegistry.getAppointmentProviders(plugin);
      for (AppointmentProvider p:providers)
      {
        try
        {
          List<Appointment> appointments = p.getAppointments(from,to);
          for (Appointment a:appointments)
          {
            try
            {
              VEvent ve = new VEvent(new net.fortuna.ical4j.model.Date(a.getDate()),a.getName());
              String uid = a.getUid();
              if (uid == null || uid.length() == 0)
                uid = a.getName() + "/" + a.getDate();
              ve.getProperties().add(new Uid(uid));
              
              String desc = a.getDescription();
              if (desc != null)
                ve.getProperties().add(new Description(desc));

              if (a.hasAlarm())
              {
                VAlarm alarm = new VAlarm(new Dur(0,0,-15,0)); // 15 Minuten vorher "-PT15M"
                alarm.getProperties().add(new Description(a.getName()));
                alarm.getProperties().add(Action.DISPLAY);
                ve.getAlarms().add(alarm);
              }
              
              this.cal.getComponents().add(ve);
              count++;
            }
            catch (Exception e)
            {
              Logger.error("error while adding appointment " + a.getDate() + ": " + a.getName(),e);
            }
          }
        }
        catch (Exception e)
        {
          Logger.error("error while fetching appointments from " + p.getName(),e);
        }
      }
    }
    
    Logger.info("added " + count + " events");
  }
  
  /**
   * Schreibt alle Termine in den Stream
   * @param os der Stream, in den die Termine geschrieben werden.
   * Der Stream wird vom IcalWriter NICHT geschlossen.
   * @throws IOException
   */
  public void write(OutputStream os) throws IOException
  {
    if (this.cal.getComponents().size() == 0)
    {
      // Wenn ich das nicht fange, fliegt sonst eine "ValidationException: Calendar must contain at least one component".
      // Eigentlich sollte in dem Fall ein leerer Kalender geschrieben werden. Dazu muesste ich
      // aber im Konstruktor von CalendarOutputter das validating ausschalten. Was wiederrum
      // auch nicht schoen ist. Daher lass ich das erstmal so.
      Logger.debug("calendar empty");
      return;
    }
    try
    {
      CalendarOutputter co = new CalendarOutputter();
      co.output(this.cal,os);
    }
    catch (ValidationException ve)
    {
      throw new IOException(ve);
    }
  }
  
}



/**********************************************************************
 * $Log: IcalWriter.java,v $
 * Revision 1.1  2011/01/20 18:37:06  willuhn
 * @N initial checkin
 *
 * Revision 1.4  2011-01-20 00:55:15  willuhn
 * *** empty log message ***
 *
 * Revision 1.3  2011-01-20 00:40:01  willuhn
 * @N Erste funktionierende Version
 *
 * Revision 1.2  2011-01-19 23:26:14  willuhn
 * *** empty log message ***
 *
 * Revision 1.1  2011-01-19 16:59:45  willuhn
 * @N initial import
 *
 **********************************************************************/