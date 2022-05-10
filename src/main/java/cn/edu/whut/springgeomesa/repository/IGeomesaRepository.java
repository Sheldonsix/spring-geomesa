package cn.edu.whut.springgeomesa.repository;

import cn.edu.whut.springgeomesa.config.IGeomesaDataConfig;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @ClassName IGeomesaRepository
 * @Description TODO
 * @Author sheldon
 * @Date 2022/5/8 15:42
 * @Version 1.0
 **/
public interface IGeomesaRepository {
    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 创建 datastore
     * @param: params
     * @return org.geotools.data.DataStore
     **/
    DataStore createDataStore(Map<String, String> params) throws IOException;

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 获取 SimpleFeatureType
     * @param: data
     * @return org.opengis.feature.simple.SimpleFeatureType
     **/
    SimpleFeatureType getSimpleFeatureType(IGeomesaDataConfig data);

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 在 datastore 添加 SimpleFeatureType
     * @param: datastore
     * @param: sft
     * @return void
     **/
    void createSchema(DataStore datastore, SimpleFeatureType sft) throws IOException;

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 获取 features
     * @param: data
     * @return java.util.List<org.opengis.feature.simple.SimpleFeature>
     **/
    List<SimpleFeature> getFeatures(IGeomesaDataConfig data);

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 插入批量数据
     * @param: datastore
     * @param: sft
     * @param: features
     * @return void
     **/
    void writeFeatures(DataStore datastore, SimpleFeatureType sft, List<SimpleFeature> features) throws IOException;

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 获取 Queries 信息
     * @param: data
     * @return java.util.List<org.geotools.data.Query>
     **/
    List<Query> getQueries(IGeomesaDataConfig data);

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 执行批量查询
     * @param: datastore
     * @param: queries
     * @return void
     **/
    void queryFeature(DataStore datastore, List<Query> queries) throws IOException;

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 删除数据源
     * @param: datastore
     * @param: typeName
     * @return void
     **/
    void cleanup(DataStore datastore, String typeName);
}
