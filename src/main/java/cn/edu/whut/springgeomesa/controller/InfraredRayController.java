package cn.edu.whut.springgeomesa.controller;

import cn.edu.whut.springgeomesa.config.impl.InfraredRayDataConfig;
import cn.edu.whut.springgeomesa.pojo.dto.GeomesaDTO;
import cn.edu.whut.springgeomesa.service.IInfraredRayService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

/**
 * @ClassName InfraredRayController
 * @Description TODO
 * @Author sheldon
 * @Date 2022/5/8 17:06
 * @Version 1.0
 **/
@RestController
@CrossOrigin
@RequestMapping(value = "/infraredray")
@Api("data")
public class InfraredRayController {
    private final static Logger logger = LoggerFactory.getLogger(InfraredRayController.class);

    @Autowired
    private IInfraredRayService iInfraredRayService;

    @Autowired
    private InfraredRayDataConfig infraredRayDataConfig;

    @Autowired
    private ValueOperations<String, Object> redisString;

    /**
     * @author sheldon
     * @date 2022/5/9
     * @description 插入红外数据
     * @param: null
     * @return
     **/
    @ApiOperation(value = "写入红外数据", notes = "写入 HBase 数据源")
    @ApiImplicitParam(name = "catalogName", value = "HBase 数据源", required = true, dataType = "String", paramType = "path")
    @GetMapping(value = "/insert/{catalogName}")
    public String insertInfrardRay(@PathVariable("catalogName") String catalogName) {
        logger.info(catalogName + "插入红外数据……");
        GeomesaDTO geomesaDTO = new GeomesaDTO();
        geomesaDTO.setParams(new HashMap<String, String>(){
            {
                put("hbase.catalog", catalogName);
                put("hbase.zookeepers", "master,slave1,slave2");
            }
        });
        geomesaDTO.setData(infraredRayDataConfig);
//        new HBaseDemo(geomesaDTO.getParams()).writeData();
//        return "插入数据成功";
        Boolean result = iInfraredRayService.insertInfraredRayData(geomesaDTO.getParams());
        if (result == true) {
            return "插入红外数据成功";
        } else {
            return "插入红外数据失败";
        }
    }

    /**
     * @author sheldon
     * @date 2022/5/16
     * @description 删除 HBase 中的红外数据
     * @param: catalogName
     * @return java.lang.String
     **/
    @ApiOperation(value = "删除数据", notes = "删除 HBase 数据源")
    @ApiImplicitParam(name = "catalogName", value = "HBase 数据源", required = true, dataType = "String", paramType = "path")
    @GetMapping(value = "/delete/{catalogName}")
    public String deleteInfraredRay(@PathVariable("catalogName") String catalogName) {
        logger.info(catalogName + "删除数据……");
        GeomesaDTO geomesaDTO = new GeomesaDTO();
        geomesaDTO.setParams(new HashMap<String, String>() {
            {
                put("hbase.catalog", catalogName);
                put("hbase.zookeepers", "master,slave1,slave2");
            }
        });
        geomesaDTO.setData(infraredRayDataConfig);
        Boolean status = iInfraredRayService.deleteInfraredRayDatastore(geomesaDTO.getParams());
        if (status == true) {
            return "删除红外数据成功";
        } else {
            return "删除红外数据失败";
        }
    }

    /**
     * @author sheldon
     * @date 2022/5/16
     * @description 根据波长查询红外数据
     * @param: catalogName
     * @param: waveLength
     * @return java.lang.String
     **/
    @ApiOperation(value = "查询红外数据", notes = "查询红外数据")
    @ApiImplicitParam(name = "catalogName", value = "HBase 数据源", required = true, dataType = "String", paramType = "path")
    @GetMapping(value = "/query/{catalogName}")
    public String queryInfraredRay(@PathVariable("catalogName") String catalogName, @RequestParam(value = "waveLength", defaultValue = "9") String waveLength,
                                   @RequestParam(value = "SID", required = false) String SID) {
        logger.info(catalogName + "查询数据……");
        if (redisString.get("attributeQuery") != null) {
            logger.info("已在 Redis 数据库中找到……");
            String infraredRayGeoJson = (String) redisString.get("attributeQuery");
            return infraredRayGeoJson;
        } else {
            GeomesaDTO geomesaDTO = new GeomesaDTO();
            geomesaDTO.setParams(new HashMap<String, String>() {
                {
                    put("hbase.catalog", catalogName);
                    put("hbase.zookeepers", "master,slave1,slave2");
                }
            });
            geomesaDTO.setData(infraredRayDataConfig);
            String infraredRayGeoJson = iInfraredRayService.attributeQuery(geomesaDTO.getParams(), waveLength, SID);
            if (infraredRayGeoJson != null) {
                return infraredRayGeoJson;
            } else {
                return "查询红外数据错误";
            }
        }
    }

    @ApiOperation(value = "时空及属性查询", notes = "时空及属性查询")
    @GetMapping("/query/SpatiotemporalAttributeParam")
    public String spatiotemporalAttributeQuery(@RequestParam(value = "catalogName", defaultValue = "data") String catalogName, @RequestParam(value = "SID", required = false) String SID,
                                               @RequestParam(value = "No") String No, @RequestParam(value = "PID", required = false) String PID, @RequestParam(value = "waveLength", required = false) String waveLength) {
        logger.info(catalogName + "时空及属性查询");
        if (redisString.get("SpatiotemporalAttributeParam" + "-" + catalogName + "-" + SID + "-" + No + "-" + PID + "-" + waveLength) != null) {
            logger.info("已在 Redis 数据库中找到……");
            String infraredRayGeoJson = (String) redisString.get("SpatiotemporalAttributeParam" + "-" + catalogName + "-" + SID + "-" + No + "-" + PID + "-" + waveLength);
            return infraredRayGeoJson;
        } else {
            GeomesaDTO geomesaDTO = new GeomesaDTO();
            geomesaDTO.setParams(new HashMap<String, String>() {
                {
                    put("hbase.catalog", catalogName);
                    put("hbase.zookeepers", "master,slave1,slave2");
                }
            });
            geomesaDTO.setData(infraredRayDataConfig);
            String infraredRayGeoJson = iInfraredRayService.spatiotemporalAttributeQuery(geomesaDTO.getParams(), catalogName, SID, PID, No, waveLength);
            if (infraredRayGeoJson != null) {
                return infraredRayGeoJson;
            } else {
                return "查询错误";
            }
        }
    }
}
