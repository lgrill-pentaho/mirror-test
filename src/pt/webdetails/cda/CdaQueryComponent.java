package pt.webdetails.cda;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.TableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.commons.connection.IPentahoResultSet;
import org.pentaho.commons.connection.memory.MemoryMetaData;
import org.pentaho.commons.connection.memory.MemoryResultSet;

import pt.webdetails.cda.query.QueryOptions;
import pt.webdetails.cda.settings.CdaSettings;
import pt.webdetails.cda.settings.SettingsManager;

/**
 * This is a CDA Pojo Component that can be used in XActions or anywhere else.
 * 
 * @author Will Gorman (wgorman@pentaho.com)
 *
 */
public class CdaQueryComponent {

  private static final Log log = LogFactory.getLog(CdaQueryComponent.class);
  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final int DEFAULT_START_PAGE = 0;
  
  IPentahoResultSet resultSet = null;
  String file = null;
  Map<String, Object> inputs = new HashMap<String, Object>();
  
  public void setFile(String file) {
    this.file = file;
  }
  
  public void setInputs(Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  public boolean validate() throws Exception {
    if (file == null) {
      log.error("File not set"); //$NON-NLS-1$
      return false;
    }
    // verify file exists
    
    return true;
  }
  
  private long inputsGetLong(String name, long defaultVal) {
      Object obj = inputs.get(name);
      // pojo component forces all strings to upper case :-(
      if (obj == null) {
        obj = inputs.get(name.toUpperCase());
      }
      if (obj == null) {
        return defaultVal;
      }
      return new Long(obj.toString());
  }
  
  private String inputsGetString(String name, String defaultVal) {
    Object obj = inputs.get(name);
    // pojo component forces all strings to upper case :-(
    if (obj == null) {
      obj = inputs.get(name.toUpperCase());
    }
    if (obj == null) {
      return defaultVal;
    }
    return obj.toString();
  }
  
  public boolean execute() throws Exception {
    final CdaEngine engine = CdaEngine.getInstance();
    final QueryOptions queryOptions = new QueryOptions();

    final CdaSettings cdaSettings = SettingsManager.getInstance().parseSettingsFile(file);

    // page info
    
    final long pageSize = inputsGetLong("pageSize", 0);
    final long pageStart = inputsGetLong("pageStart", 0);
    final boolean paginate = "true".equals(inputsGetString("paginateQuery", "false"));
    if (pageSize > 0 || pageStart > 0 || paginate) {
      if (pageSize > Integer.MAX_VALUE || pageStart > Integer.MAX_VALUE) {
        throw new ArithmeticException("Paging values too large");
      }
      queryOptions.setPaginate(true);
      queryOptions.setPageSize(pageSize > 0 ? (int) pageSize : paginate ? DEFAULT_PAGE_SIZE : 0);
      queryOptions.setPageStart(pageStart > 0 ? (int) pageStart : paginate ? DEFAULT_START_PAGE : 0);
    }

    // query info 
    
    queryOptions.setOutputType(inputsGetString("outputType", "resultset"));
    queryOptions.setDataAccessId(inputsGetString("dataAccessId", "<blank>"));
    
    // params and settings
    
    for (String param : inputs.keySet()) {
      if (param.startsWith("param")) {
        queryOptions.addParameter(param.substring(5), inputsGetString(param, ""));
      } else if (param.startsWith("setting")) {
        queryOptions.addSetting(param.substring(7), inputsGetString(param, ""));
      }
    }

    if (queryOptions.getOutputType().equals("resultset")) {
      TableModel tableModel = cdaSettings.getDataAccess(queryOptions.getDataAccessId()).doQuery(queryOptions);
      resultSet = convertTableToResultSet(tableModel);
    } else {
      // TODO: Support binary outputs
    }
    
    return true;
  }
  
  private IPentahoResultSet convertTableToResultSet(TableModel tableModel) {
    List<String> columnNames = new ArrayList<String>();
    for (int i = 0; i < tableModel.getColumnCount(); i++) {
      columnNames.add(tableModel.getColumnName(i));
    }
    MemoryMetaData metadata = new MemoryMetaData(columnNames);
    MemoryResultSet memResultSet = new MemoryResultSet(metadata);
    for (int i = 0; i < tableModel.getRowCount(); i++) {
      Object row[] = new Object[tableModel.getColumnCount()];
      for (int j = 0; j < tableModel.getColumnCount(); j++) {
        row[j] = tableModel.getValueAt(i, j);
      }
      memResultSet.addRow(row);
    }
    return memResultSet;
  }
  
  public IPentahoResultSet getResultSet() {
    return resultSet;
  }
}