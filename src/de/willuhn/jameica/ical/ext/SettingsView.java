/**********************************************************************
 * $Source: /cvsroot/jameica/jameica.ical/src/de/willuhn/jameica/ical/ext/SettingsView.java,v $
 * $Revision: 1.2 $
 * $Date: 2011/01/21 11:17:10 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn.webdesign
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.ical.ext;

import java.io.File;
import java.util.List;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.TableItem;

import de.willuhn.jameica.gui.calendar.AppointmentProvider;
import de.willuhn.jameica.gui.calendar.AppointmentProviderRegistry;
import de.willuhn.jameica.gui.extension.Extendable;
import de.willuhn.jameica.gui.extension.Extension;
import de.willuhn.jameica.gui.formatter.TableFormatter;
import de.willuhn.jameica.gui.input.FileInput;
import de.willuhn.jameica.gui.input.SpinnerInput;
import de.willuhn.jameica.gui.internal.views.Settings;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.jameica.gui.util.TabGroup;
import de.willuhn.jameica.ical.Plugin;
import de.willuhn.jameica.messaging.Message;
import de.willuhn.jameica.messaging.MessageConsumer;
import de.willuhn.jameica.messaging.SettingsChangedMessage;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.plugin.AbstractPlugin;
import de.willuhn.jameica.plugin.Manifest;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.I18N;

/**
 * Erweitert die View mit dem System-Einstellungen um die Kalender-Optionen.
 */
public class SettingsView implements Extension
{
  private final static I18N i18n = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getI18N();

  private MessageConsumer mc = null;
  
  private FileInput file     = null;
  private SpinnerInput range = null;
  private TablePart plugins  = null;
  
  /**
   * @see de.willuhn.jameica.gui.extension.Extension#extend(de.willuhn.jameica.gui.extension.Extendable)
   */
  public void extend(Extendable extendable)
  {
    if (extendable == null || !(extendable instanceof Settings))
      return;

    this.mc = new MessageConsumer() {

      /**
       * @see de.willuhn.jameica.messaging.MessageConsumer#handleMessage(de.willuhn.jameica.messaging.Message)
       */
      public void handleMessage(Message message) throws Exception
      {
        handleStore();
      }

      /**
       * @see de.willuhn.jameica.messaging.MessageConsumer#getExpectedMessageTypes()
       */
      public Class[] getExpectedMessageTypes()
      {
        return new Class[]{SettingsChangedMessage.class};
      }

      /**
       * @see de.willuhn.jameica.messaging.MessageConsumer#autoRegister()
       */
      public boolean autoRegister()
      {
        return false;
      }
    };
    Application.getMessagingFactory().registerMessageConsumer(this.mc);

    
    Settings settings = (Settings) extendable;
    
    try
    {
      TabGroup tab = new TabGroup(settings.getTabFolder(),i18n.tr("Kalender"));
      
      // Da wir keine echte View sind, haben wir auch kein unbind zum Aufraeumen.
      // Damit wir unsere GUI-Elemente aber trotzdem disposen koennen, registrieren
      // wir einen Dispose-Listener an der Tabgroup
      tab.getComposite().addDisposeListener(new DisposeListener() {
      
        public void widgetDisposed(DisposeEvent e)
        {
          file    = null;
          range   = null;
          plugins = null;
          Application.getMessagingFactory().unRegisterMessageConsumer(mc);
        }
      
      });

      tab.addHeadline(i18n.tr("Plugins, deren Termine exportiert werden sollen"));
      tab.addPart(getPlugins());
      
      tab.addHeadline(i18n.tr("Einstellungen"));
      tab.addInput(this.getFile());
      tab.addInput(this.getRange());
    }
    catch (Exception e)
    {
      Logger.error("unable to extend settings",e);
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Fehler beim Anzeigen der Kalender-Einstellungen"), StatusBarMessage.TYPE_ERROR));
    }
  }
  
  /**
   * Liefert das Eingabefeld fuer die Dateiauswahl.
   * @return Eingabefeld.
   */
  private FileInput getFile()
  {
    if (this.file != null)
      return this.file;
    
    this.file = new FileInput(de.willuhn.jameica.ical.Settings.getFile().toString(),true,new String[]{"*.ics"});
    this.file.disableClientControl();
    this.file.setName(i18n.tr("Pfad der Kalender-Datei"));
    return this.file;
  }
  
  /**
   * Liefert ein Eingabefeld fuer den zu exportierenden Zeitraum.
   * @return Eingabefeld.
   */
  private SpinnerInput getRange()
  {
    if (this.range != null)
      return this.range;
    
    this.range = new SpinnerInput(1,24,de.willuhn.jameica.ical.Settings.getRange());
    this.range.setName(i18n.tr("Zu exportierender Zeitraum (+/-)"));
    this.range.setComment(i18n.tr("Monat(e)"));
    return this.range;
  }
  
  /**
   * Liefert eine Liste der Plugins, zu denen Termine exportiert werden sollen.
   * @return
   */
  private TablePart getPlugins()
  {
    if (this.plugins != null)
      return this.plugins;
    
    plugins = new TablePart(null);
    plugins.addColumn(i18n.tr("Name"),"manifest");
    plugins.addColumn(i18n.tr("Beschreibung"),"manifest");
    plugins.setCheckable(true);
    plugins.setRememberColWidths(true);
    plugins.setRememberOrder(true);
    plugins.setSummary(false);

    // Daten hinzufuegen
    List<AbstractPlugin> list = Application.getPluginLoader().getInstalledPlugins();
    for (AbstractPlugin p:list)
    {
      // Checken, ob es ueberhaupt Termine unterstuetzt
      List<AppointmentProvider> providers = AppointmentProviderRegistry.getAppointmentProviders(p);
      if (providers == null || providers.size() == 0)
        continue;

      try
      {
        plugins.addItem(p);
      }
      catch (Exception e)
      {
        Logger.error("unable to add plugin " + p.getClass().getName(),e);
      }
    }
    
    // Aktivierte Plugins markieren
    plugins.setFormatter(new TableFormatter() {
      
      public void format(TableItem item)
      {
        if (item == null)
          return;
        
        Object data = item.getData();
        if (data == null || !(data instanceof AbstractPlugin))
          return;
        
        try
        {
          AbstractPlugin p = (AbstractPlugin) data;
          item.setChecked(de.willuhn.jameica.ical.Settings.getPlugins().contains(p));
          
          Manifest mf = p.getManifest();
          item.setText(0,mf.getName());
          item.setText(1,mf.getDescription());
        }
        catch (Exception e)
        {
          Logger.error("unable to check plugin",e);
        }
      }
    });
    
    return plugins;
  }
  
  /**
   * Speichert die Einstellungen.
   */
  private void handleStore()
  {
    String s = (String) this.getFile().getValue();
    File f = s != null && s.length() > 0 ? new File(s) : null;
    de.willuhn.jameica.ical.Settings.setFile(f);

    de.willuhn.jameica.ical.Settings.setRange(((Integer)getRange().getValue()).intValue());

    try
    {
      de.willuhn.jameica.ical.Settings.setPlugins(getPlugins().getItems(true));
    }
    catch (Exception e)
    {
      Logger.error("unable to apply plugin list",e);
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Fehler beim Übernehmen der Plugins: {0}",e.getMessage()),StatusBarMessage.TYPE_ERROR));
    }
  }
}


/*********************************************************************
 * $Log: SettingsView.java,v $
 * Revision 1.2  2011/01/21 11:17:10  willuhn
 * *** empty log message ***
 *
 * Revision 1.1  2011-01-20 18:37:06  willuhn
 * @N initial checkin
 *
 *********************************************************************/