/**********************************************************************
 * $Source: /cvsroot/jameica/jameica.ical/src/de/willuhn/jameica/ical/io/IcalWriter.java,v $
 * $Revision: 1.8 $
 * $Date: 2012/03/29 20:44:58 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.ical.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Trigger;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

import org.apache.commons.lang.ObjectUtils;

import de.willuhn.jameica.gui.calendar.AbstractAppointment;
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
  
  private Calendar cal = null;
  
  private Map<String,Integer> uidMap = new HashMap<String,Integer>();
  private HashMap<String, VEvent> lookup = new HashMap<String, VEvent>();

  /**
   * ct.
   */
  public IcalWriter()
  {
    this(null);
  }
  
  /**
   * ct.
   * @param is Liest alle Termine aus einer existierenden Calendar-Date ein.
   * Beim Schreiben neuer Termine mittels {@link IcalWriter#addRange(List, Date, Date)}
   * werden die hier eingelesenene Termine dann wiederverwendet, wenn
   * sie hinreichend mit den neuen uebereinstimmen. Damit koennen kleinere
   * Aenderungen, die der User zwischenzeitlich in seinem Kalender-Programm
   * vorgenommen hat (wie etwa bei einem Alarm auf "Snooze" zu klicken)
   * beibehalten.
   * Der Stream wird vom IcalWriter NICHT geschlossen.
   */
  public IcalWriter(InputStream is)
  {
    this.cal = new Calendar();
    this.cal.getProperties().add(new ProdId("-//Jameica " + Application.getManifest().getVersion() + "/iCal4j 1.0//DE"));
    this.cal.getProperties().add(Version.VERSION_2_0);
    this.cal.getProperties().add(CalScale.GREGORIAN);

    if (is != null)
    {
      Logger.info("try to parse existing file");
      try
      {
        CalendarBuilder builder = new CalendarBuilder();
        Calendar old = builder.build(is);
        
        // Jetzt bauen wir noch eine HashMap aus den UIDs, damit wir existierende
        // Eintraege schnell wiederfinden koennen
        Iterator i = old.getComponents().iterator();
        while (i.hasNext())
        {
          Object comp = i.next();
          if (!(comp instanceof VEvent))
            continue;
          
          VEvent event = (VEvent) comp;
          Uid uidobj = event.getUid();
          String uid = uidobj.getValue();
          this.lookup.put(uid, event);
        }
      }
      catch (Exception e)
      {
        Logger.error("unable to parse old calendar, will overwrite existing events",e);
      }
    }
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
    
    // Den Jameica-eigenen Kalender noch hinzufuegen
    BeanService service = Application.getBootLoader().getBootable(BeanService.class);
    AppointmentProvider ap = service.get(ReminderAppointmentProvider.class);
    count += this.add(Application.getManifest(),ap,from,to);

    for (Plugin plugin:plugins)
    {
      Manifest mf = plugin.getManifest();
      List<AppointmentProvider> providers = AppointmentProviderRegistry.getAppointmentProviders(plugin);
      for (AppointmentProvider p:providers)
      {
        if (!AppointmentProviderRegistry.isEnabled(p))
          continue;
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
    int added = 0;
    int reused = 0;
    
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
          // nie doppelt auftritt. Das kann durchaus oefters passieren.
          // Z.Bsp. bei sich wiederholenden Auftraegen.
          Integer i = this.uidMap.get(uid);
          if (i == null) i = 0; // neue UID, vormerken
          else           i = new Integer(i+1); // UID bekannt, Wert erhoehen
          
          this.uidMap.put(uid,i); // neue ID speichern
          uid = uid + "/" + i;    // an UID anhaengen
          
          ve.getProperties().add(new Uid(uid));
          
          String desc = a.getDescription();
          if (desc != null)
            ve.getProperties().add(new Description(desc));

          if (a.hasAlarm())
          {
            VAlarm alarm = new VAlarm(this.createDur(a));
            alarm.getProperties().add(new Description(a.getName()));
            alarm.getProperties().add(Action.DISPLAY);
            ve.getAlarms().add(alarm);
          }
          
          // mal schauen, ob es fuer den Eintrag was in der alten Datei gab
          VEvent old = this.lookup.get(uid);
          if (old != null)
          {
            
            // Jepp. Vergleichen
            if (this.equals(ve,old))
            {
              // alle wichtigen Eigenschaften sind gleich. Wiederverwenden!

              // Die Description ueberschreiben wir generell
              Description oldDesc = old.getDescription();
              if (oldDesc != null)
                old.getProperties().remove(oldDesc);
              if (desc != null)
                old.getProperties().add(ve.getDescription());
                  
              // Alle anderen Eigenschaften bleiben unberuehrt.
              this.cal.getComponents().add(old);
              reused++;
              Logger.debug("reusing event uid: " + uid);
              continue;
            }
            else
            {
              Logger.debug("overwriting event uid: " + uid);
            }
        	}

          // kein altes Event gefunden, also neues nehmen
          Logger.debug("creating event uid: " + uid);
          this.cal.getComponents().add(ve);
          added++;
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
    
    return added + reused;
  }
  
  /**
   * Erzeugt ein passendes Dur-Objekt mit dem Reminder-Offset.
   * @param a das Appointment fuer das das Reminder-Offset ermittelt werden soll.
   * @return passendes Dur-Objekt.
   */
  private Dur createDur(Appointment a)
  {
    int seconds = Appointment.ALARMTIME_SECONDS;
    if (a instanceof AbstractAppointment)
      seconds = ((AbstractAppointment)a).getAlarmTime();
    
    // Umrechnen in Tage, Stunden, Minuten, Sekunden
    int d = (int) (TimeUnit.SECONDS.toDays(seconds));
    int h = (int) (TimeUnit.SECONDS.toHours(seconds) - TimeUnit.DAYS.toHours(d));
    int m = (int) (TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.DAYS.toMinutes(d) - TimeUnit.HOURS.toMinutes(h));
    int s = (int) (TimeUnit.SECONDS.toSeconds(seconds) - TimeUnit.DAYS.toSeconds(d) - TimeUnit.HOURS.toSeconds(h) - TimeUnit.MINUTES.toSeconds(m));

    // Minus-Angaben heisst: Reminder *vor* dem Termin. Also zurueck in der Zeit
    return new Dur(-d,-h,-m,-s);
  }
  
  /**
   * Vergleicht 2 Calendar-Events und liefert true, wenn sie hinreichend uebereinstimmen,
   * dass wir den Termin wiederverwenden koennen.
   * @param ve1 Event 1.
   * @param ve2 Event 2.
   * @return true, wenn sie hinreichend uebereinstimmen.
   */
  private boolean equals(VEvent ve1, VEvent ve2)
  {
    try
    {
      // Pruefen, ob die wichtigen Attribute uebereinstimmen
      
      Logger.debug("comparing " + ve1.getUid().getValue() + " to " + ve2.getUid().getValue());

      // Gleiches Datum?
      if (!ObjectUtils.equals(ve1.getStartDate(),ve2.getStartDate()))
      {
        Logger.debug("startdate differs");
        return false;
      }
      
      // Gleicher Name?
      if (!ObjectUtils.equals(ve1.getName(),ve2.getName()))
      {
        Logger.debug("name differs");
        return false;
      }

      // Gleicher Organizer?
      if (!ObjectUtils.equals(ve1.getOrganizer(),ve2.getOrganizer()))
      {
        Logger.debug("organizer differs");
        return false;
      }
      
      // Alarme vergleichen 
      ComponentList alarms1 = ve1.getAlarms();
      ComponentList alarms2 = ve2.getAlarms();

      // Anzahl unterschiedlich
      if (alarms1.size() != alarms2.size())
      {
        Logger.debug("count of alarms differs");
        return false;
      }

      // wir haben keine Alarme. Dann passt das schon.
      if (alarms1.size() == 0)
      {
        Logger.debug("0 alarms, equals");
        return true;
      }

      // Wir vergleichen nur die Trigger. Die anderen Eigenschaften des Alarms
      // muessen wir ignorieren, weil sie vom iCal-Client modifiziert werden koennten.
      // wir vergleichen also nur den ersten
      Trigger t1 = ((VAlarm) alarms1.get(0)).getTrigger();
      Trigger t2 = ((VAlarm) alarms2.get(0)).getTrigger();
      
      if (t2 == null)
      {
        Logger.debug("no trigger, differs");
        return true;
      }

      // wir koennen hier nicht direkt die trigger vergleichen,
      // weil der Mozilla Calendar Trigger so schreibt:
      // "TRIGGER;VALUE=DURATION:-PT15M"
      // ical4j aber per Default so: "TRIGGER;-PT15M" 
      if (!ObjectUtils.equals(t1.getDuration(),t2.getDuration()))
      {
        Logger.debug("trigger differs");
        return false;
      }
      
      // hinreichend gleich
      Logger.debug("equals");
      return true;
    }
    catch (Exception e)
    {
      Logger.error("error while comparing, classify as different",e);
      return false;
    }
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
