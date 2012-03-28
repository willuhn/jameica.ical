/**********************************************************************
 * $Source: /cvsroot/jameica/jameica.ical/src/de/willuhn/jameica/ical/io/IcalWriter.java,v $
 * $Revision: 1.6 $
 * $Date: 2012/03/28 22:28:12 $
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import de.willuhn.jameica.gui.calendar.Appointment;
import de.willuhn.jameica.gui.calendar.AppointmentProvider;
import de.willuhn.jameica.gui.calendar.AppointmentProviderRegistry;
import de.willuhn.jameica.gui.calendar.ReminderAppointmentProvider;
import de.willuhn.jameica.plugin.Manifest;
import de.willuhn.jameica.plugin.Plugin;
import de.willuhn.jameica.services.BeanService;
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
  
  private Map<String,Integer> uidMap = new HashMap<String,Integer>();
  private Calendar cal = null;
  
  /**
   * ct.
   */
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
  public void addRange(List<Plugin> plugins, Date from, Date to)
  {
    if (plugins == null || plugins.size() == 0)
    {
      Logger.debug("no plugins given");
      return;
    }
    
    int count = 0;

    Logger.info("adding range " + from + " - " + to);
    
    ////////////////////////////////////////////////////////////////////////
    // COMPAT Kompatibilitaet zu Jameica 2.0
    
    // Den Jameica-eigenen Kalender noch hinzufuegen
    try
    {
      BeanService service = Application.getBootLoader().getBootable(BeanService.class);
      AppointmentProvider p = service.get(ReminderAppointmentProvider.class);
      count += this.add(Application.getManifest(),p,from,to);
    }
    catch (Throwable t) {}
    //
    ////////////////////////////////////////////////////////////////////////

    for (Plugin plugin:plugins)
    {
      Manifest mf = plugin.getManifest();
      List<AppointmentProvider> providers = AppointmentProviderRegistry.getAppointmentProviders(plugin);
      for (AppointmentProvider p:providers)
      {
        ////////////////////////////////////////////////////////////////////////
        // COMPAT Kompatibilitaet zu Jameica 2.0
        try
        {
          if (!AppointmentProviderRegistry.isEnabled(p))
            continue;
        }
        catch (NoSuchMethodError e) {}
        //
        ////////////////////////////////////////////////////////////////////////
        
        count += this.add(mf,p,from,to);
      }
    }
    
    Logger.info("added " + count + " events");
  }
  
  /**
   * Fuegt die Termine des Appointment-Providers hinzu.
   * @param mf das Manifest.
   * @param p der Provider.
   * @param from Beginn des Zeitraumes.
   * @param to Ende des Zeitraumes.
   * @return Anzahl der hinzugefuegten Termine.
   */
  private int add(Manifest mf, AppointmentProvider p, Date from, Date to)
  {
    int count = 0;
    
    try
    {
      List<Appointment> appointments = p.getAppointments(from,to);
      for (Appointment a:appointments)
      {
        try
        {
          VEvent ve = new VEvent(new net.fortuna.ical4j.model.Date(a.getDate()),a.getName());
          ve.getProperties().add(new Organizer(mf.getName()));
          
          String uid = a.getUid();
          if (uid == null || uid.length() == 0)
            uid = a.getName() + "/" + a.getDate();
          
          // Checken, ob's die UID schon gibt. Wenn ja, haengen wir noch
          // einen eigenen Zaehler dran, um sicherzustellen, dass die UID
          // nie doppelt auftritt.
          Integer i = uidMap.get(uid);
          if (i == null) i = 0; // neue UID, vormerken
          else           i = new Integer(i+1); // UID bekannt, Wert erhoehen
          
          uidMap.put(uid,i);    // neue ID speichern
          uid = uid + "/" + i;  // an UID anhaengen
          
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
    
    return count;
  }
  
  /**
   * Schreibt alle Termine in den Stream
   * @param os der Stream, in den die Termine geschrieben werden.
   * Der Stream wird vom IcalWriter NICHT geschlossen.
   * @throws IOException
   */
  public void write(OutputStream os) throws IOException
  {
    // Wenn wir keine Termine haben, muessen wir die Validierung ausschalten.
    // Andernfalls fliegt eine "ValidationException: Calendar must contain at least one component".
    // Die Kalender-Datei duerfen wir natuerlich auch nicht loeschen - daher muessen wir
    // eine leere anlegen.
    boolean validate = (this.cal.getComponents().size() > 0);

    try
    {
      CalendarOutputter co = new CalendarOutputter(validate);
      co.output(this.cal,os);
      Logger.info("calendar written");
    }
    catch (ValidationException ve)
    {
      throw new IOException(ve);
    }
  }
  
}



/**********************************************************************
 * $Log: IcalWriter.java,v $
 * Revision 1.6  2012/03/28 22:28:12  willuhn
 * @N Einfuehrung eines neuen Interfaces "Plugin", welches von "AbstractPlugin" implementiert wird. Es dient dazu, kuenftig auch Jameica-Plugins zu unterstuetzen, die selbst gar keinen eigenen Java-Code mitbringen sondern nur ein Manifest ("plugin.xml") und z.Bsp. Jars oder JS-Dateien. Plugin-Autoren muessen lediglich darauf achten, dass die Jameica-Funktionen, die bisher ein Object vom Typ "AbstractPlugin" zuruecklieferten, jetzt eines vom Typ "Plugin" liefern.
 * @C "getClassloader()" verschoben von "plugin.getRessources().getClassloader()" zu "manifest.getClassloader()" - der Zugriffsweg ist kuerzer. Die alte Variante existiert weiterhin, ist jedoch als deprecated markiert.
 *
 * Revision 1.5  2011-10-07 11:16:51  willuhn
 * @N Jameica-interne Reminder ebenfalls exportieren
 *
 * Revision 1.4  2011-10-06 10:49:47  willuhn
 * @N Nur noch Provider exportieren, die aktiviert sind - mit Abwaertskompatibilitaet
 *
 * Revision 1.3  2011-01-25 10:17:42  willuhn
 * @B http://www.willuhn.de/blog/index.php?/archives/544-jameica.ical-Termine-aus-Hibiscus-exportieren.html#c1249
 *
 * Revision 1.2  2011-01-20 23:56:18  willuhn
 * @N Scheduler zum automatischen Speichern alle 30 Minuten
 * @C Support fuer leere Kalender-Datei
 *
 * Revision 1.1  2011-01-20 18:37:06  willuhn
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