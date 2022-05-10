package cn.edu.whut.springgeomesa.repository.impl;

import cn.edu.whut.springgeomesa.config.IGeomesaDataConfig;
import cn.edu.whut.springgeomesa.repository.IGeomesaRepository;
import org.apache.hadoop.hbase.regionserver.RegionServerRunningException;

import org.geotools.data.*;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.factory.Hints;
import org.locationtech.geomesa.index.geotools.GeoMesaDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @ClassName GeomesaRepository
 * @Description TODO
 * @Author sheldon
 * @Date 2022/5/8 20:19
 * @Version 1.0
 **/
@Repository
public class GeomesaRepository implements IGeomesaRepository {
    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 创建 datastore
     * @param: params
     * @return org.geotools.data.DataStore
     **/
    @Override
    public DataStore createDataStore(Map<String, String> params) throws IOException {
        System.out.println(params);
        DataStore datastore = DataStoreFinder.getDataStore(params);
        try {
            datastore = DataStoreFinder.getDataStore(params);
            if (datastore == null) {
                System.out.println("获取 datastore 实例失败，请检查参数");
                throw new RuntimeException("这些参数获得不了数据库实例");
            }
        } catch (Exception e) {
            System.out.println("获取 datastore 实例失败，请检查参数");
            e.printStackTrace();
        }
//        // 通过参数 params 获取 datastore
//        DataStore datastore = DataStoreFinder.getDataStore(params);
//        if (datastore == null) {
//            throw new RuntimeException("这些参数无法获取数据库存储实例");
//        }
//        System.out.println();
        return datastore;
    }

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 获取 SimpleFeatureType
     * @param: data
     * @return org.opengis.feature.simple.SimpleFeatureType
     **/
    @Override
    public SimpleFeatureType getSimpleFeatureType(IGeomesaDataConfig data) {
        return data.getSimpleFeatureType();
    }

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 在 datastore 添加 SimpleFeatureType
     * @param: datastore
     * @param: sft
     * @return void
     **/
    @Override
    public void createSchema(DataStore datastore, SimpleFeatureType sft) throws IOException {
        System.out.println("正在创建数据结构：" + DataUtilities.encodeType(sft));
        // 在数据源 datastore 中添加 sft 结构
        datastore.createSchema(sft);
        System.out.println();
    }

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 获取 features
     * @param: data
     * @return java.util.List<org.opengis.feature.simple.SimpleFeature>
     **/
    @Override
    public List<SimpleFeature> getFeatures(IGeomesaDataConfig data) {
        System.out.println("获得 csv 文件转换的数据");
        List<SimpleFeature> features = data.getData(2, 1000);
        System.out.println();
        return features;
    }

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 插入批量数据
     * @param: datastore
     * @param: sft
     * @param: features
     * @return void
     **/
    @Override
    public void writeFeatures(DataStore datastore, SimpleFeatureType sft, List<SimpleFeature> features) throws IOException {
        if (features.size() > 0) {
            System.out.println("正在插入数据");
            try (FeatureWriter<SimpleFeatureType, SimpleFeature> writer = datastore.getFeatureWriterAppend(sft.getTypeName(), Transaction.AUTO_COMMIT)) {
                for (SimpleFeature feature : features) {
                    SimpleFeature toWrite = writer.next();
                    toWrite.setAttributes(feature.getAttributes());
                    ((FeatureIdImpl) toWrite.getIdentifier()).setID(feature.getID());
                    toWrite.getUserData().put(Hints.USE_PROVIDED_FID, Boolean.TRUE);
                    toWrite.getUserData().putAll(feature.getUserData());
                    writer.write();
                }
            }
            System.out.println("插入了 " + features.size() + " 数据");
            System.out.println();
        }
    }

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 获取 Queries 信息
     * @param: data
     * @return java.util.List<org.geotools.data.Query>
     **/
    @Override
    public List<Query> getQueries(IGeomesaDataConfig data) {
        return data.getQueries();
    }

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 执行批量查询
     * @param: datastore
     * @param: queries
     * @return void
     **/
    @Override
    public void queryFeature(DataStore datastore, List<Query> queries) throws IOException {
        for (Query query : queries) {
            System.out.println("正在查询： " + ECQL.toCQL(query.getFilter()));
            if (query.getPropertyNames() != null) {
                System.out.println("返回属性： " + Arrays.asList(query.getPropertyNames()));
            }
            if (query.getSortBy() != null) {
                SortBy sort = query.getSortBy()[0];
                System.out.println("排序： " + sort.getPropertyName() + " " + sort.getSortOrder());
            }
            try (FeatureReader<SimpleFeatureType, SimpleFeature> reader = datastore.getFeatureReader(query, Transaction.AUTO_COMMIT)){
                int n = 0;
                while (reader.hasNext()) {
                    SimpleFeature feature = reader.next();
                    if (n++ < 10) {
                        System.out.println(String.format("%02d", n) + " " + DataUtilities.encodeFeature(feature));
                    } else if (n == 10) {
                        System.out.println("...");
                    }
                }
                System.out.println();
                System.out.println("返回 " + n + "条数据");
                System.out.println();
            }
        }
    }

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 删除数据源
     * @param: datastore
     * @param: typeName
     * @return void
     **/
    @Override
    public void cleanup(DataStore datastore, String typeName) {
        if (datastore != null) {
            try {
                System.out.println("开始删除数据源");
                // 判断 datastore 是否是 GeomesaDataStore 的实例
                if (datastore instanceof GeoMesaDataStore) {
                    ((GeoMesaDataStore) datastore).delete();
                } else {
                    ((SimpleFeatureStore) datastore.getFeatureSource(typeName)).removeFeatures(Filter.INCLUDE);
                    datastore.removeSchema(typeName);
                }
            } catch (Exception e) {
                System.err.println("删除数据源发生错误 " + e.toString());
            } finally {
                datastore.dispose();
            }
        }
    }

}
