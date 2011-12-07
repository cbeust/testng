package org.testng.remote.strprotocol;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public interface IMessageSender {

  void connect() throws IOException;

  /**
   * Initialize the receiver.
   * Method will try to initialize receiver for the indefinite amount of time.
   */
  void initReceiver() throws SocketTimeoutException;

  /**
   * @param soTimeout
   * @throws SocketException This exception will be thrown if a connection
   * to the remote TestNG instance could not be established after soTimeout
   * milliseconds.
   */
  void initReceiver(int soTimeout) throws SocketTimeoutException;

  void sendMessage(IMessage message) throws Exception;

  /**
   * Will return null or throw EOFException when the connection has been severed.
   */
  IMessage receiveMessage() throws Exception;

  void shutDown();

  // These two methods should probably be in a separate class since they should all be
  // the same for implementers of this interface.
  void sendAck();

  void sendStop();
}
