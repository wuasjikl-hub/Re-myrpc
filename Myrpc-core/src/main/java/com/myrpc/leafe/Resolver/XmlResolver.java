package com.myrpc.leafe.Resolver;

import com.myrpc.leafe.config.RegistryConfig;
import com.myrpc.leafe.configration.Configration;
import com.myrpc.leafe.enumeration.CompressorType;
import com.myrpc.leafe.enumeration.LoadBalancerType;
import com.myrpc.leafe.enumeration.SerializerType;
import com.myrpc.leafe.utils.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.InputStream;

@Slf4j
public class XmlResolver {
    public void loadFromXml(Configration configuration) {
        try {
            // 1. 获取文档
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            // 禁用DTD校验：可以通过调用setValidating(false)方法来禁用DTD校验。
            documentBuilderFactory.setValidating(false);
            // 禁用外部实体解析：可以通过调用setFeature(String name, boolean value)方法并将“http://apache.org/xml/features/nonvalidating/load-external-dtd”设置为“false”来禁用外部实体解析。
            documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream("myrpc.xml");

            if (stream == null) {
                log.warn("未找到配置文件myrpc.xml，使用默认配置");
                return;
            }

            Document document = builder.parse(stream);

            // 2. 获取XPath解析器
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();

            // 3. 解析各个配置项
            IdGenerator idGenerator = getidGenerator(document, xPath);
            configuration.setIdGenerator(idGenerator);

            int port = parseInt(document, xPath, "/configuration/Port", 8003);
            configuration.setPort(port);

            String app = parseToStringdef(document, xPath, "/configuration/appName", "default");
            configuration.setApplication(app);

            String type = parseToStringdef(document, xPath, "/configuration/serializeType", "type", SerializerType.SERIALIZERTYPE_HESSION.getType());
            configuration.setSerializeType(type);

            String compressType = parseToStringdef(document, xPath, "/configuration/compressType", "type", CompressorType.COMPRESSTYPE_GZIP.getType());
            configuration.setCompressType(compressType);

            String balancerName = parseToStringdef(document, xPath, "/configuration/loadBalancerType", "type", LoadBalancerType.LoadBalancerType_ConsistentHash.getType());
            configuration.setBalancerName(balancerName);

            String registryType = parseToStringdef(document, xPath, "/configuration/registry", "type", "zookeeper");
            String registryAddress = parseToStringdef(document, xPath, "/configuration/registry", "Address", "localhost:2181");
            log.info("registry:{}", registryType);
            configuration.setRegistryConfig(new RegistryConfig(registryType, registryAddress));

            // 4. 验证配置
            ConfigurationValidator.validate(configuration);

        } catch (Exception e) {
            log.error("解析配置文件错误", e);
            // 可以选择抛出异常或使用默认配置
            throw new RuntimeException("配置文件解析失败", e);
        }
    }

    // 添加带默认值的解析方法
    private int parseInt(Document doc, XPath xpath, String expression, int defaultValue) {
        try {
            String value = parseToString(doc, xpath, expression);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            log.warn("解析整数失败，使用默认值: {}", defaultValue);
            return defaultValue;
        }
    }

    // 添加带默认值的字符串解析方法
    private String parseToStringdef(Document doc, XPath xpath, String expression, String defaultValue) {
        String value = parseToString(doc, xpath, expression);
        return value != null ? value : defaultValue;
    }

    // 添加带默认值的属性解析方法
    private String parseToStringdef(Document doc, XPath xpath, String expression, String attributeName, String defaultValue) {
        String value = parseToString(doc, xpath, expression, attributeName);
        return value != null ? value : defaultValue;
    }

    // 其他方法保持不变...
    private IdGenerator getidGenerator(Document doc, XPath xpath) {
        String expr = "/configuration/idGenerator";
        String dataCenterId = parseToStringdef(doc, xpath, expr, "dataCenterId", "1");
        String machineId = parseToStringdef(doc, xpath, expr, "MachineId", "1");

        try {
            Object object = parseToObject(doc, xpath, expr, new Class[]{long.class, long.class},
                    Long.parseLong(dataCenterId), Long.parseLong(machineId));
            return (IdGenerator) object;
        } catch (Exception e) {
            log.warn("解析ID生成器失败，使用默认生成器");
            return new IdGenerator(1, 1);
        }
    }

    /**
     * 返回文本值 eg.<port>7777</>
     */
    private String parseToString(Document doc, XPath xpath, String expression) {
        try {
            XPathExpression expr = xpath.compile(expression);
            Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);
            return node != null ? node.getTextContent() : null;
        } catch (XPathExpressionException e) {
            log.error("解析表达式失败: {}", expression, e);
        }
        return null;
    }

    /**
     * 获取属性值 eg.<port num="7777777"></port>
     */
    private String parseToString(Document doc, XPath xpath, String expression, String attributeName) {
        try {
            XPathExpression expr = xpath.compile(expression);
            Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);
            if (node != null && node.getAttributes() != null) {
                Node attribute = node.getAttributes().getNamedItem(attributeName);
                return attribute != null ? attribute.getNodeValue() : null;
            }
        } catch (XPathExpressionException e) {
            log.error("解析表达式失败: {}", expression, e);
        }
        return null;
    }

    /**
     * 获取对象值 eg.<idGenerator class="com.myrpc.leafe.utils.IdGenerator"/>
     */
    private Object parseToObject(Document doc, XPath xpath, String expression, Class<?>[] parameterTypes, Object... params) {
        try {
            XPathExpression expr = xpath.compile(expression);
            Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);
            if (node == null) {
                return null;
            }

            String className = node.getAttributes().getNamedItem("class").getNodeValue();
            Class<?> clazz = Class.forName(className);

            if (parameterTypes == null || parameterTypes.length == 0) {
                return clazz.newInstance();
            }

            return clazz.getDeclaredConstructor(parameterTypes).newInstance(params);
        } catch (Exception e) {
            log.error("解析对象失败: {}", expression, e);
        }
        return null;
    }
}