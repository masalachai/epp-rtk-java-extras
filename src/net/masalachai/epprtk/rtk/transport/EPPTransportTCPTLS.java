/*
**
** EPP RTK Java
** Copyright (C) 2001-2002, Tucows, Inc.
** Copyright (C) 2003, Liberty RMS
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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Properties;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * EPP Transport TCP TLS -- extends the EPPTransportTCPTLS class in EPP RTK
 * with a few extra functions.
 *
 * @author masalachai
 *
 * @see com.tucows.oxrs.epprtk.rtk.transport.EPPTransportTCPTLS
**/
public class EPPTransportTCPTLS extends
		com.tucows.oxrs.epprtk.rtk.transport.EPPTransportTCPTLS {

	protected SSLContext ctx_ = null;
	protected KeyStore ks_ = null;
	protected KeyManagerFactory kmf_ = null;
	protected SecureRandom rnd_ = null;
	
	protected String ssl_props_location_;
	
	protected String local_address_;
	protected int local_port_ = -1;

	/**
	 * Default Construtor
	 */
	public EPPTransportTCPTLS() {
		super();
	}

	/**
	 * Constructor with Hostname, Host port and timeout value
	 *
	 * @param host_name The server Hostname
	 * @param host_port The server Host port
	 * @param timeout The int socket timeout value, in milliseconds
	 */
	public EPPTransportTCPTLS(String host_name,	int host_port, int timeout) {
		super(host_name, host_port, timeout);
	}
	
	/**
	 * Constructor with Hostname, Host port, timeout value, 
	 * local address and local port
	 *
	 * @param host_name The server Hostname
	 * @param host_port The server Host port
	 * @param timeout The int socket timeout value, in milliseconds
	 * @param local_address The local address to bind to
	 * @param local_port The local port to bind to
	 */
	public EPPTransportTCPTLS(String host_name,	int host_port, int timeout,
			String local_address, int local_port) {
		super(host_name, host_port, timeout);
		local_address_ = local_address;
		local_port_ = local_port;
	}
	
	/**
	 * Constructor with Hostname, Host port, timeout value, 
	 * local address, local port, and directory with SSL keys
	 *
	 * @param host_name The server Hostname
	 * @param host_port The server Host port
	 * @param timeout The int socket timeout value, in milliseconds
	 * @param local_address The local address to bind to
	 * @param local_port The local port to bind to
	 * @param ssl_props_location Path to the SSL directory containing config and keys
	 */
	public EPPTransportTCPTLS(String host_name,	int host_port, int timeout,
			String local_address, int local_port, String ssl_props_location) {
		super(host_name, host_port, timeout);
		local_address_ = local_address;
		local_port_ = local_port;
		ssl_props_location_ = ssl_props_location;
	}

	/**
	 * Connects to the Server using previously set Hostname and port. If
	 * connection has been already established, the operation will be ignored.
	 * The method also sets the SO timeout.
	 *
	 * @throws SocketException
	 * @throws IOException
	 * @throws UnknownHostException
	 * @throws EPPTransportException
	 */
	@Override
	public void connect() throws SocketException, IOException, 
			UnknownHostException, EPPTransportException {
		
		// Call the parent class connect if no local address or port specified
		if(local_address_ == null || local_port_ < 1) {
			super.connect();
			return;
		}
		
		String method_name = "connect()";

		debug(DEBUG_LEVEL_THREE, method_name, "Entered");

		if (!preset_) {
			// Initialize to null the socket to the server
			socket_to_server_ = null;

			debug(DEBUG_LEVEL_TWO, method_name, "Using SSL/TLS");

			String ssl_props_location = ssl_props_location_;
			if(ssl_props_location == null) {
				Properties system_props = System.getProperties();
				ssl_props_location = (String) system_props.getProperty("ssl.props.location");
			}
			if (ssl_props_location == null 
					|| ssl_props_location.length() == 0) {
				throw new IOException("No ssl props location specified");
			}
			Properties ssl_props = new Properties();
			ssl_props.load(new FileInputStream(ssl_props_location + "/ssl.properties"));

			SSLSocketFactory ssl_factory = null;

			try {
				char[] passphrase1 = ((String) ssl_props.get("ssl.keystore.passphrase")).toCharArray();
				char[] passphrase2 = ((String) ssl_props.get("ssl.signedcert.passphrase")).toCharArray();
				if (ctx_ == null) {
					ctx_ = SSLContext.getInstance(((String) ssl_props.get("ssl.protocol")));
				}
				if (ks_ == null) {
					if (ssl_props.get("ssl.keystore.provider") == null) {
						ks_ = KeyStore.getInstance((String) ssl_props.get("ssl.keystore.format"));
					} else {
						ks_ = KeyStore.getInstance((String) ssl_props.get("ssl.keystore.format"), (String) ssl_props.get("ssl.keystore.provider"));
					}
					ks_.load(new FileInputStream(ssl_props_location + "/"
							+ ((String) ssl_props.get("ssl.keystore.file"))), passphrase1);
				}
				if (kmf_ == null) {
					kmf_ = KeyManagerFactory.getInstance(((String) ssl_props.get("ssl.keymanagerfactory.format")));
					kmf_.init(ks_, passphrase2);
				}

				// SSL Performance improvement from wessorh
				try {
					byte seed[] = new byte[1024];
					FileInputStream is = new FileInputStream("/dev/urandom");
					is.read(seed);
					is.close();

					rnd_ = java.security.SecureRandom.getInstance("SHA1PRNG");
					rnd_.setSeed(seed);
					debug(DEBUG_LEVEL_TWO, method_name, "SecureRandom seed set.");

				} catch (Exception xcp) {
					debug(DEBUG_LEVEL_TWO, method_name, "Error initializing SecureRandom [" + xcp.getMessage() + "], using default initialization.");
					rnd_ = null;
				}

				ctx_.init(kmf_.getKeyManagers(), null, rnd_);
				ssl_factory = ctx_.getSocketFactory();
			} catch (Exception xcp) {
				throw new IOException(xcp.getMessage());
			}

			if (local_address_ != null) {
				// connect and bind to specified address and port
				InetAddress loc_addr = InetAddress.getByName(local_address_);
				socket_to_server_ = (Socket) ssl_factory.createSocket(epp_host_name_, epp_host_port_, loc_addr, local_port_);
			} else {
				socket_to_server_ = (Socket) ssl_factory.createSocket(epp_host_name_, epp_host_port_);
			}
			
			// Force the handshake to happen now so we can check for a good connection
			SSLSession la_session = ((SSLSocket) socket_to_server_).getSession();
			if (socket_to_server_ == null
					|| la_session.getProtocol().equals("NONE")) {
				throw new EPPTransportException("Failed to establish secure connection to server.  Perhaps a bad certificate? -- use -Djavax.net.debug=all to see errors.");
			}
		}

		socket_to_server_.setSoTimeout(epp_timeout_);

		reader_from_server_ = new BufferedInputStream(socket_to_server_.getInputStream());
		writer_to_server_ = new BufferedOutputStream(socket_to_server_.getOutputStream());

		debug(DEBUG_LEVEL_TWO, method_name, "Connected to [" + socket_to_server_.getInetAddress() + ":" + socket_to_server_.getPort() + "]");

		debug(DEBUG_LEVEL_THREE, method_name, "Leaving");
	}
}
