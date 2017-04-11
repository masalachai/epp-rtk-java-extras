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
package net.masalachai.epprtk.rtk;

import net.masalachai.epprtk.rtk.transport.EPPTransportTCPTLS;
import static com.tucows.oxrs.epprtk.rtk.RTKBase.DEBUG_LEVEL_THREE;
import com.tucows.oxrs.epprtk.rtk.transport.EPPTransportBase;
import com.tucows.oxrs.epprtk.rtk.transport.EPPTransportException;
import com.tucows.oxrs.epprtk.rtk.xml.EPPGreeting;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.openrtk.idl.epprtk.epp_Exception;
import org.openrtk.idl.epprtk.epp_XMLException;

/**
 * EPP Client -- extends the EPPClient class in EPP RTK with
 * a few extra functions.
 *
 * @author masalachai
 *
 * @see com.tucows.oxrs.epprtk.rtk.EPPClient
**/
public class EPPClient extends com.tucows.oxrs.epprtk.rtk.EPPClient {

	protected String local_address_;
	protected int local_port_;
	protected EPPTransportBase transport_;

	protected String ssl_props_location_;
	
	/**
	 * Constructor with local address and port to bind to.
	 *
	 * @param epp_host_name The EPP Hostname (eg. "host.domain.tld")
	 * @param epp_host_port The EPP port
	 * @param epp_client_id The EPP client id
	 * @param epp_password The password associated with the client id
	 * @param local_address The local IP address to bind to
	 * @param local_port The local port to bind to
	 * @param ssl_props_location The directory path to the SSL properties file
	 */
	public EPPClient(String epp_host_name,
			int epp_host_port,
			String epp_client_id,
			String epp_password,
			String local_address,
			int local_port,
			String ssl_props_location) {
		super(epp_host_name, epp_host_port,
				epp_client_id, epp_password);

		local_address_ = local_address;
		local_port_ = local_port;
		ssl_props_location_ = ssl_props_location;
	}

	/**
	 * Connects to the EPP Server using previously set hostname and port. It is
	 * recommended to use connectAndGetGreeting() to connect and retrieve the
	 * EPPGreeting in one single call.
	 *
	 * @throws epp_Exception
	 * @throws UnknownHostException if the EPP host cannot be found
	 * @throws SocketException
	 * @throws IOException
	 * @throws EPPTransportException if there are problem initializing the
	 * transport class
	 * @see #connectAndGetGreeting()
	 */
	@Override
	public void connect()
			throws epp_Exception,
			UnknownHostException,
			SocketException,
			IOException,
			EPPTransportException {

		transport_ = new EPPTransportTCPTLS(getEPPHostName(), getEPPHostPort(),
				getEPPTimeout(), local_address_, local_port_, ssl_props_location_);

		// ENABLE BC PROVIDER for PKCS12 keystore
		try {
			java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		} catch (Exception xcp) {
			throw new IOException(xcp.getMessage());
		}

		setTransport(transport_);

		super.connect();
	}

	/**
	 * Issues a hello request to the EPP Server and returns the EPP XML
	 * response. This forces the server to respond with an epp_Greeting, which
	 * is otherwise only sent on initial connections to the EPP Server.
	 *
	 * @return The XML greeting from the EPP Server
	 * @throws org.openrtk.idl.epprtk.epp_Exception if the server greeting is
	 * not present
	 * @throws org.openrtk.idl.epprtk.epp_XMLException if the server's greeting
	 * is not parsable
	 */
	public String helloXML() throws epp_Exception, epp_XMLException {
		String method_name = "hello()";
		debug(DEBUG_LEVEL_THREE, method_name, "Entered");

		EPPGreeting hello_from_server;
		String xml_from_server;

		hello_from_server = new EPPGreeting();
		xml_from_server = processXML(hello_from_server.toXML());

		debug(DEBUG_LEVEL_THREE, method_name, "Leaving");

		return xml_from_server;
	}
}
