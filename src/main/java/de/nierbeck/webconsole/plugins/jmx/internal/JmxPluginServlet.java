package de.nierbeck.webconsole.plugins.jmx.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
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
import javax.servlet.http.*;

import org.apache.felix.webconsole.internal.core.BundlesServlet;

public class JmxPluginServlet extends HttpServlet {
	private MBeanServer mBeanServer;

	private final String TEMPLATE;
	private static final String ACTION_CLEAR = "clear";

	private static final String PARAMETER_ACTION = "action";

	private static final String PARAMETER_ROOT = "root";

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
			jsonKey(pw, "text");
			jsonValue(pw, domain);
			pw.write(',');
			jsonKey(pw, "expanded");
			jsonValue(pw, false);
			pw.write(',');
			jsonKey(pw, "classes");
			jsonValue(pw, "folder");
			pw.write(',');
			jsonKey(pw, "id");
			jsonValue(pw, domain);
			pw.write(',');
			jsonKey(pw, "children");
			pw.write("[");

			final Iterator iter = objectNames.iterator();
			while (iter.hasNext()) {
				final ObjectName objectName = (ObjectName) iter.next();
				final MBeanInfo mBeanInfo = mBeanServer
						.getMBeanInfo(objectName);
				renderJsonDomain(pw, objectName, mBeanInfo);
				if (iter.hasNext())
					pw.write(',');
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
		// using toString to make shure that type is set before any other
		// property
		String canonicalName = objectName.toString();
		String[] split = canonicalName.split(":");

		String[] subPaths = split[1].split(",");

		for (int i = 0; i < subPaths.length; i++) {

			String[] typeAndName = subPaths[i].split("=");

			if (i > 0) {
				jsonKey(pw, "children");
				pw.write("[{");
			}

			jsonKey(pw, "text");
			jsonValue(pw, typeAndName[1]);
			pw.write(',');

			jsonKey(pw, "classes");
			jsonValue(pw, typeAndName[0]);

			if (i < subPaths.length - 1)
				pw.write(',');

			if (i < subPaths.length && i > 0) {
				pw.write("}]");
			}

		}

		/*
		 * pw.write(','); jsonKey(pw, "children"); pw.write("[{"); final
		 * MBeanAttributeInfo[] attrs = mBeanInfo.getAttributes(); if
		 * (attrs.length > 0) { jsonKey(pw, "text"); jsonValue(pw,
		 * "attributes"); pw.write(","); jsonKey(pw, "classes"); jsonValue(pw,
		 * "attribute"); pw.write(","); jsonKey(pw,"children"); pw.write("[");
		 * for (int i = 0; i < attrs.length; i++) { final MBeanAttributeInfo
		 * attr = attrs[i]; if (!attr.isReadable()) continue; //skip non
		 * readable properties pw.write("{"); jsonKey(pw, "text"); jsonValue(pw,
		 * attr.getName()); pw.write(','); if (attr.isWritable()) { jsonKey(pw,
		 * "classes"); jsonValue(pw, "readonly"); } else { jsonKey(pw,
		 * "classes"); jsonValue(pw, "file"); } pw.write('}'); if (i <
		 * attrs.length-1) { pw.write(','); } } pw.write("]}"); pw.write(',');
		 * pw.write("{"); } jsonKey(pw, "text"); jsonValue(pw, "operations");
		 * pw.write(","); jsonKey(pw, "classes"); jsonValue(pw, "attribute");
		 * final MBeanOperationInfo[] ops = mBeanInfo.getOperations(); if
		 * (ops.length > 0) { pw.write(","); jsonKey(pw,"children");
		 * pw.write("["); for (int i = 0; i < ops.length; i++) { final
		 * MBeanOperationInfo op = ops[i]; // jsonValue(pw, op.getDescription()
		 * + ": " + op.getName() + " - " // + op.getReturnType());
		 * pw.write("{"); jsonKey(pw, "text"); jsonValue(pw, op.getName());
		 * pw.write(','); jsonKey(pw, "classes"); jsonValue(pw, "operation");
		 * pw.write("}"); if (i < ops.length-1) { pw.write(','); } }
		 * pw.write("]"); } pw.write("}]");
		 */
		pw.write("}");
	}

	private final String readTemplateFile(
			final Class<? extends JmxPluginServlet> clazz,
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
		final String root = req.getParameter(PARAMETER_ROOT);
		final HttpSession session = req.getSession(true);
		// for now we only have the clear action
		if (ACTION_CLEAR.equals(action)) {
		}
		// we always send back the json data
		resp.setContentType("application/json");
		resp.setCharacterEncoding("utf-8");

		try {
			if ("source".equals(root)) {
				renderDomain(resp.getWriter(), getDomains());
			} else if (root != null) {
				renderDomainValues(resp.getWriter(), getDomains(), root);
			} else {
				renderJSON(resp.getWriter(), null);
			}
		} catch (Exception e) {
			log("Can't render json for domains", e);
			throw new ServletException(e);
		}
	}

	private void renderDomainValues(PrintWriter pw,
			HashMap<String, ArrayList<ObjectName>> domains, String mbean) {
		pw.write('[');
		ArrayList<ObjectName> objectNames = domains.get(mbean);

		HashMap<String, List<Entry<String, String>>> sortedMap = new HashMap<String, List<Entry<String, String>>>();

		for (ObjectName objectName : objectNames) {
			
			objectName.getCanonicalName();
			
			String canonicalKeyPropertyListString = objectName
					.getCanonicalKeyPropertyListString();
			String[] split = canonicalKeyPropertyListString.split(",");

			for (String keyProps : split) {
				String[] keyProp = keyProps.split("=");
				Entry<String, String> entry = new SimpleEntry(keyProp[0],
						keyProp[1]);
				List<Entry<String, String>> list = sortedMap.get(keyProp[1]);
				if (list == null) {
					list = new ArrayList<Entry<String, String>>();
					sortedMap.put(keyProp[1], list);
				}
				list.add(entry);
			}
		}

		TreeSet<String> keys = new TreeSet<String>(sortedMap.keySet());

		for (String key : keys) {

			String classes = "";
			String id = mbean;

			List<Entry<String, String>> list = sortedMap.get(key);

			for (Entry<String, String> entry : list) {
				String descriptor = entry.getKey();
				String value = entry.getValue();
				if (value.equals(key))
					classes = descriptor;
			}
//
//			id += ":";
//			id += mbeanSelector.substring(1, mbeanSelector.length() - 1);

			pw.write("{");
			jsonKey(pw, "text");
			jsonValue(pw, key);
			pw.write(',');
			jsonKey(pw, "expanded");
			jsonValue(pw, false);
			pw.write(',');
			jsonKey(pw, "classes");
			jsonValue(pw, classes);
			pw.write(',');
			jsonKey(pw, "id");
			jsonValue(pw, id);
			pw.write(',');
			jsonKey(pw, "hasChildren");
			jsonValue(pw, true);
			pw.write('}');
			pw.write(',');
		}

		pw.write(']');
	}

	private void renderDomain(PrintWriter pw,
			HashMap<String, ArrayList<ObjectName>> domains) {
		final Set<String> keyset = new TreeSet<String>(domains.keySet());
		pw.write('[');
		final Iterator<String> iter = keyset.iterator();
		while (iter.hasNext()) {
			final String domain = iter.next();

			pw.write("{");
			jsonKey(pw, "text");
			jsonValue(pw, domain);
			pw.write(',');
			jsonKey(pw, "expanded");
			jsonValue(pw, false);
			pw.write(',');
			jsonKey(pw, "classes");
			jsonValue(pw, "folder");
			pw.write(',');
			jsonKey(pw, "id");
			jsonValue(pw, domain);
			pw.write(',');
			jsonKey(pw, "hasChildren");
			jsonValue(pw, true);
			pw.write('}');

			if (iter.hasNext())
				pw.write(',');

		}
		pw.write(']');
	}

	private HashMap<String, ArrayList<ObjectName>> getDomains()
			throws InstanceNotFoundException, IntrospectionException,
			MalformedObjectNameException, ReflectionException,
			NullPointerException {
		if (mBeanServer == null) {
			mBeanServer = ManagementFactory.getPlatformMBeanServer();
		}

		HashMap<String, ArrayList<ObjectName>> domains = null;

		if (mBeanServer != null)
			domains = getDomains(mBeanServer, null);
		else
			domains = new HashMap<String, ArrayList<ObjectName>>();

		return domains;

	}

	private void renderJSON(final PrintWriter pw, String mbean)
			throws IOException, InstanceNotFoundException,
			IntrospectionException, ReflectionException,
			MalformedObjectNameException, NullPointerException {

		final HashMap<String, ArrayList<ObjectName>> domains = getDomains(
				mBeanServer, mbean);
		final Set<String> keyset = new TreeSet<String>(domains.keySet());
		/*
		 * statusLine.append(keyset.size()); statusLine.append(" Domain"); if
		 * (keyset.size() > 1) { statusLine.append('s'); }
		 * statusLine.append(" received.");
		 * 
		 * jsonKey(pw, "status"); jsonValue(pw, statusLine.toString());
		 * 
		 * pw.write(','); jsonKey(pw, "data");
		 */
		pw.write('[');
		final Iterator<String> iter = keyset.iterator();
		while (iter.hasNext()) {
			final String domain = iter.next();

			final ArrayList objectNames = domains.get(domain);

			Collections.sort(objectNames);

			renderJsonDomain(pw, domain, objectNames);

			if (iter.hasNext())
				pw.write(',');

		}
		pw.write(']');

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
			// we only accept direct requests to a bundle if they have a slash after the label
            String mbeanDomain = null;
            if (info.startsWith("/") )
            {
                mbeanDomain = info.substring(1);
            }

            PrintWriter pw = response.getWriter();
            
            Map parameterMap = request.getParameterMap();
            
            if (mbeanDomain == null || (mbeanDomain != null &&  parameterMap.isEmpty())) {
            	this.renderJSON(pw, mbeanDomain); //just loaded initialy or when using the filter
            } else {
            	//parameter map is set, use it.
            	//first check if it is a attribute or operation
            	if (parameterMap.containsKey("attribute")) {
            		//handle attribute calls
            	} else if (parameterMap.containsKey("operation")) {
            		//Handle operation
            		this.renderAttribute(pw, mbeanDomain, parameterMap);
            	} else {
            		//what is this? shouldn't happen.
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
