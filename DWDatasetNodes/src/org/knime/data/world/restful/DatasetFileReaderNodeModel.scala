package org.knime.data.world.restful

import java.io.File
import java.net.URI

import org.knime.core.data.DataCell
import org.knime.core.data.DataColumnSpec
import org.knime.core.data.DataColumnSpecCreator
import org.knime.core.data.DataRow
import org.knime.core.data.DataTableSpec
import org.knime.core.data.DataType
import org.knime.core.data.RowKey
import org.knime.core.data.`def`.DefaultRow
import org.knime.core.data.`def`.DoubleCell
import org.knime.core.data.`def`.IntCell
import org.knime.core.data.`def`.StringCell

import org.knime.core.node.BufferedDataContainer
import org.knime.core.node.BufferedDataTable
import org.knime.core.node.CanceledExecutionException
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded
import org.knime.core.node.defaultnodesettings.SettingsModelString
import org.knime.core.node.port.database.reader.DBReader
import org.knime.core.node.port.database.DatabaseConnectionSettings
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings
import org.knime.core.node.ExecutionContext
import org.knime.core.node.ExecutionMonitor
import org.knime.core.node.InvalidSettingsException
import org.knime.core.node.NodeLogger
import org.knime.core.node.NodeModel
import org.knime.core.node.NodeSettingsRO
import org.knime.core.node.NodeSettingsWO

import org.knime.core.util.KnimeEncryption

import org.knime.data.world.prefs.DWPluginActivator

import scala.annotation.switch


/**
 * This is the model implementation of GetDatasetInfo.
 * 
 *
 * @author Appliomics, LLC
 */
class DatasetFileReaderNodeModel extends NodeModel(0, 1) {
  // NodeModel(0, 1) indicates zero incoming ports and one outgoing port

  protected val m_settings = new DatabaseQueryConnectionSettings()
  private var m_outSpec : DataTableSpec = null
  private val m_username : SettingsModelString =
    new SettingsModelString(
      DatasetFileReaderNodeModel.CFGKEY_USER,
      DatasetFileReaderNodeModel.DEFAULT_USER)
  private val m_datasetName : SettingsModelString =
    new SettingsModelString(
      DatasetFileReaderNodeModel.CFGKEY_DATASETNAME,
      DatasetFileReaderNodeModel.DEFAULT_DATASETNAME)
  private val m_datasetFileURL : SettingsModelString =
    new SettingsModelString(
      DatasetFileReaderNodeModel.CFGKEY_DATASETFILEURL,
      DatasetFileReaderNodeModel.DEFAULT_DATASETFILEURL)


  /**
   * {@inheritDoc}
   */
  protected override def execute(inData : Array[BufferedDataTable], exec : ExecutionContext) : Array[BufferedDataTable] = {
    DatasetFileReaderNodeModel.logger info("Preparing to contact data.world")

    // Reconfigure DataTableSpec for the output port
    val outputSpec : DataTableSpec = configure(null)(0)

    // Populate output port with data
    exec setProgress("Connecting to data.world...")
    val reader = getReader
    val outputDataTable : BufferedDataTable = produceResultTable(exec, reader)
    
    // Update output DataTableSpec
    // TODO: Verify that output spec never actually changes here
    m_outSpec = outputDataTable getDataTableSpec
    
    return Array[BufferedDataTable](outputDataTable)
  }

  protected def getReader() : DBReader = {
    // TODO: populate m_settings as necessary
    val query = m_settings getQuery
    val connSettings = new DatabaseQueryConnectionSettings(m_settings, query)
    val reader = connSettings getUtility() getReader(new DatabaseQueryConnectionSettings(connSettings, query))

    return reader
  }

  protected def produceResultTable(exec : ExecutionContext, reader : DBReader) : BufferedDataTable = {
    val resultDataTable = reader.createTable(exec, getCredentialsProvider)

    return resultDataTable
  }

  /**
   * {@inheritDoc}
   */
  protected override def reset() : Unit = {
    // TODO Code executed on reset.
    // Models build during execute are cleared here.
    // Also data handled in load/saveInternals will be erased here.
  }

  /**
   * {@inheritDoc}
   */
  protected override def configure(inSpecs : Array[DataTableSpec]) : Array[DataTableSpec] = {
    val username : String = DWPluginActivator.getUsername
    val password : String = DWPluginActivator.getPassword
    var account : String = username
    var dataset : String = null
    var filename : String = null
    val uri = new URI(m_datasetFileURL getStringValue)
    val path : Array[String] = uri.getPath.split("/")
    val uriQuery : String = uri.getQuery
    
    if (((path.length == 5) && ((path(3) != "workspace") || (path(4) != "file")))
        || (path.length > 5)
        || (path.length == 4)
        || (path.length < 2)) {
      DatasetFileReaderNodeModel.logger debug("Configure sees path.length: " + path.length)
      throw new InvalidSettingsException("URL path is not recognizable; expected: https://data.world/<account>/<dataset>/workspace/file?filename=<datafilename>")
    } else if (path.length == 2) {
      dataset = path(0)
      filename = path(1)
    } else if (path.length == 3) {
      account = path(0)
      dataset = path(1)
      filename = path(2)
    } else {
      if (uri.getHost != "data.world") throw new InvalidSettingsException("URL does not refer to data.world")
      if (uri.getScheme != "https") throw new InvalidSettingsException("URL is expected to use https")
      account = path(1)
      dataset = path(2)
      filename = uriQuery.split('=')(1).split('.')(0) // TODO: Make much more robust
    }
    DatasetFileReaderNodeModel.logger debug("Configure using account: " + account)
    DatasetFileReaderNodeModel.logger debug("Configure using dataset: " + dataset)
    DatasetFileReaderNodeModel.logger debug("Configure using filename: " + filename)
    
    m_settings setDriver("world.data.jdbc.Driver")
    m_settings setJDBCUrl("jdbc:data:world:sql:" + account + ":" + dataset)
    m_settings setUserName(username)  // Currently appears unused/redundant, will keep for now.
    m_settings setPassword(KnimeEncryption.encrypt(password.toArray))
    m_settings setTimezone("none")
    m_settings setAllowSpacesInColumnNames(true)

    val query : String = "SELECT * FROM " + filename
    m_settings setQuery(query)
    
    DatasetFileReaderNodeModel.logger debug("Configure about to determine output data table spec from reader")
    m_outSpec = getReader().getDataTableSpec(getCredentialsProvider)

    return Array[DataTableSpec](m_outSpec)
  }

  /**
   * {@inheritDoc}
   */
  protected override def saveSettingsTo(settings : NodeSettingsWO) : Unit = {
    //m_username saveSettingsTo(settings)
    //m_datasetName saveSettingsTo(settings)
    m_datasetFileURL saveSettingsTo(settings)
    DatasetFileReaderNodeModel.logger debug("saveSettingsTo about to call saveConnection")
    
    // TODO: Possibly replace with something that helps the related entries re: validated settings below
    //settings.addString(DatasetFileReaderNodeModel.CFGKEY_SQLSTATEMENT, m_settings.getQuery)
    
    m_settings saveConnection(settings)
  }

  /**
   * {@inheritDoc}
   */
  protected override def loadValidatedSettingsFrom(settings : NodeSettingsRO) : Unit = {
    //m_username loadSettingsFrom(settings)
    //m_datasetName loadSettingsFrom(settings)
    m_datasetFileURL loadSettingsFrom(settings)

    // TODO: Replace with something that works
    //DatasetFileReaderNodeModel.logger debug("loadValidatedSettingsFrom about to call loadValidatedConnection")
    //val settingsChanged = m_settings loadValidatedConnection(settings, getCredentialsProvider)
    //if (settingsChanged) m_outSpec = null
  }

  /**
   * {@inheritDoc}
   */
  protected override def validateSettings(settings : NodeSettingsRO) : Unit = {
    //m_username validateSettings(settings)
    //m_datasetName validateSettings(settings)
    m_datasetFileURL validateSettings(settings)  // TODO: Validate looks like a url
    
    // TODO: Replace with something that works
    //DatasetFileReaderNodeModel.logger debug("validateSettings about to call validateConnection")
    //val connSettings = new DatabaseQueryConnectionSettings
    //connSettings.validateConnection(settings, getCredentialsProvider)
  }
    
  /**
   * {@inheritDoc}
   */
  protected override def loadInternals(internDir : File, exec : ExecutionMonitor) : Unit = {
    // TODO load internal data. 
    // Everything handed to output ports is loaded automatically (data
    // returned by the execute method, models loaded in loadModelContent,
    // and user settings set through loadSettingsFrom - is all taken care 
    // of). Load here only the other internals that need to be restored
    // (e.g. data used by the views).
  }
    
  /**
   * {@inheritDoc}
   */
  protected override def saveInternals(internDir : File, exec : ExecutionMonitor) : Unit = {
    // TODO save internal models. 
    // Everything written to output ports is saved automatically (data
    // returned by the execute method, models saved in the saveModelContent,
    // and user settings saved through saveSettingsTo - is all taken care 
    // of). Save here only the other internals that need to be preserved
    // (e.g. data used by the views).
  }

}

object DatasetFileReaderNodeModel {
  // the logger instance
  private val logger : NodeLogger = NodeLogger.getLogger(classOf[DatasetFileReaderNodeModel])

  val CFGKEY_USER : String = "data.world Username"
  val DEFAULT_USER : String = "knime"
  
  val CFGKEY_DATASETNAME : String = "Dataset Name"
  val DEFAULT_DATASETNAME : String = "dataset-01"

  val CFGKEY_DATASETFILENAME : String = "Dataset File"
  val DEFAULT_DATASETFILENAME : String = "filename-01"
  
  val CFGKEY_DATASETFILEURL : String = "Dataset File URL"
  val DEFAULT_DATASETFILEURL : String = "https://data.world/test-knime/dummy-data-01/workspace/file?filename=cocktails.csv"

  val CFGKEY_SQLSTATEMENT : String = DatabaseConnectionSettings.CFG_STATEMENT
}