package cn.edu.whut.springgeomesa.service;

import java.util.Map;

/**
 * @ClassName IMilitaryShipTraceService
 * @Description TODO
 * @Author sheldon
 * @Date 2022/5/25 16:59
 * @Version 1.0
 **/
public interface IMilitaryShipTraceService {
    String attributeQuery(Map<String, String> params, String queryStr);
    Boolean insertMilitaryShipTraceData(Map<String, String> params);
    Boolean deleteMilitaryShipTraceDatastore(Map<String, String> params);
    String spatiotemporalAttributeQuery(Map<String, String> params, String queryStr);
}
