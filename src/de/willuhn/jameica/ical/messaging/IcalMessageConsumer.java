/**********************************************************************
 *
 * Copyright (c) by Olaf Willuhn
 * All rights reserved
 * 
 * GPLv2
 *
 **********************************************************************/

package de.willuhn.jameica.ical.messaging;

import de.willuhn.jameica.ical.Plugin;
import de.willuhn.jameica.ical.service.IcalService;
import de.willuhn.jameica.messaging.Message;
import de.willuhn.jameica.messaging.MessageConsumer;
import de.willuhn.jameica.messaging.SettingsChangedMessage;
import de.willuhn.jameica.messaging.SettingsRestoredMessage;
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
    return new Class[]{SystemMessage.class, SettingsChangedMessage.class, SettingsRestoredMessage.class};
  }

  /**
   * @see de.willuhn.jameica.messaging.MessageConsumer#handleMessage(de.willuhn.jameica.messaging.Message)
   */
  public void handleMessage(Message message) throws Exception
  {
    // Wenn es eine System-Message ist, dann bei bei Shutdoen
    if (message instanceof SystemMessage)
    {
      SystemMessage msg = (SystemMessage) message;
      int code = msg.getStatusCode();
      
      // Nur beim Shutdown
      if (code != SystemMessage.SYSTEM_SHUTDOWN)
        return;
    }
    
    IcalService service = (IcalService) Application.getServiceFactory().lookup(Plugin.class,"ical");
    service.run();
  }

}
