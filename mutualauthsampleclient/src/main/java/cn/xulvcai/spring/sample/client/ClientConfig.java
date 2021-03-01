package cn.xulvcai.spring.sample.client;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.tomcat.util.net.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;

@Configuration
public class ClientConfig {

    private static final String SSL_PROTO_DEFAULT = Constants.SSL_PROTO_TLSv1_2;

    @Value("${client.keystore.file}")
    private String clientKeystoreFile;

    @Value("${client.keystore.pass}")
    private String clientKeystorePass;

    @Value("${client.keystore.type}")
    private String clientKeystoreType;

    @Value("${client.truststore.file}")
    private String clientTruststoreFile;

    @Value("${client.truststore.pass}")
    private String clientTruststorePass;

    @Value("${client.truststore.type}")
    private String clientTruststoreType;

    @Value("${client.hostname.verified}")
    private boolean clientHostnameVerified;


    @Bean
    public RestTemplate restTemplate() throws Exception {
        return createRestTemplate();
    }

    private RestTemplate createRestTemplate() throws Exception {
        HttpClient httpClient = getHttpClient();
        ClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return createRestTemplate(clientHttpRequestFactory);
    }

    private RestTemplate createRestTemplate(ClientHttpRequestFactory httpRequestFactory) {
        RestTemplate restTemplate = new RestTemplate(httpRequestFactory);
        List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
        int index = -1;
        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i) instanceof StringHttpMessageConverter) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            // the RestTemplate use default StringHttpMessageConverter which support ISO-8859-1 as default charset
            // replace with UTF-8
            converters.set(index, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        } else {
            converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        }
        return restTemplate;
    }

    private HttpClient getHttpClient() throws Exception {
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", getSSLConnectionSocketFactory())
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        connectionManager.setMaxTotal(10);
        connectionManager.setDefaultMaxPerRoute(5);
        RequestConfig requestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .setSocketTimeout(1000)
                .setConnectTimeout(1000)
                .setConnectionRequestTimeout(1000)
                .build();
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
                .build();
    }

    private SSLConnectionSocketFactory getSSLConnectionSocketFactory() throws Exception {

        return new SSLConnectionSocketFactory(
                createSSLContext(),
                new String[] { SSL_PROTO_DEFAULT },
                null,
                clientHostnameVerified
                        ? SSLConnectionSocketFactory.getDefaultHostnameVerifier()
                        : NoopHostnameVerifier.INSTANCE);
    }

    private SSLContext createSSLContext() throws Exception {
        KeyStore keystore = getKeyStore(clientKeystoreFile, clientKeystorePass, clientKeystoreType);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystore, StringUtils.isEmpty(clientKeystorePass)
                ? (char[])null : clientKeystorePass.toCharArray());

        KeyStore truststore = getKeyStore(clientTruststoreFile, clientTruststorePass, clientTruststoreType);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(truststore);
        SSLContext sslContext = SSLContext.getInstance(SSL_PROTO_DEFAULT);
        sslContext.init(keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    private KeyStore getKeyStore(String keystoreFile, String keystorePass, String keystoreType) throws Exception {
        InputStream keystoreStream = new FileInputStream(ResourceUtils.getFile(keystoreFile));
        KeyStore keyStore = KeyStore.getInstance(keystoreType);
        keyStore.load(keystoreStream, StringUtils.isEmpty(keystorePass)
                ? (char[])null : keystorePass.toCharArray());

        return keyStore;
    }
}
