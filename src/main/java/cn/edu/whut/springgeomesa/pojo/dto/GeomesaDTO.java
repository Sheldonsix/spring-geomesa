package cn.edu.whut.springgeomesa.pojo.dto;

import cn.edu.whut.springgeomesa.config.IGeomesaDataConfig;

import java.util.Map;

/**
 * @ClassName GeomesaDTO
 * @Description TODO
 * @Author sheldon
 * @Date 2022/5/8 17:07
 * @Version 1.0
 **/
public class GeomesaDTO {
    // HBase 参数
    private Map<String, String> params;
    // 数据
    private IGeomesaDataConfig data;

    // 使用 set 和 get 方法
    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public IGeomesaDataConfig getData() {
        return data;
    }

    public void setData(IGeomesaDataConfig data) {
        this.data = data;
    }

}
