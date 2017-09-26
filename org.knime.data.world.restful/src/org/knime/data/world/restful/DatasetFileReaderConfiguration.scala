package org.knime.data.world.restful

import org.knime.core.node.NodeSettingsRO
import org.knime.core.node.NodeSettingsWO


class DatasetFileReaderConfiguration {
  private var m_datasetString : String = DatasetFileReaderNodeModel.DEFAULT_DATASETNAME
  private var m_tableString : String = null
  
  def getDatasetString() : String = {
    return m_datasetString
  }
  
  def setDatasetString(datasetString : String) : Unit = {
    m_datasetString = datasetString
  }
  
  def getTableString() : String = {
    return m_tableString
  }
  
  def setTableString(tableString : Any) : Unit = {
    m_tableString = tableString toString
  }
  
  def saveConfiguration(settings : NodeSettingsWO) : Unit = {
    if (m_datasetString != null) {
      settings addString(DatasetFileReaderNodeModel.CFGKEY_DATASETNAME, m_datasetString)
      settings addString(DatasetFileReaderNodeModel.CFGKEY_TABLENAME, m_tableString)
    }
  }
  
  def loadSettingsInModel(settings : NodeSettingsRO) : Unit = {
    m_datasetString = settings getString(DatasetFileReaderNodeModel.CFGKEY_DATASETNAME)
    m_tableString = settings getString(DatasetFileReaderNodeModel.CFGKEY_TABLENAME)
  }
  
  def loadSettingsInDialog(settings : NodeSettingsRO) : Unit = {
    m_datasetString = settings getString(DatasetFileReaderNodeModel.CFGKEY_DATASETNAME, DatasetFileReaderNodeModel.DEFAULT_DATASETNAME)
    m_tableString = settings getString(DatasetFileReaderNodeModel.CFGKEY_TABLENAME, DatasetFileReaderNodeModel.DEFAULT_TABLENAME)
  }
}