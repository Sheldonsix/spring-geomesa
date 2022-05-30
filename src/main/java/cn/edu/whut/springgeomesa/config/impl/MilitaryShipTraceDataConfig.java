package cn.edu.whut.springgeomesa.config.impl;

import cn.edu.whut.springgeomesa.config.IGeomesaDataConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.geotools.data.Query;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.factory.Hints;
import org.locationtech.geomesa.utils.interop.SimpleFeatureTypes;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.stereotype.Component;
import org.opengis.filter.Filter;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @ClassName MilitaryShipTraceDataConfig
 * @Description TODO
 * @Author sheldon
 * @Date 2022/5/25 16:35
 * @Version 1.0
 **/
@Component
public class MilitaryShipTraceDataConfig implements IGeomesaDataConfig {
    private SimpleFeatureType sft = null;
    private List<SimpleFeature> features = null;
    private List<Query> queries = null;

    @Override
    public String getTypeName() {
        return "military_ship_trace";
    }

    @Override
    public SimpleFeatureType getSimpleFeatureType() {
        if (this.sft == null) {
            String builder = "RECORDID:String," +       // 记录号
                    "No:Integer," +                     // 批号
                    "Voyage:String," +                  // 航次
                    "dtg:Date," +                       // 时间
                    "*geom:Point:srid=4326," +          // 位置，纬度、经度
                    "Height:Double," +                  // 高度
                    "Distance:Double," +                // 距离
                    "Az:Double," +                      // 方位
                    "Speed:Double," +                   // 速度
                    "Direction:Double," +               // 方向
                    "Type:String," +                    // 船类型
                    "ShipType:String," +                // 船种类
                    "BelongTo:String," +                // 属性
                    "Nationality:String";               // 国籍
            // 构建
            this.sft = SimpleFeatureTypes.createType(getTypeName(), builder);

            // 设置默认日期列
            this.sft.getUserData().put(SimpleFeatureTypes.DEFAULT_DATE_KEY, "dtg");
        }
        return this.sft;
    }

    @Override
    public List<SimpleFeature> getData(int offset) {
        // 初始化
        List<SimpleFeature> features = new ArrayList<>();

        // 获取文件 URL
        URL input = getClass().getClassLoader().getResource("data/military_ship.csv");
        if (input == null) {
            throw new RuntimeException("Couldn't load resource infrared.csv");
        }

        // 数据中时间的格式：yyyy/M/d H:m
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

        // 创建 Builder
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(getSimpleFeatureType());

        // 将文件中的每行记录依次解析成 SimpleFeature，追加到列表中
        try (CSVParser parser = CSVParser.parse(input, StandardCharsets.UTF_8, CSVFormat.DEFAULT)) {
            for (CSVRecord record : parser) {
                // 从 offset 行开始处理 size 个 record 转成 SimpleFeature
                if (record.getRecordNumber() < offset + 2) {
                    continue;
                }
                try {
                    // 航次
                    builder.set("Voyage", record.get(0));
                    // 批号
                    int n = Integer.parseInt(record.get(1));
                    builder.set("No", n);
                    // 时间
                    builder.set("dtg",
                            Date.from(LocalDate.parse(record.get(2), dateFormat).atStartOfDay(ZoneOffset.UTC).toInstant()));
                    // 经度
                    double longitude = Double.parseDouble(record.get(3));
                    // 维度
                    double latitude = Double.parseDouble(record.get(4));
                    builder.set("geom", "POINT (" + longitude + " " + latitude + ")");
                    // 高度
                    double height = Double.parseDouble(record.get(5));
                    builder.set("Height", height);
                    // 距离
                    double distance = Double.parseDouble(record.get(6));
                    builder.set("Distance", distance);
                    // 方位
                    double az = Double.parseDouble(record.get(7));
                    builder.set("Az", az);
                    // 航速
                    double speed = Double.parseDouble(record.get(8));
                    builder.set("Speed", speed);
                    // 航向
                    double direction = Double.parseDouble(record.get(9));
                    builder.set("Direction", direction);
                    // 类型
                    builder.set("Type", record.get(10));
                    // 种类
                    builder.set("ShipType", record.get(11));
                    // 属性
                    builder.set("BelongTo", record.get(12));
                    // 国籍
                    builder.set("Nationality", record.get(13));

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

    @Override
    public List<Query> getQueries() {
        if (queries == null) {
            try {
                // 初始化
                List<Query> queries = new ArrayList<>();

                // 不使用索引，从数据集中查询航次为72和28的所有记录
                queries.add(new Query(getTypeName(), ECQL.toFilter("Voyage = '72' OR Voyage = '28'")));

                // 使用时间谓词，查询时间在2022-02-13 00:00:00 到2022-02-20 00:00:00范围内的记录
                String during = "dtg DURING 2022-02-13T00:00:00.000Z/2022-02-20T00:00:00.000Z";
                queries.add(new Query(getTypeName(), ECQL.toFilter(during)));

                // 空间查询，查询在(110,42,112,25)内的记录
                String bbox = "bbox(geom,110,42,112,25)";
                queries.add(new Query(getTypeName(), ECQL.toFilter("bbox(geom,110,42,112,25)")));

                // 时空查询，查询时间在2022-02-13 00:00:00 到2022-02-20 00:00:00范围内，空间在(110,42,112,25)内的记录
                queries.add(new Query(getTypeName(), ECQL.toFilter(
                        bbox + " AND " + during
                )));

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
