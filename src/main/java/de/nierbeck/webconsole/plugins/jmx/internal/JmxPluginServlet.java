package de.nierbeck.webconsole.plugins.jmx.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.ServletException;
import javax.servlet.http.*;

public class JmxPluginServlet extends HttpServlet {
	private MBeanServer mBeanServer;

	private final String TEMPLATE;
	private static final String ACTION_CLEAR = "clear";

	private static final String PARAMETER_ACTION = "action";

	public JmxPluginServlet() {
		TEMPLATE = readTemplateFile(getClass(), "/res/jmx-template.html");
	}

	public String getLabel() {
		return JmxPluginConstants.LABEL;
	}

	public String getTitle() {
		return JmxPluginConstants.NAME;
	}

	/*
	 * protected void getContent(final HttpServletRequest req, final
	 * HttpServletResponse response) throws ServletException, IOException {
	 * final PrintWriter pw = response.getWriter(); if (mBeanServer == null) {
	 * mBeanServer = ManagementFactory.getPlatformMBeanServer(); } final String
	 * appRoot = req.getContextPath() + req.getServletPath(); if (mBeanServer !=
	 * null) { String pathInfo = req.getPathInfo(); pathInfo = pathInfo
	 * .substring(JmxPluginConstants.LABEL.length() + 1); try { if (pathInfo ==
	 * null || pathInfo.equals("") || pathInfo.equals("/")) { final HashMap
	 * domains = getDomains(mBeanServer); final Set keyset = domains.keySet();
	 * final Iterator iter = keyset.iterator(); while (iter.hasNext()) { final
	 * String domain = (String) iter.next(); pw.println("<a href=\"" + appRoot +
	 * "/" + JmxPluginConstants.LABEL + "/" + domain + "\"><h1>" + domain +
	 * "</h1></a>"); final ArrayList objectNames = (ArrayList) domains
	 * .get(domain); render(objectNames, pw); } } else { final HashMap domains =
	 * getDomains(mBeanServer); final String domain = pathInfo.substring(1);
	 * pw.println("<a href=\"" + appRoot + "/" + JmxPluginConstants.LABEL + "/"
	 * + domain + "\"><h1>" + domain + "</h1></a>"); final ArrayList objectNames
	 * = (ArrayList) domains .get(domain); render(objectNames, pw); } } catch
	 * (final Exception e) { e.printStackTrace(); } } }
	 */

	public HashMap getDomains(final MBeanServer mBeanServer)
			throws ReflectionException, InstanceNotFoundException,
			IntrospectionException {
		final HashMap result = new HashMap();
		final Set mbeans = mBeanServer.queryMBeans(null, null);
		final Iterator iter = mbeans.iterator();
		while (iter.hasNext()) {
			final ObjectInstance mbean = (ObjectInstance) iter.next();
			final ObjectName objectName = mbean.getObjectName();
			final String domain = objectName.getDomain();
			//
			if (result.containsKey(domain)) {
				final ArrayList list = (ArrayList) result.get(domain);
				list.add(objectName);
				result.put(domain, list);
			} else {
				final ArrayList list = new ArrayList();
				list.add(objectName);
				result.put(domain, list);
			}
		}
		return result;
	}

	private void renderJsonDomain(final PrintWriter pw, final String domain,
			final ArrayList objectNames) throws InstanceNotFoundException,
			IntrospectionException, ReflectionException, IOException {
		if (objectNames != null) {

			pw.write("{");
			jsonKey(pw, "domain");
			jsonValue(pw, domain);
			

			final Iterator iter = objectNames.iterator();
			while (iter.hasNext()) {
				final ObjectName objectName = (ObjectName) iter.next();
				final MBeanInfo mBeanInfo = mBeanServer
						.getMBeanInfo(objectName);
				renderJsonDomain(pw, objectName, mBeanInfo);
			}
			
			pw.write("}");
		}
	}

	private String getPath(final ObjectName name) {
		return name.getDomain();
	}

	private String getName(final ObjectName name) {
		final String result = "";
		return result;
	}

	private void renderJsonDomain(final PrintWriter pw,
			final ObjectName objectName, final MBeanInfo mBeanInfo) throws IOException {
		pw.write(',');
		jsonKey(pw, "attributes");
		pw.write("[");
		
		final MBeanAttributeInfo[] attrs = mBeanInfo.getAttributes();
		for (int i = 0; i < attrs.length; i++) {
			final MBeanAttributeInfo attr = attrs[i];
			jsonValue(pw, attr.getDescription() + ": " + attr.getName() + " - "
					+ attr.getType());
			if (i < attrs.length) {
				pw.write(',');
			}
		}
		pw.write("]");
		pw.write(',');
		jsonKey(pw, "opperations");
		pw.write("[");
		final MBeanOperationInfo[] ops = mBeanInfo.getOperations();
		for (int i = 0; i < ops.length; i++) {
			final MBeanOperationInfo op = ops[i];
			jsonValue(pw, op.getDescription() + ": " + op.getName() + " - "
					+ op.getReturnType());
			if (i < ops.length) {
				pw.write(',');
			}
		}
		pw.write("]");
	}

	// public void activateBundle(final BundleContext bundleContext) {
	// super.activate(bundleContext);
	// mBeanServer = ManagementFactory.getPlatformMBeanServer();
	// }
	//
	// public void deactivate() {
	// super.deactivate();
	// mBeanServer = null;
	// }

	// TODO: After this line the new stuff is added

	private final String readTemplateFile(final Class clazz,
			final String templateFile) {
		InputStream templateStream = getClass().getResourceAsStream(
				templateFile);
		if (templateStream != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] data = new byte[1024];
			try {
				int len = 0;
				while ((len = templateStream.read(data)) > 0) {
					baos.write(data, 0, len);
				}
				return baos.toString("UTF-8");
			} catch (IOException e) {
				// don't use new Exception(message, cause) because cause is 1.4+
				throw new RuntimeException("readTemplateFile: Error loading "
						+ templateFile + ": " + e);
			} finally {
				try {
					templateStream.close();
				} catch (IOException e) {
					/* ignore */
				}

			}
		}

		// template file does not exist, return an empty string
		log("readTemplateFile: File '" + templateFile
				+ "' not found through class " + clazz);
		return "";
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		final String action = req.getParameter(PARAMETER_ACTION);
		// for now we only have the clear action
		if (ACTION_CLEAR.equals(action)) {
		}
		// we always send back the json data
		resp.setContentType("application/json");
		resp.setCharacterEncoding("utf-8");

		renderJSON(resp.getWriter());
	}

	private void renderJSON(final PrintWriter pw) throws IOException {

		/*
		 * List events = this.collector.getEvents();
		 * 
		 * StringBuffer statusLine = new StringBuffer(); statusLine.append(
		 * events.size() ); statusLine.append( " Event"); if ( events.size() !=
		 * 1 ) { statusLine.append('s'); } statusLine.append( " received" ); if
		 * ( !events.isEmpty() ) { statusLine.append( " since " ); Date d = new
		 * Date(); d.setTime( ( ( EventInfo ) events.get( 0 ) ).received );
		 * statusLine.append( d ); } statusLine.append( ". (Event admin: " ); if
		 * ( !this.eventAdminAvailable ) { statusLine.append("un"); }
		 * statusLine.append("available; Config admin: "); if (
		 * !this.configAdminAvailable ) { statusLine.append("un"); }
		 * statusLine.append("available)");
		 * 
		 * // Compute scale: startTime is 0, lastTimestamp is 100% final long
		 * startTime = this.collector.getStartTime(); final long endTime =
		 * (events.size() == 0 ? startTime :
		 * ((EventInfo)events.get(events.size() - 1)).received); final float
		 * scale = (endTime == startTime ? 100.0f : 100.0f / (endTime -
		 * startTime));
		 * 
		 * pw.write("{");
		 * 
		 * jsonKey( pw, "status" ); jsonValue( pw, statusLine.toString() );
		 * pw.write(','); jsonKey( pw, "data" );
		 * 
		 * pw.write('[');
		 * 
		 * // display list in reverse order for ( int index = events.size() - 1;
		 * index >= 0; index-- ) { eventJson( pw, ( EventInfo ) events.get(
		 * index ), index, startTime, scale ); if ( index > 0 ) { pw.write(',');
		 * } }
		 * 
		 * pw.write(']');
		 * 
		 * pw.write("}");
		 */

		if (mBeanServer == null) {
			mBeanServer = ManagementFactory.getPlatformMBeanServer();
		}
		StringBuffer statusLine = new StringBuffer();
		pw.write("{");

		if (mBeanServer != null) {
			try {
				final HashMap domains = getDomains(mBeanServer);
				final Set keyset = domains.keySet();
				statusLine.append(keyset.size());
				statusLine.append(" MBean");
				if (keyset.size() > 1) {
					statusLine.append('s');
				}
				statusLine.append(" received.");

				jsonKey(pw, "status");
				jsonValue(pw, statusLine.toString());

				pw.write(',');
				jsonKey(pw, "data");

				pw.write('[');
				final Iterator iter = keyset.iterator();
				while (iter.hasNext()) {
					final String domain = (String) iter.next();

					final ArrayList objectNames = (ArrayList) domains
							.get(domain);

					renderJsonDomain(pw, domain, objectNames);

					if (iter.hasNext())
						pw.write(',');

					/*
					 * pw.println("<a href=\"" + appRoot + "/" +
					 * JmxPluginConstants.LABEL + "/" + domain + "\"><h1>" +
					 * domain + "</h1></a>"); final ArrayList objectNames =
					 * (ArrayList) domains .get(domain); render(objectNames,
					 * pw);
					 */
				}
				pw.write(']');
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		pw.write("}");
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		final String info = request.getPathInfo();
		if (info.endsWith(".json")) {
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");

			PrintWriter pw = response.getWriter();
			this.renderJSON(pw);

			// nothing more to do
			return;
		}

		this.renderContent(request, response);
	}

	protected void renderContent(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		final PrintWriter pw = response.getWriter();
		pw.print(TEMPLATE);
	}

	public URL getResource(String path) {
		if (path.startsWith("/jmx/res/ui/")) {
			return this.getClass().getResource(path.substring(4));
		}
		return null;
	}

	private void jsonValue(final PrintWriter pw, final String v)
			throws IOException {
		if (v == null || v.length() == 0) {
			pw.write("\"\"");
			return;
		}

		pw.write('"');
		char previousChar = 0;
		char c;

		for (int i = 0; i < v.length(); i += 1) {
			c = v.charAt(i);
			switch (c) {
			case '\\':
			case '"':
				pw.write('\\');
				pw.write(c);
				break;
			case '/':
				if (previousChar == '<') {
					pw.write('\\');
				}
				pw.write(c);
				break;
			case '\b':
				pw.write("\\b");
				break;
			case '\t':
				pw.write("\\t");
				break;
			case '\n':
				pw.write("\\n");
				break;
			case '\f':
				pw.write("\\f");
				break;
			case '\r':
				pw.write("\\r");
				break;
			default:
				if (c < ' ') {
					final String hexValue = "000" + Integer.toHexString(c);
					pw.write("\\u");
					pw.write(hexValue.substring(hexValue.length() - 4));
				} else {
					pw.write(c);
				}
			}
			previousChar = c;
		}
		pw.write('"');
	}

	private void jsonValue(final PrintWriter pw, final long l) {
		pw.write(Long.toString(l));
	}

	private void jsonKey(final PrintWriter pw, String key) throws IOException {
		jsonValue(pw, key);
		pw.write(':');
	}

}
