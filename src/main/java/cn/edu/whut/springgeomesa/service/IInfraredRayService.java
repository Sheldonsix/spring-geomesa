package cn.edu.whut.springgeomesa.service;

import java.util.Map;

/**
 * @ClassName IInfraredRayService
 * @Description TODO
 * @Author sheldon
 * @Date 2022/5/8 17:13
 * @Version 1.0
 **/
public interface IInfraredRayService {
    String attributeQuery(Map<String, String> params, String waveLength, String SID);
    Boolean insertInfraredRayData(Map<String, String> params);
    Boolean deleteInfraredRayDatastore(Map<String, String> params);
    String spatiotemporalAttributeQuery(Map<String, String> params, String catalogName, String SID, String PID, String No,String waveLength);
}
