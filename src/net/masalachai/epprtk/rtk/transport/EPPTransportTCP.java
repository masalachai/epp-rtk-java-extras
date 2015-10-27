/*
**
** EPP RTK Java
** Copyright (C) 2001-2002, Tucows, Inc.
** Copyright (C) 2003, Liberty RMS
**
**
** This library is free software; you can redistribute it and/or
** modify it under the terms of the GNU Lesser General Public
** License as published by the Free Software Foundation; either
** version 2.1 of the License, or (at your option) any later version.
**
** This library is distributed in the hope that it will be useful,
** but WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
** Lesser General Public License for more details.
**
** You should have received a copy of the GNU Lesser General Public
** License along with this library; if not, write to the Free Software
** Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
**
*/

package net.masalachai.epprtk.rtk.transport;

import static com.tucows.oxrs.epprtk.rtk.RTKBase.DEBUG_LEVEL_THREE;
import static com.tucows.oxrs.epprtk.rtk.RTKBase.DEBUG_LEVEL_TWO;
import com.tucows.oxrs.epprtk.rtk.transport.EPPTransportException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * EPP Transport TCP -- extends the EPPTransportTCP class in EPP RTK
 * with a few extra functions.
 *
 * @author masalachai
 *
 * @see com.tucows.oxrs.epprtk.rtk.transport.EPPTransportTCP
**/
public class EPPTransportTCP
		extends com.tucows.oxrs.epprtk.rtk.transport.EPPTransportTCP {

	protected String local_address_;
	protected int local_port_ = -1;

	/**
	 * Constructor with hostname, host port, timeout value, local address and
	 * local port.
	 * If the timeout value is zero, the default timeout value from
	 * EPPTransportBase is used.
	 *
	 * @param host_name The server Hostname
	 * @param host_port The server Host port
	 * @param timeout The int socket timeout value, in milliseconds
	 * @param local_address The local address to bind to
	 * @param local_port The local port to bind to
	 */
	public EPPTransportTCP(String host_name, int host_port, int timeout,
			String local_address, int local_port) {
		super(host_name, host_port, timeout);
		local_address_ = local_address;
		local_port_ = local_port;
	}

	/**
	 * Connects to the Server using previously set Hostname and port. If the
	 * socket was provided externally, the connection operation is skipped, but
	 * the input and output buffers are still extracted. The method also sets
	 * the SO timeout of the socket regardless of its origins.
	 * 
	 * @throws SocketException
	 * @throws IOException
	 * @throws UnknownHostException
	 * @throws EPPTransportException
	 */
	@Override
	public void connect() throws SocketException, IOException, 
			UnknownHostException, EPPTransportException {
		String method_name = "connect()";

		debug(DEBUG_LEVEL_THREE, method_name, "Entered");
		
		// call parent class connect if no local address set
		if(local_address_ == null || local_port_ < 1) {
			super.connect();
			return;
		}

		if (!preset_) {
			// Initialize to null the socket to the server
			socket_to_server_ = null;
			
			// bind to the supplied local address and port
			InetAddress loc_addr = InetAddress.getByName(local_address_);
			socket_to_server_ = new Socket(epp_host_name_, epp_host_port_, loc_addr, local_port_);
		}

		socket_to_server_.setSoTimeout(epp_timeout_);

		reader_from_server_ = new BufferedInputStream(socket_to_server_.getInputStream());
		writer_to_server_ = new BufferedOutputStream(socket_to_server_.getOutputStream());

		debug(DEBUG_LEVEL_TWO, method_name, "Connected to [" + socket_to_server_.getInetAddress() + ":" + socket_to_server_.getPort() + "]");

		debug(DEBUG_LEVEL_THREE, method_name, "Leaving");
	}
}
