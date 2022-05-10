package cn.edu.whut.springgeomesa.config.impl;

import cn.edu.whut.springgeomesa.config.IGeomesaDataConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet;
import org.geotools.data.Query;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.factory.Hints;
import org.locationtech.geomesa.utils.interop.SimpleFeatureTypes;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @ClassName InfraredRayDataConfig
 * @Description 红外线数据接口
 * @Author sheldon
 * @Date 2022/5/8 15:58
 * @Version 1.0
 **/
@Component
public class InfraredRayDataConfig implements IGeomesaDataConfig {
    private static final Logger logger = LoggerFactory.getLogger(InfraredRayDataConfig.class);

    private SimpleFeatureType sft = null;
    private List<SimpleFeature> features = null;
    private List<Query> queries = null;
    private Filter subsetFilter = null;

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 获取数据类的 Schema 名
     * @param:
     * @return java.lang.String
     **/
    @Override
    public String getTypeName() {
        return "infrared_ray";
    }

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 获取数据集的 Simple Feature Type 实例
     * @param:
     * @return org.opengis.feature.simple.SimpleFeatureType
     **/
    @Override
    public SimpleFeatureType getSimpleFeatureType() {
        if(this.sft == null) {
            String builder = "RECORDID:String," +       // 记录号
                    "No:Integer," +                     // 批号
                    "PID:String," +                     // PlatformID 平台号
                    "SID:String," +                     // SourceID 信源号
                    "dtg:Date," +                       // 时间
                    "WavelengthType:String," +          // 波长类型
                    "Wavelength:Double:index=true," +   // 波长
                    "ResponseTime:Double," +            // 响应时间
                    "DetectionRate:String";             // 探测率
            this.sft = SimpleFeatureTypes.createType(getTypeName(), builder);
            this.sft.getUserData().put(SimpleFeatureTypes.DEFAULT_DATE_KEY, "dtg");
        }
        return this.sft;
    }

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 从红外数据文件中获取数据，按每行构建成 SimpleFeature，分页读取
     * @param: offset 偏移量，相对于全部记录而言，从 0 开始
     * @param: size 读取的数量
     * @return java.util.List<org.opengis.feature.simple.SimpleFeature> 返回 SimpleFeature 列表
     **/
    @Override
    public List<SimpleFeature> getData(int offset, int size) {
        // 初始化
        List<SimpleFeature> features = new ArrayList<>();

        // 获取文件 URL
        URL input = getClass().getClassLoader().getResource("infraredray/infraredray.csv");
        if (input == null) {
            throw new RuntimeException("Couldn't load resource infrared.csv");
        }

        // 数据中时间的格式：yyyy/M/d H:m
        DateTimeFormatter dataFormat = DateTimeFormatter.ofPattern("yyy/M/d H:m", Locale.CHINA);

        // 创建 Builder
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(getSimpleFeatureType());

        // 将文件中的每行记录依次解析成 SimpleFeature，追加到列表中
        try (CSVParser parser = CSVParser.parse(input, StandardCharsets.UTF_8, CSVFormat.DEFAULT)){
            for (CSVRecord record : parser) {
                // 从 offset 行开始处理 size 个 record 转成 SimpleFeature
                if (record.getRecordNumber() < offset + 2) {
                    continue;
                } else if (record.getRecordNumber() >= offset + 2 + size) {
                    break;
                }
                try {
                    // 平台号
                    builder.set("PID", record.get(0));
                    // 信源号
                    builder.set("SID", record.get(1));
                    // 批号
                    builder.set("NO", record.get(2));
                    // 时间
                    builder.set("dtg", Date.from(LocalDate.parse(record.get(3), dataFormat).atStartOfDay(ZoneOffset.UTC).toInstant()));
                    // 波长类型
                    builder.set("WavelengthType", record.get(4));
                    // 波长
                    builder.set("Wavelength", Double.parseDouble(record.get(5)));
                    // 响应时间
                    builder.set("ResponseTime", Double.parseDouble(record.get(6)));
                    // 探测率
                    builder.set("DetectiveRate", record.get(7));

                    builder.featureUserData(Hints.USE_PROVIDED_FID, Boolean.TRUE);

                    SimpleFeature feature = builder.buildFeature(String.valueOf(record.getRecordNumber()));

                    features.add(feature);
                } catch (Exception e) {
                    System.out.println("Invalid infrared ray data.");
                    System.out.println(e.toString() + record.toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.unmodifiableList(features);
    }

    /**
     * @author sheldon
     * @date 2022/5/8
     * @description 获取数据集的查询
     * @param:
     * @return java.util.List<org.geotools.data.Query>
     **/
    @Override
    public List<Query> getQueries() {
        if (queries == null) {
            try {
                // 初始化
                List<Query> queries = new ArrayList<>();

                // 在数据集中查询批次为 3047 和 3059 的数据
                queries.add(new Query(getTypeName(), ECQL.toFilter("NO = 3047 OR NO = 3059")));

                // 使用时间为此，查询时间在 2022-02-13 00:00:00 到 2022-02-20 00:00:00 范围内的记录
                String during = "dtg DURING 2022-02-13T00:00:00.000Z/2022-02-20T00:00:00.000Z";
                queries.add(new Query(getTypeName(), ECQL.toFilter(during)));

                // 索引查询，查询波长大于 4 小于 5 的记录
                queries.add(new Query(getTypeName(), ECQL.toFilter("Wavelength > 4 AND Wavelength < 5")));

                this.queries = Collections.unmodifiableList(queries);
            } catch (CQLException e) {
                throw new RuntimeException("Error creating filter:", e);
            }
        }
        return queries;
    }

    @Override
    public Filter getSubsetFilter() {
        return null;
    }
}
