package de.nierbeck.webconsole.plugins.jmx.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JmxPluginServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private transient MBeanServer mBeanServer;

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

	public HashMap<String, ArrayList<ObjectName>> getDomains(
			final MBeanServer mBeanServer, String mbeanDomain)
			throws ReflectionException, InstanceNotFoundException,
			IntrospectionException, MalformedObjectNameException,
			NullPointerException {
		final HashMap<String, ArrayList<ObjectName>> result = new HashMap<String, ArrayList<ObjectName>>();
		ObjectName queryObjectName = null;
		if (mbeanDomain != null && !mbeanDomain.isEmpty())
			queryObjectName = new ObjectName(mbeanDomain + ":*");
		final Set mbeans = mBeanServer.queryMBeans(queryObjectName, null);
		final Iterator iter = mbeans.iterator();
		while (iter.hasNext()) {
			final ObjectInstance mbean = (ObjectInstance) iter.next();
			final ObjectName objectName = mbean.getObjectName();
			final String domain = objectName.getDomain();
			//
			if (result.containsKey(domain)) {
				final ArrayList<ObjectName> list = result.get(domain);
				list.add(objectName);
				result.put(domain, list);
			} else {
				final ArrayList<ObjectName> list = new ArrayList<ObjectName>();
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
			pw.write(',');
			jsonKey(pw, "mbeans");
			pw.write("[");

			final Iterator iter = objectNames.iterator();
			while (iter.hasNext()) {
				final ObjectName objectName = (ObjectName) iter.next();
				final MBeanInfo mBeanInfo = mBeanServer
						.getMBeanInfo(objectName);
				renderJsonDomain(pw, objectName, mBeanInfo);
			}

			pw.write("]}");
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
			final ObjectName objectName, final MBeanInfo mBeanInfo)
			throws IOException {
		pw.write("{");
		jsonKey(pw, "mbean");
		// using toString to make shure that type is set before any other
		// property
		String canonicalName = objectName.toString();
		String[] split = canonicalName.split(":");
		jsonValue(pw, split[1]);

		pw.write(',');
		jsonKey(pw, "attributes");
		pw.write("[");

		final MBeanAttributeInfo[] attrs = mBeanInfo.getAttributes();
		for (int i = 0; i < attrs.length; i++) {
			final MBeanAttributeInfo attr = attrs[i];
			if (!attr.isReadable())
				continue; // skip non readable properties
			jsonValue(pw, attr.getName() + ":writable=" + attr.isWritable());
			if (i < attrs.length) {
				pw.write(',');
			}
		}
		pw.write("]");
		pw.write(',');
		jsonKey(pw, "operations");
		pw.write("[");
		final MBeanOperationInfo[] ops = mBeanInfo.getOperations();
		for (int i = 0; i < ops.length; i++) {
			final MBeanOperationInfo op = ops[i];
			// jsonValue(pw, op.getDescription() + ": " + op.getName() + " - "
			// + op.getReturnType());
			jsonValue(pw, op.getName());
			if (i < ops.length) {
				pw.write(',');
			}

		}
		pw.write("]");
		pw.write("},");
	}

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

		renderJSON(resp.getWriter(), null);
	}

	private void renderJSON(final PrintWriter pw, String mbeanDomain)
			throws IOException {
		if (mBeanServer == null) {
			mBeanServer = ManagementFactory.getPlatformMBeanServer();
		}
		StringBuffer statusLine = new StringBuffer();
		pw.write("{");

		if (mBeanServer != null) {
			try {
				final HashMap<String, ArrayList<ObjectName>> domains = getDomains(
						mBeanServer, mbeanDomain);
				final Set<String> keyset = new TreeSet<String>(domains.keySet());
				statusLine.append(keyset.size());
				statusLine.append(" Domain");
				if (keyset.size() > 1) {
					statusLine.append('s');
				}
				statusLine.append(" received.");

				jsonKey(pw, "status");
				jsonValue(pw, statusLine.toString());

				pw.write(',');
				jsonKey(pw, "data");

				pw.write('[');
				final Iterator<String> iter = keyset.iterator();
				while (iter.hasNext()) {
					final String domain = (String) iter.next();

					final ArrayList<ObjectName> objectNames = domains
							.get(domain);

					Collections.sort(objectNames);

					renderJsonDomain(pw, domain, objectNames);

					if (iter.hasNext())
						pw.write(',');

				}
				pw.write(']');
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		pw.write("}");
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String info = request.getPathInfo();
		if (info.endsWith(".json")) {
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");

			// remove .json
			info = info.substring(0, info.length() - 5);

			// remove label and starting slash
			info = info.substring(getLabel().length() + 1);
			// we only accept direct requests to a bundle if they have a slash
			// after the label
			String mbeanDomain = null;
			if (info.startsWith("/")) {
				mbeanDomain = info.substring(1);
			}

			PrintWriter pw = response.getWriter();

			Map parameterMap = request.getParameterMap();

			if (mbeanDomain == null
					|| (mbeanDomain != null && parameterMap.isEmpty())) {
				this.renderJSON(pw, mbeanDomain); // just loaded initialy or
													// when using the filter
			} else {
				// parameter map is set, use it.
				// first check if it is a attribute or operation
				if (parameterMap.containsKey("attribute")) {
					// handle attribute calls
					this.renderAttribute(pw, mbeanDomain, parameterMap);
				} else if (parameterMap.containsKey("operation")) {
					// Handle operation
				} else {
					// what is this? shouldn't happen.
					return;
				}
			}

			// nothing more to do
			return;
		}

		this.renderContent(request, response);
	}

	private void renderAttribute(PrintWriter pw, String mbeanDomain,
			Map parameterMap) {
		try {
			
			String[] mbeans = (String[]) parameterMap.get("mbean");
			
			String[] attribute = (String[]) parameterMap.get("attribute");
			
			HashMap<String, ArrayList<ObjectName>> domains = getDomains(mBeanServer, mbeanDomain);
			Collection<ArrayList<ObjectName>> values = domains.values();
			boolean found = false;
			
			pw.write("{");
			
			jsonKey(pw, "attributeName");
			jsonValue(pw, attribute[0]);
			
			pw.write(",");
			
			for (ArrayList<ObjectName> arrayList : values) {
				for (ObjectName objectName : arrayList) {
					if (objectName.getCanonicalName().contains(mbeans[0])) {
						//TODO: getAttribute values!
						Object attributeValue = mBeanServer.getAttribute(objectName, attribute[0]);
						found = true;
						jsonKey(pw, "attributeValue");
						jsonValue(pw, attributeValue.toString());
						break;
					}
				}
				if (found)
					break;
			}
			
			pw.write("}");
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	private void jsonValue(final PrintWriter pw, final String v) {
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

	private void jsonValue(PrintWriter pw, boolean b) {
		pw.write(Boolean.toString(b));
	}

	private void jsonKey(final PrintWriter pw, String key) {
		jsonValue(pw, key);
		pw.write(':');
	}
}
