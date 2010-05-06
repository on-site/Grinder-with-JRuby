package net.grinder.common.processidentity;

import java.io.Serializable;


/**
 * The identity of a process.
 *
 * <p>Implementations should define equality so that instances are equal if
 * and only they represent the same process.</p>
 *
 * @author Philip Aston
 * @version $Revision: 3995 $
 */
public interface ProcessIdentity extends Serializable {

  /**
   * Return the process name.
   *
   * @return The process name.
   */
  String getName();

  /**
   * Return the process number. This is not necessarily set when
   * the process is started.
   *
   * @return The number.
   */
  int getNumber();
}
