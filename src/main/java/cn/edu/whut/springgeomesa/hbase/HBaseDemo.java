package cn.edu.whut.springgeomesa.hbase;

import cn.edu.whut.springgeomesa.common.Data;
import cn.edu.whut.springgeomesa.common.InfraredRayData;
import org.geotools.data.*;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.util.factory.Hints;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @ClassName HBaseDemo
 * @Description TODO
 * @Author sheldon
 * @Date 2022/5/10 10:20
 * @Version 1.0
 **/
public class HBaseDemo {
    private DataStore dataStore = null;
    // 红外数据
    private final Data ird;

    public HBaseDemo(Map<String, String> params) {
        createDataStore(params);
        this.ird = new InfraredRayData();
    }

    private void createDataStore(Map<String, String> params) {
        DataStore store = null;
        try {
            store = DataStoreFinder.getDataStore(params);
            if (store == null) {
                System.out.println("获取 datastore 实例失败，检查参数");
                System.exit(-1);
            }
        } catch (Exception e) {
            System.out.println("获取 datastore 实例失败，检查参数");
            e.printStackTrace();
            System.exit(-1);
        }
        this.dataStore = store;
    }

    public void writeData() {
        System.out.println("红外数据写入");
        try {
            writeInfraredRayData((InfraredRayData) this.ird);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeInfraredRayData(InfraredRayData data) throws IOException {
        // 清除数据
        cleanup(data.getTypeName());
        FileWriter bww = new FileWriter("hbb_" + data.getTypeName() + ".csv");
        batchWriteInfraredRayData(data, bww);
        bww.flush();
        bww.close();
    }

    private void batchWriteInfraredRayData(InfraredRayData data, FileWriter w) throws IOException {
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
}
