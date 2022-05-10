package cn.edu.whut.springgeomesa.common;

import org.geotools.data.Query;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.List;

/**
 * @ClassName Data
 * @Description TODO
 * @Author sheldon
 * @Date 2022/5/10 10:11
 * @Version 1.0
 **/
public interface Data {
    String getTypeName();
    SimpleFeatureType getSimpleFeatureType();
    List<SimpleFeature> getData(int offset, int size);
    List<Query> getQueries();
}
