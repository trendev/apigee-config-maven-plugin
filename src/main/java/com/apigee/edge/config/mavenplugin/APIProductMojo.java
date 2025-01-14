/**
 * Copyright (C) 2016 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apigee.edge.config.mavenplugin;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.apigee.edge.config.rest.RestUtil;
import com.apigee.edge.config.utils.ServerProfile;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.util.Key;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**                                                                                                                                     ¡¡
 * Goal to create API Product in Apigee EDGE
 * scope: org
 *
 * @author madhan.sadasivam
 * @goal apiproducts
 * @phase install
 */

public class APIProductMojo extends GatewayAbstractMojo
{
	static Logger logger = LogManager.getLogger(APIProductMojo.class);
	public static final String ____ATTENTION_MARKER____ =
	"************************************************************************";

	enum OPTIONS {
		none, create, update, delete, sync
	}

	OPTIONS buildOption = OPTIONS.none;

	private ServerProfile serverProfile;

    public static class APIProduct {
        @Key
        public String name;
    }
	
	public APIProductMojo() {
		super();

	}
	
	public void init() throws MojoFailureException {
		try {
			logger.info(____ATTENTION_MARKER____);
			logger.info("Apigee API Product");
			logger.info(____ATTENTION_MARKER____);

			String options="";
			serverProfile = super.getProfile();			
	
			options = super.getOptions();
			if (options != null) {
				buildOption = OPTIONS.valueOf(options);
			}
			logger.debug("Build option " + buildOption.name());
			logger.debug("Base dir " + super.getBaseDirectoryPath());
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Invalid apigee.option provided");
		} catch (RuntimeException e) {
			throw e;
		}

	}

	protected String getAPIProductName(String payload) 
            throws MojoFailureException {
		Gson gson = new Gson();
		try {
			APIProduct product = gson.fromJson(payload, APIProduct.class);
			return product.name;
		} catch (JsonParseException e) {
		  throw new MojoFailureException(e.getMessage());
		}
	}

	protected void doUpdate(List<String> products) 
            throws MojoFailureException {
		try {
			//List existingAPIProducts = null;
			if (buildOption != OPTIONS.update && 
				buildOption != OPTIONS.create &&
                buildOption != OPTIONS.delete &&
                buildOption != OPTIONS.sync) {
				return;
			}

			//Commenting due to https://github.com/apigee/apigee-config-maven-plugin/issues/130
			//logger.info("Retrieving existing API Products");
			//existingAPIProducts = getAPIProduct(serverProfile);

	        for (String product : products) {
	        	String productName = getAPIProductName(product);
	        	if (productName == null) {
	        		throw new IllegalArgumentException(
	        			"API Product does not have a name.\n" + product + "\n");
	        	}

	        	if (doesAPIProductExist(serverProfile, productName)) {
                    switch (buildOption) {
                        case update:
    						logger.info("API Product \"" + productName + 
			           					"\" exists. Updating.");
	          				updateAPIProduct(serverProfile, productName, product);
                            break;
                        case create:
        			        logger.info("API Product \"" + productName + 
    									"\" already exists. Skipping.");
                            break;
                        case delete:
                            logger.info("API Product \"" + productName + 
                                        "\" already exists. Deleting.");
                            deleteAPIProduct(serverProfile, productName);
                            break;
                        case sync:
                            logger.info("API Product \"" + productName + 
                                        "\" already exists. Deleting and recreating.");
                            deleteAPIProduct(serverProfile, productName);
                            logger.info("Creating API Product - " + productName);
                            createAPIProduct(serverProfile, product);
                            break;
	        		}
	        	} else {
                    switch (buildOption) {
                        case create:
                        case sync:
                        case update:
                            logger.info("Creating API Product - " + productName);
                            createAPIProduct(serverProfile, product);
                            break;
                        case delete:
                            logger.info("API Product \"" + productName + 
                                        "\" does not exist. Skipping.");
                            break;
                    }
	        	}
			}
		
		} catch (IOException e) {
			throw new MojoFailureException("Apigee network call error " +
														 e.getMessage());
		} catch (RuntimeException e) {
			throw e;
		}
	}

	/** 
	 * Entry point for the mojo.
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (super.isSkip()) {
			logger.info("Skipping");
			return;
		}

		Logger logger = LogManager.getLogger(APIProductMojo.class);

		try {
			
			init();

			if (buildOption == OPTIONS.none) {
				logger.info("Skipping API Products (default action)");
				return;
			}

            if (serverProfile.getEnvironment() == null) {
                throw new MojoExecutionException(
                            "Apigee environment not found in profile");
            }

			List products = getOrgConfig(logger, "apiProducts");
			if (products == null || products.size() == 0) {
				logger.info("No API Products found.");
                return;
			}

			doUpdate(products);				
			
		} catch (MojoFailureException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		}
	}

    /***************************************************************************
     * REST call wrappers
     **/
    public static String createAPIProduct(ServerProfile profile, String product)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.createOrgConfig(profile, 
                                                         "apiproducts",
                                                         product);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Create Success.");

        } catch (HttpResponseException e) {
            logger.error("API Product create error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String updateAPIProduct(ServerProfile profile, 
                                        String productName, 
                                        String product)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.updateOrgConfig(profile, 
                                                        "apiproducts", 
                                                        productName,
                                                        product);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
            	logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("API Product update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static String deleteAPIProduct(ServerProfile profile,
                                            String productName)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.deleteOrgConfig(profile, 
                                                        "apiproducts", 
                                                        productName);
        try {
            
            logger.info("Response " + response.getContentType() + "\n" +
                                        response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Delete Success.");

        } catch (HttpResponseException e) {
            logger.error("API Product delete error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return "";
    }

    public static List getAPIProduct(ServerProfile profile)
            throws IOException {
    	RestUtil restUtil = new RestUtil(profile);
        HttpResponse response = restUtil.getOrgConfig(profile, 
                                                    "apiproducts");
        if(response == null) return new ArrayList();
        JSONArray products = new JSONArray();
        try {
            logger.debug("output " + response.getContentType());
            // response can be read only once
            String payload = response.parseAsString();
            logger.debug(payload);
            JSONParser parser = new JSONParser();       
            JSONObject obj     = (JSONObject)parser.parse(payload);
            JSONArray productsArray    = (JSONArray)obj.get("apiProduct");
            for (int i = 0; productsArray != null && i < productsArray.size(); i++) {
             	 JSONObject a = (JSONObject) productsArray.get(i);
             	 products.add(a.get("name"));
           }
        } catch (ParseException pe){
            logger.error("Get API Product parse error " + pe.getMessage());
            throw new IOException(pe.getMessage());
        } catch (HttpResponseException e) {
            logger.error("Get API Product error " + e.getMessage());
            throw new IOException(e.getMessage());
        }

        return products;
    }
    
    public static boolean doesAPIProductExist(ServerProfile profile, String apiProduct)
            throws IOException {
        try {
        	RestUtil restUtil = new RestUtil(profile);
        	logger.info("Checking if APIProduct - " +apiProduct + " exist");
            HttpResponse response = restUtil.getOrgConfig(profile, "apiproducts/"+URLEncoder.encode(apiProduct, "UTF-8"));
            if(response == null) 
            	return false;
        } catch (HttpResponseException e) {
            throw new IOException(e.getMessage());
        }

        return true;
    }
}




