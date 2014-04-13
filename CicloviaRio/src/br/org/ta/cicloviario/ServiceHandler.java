package br.org.ta.cicloviario;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class ServiceHandler {
	static String response = null;
	public final static int GET = 1;
	public final static int POST = 2;
	
	public ServiceHandler() {
		
	}
	
	/**
	 * Fazendo chamada ao servico
	 * @param url - url para fazer o pedido
	 * @param method - metodo de pedido http
	 * */
	public String makeServiceCall(String url, int method) {
		return this.makeServiceCall(url, method, null);
	}
	
	/**
	 * Fazendo chamada ao servico
	 * @param url - url para fazer o pedido
	 * @param method - metodo de pedido http
	 * @param params - parametros de pedido http
	 * */
	public String makeServiceCall(String url, int method, List<NameValuePair> params) {
		try {
            // cliente http
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpEntity httpEntity = null;
            HttpResponse httpResponse = null;
             
            // Checando o tipo de metodo de requisicao de http
            if (method == POST) {
                HttpPost httpPost = new HttpPost(url);
                // adicionando parametros no post
                if (params != null) {
                    httpPost.setEntity(new UrlEncodedFormEntity(params));
                }
 
                httpResponse = httpClient.execute(httpPost);
 
            } else if (method == GET) {
                // incluindo os parametros ao url
                if (params != null) {
                    String paramString = URLEncodedUtils
                            .format(params, "utf-8");
                    url += "?" + paramString;
                }
                HttpGet httpGet = new HttpGet(url);
 
                httpResponse = httpClient.execute(httpGet);
 
            }
            httpEntity = httpResponse.getEntity();
            response = EntityUtils.toString(httpEntity);
 
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
         
        return response;
 
	}
}
