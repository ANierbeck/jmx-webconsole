package de.nierbeck.webconsole.plugins.jmx.internal;

import com.google.gson.stream.JsonWriter;

import javax.management.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.*;

public class JmxPluginServlet extends HttpServlet{
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private transient MBeanServer mBeanServer;

    private final String TEMPLATE;
    private static final String ACTION_CLEAR = "clear";

    private static final String PARAMETER_ACTION = "action";

    public JmxPluginServlet(){
        TEMPLATE = readTemplateFile(getClass(), "/res/jmx-template.html");
    }

    public String getLabel(){
        return JmxPluginConstants.LABEL;
    }

    public String getTitle(){
        return JmxPluginConstants.NAME;
    }

    public HashMap<String, ArrayList<ObjectName>> getDomains(
            final MBeanServer mBeanServer, String mbeanDomain, String mBean)
            throws MalformedObjectNameException,
            NullPointerException{
        final HashMap<String, ArrayList<ObjectName>> result = new HashMap<String, ArrayList<ObjectName>>();
        ObjectName queryObjectName = null;
        if(mbeanDomain != null && !mbeanDomain.isEmpty())
            queryObjectName = new ObjectName(mbeanDomain
                    + (mBean != null ? ":" + mBean : ":*"));
        final Set mbeans = mBeanServer.queryMBeans(queryObjectName, null);
        final Iterator iter = mbeans.iterator();
        while(iter.hasNext()){
            final ObjectInstance objectInstance = (ObjectInstance) iter.next();
            final ObjectName objectName = objectInstance.getObjectName();
            final String domain = objectName.getDomain();
            //
            if(result.containsKey(domain)){
                final ArrayList<ObjectName> list = result.get(domain);
                list.add(objectName);
                result.put(domain, list);
            } else{
                final ArrayList<ObjectName> list = new ArrayList<ObjectName>();
                list.add(objectName);
                result.put(domain, list);
            }
        }
        return result;
    }

    private void renderJsonDomain(final JsonWriter pw, final String domain,
                                  final ArrayList objectNames, boolean renderDetails)
            throws InstanceNotFoundException, IntrospectionException,
            ReflectionException, IOException, AttributeNotFoundException{
        if(objectNames != null){

            pw.beginObject();
            pw.name("domain").value(domain);

            pw.name("mbeans").beginArray();
            final Iterator iter = objectNames.iterator();
            while(iter.hasNext()){
                final ObjectName objectName = (ObjectName) iter.next();
                final MBeanInfo mBeanInfo = mBeanServer
                        .getMBeanInfo(objectName);
                renderJsonDomain(pw, objectName, mBeanInfo, renderDetails);
            }
            pw.endArray();
            pw.endObject();
        }
    }

    private String getPath(final ObjectName name){
        return name.getDomain();
    }

    private String getName(final ObjectName name){
        final String result = "";
        return result;
    }

    private void renderJsonDomain(final JsonWriter pw,
                                  final ObjectName objectName, final MBeanInfo mBeanInfo,
                                  boolean renderDetails) throws IOException,
            AttributeNotFoundException, InstanceNotFoundException{
        pw.beginObject();
        // using toString to make shure that type is set before any other
        // property
        String canonicalName = objectName.toString();
        String[] split = canonicalName.split(":");
        pw.name("mbean").value(split[1]);
        if(renderDetails){
            pw.name("attributes").beginArray();

            final MBeanAttributeInfo[] attrs = mBeanInfo.getAttributes();
            for(int i = 0; i < attrs.length; i++){
                final MBeanAttributeInfo attr = attrs[i];
                if(!attr.isReadable())
                    continue; // skip non readable properties
                // jsonValue(pw, attr.getName() + ":writable=" +
                // attr.isWritable());
                pw.beginObject();
                pw.name(attr.getName()).beginArray();
                pw.value("writable=" + attr.isWritable());
                Object value = null;
                try{
                    // Descriptor descriptor = attr.getDescriptor();
                    // descriptor.get
                    value = (mBeanServer.getAttribute(objectName,
                            attr.getName()));
                } catch(ReflectionException e){
                    // Munch skip this attribute then
                } catch(MBeanException e){
                    // Munch skip this attribute then
                } catch(RuntimeMBeanException e){
                    // Munch skip this attribute then
                }
                if(value != null){
                    if(!value.getClass().isArray()){
                        pw.value("value=" + value.toString());
                    } else{
                        pw.value("value=" + Arrays.toString((Object[]) value));
                    }
                }
                pw.endArray();
                pw.endObject();
            }
            pw.endArray();

            pw.name("operations").beginArray();

            final MBeanOperationInfo[] ops = mBeanInfo.getOperations();
            for(int i = 0; i < ops.length; i++){
                final MBeanOperationInfo op = ops[i];
                // jsonValue(pw, op.getDescription() + ": " + op.getName() +
                // " - "
                // + op.getReturnType());
                pw.value(op.getName());
            }
            pw.endArray();
        }
        pw.endObject();
    }

    private final String readTemplateFile(final Class clazz,
                                          final String templateFile){
        InputStream templateStream = getClass().getResourceAsStream(
                templateFile);
        if(templateStream != null){
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            try{
                int len = 0;
                while((len = templateStream.read(data)) > 0){
                    baos.write(data, 0, len);
                }
                return baos.toString("UTF-8");
            } catch(IOException e){
                // don't use new Exception(message, cause) because cause is 1.4+
                throw new RuntimeException("readTemplateFile: Error loading "
                        + templateFile + ": " + e);
            } finally{
                try{
                    templateStream.close();
                } catch(IOException e){
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
     * javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException{
        final String action = req.getParameter(PARAMETER_ACTION);
        // for now we only have the clear action
        if(ACTION_CLEAR.equals(action)){
        }
        // we always send back the json data
        resp.setContentType("application/json");
        resp.setCharacterEncoding("utf-8");

        renderJSON(resp.getWriter(), null, null);
    }

    private void renderJSON(final PrintWriter pw, String mbeanDomain,
                            String mbean) throws IOException{
        if(mBeanServer == null){
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        JsonWriter writer = new JsonWriter(pw);
        writer.beginObject();

        if(mBeanServer != null){
            try{
                final HashMap<String, ArrayList<ObjectName>> domains = getDomains(
                        mBeanServer, mbeanDomain, mbean);
                final Set<String> keyset = new TreeSet<String>(domains.keySet());

                writer.name("status").value(keyset.size() + "Domain received.");
                writer.name("data").beginArray();

                final Iterator<String> iter = keyset.iterator();
                while(iter.hasNext()){
                    final String domain = (String) iter.next();

                    final ArrayList<ObjectName> objectNames = domains
                            .get(domain);

                    Collections.sort(objectNames);

                    renderJsonDomain(writer, domain, objectNames, mbean != null);
                }
                writer.endArray();
                writer.endObject();
            } catch(final Exception e){
                e.printStackTrace();
            }
        }

    }

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException{

        String info = request.getPathInfo();
        if(info.endsWith(".json")){
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // remove .json
            info = info.substring(0, info.length() - 5);

            // remove label and starting slash
            info = info.substring(getLabel().length() + 1);
            // we only accept direct requests to a bundle if they have a slash
            // after the label
            String mbeanDomain = null;
            if(info.startsWith("/")){
                mbeanDomain = info.substring(1);
            }

            PrintWriter pw = response.getWriter();
            JsonWriter writer = new JsonWriter(pw);

            Map parameterMap = request.getParameterMap();

            if(mbeanDomain == null
                    || (mbeanDomain != null && parameterMap.isEmpty())){
                this.renderJSON(pw, mbeanDomain, null); // just loaded initialy
                // or
                // when using the filter
            } else{
                // parameter map is set, use it.
                // first check if it is a attribute or operation
                if(parameterMap.containsKey("attribute")){
                    // handle attribute calls
                    this.renderAttribute(writer, mbeanDomain, parameterMap);
                } else if(parameterMap.containsKey("operation")){
                    // Handle operation
                } else if(parameterMap.containsKey("mbean")){
                    this.renderJSON(pw, mbeanDomain,
                            ((String[]) parameterMap.get("mbean"))[0]);
                } else{
                    // what is this? shouldn't happen.
                    return;
                }
            }

            // nothing more to do
            return;
        }

        this.renderContent(request, response);
    }

    private void renderAttribute(JsonWriter pw, String mbeanDomain,
                                 Map parameterMap){
        try{

            String[] mbeans = (String[]) parameterMap.get("mbean");

            String[] attribute = (String[]) parameterMap.get("attribute");

            HashMap<String, ArrayList<ObjectName>> domains = getDomains(
                    mBeanServer, mbeanDomain, mbeans[0]);
            Collection<ArrayList<ObjectName>> values = domains.values();
            boolean found = false;

            pw.beginObject();
            pw.name("attributeName");
            pw.value(attribute[0]);

            for(ArrayList<ObjectName> arrayList : values){
                for(ObjectName objectName : arrayList){
                    if(objectName.getCanonicalName().contains(mbeans[0])){
                        // TODO: getAttribute values!
                        Object attributeValue = mBeanServer.getAttribute(
                                objectName, attribute[0]);
                        found = true;
                        pw.name("attributeValue");
                        pw.value(attributeValue.toString());
                        break;
                    }
                }
                if(found)
                    break;
            }

            pw.endObject();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    protected void renderContent(HttpServletRequest request,
                                 HttpServletResponse response) throws ServletException, IOException{
        final PrintWriter pw = response.getWriter();
        pw.print(TEMPLATE);
    }

    public URL getResource(String path){
        if(path.startsWith("/jmx/res/ui/")){
            return this.getClass().getResource(path.substring(4));
        }
        return null;
    }
}
