/**********************************************************************
 * $Source: /cvsroot/jameica/jameica.ical/src/de/willuhn/jameica/ical/messaging/IcalMessageConsumer.java,v $
 * $Revision: 1.2 $
 * $Date: 2011/01/21 10:21:06 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.ical.messaging;

import de.willuhn.jameica.ical.Plugin;
import de.willuhn.jameica.ical.service.IcalService;
import de.willuhn.jameica.messaging.Message;
import de.willuhn.jameica.messaging.MessageConsumer;
import de.willuhn.jameica.messaging.SystemMessage;
import de.willuhn.jameica.system.Application;

/**
 * Wird ueber den Start und Stop von Jameica benachrichtigt und schreibt daraufhin die Ical-Datei.
 */
public class IcalMessageConsumer implements MessageConsumer
{

  /**
   * @see de.willuhn.jameica.messaging.MessageConsumer#autoRegister()
   */
  public boolean autoRegister()
  {
    return true;
  }

  /**
   * @see de.willuhn.jameica.messaging.MessageConsumer#getExpectedMessageTypes()
   */
  public Class[] getExpectedMessageTypes()
  {
    return new Class[]{SystemMessage.class};
  }

  /**
   * @see de.willuhn.jameica.messaging.MessageConsumer#handleMessage(de.willuhn.jameica.messaging.Message)
   */
  public void handleMessage(Message message) throws Exception
  {
    SystemMessage msg = (SystemMessage) message;
    int code = msg.getStatusCode();
    
    // Nur beim Shutdown
    if (code != SystemMessage.SYSTEM_SHUTDOWN)
      return;
    
    IcalService service = (IcalService) Application.getServiceFactory().lookup(Plugin.class,"ical");
    service.run();
  }

}



/**********************************************************************
 * $Log: IcalMessageConsumer.java,v $
 * Revision 1.2  2011/01/21 10:21:06  willuhn
 * @C Nur beim Shutdown (und ueber Scheduler) speichern. Beim Start verzoegert das unnoetig den Aufbau der Grafik. Und es macht wenig Sinn, weil zu dem Zeitpunkt ohnehin noch keine Aenderungen vorliegen
 *
 * Revision 1.1  2011-01-20 18:37:05  willuhn
 * @N initial checkin
 *
 * Revision 1.1  2011-01-20 00:40:02  willuhn
 * @N Erste funktionierende Version
 *
 **********************************************************************/