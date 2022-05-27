package cn.edu.whut.springgeomesa.service.impl;

import cn.edu.whut.springgeomesa.config.IGeomesaDataConfig;
import cn.edu.whut.springgeomesa.repository.IGeomesaRepository;
import cn.edu.whut.springgeomesa.service.IMilitaryShipTraceService;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName MilitaryShipTraceService
 * @Description 军船轨迹数据操作
 * @Author sheldon
 * @Date 2022/5/25 16:58
 * @Version 1.0
 **/
@Service
public class MilitaryShipTraceService implements IMilitaryShipTraceService {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(MilitaryShipTraceService.class);
    private final IGeomesaRepository geomesaRepository;
    private final IGeomesaDataConfig iGeomesaDataConfig;
    private String dataTypeName;

    @Autowired
    private ValueOperations<String, Object> redisString;


    @Autowired
    public MilitaryShipTraceService(IGeomesaRepository geomesaRepository, @Qualifier("militaryShipTraceDataConfig") IGeomesaDataConfig iGeomesaDataConfig) {
        this.geomesaRepository = geomesaRepository;
        this.iGeomesaDataConfig = iGeomesaDataConfig;
        this.dataTypeName = iGeomesaDataConfig.getTypeName();
    }

    @Override
    public String attributeQuery(Map<String, String> params, String queryStr) {
        try {
            // 获取数据源
            DataStore dataStore = geomesaRepository.createDataStore(params);
            // 基本查询语句
            Query query = new Query(dataTypeName, ECQL.toFilter(queryStr));

            logger.info("正在查询：" + ECQL.toCQL(query.getFilter()));
            if (query.getPropertyNames() != null) {
                logger.info("返回属性： " + Arrays.asList(query.getPropertyNames()));
            }
            // 获取 FeatureSource
            SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataTypeName);
            // 获取 featureCollection
            SimpleFeatureCollection featureCollection = featureSource.getFeatures(query);
            FeatureJSON fjson = new FeatureJSON();
            StringWriter writer = new StringWriter();
            logger.info("正在写出 GeoJson 数据： ");
            // 写出 GeoJson 格式
            fjson.writeFeatureCollection(featureCollection, writer);
            String json = writer.toString();
            // 写入 Redis 数据库
            redisString.set(queryStr, json, 60 * 10, TimeUnit.SECONDS);
            String value = (String) redisString.get(queryStr);
            System.out.println(value);
            return json;
        } catch (Exception e) {
            logger.error("基本时空查询错误：" + e);
            throw new RuntimeException("基本时空查询错误：" + e);
        }
    }

    @Override
    public Boolean insertMilitaryShipTraceData(Map<String, String> params) {
        try {
            // 创建数据源
            DataStore dataStore = geomesaRepository.createDataStore(params);
            // 获取 SimpleFeatureType
            SimpleFeatureType sft = geomesaRepository.getSimpleFeatureType(iGeomesaDataConfig);
            // datastore 创建 SimpleFeatureType
            geomesaRepository.createSchema(dataStore, sft);
            List<SimpleFeature> features = geomesaRepository.getFeatures(iGeomesaDataConfig);
            if (features.size() > 0) {
                // 插入批量数据
                geomesaRepository.writeFeatures(dataStore, sft, features);
            }
            System.out.println("写入： " + features.size() + "/" + "features");
            return true;
        } catch (Exception e) {
            logger.error("插入数据失败：", e);
            throw new RuntimeException("插入数据失败：", e);
        }
    }

    @Override
    public Boolean deleteMilitaryShipTraceDatastore(Map<String, String> params) {
        try {
            DataStore dataStore = geomesaRepository.createDataStore(params);
            geomesaRepository.cleanup(dataStore, dataTypeName);
            return true;
        } catch (Exception e) {
            logger.error("删除数据失败：", e);
            throw new RuntimeException("删除数据失败：", e);
        }
    }

    @Override
    public String spatiotemporalAttributeQuery(Map<String, String> params, String catalogName, String SID, String PID, String No, String waveLength) {
        try {
            // 获得数据源
            DataStore dataStore = geomesaRepository.createDataStore(params);
            Query query = new Query(dataTypeName, ECQL.toFilter("SID = " + SID + " AND " + "PID = " + PID + " AND " + "Wavelength > " + waveLength + " AND " + "Wavelength < " + (Integer.parseInt(waveLength) + 1) + " AND " + "No = " + No),
                    new String[]{"No", "WavelengthType"});
            logger.info("正在查询：" + ECQL.toCQL(query.getFilter()));
            if (query.getPropertyNames() != null) {
                logger.info("返回属性： " + Arrays.asList(query.getPropertyNames()));
            }
            // 获取 FeatureSource
            SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataTypeName);
            // 获取 featureCollection
            SimpleFeatureCollection featureCollection = featureSource.getFeatures(query);
            FeatureJSON featureJSON = new FeatureJSON();
            StringWriter writer = new StringWriter();
            logger.info("正在写 GeoJson 数据： ");
            // 写出 GeoJson 格式数据
            featureJSON.writeFeatureCollection(featureCollection, writer);
            String json = writer.toString();
//            redisString.set("SpatiotemporalAttributeParam" + "-" + catalogName + "-" + SID + "-" + No + "-" + PID + "-" + waveLength, json, 60*10, TimeUnit.SECONDS);
//            String value = (String) redisString.get("SpatiotemporalAttributeParam" + "-" + catalogName + "-" + SID + "-" + No + "-" + PID + "-" + waveLength);
//            System.out.println(value);
            return json;
        } catch (Exception e) {
            logger.error("时空及属性查询：" + e);
            throw new RuntimeException("时空及属性查询：" + e);
        }
    }
}
