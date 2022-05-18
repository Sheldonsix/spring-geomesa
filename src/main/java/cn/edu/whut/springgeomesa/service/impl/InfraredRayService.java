package cn.edu.whut.springgeomesa.service.impl;

import cn.edu.whut.springgeomesa.config.IGeomesaDataConfig;
import cn.edu.whut.springgeomesa.config.impl.InfraredRayDataConfig;
import cn.edu.whut.springgeomesa.repository.IGeomesaRepository;
import cn.edu.whut.springgeomesa.service.IInfraredRayService;
import org.geotools.data.*;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.util.factory.Hints;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName InfraredRayService
 * @Description TODO
 * @Author sheldon
 * @Date 2022/5/8 17:20
 * @Version 1.0
 **/
@Service
public class InfraredRayService implements IInfraredRayService {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(InfraredRayService.class);
    private final IGeomesaRepository geomesaRepository;
    private final IGeomesaDataConfig iGeomesaDataConfig;
    private String dataTypeName;
//    private final InfraredRayDataConfig ird = new InfraredRayDataConfig();


    @Autowired
    public InfraredRayService(IGeomesaRepository geomesaRepository, @Qualifier("infraredRayDataConfig")IGeomesaDataConfig iGeomesaDataConfig) {
        this.geomesaRepository = geomesaRepository;
        this.iGeomesaDataConfig = iGeomesaDataConfig;
        this.dataTypeName = iGeomesaDataConfig.getTypeName();
    }
    
    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 属性查询
     * @param: params
     * @param: SID 信源号
     * @return java.lang.String
     **/
    @Override
    public String attributeQuery(Map<String, String> params, String waveLength, String SID) {
        try {
            // 获取数据源
            DataStore dataStore = geomesaRepository.createDataStore(params);
            // 基本查询语句
            Query query;
            if (SID != null) {
                query = new Query(dataTypeName, ECQL.toFilter("Wavelength > " + "'" + waveLength + "'" + " AND " + "Wavelength < " + "'" + (waveLength + 1) + "'" + " AND " +"SID = " + SID));
            } else {
                query = new Query(dataTypeName, ECQL.toFilter("Wavelength > "  + waveLength + " AND " + "Wavelength < " + (Integer.parseInt(waveLength) + 1)));
            }
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
            return json;
        } catch (Exception e) {
            logger.error("基本时空查询错误：" + e);
            throw new RuntimeException("基本时空查询错误：" + e);
        }
    }
    
    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 总方法：创建数据源并写入数据
     * @param: params
     * @return java.lang.Boolean
     **/
    @Override
    public Boolean insertInfraredRayData(Map<String, String> params) {
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
/**
    private Map<String, String> defaultParams() {
        // 默认配置参数
        // 元数据表名 hbase.catalog: DMTest
        // HBase 实例的 zookeeper 地址 hbase.zookeepers: localhost
        Map<String, String> params = new HashMap<>();
        params.put("hbase.catalog", "DMTest");
        params.put("hbase.zookeepers", "master,slave1,slave2");
        return params;
    }

    private void createDataStore(Map<String, String> params) {
        DataStore store = null;
        try {
            store = DataStoreFinder.getDataStore(params);
            if (store == null) {
                System.out.println("获取 datastore 实例失败，请检查参数");
                // 获取实例失败直接退出
                System.exit(-1);
            }
        } catch (Exception e) {
            System.out.println("获取 datastore 实例失败，请检查参数");
            e.printStackTrace();
            // 获取实例失败直接退出
            System.exit(-1);
        }
        this.dataStore = store;
    }

    public void writeData() throws IOException {
        System.out.println("红外数据写入");
        writeInfraredRayData(this.ird);
    }

    private void writeInfraredRayData(InfraredRayDataConfig data) throws IOException {
        // 清除数据
        FileWriter bww = new FileWriter("hbb_" + data.getTypeName() + ".csv");
        batchWriteInfraredRayData(data, bww);
        bww.flush();
        bww.close();
    }

    private void batchWriteInfraredRayData(InfraredRayDataConfig data, FileWriter w) throws IOException {
        // 创建 Schema
        System.out.println("创建 Schema");
        SimpleFeatureType sft = data.getSimpleFeatureType();
        createSchema(sft);
        // 写入数据
        System.out.println("开始写入");
        int count = 0;
        while (count < 500000) {
            List<SimpleFeature> features = data.getData(count, 1000);
            if (features.size() > 0) {
                batchWriteFeatures(sft, features, w);
            } else {
                break;
            }
            System.out.println("写入 " + features.size() + "/" + count + " features");
            count += features.size();
        }
    }

    private void createSchema(SimpleFeatureType simpleFeatureType) throws IOException {
        System.out.println("创建 Schema: " + DataUtilities.encodeType(simpleFeatureType));
        dataStore.createSchema(simpleFeatureType);
    }

    private void batchWriteFeatures(SimpleFeatureType simpleFeatureType, List<SimpleFeature> features, FileWriter w) throws IOException {
        if(features.size() > 0) {
            long startTime = System.currentTimeMillis();    //获取开始时间
            try(FeatureWriter<SimpleFeatureType, SimpleFeature> writer =
                        dataStore.getFeatureWriterAppend(simpleFeatureType.getTypeName(), Transaction.AUTO_COMMIT)) {
                for (SimpleFeature feature :
                        features) {
                    // 获取下一个要写入的sf
                    SimpleFeature toWrite = writer.next();

                    // 复制属性值
                    toWrite.setAttributes(feature.getAttributes());

                    // if you want to set the feature ID, you have to cast to an implementation class
                    // and add the USE_PROVIDED_FID hint to the user data
                    ((FeatureIdImpl) toWrite.getIdentifier()).setID(feature.getID());
                    toWrite.getUserData().put(Hints.USE_PROVIDED_FID, Boolean.TRUE);

                    // alternatively, you can use the PROVIDED_FID hint directly
                    // toWrite.getUserData().put(Hints.PROVIDED_FID, feature.getID());

                    // if no feature ID is set, a UUID will be generated for you

                    // make sure to copy the user data, if there is any
                    toWrite.getUserData().putAll(feature.getUserData());

                    // write the feature
                    writer.write();
                }
            }
            long endTime = System.currentTimeMillis();    // 获取结束时间
            w.write((endTime - startTime) + "\n"); // 将耗时写到文件中
        }
    }

    private void cleanup(String typeName) {
        if (this.dataStore != null) {
            try {
                System.out.println("Cleaning up test data");
                ((SimpleFeatureStore) this.dataStore.getFeatureSource(typeName)).removeFeatures(Filter.INCLUDE);
                this.dataStore.removeSchema(typeName);
            } catch (Exception e) {
                System.err.println("Exception cleaning up test data: " + e.toString());
            }
        }
    }
**/
    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 总方法：删除数据源
     * @param: params
     * @return java.lang.Boolean
     **/
    @Override
    public Boolean deleteInfraredRayDatastore(Map<String, String> params) {
        try {
            DataStore dataStore = geomesaRepository.createDataStore(params);
            geomesaRepository.cleanup(dataStore, dataTypeName);
            return true;
        } catch (Exception e) {
            logger.error("删除数据失败：", e);
            throw new RuntimeException("删除数据失败：", e);
        }
    }

    /**
     * @author sheldon
     * @date 2022/5/17
     * @description 时空交互查询
     * @param: params
     * @param: catalogName
     * @param: SID
     * @param: PID
     * @param: waveLength
     * @param: responseTime
     * @param: detectionRate
     * @return java.lang.String
     **/
    @Override
    public String spatiotemporalAttributeQuery(Map<String, String> params, String SID, String PID, String No, String waveLength) {
        try {
            // 获得数据源
            DataStore dataStore = geomesaRepository.createDataStore(params);
            Query query = new Query(dataTypeName, ECQL.toFilter("SID = " + SID + " AND " + "PID = " + PID + " AND " + "Wavelength > " + waveLength +" AND " + "Wavelength < " + (Integer.parseInt(waveLength) + 1) + " AND " + "No = " + No),
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
            return json;
        } catch (Exception e) {
            logger.error("时空及属性查询：" + e);
            throw new RuntimeException("时空及属性查询：" + e);
        }
    }

}
