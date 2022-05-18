package cn.edu.whut.springgeomesa.config;

import org.geotools.data.Query;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.util.List;

/**
 * @ClassName IGeomesaDataConfig
 * @Description 通用 geomesa 数据接口
 * @Author sheldon
 * @Date 2022/5/8 15:48
 * @Version 1.0
 **/

public interface IGeomesaDataConfig {
    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 获取数据类的 Schema 名
     * @param:
     * @return java.lang.String
     **/
    String getTypeName();

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 获取数据集的 Simple Feature Type 实例
     * @param:
     * @return org.opengis.feature.simple.SimpleFeatureType
     **/
    SimpleFeatureType getSimpleFeatureType();

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 获取数据集的数据
     * @param: offset
     * @param: size
     * @return java.util.List<org.opengis.feature.simple.SimpleFeature>
     **/
    List<SimpleFeature> getData(int offset);

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 获取数据集的查询
     * @param:
     * @return java.util.List<org.geotools.data.Query>
     **/
    List<Query> getQueries();
    Filter getSubsetFilter();
}
