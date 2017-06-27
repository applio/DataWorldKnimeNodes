package org.knime.data.world.restful

import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

import com.google.gson.Gson

import java.net.URI


case class SourceInfo(url : String,
                      syncStatus : String,
                      lastSyncStart : String,
                      lastSyncSuccess : String,
                      lastSyncFailure : String)
case class FileInfo(name : String,
                    sizeInBytes : Int,
                    source : SourceInfo,
                    created : String,
                    updated : String,
                    description : String,
                    labels : Array[String])
case class DatasetInfo(owner : String,
                       id : String,
                       title : String,
                       description : String,
                       summary : String,
                       tags : Array[String],
                       license : String,
                       visibility : String,
                       files : Array[FileInfo],
                       status : String,
                       created : String,
                       updated : String)

                       
class GetDatasetInfo {
  val uriPrebuild = new URIBuilder() setScheme("https") setHost("api.data.world")

  // TODO: Pull from preferences
  val apiKey : String = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwcm9kLXVzZXItY2xpZW50OnRlc3Qta25pbWUiLCJpc3MiOiJhZ2VudDp0ZXN0LWtuaW1lOjpjMTJmNGQ3Mi05NWVkLTQ4YzYtOWY4ZS1lOGZjNzBlNzM0NGMiLCJpYXQiOjE0OTU5ODU4OTYsInJvbGUiOlsidXNlcl9hcGlfd3JpdGUiLCJ1c2VyX2FwaV9yZWFkIl0sImdlbmVyYWwtcHVycG9zZSI6dHJ1ZX0.hj99p_bq3YjFcI-wqluqLFK0p1Mq1U2uaC_mJzU3ExEELYhxpPRz9881EXV-BvprepMjwgQQuRezi3CoQL17ZA"

  def request(username : String, dataset : String) : DatasetInfo = {
    val uri = uriPrebuild setPath("/v0/datasets/" + username + "/" + dataset) build
    
    return getRestJsonContent(uri, apiKey)
  }
  
  def request(urlAsString : String) : DatasetInfo = {
    val uri = new URIBuilder(urlAsString) build
    
    return getRestJsonContent(uri, apiKey)
  }

  def getRestJsonContent(url : URI, apiKey : String) : DatasetInfo = {
    val httpGet = new HttpGet(url)
    val requestConfig = RequestConfig custom() setSocketTimeout(5000) setConnectTimeout(5000) build()
    httpGet.setConfig(requestConfig)
    httpGet addHeader("Authorization", "Bearer " + apiKey)
    
    val httpClient = HttpClients createDefault()
    val httpResponse = httpClient execute(httpGet)
    
    val gson = new Gson
    var datasetInfo : DatasetInfo = null
    
    try {
      val entity = httpResponse getEntity
      val content = EntityUtils toString(entity)
      datasetInfo = gson fromJson(content, classOf[DatasetInfo])
    } finally {
      httpResponse close()
    }

    return datasetInfo
  }
}