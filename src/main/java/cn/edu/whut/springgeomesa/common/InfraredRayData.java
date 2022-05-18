package cn.edu.whut.springgeomesa.common;

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
import org.opengis.filter.Filter;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @ClassName InfraredRayData
 * @Description TODO
 * @Author sheldon
 * @Date 2022/5/10 10:14
 * @Version 1.0
 **/
public class InfraredRayData implements Data{
    private SimpleFeatureType sft = null;
    private List<Query> queries = null;
    private Filter subsetFilter = null;
    
    /**
     * @author sheldon
     * @date 2022/5/10
     * @description //TODO
     * @param: 
     * @return java.lang.String
     **/
    @Override
    public String getTypeName() {
        return "infrared_ray";
    }
    
    /**
     * @author sheldon
     * @date 2022/5/10
     * @description //TODO
     * @param: 
     * @return org.opengis.feature.simple.SimpleFeatureType
     **/
    @Override
    public SimpleFeatureType getSimpleFeatureType() {
        if(this.sft == null) {
            // spec
            String builder = "RECORDID:String," +       // 记录号
                    "NO:Integer," +                     // 批号
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
     * @date 2022/5/10
     * @description //TODO
     * @param: offset
     * @param: size
     * @return java.util.List<org.opengis.feature.simple.SimpleFeature>
     **/
    @Override
    public List<SimpleFeature> getData(int offset, int size) {
        // 初始化
        List<SimpleFeature> features = new ArrayList<>();

        // 获取文件URL
        URL input = getClass().getClassLoader().getResource("infraredray/infraredray.csv");
        if (input == null) {
            throw new RuntimeException("Couldn't load resource red.csv");
        }

        // 数据中时间的格式，2022/1/24 7:49
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy/M/d H:m", Locale.CHINA);

        // 创建Builder
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(getSimpleFeatureType());

        // 将文件中的每行记录依次解析成SimpleFeature，并追加到列表中
        try (CSVParser parser = CSVParser.parse(input, StandardCharsets.UTF_8, CSVFormat.DEFAULT)){
            for (CSVRecord record :
                    parser) {
                // 从offset开始处理size个record成SimpleFeature
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
                    builder.set("No", Integer.valueOf(record.get(2)));
                    // 时间
                    builder.set("dtg",
                            Date.from(LocalDate.parse(record.get(3), dateFormat).atStartOfDay(ZoneOffset.UTC).toInstant()));
                    // 波长类型
                    builder.set("WavelengthType", record.get(4));
                    // 波长
                    double wavelength = Double.parseDouble(record.get(5));
                    builder.set("Wavelength", wavelength);
                    // 响应时间
                    double responseTime = Double.parseDouble(record.get(6));
                    builder.set("ResponseTime", responseTime);
                    // 探测率
                    builder.set("DetectionRate", record.get(7));

                    builder.featureUserData(Hints.USE_PROVIDED_FID, Boolean.TRUE);

                    SimpleFeature feature = builder.buildFeature(String.valueOf(record.getRecordNumber()));

                    features.add(feature);
                } catch (Exception e) {
                    System.out.println("invalid infrared ray data.");
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
     * @date 2022/5/10
     * @description //TODO
     * @param: 
     * @return java.util.List<org.geotools.data.Query>
     **/
    @Override
    public List<Query> getQueries() {
        if (queries == null) {
            try {
                // 初始化
                List<Query> queries = new ArrayList<>();

                // 不使用索引，从数据集中查询批次为3047和3059的数据
                queries.add(new Query(getTypeName(), ECQL.toFilter("No = 3047 OR No = 3059")));

                // 使用时间谓词，查询时间在2022-02-13 00:00:00 到2022-02-20 00:00:00范围内的记录
                String during = "dtg DURING 2022-02-13T00:00:00.000Z/2022-02-20T00:00:00.000Z";
                queries.add(new Query(getTypeName(), ECQL.toFilter(during)));

                // 索引查询，查询波长大于4小于5的记录
                queries.add(new Query(getTypeName(), ECQL.toFilter("Wavelength > 4 AND Wavelength < 5")));

                this.queries = Collections.unmodifiableList(queries);
            } catch (CQLException e) {
                throw new RuntimeException("Error creating filter:", e);
            }
        }
        return queries;
    }
    
}
